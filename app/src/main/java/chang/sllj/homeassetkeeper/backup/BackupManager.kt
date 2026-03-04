package chang.sllj.homeassetkeeper.backup

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.room.withTransaction
import chang.sllj.homeassetkeeper.data.local.AppDatabase
import chang.sllj.homeassetkeeper.data.local.dao.ItemDao
import chang.sllj.homeassetkeeper.data.local.dao.MaintenanceLogDao
import chang.sllj.homeassetkeeper.data.local.dao.SpecificationDao
import chang.sllj.homeassetkeeper.data.local.dao.WarrantyReceiptDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.firstOrNull
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates the full backup/restore pipeline.
 *
 * ## Export (backup)
 * 1. One-shot read of all entities via DAOs.
 * 2. Serialize to a JSON document ([BackupSerializer]) and four CSV files ([CsvExporter]).
 * 3. Package everything into a ZIP in memory, including any captured JPEG files.
 * 4. Write the ZIP bytes to the SAF [Uri] selected by the user.
 *
 * ## Import (restore)
 * 1. Open the SAF [Uri] as a [ZipInputStream].
 * 2. Extract `metadata.json` and all `images/` entries.
 * 3. Deserialize entities; rewrite image paths to the current device's storage directory.
 * 4. Atomically clear the database and re-insert all rows via [AppDatabase.withTransaction].
 *
 * All file I/O methods are suspend functions intended to run on [kotlinx.coroutines.Dispatchers.IO].
 */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val itemDao: ItemDao,
    private val specDao: SpecificationDao,
    private val warrantyDao: WarrantyReceiptDao,
    private val maintenanceDao: MaintenanceLogDao
) {

    companion object {
        private const val ENTRY_METADATA = "metadata.json"
        private const val ENTRY_ITEMS_CSV = "items.csv"
        private const val ENTRY_SPECS_CSV = "specifications.csv"
        private const val ENTRY_WARRANTIES_CSV = "warranty_receipts.csv"
        private const val ENTRY_MAINTENANCE_CSV = "maintenance_logs.csv"
        private const val IMAGES_DIR = "images/"
    }

    // ── Export ────────────────────────────────────────────────────────────────

    /**
     * Builds the full backup ZIP in memory and writes it to [destinationUri].
     *
     * @throws IOException if the destination stream cannot be opened.
     */
    suspend fun exportToUri(destinationUri: Uri) {
        val zipBytes = buildBackupZip()
        context.contentResolver.openOutputStream(destinationUri)?.use { out ->
            out.write(zipBytes)
        } ?: throw IOException("Cannot open output stream for the selected file.")
    }

    /**
     * Loads all entities from the database, serialises them, and packages
     * everything (including captured images) into an in-memory ZIP.
     */
    private suspend fun buildBackupZip(): ByteArray {
        val items = itemDao.getAllActiveItems().firstOrNull() ?: emptyList()
        val archived = itemDao.getAllArchivedItems().firstOrNull() ?: emptyList()
        val allItems = items + archived
        val specs = allItems.flatMap { item ->
            specDao.getSpecsForItem(item.id).firstOrNull() ?: emptyList()
        }
        val warranties = allItems.flatMap { item ->
            warrantyDao.getWarrantiesForItem(item.id).firstOrNull() ?: emptyList()
        }
        val maintenance = allItems.flatMap { item ->
            maintenanceDao.getLogsForItem(item.id).firstOrNull() ?: emptyList()
        }

        val json = BackupSerializer.serialize(allItems, specs, warranties, maintenance)
        val itemsCsv = CsvExporter.exportItemsCsv(allItems)
        val specsCsv = CsvExporter.exportSpecsCsv(specs)
        val warrantiesCsv = CsvExporter.exportWarrantiesCsv(warranties)
        val maintenanceCsv = CsvExporter.exportMaintenanceCsv(maintenance)

        // Collect unique, existing image paths from items (comma-separated) and warranty receipts.
        val imagePaths = (
            allItems.flatMap { it.imagePaths.split(",").filter(String::isNotBlank) } +
            warranties.map { it.receiptImagePath }
        ).filter { it.isNotEmpty() }.distinct()

        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zip ->
            zip.writeTextEntry(ENTRY_METADATA, json)
            zip.writeTextEntry(ENTRY_ITEMS_CSV, itemsCsv)
            zip.writeTextEntry(ENTRY_SPECS_CSV, specsCsv)
            zip.writeTextEntry(ENTRY_WARRANTIES_CSV, warrantiesCsv)
            zip.writeTextEntry(ENTRY_MAINTENANCE_CSV, maintenanceCsv)

            imagePaths.forEach { absPath ->
                val file = File(absPath)
                if (file.exists() && file.isFile) {
                    zip.putNextEntry(ZipEntry("$IMAGES_DIR${file.name}"))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        }
        return baos.toByteArray()
    }

    // ── Import ────────────────────────────────────────────────────────────────

    /**
     * Reads a ZIP from [sourceUri], restores the database, and re-copies image files
     * to [Context.getExternalFilesDir]. Existing data is **replaced** entirely.
     *
     * @throws IOException if the source cannot be read.
     * @throws org.json.JSONException if `metadata.json` is malformed.
     * @throws IllegalArgumentException if the backup version is incompatible.
     */
    suspend fun importFromUri(sourceUri: Uri) {
        val imagesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?: context.filesDir
        imagesDir.mkdirs()

        var backupJson: String? = null

        // First pass: extract metadata.json and all image files.
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    when {
                        entry.name == ENTRY_METADATA -> {
                            backupJson = zip.bufferedReader(Charsets.UTF_8).readText()
                        }
                        entry.name.startsWith(IMAGES_DIR) -> {
                            val fileName = entry.name.removePrefix(IMAGES_DIR)
                            if (fileName.isNotEmpty()) {
                                File(imagesDir, fileName).outputStream().use { zip.copyTo(it) }
                            }
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } ?: throw IOException("Cannot open the selected backup file.")

        val json = backupJson
            ?: throw IOException("Invalid backup archive: $ENTRY_METADATA not found.")

        val data = BackupSerializer.deserialize(json)

        // Rewrite absolute image paths to the current device's storage directory.
        // imagePaths is comma-separated; each segment is rewritten individually.
        val fixedItems = data.items.map { item ->
            if (item.imagePaths.isNotBlank()) {
                val rewritten = item.imagePaths
                    .split(",")
                    .filter(String::isNotBlank)
                    .joinToString(",") { path ->
                        File(imagesDir, File(path).name).absolutePath
                    }
                item.copy(imagePaths = rewritten)
            } else item
        }
        val fixedWarranties = data.warrantyReceipts.map { warranty ->
            if (warranty.receiptImagePath.isNotEmpty()) {
                warranty.copy(
                    receiptImagePath = File(
                        imagesDir, File(warranty.receiptImagePath).name
                    ).absolutePath
                )
            } else warranty
        }

        // Atomic database replacement inside a single transaction.
        database.withTransaction {
            // clearAllTables() is generated by Room in a FK-safe order.
            database.clearAllTables()
            fixedItems.forEach { itemDao.upsertItem(it) }
            data.specifications.forEach { specDao.upsertSpec(it) }
            fixedWarranties.forEach { warrantyDao.upsertWarranty(it) }
            data.maintenanceLogs.forEach { maintenanceDao.upsertLog(it) }
        }
    }
}

// ── ZipOutputStream extension ─────────────────────────────────────────────────

private fun ZipOutputStream.writeTextEntry(name: String, content: String) {
    putNextEntry(ZipEntry(name))
    write(content.toByteArray(Charsets.UTF_8))
    closeEntry()
}

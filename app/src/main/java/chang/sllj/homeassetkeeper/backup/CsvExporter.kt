package chang.sllj.homeassetkeeper.backup

import chang.sllj.homeassetkeeper.data.local.entity.ItemEntity
import chang.sllj.homeassetkeeper.data.local.entity.MaintenanceLogEntity
import chang.sllj.homeassetkeeper.data.local.entity.SpecificationEntity
import chang.sllj.homeassetkeeper.data.local.entity.WarrantyReceiptEntity
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Produces RFC-4180-compliant CSV strings for each entity type.
 *
 * Fields containing commas, double-quotes, or newlines are wrapped in double
 * quotes with internal quotes escaped by doubling ("").
 *
 * Timestamps are written in both their raw epoch-ms form and a human-readable
 * ISO-8601 UTC form so the export is useful for spreadsheet analysis.
 */
object CsvExporter {

    private val ISO_FORMATTER: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC)

    // ── Items ─────────────────────────────────────────────────────────────────

    fun exportItemsCsv(items: List<ItemEntity>): String = buildString {
        appendLine(
            csvRow(
                "id", "name", "category", "brand", "modelNumber", "serialNumber",
                "purchaseDateMs", "purchaseDate", "purchasePriceCents", "purchasePrice",
                "location", "notes", "imagePaths", "isArchived", "createdAtMs", "updatedAtMs"
            )
        )
        items.forEach { item ->
            appendLine(
                csvRow(
                    item.id,
                    item.name,
                    item.category,
                    item.brand,
                    item.modelNumber,
                    item.serialNumber,
                    item.purchaseDateMs?.toString() ?: "",
                    item.purchaseDateMs?.toIsoDate() ?: "",
                    item.purchasePriceCents?.toString() ?: "",
                    item.purchasePriceCents?.let { "%.2f".format(it / 100.0) } ?: "",
                    item.location,
                    item.notes,
                    item.imagePaths,
                    item.isArchived.toString(),
                    item.createdAtMs.toString(),
                    item.updatedAtMs.toString()
                )
            )
        }
    }

    // ── Specifications ────────────────────────────────────────────────────────

    fun exportSpecsCsv(specs: List<SpecificationEntity>): String = buildString {
        appendLine(csvRow("id", "itemId", "key", "value", "sortOrder"))
        specs.forEach { spec ->
            appendLine(csvRow(spec.id, spec.itemId, spec.key, spec.value, spec.sortOrder.toString()))
        }
    }

    // ── Warranty receipts ─────────────────────────────────────────────────────

    fun exportWarrantiesCsv(warranties: List<WarrantyReceiptEntity>): String = buildString {
        appendLine(
            csvRow(
                "id", "itemId", "warrantyType", "providerName",
                "startDateMs", "startDate", "expiryDateMs", "expiryDate",
                "receiptImagePath", "notes", "isAlertEnabled", "alertDaysBefore"
            )
        )
        warranties.forEach { w ->
            appendLine(
                csvRow(
                    w.id,
                    w.itemId,
                    w.warrantyType.name,
                    w.providerName,
                    w.startDateMs.toString(),
                    w.startDateMs.toIsoDate(),
                    w.expiryDateMs.toString(),
                    w.expiryDateMs.toIsoDate(),
                    w.receiptImagePath,
                    w.notes,
                    w.isAlertEnabled.toString(),
                    w.alertDaysBefore.toString()
                )
            )
        }
    }

    // ── Maintenance logs ──────────────────────────────────────────────────────

    fun exportMaintenanceCsv(logs: List<MaintenanceLogEntity>): String = buildString {
        appendLine(
            csvRow(
                "id", "itemId", "description",
                "scheduledDateMs", "scheduledDate",
                "completedDateMs", "completedDate",
                "costCents", "cost", "notes", "createdAtMs"
            )
        )
        logs.forEach { log ->
            appendLine(
                csvRow(
                    log.id,
                    log.itemId,
                    log.description,
                    log.scheduledDateMs?.toString() ?: "",
                    log.scheduledDateMs?.toIsoDate() ?: "",
                    log.completedDateMs?.toString() ?: "",
                    log.completedDateMs?.toIsoDate() ?: "",
                    log.costCents?.toString() ?: "",
                    log.costCents?.let { "%.2f".format(it / 100.0) } ?: "",
                    log.notes,
                    log.createdAtMs.toString()
                )
            )
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun csvRow(vararg fields: String): String =
        fields.joinToString(",") { field -> escapeCsvField(field) }

    /**
     * RFC-4180 escaping: wrap the field in double-quotes if it contains a comma,
     * double-quote, or newline; escape internal double-quotes by doubling them.
     */
    private fun escapeCsvField(field: String): String {
        val needsQuoting = field.contains(',') || field.contains('"') || field.contains('\n')
        return if (needsQuoting) "\"${field.replace("\"", "\"\"")}\"" else field
    }

    private fun Long.toIsoDate(): String =
        ISO_FORMATTER.format(Instant.ofEpochMilli(this))
}

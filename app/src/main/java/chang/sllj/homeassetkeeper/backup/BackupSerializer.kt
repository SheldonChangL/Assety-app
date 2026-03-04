package chang.sllj.homeassetkeeper.backup

import chang.sllj.homeassetkeeper.data.local.entity.ItemEntity
import chang.sllj.homeassetkeeper.data.local.entity.MaintenanceLogEntity
import chang.sllj.homeassetkeeper.data.local.entity.SpecificationEntity
import chang.sllj.homeassetkeeper.data.local.entity.WarrantyReceiptEntity
import chang.sllj.homeassetkeeper.data.local.entity.WarrantyType
import org.json.JSONArray
import org.json.JSONObject

/**
 * Serialises and deserialises the full application dataset to/from a single
 * UTF-8 JSON string. Uses [org.json] (bundled in Android SDK — no extra dependency).
 *
 * ## Format
 * ```json
 * {
 *   "version": 1,
 *   "exportedAtMs": 1735000000000,
 *   "items": [ { … } ],
 *   "specifications": [ { … } ],
 *   "warrantyReceipts": [ { … } ],
 *   "maintenanceLogs": [ { … } ]
 * }
 * ```
 *
 * Nullable Long fields are stored as JSON `null` via [JSONObject.NULL] so that
 * future schema additions can use the `has(key) && !isNull(key)` pattern safely.
 *
 * [WarrantyType] is stored by its enum `name` (string) rather than ordinal so
 * reordering the enum never corrupts existing backups.
 */
object BackupSerializer {

    const val BACKUP_VERSION = 1

    // ── Public data holder ────────────────────────────────────────────────────

    data class BackupData(
        val version: Int,
        val exportedAtMs: Long,
        val items: List<ItemEntity>,
        val specifications: List<SpecificationEntity>,
        val warrantyReceipts: List<WarrantyReceiptEntity>,
        val maintenanceLogs: List<MaintenanceLogEntity>
    )

    // ── Serialise ─────────────────────────────────────────────────────────────

    fun serialize(
        items: List<ItemEntity>,
        specs: List<SpecificationEntity>,
        warranties: List<WarrantyReceiptEntity>,
        maintenanceLogs: List<MaintenanceLogEntity>
    ): String = JSONObject().apply {
        put("version", BACKUP_VERSION)
        put("exportedAtMs", System.currentTimeMillis())
        put("items", JSONArray(items.map { it.toJson() }))
        put("specifications", JSONArray(specs.map { it.toJson() }))
        put("warrantyReceipts", JSONArray(warranties.map { it.toJson() }))
        put("maintenanceLogs", JSONArray(maintenanceLogs.map { it.toJson() }))
    }.toString(2)  // pretty-printed for human readability inside the ZIP

    // ── Deserialise ───────────────────────────────────────────────────────────

    /**
     * Parses a JSON string produced by [serialize].
     * @throws org.json.JSONException if the string is not valid JSON.
     * @throws IllegalArgumentException if required top-level keys are missing.
     */
    fun deserialize(json: String): BackupData {
        val root = JSONObject(json)
        val version = root.getInt("version")
        require(version <= BACKUP_VERSION) {
            "Backup version $version is newer than this app supports (max $BACKUP_VERSION)."
        }
        return BackupData(
            version = version,
            exportedAtMs = root.getLong("exportedAtMs"),
            items = root.getJSONArray("items").mapObjects { toItemEntity() },
            specifications = root.getJSONArray("specifications").mapObjects { toSpecEntity() },
            warrantyReceipts = root.getJSONArray("warrantyReceipts").mapObjects { toWarrantyEntity() },
            maintenanceLogs = root.getJSONArray("maintenanceLogs").mapObjects { toMaintenanceEntity() }
        )
    }

    // ── Entity → JSONObject ───────────────────────────────────────────────────

    private fun ItemEntity.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("category", category)
        put("brand", brand)
        put("modelNumber", modelNumber)
        put("serialNumber", serialNumber)
        put("purchaseDateMs", purchaseDateMs ?: JSONObject.NULL)
        put("purchasePriceCents", purchasePriceCents ?: JSONObject.NULL)
        put("location", location)
        put("notes", notes)
        put("imagePaths", imagePaths)
        put("isArchived", isArchived)
        put("createdAtMs", createdAtMs)
        put("updatedAtMs", updatedAtMs)
    }

    private fun SpecificationEntity.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("itemId", itemId)
        put("key", key)
        put("value", value)
        put("sortOrder", sortOrder)
    }

    private fun WarrantyReceiptEntity.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("itemId", itemId)
        put("warrantyType", warrantyType.name)
        put("providerName", providerName)
        put("startDateMs", startDateMs)
        put("expiryDateMs", expiryDateMs)
        put("receiptImagePath", receiptImagePath)
        put("notes", notes)
        put("isAlertEnabled", isAlertEnabled)
        put("alertDaysBefore", alertDaysBefore)
    }

    private fun MaintenanceLogEntity.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("itemId", itemId)
        put("description", description)
        put("scheduledDateMs", scheduledDateMs ?: JSONObject.NULL)
        put("completedDateMs", completedDateMs ?: JSONObject.NULL)
        put("costCents", costCents ?: JSONObject.NULL)
        put("notes", notes)
        put("createdAtMs", createdAtMs)
    }

    // ── JSONObject → Entity ───────────────────────────────────────────────────

    private fun JSONObject.toItemEntity() = ItemEntity(
        id = getString("id"),
        name = getString("name"),
        category = getString("category"),
        brand = getString("brand"),
        modelNumber = getString("modelNumber"),
        serialNumber = getString("serialNumber"),
        purchaseDateMs = longOrNull("purchaseDateMs"),
        purchasePriceCents = longOrNull("purchasePriceCents"),
        location = getString("location"),
        notes = getString("notes"),
        // "imagePaths" is the v2 key; fall back to "imagePath" when restoring a v1 backup.
        imagePaths = optString("imagePaths", optString("imagePath", "")),
        isArchived = getBoolean("isArchived"),
        createdAtMs = getLong("createdAtMs"),
        updatedAtMs = getLong("updatedAtMs")
    )

    private fun JSONObject.toSpecEntity() = SpecificationEntity(
        id = getString("id"),
        itemId = getString("itemId"),
        key = getString("key"),
        value = getString("value"),
        sortOrder = getInt("sortOrder")
    )

    private fun JSONObject.toWarrantyEntity() = WarrantyReceiptEntity(
        id = getString("id"),
        itemId = getString("itemId"),
        warrantyType = WarrantyType.valueOf(getString("warrantyType")),
        providerName = getString("providerName"),
        startDateMs = getLong("startDateMs"),
        expiryDateMs = getLong("expiryDateMs"),
        receiptImagePath = getString("receiptImagePath"),
        notes = getString("notes"),
        isAlertEnabled = getBoolean("isAlertEnabled"),
        alertDaysBefore = getInt("alertDaysBefore")
    )

    private fun JSONObject.toMaintenanceEntity() = MaintenanceLogEntity(
        id = getString("id"),
        itemId = getString("itemId"),
        description = getString("description"),
        scheduledDateMs = longOrNull("scheduledDateMs"),
        completedDateMs = longOrNull("completedDateMs"),
        costCents = longOrNull("costCents"),
        notes = getString("notes"),
        createdAtMs = getLong("createdAtMs")
    )

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun JSONObject.longOrNull(key: String): Long? =
        if (has(key) && !isNull(key)) getLong(key) else null

    private inline fun <T> JSONArray.mapObjects(transform: JSONObject.() -> T): List<T> =
        (0 until length()).map { getJSONObject(it).transform() }
}

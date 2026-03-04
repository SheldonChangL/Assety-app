package chang.sllj.homeassetkeeper.data.repository

import chang.sllj.homeassetkeeper.data.local.entity.ItemEntity
import chang.sllj.homeassetkeeper.data.local.entity.MaintenanceLogEntity
import chang.sllj.homeassetkeeper.data.local.entity.SpecificationEntity
import chang.sllj.homeassetkeeper.data.local.entity.WarrantyReceiptEntity
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for all item-related data.
 *
 * Flow-returning functions are for UI observation and re-emit on every table change.
 * Suspend functions are one-shot writes or WorkManager batch reads.
 */
interface ItemRepository {

    // ── Item observation ──────────────────────────────────────────────────────

    fun getAllActiveItems(): Flow<List<ItemEntity>>
    fun getArchivedItems(): Flow<List<ItemEntity>>
    fun getItemById(id: String): Flow<ItemEntity?>
    fun searchItems(query: String): Flow<List<ItemEntity>>
    fun getItemsByCategory(category: String): Flow<List<ItemEntity>>
    fun getItemsByLocation(location: String): Flow<List<ItemEntity>>
    fun getAllCategories(): Flow<List<String>>
    fun getAllLocations(): Flow<List<String>>

    // ── Specification observation ─────────────────────────────────────────────

    fun getSpecsForItem(itemId: String): Flow<List<SpecificationEntity>>

    // ── Warranty observation ──────────────────────────────────────────────────

    fun getWarrantiesForItem(itemId: String): Flow<List<WarrantyReceiptEntity>>

    /**
     * All future warranties with alerts enabled, soonest expiry first.
     * [nowMs] is the epoch-ms cutoff; only warranties expiring after this moment
     * are included.
     */
    fun getActiveAlertedWarranties(nowMs: Long): Flow<List<WarrantyReceiptEntity>>

    // ── Maintenance observation ───────────────────────────────────────────────

    fun getLogsForItem(itemId: String): Flow<List<MaintenanceLogEntity>>

    /**
     * Pending tasks (completedDateMs IS NULL) whose scheduled date falls within
     * [fromMs]..[toMs]. Emits on every table change.
     */
    fun getUpcomingMaintenanceFlow(fromMs: Long, toMs: Long): Flow<List<MaintenanceLogEntity>>

    // ── Item writes ───────────────────────────────────────────────────────────

    /**
     * Atomically upsert [item] and replace its specification list.
     * Existing specs for [item.id] are deleted then re-inserted in a single
     * database transaction so the UI never sees a partially-updated state.
     */
    suspend fun saveItemWithSpecs(item: ItemEntity, specs: List<SpecificationEntity>)

    suspend fun archiveItem(id: String, nowMs: Long)
    suspend fun unarchiveItem(id: String, nowMs: Long)

    /**
     * Hard-delete. Cascades to specs, warranties, and maintenance logs via FK rules.
     * Also deletes the item's image file from external files dir if one exists.
     */
    suspend fun deleteItem(item: ItemEntity)

    // ── Warranty writes ───────────────────────────────────────────────────────

    suspend fun upsertWarranty(warranty: WarrantyReceiptEntity)
    suspend fun deleteWarranty(warranty: WarrantyReceiptEntity)

    // ── Maintenance writes ────────────────────────────────────────────────────

    suspend fun upsertMaintenanceLog(log: MaintenanceLogEntity)
    suspend fun deleteMaintenanceLog(log: MaintenanceLogEntity)

    /**
     * One-shot (non-reactive) lookup used by WorkManager to resolve item names.
     * Returns null if the item was deleted before the worker ran.
     */
    suspend fun getItemByIdOnce(id: String): ItemEntity?

    // ── WorkManager one-shot reads ────────────────────────────────────────────

    suspend fun getExpiringWarrantiesInRange(
        fromMs: Long,
        toMs: Long
    ): List<WarrantyReceiptEntity>

    suspend fun getUpcomingMaintenanceTasks(
        fromMs: Long,
        toMs: Long
    ): List<MaintenanceLogEntity>
}

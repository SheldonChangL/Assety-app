package chang.sllj.homeassetkeeper.data.repository

import androidx.room.withTransaction
import chang.sllj.homeassetkeeper.data.local.AppDatabase
import chang.sllj.homeassetkeeper.data.local.dao.ItemDao
import chang.sllj.homeassetkeeper.data.local.dao.MaintenanceLogDao
import chang.sllj.homeassetkeeper.data.local.dao.SpecificationDao
import chang.sllj.homeassetkeeper.data.local.dao.WarrantyReceiptDao
import chang.sllj.homeassetkeeper.data.local.entity.ItemEntity
import chang.sllj.homeassetkeeper.data.local.entity.MaintenanceLogEntity
import chang.sllj.homeassetkeeper.data.local.entity.SpecificationEntity
import chang.sllj.homeassetkeeper.data.local.entity.WarrantyReceiptEntity
import kotlinx.coroutines.flow.Flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val itemDao: ItemDao,
    private val specDao: SpecificationDao,
    private val warrantyDao: WarrantyReceiptDao,
    private val maintenanceDao: MaintenanceLogDao
) : ItemRepository {

    // ── Item observation ──────────────────────────────────────────────────────

    override fun getAllActiveItems(): Flow<List<ItemEntity>> =
        itemDao.getAllActiveItems()

    override fun getArchivedItems(): Flow<List<ItemEntity>> =
        itemDao.getAllArchivedItems()

    override fun getItemById(id: String): Flow<ItemEntity?> =
        itemDao.getItemById(id)

    override fun searchItems(query: String): Flow<List<ItemEntity>> =
        itemDao.searchItems(query)

    override fun getItemsByCategory(category: String): Flow<List<ItemEntity>> =
        itemDao.getItemsByCategory(category)

    override fun getItemsByLocation(location: String): Flow<List<ItemEntity>> =
        itemDao.getItemsByLocation(location)

    override fun getAllCategories(): Flow<List<String>> =
        itemDao.getAllCategories()

    override fun getAllLocations(): Flow<List<String>> =
        itemDao.getAllLocations()

    // ── Specification observation ─────────────────────────────────────────────

    override fun getSpecsForItem(itemId: String): Flow<List<SpecificationEntity>> =
        specDao.getSpecsForItem(itemId)

    // ── Warranty observation ──────────────────────────────────────────────────

    override fun getWarrantiesForItem(itemId: String): Flow<List<WarrantyReceiptEntity>> =
        warrantyDao.getWarrantiesForItem(itemId)

    override fun getActiveAlertedWarranties(nowMs: Long): Flow<List<WarrantyReceiptEntity>> =
        warrantyDao.getActiveAlertedWarranties(nowMs)

    // ── Maintenance observation ───────────────────────────────────────────────

    override fun getLogsForItem(itemId: String): Flow<List<MaintenanceLogEntity>> =
        maintenanceDao.getLogsForItem(itemId)

    override fun getUpcomingMaintenanceFlow(
        fromMs: Long,
        toMs: Long
    ): Flow<List<MaintenanceLogEntity>> =
        maintenanceDao.getUpcomingMaintenanceFlow(fromMs, toMs)

    // ── Item writes ───────────────────────────────────────────────────────────

    override suspend fun saveItemWithSpecs(
        item: ItemEntity,
        specs: List<SpecificationEntity>
    ) {
        // withTransaction wraps the two DAO calls in a single SQLite transaction,
        // ensuring the UI never observes an item with a stale or missing spec list.
        database.withTransaction {
            itemDao.upsertItem(item)
            specDao.deleteSpecsForItem(item.id)
            specDao.upsertSpecs(specs)
        }
    }

    override suspend fun archiveItem(id: String, nowMs: Long) =
        itemDao.archiveItem(id, nowMs)

    override suspend fun unarchiveItem(id: String, nowMs: Long) =
        itemDao.unarchiveItem(id, nowMs)

    override suspend fun deleteItem(item: ItemEntity) {
        // Delete all captured photos from storage before removing the DB row so
        // no orphaned files are left behind if the DB delete succeeds first.
        item.imagePaths
            .split(",")
            .filter { it.isNotBlank() }
            .forEach { path -> runCatching { File(path).delete() } }
        itemDao.deleteItem(item)
    }

    // ── Warranty writes ───────────────────────────────────────────────────────

    override suspend fun upsertWarranty(warranty: WarrantyReceiptEntity) =
        warrantyDao.upsertWarranty(warranty)

    override suspend fun deleteWarranty(warranty: WarrantyReceiptEntity) {
        if (warranty.receiptImagePath.isNotEmpty()) {
            runCatching { File(warranty.receiptImagePath).delete() }
        }
        warrantyDao.deleteWarranty(warranty)
    }

    // ── Maintenance writes ────────────────────────────────────────────────────

    override suspend fun upsertMaintenanceLog(log: MaintenanceLogEntity) =
        maintenanceDao.upsertLog(log)

    override suspend fun deleteMaintenanceLog(log: MaintenanceLogEntity) =
        maintenanceDao.deleteLog(log)

    override suspend fun getItemByIdOnce(id: String): ItemEntity? =
        itemDao.getItemByIdOnce(id)

    // ── WorkManager one-shot reads ────────────────────────────────────────────

    override suspend fun getExpiringWarrantiesInRange(
        fromMs: Long,
        toMs: Long
    ): List<WarrantyReceiptEntity> =
        warrantyDao.getExpiringWarrantiesInRange(fromMs, toMs)

    override suspend fun getUpcomingMaintenanceTasks(
        fromMs: Long,
        toMs: Long
    ): List<MaintenanceLogEntity> =
        maintenanceDao.getUpcomingMaintenance(fromMs, toMs)
}

package chang.sllj.homeassetkeeper.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import chang.sllj.homeassetkeeper.data.local.entity.ItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {

    // ── Observation queries (Flow — emits whenever the underlying table changes) ──

    /** All non-archived items, newest first. */
    @Query("SELECT * FROM items WHERE isArchived = 0 ORDER BY updatedAtMs DESC")
    fun getAllActiveItems(): Flow<List<ItemEntity>>

    /** All archived items, newest first. */
    @Query("SELECT * FROM items WHERE isArchived = 1 ORDER BY updatedAtMs DESC")
    fun getAllArchivedItems(): Flow<List<ItemEntity>>

    /** Single item by primary key; emits null if the item is deleted. */
    @Query("SELECT * FROM items WHERE id = :id")
    fun getItemById(id: String): Flow<ItemEntity?>

    /** Items matching a category, sorted alphabetically. */
    @Query(
        "SELECT * FROM items WHERE category = :category AND isArchived = 0 ORDER BY name ASC"
    )
    fun getItemsByCategory(category: String): Flow<List<ItemEntity>>

    /** Items in a given location, sorted alphabetically. */
    @Query(
        "SELECT * FROM items WHERE location = :location AND isArchived = 0 ORDER BY name ASC"
    )
    fun getItemsByLocation(location: String): Flow<List<ItemEntity>>

    /**
     * Full-text search across name, brand, model number, and serial number.
     * Uses a leading-wildcard LIKE so every substring is matched; for large
     * datasets a FTS4/FTS5 table should replace this in a later migration.
     */
    @Query(
        """
        SELECT * FROM items
        WHERE isArchived = 0
          AND (name        LIKE '%' || :query || '%'
            OR brand       LIKE '%' || :query || '%'
            OR modelNumber LIKE '%' || :query || '%'
            OR serialNumber LIKE '%' || :query || '%')
        ORDER BY name ASC
        """
    )
    fun searchItems(query: String): Flow<List<ItemEntity>>

    /** Distinct category labels in use, ordered alphabetically. */
    @Query("SELECT DISTINCT category FROM items WHERE isArchived = 0 ORDER BY category ASC")
    fun getAllCategories(): Flow<List<String>>

    /** Distinct location labels in use, ordered alphabetically. */
    @Query("SELECT DISTINCT location FROM items WHERE isArchived = 0 ORDER BY location ASC")
    fun getAllLocations(): Flow<List<String>>

    /** Live count of active (non-archived) items. */
    @Query("SELECT COUNT(*) FROM items WHERE isArchived = 0")
    fun getActiveItemCount(): Flow<Int>

    /**
     * One-shot (non-reactive) item lookup for WorkManager. Returns null if the item
     * has been deleted between when the warranty/log record was read and this call.
     */
    @Query("SELECT * FROM items WHERE id = :id LIMIT 1")
    suspend fun getItemByIdOnce(id: String): ItemEntity?

    // ── Write operations (suspend — must be called from a coroutine) ──

    /**
     * Insert or replace. Pass the full updated entity with a refreshed [updatedAtMs]
     * timestamp; Room will overwrite the existing row if [id] already exists.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItem(item: ItemEntity)

    /**
     * Soft-delete: archive the item so it disappears from default views without
     * destroying its warranty and maintenance history.
     */
    @Query("UPDATE items SET isArchived = 1, updatedAtMs = :nowMs WHERE id = :id")
    suspend fun archiveItem(id: String, nowMs: Long)

    /** Restore a previously archived item. */
    @Query("UPDATE items SET isArchived = 0, updatedAtMs = :nowMs WHERE id = :id")
    suspend fun unarchiveItem(id: String, nowMs: Long)

    /**
     * Hard-delete. Cascades to [SpecificationEntity], [WarrantyReceiptEntity],
     * and [MaintenanceLogEntity] via FK CASCADE rules.
     */
    @Delete
    suspend fun deleteItem(item: ItemEntity)

    /** Hard-delete by ID without loading the entity first. */
    @Query("DELETE FROM items WHERE id = :id")
    suspend fun deleteItemById(id: String)
}

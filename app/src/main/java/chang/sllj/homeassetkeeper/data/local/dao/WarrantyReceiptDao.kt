package chang.sllj.homeassetkeeper.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import chang.sllj.homeassetkeeper.data.local.entity.WarrantyReceiptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WarrantyReceiptDao {

    /** All warranties for one item, soonest expiry first. */
    @Query(
        "SELECT * FROM warranty_receipts WHERE itemId = :itemId ORDER BY expiryDateMs ASC"
    )
    fun getWarrantiesForItem(itemId: String): Flow<List<WarrantyReceiptEntity>>

    /**
     * Active (not yet expired) warranties with alerts enabled, soonest expiry first.
     * Used by the Dashboard to surface upcoming expirations.
     */
    @Query(
        """
        SELECT * FROM warranty_receipts
        WHERE isAlertEnabled = 1
          AND expiryDateMs > :nowMs
        ORDER BY expiryDateMs ASC
        """
    )
    fun getActiveAlertedWarranties(nowMs: Long): Flow<List<WarrantyReceiptEntity>>

    /**
     * One-shot query for WorkManager: returns warranties whose expiry falls within
     * [fromMs]..[toMs] and whose alert is enabled.
     *
     * Example: fromMs = now, toMs = now + 30 days → all items expiring within 30 days.
     */
    @Query(
        """
        SELECT * FROM warranty_receipts
        WHERE isAlertEnabled = 1
          AND expiryDateMs BETWEEN :fromMs AND :toMs
        ORDER BY expiryDateMs ASC
        """
    )
    suspend fun getExpiringWarrantiesInRange(
        fromMs: Long,
        toMs: Long
    ): List<WarrantyReceiptEntity>

    /** Insert or update a warranty/receipt record. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWarranty(warranty: WarrantyReceiptEntity)

    /** Remove a single warranty record. */
    @Delete
    suspend fun deleteWarranty(warranty: WarrantyReceiptEntity)

    /** Remove all warranty records for an item (for hard-delete support). */
    @Query("DELETE FROM warranty_receipts WHERE itemId = :itemId")
    suspend fun deleteWarrantiesForItem(itemId: String)
}

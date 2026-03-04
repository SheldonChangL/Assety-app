package chang.sllj.homeassetkeeper.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import chang.sllj.homeassetkeeper.data.local.entity.MaintenanceLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MaintenanceLogDao {

    /**
     * All maintenance logs for one item.
     * Upcoming scheduled tasks appear first (by scheduledDateMs ASC);
     * ad-hoc/historical records are ordered by creation time descending.
     */
    @Query(
        """
        SELECT * FROM maintenance_logs
        WHERE itemId = :itemId
        ORDER BY
            CASE WHEN completedDateMs IS NULL AND scheduledDateMs IS NOT NULL THEN 0 ELSE 1 END ASC,
            scheduledDateMs ASC,
            createdAtMs DESC
        """
    )
    fun getLogsForItem(itemId: String): Flow<List<MaintenanceLogEntity>>

    /**
     * Reactive query for the Dashboard: emits on every table change.
     * Shows pending tasks whose scheduled date is within the given window.
     */
    @Query(
        """
        SELECT * FROM maintenance_logs
        WHERE completedDateMs IS NULL
          AND scheduledDateMs IS NOT NULL
          AND scheduledDateMs BETWEEN :fromMs AND :toMs
        ORDER BY scheduledDateMs ASC
        """
    )
    fun getUpcomingMaintenanceFlow(fromMs: Long, toMs: Long): Flow<List<MaintenanceLogEntity>>

    /**
     * One-shot query for WorkManager: pending (not yet completed) tasks whose
     * scheduled date falls within [fromMs]..[toMs].
     *
     * Example: fromMs = now, toMs = now + 30 days → tasks due within 30 days.
     */
    @Query(
        """
        SELECT * FROM maintenance_logs
        WHERE completedDateMs IS NULL
          AND scheduledDateMs IS NOT NULL
          AND scheduledDateMs BETWEEN :fromMs AND :toMs
        ORDER BY scheduledDateMs ASC
        """
    )
    suspend fun getUpcomingMaintenance(fromMs: Long, toMs: Long): List<MaintenanceLogEntity>

    /** Insert or update a maintenance log entry. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLog(log: MaintenanceLogEntity)

    /** Remove a single maintenance log entry. */
    @Delete
    suspend fun deleteLog(log: MaintenanceLogEntity)

    /** Remove all maintenance logs for an item (for hard-delete support). */
    @Query("DELETE FROM maintenance_logs WHERE itemId = :itemId")
    suspend fun deleteLogsForItem(itemId: String)
}

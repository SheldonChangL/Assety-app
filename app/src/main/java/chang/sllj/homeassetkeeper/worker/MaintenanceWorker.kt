package chang.sllj.homeassetkeeper.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import chang.sllj.homeassetkeeper.data.local.entity.WarrantyType
import chang.sllj.homeassetkeeper.data.repository.ItemRepository
import chang.sllj.homeassetkeeper.domain.util.WarrantyCalculator
import chang.sllj.homeassetkeeper.notification.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * WorkManager background task that fires once per day to scan for upcoming warranty
 * expirations and pending maintenance tasks, then posts local notifications for each.
 *
 * ### Design decisions
 * - Extends [CoroutineWorker] so all DB access runs on the IO dispatcher without
 *   blocking the WorkManager thread pool.
 * - Annotated with [@HiltWorker] so Hilt injects [ItemRepository] and
 *   [NotificationHelper] through the [HiltWorkerFactory] configured in
 *   [HomeAssetKeeperApplication].
 * - Uses [AlarmManager] is deliberately NOT used; WorkManager's
 *   [PeriodicWorkRequest] provides the daily scheduling, fully compliant with
 *   Android 14+ background execution limits.
 * - Notification IDs are derived from the record UUID's [String.hashCode]; repeated
 *   runs update existing notifications rather than stacking duplicates.
 *
 * ### Look-ahead window
 * The worker queries both tables for records falling within **[LOOKAHEAD_DAYS] days**
 * from now. Each warranty also carries an [WarrantyReceiptEntity.alertDaysBefore]
 * field; the worker honours that per-record threshold so a user who set "alert 7 days
 * before" will not be notified until the warranty is ≤ 7 days away.
 */
@HiltWorker
class MaintenanceWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: ItemRepository,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, params) {

    companion object {
        /** Maximum look-ahead window: notify for anything due within 30 days. */
        const val LOOKAHEAD_DAYS = 30L
        /** WorkManager unique task name, used for deduplication. */
        const val WORK_NAME = "daily_expiry_check"
    }

    override suspend fun doWork(): Result {
        val nowMs = System.currentTimeMillis()
        val windowEndMs = nowMs + TimeUnit.DAYS.toMillis(LOOKAHEAD_DAYS)

        checkWarrantyExpiries(nowMs, windowEndMs)
        checkMaintenanceDue(nowMs, windowEndMs)

        return Result.success()
    }

    // ── Warranty expiry notifications ─────────────────────────────────────────

    /**
     * Queries all alert-enabled warranties expiring within the window.
     * Respects [WarrantyReceiptEntity.alertDaysBefore]: skips a warranty if the
     * remaining days exceed the per-record threshold.
     */
    private suspend fun checkWarrantyExpiries(nowMs: Long, windowEndMs: Long) {
        val warranties = repository.getExpiringWarrantiesInRange(nowMs, windowEndMs)

        for (warranty in warranties) {
            val daysLeft = WarrantyCalculator.daysUntilExpiry(warranty.expiryDateMs, nowMs)
            // Honour the per-warranty alert threshold.
            if (daysLeft > warranty.alertDaysBefore) continue

            val item = repository.getItemByIdOnce(warranty.itemId) ?: continue
            val typeLabel = warranty.warrantyType.toDisplayLabel()

            notificationHelper.postWarrantyExpiryNotification(
                warrantyId = warranty.id,
                itemName = item.name,
                warrantyType = typeLabel,
                daysLeft = daysLeft
            )
        }
    }

    // ── Maintenance-due notifications ─────────────────────────────────────────

    /**
     * Queries all pending (not yet completed) maintenance tasks scheduled within
     * the window and posts a notification for each.
     */
    private suspend fun checkMaintenanceDue(nowMs: Long, windowEndMs: Long) {
        val logs = repository.getUpcomingMaintenanceTasks(nowMs, windowEndMs)

        for (log in logs) {
            val scheduledMs = log.scheduledDateMs ?: continue
            val daysLeft = WarrantyCalculator.daysUntilExpiry(scheduledMs, nowMs)

            val item = repository.getItemByIdOnce(log.itemId) ?: continue

            notificationHelper.postMaintenanceDueNotification(
                logId = log.id,
                itemName = item.name,
                description = log.description,
                daysLeft = daysLeft
            )
        }
    }
}

// ── Extension ────────────────────────────────────────────────────────────────

private fun WarrantyType.toDisplayLabel(): String = when (this) {
    WarrantyType.MANUFACTURER -> "Manufacturer"
    WarrantyType.EXTENDED -> "Extended"
    WarrantyType.STORE_PROTECTION -> "Store Protection"
}

package chang.sllj.homeassetkeeper.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules the daily [MaintenanceWorker] using WorkManager's [PeriodicWorkRequest].
 *
 * ### Scheduling strategy
 * [ExistingPeriodicWorkPolicy.KEEP] is used so that a WorkManager chain queued on the
 * previous app launch is not cancelled and re-queued on every subsequent launch. The
 * existing chain keeps its place in the WorkManager queue and its accumulated flex period.
 *
 * ### Constraints
 * The worker requires [NetworkType.NOT_REQUIRED] (no network) since all data comes from
 * the local encrypted Room database. Battery not-low is requested as a soft hint to
 * the OS scheduler — the worker is lightweight enough that even if the OS ignores the
 * hint, it will complete quickly without draining the battery.
 *
 * ### No AlarmManager
 * Exact alarms ([AlarmManager.setExact] / [AlarmManager.setExactAndAllowWhileIdle])
 * are deliberately avoided to comply with Android 14+ restrictions on
 * SCHEDULE_EXACT_ALARM. WorkManager's internal scheduler handles wake-locks and
 * Doze-mode appropriately.
 *
 * ### Flex interval
 * A flex window of [FLEX_INTERVAL_HOURS] hours within the 24-hour period allows
 * WorkManager to batch the job with other periodic work and run it at the most
 * battery-efficient time of day.
 */
@Singleton
class WorkManagerScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private companion object {
        const val REPEAT_INTERVAL_HOURS = 24L
        const val FLEX_INTERVAL_HOURS = 4L
    }

    /**
     * Enqueues the daily [MaintenanceWorker] if it is not already scheduled.
     *
     * Calling this multiple times (e.g. on every [Application.onCreate]) is safe
     * because [ExistingPeriodicWorkPolicy.KEEP] prevents duplicate registrations.
     */
    fun scheduleDailyCheck() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val request = PeriodicWorkRequestBuilder<MaintenanceWorker>(
            repeatInterval = REPEAT_INTERVAL_HOURS,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
            flexTimeInterval = FLEX_INTERVAL_HOURS,
            flexTimeIntervalUnit = TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            MaintenanceWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}

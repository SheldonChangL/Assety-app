package chang.sllj.homeassetkeeper

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import chang.sllj.homeassetkeeper.notification.NotificationHelper
import chang.sllj.homeassetkeeper.worker.WorkManagerScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application entry point.
 *
 * Implements [Configuration.Provider] so that WorkManager is initialised manually with
 * Hilt's [HiltWorkerFactory]. This is required when using `hilt-work` — without it,
 * WorkManager's App Startup initializer self-initialises (without the HiltWorkerFactory)
 * and can independently register constraint-tracking receivers, causing
 * RECEIVER_NOT_EXPORTED SecurityExceptions on Android 13+.
 *
 * The corresponding App Startup removal for WorkManagerInitializer lives in
 * AndroidManifest.xml (InitializationProvider / tools:node="remove").
 */
@HiltAndroidApp
class HomeAssetKeeperApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var workManagerScheduler: WorkManagerScheduler

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Load the SQLCipher native library before any database access.
        // net.zetetic:sqlcipher-android does not auto-load its .so; this must be
        // called explicitly, equivalent to the deprecated SQLiteDatabase.loadLibs().
        System.loadLibrary("sqlcipher")
        // Register notification channels (idempotent; safe to call on every launch).
        notificationHelper.createNotificationChannels()
        // Enqueue the daily background check for expiring warranties and maintenance.
        workManagerScheduler.scheduleDailyCheck()
    }
}

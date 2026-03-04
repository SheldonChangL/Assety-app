package chang.sllj.homeassetkeeper.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import chang.sllj.homeassetkeeper.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages notification channels and dispatches local notifications for warranty
 * expiry and scheduled maintenance events.
 *
 * Two high-priority channels are registered:
 *  - [CHANNEL_WARRANTY_EXPIRY] – fires when a warranty is about to expire.
 *  - [CHANNEL_MAINTENANCE_DUE] – fires when a scheduled maintenance task is approaching.
 *
 * [createNotificationChannels] is idempotent: calling it multiple times (e.g. on every
 * [Application.onCreate]) is harmless — the OS no-ops if the channels already exist.
 *
 * Notification IDs are derived from the record UUID's [String.hashCode] so that
 * repeated WorkManager runs update existing notifications instead of stacking new ones.
 */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        const val CHANNEL_WARRANTY_EXPIRY = "channel_warranty_expiry"
        const val CHANNEL_MAINTENANCE_DUE = "channel_maintenance_due"
    }

    /**
     * Creates (or no-ops if already present) the two notification channels.
     * Must be called before any [postWarrantyExpiryNotification] or
     * [postMaintenanceDueNotification] call.
     */
    fun createNotificationChannels() {
        val manager = context.getSystemService(NotificationManager::class.java)

        val warrantyChannel = NotificationChannel(
            CHANNEL_WARRANTY_EXPIRY,
            "Warranty Expiry",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts when a warranty is about to expire"
            enableVibration(true)
        }

        val maintenanceChannel = NotificationChannel(
            CHANNEL_MAINTENANCE_DUE,
            "Maintenance Due",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts when a scheduled maintenance task is approaching"
            enableVibration(true)
        }

        manager.createNotificationChannels(listOf(warrantyChannel, maintenanceChannel))
    }

    /**
     * Posts (or updates) a "Warranty Expiring" notification.
     *
     * @param warrantyId  UUID of the [WarrantyReceiptEntity]; used as stable notification ID.
     * @param itemName    Display name of the associated asset.
     * @param warrantyType Human-readable type label (e.g. "Manufacturer", "Extended").
     * @param daysLeft    Whole days until expiry; 0 = expires today, negative = already expired.
     */
    fun postWarrantyExpiryNotification(
        warrantyId: String,
        itemName: String,
        warrantyType: String,
        daysLeft: Long
    ) {
        if (!areNotificationsPermitted()) return

        val body = when {
            daysLeft <= 0 -> "$itemName – $warrantyType warranty has expired"
            daysLeft == 1L -> "$itemName – $warrantyType warranty expires tomorrow"
            else -> "$itemName – $warrantyType warranty expires in $daysLeft days"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_WARRANTY_EXPIRY)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Warranty Expiring Soon")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(warrantyId.hashCode(), notification)
    }

    /**
     * Posts (or updates) a "Maintenance Due" notification.
     *
     * @param logId       UUID of the [MaintenanceLogEntity]; used as stable notification ID.
     * @param itemName    Display name of the associated asset.
     * @param description Task description from the maintenance log.
     * @param daysLeft    Whole days until the scheduled date; 0 = due today, negative = overdue.
     */
    fun postMaintenanceDueNotification(
        logId: String,
        itemName: String,
        description: String,
        daysLeft: Long
    ) {
        if (!areNotificationsPermitted()) return

        val body = when {
            daysLeft <= 0 -> "$itemName – \"$description\" maintenance is due today"
            daysLeft == 1L -> "$itemName – \"$description\" maintenance is due tomorrow"
            else -> "$itemName – \"$description\" maintenance is due in $daysLeft days"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_MAINTENANCE_DUE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Maintenance Due")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(logId.hashCode(), notification)
    }

    /**
     * Returns true if the app has permission to post notifications.
     *
     * On Android 13+ (API 33+) the POST_NOTIFICATIONS runtime permission is required.
     * On earlier API levels, [NotificationManagerCompat.areNotificationsEnabled] is
     * sufficient (it reflects whether the user toggled notifications off in Settings).
     */
    private fun areNotificationsPermitted(): Boolean {
        val notificationManager = NotificationManagerCompat.from(context)
        if (!notificationManager.areNotificationsEnabled()) return false
        // Android 13+ requires the POST_NOTIFICATIONS runtime permission.
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }
}

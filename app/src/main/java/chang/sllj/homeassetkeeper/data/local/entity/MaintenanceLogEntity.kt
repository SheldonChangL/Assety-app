package chang.sllj.homeassetkeeper.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A maintenance or service record linked to one [ItemEntity].
 *
 * Covers both scheduled future tasks ([scheduledDateMs] non-null, [completedDateMs] null)
 * and historical service records ([completedDateMs] non-null).
 *
 * [costCents] is stored in cents to avoid floating-point errors; null means not recorded.
 *
 * WorkManager queries [scheduledDateMs] for upcoming-maintenance notifications.
 */
@Entity(
    tableName = "maintenance_logs",
    foreignKeys = [
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["itemId"]),
        Index(value = ["scheduledDateMs"])
    ]
)
data class MaintenanceLogEntity(
    @PrimaryKey val id: String,         // UUID string
    val itemId: String,                 // FK → items.id
    val description: String,
    val scheduledDateMs: Long?,         // null for ad-hoc/historical logs
    val completedDateMs: Long?,         // null if the task is still pending
    val costCents: Long?,               // null if cost not recorded
    val notes: String,
    val createdAtMs: Long
)

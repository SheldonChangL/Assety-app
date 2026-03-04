package chang.sllj.homeassetkeeper.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a single home asset (appliance, electronics, furniture, etc.).
 *
 * Monetary values are stored as Long cents (e.g. $12.99 → 1299L) to avoid
 * floating-point rounding errors. A null value means "not recorded".
 *
 * Optional strings use "" rather than null to simplify Compose state handling.
 *
 * [imagePaths] stores up to three absolute JPEG paths (comma-separated) inside
 * [android.content.Context.getExternalFilesDir]; "" when no images are attached.
 *
 * Indices on [category] and [location] accelerate the common filter queries.
 */
@Entity(
    tableName = "items",
    indices = [
        Index(value = ["category"]),
        Index(value = ["location"]),
        Index(value = ["isArchived"])
    ]
)
data class ItemEntity(
    @PrimaryKey val id: String,           // UUID string
    val name: String,
    val category: String,                 // e.g. "Appliances", "Electronics"
    val brand: String,                    // "" if unknown
    val modelNumber: String,              // "" if unknown
    val serialNumber: String,             // "" if unknown
    val purchaseDateMs: Long?,            // Unix epoch ms; null if not recorded
    val purchasePriceCents: Long?,        // price in cents; null if not recorded
    val location: String,                 // room or area (e.g. "Kitchen")
    val notes: String,
    val imagePaths: String,               // comma-separated absolute paths; "" if none
    val isArchived: Boolean,              // soft-delete flag
    val createdAtMs: Long,
    val updatedAtMs: Long
)

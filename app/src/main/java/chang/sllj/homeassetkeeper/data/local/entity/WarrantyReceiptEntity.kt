package chang.sllj.homeassetkeeper.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Classifies the origin of a warranty.
 */
enum class WarrantyType {
    MANUFACTURER,       // factory warranty from the brand
    EXTENDED,           // purchased extended warranty plan
    STORE_PROTECTION    // store protection or credit-card coverage
}

/**
 * A warranty or receipt record linked to one [ItemEntity].
 *
 * A single item can have multiple warranties (e.g. manufacturer + extended).
 * [receiptImagePath] stores the path to a CameraX-captured receipt image inside
 * getExternalFilesDir(); it is "" if no image was captured.
 *
 * WorkManager uses [expiryDateMs] + [isAlertEnabled] + [alertDaysBefore] to
 * determine when to post a local notification.
 *
 * Indices on [expiryDateMs] accelerate the range query in WarrantyReceiptDao.
 */
@Entity(
    tableName = "warranty_receipts",
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
        Index(value = ["expiryDateMs"])
    ]
)
data class WarrantyReceiptEntity(
    @PrimaryKey val id: String,         // UUID string
    val itemId: String,                 // FK → items.id
    val warrantyType: WarrantyType,
    val providerName: String,           // brand or store name; "" if unknown
    val startDateMs: Long,              // Unix epoch ms
    val expiryDateMs: Long,             // Unix epoch ms
    val receiptImagePath: String,       // absolute path; "" if no image
    val notes: String,
    val isAlertEnabled: Boolean,        // whether WorkManager should notify
    val alertDaysBefore: Int            // post notification when expiry is within N days
)

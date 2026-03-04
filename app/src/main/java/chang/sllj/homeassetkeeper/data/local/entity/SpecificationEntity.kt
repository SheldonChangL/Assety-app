package chang.sllj.homeassetkeeper.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A key-value specification attached to a single [ItemEntity].
 *
 * Examples: "Wattage" → "1500W", "Color" → "Stainless Steel", "Dimensions" → "30×20×15 cm".
 *
 * [sortOrder] controls display order in the UI; lower numbers appear first.
 * CASCADE delete keeps the table clean when a parent item is removed.
 */
@Entity(
    tableName = "specifications",
    foreignKeys = [
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["itemId"])]
)
data class SpecificationEntity(
    @PrimaryKey val id: String,   // UUID string
    val itemId: String,           // FK → items.id
    val key: String,
    val value: String,
    val sortOrder: Int            // ascending display order
)

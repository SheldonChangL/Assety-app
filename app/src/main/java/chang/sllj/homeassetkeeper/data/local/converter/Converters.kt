package chang.sllj.homeassetkeeper.data.local.converter

import androidx.room.TypeConverter
import chang.sllj.homeassetkeeper.data.local.entity.WarrantyType

/**
 * Room TypeConverters for types that SQLite cannot store natively.
 *
 * [WarrantyType] is persisted as its string name rather than its ordinal index,
 * making the stored data resilient to enum reordering in future schema migrations.
 */
class Converters {

    @TypeConverter
    fun warrantyTypeToString(type: WarrantyType): String = type.name

    @TypeConverter
    fun stringToWarrantyType(value: String): WarrantyType =
        WarrantyType.valueOf(value)
}

package chang.sllj.homeassetkeeper.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RenameColumn
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import chang.sllj.homeassetkeeper.data.local.converter.Converters
import chang.sllj.homeassetkeeper.data.local.dao.ItemDao
import chang.sllj.homeassetkeeper.data.local.dao.MaintenanceLogDao
import chang.sllj.homeassetkeeper.data.local.dao.SpecificationDao
import chang.sllj.homeassetkeeper.data.local.dao.WarrantyReceiptDao
import chang.sllj.homeassetkeeper.data.local.entity.ItemEntity
import chang.sllj.homeassetkeeper.data.local.entity.MaintenanceLogEntity
import chang.sllj.homeassetkeeper.data.local.entity.SpecificationEntity
import chang.sllj.homeassetkeeper.data.local.entity.WarrantyReceiptEntity

/**
 * Room database declaration.
 *
 * The physical SQLite file is encrypted by SQLCipher; the [SupportFactory] is
 * injected in [DatabaseModule] — this class only declares the schema.
 *
 * ## Schema version history
 * - v1: Initial schema.
 * - v2: [AutoMigration] renames `items.imagePath` → `items.imagePaths` to support
 *       up to three comma-separated image paths per asset.
 *
 * [exportSchema] = true: KSP writes a schema JSON to app/schemas/ on every build,
 * providing a migration audit trail committed to VCS.
 */
@Database(
    entities = [
        ItemEntity::class,
        SpecificationEntity::class,
        WarrantyReceiptEntity::class,
        MaintenanceLogEntity::class
    ],
    version = 2,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2, spec = AppDatabase.Migration1To2::class)
    ]
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun itemDao(): ItemDao
    abstract fun specificationDao(): SpecificationDao
    abstract fun warrantyReceiptDao(): WarrantyReceiptDao
    abstract fun maintenanceLogDao(): MaintenanceLogDao

    /** Renames the single-image `imagePath` column to the multi-image `imagePaths` column. */
    @RenameColumn(tableName = "items", fromColumnName = "imagePath", toColumnName = "imagePaths")
    class Migration1To2 : AutoMigrationSpec

    companion object {
        const val DATABASE_NAME = "home_asset_keeper.db"
    }
}

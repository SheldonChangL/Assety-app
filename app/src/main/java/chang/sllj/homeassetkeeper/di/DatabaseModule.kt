package chang.sllj.homeassetkeeper.di

import android.content.Context
import androidx.room.Room
import chang.sllj.homeassetkeeper.data.local.AppDatabase
import chang.sllj.homeassetkeeper.data.local.dao.ItemDao
import chang.sllj.homeassetkeeper.data.local.dao.MaintenanceLogDao
import chang.sllj.homeassetkeeper.data.local.dao.SpecificationDao
import chang.sllj.homeassetkeeper.data.local.dao.WarrantyReceiptDao
import chang.sllj.homeassetkeeper.data.security.DatabaseKeyManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.util.Arrays
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Provides the single [AppDatabase] instance for the application lifetime.
     *
     * ## Encryption flow
     * 1. [DatabaseKeyManager.getOrCreatePassphrase] returns the AES-GCM–decrypted
     *    passphrase from the Android Keystore (generating one on first launch).
     * 2. [SupportFactory] wraps the passphrase and passes it to SQLCipher's native
     *    layer when the database file is opened. SQLCipher internally copies the
     *    passphrase, so we zero our copy immediately after.
     * 3. The `finally` block guarantees zeroing even if Room.build() throws.
     *
     * ## Corrupt-database handling
     * If [DatabaseKeyManager] generates a new passphrase because the Keystore key
     * was lost, Room will fail to open the old (now-undecryptable) file. The
     * `fallbackToDestructiveMigration()` call makes Room delete and recreate the
     * database in that scenario. All user data is lost, but the app remains
     * functional — this mirrors Android's own "Forgot PIN" behavior.
     *
     * In a future version this could be replaced with an explicit user-facing
     * warning and a manual "Reset database" option.
     */
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        keyManager: DatabaseKeyManager
    ): AppDatabase {
        val passphrase: ByteArray = keyManager.getOrCreatePassphrase()
        return try {
            val factory = SupportOpenHelperFactory(passphrase)
            Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                AppDatabase.DATABASE_NAME
            )
                .openHelperFactory(factory)
                // Only used when the Keystore key is lost and a new passphrase is
                // generated. In normal operation (schema migration) supply a proper
                // Migration object instead.
                .fallbackToDestructiveMigration()
                .build()
        } finally {
            // Zero the plaintext passphrase; SQLCipher already copied it internally.
            Arrays.fill(passphrase, 0.toByte())
        }
    }

    // ── DAO providers ─────────────────────────────────────────────────────────
    // DAOs are stateless; Room returns the same underlying implementation each
    // time, so @Singleton scoping here avoids redundant Hilt allocations.

    @Provides
    @Singleton
    fun provideItemDao(db: AppDatabase): ItemDao = db.itemDao()

    @Provides
    @Singleton
    fun provideSpecificationDao(db: AppDatabase): SpecificationDao = db.specificationDao()

    @Provides
    @Singleton
    fun provideWarrantyReceiptDao(db: AppDatabase): WarrantyReceiptDao =
        db.warrantyReceiptDao()

    @Provides
    @Singleton
    fun provideMaintenanceLogDao(db: AppDatabase): MaintenanceLogDao =
        db.maintenanceLogDao()
}

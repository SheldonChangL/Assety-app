package chang.sllj.homeassetkeeper.data.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the SQLCipher database passphrase using the Android Keystore system.
 *
 * ## Security design
 * 1. On first launch, [PASSPHRASE_BYTE_LENGTH] cryptographically random bytes are generated.
 * 2. An AES-256-GCM key is generated inside the Android Keystore (hardware-backed where
 *    available). This key never leaves the secure element.
 * 3. The random passphrase is encrypted with that Keystore key and stored as Base64 in
 *    [SharedPreferences]. The IV is stored alongside it.
 * 4. On subsequent launches the ciphertext is decrypted on-device by the same Keystore key.
 *
 * ## Key-loss resilience
 * If the Keystore entry is destroyed (e.g. factory reset, biometric re-enrollment on
 * devices that tie keys to biometrics), decryption will fail. The app responds by generating
 * a fresh passphrase and overwriting the stale ciphertext. The existing encrypted database
 * becomes unreadable; [DatabaseModule] detects the resulting open failure and deletes the
 * corrupt file so the user can start fresh.
 *
 * ## Thread safety
 * All Keystore and SharedPreferences operations are synchronous. Call
 * [getOrCreatePassphrase] only from a background thread (e.g. inside a Hilt
 * @Singleton provider, which Room initialises lazily on first access).
 */
@Singleton
class DatabaseKeyManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "HomeAssetKeeperDbMasterKey"
        private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val KEY_SIZE_BITS = 256
        private const val PASSPHRASE_BYTE_LENGTH = 32

        private const val PREFS_NAME = "hak_secure_prefs"
        private const val PREF_ENC_PASSPHRASE = "enc_passphrase_b64"
        private const val PREF_ENC_IV = "enc_iv_b64"
    }

    private val keyStore: KeyStore =
        KeyStore.getInstance(KEYSTORE_PROVIDER).also { it.load(null) }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Returns the plaintext database passphrase as a [ByteArray].
     *
     * **The caller is responsible for zeroing the returned array** (via
     * `java.util.Arrays.fill(passphrase, 0)`) as soon as the database has been
     * opened. SQLCipher copies the passphrase internally; holding it in the heap
     * afterwards is an unnecessary security risk.
     */
    fun getOrCreatePassphrase(): ByteArray {
        val encB64 = prefs.getString(PREF_ENC_PASSPHRASE, null)
        val ivB64 = prefs.getString(PREF_ENC_IV, null)

        if (encB64 != null && ivB64 != null) {
            return try {
                decryptPassphrase(
                    cipherText = Base64.decode(encB64, Base64.DEFAULT),
                    iv = Base64.decode(ivB64, Base64.DEFAULT)
                )
            } catch (_: Exception) {
                // Keystore key is gone (device reset, biometric change, etc.).
                // The encrypted database is permanently unreadable; generate a
                // fresh passphrase and let DatabaseModule handle DB file cleanup.
                generateAndPersist()
            }
        }

        return generateAndPersist()
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun generateAndPersist(): ByteArray {
        // Remove any stale Keystore entry before creating a new one.
        if (keyStore.containsAlias(KEY_ALIAS)) {
            keyStore.deleteEntry(KEY_ALIAS)
        }

        val passphrase = ByteArray(PASSPHRASE_BYTE_LENGTH)
            .also { SecureRandom().nextBytes(it) }

        val (cipherText, iv) = encryptPassphrase(passphrase)

        // commit() is intentionally synchronous: the passphrase must be durably
        // persisted before the database open attempt that follows.
        prefs.edit()
            .putString(PREF_ENC_PASSPHRASE, Base64.encodeToString(cipherText, Base64.DEFAULT))
            .putString(PREF_ENC_IV, Base64.encodeToString(iv, Base64.DEFAULT))
            .commit()

        return passphrase
    }

    private fun getOrCreateKeystoreKey(): SecretKey {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE_BITS)
                .build()

            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
                .also { it.init(spec) }
                .generateKey()
        }

        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }

    private fun encryptPassphrase(plaintext: ByteArray): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKeystoreKey())
        val cipherText = cipher.doFinal(plaintext)
        return cipherText to cipher.iv
    }

    private fun decryptPassphrase(cipherText: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateKeystoreKey(),
            GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        )
        return cipher.doFinal(cipherText)
    }
}

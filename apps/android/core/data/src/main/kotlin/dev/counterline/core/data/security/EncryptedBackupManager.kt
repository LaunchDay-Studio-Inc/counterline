package dev.counterline.core.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides AES-256-GCM encryption backed by the Android Keystore for secure
 * export/import of user progress data.
 *
 * File format (v1):
 *   [1 byte]  version = 0x01
 *   [4 bytes] IV length (big-endian int)
 *   [N bytes] IV
 *   [rest]    AES-GCM ciphertext (includes 128-bit auth tag)
 */
@Singleton
class EncryptedBackupManager @Inject constructor() {

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "counterline_backup_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val FORMAT_VERSION: Byte = 0x01
        private const val MAX_IMPORT_SIZE = 50 * 1024 * 1024 // 50 MB limit
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        keyStore.getKey(KEY_ALIAS, null)?.let { return it as SecretKey }

        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
            .apply { init(spec) }
            .generateKey()
    }

    /**
     * Encrypts [plainData] and writes the encrypted backup to [output].
     */
    fun encrypt(plainData: ByteArray, output: OutputStream) {
        val key = getOrCreateKey()
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, key)
        }
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plainData)

        output.write(FORMAT_VERSION.toInt())
        output.write(iv.size.toBigEndianBytes())
        output.write(iv)
        output.write(ciphertext)
        output.flush()
    }

    /**
     * Reads an encrypted backup from [input] and returns the decrypted data.
     *
     * @throws InvalidBackupException if the format is invalid or tampered with.
     */
    fun decrypt(input: InputStream): ByteArray {
        val version = input.read()
        if (version != FORMAT_VERSION.toInt()) {
            throw InvalidBackupException("Unsupported backup format version: $version")
        }

        val ivLenBytes = ByteArray(4)
        if (input.read(ivLenBytes) != 4) {
            throw InvalidBackupException("Truncated backup: missing IV length")
        }
        val ivLen = ivLenBytes.fromBigEndianInt()
        if (ivLen < 12 || ivLen > 16) {
            throw InvalidBackupException("Invalid IV length: $ivLen")
        }

        val iv = ByteArray(ivLen)
        if (input.read(iv) != ivLen) {
            throw InvalidBackupException("Truncated backup: incomplete IV")
        }

        val ciphertext = input.readBytes()
        if (ciphertext.size > MAX_IMPORT_SIZE) {
            throw InvalidBackupException("Backup exceeds maximum size of ${MAX_IMPORT_SIZE / 1024 / 1024} MB")
        }

        val key = getOrCreateKey()
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        }

        return try {
            cipher.doFinal(ciphertext)
        } catch (e: Exception) {
            throw InvalidBackupException("Decryption failed — backup may be corrupted or tampered with", e)
        }
    }

    private fun Int.toBigEndianBytes(): ByteArray = byteArrayOf(
        (this shr 24).toByte(),
        (this shr 16).toByte(),
        (this shr 8).toByte(),
        this.toByte(),
    )

    private fun ByteArray.fromBigEndianInt(): Int =
        (this[0].toInt() and 0xFF shl 24) or
            (this[1].toInt() and 0xFF shl 16) or
            (this[2].toInt() and 0xFF shl 8) or
            (this[3].toInt() and 0xFF)
}

class InvalidBackupException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

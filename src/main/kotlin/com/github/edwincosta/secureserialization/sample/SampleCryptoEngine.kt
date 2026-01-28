package com.github.edwincosta.secureserialization.sample

import com.github.edwincosta.secureserialization.secure.SecureCryptoEngine
import java.security.Key
import java.util.Base64

/**
 * Sample implementation of [SecureCryptoEngine] for demonstration and testing purposes.
 *
 * This class provides a **fake** encryption/decryption implementation that is **NOT secure**
 * and should **NEVER be used in production**. It simply Base64-encodes the plaintext instead
 * of performing real encryption.
 *
 * ## Security Warning
 * ⚠️ **DO NOT USE IN PRODUCTION!** ⚠️
 *
 * This implementation:
 * - Does NOT provide any real encryption
 * - Only performs Base64 encoding (which is easily reversible)
 * - Uses a fixed, fake IV that provides no security
 * - Ignores the encryption key parameter completely
 * - Does not protect data confidentiality or integrity
 *
 * ## Purpose
 * This class is intended solely for:
 * - Unit testing the serialization framework
 * - Integration testing without real cryptography overhead
 * - Proof of concept demonstrations
 * - Understanding the encryption workflow
 * - Development and learning purposes
 *
 * ## Production Implementation Example
 * ```kotlin
 * class AesGcmCryptoEngine : SecureCryptoEngine {
 *     override fun generateIv(): String {
 *         val iv = ByteArray(12) // GCM standard IV size
 *         SecureRandom().nextBytes(iv)
 *         return Base64.getEncoder().encodeToString(iv)
 *     }
 *
 *     override fun encrypt(plainText: String, key: Key, iv: String): String {
 *         val cipher = Cipher.getInstance("AES/GCM/NoPadding")
 *         val ivBytes = Base64.getDecoder().decode(iv)
 *         cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, ivBytes))
 *         val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
 *         return Base64.getEncoder().encodeToString(encrypted)
 *     }
 *
 *     override fun decrypt(cipherText: String, key: Key, iv: String): String {
 *         val cipher = Cipher.getInstance("AES/GCM/NoPadding")
 *         val ivBytes = Base64.getDecoder().decode(iv)
 *         cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, ivBytes))
 *         val decrypted = cipher.doFinal(Base64.getDecoder().decode(cipherText))
 *         return String(decrypted, Charsets.UTF_8)
 *     }
 * }
 * ```
 *
 * ## Recommended Production Algorithms
 * - **AES-256-GCM**: Authenticated encryption (AEAD) - recommended
 * - **AES-256-CBC** with HMAC: For compatibility with older systems
 * - **ChaCha20-Poly1305**: Modern AEAD cipher, good for mobile devices
 *
 * @see SecureCryptoEngine
 * @see SampleKeyProvider
 */
class SampleCryptoEngine : SecureCryptoEngine {

    /**
     * Generates a fake initialization vector (IV) for demonstration purposes.
     *
     * **WARNING:** This returns a fixed string "sample-iv" instead of a cryptographically
     * secure random value. Real implementations must generate unique, random IVs for each
     * encryption operation.
     *
     * @return A hardcoded string "sample-iv" (NOT secure).
     */
    override fun generateIv(): String {
        // Just a fake IV for demonstration purposes
        return "sample-iv"
    }

    /**
     * Performs fake "encryption" by Base64-encoding the plaintext.
     *
     * **WARNING:** This is NOT real encryption! It only encodes the text, which can be
     * easily decoded by anyone. The `key` and `iv` parameters are completely ignored.
     *
     * @param plainText The text to "encrypt" (actually just Base64-encode).
     * @param key The encryption key (ignored in this fake implementation).
     * @param iv The initialization vector (ignored in this fake implementation).
     * @return Base64-encoded plaintext (NOT actually encrypted).
     */
    override fun encrypt(plainText: String, key: Key, iv: String): String {
        // NOT real encryption. Only for sample/demo.
        return Base64.getEncoder().encodeToString(plainText.toByteArray())
    }

    /**
     * Performs fake "decryption" by Base64-decoding the ciphertext.
     *
     * **WARNING:** This is NOT real decryption! It only decodes Base64-encoded text.
     * The `key` and `iv` parameters are completely ignored.
     *
     * @param cipherText The text to "decrypt" (actually just Base64-decode).
     * @param key The decryption key (ignored in this fake implementation).
     * @param iv The initialization vector (ignored in this fake implementation).
     * @return Base64-decoded text (NOT actually decrypted).
     */
    override fun decrypt(cipherText: String, key: Key, iv: String): String {
        // NOT real decryption. Only for sample/demo.
        return String(Base64.getDecoder().decode(cipherText))
    }
}
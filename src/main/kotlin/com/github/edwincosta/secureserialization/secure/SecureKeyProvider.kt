package com.github.edwincosta.secureserialization.secure

import java.security.Key

/**
 * Interface for providing encryption/decryption keys to the [SecureSerializer].
 *
 * Implementations of this interface are responsible for managing and providing
 * cryptographic keys used for encrypting and decrypting [SecureProperty] annotated fields.
 * The key provider abstracts the key management logic from the serialization process,
 * allowing for flexible key storage and rotation strategies.
 *
 * ## Implementation Considerations
 * - Return `null` from [currentKey] if encryption/decryption should be skipped
 * - Ensure the key is appropriate for the encryption algorithm used by [SecureCryptoEngine]
 * - Consider implementing key rotation and versioning for production use
 * - Store keys securely (e.g., using Android Keystore, Java KeyStore, or secure vaults)
 *
 * ## Usage Example
 * ```kotlin
 * class MyKeyProvider : SecureKeyProvider {
 *     override fun currentKey(): Key? {
 *         // Retrieve key from secure storage
 *         return keyStore.getKey("myKeyAlias", password)
 *     }
 * }
 *
 * object MySerializer : SecureSerializer<MyData>(
 *     dataClass = MyData::class,
 *     keyProvider = MyKeyProvider(),
 *     cryptoEngine = MyCryptoEngine()
 * )
 * ```
 *
 * @see SecureSerializer
 * @see SecureCryptoEngine
 */
interface SecureKeyProvider {
    /**
     * Returns the current encryption/decryption key.
     *
     * @return The cryptographic key to use for encryption and decryption operations,
     *         or `null` if encryption/decryption should be skipped.
     */
    fun currentKey(): Key?
}
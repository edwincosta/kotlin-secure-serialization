package com.github.edwincosta.secureserialization.sample

import com.github.edwincosta.secureserialization.secure.SecureKeyProvider
import java.security.Key
import javax.crypto.spec.SecretKeySpec

/**
 * Sample implementation of [SecureKeyProvider] for demonstration and testing purposes.
 *
 * This class provides a hardcoded, fake encryption key that is **NOT secure** and should
 * **NEVER be used in production**. It is intended solely for:
 * - Unit testing
 * - Integration testing
 * - Proof of concept demonstrations
 * - Development and learning purposes
 *
 * ## Security Warning
 * ⚠️ **DO NOT USE IN PRODUCTION!** ⚠️
 *
 * This implementation uses a hardcoded key ("0123456789abcdef") which provides no real
 * security. In production environments, you should:
 * - Use a proper key management system (e.g., Android Keystore, Java KeyStore, AWS KMS, HashiCorp Vault)
 * - Generate cryptographically secure random keys
 * - Implement key rotation policies
 * - Store keys securely with appropriate access controls
 * - Never hardcode keys in source code
 *
 * ## Production Implementation Example
 * ```kotlin
 * class ProductionKeyProvider(private val keyStore: KeyStore) : SecureKeyProvider {
 *     override fun currentKey(): Key? {
 *         return try {
 *             keyStore.getKey("myKeyAlias", keyPassword) as SecretKey
 *         } catch (e: Exception) {
 *             logger.error("Failed to retrieve key", e)
 *             null
 *         }
 *     }
 * }
 * ```
 *
 * @see SecureKeyProvider
 * @see SampleCryptoEngine
 */
class SampleKeyProvider : SecureKeyProvider {

    /**
     * A fake, hardcoded AES key for demonstration purposes only.
     *
     * **WARNING:** This key provides NO real security.
     */
    private val fakeKey: Key = SecretKeySpec("0123456789abcdef".toByteArray(), "AES")

    /**
     * Returns the fake encryption key.
     *
     * @return A hardcoded AES key that should only be used for testing.
     */
    override fun currentKey(): Key? = fakeKey
}
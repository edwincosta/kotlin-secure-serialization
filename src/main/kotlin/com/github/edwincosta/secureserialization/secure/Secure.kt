package com.github.edwincosta.secureserialization.secure

import kotlinx.serialization.SerialName

/**
 * Marker interface for classes that support end-to-end encryption during serialization.
 *
 * Classes implementing this interface can be serialized with encrypted properties
 * when using [SecureSerializer]. The interface provides a mechanism to store the
 * initialization vector (IV) needed for encryption/decryption operations.
 *
 * ## Usage Example
 * ```kotlin
 * @Serializable(with = MySerializer::class)
 * data class SecureData(
 *     val id: Int,
 *     @SecureProperty
 *     val sensitiveInfo: String,
 *     override val e2eIv: String? = null
 * ) : Secure
 * ```
 *
 * @property e2eIv The initialization vector (IV) used for encryption/decryption.
 *                 This value is automatically generated during serialization and
 *                 stored alongside the encrypted data. It must be present for
 *                 successful decryption.
 *
 * @see SecureSerializer
 * @see SecureProperty
 */
interface Secure {
    @SerialName("e2e_iv")
    val e2eIv: String?
}

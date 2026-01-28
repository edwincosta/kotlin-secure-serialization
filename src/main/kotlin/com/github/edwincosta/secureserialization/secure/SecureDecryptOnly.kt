package com.github.edwincosta.secureserialization.secure

/**
 * Annotation that prevents encryption of [SecureProperty] fields during serialization.
 *
 * When applied to a class, this annotation makes the [SecureSerializer] skip the encryption
 * step during serialization while still supporting decryption during deserialization. This is
 * useful for scenarios where:
 * - You need to read encrypted data but don't want to re-encrypt it
 * - The client should only decrypt data but never encrypt it
 * - You want to forward encrypted data as-is without modification
 *
 * Properties marked with [SecureProperty] will still be decrypted when deserializing, but
 * will be serialized in their plain form (or as-is if already encrypted) without re-encryption.
 *
 * ## Usage Example
 * ```kotlin
 * @SecureDecryptOnly
 * @Serializable(with = MySerializer::class)
 * data class ReadOnlyUser(
 *     val id: Int,
 *     @SecureProperty
 *     val email: String,
 *     override val e2eIv: String? = null
 * ) : Secure
 * ```
 *
 * In this example, `email` will be decrypted when reading but won't be encrypted when writing.
 *
 * @see SecureProperty
 * @see SecureSerializer
 * @see Secure
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class SecureDecryptOnly

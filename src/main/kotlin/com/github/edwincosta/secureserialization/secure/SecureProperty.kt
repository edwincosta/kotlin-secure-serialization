package com.github.edwincosta.secureserialization.secure

/**
 * Marks a property for automatic encryption during serialization and decryption during deserialization.
 *
 * Properties annotated with `@SecureProperty` will be automatically encrypted when the object
 * is serialized using [SecureSerializer], provided that:
 * - A valid encryption key is available from the [SecureKeyProvider]
 * - The containing class implements the [Secure] interface
 * - The class is not annotated with [SecureDecryptOnly]
 *
 * During serialization, the annotated property will be replaced with an encrypted version
 * prefixed with "e2e_" (e.g., "email" becomes "e2e_email"). During deserialization, the
 * encrypted value is automatically decrypted and assigned to the property.
 *
 * ## Usage Example
 * ```kotlin
 * @Serializable(with = MySerializer::class)
 * data class User(
 *     val id: Int,
 *     @SerialName("email")
 *     @SecureProperty
 *     val email: String,
 *     override val e2eIv: String? = null
 * ) : Secure
 * ```
 *
 * In this example, when serialized, the `email` field will be encrypted and stored as `e2e_email`.
 *
 * @see SecureSerializer
 * @see Secure
 * @see SecureDecryptOnly
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class SecureProperty

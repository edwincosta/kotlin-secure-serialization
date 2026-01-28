package com.github.edwincosta.secureserialization.sample

import com.github.edwincosta.secureserialization.secure.Secure
import com.github.edwincosta.secureserialization.secure.SecureProperty
import com.github.edwincosta.secureserialization.secure.SecureSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Custom serializer instance for the [User] class.
 *
 * This serializer demonstrates how to configure [SecureSerializer] for a specific data class.
 * It uses sample implementations of key provider and crypto engine for demonstration purposes.
 *
 * **Note:** The [SampleKeyProvider] and [SampleCryptoEngine] used here are NOT secure
 * and should only be used for testing and demonstration. In production, use proper
 * implementations with real encryption algorithms and secure key management.
 *
 * @see User
 * @see SampleKeyProvider
 * @see SampleCryptoEngine
 */
object UserSerializer : SecureSerializer<User>(
    dataClass = User::class,
    keyProvider = SampleKeyProvider(),
    cryptoEngine = SampleCryptoEngine(),
)

/**
 * Sample data class demonstrating end-to-end encryption of sensitive user information.
 *
 * This class serves as an example of how to implement the [Secure] interface and use
 * [SecureProperty] annotations to protect sensitive data during serialization. Properties
 * marked with [SecureProperty] (firstName, lastName, email) will be automatically encrypted
 * when serialized and decrypted when deserialized.
 *
 * ## Serialization Example
 * ```kotlin
 * val user = User(
 *     id = 1,
 *     firstName = "John",
 *     lastName = "Doe",
 *     email = "john.doe@example.com"
 * )
 *
 * val json = Json { prettyPrint = true }
 * val serialized = json.encodeToString(UserSerializer, user)
 * // Produces JSON with encrypted firstName, lastName, and email
 *
 * val deserialized = json.decodeFromString(UserSerializer, serialized)
 * // Automatically decrypts the secure properties
 * ```
 *
 * ## Serialized JSON Format
 * ```json
 * {
 *   "id": 1,
 *   "e2e_firstname": "encrypted_base64_data",
 *   "e2e_lastname": "encrypted_base64_data",
 *   "e2e_email": "encrypted_base64_data",
 *   "e2e_iv": "initialization_vector"
 * }
 * ```
 *
 * @property id The user's unique identifier (not encrypted).
 * @property firstName The user's first name (encrypted during serialization).
 * @property lastName The user's last name (encrypted during serialization).
 * @property email The user's email address (encrypted during serialization).
 * @property e2eIv The initialization vector for encryption/decryption operations.
 *
 * @see Secure
 * @see SecureProperty
 * @see UserSerializer
 */
@Serializable(with = UserSerializer::class)
data class User(
    @SerialName("id")
    val id: Int,

    @SerialName("firstname")
    @SecureProperty
    val firstName: String?,

    @SerialName("lastname")
    @SecureProperty
    val lastName: String?,

    @SerialName("email")
    @SecureProperty
    val email: String?,

    @SerialName("e2e_iv")
    override val e2eIv: String? = null,
) : Secure

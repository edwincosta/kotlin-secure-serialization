package com.github.edwincosta.secureserialization.sample

import com.github.edwincosta.secureserialization.secure.Secure
import com.github.edwincosta.secureserialization.secure.SecureProperty
import com.github.edwincosta.secureserialization.secure.SecureSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Custom serializer instance for User
object UserSerializer : SecureSerializer<User>(
    dataClass = User::class,
    keyProvider = SampleKeyProvider(),
    cryptoEngine = SampleCryptoEngine(),
)

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

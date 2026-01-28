package com.github.edwincosta.secureserialization

import com.github.edwincosta.secureserialization.sample.SampleCryptoEngine
import com.github.edwincosta.secureserialization.sample.SampleKeyProvider
import com.github.edwincosta.secureserialization.secure.Secure
import com.github.edwincosta.secureserialization.secure.SecureKeyProvider
import com.github.edwincosta.secureserialization.secure.SecureProperty
import com.github.edwincosta.secureserialization.secure.SecureSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.security.Key

class SecureSerializerTest {

    // ---------- Test model ----------

    object TestUserSerializer : SecureSerializer<TestUser>(
        dataClass = TestUser::class,
        keyProvider = SampleKeyProvider(),
        cryptoEngine = SampleCryptoEngine(),
    )

    @Serializable(with = TestUserSerializer::class)
    data class TestUser(
        @SerialName("id")
        val id: Int,

        @SerialName("name")
        @SecureProperty
        val name: String?,

        @SerialName("email")
        @SecureProperty
        val email: String?,

        @SerialName("age")
        val age: Int,

        @SerialName("e2e_iv")
        override val e2eIv: String? = null,
    ) : Secure

    private val json = Json {
        prettyPrint = false
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    // ---------- Tests ----------

    @Test
    fun `should serialize with encryption when key is present`() {
        val user = TestUser(
            id = 1,
            name = "John",
            email = "john@test.com",
            age = 30,
        )

        val encoded = json.encodeToString(TestUser.serializer(), user)

        // Should contain encrypted fields
        assertTrue(encoded.contains("e2e_name"))
        assertTrue(encoded.contains("e2e_email"))
        assertTrue(encoded.contains("e2e_iv"))

        // Should NOT contain plain sensitive fields
        assertFalse(encoded.contains("\"name\""))
        assertFalse(encoded.contains("\"email\""))

        // Should still contain non-secure field
        assertTrue(encoded.contains("\"age\""))
    }

    @Test
    fun `should deserialize encrypted json into plain object`() {
        val encryptedJson = """
            {
              "id": 1,
              "e2e_iv": "sample-iv",
              "e2e_name": "Sm9obg==",
              "e2e_email": "am9obkB0ZXN0LmNvbQ==",
              "age": 30
            }
        """.trimIndent()

        val user = json.decodeFromString(TestUser.serializer(), encryptedJson)

        assertEquals("John", user.name)
        assertEquals("john@test.com", user.email)
        assertEquals(30, user.age)
    }

    @Test
    fun `should serialize plain when no key is available`() {
        // KeyProvider that returns null
        val noKeySerializer = object : SecureSerializer<TestUser>(
            dataClass = TestUser::class,
            keyProvider = object : SecureKeyProvider {
                override fun currentKey(): Key? = null
            },
            cryptoEngine = SampleCryptoEngine(),
        ) {}

        val user = TestUser(
            id = 1,
            name = "John",
            email = "john@test.com",
            age = 30,
        )

        val localJson = Json {
            prettyPrint = false
            encodeDefaults = true
        }

        val encoded = localJson.encodeToString(noKeySerializer, user)

        // Should contain plain fields
        assertTrue(encoded.contains("\"name\""))
        assertTrue(encoded.contains("\"email\""))

        // Should NOT contain encrypted fields
        assertFalse(encoded.contains("e2e_name"))
        assertFalse(encoded.contains("e2e_email"))
    }

    @Test
    fun `should ignore non secure fields`() {
        val user = TestUser(
            id = 1,
            name = "John",
            email = "john@test.com",
            age = 30,
        )

        val encoded = json.encodeToString(TestUser.serializer(), user)

        // age is not annotated, must not be encrypted
        assertTrue(encoded.contains("\"age\""))
        assertFalse(encoded.contains("e2e_age"))
    }
}

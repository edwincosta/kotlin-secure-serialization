package com.github.edwincosta.secureserialization.secure

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Transient
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json.Default.serializersModule
import kotlinx.serialization.serializer
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible

/**
 * A custom Kotlinx Serialization serializer that provides automatic encryption and decryption
 * of properties marked with [SecureProperty].
 *
 * This serializer implements end-to-end encryption for sensitive data during serialization,
 * automatically encrypting marked properties before they are written and decrypting them when
 * read back. It integrates with Kotlinx Serialization framework and supports nullable properties,
 * custom serial names, and transient fields.
 *
 * ## Key Features
 * - Automatic encryption/decryption of [SecureProperty] annotated fields
 * - Support for nullable and optional properties
 * - Integration with custom [SecureKeyProvider] for key management
 * - Pluggable [SecureCryptoEngine] for different encryption algorithms
 * - Respects [SecureDecryptOnly] annotation to skip encryption
 * - Automatic IV generation and storage
 * - Handles [SerialName] annotations for custom field naming
 *
 * ## Usage Example
 * ```kotlin
 * @Serializable(with = UserSerializer::class)
 * data class User(
 *     val id: Int,
 *     @SecureProperty
 *     val email: String,
 *     @SecureProperty
 *     val ssn: String?,
 *     override val e2eIv: String? = null
 * ) : Secure
 *
 * object UserSerializer : SecureSerializer<User>(
 *     dataClass = User::class,
 *     keyProvider = MyKeyProvider(),
 *     cryptoEngine = AesGcmEngine()
 * )
 *
 * // Usage
 * val json = Json { /* config */ }
 * val user = User(1, "user@example.com", "123-45-6789")
 * val encrypted = json.encodeToString(UserSerializer, user)
 * val decrypted = json.decodeFromString(UserSerializer, encrypted)
 * ```
 *
 * ## Serialization Format
 * Properties marked with [SecureProperty] are serialized with an "e2e_" prefix:
 * ```json
 * {
 *   "id": 1,
 *   "e2e_email": "encrypted_data",
 *   "e2e_ssn": "encrypted_data",
 *   "e2e_iv": "initialization_vector"
 * }
 * ```
 *
 * @param T The type of the data class to serialize. Must implement [Secure].
 * @param dataClass The KClass of the data class being serialized.
 * @param keyProvider Provider for encryption/decryption keys.
 * @param cryptoEngine Engine that performs the actual encryption/decryption operations.
 * @param secureAttr The name of the property that stores the IV. Defaults to "e2e_iv".
 * @param secureAttrPrefix The prefix added to encrypted property names. Defaults to "e2e_".
 *
 * @see Secure
 * @see SecureProperty
 * @see SecureKeyProvider
 * @see SecureCryptoEngine
 * @see SecureDecryptOnly
 */
open class SecureSerializer<T : Secure>(
    private val dataClass: KClass<T>,
    private val keyProvider: SecureKeyProvider,
    private val cryptoEngine: SecureCryptoEngine,
    open val secureAttr: String = "e2e_iv",
    open val secureAttrPrefix: String = "e2e_",
) : KSerializer<T> {

    /**
     * The serial descriptor that describes the structure of the serialized data.
     *
     * This descriptor is built dynamically based on the data class structure, including
     * both regular properties and encrypted versions of [SecureProperty] annotated fields.
     * Properties marked with [Transient] are excluded from the descriptor.
     */
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(dataClass.simpleName!!) {
        dataClass.primaryConstructor!!.parameters.forEach { param ->
            val prop = dataClass.declaredMemberProperties.first { it.name == param.name }
            // Skips properties annotated with @Transient
            if (!prop.hasAnnotation<Transient>()) {
                val serialName = prop.findAnnotation<SerialName>()?.value ?: prop.name
                element(serialName, prop.returnType.descriptor())
                if (prop.hasAnnotation<SecureProperty>()) {
                    val secureSerialName = "$secureAttrPrefix$serialName"
                    element(
                        secureSerialName,
                        PrimitiveSerialDescriptor(secureSerialName, PrimitiveKind.STRING),
                        isOptional = prop.returnType.isMarkedNullable,
                    )
                }
            }
        }
    }

    /**
     * Serializes the given value, encrypting properties marked with [SecureProperty].
     *
     * This method performs the following steps:
     * 1. Checks if the class is annotated with [SecureDecryptOnly]
     * 2. Retrieves the encryption key from [keyProvider]
     * 3. Generates a new IV using [cryptoEngine] if encryption is enabled
     * 4. Iterates through all properties:
     *    - Encrypts [SecureProperty] annotated fields (if key and IV are available)
     *    - Serializes regular fields as-is
     *    - Stores the generated IV in the [secureAttr] field
     * 5. Skips properties marked with [Transient]
     *
     * If no encryption key is available or the class is marked [SecureDecryptOnly],
     * secure properties are serialized without encryption.
     *
     * @param encoder The encoder to write the serialized data to.
     * @param value The object to serialize.
     */
    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: T) {
        val compositeEncoder = encoder.beginStructure(descriptor)

        val decryptOnly = dataClass.hasAnnotation<SecureDecryptOnly>()
        val key = keyProvider.currentKey()

        val generatedIv = if (key != null && !decryptOnly) cryptoEngine.generateIv() else null

        dataClass.primaryConstructor!!.parameters.forEach { param ->
            val prop = dataClass.declaredMemberProperties.first { it.name == param.name }
            prop.isAccessible = true

            // Skips properties annotated with @Transient
            if (!prop.hasAnnotation<Transient>()) {
                val propValue = prop.get(value)
                val serialName = prop.findAnnotation<SerialName>()?.value ?: prop.name
                val serializer = serializersModule.serializer(prop.returnType)

                if (serialName == secureAttr) {
                    compositeEncoder.encodeNullableSerializableElement(
                        descriptor,
                        descriptor.getElementIndex(serialName),
                        serializer,
                        generatedIv,
                    )
                } else if (prop.hasAnnotation<SecureProperty>() && key != null && generatedIv != null && propValue != null) {
                    val secureSerialName = "$secureAttrPrefix$serialName"
                    val encryptedValue = cryptoEngine.encrypt(propValue.toString(), key, generatedIv)
                    compositeEncoder.encodeNullableSerializableElement(
                        descriptor,
                        descriptor.getElementIndex(secureSerialName),
                        serializersModule.serializer<String>(),
                        encryptedValue,
                    )
                } else {
                    compositeEncoder.encodeNullableSerializableElement(
                        descriptor,
                        descriptor.getElementIndex(serialName),
                        serializer,
                        propValue,
                    )
                }
            }
        }

        compositeEncoder.endStructure(descriptor)
    }

    /**
     * Deserializes the given input, decrypting properties marked with [SecureProperty].
     *
     * This method performs the following steps:
     * 1. Decodes all properties from the input into a map
     * 2. Retrieves the encryption key from [keyProvider]
     * 3. Extracts the IV from the [secureAttr] field
     * 4. For each constructor parameter:
     *    - Decrypts [SecureProperty] annotated fields using the key and IV
     *    - Converts decrypted strings back to their original types
     *    - Uses regular values for non-encrypted fields
     * 5. Calls the primary constructor with the prepared parameter values
     *
     * If no encryption key or IV is available, secure properties are read without decryption.
     * The method handles type conversion for primitive types automatically.
     *
     * @param decoder The decoder to read the serialized data from.
     * @return The deserialized and decrypted object.
     */
    @OptIn(ExperimentalSerializationApi::class)
    override fun deserialize(decoder: Decoder): T {
        val dec = decoder.beginStructure(descriptor)
        val values = mutableMapOf<String, Any?>()
        val serialNameToProp = dataClass
            .declaredMemberProperties
            .associateBy {
                (it.findAnnotation<SerialName>()?.value ?: it.name)
            }

        loop@ while (true) {
            when (val index = dec.decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> break@loop
                else -> {
                    val name = descriptor.getElementName(index)

                    val prop = serialNameToProp[name]

                    val serializer = prop
                        ?.returnType
                        ?.let {
                            serializersModule.serializer(it)
                        }
                        // If not found try to deserialize as String
                        ?: serializersModule.serializer<String>()

                    values[name] = dec.decodeNullableSerializableElement(descriptor, index, serializer)
                }
            }
        }
        dec.endStructure(descriptor)

        val key = keyProvider.currentKey()
        val ivValue = dataClass
            .declaredMemberProperties
            .firstOrNull { (it.findAnnotation<SerialName>()?.value ?: it.name) == secureAttr }
            ?.let {
                val propName = it.findAnnotation<SerialName>()?.value ?: it.name
                values[propName] as String?
            }

        val constructor = dataClass.primaryConstructor!!
        val parameterValues = mutableMapOf<KParameter, Any?>()

        constructor.parameters.forEach { param ->
            val memberProperty = dataClass.declaredMemberProperties.first { it.name == param.name }
            val paramSerialName = memberProperty.findAnnotation<SerialName>()?.value ?: memberProperty.name

            val paramValue = if (key != null
                && ivValue != null
                && memberProperty.hasAnnotation<SecureProperty>()
            ) {
                val securePropName = "$secureAttrPrefix$paramSerialName"
                val encryptedValue = values[securePropName] as String?
                val decryptedValue = encryptedValue?.let { cryptoEngine.decrypt(it, key, ivValue) }
                param.type.deserialize(decryptedValue, descriptor = param.type.descriptor())
            } else {
                values[paramSerialName]
            }

            if (paramValue != null || !param.isOptional) {
                parameterValues[param] = paramValue
            }
        }

        return constructor.callBy(parameterValues)
    }

    /**
     * Hook for custom deserialization of non-primitive kinds.
     *
     * Use this to convert decrypted string values into complex types that are not
     * handled by the primitive branch in `deserialize()`. The default implementation
     * returns `null`.
     *
     * Example:
     * ```kotlin
     * override fun deserializeNonPrimitiveKind(
     *     kindType: KType,
     *     value: String?,
     *     descriptor: SerialDescriptor
     * ): Any? {
     *     if (kindType.classifier == CustomType::class && value != null) {
     *         return CustomType(value)
     *     }
     *
     *     return null
     * }
     * ```
     *
     * @param kindType The target type to deserialize into.
     * @param value The decrypted string value, or null.
     * @param descriptor The serial descriptor for the target type.
     * @return A deserialized value, or null if not handled.
     */
    protected open fun deserializeNonPrimitiveKind(
        kindType: KType,
        value: String?,
        descriptor: SerialDescriptor
    ): Any? {
        return null
    }

    /**
     * Extension function to get the serial descriptor for a given KType.
     *
     * @return The [SerialDescriptor] for this type.
     */
    protected fun KType.descriptor(): SerialDescriptor {
        val kSerializer = serializersModule.serializer(this)
        return kSerializer.descriptor
    }

    /**
     * Extension function to deserialize a string value to the appropriate primitive type.
     *
     * This function converts decrypted string values back to their original types based
     * on the type descriptor. Supports all Kotlin primitive types (Boolean, Byte, Char,
     * Short, Int, Long, Double, Float, and String).
     *
     * @param value The string value to deserialize, or null.
     * @param descriptor The [SerialDescriptor] that describes the target type for deserialization.
     * @return The deserialized value in its original type, or null if the value is null
     *         or cannot be converted.
     */
    protected open fun KType.deserialize(value: String?, descriptor: SerialDescriptor): Any? {
        return when (descriptor.kind) {
            PrimitiveKind.BOOLEAN -> value?.toBoolean()
            PrimitiveKind.BYTE -> value?.toByteOrNull()
            PrimitiveKind.CHAR -> value?.get(0)
            PrimitiveKind.SHORT -> value?.toShortOrNull()
            PrimitiveKind.INT -> value?.toIntOrNull()
            PrimitiveKind.LONG -> value?.toLongOrNull()
            PrimitiveKind.DOUBLE -> value?.toDoubleOrNull()
            PrimitiveKind.FLOAT -> value?.toFloatOrNull()
            PrimitiveKind.STRING -> value
            else -> {
                deserializeNonPrimitiveKind(this, value, descriptor)
            }
        }
    }
}

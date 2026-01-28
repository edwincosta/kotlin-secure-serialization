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

open class SecureSerializer<T : Secure>(
    private val dataClass: KClass<T>,
    private val keyProvider: SecureKeyProvider,
    private val cryptoEngine: SecureCryptoEngine,
    open val secureAttr: String = "e2e_iv",
    open val secureAttrPrefix: String = "e2e_",
) : KSerializer<T> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(dataClass.simpleName!!) {
        dataClass.primaryConstructor!!.parameters.forEach { param ->
            val prop = dataClass.declaredMemberProperties.first { it.name == param.name }
            // Does not includes into descriptor, properties with @DisplayNameFormat annotation
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
                param.type.deserialize(decryptedValue)
            } else {
                values[paramSerialName]
            }

            if (paramValue != null || !param.isOptional) {
                parameterValues[param] = paramValue
            }
        }

        return constructor.callBy(parameterValues)
    }

    // Extension function to get the KSerializer for a given KType
    private fun KType.descriptor(): SerialDescriptor {
        val kSerializer = serializersModule.serializer(this)
        return kSerializer.descriptor
    }

    protected fun KType.deserialize(value: String?): Any? {
        val descriptor = descriptor()

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
                null
            }
        }
    }
}

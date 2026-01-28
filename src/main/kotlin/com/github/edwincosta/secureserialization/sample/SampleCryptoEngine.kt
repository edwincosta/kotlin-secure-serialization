package com.github.edwincosta.secureserialization.sample

import com.github.edwincosta.secureserialization.secure.SecureCryptoEngine
import java.security.Key
import java.util.Base64

class SampleCryptoEngine : SecureCryptoEngine {

    override fun generateIv(): String {
        // Just a fake IV for demonstration purposes
        return "sample-iv"
    }

    override fun encrypt(plainText: String, key: Key, iv: String): String {
        // NOT real encryption. Only for sample/demo.
        return Base64.getEncoder().encodeToString(plainText.toByteArray())
    }

    override fun decrypt(cipherText: String, key: Key, iv: String): String {
        // NOT real decryption. Only for sample/demo.
        return String(Base64.getDecoder().decode(cipherText))
    }
}
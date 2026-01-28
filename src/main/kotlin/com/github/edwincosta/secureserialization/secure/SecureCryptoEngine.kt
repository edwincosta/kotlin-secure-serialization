package com.github.edwincosta.secureserialization.secure

import java.security.Key

interface SecureCryptoEngine {
    fun generateIv(): String
    fun encrypt(plainText: String, key: Key, iv: String): String
    fun decrypt(cipherText: String, key: Key, iv: String): String
}
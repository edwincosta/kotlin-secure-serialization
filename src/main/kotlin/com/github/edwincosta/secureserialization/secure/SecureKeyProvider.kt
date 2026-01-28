package com.github.edwincosta.secureserialization.secure

import java.security.Key

interface SecureKeyProvider {
    fun currentKey(): Key?
}
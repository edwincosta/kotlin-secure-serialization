package com.github.edwincosta.secureserialization.sample

import com.github.edwincosta.secureserialization.secure.SecureKeyProvider
import java.security.Key
import javax.crypto.spec.SecretKeySpec

class SampleKeyProvider : SecureKeyProvider {

    private val fakeKey: Key = SecretKeySpec("0123456789abcdef".toByteArray(), "AES")

    override fun currentKey(): Key? = fakeKey
}
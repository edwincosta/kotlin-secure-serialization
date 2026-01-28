package com.github.edwincosta.secureserialization.secure

import kotlinx.serialization.SerialName

interface Secure {
    @SerialName("e2e_iv")
    val e2eIv: String?
}

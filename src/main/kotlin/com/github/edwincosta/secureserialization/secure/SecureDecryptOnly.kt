package com.github.edwincosta.secureserialization.secure

// this annotation makes it so that we don't encrypt the fields when serializing
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class SecureDecryptOnly

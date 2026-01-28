plugins {
    kotlin("jvm") version libs.versions.kotlin
    alias(libs.plugins.kotlin.serialization)
}

group = "com.github.edwincosta"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.kotlinx.serialization)
    implementation(libs.kotlin.reflect)
}
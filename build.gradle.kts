plugins {
    kotlin("jvm") version "2.0.0"
}

group = "com.github.edwincosta"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
}
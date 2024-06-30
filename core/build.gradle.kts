plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.dokka)
    `maven-publish`
    signing
    java
}

group = "dev.redstones.nebula.core"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.bundles.ktor.client)
    testImplementation(libs.bundles.test)
}

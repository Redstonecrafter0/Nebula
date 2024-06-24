plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlinx.serialization)
}

group = "dev.redstones.nebula.core"
version = libs.versions.nebula.get()

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.bundles.ktor.client)
    testImplementation(libs.ktor.client.java)
}

kotlin {
    jvmToolchain(17)
}

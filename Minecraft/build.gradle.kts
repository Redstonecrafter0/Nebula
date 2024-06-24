plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlinx.serialization)
}

group = "dev.redstones.nebula.minecraft"
version = libs.versions.nebula.get()

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.bundles.ktor.client)
    implementation(project(":core"))
    testImplementation(libs.ktor.client.java)
}

kotlin {
    jvmToolchain(17)
}

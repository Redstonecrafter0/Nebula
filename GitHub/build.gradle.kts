plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlinx.serialization)
}

group = "dev.redstones.nebula.github"
version = libs.versions.nebula.get()

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.bundles.ktor.client)
    implementation(project(":core"))
    testImplementation(libs.bundles.test)
}

kotlin {
    jvmToolchain(17)
}

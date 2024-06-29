plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlinx.serialization)
}

group = "dev.redstones.nebula.k3d"
version = libs.versions.nebula.get()

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.bundles.ktor.client)
    implementation(project(":core"))
    implementation(project(":GitHub"))
    testImplementation(libs.bundles.test)
}

kotlin {
    jvmToolchain(17)
}

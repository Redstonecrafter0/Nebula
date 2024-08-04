plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.dokka)
    `maven-publish`
    signing
    java
}

group = "dev.redstones.nebula.jdk"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.bundles.ktor.client)
    implementation(project(":core"))
    implementation(project(":Archive"))
    testImplementation(libs.bundles.test)
}

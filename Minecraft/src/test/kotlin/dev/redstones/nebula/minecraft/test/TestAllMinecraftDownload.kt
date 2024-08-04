package dev.redstones.nebula.minecraft.test

import dev.redstones.nebula.event.NebulaEventDownloadFinished
import dev.redstones.nebula.installNebula
import dev.redstones.nebula.minecraft.*
import dev.redstones.nebula.minecraft.dao.MinecraftVersionJson
import dev.redstones.nebula.toNebula
import io.ktor.client.*
import io.ktor.client.engine.java.*
import kotlinx.coroutines.flow.last
import kotlinx.serialization.json.Json
import me.tongfei.progressbar.ProgressBar
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText

private val json = Json {
    ignoreUnknownKeys = true
}

suspend fun main() {
    val progressBar = ProgressBar("Downloading", 0)
    val client = HttpClient(Java) {
        installNebula()
    }.toNebula()
    progressBar.extraMessage = "Loading versions"
    val versions = client.listMinecraftVersions()!!
    progressBar.maxHint(versions.versions.size.toLong())
    for (version in versions.versions) {
        progressBar.extraMessage = "Loading ${version.id}"
        progressBar.step()
        progressBar.refresh()

        val outputFile = Path.of("test/minecraft/all/versions/${version.id}/client.json")
        if (!client.downloadMinecraftVersionJson(version, outputFile)) {
            System.err.println("failed")
        }
        val versionJson = json.decodeFromString<MinecraftVersionJson>(outputFile.readText())

        val assetIndexFile = Path.of("test/minecraft/all/assets/${versionJson.assetIndex.id}.json")
        if (!assetIndexFile.exists() || !assetIndexFile.isRegularFile()) {
            val assetsFinishedEvent = client.downloadMinecraftAssets(versionJson.assetIndex, assetIndexFile, Path.of("test/minecraft/all/objects")).last() as NebulaEventDownloadFinished
            if (!assetsFinishedEvent.success) {
                System.err.println("failed")
            }
        }

        val clientFinishedEvent = client.downloadMinecraftClientJar(versionJson, Path.of("test/minecraft/all/versions/${version.id}/client.jar")).last() as NebulaEventDownloadFinished
        if (!clientFinishedEvent.success) {
            System.err.println("failed")
        }

        if (versionJson.downloads.server != null) {
            val serverFinishedEvent = client.downloadMinecraftServerJar(versionJson, Path.of("test/minecraft/all/versions/${version.id}/server.jar")).last() as NebulaEventDownloadFinished
            if (!serverFinishedEvent.success) {
                System.err.println("failed")
            }
        }

        if (versionJson.logging != null) {
            if (!client.downloadMinecraftClientLoggingConfig(versionJson, Path.of("test/minecraft/all/logging/${versionJson.logging!!.client.file.id}"))) {
                System.err.println("failed")
            }
        }

        val librariesFinishedEvent = client.downloadMinecraftClientLibraries(versionJson, Path.of("test/minecraft/all/libraries")).last() as NebulaEventDownloadFinished
        if (!librariesFinishedEvent.success) {
            System.err.println("failed")
        }
    }
}

package dev.redstones.nebula.minecraft.test

import dev.redstones.nebula.DownloadManager
import dev.redstones.nebula.minecraft.*
import dev.redstones.nebula.minecraft.dao.MinecraftVersionJson
import io.ktor.client.engine.java.*
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
    val progress = ProgressBar("Versions", 0)
    val downloader = DownloadManager(Java)
    downloader.enqueue {
        progress.extraMessage = "Loading versions"
        val versions = listMinecraftVersions()!!
        progress.maxHint(versions.versions.size.toLong())
        for (version in versions.versions) {
            progress.extraMessage = "Loading ${version.id}"
            progress.step()
            progress.refresh()

            val outputFile = Path.of("test/minecraft/all/versions/${version.id}/client.json")
            if (!downloadMinecraftVersionJson(version, outputFile)) {
                System.err.println("failed")
            }
            val versionJson = json.decodeFromString<MinecraftVersionJson>(outputFile.readText())

            val assetIndexFile = Path.of("test/minecraft/all/assets/${versionJson.assetIndex.id}.json")
            if (!assetIndexFile.exists() || !assetIndexFile.isRegularFile()) {
                if (!downloadMinecraftAssets(versionJson.assetIndex, assetIndexFile, Path.of("test/minecraft/all/objects"))) {
                    System.err.println("failed")
                }
            }

            if (!downloadMinecraftClientJar(versionJson, Path.of("test/minecraft/all/versions/${version.id}/client.jar"))) {
                System.err.println("failed")
            }

            if (versionJson.downloads.server != null) {
                if (!downloadMinecraftServerJar(versionJson, Path.of("test/minecraft/all/versions/${version.id}/server.jar"))) {
                    System.err.println("failed")
                }
            }

            if (versionJson.logging != null) {
                if (!downloadMinecraftClientLoggingConfig(versionJson, Path.of("test/minecraft/all/logging/${versionJson.logging!!.client.file.id}"))) {
                    System.err.println("failed")
                }
            }

            if (!downloadMinecraftClientLibraries(versionJson, Path.of("test/minecraft/all/libraries"))) {
                System.err.println("failed")
            }
        }
    }
    downloader.runSingle()
}

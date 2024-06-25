package dev.redstones.nebula.minecraft.test

import dev.redstones.nebula.DownloadManager
import dev.redstones.nebula.minecraft.*
import dev.redstones.nebula.minecraft.dao.MinecraftVersionJson
import io.ktor.client.engine.java.*
import kotlinx.serialization.json.Json
import java.nio.file.Path
import kotlin.io.path.readText

private val json = Json {
    ignoreUnknownKeys = true
}

suspend fun main() {
    val downloader = DownloadManager(Java)
    downloader.enqueue {
        val versions = listVersionsMinecraft()!!
        val latest = versions.versions.first { it.id == versions.latest.release }
        val outputFile = Path.of("test/test.json")
        if (!downloadMinecraftVersionJson(latest, outputFile)) {
            System.err.println("failed")
        }
        if (!downloadMinecraftAssets(outputFile, Path.of("test/assets.json"), Path.of("test/objects"))) {
            System.err.println("failed")
        }
        if (!downloadMinecraftClientJar(outputFile, Path.of("test/client.jar"))) {
            System.err.println("failed")
        }
        if (!downloadMinecraftServerJar(outputFile, Path.of("test/server.jar"))) {
            System.err.println("failed")
        }
        if (!downloadMinecraftClientLoggingConfig(outputFile, Path.of("test/client-1.12.xml"))) {
            System.err.println("failed")
        }
        if (!downloadMinecraftClientLibraries(outputFile, Path.of("test/libraries"))) {
            System.err.println("failed")
        }
        val meta = json.decodeFromString<MinecraftVersionJson>(outputFile.readText())
        println(meta)
    }
    downloader.runSingle()
}

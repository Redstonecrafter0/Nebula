package dev.redstones.nebula.minecraft.test

import dev.redstones.nebula.DownloadManager
import dev.redstones.nebula.minecraft.*
import io.ktor.client.engine.java.*
import me.tongfei.progressbar.ProgressBar
import java.nio.file.Path

suspend fun main() {
    val steps = ProgressBar("Steps", 7)
    var subBar: ProgressBar? = null
    val downloader = DownloadManager(Java)
    downloader.addEventListener {
        onStart { step: Int, _: Int, max: Long? ->
            subBar = if (max == null) {
                null
            } else {
                ProgressBar("Downloading step $step", max)
            }
        }
        onProgress {
            if (it != null) {
                subBar?.stepTo(it)
            }
        }
        onFinished { success, message ->
            steps.step()
            subBar?.close()
        }
    }
    downloader.download {
        maxStep = 7
        steps.extraMessage = "Loading versions"
        steps.step()

        val versions = listMinecraftVersions()!!
        val latest = versions.versions.first { it.id == versions.latest.release }

        val outputFile = Path.of("test/minecraft/single/test.json")
        steps.extraMessage = "Loading ${latest.id} json"
        if (!downloadMinecraftVersionJson(latest, outputFile)) {
            System.err.println("failed")
        }

        steps.extraMessage = "Loading assets"
        if (!downloadMinecraftAssets(outputFile, Path.of("test/minecraft/single/assets.json"), Path.of("test/minecraft/single/objects"))) {
            System.err.println("failed")
        }

        steps.extraMessage = "Loading client jar"
        if (!downloadMinecraftClientJar(outputFile, Path.of("test/minecraft/single/client.jar"))) {
            System.err.println("failed")
        }

        steps.extraMessage = "Loading server jar"
        if (!downloadMinecraftServerJar(outputFile, Path.of("test/minecraft/single/server.jar"))) {
            System.err.println("failed")
        }

        steps.extraMessage = "Loading logging config"
        if (!downloadMinecraftClientLoggingConfig(outputFile, Path.of("test/minecraft/single/client-1.12.xml"))) {
            System.err.println("failed")
        }

        steps.extraMessage = "Loading libraries"
        if (!downloadMinecraftClientLibraries(outputFile, Path.of("test/minecraft/single/libraries"))) {
            System.err.println("failed")
        }
    }
}

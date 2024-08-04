package dev.redstones.nebula.minecraft.test

import dev.redstones.nebula.DownloadWatcher
import dev.redstones.nebula.event.NebulaEventDownloadFinished
import dev.redstones.nebula.installNebula
import dev.redstones.nebula.minecraft.*
import dev.redstones.nebula.toNebula
import dev.redstones.nebula.watch
import io.ktor.client.*
import io.ktor.client.engine.java.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.launch
import me.tongfei.progressbar.ProgressBar
import java.nio.file.Path

suspend fun main() {
    val progressBar = ProgressBar("Downloading", 0)
    val watcher = DownloadWatcher()
    val client = HttpClient(Java) {
        installNebula()
    }.toNebula()
    coroutineScope {
        val job = launch {
            watcher.subscribe().collect {
                if (it != null) {
                    progressBar.stepTo(it.pos)
                    progressBar.maxHint(it.totalSize ?: 0)
                }
            }
        }
        launch {
            val versions = client.listMinecraftVersions()!!
            val latest = versions.versions.first { it.id == versions.latest.release }

            progressBar.extraMessage = "Loading ${latest.id} json"
            val outputFile = Path.of("test/minecraft/single/test.json")
            if (!client.downloadMinecraftVersionJson(latest, outputFile)) {
                System.err.println("failed")
            }

            progressBar.extraMessage = "Loading assets"
            val assetsFinishedEvent = client.downloadMinecraftAssets(outputFile, Path.of("test/minecraft/single/assets.json"), Path.of("test/minecraft/single/objects")).watch(watcher).last() as NebulaEventDownloadFinished
            if (!assetsFinishedEvent.success) {
                System.err.println("failed")
            }

            progressBar.extraMessage = "Loading client jar"
            val clientJarFinishedEvent = client.downloadMinecraftClientJar(outputFile, Path.of("test/minecraft/single/client.jar")).watch(watcher).last() as NebulaEventDownloadFinished
            if (!clientJarFinishedEvent.success) {
                System.err.println("failed")
            }

            progressBar.extraMessage = "Loading server jar"
            val serverJarFinishedEvent = client.downloadMinecraftServerJar(outputFile, Path.of("test/minecraft/single/server.jar")).watch(watcher).last() as NebulaEventDownloadFinished
            if (!serverJarFinishedEvent.success) {
                System.err.println("failed")
            }

            progressBar.extraMessage = "Loading logging config"
            if (!client.downloadMinecraftClientLoggingConfig(outputFile, Path.of("test/minecraft/single/client-1.12.xml"))) {
                System.err.println("failed")
            }

            progressBar.extraMessage = "Loading libraries"
            val librariesFinishedEvent = client.downloadMinecraftClientLibraries(outputFile, Path.of("test/minecraft/single/libraries")).watch(watcher).last() as NebulaEventDownloadFinished
            if (!librariesFinishedEvent.success) {
                System.err.println("failed")
            }
            job.cancel()
        }
    }
}

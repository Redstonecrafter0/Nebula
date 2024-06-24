package dev.redstones.nebula.minecraft.test

import dev.redstones.nebula.DownloadManager
import dev.redstones.nebula.minecraft.downloadMinecraftClient
import dev.redstones.nebula.minecraft.listVersionsMinecraft
import io.ktor.client.engine.java.*
import java.nio.file.Path
import kotlin.io.path.moveTo

suspend fun main() {
    val downloader = DownloadManager(Java)
    val versions = downloader.listVersionsMinecraft()!!
    val latest = versions.versions.first { it.id == versions.latest.release }
    val output = downloader.downloadMinecraftClient(latest)!!
    output.jsonFile.moveTo(Path.of("test/test.json").also { it.parent.toFile().mkdirs() })
}

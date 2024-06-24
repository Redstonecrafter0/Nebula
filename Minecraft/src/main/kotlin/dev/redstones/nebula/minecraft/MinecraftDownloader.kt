package dev.redstones.nebula.minecraft

import dev.redstones.nebula.DownloadManager
import dev.redstones.nebula.minecraft.dao.MinecraftClientJson
import dev.redstones.nebula.minecraft.dao.MinecraftVersionManifestV2
import dev.redstones.nebula.util.HashAlgorithms
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.nio.file.Files
import kotlin.io.path.createFile
import kotlin.io.path.readText

private val json = Json {
    ignoreUnknownKeys = true
}

suspend fun DownloadManager.downloadMinecraftClient(manifest: MinecraftVersionManifestV2.Version): MinecraftDownloadResult? {
    val tmpDir = withContext(Dispatchers.IO) {
        Files.createTempDirectory("nebula-")
    }
    val metaFile = tmpDir.resolve("client.json").createFile()
    if (!client.prepareGet(manifest.url).executeVerify(metaFile, manifest.sha1, HashAlgorithms.SHA1)) {
        return null
    }
    val meta = json.decodeFromString<MinecraftClientJson>(metaFile.readText())
    println(meta)
    return MinecraftDownloadResult(metaFile)
}

suspend fun DownloadManager.listVersionsMinecraft(): MinecraftVersionManifestV2? {
    val response = client.get("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json")
    if (response.status != HttpStatusCode.OK) {
        return null
    }
    return response.body()
}

package dev.redstones.nebula.minecraft

import dev.redstones.nebula.DownloadQueueItem
import dev.redstones.nebula.minecraft.dao.MinecraftAssetsObjects
import dev.redstones.nebula.minecraft.dao.MinecraftVersionJson
import dev.redstones.nebula.minecraft.dao.MinecraftVersionManifestV2
import dev.redstones.nebula.util.HashAlgorithms
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicLong
import kotlin.io.path.*

private val json = Json {
    ignoreUnknownKeys = true
}

private val os = System.getProperty("os.name").lowercase().let {
    when {
        "win" in it -> "windows"
        "nix" in it || "nux" in it || "aix" in it -> "linux"
        "mac" in it -> "osx"
        else -> ""
    }
}

private val x86 = System.getProperty("os.arch").let {
    it == "x86"
}

suspend fun DownloadQueueItem.downloadMinecraftVersionJson(manifest: MinecraftVersionManifestV2.Version, target: Path): Boolean {
    notifyStart()
    return downloadFileVerified(target, manifest.url, manifest.sha1, HashAlgorithms.SHA1).notifyFinishedDefault()
}

suspend fun DownloadQueueItem.downloadMinecraftAssets(assetsIndex: Path, assetsIndexTarget: Path, objectsDir: Path): Boolean = downloadMinecraftAssets(assetsIndex.readText(), assetsIndexTarget, objectsDir)

suspend fun DownloadQueueItem.downloadMinecraftAssets(assetsIndex: String, assetsIndexTarget: Path, objectsDir: Path): Boolean = downloadMinecraftAssets(json.decodeFromString<MinecraftVersionJson>(assetsIndex), assetsIndexTarget, objectsDir)

suspend fun DownloadQueueItem.downloadMinecraftAssets(assetsIndex: MinecraftVersionJson, assetsIndexTarget: Path, objectsDir: Path): Boolean = downloadMinecraftAssets(assetsIndex.assetIndex, assetsIndexTarget, objectsDir)

suspend fun DownloadQueueItem.downloadMinecraftAssets(assetsIndex: MinecraftVersionJson.AssetIndex, assetsIndexTarget: Path, objectsDir: Path): Boolean {
    notifyStart(assetsIndex.totalSize)
    if (!downloadFileVerified(assetsIndexTarget, assetsIndex.url, assetsIndex.sha1, HashAlgorithms.SHA1)) {
        notifyFinished(false, "Wrong response code or hash failure")
        return false
    }
    val assetsObjects = json.decodeFromString<MinecraftAssetsObjects>(assetsIndexTarget.readText())
    val chunked = assetsObjects.parsedObjects.values.shuffled().chunked(assetsObjects.parsedObjects.size / 16)
    val jobs = mutableListOf<Job>()
    val atomPos = AtomicLong(0)
    for (chunk in chunked) {
        coroutineScope {
            jobs += launch {
                for (i in chunk) {
                    val path = "${i.hash.take(2)}/${i.hash}"
                    var pos = 0L
                    if (!downloadFileVerified(objectsDir.resolve(path), "https://resources.download.minecraft.net/${path}", i.hash, HashAlgorithms.SHA1) {
                        notifyProgress(atomPos.addAndGet(it - pos))
                        pos = it
                    }) {
                        System.err.println("error")
                    }
                }
            }
        }
    }
    joinAll(*jobs.toTypedArray())
    notifyFinished()
    return true
}

suspend fun DownloadQueueItem.downloadMinecraftClientJar(versionJson: Path, target: Path): Boolean = downloadMinecraftClientJar(versionJson.readText(), target)

suspend fun DownloadQueueItem.downloadMinecraftClientJar(versionJson: String, target: Path): Boolean = downloadMinecraftClientJar(json.decodeFromString<MinecraftVersionJson>(versionJson), target)

suspend fun DownloadQueueItem.downloadMinecraftClientJar(versionJson: MinecraftVersionJson, target: Path): Boolean = downloadMinecraftJar(versionJson.downloads.client, target)

suspend fun DownloadQueueItem.downloadMinecraftServerJar(versionJson: Path, target: Path): Boolean = downloadMinecraftServerJar(versionJson.readText(), target)

suspend fun DownloadQueueItem.downloadMinecraftServerJar(versionJson: String, target: Path): Boolean = downloadMinecraftServerJar(json.decodeFromString<MinecraftVersionJson>(versionJson), target)

suspend fun DownloadQueueItem.downloadMinecraftServerJar(versionJson: MinecraftVersionJson, target: Path): Boolean = downloadMinecraftJar(versionJson.downloads.server!!, target)

suspend fun DownloadQueueItem.downloadMinecraftJar(download: MinecraftVersionJson.Downloads.Download, target: Path): Boolean {
    notifyStart(download.size.toLong())
    return downloadFileVerified(target, download.url, download.sha1, HashAlgorithms.SHA1) {
        notifyProgress(it)
    }.notifyFinishedDefault()
}

suspend fun DownloadQueueItem.downloadMinecraftClientLoggingConfig(versionJson: Path, target: Path): Boolean = downloadMinecraftClientLoggingConfig(versionJson.readText(), target)

suspend fun DownloadQueueItem.downloadMinecraftClientLoggingConfig(versionJson: String, target: Path): Boolean = downloadMinecraftClientLoggingConfig(json.decodeFromString<MinecraftVersionJson>(versionJson), target)

suspend fun DownloadQueueItem.downloadMinecraftClientLoggingConfig(versionJson: MinecraftVersionJson, target: Path): Boolean = downloadMinecraftClientLoggingConfig(versionJson.logging!!, target)

suspend fun DownloadQueueItem.downloadMinecraftClientLoggingConfig(logging: MinecraftVersionJson.Logging, target: Path): Boolean {
    notifyStart()
    return downloadFileVerified(target, logging.client.file.url, logging.client.file.sha1, HashAlgorithms.SHA1).notifyFinishedDefault()
}

suspend fun DownloadQueueItem.downloadMinecraftClientLibraries(versionJson: Path, outputDir: Path, filterForSystem: Boolean = true): Boolean = downloadMinecraftClientLibraries(versionJson.readText(), outputDir, filterForSystem)

suspend fun DownloadQueueItem.downloadMinecraftClientLibraries(versionJson: String, outputDir: Path, filterForSystem: Boolean = true): Boolean = downloadMinecraftClientLibraries(json.decodeFromString<MinecraftVersionJson>(versionJson), outputDir, filterForSystem)

suspend fun DownloadQueueItem.downloadMinecraftClientLibraries(versionJson: MinecraftVersionJson, outputDir: Path, filterForSystem: Boolean = true): Boolean = downloadMinecraftClientLibraries(versionJson.libraries, outputDir, filterForSystem)

suspend fun DownloadQueueItem.downloadMinecraftClientLibraries(libraries: List<MinecraftVersionJson.Library>, outputDir: Path, filterForSystem: Boolean = true): Boolean {
    val filtered = if (filterForSystem) {
        libraries.filter {
            if (it.rules == null) {
                return@filter true
            }
            var allowed = MinecraftVersionJson.Library.Rule.Action.DISALLOW
            it.rules.forEach { rule ->
                if (rule.os == null) {
                    allowed = rule.action
                    return@forEach
                }
                if (
                    (rule.os.name == null || rule.os.name == os) &&
                    (rule.os.version?.replace("\\\\", "\\")?.toRegex()
                        ?.matches(System.getProperty("os.version")) != false) &&
                    (rule.os.arch == null || (rule.os.arch == "x86" && x86))
                ) {
                    allowed = rule.action
                }
            }
            allowed == MinecraftVersionJson.Library.Rule.Action.ALLOW
        }
    } else {
        libraries
    }
    notifyStart(filtered.sumOf {
        (it.downloads.artifact?.size?.toLong() ?: 0) + (it.downloads.classifiers[it.natives[os]]?.size ?: 0)
    })
    val atomPos = AtomicLong(0)
    val chunkSize = filtered.size / 16
    val chunked = filtered.shuffled().chunked(if (chunkSize == 0) filtered.size else chunkSize)
    val jobs = mutableListOf<Job>()
    val outCanonicalPath = outputDir.toFile().canonicalPath
    for (chunk in chunked) {
        coroutineScope {
            jobs += launch {
                for (i in chunk) {
                    var pos = 0L
                    val artifact = i.downloads.artifact
                    if (artifact != null) {
                        val out = outputDir.resolve(artifact.path)
                        if (!out.toFile().canonicalPath.startsWith(outCanonicalPath)) {
                            throw IllegalAccessError("tried saving library outside of library directory")
                        }
                        if (!downloadFileVerified(out, artifact.url, artifact.sha1, HashAlgorithms.SHA1) {
                            notifyProgress(atomPos.addAndGet(it - pos))
                            pos = it
                        }) {
                            System.err.println("error")
                        }
                    }
                    if (i.natives.isEmpty()) {
                        continue
                    }
                    val native = i.downloads.classifiers[i.natives[os]] ?: continue
                    val outNative = outputDir.resolve(native.path)
                    if (!outNative.toFile().canonicalPath.startsWith(outCanonicalPath)) {
                        throw IllegalAccessError("tried saving library outside of library directory")
                    }
                    pos = 0
                    if (!downloadFileVerified(outNative, native.url, native.sha1, HashAlgorithms.SHA1) {
                        notifyProgress(atomPos.addAndGet(it - pos))
                        pos = it
                    }) {
                        System.err.println("error")
                    }
                }
            }
        }
    }
    joinAll(*jobs.toTypedArray())
    notifyFinished()
    return true
}

suspend fun DownloadQueueItem.listMinecraftVersions(): MinecraftVersionManifestV2? {
    notifyStart()
    val response = client.get("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json")
    if (response.status != HttpStatusCode.OK) {
        false.notifyFinishedDefault()
        return null
    }
    return response.body<MinecraftVersionManifestV2>().also { notifyFinished() }
}

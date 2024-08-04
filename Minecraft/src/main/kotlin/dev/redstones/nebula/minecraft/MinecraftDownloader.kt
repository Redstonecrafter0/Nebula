package dev.redstones.nebula.minecraft

import dev.redstones.nebula.NebulaClient
import dev.redstones.nebula.event.NebulaEvent
import dev.redstones.nebula.event.NebulaEventDownloadFinished
import dev.redstones.nebula.event.NebulaEventDownloadProgress
import dev.redstones.nebula.event.NebulaEventDownloadStarted
import dev.redstones.nebula.minecraft.dao.MinecraftAssetsObjects
import dev.redstones.nebula.minecraft.dao.MinecraftVersionJson
import dev.redstones.nebula.minecraft.dao.MinecraftVersionManifestV2
import dev.redstones.nebula.util.HashAlgorithms
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import java.nio.file.Path
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
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

suspend fun NebulaClient.downloadMinecraftVersionJson(manifest: MinecraftVersionManifestV2.Version, target: Path): Boolean {
    return downloadFileVerified(target, manifest.url, manifest.sha1, HashAlgorithms.SHA1)
}

fun NebulaClient.downloadMinecraftAssets(assetsIndex: Path, assetsIndexTarget: Path, objectsDir: Path): Flow<NebulaEvent> = downloadMinecraftAssets(assetsIndex.readText(), assetsIndexTarget, objectsDir)

fun NebulaClient.downloadMinecraftAssets(assetsIndex: String, assetsIndexTarget: Path, objectsDir: Path): Flow<NebulaEvent> = downloadMinecraftAssets(json.decodeFromString<MinecraftVersionJson>(assetsIndex), assetsIndexTarget, objectsDir)

fun NebulaClient.downloadMinecraftAssets(assetsIndex: MinecraftVersionJson, assetsIndexTarget: Path, objectsDir: Path): Flow<NebulaEvent> = downloadMinecraftAssets(assetsIndex.assetIndex, assetsIndexTarget, objectsDir)

fun NebulaClient.downloadMinecraftAssets(assetsIndex: MinecraftVersionJson.AssetIndex, assetsIndexTarget: Path, objectsDir: Path): Flow<NebulaEvent> = channelFlow {
    send(NebulaEventDownloadStarted(assetsIndex.totalSize))
    if (!downloadFileVerified(assetsIndexTarget, assetsIndex.url, assetsIndex.sha1, HashAlgorithms.SHA1)) {
        send(NebulaEventDownloadFinished(false, "Wrong response code or hash failure"))
        return@channelFlow
    }
    val assetsObjects = ConcurrentHashMap(json.decodeFromString<MinecraftAssetsObjects>(assetsIndexTarget.readText()).parsedObjects).asIterable().iterator()
    val atomPos = AtomicLong(0)
    coroutineScope {
        repeat(16) {
            launch {
                try {
                    while (true) {
                        val i = assetsObjects.next().value
                        val path = "${i.hash.take(2)}/${i.hash}"
                        var pos = 0L
                        if (!downloadFileVerified(objectsDir.resolve(path), "https://resources.download.minecraft.net/${path}", i.hash, HashAlgorithms.SHA1) {
                            send(NebulaEventDownloadProgress(atomPos.addAndGet(it - pos), assetsIndex.totalSize))
                            pos = it
                        }) {
                            System.err.println("error")
                        }
                    }
                } catch (_: NoSuchElementException) {
                    // loop ended
                }
            }
        }
    }
    send(NebulaEventDownloadFinished(true))
}

fun NebulaClient.downloadMinecraftClientJar(versionJson: Path, target: Path): Flow<NebulaEvent> = downloadMinecraftClientJar(versionJson.readText(), target)

fun NebulaClient.downloadMinecraftClientJar(versionJson: String, target: Path): Flow<NebulaEvent> = downloadMinecraftClientJar(json.decodeFromString<MinecraftVersionJson>(versionJson), target)

fun NebulaClient.downloadMinecraftClientJar(versionJson: MinecraftVersionJson, target: Path): Flow<NebulaEvent> = downloadMinecraftJar(versionJson.downloads.client, target)

fun NebulaClient.downloadMinecraftServerJar(versionJson: Path, target: Path): Flow<NebulaEvent> = downloadMinecraftServerJar(versionJson.readText(), target)

fun NebulaClient.downloadMinecraftServerJar(versionJson: String, target: Path): Flow<NebulaEvent> = downloadMinecraftServerJar(json.decodeFromString<MinecraftVersionJson>(versionJson), target)

fun NebulaClient.downloadMinecraftServerJar(versionJson: MinecraftVersionJson, target: Path): Flow<NebulaEvent> = downloadMinecraftJar(versionJson.downloads.server!!, target)

fun NebulaClient.downloadMinecraftJar(download: MinecraftVersionJson.Downloads.Download, target: Path): Flow<NebulaEvent> = flow {
    emit(NebulaEventDownloadStarted(download.size.toLong()))
    emit(downloadFileVerified(target, download.url, download.sha1, HashAlgorithms.SHA1) {
        emit(NebulaEventDownloadProgress(it, download.size.toLong()))
    }.toDefaultFinishEvent())
}

suspend fun NebulaClient.downloadMinecraftClientLoggingConfig(versionJson: Path, target: Path): Boolean = downloadMinecraftClientLoggingConfig(versionJson.readText(), target)

suspend fun NebulaClient.downloadMinecraftClientLoggingConfig(versionJson: String, target: Path): Boolean = downloadMinecraftClientLoggingConfig(json.decodeFromString<MinecraftVersionJson>(versionJson), target)

suspend fun NebulaClient.downloadMinecraftClientLoggingConfig(versionJson: MinecraftVersionJson, target: Path): Boolean = downloadMinecraftClientLoggingConfig(versionJson.logging!!, target)

suspend fun NebulaClient.downloadMinecraftClientLoggingConfig(logging: MinecraftVersionJson.Logging, target: Path): Boolean {
    return downloadFileVerified(target, logging.client.file.url, logging.client.file.sha1, HashAlgorithms.SHA1)
}

fun NebulaClient.downloadMinecraftClientLibraries(versionJson: Path, outputDir: Path, filterForSystem: Boolean = true): Flow<NebulaEvent> = downloadMinecraftClientLibraries(versionJson.readText(), outputDir, filterForSystem)

fun NebulaClient.downloadMinecraftClientLibraries(versionJson: String, outputDir: Path, filterForSystem: Boolean = true): Flow<NebulaEvent> = downloadMinecraftClientLibraries(json.decodeFromString<MinecraftVersionJson>(versionJson), outputDir, filterForSystem)

fun NebulaClient.downloadMinecraftClientLibraries(versionJson: MinecraftVersionJson, outputDir: Path, filterForSystem: Boolean = true): Flow<NebulaEvent> = downloadMinecraftClientLibraries(versionJson.libraries, outputDir, filterForSystem)

fun NebulaClient.downloadMinecraftClientLibraries(libraries: List<MinecraftVersionJson.Library>, outputDir: Path, filterForSystem: Boolean = true): Flow<NebulaEvent> = channelFlow {
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
    val totalSize = filtered.sumOf {
        (it.downloads.artifact?.size?.toLong() ?: 0) + (it.downloads.classifiers[it.natives[os]]?.size ?: 0)
    }
    send(NebulaEventDownloadStarted(totalSize))
    val atomPos = AtomicLong(0)
    val chunkSize = if (filtered.size / 16 == 0) filtered.size else filtered.size / 16
    val outCanonicalPath = outputDir.toFile().canonicalPath
    val iterator = Collections.synchronizedSet(filtered.toSet()).iterator()
    coroutineScope {
        repeat(chunkSize) {
            launch {
                try {
                    while (true) {
                        val library = iterator.next()
                        var pos = 0L
                        val artifact = library.downloads.artifact
                        if (artifact != null) {
                            val out = outputDir.resolve(artifact.path)
                            if (!out.toFile().canonicalPath.startsWith(outCanonicalPath)) {
                                throw IllegalAccessError("tried saving library outside of library directory")
                            }
                            if (!downloadFileVerified(out, artifact.url, artifact.sha1, HashAlgorithms.SHA1) {
                                send(NebulaEventDownloadProgress(atomPos.addAndGet(it - pos), totalSize))
                                pos = it
                            }) {
                                System.err.println("error")
                            }
                        }
                        if (library.natives.isEmpty()) {
                            continue
                        }
                        val native = library.downloads.classifiers[library.natives[os]] ?: continue
                        val outNative = outputDir.resolve(native.path)
                        if (!outNative.toFile().canonicalPath.startsWith(outCanonicalPath)) {
                            throw IllegalAccessError("tried saving library outside of library directory")
                        }
                        pos = 0
                        if (!downloadFileVerified(outNative, native.url, native.sha1, HashAlgorithms.SHA1) {
                            send(NebulaEventDownloadProgress(atomPos.addAndGet(it - pos), totalSize))
                            pos = it
                        }) {
                            System.err.println("error")
                        }
                    }
                } catch (_: NoSuchElementException) {
                    // loop ended
                }
            }
        }
    }
    send(NebulaEventDownloadFinished(true))
}

suspend fun NebulaClient.listMinecraftVersions(): MinecraftVersionManifestV2? {
    val response = client.get("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json")
    if (response.status != HttpStatusCode.OK) {
        return null
    }
    return response.body<MinecraftVersionManifestV2>()
}

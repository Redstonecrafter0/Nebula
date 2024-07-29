package dev.redstones.nebula.jdk

import dev.redstones.nebula.DownloadQueueItem
import dev.redstones.nebula.archive.downloadFileUnpacked
import dev.redstones.nebula.archive.downloadFileVerifiedUnpacked
import dev.redstones.nebula.jdk.dao.*
import dev.redstones.nebula.util.HashAlgorithms
import dev.redstones.nebula.util.moveToSafely
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

suspend fun DownloadQueueItem.listJdkDistributions(): List<FooJayDistribution>? {
    notifyStart()
    val response = client.get("https://api.foojay.io/disco/v3.0/distributions?include_versions=true&include_synonyms=true")
    if (response.status != HttpStatusCode.OK) {
        false.notifyFinishedDefault()
        return null
    }
    return (response.body<FooJayDistributionsResponse>().result.filterNotNull()).also { notifyFinished() }
}

suspend fun DownloadQueueItem.listJdkMajorVersions(ea: Boolean = false, ga: Boolean = true): List<FooJayMajorVersion>? {
    notifyStart()
    val response = client.get("https://api.foojay.io/disco/v3.0/major_versions?ea=$ea&ga=$ga&include_versions=false")
    if (response.status != HttpStatusCode.OK) {
        false.notifyFinishedDefault()
        return null
    }
    return (response.body<FooJayMajorVersionsResponse>().result.filterNotNull()).also { notifyFinished() }
}

/**
 * @param distribution unsafe input. sanitization required.
 * @param version unsafe input. sanitization required.
 * */
suspend fun DownloadQueueItem.listJdkPackages(majorVersion: Int, distribution: String? = null, version: String? = null, ea: Boolean = false, ga: Boolean = true, bitness: Int? = null, freeForProductionUse: Boolean = true): List<FooJayPackage>? {
    notifyStart()
    val response = client.get("https://api.foojay.io/disco/v3.0/packages?jdk_version=$majorVersion${if (version != null) "&version=$version" else ""}${if (distribution != null) "&distribution=${URLEncoder.encode(distribution, Charsets.UTF_8)}" else ""}&archive_type=tar&archive_type=tar.gz&archive_type=tgz&archive_type=zip&package_type=jdk${if (ga) "&release_status=ga" else ""}${if (ea) "&release_status=ea" else ""}${
        when (bitness) {
            32, 64 -> "&bitness=$bitness"
            else -> ""
        }
    }&directly_downloadable=true${if (freeForProductionUse) "&free_to_use_in_production=true" else ""}")
    if (response.status != HttpStatusCode.OK) {
        false.notifyFinishedDefault()
        return null
    }
    return (response.body<FooJayPackagesResponse>().result.filterNotNull()).also { notifyFinished() }
}

private suspend fun DownloadQueueItem.resolveJdkPackageInfo(jdkPackage: FooJayPackage): FooJayPackageInfo? {
    require(jdkPackage.directlyDownloadable) { "package is not directly downloadable" }
    val response = client.get(jdkPackage.links.pkgInfoUri)
    if (response.status != HttpStatusCode.OK) {
        return null
    }
    return response.body<FooJayPackageInfoResponse>().result.firstNotNullOfOrNull { it }
}

@OptIn(ExperimentalPathApi::class)
suspend fun DownloadQueueItem.downloadJdkPackage(jdkPackage: FooJayPackage, target: Path): Boolean {
    require(jdkPackage.directlyDownloadable) { "package is not directly downloadable" }
    notifyStart(jdkPackage.size)
    if (target.exists() && target.isDirectory()) {
        notifyProgress(jdkPackage.size)
        return true.notifyFinishedDefault()
    }
    val packageInfo = resolveJdkPackageInfo(jdkPackage) ?: return false.notifyFinishedDefault()
    val tmpDir = withContext(Dispatchers.IO) {
        Files.createTempDirectory("nebula-")
    }
    val algorithm = HashAlgorithms[packageInfo.checksumType]
    val success = if (algorithm == null) {
        downloadFileUnpacked(tmpDir, packageInfo.directDownloadUri, jdkPackage.archiveType, {
            headers.clear()
            headers {
                header("Accept", "*/*")
                header("User-Agent", "curl/8.8.0")
            }
        }) {
            notifyProgress(it)
        }
    } else {
        downloadFileVerifiedUnpacked(tmpDir, packageInfo.directDownloadUri, packageInfo.checksum, algorithm, jdkPackage.archiveType, {
            headers.clear()
            headers {
                header("Accept", "*/*")
                header("User-Agent", "curl/8.8.0")
            }
        }) {
            notifyProgress(it)
        }
    }
    val innerDirs = tmpDir.toFile().list { dir, _ -> dir.isDirectory }
    val tmpPath = if (innerDirs?.size == 1) {
        tmpDir.resolve(innerDirs.first())
    } else {
        tmpDir
    }
    target.deleteRecursively()
    target.parent.toFile().mkdirs()
    tmpPath.moveToSafely(target)
    return success.notifyFinishedDefault()
}

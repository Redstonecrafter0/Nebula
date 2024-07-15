package dev.redstones.nebula.jdk

import dev.redstones.nebula.DownloadQueueItem
import dev.redstones.nebula.archive.downloadFileVerifiedUnpacked
import dev.redstones.nebula.jdk.dao.*
import dev.redstones.nebula.util.HashAlgorithms
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*

suspend fun DownloadQueueItem.listJdkDistributions(): List<FooJayDistribution>? {
    notifyStart()
    val response = client.get("https://api.foojay.io/disco/v3.0/distributions?include_versions=true&include_synonyms=true")
    if (response.status != HttpStatusCode.OK) {
        false.notifyFinishedDefault()
        return null
    }
    return (response.body<FooJayDistributionsResponse>().result).also { notifyFinished() }
}

suspend fun DownloadQueueItem.listJdkMajorVersions(ea: Boolean = false, ga: Boolean = true): List<FooJayMajorVersion>? {
    notifyStart()
    val response = client.get("https://api.foojay.io/disco/v3.0/major_versions?ea=$ea&ga=$ga&include_versions=false")
    if (response.status != HttpStatusCode.OK) {
        false.notifyFinishedDefault()
        return null
    }
    return (response.body<FooJayMajorVersionsResponse>().result).also { notifyFinished() }
}

/**
 * @param distribution unsafe input. sanitization required.
 * @param version unsafe input. sanitization required.
 * */
suspend fun DownloadQueueItem.listJdkPackages(majorVersion: Int, distribution: String? = null, version: String? = null, ea: Boolean = false, ga: Boolean = true, bitness: Int? = null, freeForProductionUse: Boolean = true): List<FooJayPackage>? {
    notifyStart()
    val response = client.get("https://api.foojay.io/disco/v3.0/packages?jdk_version=$majorVersion${if (version != null) "&version=$version" else ""}${if (distribution != null) "&distribution=$distribution" else ""}&archive_type=tar&archive_type=tar.gz&archive_type=tgz&archive_type=zip&package_type=jdk${if (ga) "&release_status=ga" else ""}${if (ea) "&release_status=ea" else ""}${
        when (bitness) {
            32, 64 -> "&bitness=$bitness"
            else -> ""
        }
    }&directly_downloadable=true${if (freeForProductionUse) "&free_to_use_in_production=true" else ""}")
    if (response.status != HttpStatusCode.OK) {
        false.notifyFinishedDefault()
        return null
    }
    return (response.body<FooJayPackagesResponse>().result).also { notifyFinished() }
}

suspend fun DownloadQueueItem.downloadJdkPackage(jdkPackage: FooJayPackage, target: Path): Boolean {
    require(jdkPackage.directlyDownloadable != "yes") { "package is not directly downloadable" }
    notifyStart(jdkPackage.size)
    return downloadFileVerifiedUnpacked(target, jdkPackage.links.pkgDownloadRedirect, jdkPackage.id, HashAlgorithms.SHA256, jdkPackage.archiveType) {
        notifyProgress(it)
    }.notifyFinishedDefault()
}

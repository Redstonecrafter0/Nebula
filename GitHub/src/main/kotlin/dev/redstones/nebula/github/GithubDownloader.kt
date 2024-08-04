package dev.redstones.nebula.github

import dev.redstones.nebula.NebulaClient
import dev.redstones.nebula.event.NebulaEvent
import dev.redstones.nebula.event.NebulaEventDownloadProgress
import dev.redstones.nebula.event.NebulaEventDownloadStarted
import dev.redstones.nebula.github.dao.GitHubRelease
import dev.redstones.nebula.github.dao.GitHubSearchResults
import dev.redstones.nebula.util.moveToSafely
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*

// page=515>; rel="last"
private val lastPageRegex = "(?<=page=)\\d+(?=>; rel=\"last\")".toRegex()

private val HttpResponse.lastPage: Int?
    get() {
        val link = headers["Link"]
        return if (link != null) {
            lastPageRegex.find(link)?.value?.toInt()
        } else {
            null
        }
    }

/**
 * @param query unsafe input. sanitization required.
 * */
suspend fun NebulaClient.searchGitHub(query: String, perPage: Int = 30, page: Int = 1, token: String? = null): Pair<GitHubSearchResults, Int>? {
    require(perPage in 1..100) { "perPage must be in 1..100" }
    val response = client.get("https://api.github.com/search/repositories?per_page=$perPage&page=$page&q=$query") {
        if (token != null) {
            headers {
                header("Authorization", "Bearer $token")
            }
        }
    }
    if (response.status != HttpStatusCode.OK) {
        return null
    }
    return (response.body<GitHubSearchResults>() to (response.lastPage ?: page))
}

/**
 * @param fullName unsafe input. sanitization required.
 * */
suspend fun NebulaClient.listGitHubReleases(fullName: String, perPage: Int = 30, page: Int = 1, token: String? = null): Pair<List<GitHubRelease>, Int>? {
    val response = client.get("https://api.github.com/repos/$fullName/releases?per_page=$perPage&page=$page") {
        if (token != null) {
            headers {
                header("Authorization", "Bearer $token")
            }
        }
    }
    if (response.status != HttpStatusCode.OK) {
        return null
    }
    return (response.body<List<GitHubRelease>>() to (response.lastPage ?: page))
}

fun NebulaClient.downloadGitHubReleaseAsset(asset: GitHubRelease.Asset, target: Path, token: String? = null): Flow<NebulaEvent> = flow {
    emit(NebulaEventDownloadStarted(asset.size))
    downloadFileUnverified(target, asset.browserDownloadUrl, asset.size, {
        if (token != null) {
            headers {
                header("Authorization", "Bearer $token")
            }
        }
    }) {
        emit(NebulaEventDownloadProgress(it, asset.size))
    }.let { emit(it.toDefaultFinishEvent()) }
}

fun NebulaClient.downloadGitHubReleaseAssetVerified(asset: GitHubRelease.Asset, target: Path, hash: String, algorithm: String, token: String? = null): Flow<NebulaEvent> = flow {
    emit(NebulaEventDownloadStarted(asset.size))
    downloadFileVerified(target, asset.browserDownloadUrl, hash, algorithm, {
        if (token != null) {
            headers {
                header("Authorization", "Bearer $token")
            }
        }
    }) {
        emit(NebulaEventDownloadProgress(it, asset.size))
    }.let { emit(it.toDefaultFinishEvent()) }
}

@OptIn(ExperimentalPathApi::class)
fun NebulaClient.downloadGitHubRelease(release: GitHubRelease, target: Path, token: String? = null): Flow<NebulaEvent> = flow {
    val totalSize = release.assets.sumOf { it.size }
    emit(NebulaEventDownloadStarted(totalSize))
    val tmpPath = withContext(Dispatchers.IO) {
        Files.createTempDirectory("nebula-")
    }
    val tmpPathName = tmpPath.toFile().canonicalPath
    var pos = 0L
    for (asset in release.assets) {
        val path = tmpPath.resolve(asset.name)
        if (!path.toFile().canonicalPath.startsWith(tmpPathName)) {
            throw IllegalStateException("tried saving file outside of allowed directory")
        }
        var innerPos = 0L
        if (!downloadFileUnverified(path, asset.browserDownloadUrl, asset.size, {
            if (token != null) {
                headers {
                    header("Authorization", "Bearer $token")
                }
            }
        }) {
            pos += it - innerPos
            innerPos = it
            emit(NebulaEventDownloadProgress(pos, totalSize))
        }) {
            emit(false.toDefaultFinishEvent())
            return@flow
        }
    }
    if (target.exists() && target.isDirectory()) {
        target.deleteRecursively()
    }
    target.deleteIfExists()
    target.parent.toFile().mkdirs()
    tmpPath.moveToSafely(target)
    emit(true.toDefaultFinishEvent())
}

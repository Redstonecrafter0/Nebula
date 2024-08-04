package dev.redstones.nebula.k3d

import dev.redstones.nebula.NebulaClient
import dev.redstones.nebula.event.NebulaEvent
import dev.redstones.nebula.event.NebulaEventDownloadFinished
import dev.redstones.nebula.event.NebulaEventDownloadStarted
import dev.redstones.nebula.github.dao.GitHubRelease
import dev.redstones.nebula.github.downloadGitHubReleaseAsset
import dev.redstones.nebula.github.downloadGitHubReleaseAssetVerified
import dev.redstones.nebula.github.listGitHubReleases
import dev.redstones.nebula.util.HashAlgorithms
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText

suspend fun NebulaClient.listK3dVersions(perPage: Int = 1, page: Int = 1, token: String? = null): Pair<List<GitHubRelease>, Int>? {
    return listGitHubReleases("k3d-io/k3d", perPage, page, token)
}

/**
 * @param os `windows` or `linux` or `null` to detect automatically
 * @param arch `amd64` or `arm` or `arm64` or `aarch64` or `null` to detect automatically
 * */
suspend fun NebulaClient.downloadK3d(release: GitHubRelease, target: Path, os: String? = null, arch: String? = null, token: String? = null): Flow<NebulaEvent> {
    val javaOs = System.getProperty("os.name").lowercase()
    val os = os ?: when {
        javaOs.startsWith("windows") -> "windows"
        javaOs.startsWith("linux") -> "linux"
        else -> return flow {
            emit(NebulaEventDownloadStarted())
            emit(NebulaEventDownloadFinished(false, "platform not supported"))
        }
    }
    val arch = (arch ?: System.getProperty("os.arch")).let {
        if (it == "aarch64") {
            "arm64"
        } else {
            it
        }
    }
    val name = "k3d-$os-$arch" + if (os == "windows") ".exe" else ""
    val asset = release.assets.firstOrNull { it.name == name }
    if (asset == null || (arch != null && "." in arch)) {
        return flow {
            emit(NebulaEventDownloadStarted())
            emit(NebulaEventDownloadFinished(false, "platform not supported"))
        }
    }

    val checksumFile = withContext(Dispatchers.IO) {
        Files.createTempFile("nebula-", null)
    }
    val finishEvent = downloadGitHubReleaseAsset(release.assets.first { it.name == "checksums.txt" }, checksumFile, token).toList().last() as NebulaEventDownloadFinished
    if (!finishEvent.success) {
        return flow {
            emit(NebulaEventDownloadStarted(0))
            emit(finishEvent)
        }
    }
    val hashes = checksumFile.readText().split("\n")
        .filter { it.isNotEmpty() }
        .map { it.split("  _dist/") }
        .associate { it[1] to it[0] }

    val hash = hashes[name] ?: return flow {
        emit(NebulaEventDownloadStarted(0))
        emit(NebulaEventDownloadFinished(false, "hash failure. checksum not found"))
    }
    return downloadGitHubReleaseAssetVerified(asset, target, hash, HashAlgorithms.SHA256, token)
}

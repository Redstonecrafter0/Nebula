package dev.redstones.nebula.k3d

import dev.redstones.nebula.DownloadQueueItem
import dev.redstones.nebula.github.dao.GitHubRelease
import dev.redstones.nebula.github.downloadGitHubReleaseAsset
import dev.redstones.nebula.github.downloadGitHubReleaseAssetVerified
import dev.redstones.nebula.github.listGitHubReleases
import dev.redstones.nebula.util.HashAlgorithms
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText

suspend fun DownloadQueueItem.listK3dVersions(perPage: Int = 1, page: Int = 1, token: String? = null): Pair<List<GitHubRelease>, Int>? {
    return listGitHubReleases("k3d-io/k3d", perPage, page, token)
}

/**
 * @param os `windows` or `linux` or `null` to detect automatically
 * @param arch `amd64` or `arm` or `arm64` or `aarch64` or `null` to detect automatically
 * */
suspend fun DownloadQueueItem.downloadK3d(release: GitHubRelease, target: Path, os: String? = null, arch: String? = null, token: String? = null): Boolean {
    val javaOs = System.getProperty("os.name").lowercase()
    val os = os ?: when {
        javaOs.startsWith("windows") -> "windows"
        javaOs.startsWith("linux") -> "linux"
        else -> {
            notifyStart()
            notifyFinished(false, "platform not supported")
            return false
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
        notifyStart()
        notifyFinished(false, "platform not supported")
        return false
    }

    val checksumFile = withContext(Dispatchers.IO) {
        Files.createTempFile("nebula-", null)
    }
    notifyStart(asset.size)
    if (!downloadGitHubReleaseAsset(release.assets.first { it.name == "checksums.txt" }, checksumFile, token, true)) {
        return false.notifyFinishedDefault()
    }
    val hashes = checksumFile.readText().split("\n")
        .filter { it.isNotEmpty() }
        .map { it.split("  _dist/") }
        .associate { it[1] to it[0] }

    val hash = hashes[name]
    if (hash == null) {
        notifyFinished(false, "hash failure. checksum not found")
        return false
    }
    return downloadGitHubReleaseAssetVerified(asset, target, hash, HashAlgorithms.SHA256, token)
}

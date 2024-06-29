package dev.redstones.nebula.cloudflared

import dev.redstones.nebula.DownloadQueueItem
import dev.redstones.nebula.github.dao.GitHubRelease
import dev.redstones.nebula.github.downloadGitHubReleaseAssetVerified
import dev.redstones.nebula.github.listGitHubReleases
import dev.redstones.nebula.util.HashAlgorithms
import java.nio.file.Path

suspend fun DownloadQueueItem.listCloudflaredVersions(perPage: Int = 1, page: Int = 1, token: String? = null): Pair<List<GitHubRelease>, Int>? {
    return listGitHubReleases("cloudflare/cloudflared", perPage, page, token)
}

/**
 * @param os `windows` or `linux` or `null` to detect automatically
 * @param arch `amd64` or `arm` or `arm64` or `aarch64` or `null` to detect automatically
 * */
suspend fun DownloadQueueItem.downloadCloudflared(release: GitHubRelease, target: Path, os: String? = null, arch: String? = null, token: String? = null): Boolean {
    val hashes = release.body.split("\n")
        .filter { it.startsWith("cloudflared-") }
        .map { it.split(": ") }
        .associate { it[0] to it[1] }
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
    val name = "cloudflared-$os-${arch ?: System.getProperty("os.arch")}" + if (os == "windows") ".exe" else ""
    val hash = hashes[name]
    val asset = release.assets.firstOrNull { it.name == name }
    if (asset == null || hash == null || (arch != null && "." in arch)) {
        notifyStart()
        notifyFinished(false, "platform not supported")
        return false
    }
    return downloadGitHubReleaseAssetVerified(asset, target, hash, HashAlgorithms.SHA256, token)
}

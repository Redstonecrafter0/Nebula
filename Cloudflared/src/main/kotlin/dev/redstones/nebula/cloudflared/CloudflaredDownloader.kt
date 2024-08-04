package dev.redstones.nebula.cloudflared

import dev.redstones.nebula.NebulaClient
import dev.redstones.nebula.event.NebulaEvent
import dev.redstones.nebula.event.NebulaEventDownloadFinished
import dev.redstones.nebula.event.NebulaEventDownloadStarted
import dev.redstones.nebula.github.dao.GitHubRelease
import dev.redstones.nebula.github.downloadGitHubReleaseAssetVerified
import dev.redstones.nebula.github.listGitHubReleases
import dev.redstones.nebula.util.HashAlgorithms
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.nio.file.Path

suspend fun NebulaClient.listCloudflaredVersions(perPage: Int = 1, page: Int = 1, token: String? = null): Pair<List<GitHubRelease>, Int>? {
    return listGitHubReleases("cloudflare/cloudflared", perPage, page, token)
}

/**
 * @param os `windows` or `linux` or `null` to detect automatically
 * @param arch `amd64` or `arm` or `arm64` or `aarch64` or `null` to detect automatically
 * */
fun NebulaClient.downloadCloudflared(release: GitHubRelease, target: Path, os: String? = null, arch: String? = null, token: String? = null): Flow<NebulaEvent> {
    val hashes = release.body.split("\n")
        .filter { it.startsWith("cloudflared-") }
        .map { it.split(": ") }
        .associate { it[0] to it[1] }
    val javaOs = System.getProperty("os.name").lowercase()
    val os = os ?: when {
        javaOs.startsWith("windows") -> "windows"
        javaOs.startsWith("linux") -> "linux"
        else -> return flow {
            emit(NebulaEventDownloadStarted())
            emit(NebulaEventDownloadFinished(false, "platform not supported"))
        }
    }
    val name = "cloudflared-$os-${arch ?: System.getProperty("os.arch")}" + if (os == "windows") ".exe" else ""
    val hash = hashes[name]
    val asset = release.assets.firstOrNull { it.name == name }
    if (asset == null || hash == null || (arch != null && "." in arch)) {
        return flow {
            emit(NebulaEventDownloadStarted())
            emit(NebulaEventDownloadFinished(false, "platform not supported"))
        }
    }
    return downloadGitHubReleaseAssetVerified(asset, target, hash, HashAlgorithms.SHA256, token)
}

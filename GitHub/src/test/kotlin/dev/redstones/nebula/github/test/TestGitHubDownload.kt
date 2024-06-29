package dev.redstones.nebula.github.test

import dev.redstones.nebula.DownloadManager
import dev.redstones.nebula.github.downloadGitHubRelease
import dev.redstones.nebula.github.downloadGitHubReleaseAsset
import dev.redstones.nebula.github.listGitHubReleases
import dev.redstones.nebula.github.searchGitHub
import io.ktor.client.engine.java.*
import me.tongfei.progressbar.ProgressBar
import java.nio.file.Path

suspend fun main() {
    val steps = ProgressBar("Steps", 4)
    var subBar: ProgressBar? = null
    val downloader = DownloadManager(Java)
    downloader.enqueue {
        maxStep = 4
        steps.extraMessage = "Loading versions"
        steps.step()
        val (result, lastPage) = searchGitHub("topic:schizoid+schizoid")!!
        steps.extraMessage = "Loading releases"
        val (releases, lastReleasePage) = listGitHubReleases(result.items.first().fullName)!!
        steps.extraMessage = "Downloading releases"
        downloadGitHubRelease(releases.first(), Path.of("test/github/release"))
        val asset = releases.first().assets.first()
        val assetDir = Path.of("test/github/asset")
        val assetTarget = assetDir.resolve(asset.name)
        if (!assetTarget.toFile().canonicalPath.startsWith(assetDir.toFile().canonicalPath)) {
            throw IllegalStateException("trying to write outside of allowed path")
        }
        steps.extraMessage = "Downloading single asset"
        downloadGitHubReleaseAsset(asset, assetTarget)
        println("Last page $lastPage")
        println("Last page $lastReleasePage")
    }.addEventListener {
        onStart { step: Int, _: Int, max: Long? ->
            subBar = if (max == null) {
                null
            } else {
                ProgressBar("Downloading step $step", max)
            }
        }
        onProgress {
            if (it != null) {
                subBar?.stepTo(it)
            }
        }
        onFinished { success, message ->
            steps.step()
            subBar?.close()
        }
    }
    downloader.runSingle()
}

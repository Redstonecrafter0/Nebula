package dev.redstones.nebula.github.test

import dev.redstones.nebula.DownloadWatcher
import dev.redstones.nebula.event.NebulaEventDownloadFinished
import dev.redstones.nebula.event.NebulaEventDownloadProgress
import dev.redstones.nebula.event.NebulaEventDownloadStarted
import dev.redstones.nebula.github.downloadGitHubRelease
import dev.redstones.nebula.github.downloadGitHubReleaseAsset
import dev.redstones.nebula.github.listGitHubReleases
import dev.redstones.nebula.github.searchGitHub
import dev.redstones.nebula.installNebula
import dev.redstones.nebula.toNebula
import dev.redstones.nebula.watch
import io.ktor.client.*
import io.ktor.client.engine.java.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import me.tongfei.progressbar.ProgressBar
import java.nio.file.Path

suspend fun main() {
    var progressBar: ProgressBar? = null
    val watcher = DownloadWatcher()
    val client = HttpClient(Java) {
        installNebula()
    }.toNebula()
    coroutineScope {
        val job = launch {
            watcher.subscribe().collect {
                println(it)
            }
        }
        launch {
            val (result, lastPage) = client.searchGitHub("topic:schizoid+schizoid")!!
            val (releases, lastReleasePage) = client.listGitHubReleases(result.items.first().fullName)!!
            println("Last page $lastPage")
            println("Last page $lastReleasePage")
            client.downloadGitHubRelease(releases.first(), Path.of("test/github/release")).collect {
                when (it) {
                    is NebulaEventDownloadStarted -> progressBar = ProgressBar("Downloading release", it.totalSize ?: 0)
                    is NebulaEventDownloadProgress -> progressBar!!.stepTo(it.pos)
                    is NebulaEventDownloadFinished -> progressBar!!.close()
                }
            }
            val asset = releases.first().assets.first()
            val assetDir = Path.of("test/github/asset")
            val assetTarget = assetDir.resolve(asset.name)
            if (!assetTarget.toFile().canonicalPath.startsWith(assetDir.toFile().canonicalPath)) {
                throw IllegalStateException("trying to write outside of allowed path")
            }
            client.downloadGitHubReleaseAsset(asset, assetTarget).watch(watcher).collect {}
            job.cancel()
        }
    }
}

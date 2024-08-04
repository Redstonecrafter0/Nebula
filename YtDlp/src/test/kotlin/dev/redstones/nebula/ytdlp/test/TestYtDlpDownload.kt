package dev.redstones.nebula.ytdlp.test

import dev.redstones.nebula.DownloadWatcher
import dev.redstones.nebula.installNebula
import dev.redstones.nebula.toNebula
import dev.redstones.nebula.watch
import dev.redstones.nebula.ytdlp.downloadYtDlp
import dev.redstones.nebula.ytdlp.listYtDlpVersions
import io.ktor.client.*
import io.ktor.client.engine.java.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import me.tongfei.progressbar.ProgressBar
import java.nio.file.Path

suspend fun main() {
    val progressBar = ProgressBar("Downloading", 0)
    val watcher = DownloadWatcher()
    val client = HttpClient(Java) {
        installNebula()
    }.toNebula()
    coroutineScope {
        val job = launch {
            watcher.subscribe().collect {
                if (it != null) {
                    progressBar.stepTo(it.pos)
                    progressBar.maxHint(it.totalSize ?: 0)
                }
            }
        }
        launch {
            val version = client.listYtDlpVersions()!!.first.first()
            client.downloadYtDlp(version, Path.of("test/ytdlp/yt-dlp" + if (System.getProperty("os.name").lowercase().startsWith("windows")) ".exe" else "")).watch(watcher).collect {}
            job.cancel()
        }
    }
}

package dev.redstones.nebula.ytdlp.test

import dev.redstones.nebula.DownloadManager
import dev.redstones.nebula.ytdlp.downloadYtDlp
import dev.redstones.nebula.ytdlp.listYtDlpVersions
import io.ktor.client.engine.java.*
import me.tongfei.progressbar.ProgressBar
import java.nio.file.Path

suspend fun main() {
    val steps = ProgressBar("Steps", 2)
    var subBar: ProgressBar? = null
    val downloader = DownloadManager(Java)
    downloader.addEventListener {
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
    downloader.download {
        maxStep = 2
        steps.extraMessage = "Loading releases"
        steps.step()
        val version = listYtDlpVersions()!!.first.first()
        steps.extraMessage = "Downloading releases"
        downloadYtDlp(version, Path.of("test/ytdlp/yt-dlp" + if (System.getProperty("os.name").lowercase().startsWith("windows")) ".exe" else ""))
    }
}

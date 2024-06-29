package dev.redstones.nebula.k3d.test

import dev.redstones.nebula.DownloadManager
import dev.redstones.nebula.k3d.downloadK3d
import dev.redstones.nebula.k3d.listK3dVersions
import io.ktor.client.engine.java.*
import me.tongfei.progressbar.ProgressBar
import java.nio.file.Path

suspend fun main() {
    val steps = ProgressBar("Steps", 2)
    var subBar: ProgressBar? = null
    val downloader = DownloadManager(Java)
    downloader.enqueue {
        maxStep = 2
        steps.extraMessage = "Loading releases"
        steps.step()
        val version = listK3dVersions()!!.first.first()
        steps.extraMessage = "Downloading releases"
        downloadK3d(version, Path.of("test/k3d/k3d" + if (System.getProperty("os.name").lowercase().startsWith("windows")) ".exe" else ""))
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

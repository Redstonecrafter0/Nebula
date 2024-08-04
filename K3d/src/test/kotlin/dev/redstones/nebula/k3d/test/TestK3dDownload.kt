package dev.redstones.nebula.k3d.test

import dev.redstones.nebula.DownloadWatcher
import dev.redstones.nebula.installNebula
import dev.redstones.nebula.k3d.downloadK3d
import dev.redstones.nebula.k3d.listK3dVersions
import dev.redstones.nebula.toNebula
import dev.redstones.nebula.watch
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
            val version = client.listK3dVersions()!!.first.first()
            client.downloadK3d(version, Path.of("test/k3d/k3d" + if (System.getProperty("os.name").lowercase().startsWith("windows")) ".exe" else "")).watch(watcher).collect {}
            job.cancel()
        }
    }
}

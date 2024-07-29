package dev.redstones.nebula.jdk.test

import dev.redstones.nebula.DownloadManager
import dev.redstones.nebula.jdk.dao.FooJayPackage
import dev.redstones.nebula.jdk.downloadJdkPackage
import dev.redstones.nebula.jdk.listJdkDistributions
import dev.redstones.nebula.jdk.listJdkMajorVersions
import dev.redstones.nebula.jdk.listJdkPackages
import io.ktor.client.engine.java.*
import me.tongfei.progressbar.ProgressBar
import java.nio.file.Path

suspend fun main() {
    val steps = ProgressBar("Steps", 4)
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
        steps.extraMessage = "Loading distributions"
        steps.step()
        val distributions = listJdkDistributions()!!
        steps.extraMessage = "Loading releases"
        val versions = listJdkMajorVersions()!!
        steps.extraMessage = "Loading packages"
        val packages = mutableMapOf<Pair<String, Int>, FooJayPackage>()
        for (i in distributions.map { it.name }) {
            for (j in versions) {
                packages += (i to j.majorVersion) to (listJdkPackages(j.majorVersion, i)?.firstOrNull { it.operatingSystem == "linux" || it.operatingSystem == "windows" } ?: continue)
            }
        }
        steps.extraMessage = "Downloading packages"
        for (i in packages) {
            downloadJdkPackage(i.value, Path.of("test/jdk/${i.key.first}/${i.key.second}/${i.value.javaVersion}"))
        }
    }
}

package dev.redstones.nebula

import dev.redstones.nebula.util.HashAlgorithms
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.*

class DownloadQueueItem(val client: HttpClient, val download: suspend DownloadQueueItem.() -> Unit) {

    internal var manager: DownloadManager? = null

    fun notifyStart(max: Long? = null) {
    }

    fun notifyProgress(pos: Long? = null) {
    }

    fun notifySuccess() {
    }

    fun notifyRetry() {
    }

    fun notifyFailure(final: Boolean, reason: String) {
    }

    @OptIn(ExperimentalStdlibApi::class)
    suspend fun HttpStatement.executeVerify(location: Path, hash: String, algorithm: String): Boolean {
        return execute { response ->
            if (response.status != HttpStatusCode.OK) {
                return@execute false
            }
            val digest = MessageDigest.getInstance(algorithm)
            val channel = response.bodyAsChannel()
            while (!channel.isClosedForRead) {
                val packet = channel.readRemaining(8192L)
                while (!packet.isEmpty) {
                    val bytes = packet.readBytes()
                    location.appendBytes(bytes)
                    digest.update(bytes)
                }
            }
            if (!digest.digest().contentEquals(hash.hexToByteArray())) {
                location.deleteIfExists()
                return@execute false
            }
            return@execute true
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    suspend fun downloadFileVerified(target: Path, url: String, hash: String, algorithm: String): Boolean {
        if (target.exists() && target.isRegularFile()) {
            if (HashAlgorithms.getFileHash(target, algorithm).contentEquals(hash.hexToByteArray())) {
                return true
            }
        }
        val tmpFile = withContext(Dispatchers.IO) {
            Files.createTempFile("nebula-", null)
        }
        if (!client.prepareGet(url).executeVerify(tmpFile, hash, algorithm)) {
            tmpFile.deleteIfExists()
            return false
        }
        target.deleteIfExists()
        target.parent.toFile().mkdirs()
        tmpFile.moveTo(target)
        return true
    }

}

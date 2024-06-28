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
    internal var listeners = emptyList<DownloadEventListener>()
    private var step = -1
    var maxStep = 0

    suspend fun notifyStart(max: Long? = null) {
        step++
        listeners.forEach { it.onStart(step, maxStep, max) }
    }

    suspend fun notifyProgress(pos: Long? = null) {
        listeners.forEach { it.onProgress(pos) }
    }

    suspend fun onRetry() {
        listeners.forEach { it.onRetry() }
    }

    suspend fun notifyFinished(success: Boolean = true, reason: String? = null) {
        listeners.forEach { it.onFinished(success, reason) }
    }

    fun addEventListener(block: DownloadEventListener.Builder.() -> Unit) {
        val builder = DownloadEventListener.Builder()
        builder.block()
        addEventListener(builder.build())
    }

    fun addEventListener(listener: DownloadEventListener) {
        listeners += listener
    }

    suspend fun Boolean.notifyFinishedDefault(): Boolean {
        notifyFinished(this, if (this) "Wrong response code, size or hash failure" else null)
        return this
    }

    @OptIn(ExperimentalStdlibApi::class)
    suspend fun HttpStatement.executeVerify(location: Path, hash: String, algorithm: String, progressCallback: suspend (Long) -> Unit = {}): Boolean {
        return execute { response ->
            if (response.status != HttpStatusCode.OK) {
                return@execute false
            }
            var pos = 0L
            val digest = MessageDigest.getInstance(algorithm)
            val channel = response.bodyAsChannel()
            while (!channel.isClosedForRead) {
                val packet = channel.readRemaining(8192L)
                while (!packet.isEmpty) {
                    val bytes = packet.readBytes()
                    location.appendBytes(bytes)
                    digest.update(bytes)
                    pos += bytes.size
                    progressCallback(pos)
                }
            }
            if (!digest.digest().contentEquals(hash.hexToByteArray())) {
                location.deleteIfExists()
                return@execute false
            }
            return@execute true
        }
    }

    suspend fun HttpStatement.executeUnverified(location: Path, expectedFileSize: Long? = null, progressCallback: suspend (Long) -> Unit = {}): Boolean {
        return execute { response ->
            if (response.status != HttpStatusCode.OK) {
                return@execute false
            }
            var pos = 0L
            val channel = response.bodyAsChannel()
            while (!channel.isClosedForRead) {
                val packet = channel.readRemaining(8192L)
                while (!packet.isEmpty) {
                    val bytes = packet.readBytes()
                    location.appendBytes(bytes)
                    pos += bytes.size
                    progressCallback(pos)
                }
            }
            return@execute expectedFileSize == null || expectedFileSize == pos
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    suspend fun downloadFileVerified(target: Path, url: String, hash: String, algorithm: String, requestBuilder: HttpRequestBuilder.() -> Unit = {}, progressCallback: suspend (Long) -> Unit = {}): Boolean {
        if (target.exists() && target.isRegularFile()) {
            if (HashAlgorithms.getFileHash(target, algorithm).contentEquals(hash.hexToByteArray())) {
                progressCallback(target.fileSize())
                return true
            }
        }
        val tmpFile = withContext(Dispatchers.IO) {
            Files.createTempFile("nebula-", null)
        }
        if (!client.prepareGet(url, requestBuilder).executeVerify(tmpFile, hash, algorithm, progressCallback)) {
            tmpFile.deleteIfExists()
            return false
        }
        target.deleteIfExists()
        target.parent.toFile().mkdirs()
        tmpFile.moveTo(target)
        return true
    }

    suspend fun downloadFileUnverified(target: Path, url: String, expectedFileSize: Long? = null, requestBuilder: HttpRequestBuilder.() -> Unit = {}, progressCallback: suspend (Long) -> Unit = {}): Boolean {
        if (target.exists() && target.isRegularFile()) {
            if (expectedFileSize != null && expectedFileSize == target.fileSize()) {
                progressCallback(target.fileSize())
                return true
            }
        }
        val tmpFile = withContext(Dispatchers.IO) {
            Files.createTempFile("nebula-", null)
        }
        if (!client.prepareGet(url, requestBuilder).executeUnverified(tmpFile, expectedFileSize, progressCallback)) {
            tmpFile.deleteIfExists()
            return false
        }
        target.deleteIfExists()
        target.parent.toFile().mkdirs()
        tmpFile.moveTo(target)
        return true
    }

}

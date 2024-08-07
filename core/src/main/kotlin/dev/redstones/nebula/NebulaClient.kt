package dev.redstones.nebula

import dev.redstones.nebula.event.NebulaEventDownloadFinished
import dev.redstones.nebula.util.HashAlgorithms
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.serialization.kotlinx.xml.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.*

class NebulaClient internal constructor(val client: HttpClient) {

    fun Boolean.toDefaultFinishEvent(): NebulaEventDownloadFinished {
        return NebulaEventDownloadFinished(this, if (this) "Wrong response code, size or hash failure" else null)
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

fun HttpClient.toNebula(): NebulaClient = NebulaClient(this)

fun HttpClientConfig<*>.installNebula() {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
        xml()
    }
}

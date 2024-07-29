package dev.redstones.nebula.archive

import dev.redstones.nebula.DownloadQueueItem
import dev.redstones.nebula.util.moveToSafely
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.*
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorInputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.concurrent.thread
import kotlin.io.path.*
import kotlin.system.exitProcess

private fun <T: ArchiveInputStream<*>> unpackArchive(inputStream: T, target: Path) {
    val pathName = target.toFile().canonicalPath
    inputStream.use { archive ->
        lateinit var entry: ArchiveEntry
        while (archive.nextEntry?.also { entry = it } != null) {
            val file = target.resolve(entry.name)
            if (!file.toFile().canonicalPath.startsWith(pathName)) {
                throw InvalidPathException(file.toFile().canonicalPath, "outside allowed path $pathName")
            }
            if (entry.isDirectory) {
                file.toFile().mkdirs()
            } else {
                file.parent.toFile().mkdirs()
                file.outputStream().use { os ->
                    val buffer = ByteArray(8192)
                    var len: Int
                    while (archive.read(buffer).also { len = it } > 0) {
                        os.write(buffer, 0, len)
                    }
                }
            }
        }
    }
}

internal suspend fun HttpStatement.executeUnpack(target: Path, fileExt: String, progressCallback: suspend (Long) -> Unit = {}): Boolean {
    return execute { response ->
        if (response.status != HttpStatusCode.OK) {
            return@execute false
        }
        println(response.request.url)
        if ((response.contentLength() ?: 0) < 1000000) {
            val text = response.bodyAsText()
            println(text)
            exitProcess(-1)
        }
        var pos = 0L
        val channel = response.bodyAsChannel()
        val outputStream = PipedOutputStream()
        val inputStream = PipedInputStream(outputStream)
        coroutineScope {
            launch {
                val thread = thread {
                    when (fileExt) {
                        "zip" -> unpackArchive(ZipArchiveInputStream(inputStream), target)
                        "tar" -> unpackArchive(TarArchiveInputStream(inputStream), target)
                        "tar.gz", "tgz" -> unpackArchive(TarArchiveInputStream(GzipCompressorInputStream(inputStream)), target)
                        "tar.bz2", "tbz2" -> unpackArchive(TarArchiveInputStream(BZip2CompressorInputStream(inputStream)), target)
                        "tar.xz", "txz" -> unpackArchive(TarArchiveInputStream(XZCompressorInputStream(inputStream)), target)
                        "tar.zst", "tzst" -> unpackArchive(TarArchiveInputStream(ZstdCompressorInputStream(inputStream)), target)
                        else -> throw UnsupportedOperationException("archive type $fileExt not supported")
                    }
                }
                while (thread.isAlive) {
                    delay(500)
                }
            }
            launch {
                while (!channel.isClosedForRead) {
                    val packet = channel.readRemaining(8192L)
                    while (!packet.isEmpty) {
                        val bytes = packet.readBytes()
                        outputStream.write(bytes)
                        pos += bytes.size
                        progressCallback(pos)
                    }
                }
                outputStream.close()
            }
        }
        return@execute true
    }
}

@OptIn(ExperimentalStdlibApi::class, ExperimentalPathApi::class)
internal suspend fun HttpStatement.executeVerifyUnpack(target: Path, hash: String, algorithm: String, fileExt: String, progressCallback: suspend (Long) -> Unit = {}): Boolean {
    return execute { response ->
        if (response.status != HttpStatusCode.OK) {
            return@execute false
        }
        println(response.request.url)
        if ((response.contentLength() ?: 0) < 1000000) {
            println(response.bodyAsText())
            exitProcess(-1)
        }
        var pos = 0L
        val digest = MessageDigest.getInstance(algorithm)
        val channel = response.bodyAsChannel()
        val outputStream = PipedOutputStream()
        val inputStream = PipedInputStream(outputStream)
        coroutineScope {
            launch {
                val thread = thread {
                    when (fileExt) {
                        "zip" -> unpackArchive(ZipArchiveInputStream(inputStream), target)
                        "tar" -> unpackArchive(TarArchiveInputStream(inputStream), target)
                        "tar.gz", "tgz" -> unpackArchive(TarArchiveInputStream(GzipCompressorInputStream(inputStream)), target)
                        "tar.bz2", "tbz2" -> unpackArchive(TarArchiveInputStream(BZip2CompressorInputStream(inputStream)), target)
                        "tar.xz", "txz" -> unpackArchive(TarArchiveInputStream(XZCompressorInputStream(inputStream)), target)
                        "tar.zst", "tzst" -> unpackArchive(TarArchiveInputStream(ZstdCompressorInputStream(inputStream)), target)
                        else -> throw UnsupportedOperationException("archive type $fileExt not supported")
                    }
                }
                while (thread.isAlive) {
                    delay(500)
                }
            }
            launch {
                while (!channel.isClosedForRead) {
                    val packet = channel.readRemaining(8192L)
                    while (!packet.isEmpty) {
                        val bytes = packet.readBytes()
                        outputStream.write(bytes)
                        digest.update(bytes)
                        pos += bytes.size
                        progressCallback(pos)
                    }
                }
                outputStream.close()
            }
        }
        if (!digest.digest().contentEquals(hash.hexToByteArray())) {
            target.deleteRecursively()
            return@execute false
        }
        return@execute true
    }
}

@OptIn(ExperimentalPathApi::class)
suspend fun DownloadQueueItem.downloadFileUnpacked(target: Path, url: String, fileExt: String, requestBuilder: HttpRequestBuilder.() -> Unit = {}, progressCallback: suspend (Long) -> Unit = {}): Boolean {
    val tmpDir = withContext(Dispatchers.IO) {
        Files.createTempDirectory("nebula-")
    }
    if (!client.prepareGet(url, requestBuilder).executeUnpack(tmpDir, fileExt, progressCallback)) {
        tmpDir.deleteIfExists()
        return false
    }
    target.deleteRecursively()
    target.parent.toFile().mkdirs()
    tmpDir.moveToSafely(target)
    return true
}

@OptIn(ExperimentalPathApi::class)
suspend fun DownloadQueueItem.downloadFileVerifiedUnpacked(target: Path, url: String, hash: String, algorithm: String, fileExt: String, requestBuilder: HttpRequestBuilder.() -> Unit = {}, progressCallback: suspend (Long) -> Unit = {}): Boolean {
    val tmpDir = withContext(Dispatchers.IO) {
        Files.createTempDirectory("nebula-")
    }
    if (!client.prepareGet(url, requestBuilder).executeVerifyUnpack(tmpDir, hash, algorithm, fileExt, progressCallback)) {
        tmpDir.deleteIfExists()
        return false
    }
    target.deleteRecursively()
    target.parent.toFile().mkdirs()
    tmpDir.moveToSafely(target)
    return true
}

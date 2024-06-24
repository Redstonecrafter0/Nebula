package dev.redstones.nebula

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.serialization.kotlinx.xml.*
import io.ktor.utils.io.core.*
import kotlinx.serialization.json.Json
import java.nio.file.Path
import java.security.MessageDigest
import java.util.*
import kotlin.io.path.appendBytes
import kotlin.io.path.deleteIfExists

class DownloadManager internal constructor(val client: HttpClient) {

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

}

internal fun defaultEngine(): HttpClientEngineFactory<*> {
    val clazz = HttpClientEngineContainer::class.java
    val container = ServiceLoader.load(clazz, clazz.classLoader).toList().firstOrNull()
        ?: throw IllegalStateException("Missing ktor engine. See https://ktor.io/docs/http-client-engines.html")
    return container.factory
}

fun <T : HttpClientEngineConfig> DownloadManager(factory: HttpClientEngineFactory<T>? = null, block: ConfigBuilder<T>.() -> Unit = {}): DownloadManager {
    val config = ConfigBuilder<T>().apply(block)
    val client = HttpClient(factory ?: defaultEngine()) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
            xml()
        }
        @Suppress("UNCHECKED_CAST")
        config.ktorConfigBlock.invoke(this as HttpClientConfig<T>)
    }
    return DownloadManager(client)
}

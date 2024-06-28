package dev.redstones.nebula

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.serialization.kotlinx.xml.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.util.*
import kotlin.collections.ArrayDeque
import kotlin.concurrent.thread

class DownloadManager internal constructor(private val client: HttpClient) {

    private val deque = ArrayDeque<DownloadQueueItem>()
    private val listeners = mutableListOf<DownloadEventListener>()

    private var running = false

    fun enqueue(block: suspend DownloadQueueItem.() -> Unit): DownloadQueueItem {
        val item = DownloadQueueItem(client, block).apply {
            manager = this@DownloadManager
            listeners = this@DownloadManager.listeners.toList()
        }
        deque += item
        return item
    }

    fun addEventListener(block: DownloadEventListener.Builder.() -> Unit) {
        val builder = DownloadEventListener.Builder()
        builder.block()
        addEventListener(builder.build())
    }

    fun addEventListener(listener: DownloadEventListener) {
        listeners += listener
    }

    fun removeEventListener(listener: DownloadEventListener) {
        listeners -= listener
    }

    fun start() {
        running = true
        thread(name = "DownloadManager") {
            runSync()
        }
    }

    fun runSync() {
        runBlocking {
            while (running) {
                if (!runSingle()) {
                    delay(100)
                }
            }
        }
    }

    suspend fun runSingle(): Boolean {
        val queueItem = deque.removeFirstOrNull() ?: return false
        queueItem.download(queueItem)
        return true
    }

    fun stop() {
        running = false
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

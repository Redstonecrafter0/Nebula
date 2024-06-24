package dev.redstones.nebula

import io.ktor.client.*
import io.ktor.client.engine.*

class ConfigBuilder<T : HttpClientEngineConfig> internal constructor() {

    internal var ktorConfigBlock: HttpClientConfig<T>.() -> Unit = {}

    fun ktorConfig(block: HttpClientConfig<T>.() -> Unit) {
        ktorConfigBlock = block
    }

}
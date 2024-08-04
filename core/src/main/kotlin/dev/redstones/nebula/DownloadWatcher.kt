package dev.redstones.nebula

import dev.redstones.nebula.event.NebulaEvent
import dev.redstones.nebula.event.NebulaEventDownloadFinished
import dev.redstones.nebula.event.NebulaEventDownloadProgress
import dev.redstones.nebula.event.NebulaEventDownloadStarted
import kotlinx.coroutines.flow.*
import java.util.concurrent.ConcurrentHashMap

class DownloadWatcher {

    data class Progress(val pos: Long, val totalSize: Long?)

    internal val watched = ConcurrentHashMap<Flow<NebulaEvent>, Progress>()

    internal val mutableFlow: MutableStateFlow<Progress?> = MutableStateFlow(null)

    private val flow = mutableFlow.asStateFlow()

    fun subscribe(): Flow<Progress?> {
        return flow
    }

}

fun Flow<NebulaEvent>.watch(watcher: DownloadWatcher) = onEach { event ->
    when (event) {
        is NebulaEventDownloadStarted -> {
            watcher.watched[this] = DownloadWatcher.Progress(0, event.totalSize)
        }
        is NebulaEventDownloadProgress -> {
            watcher.watched[this] = DownloadWatcher.Progress(event.pos, event.totalSize)
        }
        is NebulaEventDownloadFinished -> {
            watcher.watched.remove(this)
        }
    }
    watcher.mutableFlow.update {
        val progresses = watcher.watched.values
        if (progresses.isEmpty()) {
            null
        } else {
            DownloadWatcher.Progress(progresses.sumOf { it.pos }, progresses.sumOf { it.totalSize ?: it.pos })
        }
    }
}

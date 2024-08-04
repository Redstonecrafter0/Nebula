package dev.redstones.nebula.event

interface NebulaEvent

class NebulaEventDownloadStarted(val totalSize: Long? = null) : NebulaEvent

class NebulaEventDownloadProgress(val pos: Long, val totalSize: Long = pos) : NebulaEvent

class NebulaEventDownloadFinished(val success: Boolean, val message: String? = null) : NebulaEvent

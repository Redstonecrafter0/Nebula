package dev.redstones.nebula

typealias DownloadCallbackStart = suspend (step: Int, maxStep: Int, max: Long?) -> Unit
typealias DownloadCallbackProgress = suspend (pos: Long?) -> Unit
typealias DownloadCallbackFinished = suspend (success: Boolean, message: String?) -> Unit

class DownloadEventListener(
    val onStart: DownloadCallbackStart,
    val onProgress: DownloadCallbackProgress,
    val onFinished: DownloadCallbackFinished
) {

    class Builder {

        private var blockOnStart: DownloadCallbackStart = { _: Int, _: Int, _: Long? -> }
        private var blockOnProgress: DownloadCallbackProgress = { _: Long? -> }
        private var blockOnFinished: DownloadCallbackFinished = { _: Boolean, _: String? -> }

        fun onStart(block: DownloadCallbackStart) {
            blockOnStart = block
        }

        fun onProgress(block: DownloadCallbackProgress) {
            blockOnProgress = block
        }

        fun onFinished(block: DownloadCallbackFinished) {
            blockOnFinished = block
        }

        fun build(): DownloadEventListener {
            return DownloadEventListener(blockOnStart, blockOnProgress, blockOnFinished)
        }
    }

}

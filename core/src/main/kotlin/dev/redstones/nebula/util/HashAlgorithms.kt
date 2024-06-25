package dev.redstones.nebula.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.inputStream

object HashAlgorithms {

    const val SHA1 = "SHA-1"

    suspend fun getFileHash(file: Path, algorithm: String): ByteArray {
        return withContext(Dispatchers.IO) {
            val digest = MessageDigest.getInstance(algorithm)
            file.inputStream().use { stream ->
                val buffer = ByteArray(8192)
                var len: Int
                while (stream.read(buffer).also { len = it } > 0) {
                    digest.update(buffer, 0, len)
                }
            }
            digest.digest()
        }
    }

}

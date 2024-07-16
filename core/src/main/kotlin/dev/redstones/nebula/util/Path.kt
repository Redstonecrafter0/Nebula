package dev.redstones.nebula.util

import java.nio.file.DirectoryNotEmptyException
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.copyToRecursively
import kotlin.io.path.deleteRecursively
import kotlin.io.path.moveTo

@OptIn(ExperimentalPathApi::class)
fun Path.moveToSafely(target: Path) {
    try {
        moveTo(target)
    } catch (_: DirectoryNotEmptyException) {
        copyToRecursively(target, {_: Path, innerTarget: Path, exception: Exception ->
            innerTarget.deleteRecursively()
            throw exception
        }, false)
        deleteRecursively()
    }
}

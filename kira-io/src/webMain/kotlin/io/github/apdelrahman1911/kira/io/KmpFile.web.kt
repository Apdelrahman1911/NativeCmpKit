package io.github.apdelrahman1911.kira.io

import io.github.apdelrahman1911.kira.core.PlatformContext
import org.w3c.files.File

actual class KmpFile(
    val file: File,
)

actual fun KmpFile.exists(context: PlatformContext) = true

actual suspend fun KmpFile.readByteArray(context: PlatformContext): ByteArray =
    readFileAsBytes(file)

actual fun KmpFile.getName(context: PlatformContext): String? = file.name

actual fun KmpFile.getPath(context: PlatformContext): String? = file.name

actual fun KmpFile.isDirectory(context: PlatformContext): Boolean = !file.name.contains(".")

internal expect suspend fun readFileAsBytes(file: File): ByteArray

/**
 * Converts this [org.w3c.files.File] to a [KmpFile].
 */
fun File.toKmpFile(): KmpFile = KmpFile(this)

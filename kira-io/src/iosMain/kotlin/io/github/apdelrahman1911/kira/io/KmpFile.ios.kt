package io.github.apdelrahman1911.kira.io

import io.github.apdelrahman1911.kira.core.InternalKiraApi
import io.github.apdelrahman1911.kira.core.PlatformContext
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.io.IOException
import kotlinx.io.files.FileNotFoundException
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSURLIsDirectoryKey
import platform.Foundation.dataWithContentsOfURL

/**
 * A wrapper class for a file in the platform-specific implementation.
 *
 * @property url A persistent copy of the file that remains accessible after the picker callback.
 * @property originalUrl The original URL returned by the file picker. May become inaccessible
 * due to iOS security scoping — use for metadata only (file name, path).
 */
actual class KmpFile @InternalKiraApi constructor(
    val url: NSURL,
    val originalUrl: NSURL,
) {
    @OptIn(InternalKiraApi::class)
    constructor(url: NSURL) : this(originalUrl = url, url = url)
}

actual fun KmpFile.exists(context: PlatformContext): Boolean {
    return NSFileManager.defaultManager.fileExistsAtPath(url.path ?: return false)
}

@OptIn(ExperimentalForeignApi::class)
actual suspend fun KmpFile.readByteArray(context: PlatformContext): ByteArray {
    if (!url.isFileURL() || url.path == null) {
        throw FileNotFoundException("iOS KmpFile content requires a file: URL with a filesystem path")
    }
    val accessing = url.startAccessingSecurityScopedResource()
    try {
        val data = NSData.dataWithContentsOfURL(url) ?: throw IOException("Could not read the file")
        return data.toCheckedByteArray()
    } finally {
        if (accessing) url.stopAccessingSecurityScopedResource()
    }
}

actual fun KmpFile.getName(context: PlatformContext): String? =
    originalUrl.absoluteString
        ?.removeSuffix("/")
        ?.split('/')
        ?.lastOrNull()

actual fun KmpFile.getPath(context: PlatformContext): String? =
    originalUrl.absoluteString

@OptIn(ExperimentalForeignApi::class)
actual fun KmpFile.isDirectory(context: PlatformContext): Boolean {
    val result = originalUrl.resourceValuesForKeys(listOf(NSURLIsDirectoryKey), error = null)
    return result?.get(NSURLIsDirectoryKey) == true
}

/**
 * Converts this [NSURL] to a [KmpFile].
 */
fun NSURL.toKmpFile(): KmpFile = KmpFile(this)

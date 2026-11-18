package io.github.apdelrahman1911.kira.io

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.io.buffered
import kotlinx.io.files.FileNotFoundException
import kotlinx.io.readByteArray
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.Foundation.create
import platform.Foundation.writeToURL
import platform.posix.chmod
import platform.posix.geteuid
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class NativeReadSafetyTest {
    private val directory = "${NSTemporaryDirectory()}/nativecmpkit-read-${NSUUID().UUIDString}"

    init {
        check(NSFileManager.defaultManager.createDirectoryAtPath(directory, true, null, null))
    }

    @AfterTest
    fun cleanup() {
        check(NSFileManager.defaultManager.removeItemAtPath(directory, null))
    }

    @Test
    fun readsRealEmptyOneByteAndBinaryFoundationFiles() = runTest {
        for ((index, bytes) in listOf(byteArrayOf(), byteArrayOf(-128), byteArrayOf(0, -1, 42, -128, 127, 1)).withIndex()) {
            val file = write("fixture-$index", bytes)
            assertContentEquals(bytes, file.readByteArray())
            assertContentEquals(bytes, file.source().buffered().use { it.readByteArray() })
        }
    }

    @Test
    fun missingAndNonFileUrlsFailRatherThanReturnEmptyData() = runTest {
        assertFailsWith<IOException> { KmpFile(NSURL.fileURLWithPath("$directory/missing")).readByteArray() }
        val file = write("private.bin", byteArrayOf(5, 0, -1, 8))
        val remote = KmpFile(NSURL(string = "https://127.0.0.1:1${file.url.path}"))
        assertFailsWith<FileNotFoundException> { remote.readByteArray() }
        assertContentEquals(byteArrayOf(5, 0, -1, 8), file.readByteArray())
    }

    @Test
    fun unreadableFileRaisesAnErrorAndCanBeReadOnceRestored() = runTest {
        check(geteuid() != 0u) { "Unreadable-file coverage requires a non-root simulator process" }
        val bytes = byteArrayOf(8, -1)
        val file = write("unreadable", bytes)
        val path = checkNotNull(file.url.path)
        check(chmod(path, 0u) == 0)
        try {
            assertFailsWith<IOException> { file.readByteArray() }
        } finally {
            check(chmod(path, 384u) == 0)
        }
        assertContentEquals(bytes, file.readByteArray())
    }

    private fun write(name: String, bytes: ByteArray): KmpFile {
        val url = NSURL.fileURLWithPath("$directory/$name")
        val data = if (bytes.isEmpty()) NSData() else bytes.usePinned {
            NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong())
        }
        check(data.writeToURL(url, atomically = true))
        return KmpFile(url)
    }
}

package com.hhp227.concafe.data.repository

import com.hhp227.concafe.domain.model.CompressedImageData
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfFile
import platform.posix.memcpy

actual fun compressImageForUpload(localPath: String, maxBytes: Int): CompressedImageData {
    val resolvedPath = resolveLocalPath(localPath)
    val sourceData = NSData.dataWithContentsOfFile(resolvedPath)
        ?: throw IllegalArgumentException("image file not found: $resolvedPath")
    val bytes = sourceData.toByteArray()
    val fileName = resolvedPath.substringAfterLast('/').ifBlank { "image.jpg" }
    return CompressedImageData(bytes = bytes, fileName = fileName)
}

private fun resolveLocalPath(path: String): String {
    if (path.startsWith("file://")) {
        return NSURL.URLWithString(path)?.path ?: path.removePrefix("file://")
    }
    return path
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    if (size == 0) return ByteArray(0)
    val byteArray = ByteArray(size)
    memScoped {
        byteArray.usePinned { pinned ->
            memcpy(pinned.addressOf(0), bytes, length)
        }
    }
    return byteArray
}

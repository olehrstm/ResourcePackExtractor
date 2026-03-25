package de.ole101.rpx.util

import java.io.File
import java.util.Locale

fun File.getHumanReadableSize(): String {
    val size = length()
    return formatFileSize(size)
}

fun formatFileSize(size: Long): String {
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var bytes = size.toDouble()
    var unitIndex = 0

    while (bytes >= 1024 && unitIndex < units.size - 1) {
        bytes /= 1024
        unitIndex++
    }

    return String.format(Locale.ROOT, "%.2f %s", bytes, units[unitIndex])
}

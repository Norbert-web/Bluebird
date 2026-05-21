package com.win11launcher.ui.screens

import java.io.File
import java.text.SimpleDateFormat
import java.util.*

fun formatFileSize(bytes: Long): String {
    if (bytes < 1024) return "${bytes}B"
    if (bytes < 1024 * 1024) return "%.1f KB".format(bytes / 1024.0)
    if (bytes < 1024 * 1024 * 1024) return "%.1f MB".format(bytes / (1024.0 * 1024.0))
    return "%.1f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
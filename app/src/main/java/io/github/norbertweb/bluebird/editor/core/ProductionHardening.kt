package io.github.norbertweb.bluebird.editor.core

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

/** Small, dependency-free safety primitives used by the production editor paths. */
object ProductionHardening {
    const val MAX_EDITOR_FILE_BYTES = 8L * 1024L * 1024L
    const val MAX_AUTOSAVE_BYTES = 8L * 1024L * 1024L

    fun canonicalOrNull(file: File): File? = runCatching { file.canonicalFile }.getOrNull()

    fun isRegularReadableFile(file: File, maxBytes: Long = MAX_EDITOR_FILE_BYTES): Boolean =
        file.isFile && file.canRead() && file.length() <= maxBytes

    fun safeReadText(file: File, charset: Charset = Charsets.UTF_8, maxBytes: Long = MAX_EDITOR_FILE_BYTES): String {
        require(isRegularReadableFile(file, maxBytes)) { "File is unavailable or exceeds the editor size limit" }
        return FileInputStream(file).use { input ->
            val bytes = input.readBounded(maxBytes)
            bytes.toString(charset)
        }
    }

    /** Atomic replace: write beside the target, fsync, then rename over it. */
    fun atomicWriteText(target: File, text: String, charset: Charset = Charsets.UTF_8, maxBytes: Long = MAX_EDITOR_FILE_BYTES) {
        val bytes = text.toByteArray(charset)
        require(bytes.size.toLong() <= maxBytes) { "Document exceeds the editor size limit" }
        val parent = target.absoluteFile.parentFile ?: error("Target has no parent directory")
        if (!parent.exists() && !parent.mkdirs()) error("Unable to create target directory")
        val tmp = File(parent, ".${target.name}.bluebird.tmp")
        try {
            FileOutputStream(tmp).use { out ->
                out.write(bytes)
                out.fd.sync()
            }
            try {
                Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            } catch (_: Exception) {
                Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            if (tmp.exists()) runCatching { tmp.delete() }
        }
    }

    fun sha256(text: String, charset: Charset = Charsets.UTF_8): String =
        MessageDigest.getInstance("SHA-256").digest(text.toByteArray(charset)).joinToString("") { "%02x".format(it) }

    private fun FileInputStream.readBounded(maxBytes: Long): ByteArray {
        val expected = channel.size()
        require(expected <= maxBytes) { "File exceeds the editor size limit" }
        val buffer = ByteArray(expected.toInt())
        var offset = 0
        while (offset < buffer.size) {
            val read = read(buffer, offset, buffer.size - offset)
            if (read < 0) break
            offset += read
        }
        return if (offset == buffer.size) buffer else buffer.copyOf(offset)
    }
}

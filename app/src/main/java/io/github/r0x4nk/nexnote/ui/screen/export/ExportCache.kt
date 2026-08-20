package io.github.r0x4nk.nexnote.ui.screen.export

import java.io.File
import java.io.IOException
import java.util.UUID

/**
 * Owns temporary export files and their retention policy.
 *
 * Files are intentionally kept long enough for a receiving app to consume the
 * granted content URI. Stale files are removed on app startup and before each
 * new export; Android may clear the cache earlier under storage pressure.
 */
internal class ExportCache(
    cacheDir: File,
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val uniqueToken: () -> String = { UUID.randomUUID().toString() }
) {
    private val canonicalCacheDir: File = cacheDir.canonicalFile
    val directory: File = File(cacheDir, DIRECTORY_NAME)

    fun prepareFile(preferredName: String): File {
        cleanupExpired()
        ensureDirectory()
        require(preferredName.isSafeLeafName()) { "Export file name must be a simple file name" }

        val dotIndex = preferredName.lastIndexOf('.')
        val stem = if (dotIndex > 0) preferredName.substring(0, dotIndex) else preferredName
        val extension = if (dotIndex > 0) preferredName.substring(dotIndex) else ""
        val token = uniqueToken().replace(UNSAFE_TOKEN_CHARACTERS, "_").ifBlank { "unique" }
        val candidate = File(directory, "${stem}_${nowMillis()}_$token$extension")
        val canonicalDirectory = directory.canonicalFile
        val canonicalCandidate = candidate.canonicalFile
        if (canonicalCandidate.parentFile != canonicalDirectory) {
            throw IOException("Export path escapes the cache directory")
        }
        return canonicalCandidate
    }

    fun cleanupExpired(): Int {
        if (!directory.exists() || !directory.isDirectory) return 0
        val now = nowMillis()
        return directory.listFiles()
            .orEmpty()
            .asSequence()
            .filter { it.isFile && now - it.lastModified() >= RETENTION_MILLIS }
            .count { it.delete() }
    }

    private fun ensureDirectory() {
        if (!directory.exists() && !directory.mkdirs()) {
            throw IOException("Could not create export cache directory")
        }
        if (!directory.isDirectory || directory.canonicalFile.parentFile != canonicalCacheDir) {
            throw IOException("Export cache directory is invalid")
        }
    }

    private fun String.isSafeLeafName(): Boolean =
        isNotBlank() && this != "." && this != ".." &&
            '/' !in this && '\\' !in this

    companion object {
        internal const val RETENTION_MILLIS = 24L * 60L * 60L * 1_000L
        private const val DIRECTORY_NAME = "exports"
        private val UNSAFE_TOKEN_CHARACTERS = Regex("[^A-Za-z0-9_-]")
    }
}

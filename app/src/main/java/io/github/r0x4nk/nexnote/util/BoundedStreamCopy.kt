package io.github.r0x4nk.nexnote.util

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

internal class InputTooLargeException(message: String) : IOException(message)

/** Fixed working memory, no application file-size quota. */
internal fun copyStreaming(
    input: InputStream,
    output: OutputStream,
    checkCancellation: () -> Unit = {}
): Long = copyBounded(input, output, Long.MAX_VALUE, checkCancellation)

internal fun copyBounded(
    input: InputStream,
    output: OutputStream,
    maxBytes: Long,
    checkCancellation: () -> Unit = {}
): Long {
    require(maxBytes >= 0L)
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0L
    try {
        while (true) {
            checkCancellation()
            val read = input.read(buffer)
            if (read == -1) return total
            if (read == 0) continue
            if (read.toLong() > maxBytes - total) {
                throw InputTooLargeException("Input exceeds the $maxBytes byte limit")
            }
            output.write(buffer, 0, read)
            total += read
        }
    } finally { buffer.fill(0) }
}

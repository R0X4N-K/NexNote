package io.github.r0x4nk.nexnote.util

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

internal class InputTooLargeException(message: String) : IOException(message)

internal fun copyBounded(
    input: InputStream,
    output: OutputStream,
    maxBytes: Long
): Long {
    require(maxBytes >= 0L)
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0L
    while (true) {
        val read = input.read(buffer)
        if (read == -1) return total
        if (read.toLong() > maxBytes - total) {
            throw InputTooLargeException("Input exceeds the $maxBytes byte limit")
        }
        output.write(buffer, 0, read)
        total += read
    }
}

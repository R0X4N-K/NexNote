package io.github.r0x4nk.nexnote.util

import kotlinx.coroutines.CancellationException

/** Result wrapper for coroutine boundaries that must never consume cancellation. */
inline fun <T> runCatchingPreservingCancellation(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        Result.failure(error)
    }

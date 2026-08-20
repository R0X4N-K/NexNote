package io.github.r0x4nk.nexnote.ui.screen.editor

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Application-owned FIFO for editor writes.
 *
 * Normal saves inherit cancellation from their caller. Final saves requested
 * during ViewModel shutdown are children of the long-lived owner instead, so
 * they have an explicit lifecycle and never create an ad-hoc detached scope.
 * A single worker also prevents old and new editor instances from writing the
 * same repository concurrently.
 */
internal class EditorSaveCoordinator(
    ownerScope: CoroutineScope,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val requests = Channel<SaveRequest>(capacity = Channel.UNLIMITED)
    private val worker: Job = ownerScope.launch(dispatcher) {
        try {
            for (request in requests) {
                runRequest(request)
            }
        } finally {
            val cancellation = CancellationException("Editor save owner stopped")
            while (true) {
                val pending = requests.tryReceive().getOrNull() ?: break
                pending.job.cancel(cancellation)
                pending.completion.cancel(cancellation)
            }
        }
    }

    suspend fun runSave(block: suspend () -> Boolean): Boolean {
        val requestJob = SupervisorJob(currentCoroutineContext()[Job])
        val completion = CompletableDeferred<Boolean>()
        requests.send(SaveRequest(requestJob, block, completion))
        return completion.await()
    }

    fun enqueueFinalSave(block: suspend () -> Boolean): Deferred<Boolean> {
        val requestJob = SupervisorJob(worker)
        val completion = CompletableDeferred<Boolean>()
        val result = requests.trySend(SaveRequest(requestJob, block, completion))
        if (result.isFailure) {
            val error = CancellationException("Editor save owner is unavailable")
            requestJob.cancel(error)
            completion.cancel(error)
        }
        return completion
    }

    suspend fun awaitIdle() {
        runSave { true }
    }

    private suspend fun runRequest(request: SaveRequest) {
        if (!request.job.isActive) {
            request.completion.cancel(CancellationException("Editor save was cancelled"))
            return
        }

        try {
            val result = withContext(request.job) { request.block() }
            request.job.complete()
            request.completion.complete(result)
        } catch (error: CancellationException) {
            request.job.cancel(error)
            request.completion.cancel(error)
        } catch (error: Throwable) {
            request.job.complete()
            request.completion.completeExceptionally(error)
        }
    }

    private data class SaveRequest(
        val job: CompletableJob,
        val block: suspend () -> Boolean,
        val completion: CompletableDeferred<Boolean>
    )
}

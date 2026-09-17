package io.github.r0x4nk.nexnote.ui.common

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Serializes user mutations and releases progress on success, failure and cancellation. */
internal class NoteOperationRunner(private val scope: CoroutineScope) {
    private val activeLabel = MutableStateFlow<String?>(null)
    val progress = activeLabel.asStateFlow()

    fun launch(label: String, onError: (Exception) -> Unit, action: suspend () -> Unit) {
        if (!activeLabel.compareAndSet(null, label)) return
        scope.launch {
            try {
                action()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                onError(error)
            }
        }.invokeOnCompletion { activeLabel.value = null }
    }
}

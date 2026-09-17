package io.github.r0x4nk.nexnote.fileimport

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.r0x4nk.nexnote.NexNoteApp
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** Keeps pending imports and navigation alive across activity recreation. */
internal class ExternalImportViewModel(application: Application) : AndroidViewModel(application) {
    private val useCases = (application as NexNoteApp).useCases
    private val strings = (application as NexNoteApp).strings
    private val fileImporter = ExternalFileImporter(
        application.contentResolver,
        useCases.notes.saveNote,
        useCases.tags.indexNoteTags,
        strings
    )
    private val shareImporter = ExternalShareImporter(
        application.contentResolver,
        useCases.notes.saveNote,
        useCases.tags.indexNoteTags,
        useCases.images.copyNoteImageToInternal,
        useCases.images.deleteNoteImage,
        useCases.notes.moveNoteToTrash,
        useCases.notes.deleteNotePermanently,
        strings
    )
    private val intents = Channel<Intent>(Channel.UNLIMITED)
    private val errors = Channel<String>(Channel.BUFFERED)
    private val pendingOpen = MutableStateFlow<ExternalFileOpenRequest?>(null)
    val openRequest = pendingOpen.asStateFlow()
    val errorMessages = errors.receiveAsFlow()
    private var nextRequestId = 0L

    init {
        viewModelScope.launch {
            for (intent in intents) {
                val result = if (intent.action == Intent.ACTION_SEND) {
                    shareImporter.importFrom(intent)
                } else {
                    fileImporter.importFrom(intent)
                }
                when (result) {
                    ExternalFileImportResult.Ignored -> Unit
                    is ExternalFileImportResult.Failed -> errors.send(result.message)
                    is ExternalFileImportResult.Imported -> {
                        pendingOpen.value = ExternalFileOpenRequest(++nextRequestId, result.noteId)
                    }
                }
            }
        }
    }

    fun accept(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND || intent?.action == Intent.ACTION_VIEW) {
            intents.trySend(Intent(intent))
        }
    }

    fun consumeOpenRequest(requestId: Long) {
        if (pendingOpen.value?.requestId == requestId) pendingOpen.value = null
    }
}

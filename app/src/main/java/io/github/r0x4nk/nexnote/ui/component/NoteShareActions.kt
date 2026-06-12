package io.github.r0x4nk.nexnote.ui.component

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.ui.common.shareAsText
import io.github.r0x4nk.nexnote.ui.common.shareSubject
import kotlinx.coroutines.launch

@Immutable
internal data class NoteShareCallbacks(
    val onShareNote: (Note) -> Unit,
    val onShareNotes: (Collection<Note>) -> Unit
)

@Composable
internal fun rememberNoteShareCallbacks(
    snackbarHostState: SnackbarHostState
): NoteShareCallbacks {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    return remember(context, snackbarHostState, scope) {
        val shareNotes: (Collection<Note>) -> Unit = shareNotes@{ notes ->
            if (notes.isEmpty()) return@shareNotes

            val chooserTitle = if (notes.size == 1) "Share note" else "Share notes"
            val shareIntent = notes.toTextShareIntent()
            try {
                context.startActivity(Intent.createChooser(shareIntent, chooserTitle))
            } catch (_: ActivityNotFoundException) {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "No app available to share notes",
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
        NoteShareCallbacks(
            onShareNote = { note -> shareNotes(listOf(note)) },
            onShareNotes = shareNotes
        )
    }
}

private fun Collection<Note>.toTextShareIntent(): Intent =
    Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, shareSubject())
        putExtra(Intent.EXTRA_TITLE, shareSubject())
        putExtra(Intent.EXTRA_TEXT, shareAsText())
    }

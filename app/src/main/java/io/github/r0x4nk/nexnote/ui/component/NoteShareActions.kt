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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import io.github.r0x4nk.nexnote.R
import io.github.r0x4nk.nexnote.di.StringProvider
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
    val resources = LocalResources.current
    val strings = remember(resources) {
        StringProvider { id, args -> resources.getString(id, *args) }
    }
    val shareNoteTitle = stringResource(R.string.common_share_note)
    val shareNotesTitle = stringResource(R.string.share_notes)
    val noAppMessage = stringResource(R.string.share_no_app)

    return remember(
        context,
        snackbarHostState,
        scope,
        strings,
        shareNoteTitle,
        shareNotesTitle,
        noAppMessage
    ) {
        val shareNotes: (Collection<Note>) -> Unit = shareNotes@{ notes ->
            if (notes.isEmpty()) return@shareNotes

            val chooserTitle = if (notes.size == 1) shareNoteTitle else shareNotesTitle
            val shareIntent = notes.toTextShareIntent(notes.shareSubject(strings))
            try {
                context.startActivity(Intent.createChooser(shareIntent, chooserTitle))
            } catch (_: ActivityNotFoundException) {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = noAppMessage,
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

private fun Collection<Note>.toTextShareIntent(subject: String): Intent =
    Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TITLE, subject)
        putExtra(Intent.EXTRA_TEXT, shareAsText())
    }

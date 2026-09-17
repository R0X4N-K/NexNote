package io.github.r0x4nk.nexnote.ui.screen.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.r0x4nk.nexnote.R
import io.github.r0x4nk.nexnote.ui.component.NexSheetDragHandle
import io.github.r0x4nk.nexnote.ui.component.NexSheetHeader

private val HeadingLevels: IntRange = 1..6

private fun headingPreviewSize(level: Int) = when (level) {
    1 -> 22.sp
    2 -> 19.sp
    3 -> 17.sp
    4 -> 16.sp
    5 -> 15.sp
    else -> 14.sp
}

/**
 * Heading-level chooser for the editor toolbar.
 *
 * Presented as a modal bottom sheet to match the shared chooser pattern used by
 * the attachment picker, instead of a small anchored dropdown menu.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditorHeadingSheet(
    onDismissRequest: () -> Unit,
    onSelectLevel: (Int) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        dragHandle = { NexSheetDragHandle() },
        tonalElevation = 1.dp
    ) {
        Column(Modifier.padding(bottom = 24.dp)) {
            NexSheetHeader(
                title = stringResource(R.string.editor_heading_level),
                onBack = onDismissRequest
            )
            HeadingLevels.forEach { level ->
                ListItem(
                    headlineContent = {
                        Text(
                            text = "H$level",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = headingPreviewSize(level)
                            )
                        )
                    },
                    modifier = Modifier.clickable { onSelectLevel(level) }
                )
            }
        }
    }
}

/**
 * Link-type chooser for the editor toolbar.
 *
 * Mirrors the attachment picker bottom sheet so every "pick one" toolbar tool
 * opens the same way.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditorLinkSheet(
    onDismissRequest: () -> Unit,
    onInsertWebLink: () -> Unit,
    onInsertNoteLink: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        dragHandle = { NexSheetDragHandle() },
        tonalElevation = 1.dp
    ) {
        Column(Modifier.padding(bottom = 24.dp)) {
            NexSheetHeader(
                title = stringResource(R.string.editor_insert_link),
                onBack = onDismissRequest
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.editor_web_link)) },
                leadingContent = { Icon(Icons.Default.Public, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onInsertWebLink)
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.editor_note_link)) },
                leadingContent = { Icon(Icons.AutoMirrored.Filled.Note, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onInsertNoteLink)
            )
        }
    }
}

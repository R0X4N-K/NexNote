package io.github.r0x4nk.nexnote.ui.screen.editor

import android.content.ContentResolver
import android.net.Uri
import android.webkit.MimeTypeMap
import android.content.Context
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.R
import io.github.r0x4nk.nexnote.ui.component.NexSheetDragHandle
import io.github.r0x4nk.nexnote.ui.component.NexSheetHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun rememberLaunchAttachmentPicker(
    context: Context,
    state: EditorScreenState,
    viewModel: EditorViewModel
): () -> Unit {
    var showPicker by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val launchImage = rememberLaunchImagePickerAtCursor(context, state, viewModel)
    val documentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val offset = state.pendingAttachmentInsertionOffsetState.value
        state.pendingAttachmentInsertionOffsetState.value = null
        if (uri != null) {
            val resolver = context.applicationContext.contentResolver
            viewModel.pendingAttachment.accept { viewModel.onAttachmentPicked(
                fileName = { resolver.attachmentFileName(uri) },
                openInputStream = { resolver.openInputStream(uri) },
                insertionOffset = offset
            ) }
        } else {
            viewModel.pendingAttachment.cancel()
        }
    }
    if (showPicker) {
        ModalBottomSheet(
            onDismissRequest = { showPicker = false },
            dragHandle = { NexSheetDragHandle() },
            tonalElevation = 1.dp
        ) {
            Column(Modifier.padding(bottom = 24.dp)) {
                NexSheetHeader(
                    title = stringResource(R.string.attachment_add),
                    onBack = { showPicker = false }
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.attachment_photo)) },
                    leadingContent = { Icon(Icons.Default.Image, null) },
                    modifier = Modifier.clickable { showPicker = false; launchImage() }
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.attachment_document)) },
                    supportingContent = { Text(stringResource(R.string.attachment_formats)) },
                    leadingContent = { Icon(Icons.Default.AttachFile, null) },
                    modifier = Modifier.clickable {
                        showPicker = false
                        state.pendingAttachmentInsertionOffsetState.value = state.currentContentTextFieldValue().selection.end
                        scope.launch {
                            if (viewModel.pendingAttachment.prepare()) {
                                try {
                                    documentLauncher.launch(arrayOf("*/*"))
                                } catch (_: android.content.ActivityNotFoundException) {
                                    viewModel.pendingAttachment.cancel()
                                }
                            }
                        }
                    }
                )
            }
        }
    }
    return { showPicker = true }
}

private fun ContentResolver.attachmentFileName(uri: Uri): String {
    val name = query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    }?.takeIf { it.isNotBlank() } ?: "attachment"
    if ('.' in name) return name
    val extension = getType(uri)?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
    return if (extension.isNullOrBlank()) name else "$name.$extension"
}

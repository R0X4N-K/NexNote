package io.github.r0x4nk.nexnote.ui.component

import android.text.format.Formatter
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.R
import io.github.r0x4nk.nexnote.domain.model.AttachmentKind
import io.github.r0x4nk.nexnote.domain.model.NoteAttachment
import io.github.r0x4nk.nexnote.ui.screen.export.AttachmentOpenManager
import io.github.r0x4nk.nexnote.util.runCatchingPreservingCancellation
import java.io.File
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun MarkdownAttachmentCard(
    attachment: NoteAttachment,
    fileProvider: ((String) -> File)?,
    isVault: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var size by remember(attachment.path) { mutableStateOf<Long?>(null) }
    var opening by remember(attachment.path) { mutableStateOf(false) }
    LaunchedEffect(attachment.path, fileProvider, isVault) {
        size = if (isVault) null else withContext(Dispatchers.IO) {
            runCatching { fileProvider?.invoke(attachment.path)?.takeIf { it.isFile }?.length() }.getOrNull()
        }
    }
    val scheme = MaterialTheme.colorScheme
    val (container, foreground) = when (attachment.kind) {
        AttachmentKind.PDF -> scheme.errorContainer to scheme.onErrorContainer
        AttachmentKind.SPREADSHEET, AttachmentKind.AUDIO -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        AttachmentKind.DOCUMENT, AttachmentKind.PRESENTATION -> scheme.primaryContainer to scheme.onPrimaryContainer
        else -> scheme.secondaryContainer to scheme.onSecondaryContainer
    }
    val icon = when (attachment.kind) {
        AttachmentKind.PDF -> Icons.Default.PictureAsPdf
        AttachmentKind.DOCUMENT -> Icons.Default.Description
        AttachmentKind.SPREADSHEET -> Icons.Default.TableChart
        AttachmentKind.PRESENTATION -> Icons.Default.Slideshow
        AttachmentKind.AUDIO -> Icons.Default.AudioFile
        AttachmentKind.VIDEO -> Icons.Default.VideoFile
        AttachmentKind.ARCHIVE -> Icons.Default.FolderZip
        AttachmentKind.FILE -> Icons.AutoMirrored.Filled.InsertDriveFile
    }
    val errorText = stringResource(R.string.attachment_open_failed)
    OutlinedCard(
        onClick = {
            val provider = fileProvider ?: return@OutlinedCard
            opening = true
            scope.launch {
                try {
                    val result = runCatchingPreservingCancellation {
                        val intent = AttachmentOpenManager(context.applicationContext).buildIntent(attachment, provider)
                        context.startActivity(intent)
                    }
                    if (result.isFailure) Toast.makeText(context, errorText, Toast.LENGTH_LONG).show()
                } finally { opening = false }
            }
        },
        enabled = !isVault && size != null && !opening,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.outlinedCardColors(containerColor = scheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(color = container, contentColor = foreground, shape = MaterialTheme.shapes.medium) {
                Icon(icon, null, Modifier.padding(12.dp).size(28.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(attachment.displayName, style = MaterialTheme.typography.titleSmall,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(attachment.extension.uppercase(Locale.ROOT),
                        if (isVault) stringResource(R.string.attachment_vault_hint)
                        else size?.let { Formatter.formatShortFileSize(context, it) }
                            ?: stringResource(R.string.attachment_unavailable)).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant
                )
            }
            Icon(if (isVault) Icons.Default.Lock else Icons.AutoMirrored.Filled.OpenInNew,
                stringResource(R.string.attachment_open), Modifier.size(20.dp))
        }
    }
}

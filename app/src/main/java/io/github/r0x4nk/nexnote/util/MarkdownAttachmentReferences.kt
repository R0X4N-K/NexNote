package io.github.r0x4nk.nexnote.util

import androidx.compose.ui.text.AnnotatedString
import io.github.r0x4nk.nexnote.domain.model.NoteAttachment

/** Keep live file references when a user moves an attachment link into a list, quote or table. */
fun MarkdownBlock.referencedAttachments(): List<NoteAttachment> = when (this) {
    is MarkdownBlock.AttachmentBlock -> listOf(attachment)
    is MarkdownBlock.TextBlock -> annotatedString.attachments()
    is MarkdownBlock.BlockquoteBlock -> content.attachments()
    is MarkdownBlock.TableBlock -> (headers + rows.flatten()).flatMap { it.attachments() }
    else -> emptyList()
}.distinctBy { it.path }

private fun AnnotatedString.attachments(): List<NoteAttachment> =
    getStringAnnotations("URL", 0, length).mapNotNull { reference ->
        reference.item.takeIf(NoteAttachment::isAttachmentPath)?.let { path ->
            NoteAttachment(path, text.substring(reference.start, reference.end))
        }
    }

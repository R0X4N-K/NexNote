package io.github.r0x4nk.nexnote.util

import io.github.r0x4nk.nexnote.domain.model.AttachmentKind
import io.github.r0x4nk.nexnote.domain.model.NoteAttachment
import io.github.r0x4nk.nexnote.ui.component.buildMarkdownBlockSourceRanges
import org.junit.Assert.*
import org.junit.Test

class AttachmentMarkdownTest {
    private val path = "images/attachments/note_1_abcd-1234.pdf"

    @Test fun `filenames containing Markdown metacharacters round trip`() {
        val attachment = NoteAttachment(path, "Budget [final] \\ (2026).pdf")
        val text = AttachmentMarkdown.format(attachment)
        assertEquals(attachment, AttachmentMarkdown.parse(text))
        assertEquals(attachment.displayName, MarkdownPlainText.fromMarkdown(text))
        assertEquals(AttachmentKind.PDF, attachment.kind)
    }

    @Test fun `code fences and ordinary links never become attachments`() {
        val line = AttachmentMarkdown.format(NoteAttachment(path, "file.pdf"))
        val blocks = MarkdownParser.parseBlocks("```\n$line\n```\n[web](https://example.com)", MarkdownColors.Unspecified)
        assertTrue(blocks.none { it is MarkdownBlock.AttachmentBlock })
        assertNull(AttachmentMarkdown.parse("[file](images/attachments/../../secret)"))
        assertNull(AttachmentMarkdown.parse("[file](file:///private/file.pdf)"))
    }

    @Test fun `filenames do not create tags or lose inline formatting characters`() {
        val attachment = NoteAttachment(path, "Report #private *draft* _final_.pdf")
        val line = AttachmentMarkdown.format(attachment)
        assertEquals(attachment, AttachmentMarkdown.parse(line))
        assertTrue(TagParser.extractTags(line).isEmpty())
        val inline = MarkdownParser.parseBlocks("Read $line", MarkdownColors.Unspecified)
            .flatMap { it.referencedAttachments() }.single()
        assertEquals(attachment, inline)
    }

    @Test fun `attachment block source ranges stay aligned with mixed Markdown`() {
        val line = AttachmentMarkdown.format(NoteAttachment(path, "file.pdf"))
        val markdown = "Before\n$line\n- [ ] task\n> quote\n$line\n~~~\n$line\n~~~"
        val blocks = MarkdownParser.parseBlocks(markdown, MarkdownColors.Unspecified)
        val ranges = buildMarkdownBlockSourceRanges(markdown)
        assertEquals(blocks.size, ranges.size)
        blocks.forEachIndexed { index, block ->
            if (block is MarkdownBlock.AttachmentBlock) {
                assertEquals(line, markdown.substring(ranges[index].start, ranges[index].end))
            }
        }
        assertEquals(2, blocks.count { it is MarkdownBlock.AttachmentBlock })
    }

    @Test fun `inline list quote and table references remain attachments but code does not`() {
        val markdown = "- Read [Report]($path)\n> [Report]($path)\n| File |\n| --- |\n| [Report]($path) |\n```\n[Report]($path)\n```"
        val blocks = MarkdownParser.parseBlocks(markdown, MarkdownColors.Unspecified)
        assertEquals(3, blocks.flatMap { it.referencedAttachments() }.size)
        assertTrue(blocks.filterIsInstance<MarkdownBlock.CodeBlock>().single().referencedAttachments().isEmpty())
    }

    @Test fun `file categories are case insensitive and unknown formats remain supported`() {
        assertEquals(AttachmentKind.AUDIO, AttachmentKind.fromExtension("OPUS"))
        assertEquals(AttachmentKind.SPREADSHEET, AttachmentKind.fromExtension("xlsx"))
        assertEquals(AttachmentKind.FILE, AttachmentKind.fromExtension("unknown"))
        assertEquals("bin", NoteAttachment.safeExtension("file.pdf/../../secret"))
    }
}

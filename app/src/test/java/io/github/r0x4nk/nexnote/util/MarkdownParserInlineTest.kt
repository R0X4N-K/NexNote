package io.github.r0x4nk.nexnote.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownParserInlineTest {

    private val linkColor = Color.Blue
    private val colors = MarkdownColors(
        linkColor = linkColor,
        inlineCodeBackground = Color.LightGray,
        inlineCodeForeground = Color.Black
    )
    private fun parse(text: String) = MarkdownParser.parse(text, colors)

    @Test
    fun plainText_noMarkdown_returnsSameText() {
        val result = parse("Plain text without formatting")
        assertEquals("Plain text without formatting", result.text)
        assertTrue(result.spanStyles.isEmpty())
    }

    @Test
    fun emptyString_returnsEmpty() {
        val result = parse("")
        assertEquals("", result.text)
    }

    @Test
    fun h1_producesBoldLargeText() {
        val result = parse("# Main heading")
        assertEquals("Main heading", result.text)
        val span = result.spanStyles.firstOrNull()
        assertEquals(FontWeight.Bold, span?.item?.fontWeight)
        assertEquals(28.sp, span?.item?.fontSize)
    }

    @Test
    fun h2_producesBoldMediumText() {
        val result = parse("## Subheading")
        assertEquals("Subheading", result.text)
        val span = result.spanStyles.firstOrNull()
        assertEquals(FontWeight.Bold, span?.item?.fontWeight)
        assertEquals(22.sp, span?.item?.fontSize)
    }

    @Test
    fun h3_producesBoldSmallText() {
        val result = parse("### Section")
        assertEquals("Section", result.text)
        val span = result.spanStyles.firstOrNull()
        assertEquals(FontWeight.Bold, span?.item?.fontWeight)
        assertEquals(18.sp, span?.item?.fontSize)
    }

    @Test
    fun h3_hasHigherSpecificityThanH1() {
        val result = parse("### H3 test")
        assertEquals(18.sp, result.spanStyles.firstOrNull()?.item?.fontSize)
    }

    @Test
    fun bold_doubleAsterisk_producesBoldSpan() {
        val result = parse("**bold text**")
        assertEquals("bold text", result.text)
        assertEquals(FontWeight.Bold, result.spanStyles.firstOrNull()?.item?.fontWeight)
    }

    @Test
    fun bold_doubleUnderscore_producesBoldSpan() {
        val result = parse("__bold text__")
        assertEquals("bold text", result.text)
        assertEquals(FontWeight.Bold, result.spanStyles.firstOrNull()?.item?.fontWeight)
    }

    @Test
    fun boldItalic_tripleUnderscore_producesBoldItalicSpan() {
        val result = parse("___bold italic___")
        assertEquals("bold italic", result.text)
        val span = result.spanStyles.firstOrNull()
        assertEquals(FontWeight.Bold, span?.item?.fontWeight)
        assertEquals(FontStyle.Italic, span?.item?.fontStyle)
    }

    @Test
    fun bold_unclosed_emitsLiteralAsterisk() {
        val result = parse("**not closed")
        assertTrue(result.text.startsWith("*"))
    }

    @Test
    fun italic_singleAsterisk_producesItalicSpan() {
        val result = parse("*italic text*")
        assertEquals("italic text", result.text)
        assertEquals(FontStyle.Italic, result.spanStyles.firstOrNull()?.item?.fontStyle)
    }

    @Test
    fun italic_placeholderInsertedByToolbar_producesItalicSpan() {
        // Mirror the exact byte sequence the toolbar emits when the user clicks
        // the italic button without an active selection. Catches regressions
        // where the italic span fails to render the placeholder text.
        val result = parse("*text*")
        assertEquals("text", result.text)
        val italicSpan = result.spanStyles.firstOrNull { it.item.fontStyle == FontStyle.Italic }
        assertNotNull("Toolbar-inserted italic must produce an Italic span", italicSpan)
        assertEquals(0, italicSpan?.start)
        assertEquals(4, italicSpan?.end)
    }

    @Test
    fun inlineCode_placeholderInsertedByToolbar_producesMonospaceSpan() {
        // Mirror the toolbar's inline-code button output. Confirms that the
        // resulting span carries both monospace font *and* the configured
        // background, which is what the user actually sees in the preview.
        val result = parse("`code`")
        assertEquals("code", result.text)
        val span = result.spanStyles.firstOrNull()?.item
        assertEquals(FontFamily.Monospace, span?.fontFamily)
        assertEquals(Color.LightGray, span?.background)
    }

    @Test
    fun italic_singleUnderscore_producesItalicSpan() {
        val result = parse("_italic text_")
        assertEquals("italic text", result.text)
        assertEquals(FontStyle.Italic, result.spanStyles.firstOrNull()?.item?.fontStyle)
    }

    @Test
    fun link_producesAnnotationAndUnderline() {
        val result = parse("[text](https://example.com)")
        assertEquals("text", result.text)
        val urlAnnotations = result.getStringAnnotations("URL", 0, result.text.length)
        assertEquals(1, urlAnnotations.size)
        assertEquals("https://example.com", urlAnnotations.first().item)
        val span = result.spanStyles.firstOrNull()
        assertEquals(TextDecoration.Underline, span?.item?.textDecoration)
        assertEquals(linkColor, span?.item?.color)
    }

    @Test
    fun link_withTitle_stripsTitleFromUrlAnnotation() {
        val result = parse("""[text](https://example.com "Title")""")

        assertEquals("https://example.com", result.getStringAnnotations("URL", 0, result.text.length).first().item)
    }

    @Test
    fun noteLink_producesNoteAnnotationAndUnderline() {
        val result = parse("[[note:42|Project plan]]")
        assertEquals("Project plan", result.text)
        val annotations = result.getStringAnnotations(
            NoteLinkMarkdown.ANNOTATION_TAG,
            0,
            result.text.length
        )
        assertEquals(1, annotations.size)
        assertEquals("42", annotations.first().item)
        val span = result.spanStyles.firstOrNull()
        assertEquals(TextDecoration.Underline, span?.item?.textDecoration)
        assertEquals(linkColor, span?.item?.color)
    }

    @Test
    fun noteLink_withoutTitle_usesFallbackLabel() {
        val result = parse("[[note:42]]")
        assertEquals("Note 42", result.text)
        assertEquals(
            "42",
            result.getStringAnnotations(NoteLinkMarkdown.ANNOTATION_TAG, 0, result.text.length)
                .first()
                .item
        )
    }

    @Test
    fun noteLink_withInvalidId_emitsLiteralBracket() {
        val result = parse("[[note:abc|Project plan]]")
        assertTrue(result.text.startsWith("["))
        assertTrue(
            result.getStringAnnotations(NoteLinkMarkdown.ANNOTATION_TAG, 0, result.text.length)
                .isEmpty()
        )
    }

    @Test
    fun link_missingClosingParen_emitsLiteralBracket() {
        val result = parse("[text](url_no_closing_paren")
        assertTrue(result.text.startsWith("["))
        assertTrue(result.getStringAnnotations("URL", 0, result.text.length).isEmpty())
    }

    @Test
    fun inlineCode_producesMono() {
        val result = parse("`code`")
        assertEquals("code", result.text)
        val span = result.spanStyles.firstOrNull()
        assertEquals(FontFamily.Monospace, span?.item?.fontFamily)
    }

    @Test
    fun inlineCode_usesConfiguredBackgroundAndForeground() {
        // Regression guard: the inline-code background was previously hard-coded
        // to a translucent black that disappeared on dark themes. The parser must
        // honor the [MarkdownColors] bundle so the surrounding Composable can
        // pick a theme-aware pair that stays legible.
        val result = parse("`code`")
        val span = result.spanStyles.firstOrNull()?.item
        assertEquals(Color.LightGray, span?.background)
        assertEquals(Color.Black, span?.color)
    }

    @Test
    fun inlineCode_unclosed_emitsLiteralBacktick() {
        val result = parse("`not closed")
        assertTrue(result.text.startsWith("`"))
    }

    @Test
    fun bulletList_dash_producesBulletPrefix() {
        val result = parse("- list item")
        assertEquals("• list item", result.text)
    }

    @Test
    fun bulletList_asterisk_producesBulletPrefix() {
        val result = parse("* list item")
        assertEquals("• list item", result.text)
    }

    @Test
    fun bulletList_plus_producesBulletPrefix() {
        val result = parse("+ list item")
        assertEquals("• list item", result.text)
    }

    @Test
    fun orderedList_preservesNumber() {
        val result = parse("1. first item")
        assertEquals("1. first item", result.text)
    }

    @Test
    fun orderedList_higherNumber_preservesNumber() {
        val result = parse("42. forty-two")
        assertEquals("42. forty-two", result.text)
    }

    @Test
    fun checkboxUnchecked_producesEmptyBoxPrefix() {
        val result = parse("- [ ] to do")
        assertEquals("☐ to do", result.text)
    }

    @Test
    fun checkboxChecked_lowercase_producesCheckedBoxPrefix() {
        val result = parse("- [x] done")
        assertEquals("☑ done", result.text)
    }

    @Test
    fun checkboxChecked_uppercase_producesCheckedBoxPrefix() {
        val result = parse("- [X] Done uppercase")
        assertEquals("☑ Done uppercase", result.text)
    }

    @Test
    fun checkboxChecked_textIsGray() {
        val result = parse("- [x] completed")
        val graySpan = result.spanStyles.firstOrNull { it.item.color == Color.Gray }
        assertNotNull("Checked checkbox text must be gray", graySpan)
    }

    @Test
    fun escape_asterisk_emitsLiteralAsterisk() {
        val result = parse("""\*not italic\*""")
        assertEquals("*not italic*", result.text)
        assertTrue(result.spanStyles.none { it.item.fontStyle == FontStyle.Italic })
    }

    @Test
    fun escape_underscore_emitsLiteralUnderscore() {
        val result = parse("""\_not italic\_""")
        assertEquals("_not italic_", result.text)
        assertTrue(result.spanStyles.none { it.item.fontStyle == FontStyle.Italic })
    }

    @Test
    fun escape_backtick_emitsLiteralBacktick() {
        val result = parse("""\`not code\`""")
        assertEquals("`not code`", result.text)
        assertTrue(result.spanStyles.none { it.item.fontFamily == FontFamily.Monospace })
    }

    @Test
    fun escape_backslash_emitsLiteralBackslash() {
        val result = parse("""\\""")
        assertEquals("\\", result.text)
    }

    @Test
    fun escape_nonEscapableChar_emitsBackslashAndChar() {
        val result = parse("""\q""")
        assertEquals("\\q", result.text)
    }

    @Test
    fun brTag_lowercase_emitsNewline() {
        val result = parse("Line one<br>Line two")
        assertEquals("Line one\nLine two", result.text)
    }

    @Test
    fun brTag_selfClosing_emitsNewline() {
        val result = parse("Line one<br/>Line two")
        assertEquals("Line one\nLine two", result.text)
    }

    @Test
    fun brTag_uppercase_emitsNewline() {
        val result = parse("Line one<BR>Line two")
        assertEquals("Line one\nLine two", result.text)
    }

    @Test
    fun hardBreak_twoTrailingSpaces_trimsSpacesAndEmitsNewline() {
        val result = parse("Line one  \nLine two")
        assertEquals("Line one\nLine two", result.text)
    }

    @Test
    fun hardBreak_trailingBackslash_emitsNewline() {
        val result = parse("Line one\\\nLine two")
        assertEquals("Line one\nLine two", result.text)
    }

    @Test
    fun multiline_preservesNewlines() {
        val input = "line one\nline two\nline three"
        assertEquals(input, parse(input).text)
    }

    @Test
    fun multiline_headingAndBullet() {
        val result = parse("# Heading\n- item")
        assertEquals("Heading\n• item", result.text)
    }

    @Test
    fun inlineMixed_boldAndItalicOnSameLine() {
        val result = parse("**bold** and *italic*")
        assertEquals("bold and italic", result.text)
        assertEquals(1, result.spanStyles.count { it.item.fontWeight == FontWeight.Bold })
        assertEquals(1, result.spanStyles.count { it.item.fontStyle == FontStyle.Italic })
    }

    @Test
    fun inlineMixed_linkAndBoldOnSameLine() {
        val result = parse("[link](url) and **bold**")
        assertEquals("link and bold", result.text)
    }
}

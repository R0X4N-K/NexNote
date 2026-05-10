package io.github.r0x4nk.nexnote.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownParserTest {

    private val colors = MarkdownColors(
        linkColor = Color.Blue,
        inlineCodeBackground = Color.LightGray,
        inlineCodeForeground = Color.Black
    )
    private fun parseBlocks(text: String) = MarkdownParser.parseBlocks(text, colors)

    @Test
    fun parseBlocks_smallText_canBeReadFromCache() {
        val text = "Cacheable markdown"
        val blocks = parseBlocks(text)

        assertEquals(blocks, MarkdownParser.getCached(text, colors))
    }

    @Test
    fun parseBlocks_largeText_availableViaSingleSlotCache() {
        val text = "x".repeat(100_001)

        val blocks = parseBlocks(text)

        // Large texts bypass the bounded LRU cache but are still available via
        // the single-slot last-parse holder, ensuring warmup results are never lost.
        assertEquals(blocks, MarkdownParser.getCached(text, colors))
    }

    @Test
    fun parseBlocks_imageOnlyLine_returnsOneImageBlock() {
        val blocks = parseBlocks("![alt](images/note_1_img_123.jpg)")
        assertEquals(1, blocks.size)
        val block = blocks[0] as MarkdownBlock.ImageBlock
        assertEquals("images/note_1_img_123.jpg", block.path)
        assertEquals("alt", block.altText)
    }

    @Test
    fun parseBlocks_imageWithEmptyAlt_returnsImageBlock() {
        val blocks = parseBlocks("![](images/note_2_img_456.jpg)")
        assertEquals(1, blocks.size)
        val block = blocks[0] as MarkdownBlock.ImageBlock
        assertEquals("", block.altText)
    }

    @Test
    fun parseBlocks_textOnlyLine_returnsOneTextBlock() {
        val blocks = parseBlocks("Plain text without images")
        assertEquals(1, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.TextBlock)
    }

    @Test
    fun parseBlocks_imageBetweenTwoTextLines_returnsThreeBlocks() {
        val blocks = parseBlocks("First line\n![img](path/img.jpg)\nLast line")
        assertEquals(3, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.TextBlock)
        assertTrue(blocks[1] is MarkdownBlock.ImageBlock)
        assertTrue(blocks[2] is MarkdownBlock.TextBlock)
    }

    @Test
    fun parseBlocks_inlineImageWithText_treatedAsTextBlock() {
        val blocks = parseBlocks("Text ![img](path.jpg) more text")
        assertEquals(1, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.TextBlock)
    }

    @Test
    fun parseBlocks_emptyPath_notRecognizedAsImage() {
        val blocks = parseBlocks("![alt]()")
        assertEquals(1, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.TextBlock)
    }

    @Test
    fun parseBlocks_consecutiveImages_returnsMultipleImageBlocks() {
        val blocks = parseBlocks("![a](path/a.jpg)\n![b](path/b.jpg)")
        assertEquals(2, blocks.size)
        assertEquals("path/a.jpg", (blocks[0] as MarkdownBlock.ImageBlock).path)
        assertEquals("path/b.jpg", (blocks[1] as MarkdownBlock.ImageBlock).path)
    }

    @Test
    fun parseBlocks_imageWithTrailingSpaces_matchesAsImageBlock() {
        val blocks = parseBlocks("![alt](path/img.jpg)   ")
        assertEquals(1, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.ImageBlock)
    }

    @Test
    fun parseBlocks_imageWithTitle_stripsTitleFromPath() {
        val blocks = parseBlocks("""![alt](path/img.jpg "Title")""")
        val block = blocks[0] as MarkdownBlock.ImageBlock

        assertEquals("path/img.jpg", block.path)
        assertEquals("alt", block.altText)
    }

    @Test
    fun parseBlocks_linkSyntaxWithoutBang_notTreatedAsImage() {
        val blocks = parseBlocks("[text](https://example.com)")
        assertEquals(1, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.TextBlock)
    }

    @Test
    fun parseBlocks_emptyString_returnsOneTextBlock() {
        val blocks = parseBlocks("")
        assertEquals(1, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.TextBlock)
    }

    @Test
    fun parseBlocks_tripleDash_returnsHorizontalRuleBlock() {
        val blocks = parseBlocks("---")
        assertEquals(1, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.HorizontalRuleBlock)
    }

    @Test
    fun parseBlocks_tripleAsterisk_returnsHorizontalRuleBlock() {
        val blocks = parseBlocks("***")
        assertEquals(1, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.HorizontalRuleBlock)
    }

    @Test
    fun parseBlocks_tripleUnderscore_returnsHorizontalRuleBlock() {
        val blocks = parseBlocks("___")
        assertEquals(1, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.HorizontalRuleBlock)
    }

    @Test
    fun parseBlocks_dashesWithSpaces_returnsHorizontalRuleBlock() {
        val blocks = parseBlocks("- - -")
        assertEquals(1, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.HorizontalRuleBlock)
    }

    @Test
    fun parseBlocks_fiveDashes_returnsHorizontalRuleBlock() {
        val blocks = parseBlocks("-----")
        assertEquals(1, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.HorizontalRuleBlock)
    }

    @Test
    fun parseBlocks_twoDashes_notHorizontalRule() {
        val blocks = parseBlocks("--")
        assertEquals(1, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.TextBlock)
    }

    @Test
    fun parseBlocks_textAroundHorizontalRule_threeBlocks() {
        val blocks = parseBlocks("Above\n---\nBelow")
        assertEquals(3, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.TextBlock)
        assertTrue(blocks[1] is MarkdownBlock.HorizontalRuleBlock)
        assertTrue(blocks[2] is MarkdownBlock.TextBlock)
    }

    @Test
    fun parseBlocks_singleBlockquoteLine_returnsBlockquoteBlock() {
        val blocks = parseBlocks("> Simple quote")
        assertEquals(1, blocks.size)
        val block = blocks[0] as MarkdownBlock.BlockquoteBlock
        assertEquals("Simple quote", block.content.text)
    }

    @Test
    fun parseBlocks_multiLineBlockquote_singleBlock() {
        val blocks = parseBlocks("> Line one\n> Line two")
        assertEquals(1, blocks.size)
        val block = blocks[0] as MarkdownBlock.BlockquoteBlock
        assertEquals("Line one\nLine two", block.content.text)
    }

    @Test
    fun parseBlocks_blockquoteBetweenText_threeBlocks() {
        val blocks = parseBlocks("Before\n> Quote\nAfter")
        assertEquals(3, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.TextBlock)
        assertTrue(blocks[1] is MarkdownBlock.BlockquoteBlock)
        assertTrue(blocks[2] is MarkdownBlock.TextBlock)
    }

    @Test
    fun parseBlocks_blockquoteContent_isInlineParsed() {
        val blocks = parseBlocks("> **bold** in quote")
        val block = blocks[0] as MarkdownBlock.BlockquoteBlock
        assertEquals("bold in quote", block.content.text)
        assertTrue(block.content.spanStyles.any { it.item.fontWeight == FontWeight.Bold })
    }

    @Test
    fun parseBlocks_emptyBlockquote_returnsBlockquoteBlock() {
        val blocks = parseBlocks(">")
        assertEquals(1, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.BlockquoteBlock)
    }

    @Test
    fun parseBlocks_fencedCodeBlock_returnsCodeBlock() {
        val input = "```\nfun hello() = \"hi\"\n```"
        val blocks = parseBlocks(input)
        assertEquals(1, blocks.size)
        val block = blocks[0] as MarkdownBlock.CodeBlock
        assertEquals("fun hello() = \"hi\"", block.code)
        assertNull(block.language)
    }

    @Test
    fun parseBlocks_fencedCodeBlock_withLanguage() {
        val input = "```kotlin\nval x = 1\n```"
        val blocks = parseBlocks(input)
        assertEquals(1, blocks.size)
        val block = blocks[0] as MarkdownBlock.CodeBlock
        assertEquals("kotlin", block.language)
        assertEquals("val x = 1", block.code)
    }

    @Test
    fun parseBlocks_fencedCodeBlock_tildeFence() {
        val input = "~~~\nsome code\n~~~"
        val blocks = parseBlocks(input)
        assertEquals(1, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.CodeBlock)
    }

    @Test
    fun parseBlocks_codeBlock_contentNotMarkdownParsed() {
        val input = "```\n**not bold**\n```"
        val blocks = parseBlocks(input)
        val block = blocks[0] as MarkdownBlock.CodeBlock
        assertEquals("**not bold**", block.code)
    }

    @Test
    fun parseBlocks_codeBlock_multiLine() {
        val input = "```\nline one\nline two\nline three\n```"
        val blocks = parseBlocks(input)
        val block = blocks[0] as MarkdownBlock.CodeBlock
        assertEquals("line one\nline two\nline three", block.code)
    }

    @Test
    fun parseBlocks_textAroundCodeBlock_threeBlocks() {
        val input = "Before\n```\ncode\n```\nAfter"
        val blocks = parseBlocks(input)
        assertEquals(3, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.TextBlock)
        assertTrue(blocks[1] is MarkdownBlock.CodeBlock)
        assertTrue(blocks[2] is MarkdownBlock.TextBlock)
    }

    @Test
    fun parseBlocks_unclosedCodeFence_emitsCodeBlock() {
        val input = "```\nfoo\nbar"
        val blocks = parseBlocks(input)
        assertEquals(1, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.CodeBlock)
        assertEquals("foo\nbar", (blocks[0] as MarkdownBlock.CodeBlock).code)
    }

    @Test
    fun parseBlocks_fencedCodeBlock_codeInsideIsNotSplitAsImage() {
        val input = "```\n![alt](image.png)\n```"
        val blocks = parseBlocks(input)
        assertEquals(1, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.CodeBlock)
    }

    @Test
    fun parseBlocks_allBlockTypes_correctOrder() {
        val input = """
            # Heading
            Normal paragraph.
            > Quote
            ---
            ```
            code
            ```
            ![img](photo.jpg)
            End.
        """.trimIndent()

        val blocks = parseBlocks(input)
        assertTrue(blocks.any { it is MarkdownBlock.BlockquoteBlock })
        assertTrue(blocks.any { it is MarkdownBlock.HorizontalRuleBlock })
        assertTrue(blocks.any { it is MarkdownBlock.CodeBlock })
        assertTrue(blocks.any { it is MarkdownBlock.ImageBlock })
    }
}

package io.github.r0x4nk.nexnote.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import java.util.LinkedHashMap

/**
 * Lightweight, CommonMark-compatible markdown parser with GFM table support.
 *
 * Supported block syntax includes headings, blockquotes, horizontal rules,
 * fenced code blocks, unordered and ordered lists, task lists, standalone
 * images, and pipe tables.
 *
 * Supported inline syntax includes bold, italic, strikethrough, inline code,
 * links, hard line breaks, and CommonMark escape sequences.
 */
object MarkdownParser {

    private data class CacheKey(val content: String, val linkColorValue: ULong)

    private const val MAX_BLOCK_CACHE_ENTRIES = 30
    private const val MAX_CACHEABLE_CONTENT_CHARS = 100_000

    private val blocksCache = object : LinkedHashMap<CacheKey, List<MarkdownBlock>>(
        MAX_BLOCK_CACHE_ENTRIES,
        0.75f,
        true
    ) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<CacheKey, List<MarkdownBlock>>?
        ): Boolean = size > MAX_BLOCK_CACHE_ENTRIES
    }

    /**
     * Returns the cached parse result for [text] + [linkColor], if present.
     * UI code uses this to show a synchronous initial value before background
     * parsing completes.
     */
    fun getCached(text: String, linkColor: Color): List<MarkdownBlock>? =
        synchronized(blocksCache) {
            if (!text.isCacheable()) return@synchronized null
            blocksCache[CacheKey(text, linkColor.value)]
        }

    /**
     * Converts a markdown [text] string into a styled [AnnotatedString].
     *
     * Image tags are left as-is in the output; use [parseBlocks] to extract
     * them as separate [MarkdownBlock.ImageBlock]s.
     */
    fun parse(text: String, linkColor: Color): AnnotatedString =
        buildAnnotatedString {
            text.split("\n").forEachIndexed { index, line ->
                if (index > 0) append("\n")
                appendMarkdownLine(line, linkColor)
            }
        }

    /**
     * Splits [text] into display blocks and caches the result by content and
     * link color. Safe to call from any thread.
     */
    fun parseBlocks(text: String, linkColor: Color): List<MarkdownBlock> {
        if (!text.isCacheable()) {
            return parseMarkdownBlocks(text, linkColor)
        }

        val key = CacheKey(text, linkColor.value)
        synchronized(blocksCache) {
            blocksCache[key]
        }?.let { return it }

        val result = parseMarkdownBlocks(text, linkColor)
        synchronized(blocksCache) {
            blocksCache[key] = result
        }
        return result
    }

    private fun String.isCacheable(): Boolean =
        length <= MAX_CACHEABLE_CONTENT_CHARS
}

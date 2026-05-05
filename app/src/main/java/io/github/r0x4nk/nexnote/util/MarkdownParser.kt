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
     * Single-slot cache holding the most recently parsed result regardless of
     * content size. Uses a single volatile reference (not two separate fields) to
     * guarantee atomicity between key and result — preventing torn reads when the
     * warmup thread updates the slot concurrently with a main-thread [getCached]
     * read.
     *
     * This ensures that results produced by the preview warmup phase are
     * immediately available to [getCached] even for notes that exceed the LRU
     * cache size limit, eliminating the blank gap between animation end and
     * rendered content.
     */
    private data class LastParseEntry(val key: CacheKey, val blocks: List<MarkdownBlock>)

    @Volatile
    private var lastParse: LastParseEntry? = null

    /**
     * Returns the cached parse result for [text] + [linkColor], if present.
     * Checks both the bounded LRU cache (for smaller notes) and the single-slot
     * last-parse holder (for any size), so a preceding [parseBlocks] warmup call
     * always provides an immediate hit.
     */
    fun getCached(text: String, linkColor: Color): List<MarkdownBlock>? {
        val key = CacheKey(text, linkColor.value)

        // Check single-slot last-parse first (works for any content size)
        lastParse?.let { entry ->
            if (entry.key == key) return entry.blocks
        }

        // Fall back to the bounded LRU cache for smaller notes
        if (!text.isCacheable()) return null
        return synchronized(blocksCache) { blocksCache[key] }
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
     * Splits [text] into display blocks, caches the result, and returns it.
     * Safe to call from any thread.
     *
     * Results are stored in:
     * - The bounded LRU cache (for notes ≤ [MAX_CACHEABLE_CONTENT_CHARS])
     * - The single-slot last-parse holder (always, regardless of size)
     *
     * The single-slot holder guarantees that a warmup call immediately before
     * composition provides a synchronous hit via [getCached], even for very
     * large notes that would otherwise exceed the LRU cache size limit.
     */
    fun parseBlocks(text: String, linkColor: Color): List<MarkdownBlock> {
        val key = CacheKey(text, linkColor.value)

        // Check LRU cache first
        if (text.isCacheable()) {
            synchronized(blocksCache) { blocksCache[key] }?.let {
                lastParse = LastParseEntry(key, it)
                return it
            }
        }

        val result = parseMarkdownBlocks(text, linkColor)

        // Store in bounded LRU cache if within size limit
        if (text.isCacheable()) {
            synchronized(blocksCache) { blocksCache[key] = result }
        }
        // Always update single-slot holder so the next getCached() hits
        lastParse = LastParseEntry(key, result)

        return result
    }

    private fun String.isCacheable(): Boolean =
        length <= MAX_CACHEABLE_CONTENT_CHARS
}

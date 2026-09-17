package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.model.IndexedNoteStatistics
import io.github.r0x4nk.nexnote.util.TagParser

/** Extracts the text-derived fields stored in the statistics index. */
internal object NoteStatisticsTextAnalyzer {
    fun analyze(
        noteId: Long,
        content: String,
        creationDate: Long,
        lastModifiedDate: Long
    ): IndexedNoteStatistics = IndexedNoteStatistics(
        noteId = noteId,
        creationDate = creationDate,
        sourceLastModifiedDate = lastModifiedDate,
        characterCount = content.length,
        wordCount = countWords(content),
        tagNames = TagParser.extractTags(content)
    )

    // Reuse one matcher: Kotlin MatchResult.next() creates a new matcher per word,
    // repeatedly resetting the entire input in Android's regex implementation.
    private fun countWords(content: String): Int {
        val matcher = WORD_PATTERN.toPattern().matcher(content)
        var count = 0
        while (matcher.find()) count++
        return count
    }

    private val WORD_PATTERN = Regex("""[\p{L}\p{N}]+(?:['’][\p{L}\p{N}]+)*""")
}

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
        wordCount = WORD_PATTERN.findAll(content).count(),
        tagNames = TagParser.extractTags(content)
    )

    private val WORD_PATTERN = Regex("""[\p{L}\p{N}]+(?:['’][\p{L}\p{N}]+)*""")
}

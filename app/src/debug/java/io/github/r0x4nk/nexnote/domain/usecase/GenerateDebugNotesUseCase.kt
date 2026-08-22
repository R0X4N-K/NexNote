package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.util.NoteLinkMarkdown
import java.util.TimeZone
import kotlin.math.max
import kotlin.random.Random

/**
 * Creates varied normal notes for exercising list, search, agenda, tag and
 * Markdown performance in development builds.
 */
internal class GenerateDebugNotesUseCase(
    private val saveNote: suspend (Note) -> Long,
    private val indexNoteTags: suspend (Long, String) -> Unit,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
    private val random: Random = Random.Default
) {
    /**
     * Persists [count] test notes through the same save and tag-index paths used
     * by the editor. [onProgress] reports the number of rows already persisted.
     */
    suspend operator fun invoke(
        count: Int,
        onProgress: (createdCount: Int) -> Unit = {}
    ): Int {
        require(count in MIN_NOTE_COUNT..MAX_NOTE_COUNT) {
            "Note count must be between $MIN_NOTE_COUNT and $MAX_NOTE_COUNT."
        }

        val now = currentTimeMillis()
        val timezone = TimeZone.getDefault().id
        val persistedNotes = ArrayList<PersistedDebugNote>(count)

        repeat(count) { index ->
            val title = createTitle(index)
            val creationDate = createCreationDate(index, count, now)
            val content = createContent(
                title = title,
                index = index,
                previousNotes = persistedNotes
            )
            val noteId = saveNote(
                Note(
                    title = title,
                    content = content,
                    isMarkdown = true,
                    creationDate = creationDate,
                    timezone = timezone,
                    isPinned = index > 0 && index % PIN_INTERVAL == 0,
                    backgroundColor = backgroundColors[index % backgroundColors.size],
                    isPreviewMode = index % PREVIEW_INTERVAL == 0
                )
            )
            persistedNotes += PersistedDebugNote(noteId, title)
            val createdCount = index + 1
            onProgress(createdCount)
            indexNoteTags(noteId, content)
        }

        return persistedNotes.size
    }

    private fun createTitle(index: Int): String {
        val adjective = titleAdjectives.random(random)
        val subject = titleSubjects.random(random)
        return "$adjective $subject ${index + 1}"
    }

    private fun createCreationDate(index: Int, count: Int, now: Long): Long {
        if (count == 1) return now - random.nextLong(MAX_NOTE_AGE_MILLIS + 1L)

        val bucketWidth = max(1L, MAX_NOTE_AGE_MILLIS / (count - 1L))
        val bucketStart = index * bucketWidth
        val jitter = random.nextLong(bucketWidth)
        val age = (bucketStart + jitter).coerceAtMost(MAX_NOTE_AGE_MILLIS)
        return now - age
    }

    private fun createContent(
        title: String,
        index: Int,
        previousNotes: List<PersistedDebugNote>
    ): String {
        val tags = tagPool.shuffled(random).take(1 + random.nextInt(MAX_TAGS_PER_NOTE))
        val paragraphCount = paragraphCounts[index % paragraphCounts.size]
        val internalLink = previousNotes.takeIf { it.isNotEmpty() }
            ?.random(random)
            ?.let { NoteLinkMarkdown.create(it.id, it.title) }

        return buildString {
            append("# ").append(title).append("\n\n")
            append(introSentences.random(random)).append("\n\n")
            append("## Context\n\n")
            repeat(paragraphCount) { paragraphIndex ->
                append(createParagraph(index, paragraphIndex)).append("\n\n")
            }
            append("## Tasks\n\n")
            append("- [ ] Review the current implementation\n")
            append("- [x] Capture a reproducible scenario\n")
            append("- [ ] Compare results on another device\n\n")
            append("> ").append(quotes.random(random)).append("\n\n")
            append("## References\n\n")
            append("- [Android Developers](https://developer.android.com/)\n")
            append("- [CommonMark specification](https://spec.commonmark.org/)\n")
            internalLink?.let { append("- Related note: ").append(it).append("\n") }
            append("\n")
            if (index % TABLE_INTERVAL == 0) {
                append("| Metric | Value |\n")
                append("| --- | ---: |\n")
                append("| Iteration | ").append(index + 1).append(" |\n")
                append("| Paragraphs | ").append(paragraphCount).append(" |\n\n")
            }
            if (index % CODE_INTERVAL == 0) {
                append("```kotlin\n")
                append("val sampleIndex = ").append(index + 1).append("\n")
                append("check(sampleIndex > 0)\n")
                append("```\n\n")
            }
            append(tags.joinToString(separator = " ") { "#$it" })
        }
    }

    private fun createParagraph(noteIndex: Int, paragraphIndex: Int): String {
        val sentenceCount = 2 + random.nextInt(4)
        return buildString {
            repeat(sentenceCount) { sentenceIndex ->
                if (sentenceIndex > 0) append(' ')
                append(bodySentences.random(random))
            }
            if ((noteIndex + paragraphIndex) % INLINE_FORMAT_INTERVAL == 0) {
                append(" The values **must remain stable** and `renderTimeMs` is recorded.")
            }
        }
    }

    private data class PersistedDebugNote(
        val id: Long,
        val title: String
    )

    companion object {
        const val DEFAULT_NOTE_COUNT = 500
        const val MIN_NOTE_COUNT = 1
        const val MAX_NOTE_COUNT = 10_000

        private const val MAX_NOTE_AGE_MILLIS = 3L * 365L * 24L * 60L * 60L * 1_000L
        private const val MAX_TAGS_PER_NOTE = 5
        private const val PIN_INTERVAL = 17
        private const val PREVIEW_INTERVAL = 3
        private const val TABLE_INTERVAL = 4
        private const val CODE_INTERVAL = 5
        private const val INLINE_FORMAT_INTERVAL = 3

        private val paragraphCounts = intArrayOf(1, 3, 7, 14, 28)
        private val backgroundColors = listOf(
            null,
            0xFFFFF3E0.toInt(),
            0xFFE8F5E9.toInt(),
            0xFFE3F2FD.toInt(),
            0xFFF3E5F5.toInt()
        )
        private val titleAdjectives = listOf(
            "Focused", "Practical", "Weekly", "Detailed", "Quick", "Exploratory",
            "Reliable", "Technical", "Creative", "Field"
        )
        private val titleSubjects = listOf(
            "performance review", "project log", "research brief", "meeting notes",
            "release checklist", "design journal", "reading list", "experiment report",
            "travel plan", "learning roadmap"
        )
        private val tagPool = listOf(
            "android", "benchmark", "compose", "database", "design", "documentation",
            "idea", "kotlin", "markdown", "performance", "personal", "project",
            "release", "research", "testing", "todo", "work"
        )
        private val introSentences = listOf(
            "This generated note combines representative content for development testing.",
            "This sample captures a realistic mix of prose, structure, links, and metadata.",
            "This entry is synthetic data intended for repeatable performance checks.",
            "This note models the kind of mixed content used during everyday planning."
        )
        private val bodySentences = listOf(
            "The first pass establishes a baseline before any optimization is applied.",
            "Measurements should include cold start, scrolling, filtering, and search latency.",
            "A representative data set makes regressions easier to reproduce and compare.",
            "The implementation keeps persistence behavior aligned with notes created by the editor.",
            "Longer passages exercise text measurement while shorter entries vary the visible layout.",
            "Tags connect the sample to filtered views and usage-based rankings.",
            "Creation dates are distributed over time so agenda queries cover many calendar ranges.",
            "Links and Markdown blocks exercise parsing without requiring external test fixtures.",
            "Results are more useful when the same interaction is repeated on comparable devices.",
            "Rendering and database work should be observed separately when investigating a slowdown."
        )
        private val quotes = listOf(
            "Measure first, then optimize the bottleneck that the data actually reveals.",
            "A useful benchmark resembles real usage closely enough to expose practical limits.",
            "Synthetic data is valuable when its shape remains diverse and its purpose is explicit."
        )
    }
}

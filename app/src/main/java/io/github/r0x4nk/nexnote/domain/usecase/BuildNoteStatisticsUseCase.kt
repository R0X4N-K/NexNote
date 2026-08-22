package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.model.DailyWritingActivity
import io.github.r0x4nk.nexnote.domain.model.IndexedNoteStatistics
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.NoteStatistics
import io.github.r0x4nk.nexnote.domain.model.Tag
import io.github.r0x4nk.nexnote.domain.model.TagUsageStatistic
import io.github.r0x4nk.nexnote.util.TagParser
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.yield

/** Prepares calendar-year writing statistics from active, non-Vault notes. */
class BuildNoteStatisticsUseCase {

    private val cacheLock = Any()
    private var cachedHistory: CachedStatisticsHistory? = null
    private val cachedNoteAnalyses = mutableMapOf<Long, CachedNoteAnalysis>()

    /**
     * Parses note text and timestamps once, then returns an immutable history
     * that can produce individual years without scanning the source lists again.
     */
    internal fun prepare(
        notes: List<Note>,
        tags: List<Tag>,
        timezoneId: String,
        nowMillis: Long = System.currentTimeMillis()
    ): PreparedNoteStatistics {
        val zoneId = timezoneId.toZoneIdOrUtc()
        val today = nowMillis.toLocalDate(zoneId)
        val sourceVersion = buildSourceVersion(notes, tags, timezoneId, today)
        synchronized(cacheLock) {
            cachedHistory
                ?.takeIf { cached -> cached.sourceVersion == sourceVersion }
                ?.history
                ?.let { return it }
        }

        return buildPreparedHistory(notes, tags, zoneId, today).also { history ->
            cacheCompletedHistory(sourceVersion, history, notes)
        }
    }

    /** Builds history from persisted text analyses without reading note bodies. */
    internal fun prepareIndexed(
        notes: List<IndexedNoteStatistics>,
        tags: List<Tag>,
        timezoneId: String,
        nowMillis: Long = System.currentTimeMillis()
    ): PreparedNoteStatistics {
        val zoneId = timezoneId.toZoneIdOrUtc()
        val today = nowMillis.toLocalDate(zoneId)
        val accumulator = StatisticsAccumulator(zoneId, today)
        tags.forEach(accumulator::addTag)
        notes.forEach(accumulator::addIndexedNote)
        return accumulator.freeze()
    }

    /**
     * Emits bounded partial histories while note contents are analyzed. Cached
     * note analyses make an insertion proportional to the changed text instead
     * of reparsing every existing note.
     */
    internal fun prepareIncrementally(
        notes: List<Note>,
        tags: List<Tag>,
        timezoneId: String,
        nowMillis: Long = System.currentTimeMillis()
    ): Flow<StatisticsPreparationProgress> = flow {
        val zoneId = timezoneId.toZoneIdOrUtc()
        val today = nowMillis.toLocalDate(zoneId)
        val sourceVersion = buildSourceVersion(notes, tags, timezoneId, today)
        val cached = synchronized(cacheLock) {
            cachedHistory?.takeIf { history -> history.sourceVersion == sourceVersion }
        }
        if (cached != null) {
            emit(
                StatisticsPreparationProgress(
                    history = cached.history,
                    processedNotes = notes.size,
                    totalNotes = notes.size,
                    isComplete = true,
                    isHistoryCacheHit = true
                )
            )
            return@flow
        }

        val accumulator = StatisticsAccumulator(zoneId, today)
        tags.forEach(accumulator::addTag)
        if (notes.isEmpty()) {
            val history = accumulator.freeze()
            cacheCompletedHistory(sourceVersion, history, notes)
            emit(
                StatisticsPreparationProgress(
                    history = history,
                    processedNotes = 0,
                    totalNotes = 0,
                    isComplete = true
                )
            )
            return@flow
        }

        emit(
            StatisticsPreparationProgress(
                history = accumulator.freeze(),
                processedNotes = 0,
                totalNotes = notes.size,
                isComplete = false
            )
        )
        val batchSize = (notes.size / TARGET_PROGRESS_UPDATES)
            .coerceIn(MIN_PROGRESS_BATCH_SIZE, MAX_PROGRESS_BATCH_SIZE)
        notes.forEachIndexed { index, note ->
            currentCoroutineContext().ensureActive()
            accumulator.addNote(note, note.analysis())
            val processedNotes = index + 1
            val isComplete = processedNotes == notes.size
            if (isComplete || processedNotes % batchSize == 0) {
                val history = accumulator.freeze()
                if (isComplete) {
                    cacheCompletedHistory(sourceVersion, history, notes)
                }
                emit(
                    StatisticsPreparationProgress(
                        history = history,
                        processedNotes = processedNotes,
                        totalNotes = notes.size,
                        isComplete = isComplete
                    )
                )
                if (!isComplete) yield()
            }
        }
    }

    private fun buildPreparedHistory(
        notes: List<Note>,
        tags: List<Tag>,
        zoneId: ZoneId,
        today: LocalDate
    ): PreparedNoteStatistics {
        val accumulator = StatisticsAccumulator(zoneId, today)
        tags.forEach(accumulator::addTag)
        notes.forEach { note -> accumulator.addNote(note, note.analysis()) }
        return accumulator.freeze()
    }

    private fun Note.analysis(): NoteTextAnalysis {
        val version = statisticsVersion()
        synchronized(cacheLock) {
            cachedNoteAnalyses[id]
                ?.takeIf { cached -> cached.version == version }
                ?.analysis
                ?.let { return it }
        }

        val analysis = NoteTextAnalysis(
            wordCount = content.wordCount(),
            tagNames = TagParser.extractTags(content)
        )
        synchronized(cacheLock) {
            cachedNoteAnalyses[id] = CachedNoteAnalysis(version, analysis)
        }
        return analysis
    }

    private fun cacheCompletedHistory(
        sourceVersion: StatisticsSourceVersion,
        history: PreparedNoteStatistics,
        notes: List<Note>
    ) {
        val activeNoteIds = notes.asSequence().map(Note::id).toSet()
        synchronized(cacheLock) {
            cachedHistory = CachedStatisticsHistory(sourceVersion, history)
            cachedNoteAnalyses.keys.retainAll(activeNoteIds)
        }
    }

    /** Builds one snapshot directly for callers that do not need year switching. */
    operator fun invoke(
        notes: List<Note>,
        tags: List<Tag>,
        selectedYear: Int,
        timezoneId: String,
        nowMillis: Long = System.currentTimeMillis()
    ): NoteStatistics = prepare(
        notes = notes,
        tags = tags,
        timezoneId = timezoneId,
        nowMillis = nowMillis
    ).statisticsFor(selectedYear)
}

internal data class StatisticsPreparationProgress(
    val history: PreparedNoteStatistics,
    val processedNotes: Int,
    val totalNotes: Int,
    val isComplete: Boolean,
    val isHistoryCacheHit: Boolean = false
)

private data class CachedStatisticsHistory(
    val sourceVersion: StatisticsSourceVersion,
    val history: PreparedNoteStatistics
)

private data class StatisticsSourceVersion(
    val timezoneId: String,
    val today: LocalDate,
    val notes: Map<Long, NoteStatisticsVersion>,
    val tags: Map<String, TagStatisticsVersion>
)

private data class NoteStatisticsVersion(
    val id: Long,
    val creationDate: Long,
    val lastModifiedDate: Long,
    val contentLength: Int
)

private data class TagStatisticsVersion(
    val name: String,
    val createdDate: Long
)

private fun Note.statisticsVersion(): NoteStatisticsVersion = NoteStatisticsVersion(
    id = id,
    creationDate = creationDate,
    lastModifiedDate = lastModifiedDate,
    contentLength = content.length
)

private fun Tag.statisticsVersion(): TagStatisticsVersion = TagStatisticsVersion(
    name = name,
    createdDate = createdDate
)

private fun buildSourceVersion(
    notes: List<Note>,
    tags: List<Tag>,
    timezoneId: String,
    today: LocalDate
): StatisticsSourceVersion = StatisticsSourceVersion(
    timezoneId = timezoneId,
    today = today,
    notes = notes.associate { note -> note.id to note.statisticsVersion() },
    tags = tags.associate { tag -> tag.name to tag.statisticsVersion() }
)

private data class CachedNoteAnalysis(
    val version: NoteStatisticsVersion,
    val analysis: NoteTextAnalysis
)

private data class NoteTextAnalysis(
    val wordCount: Int,
    val tagNames: Set<String>
)

private class StatisticsAccumulator(
    private val zoneId: ZoneId,
    private val today: LocalDate
) {
    private val years = mutableMapOf<Int, MutableYearStatistics>()
    private val activeDates = mutableSetOf<LocalDate>()
    private val noteYears = mutableSetOf<Int>()

    fun addNote(note: Note, analysis: NoteTextAnalysis) {
        val date = note.creationDate.toLocalDate(zoneId)
        years.getOrPut(date.year, ::MutableYearStatistics).addNote(
            date = date,
            wordCount = analysis.wordCount,
            characterCount = note.charCount,
            tagNames = analysis.tagNames
        )
        activeDates += date
        noteYears += date.year
    }

    fun addIndexedNote(note: IndexedNoteStatistics) {
        val date = note.creationDate.toLocalDate(zoneId)
        years.getOrPut(date.year, ::MutableYearStatistics).addNote(
            date = date,
            wordCount = note.wordCount,
            characterCount = note.characterCount,
            tagNames = note.tagNames
        )
        activeDates += date
        noteYears += date.year
    }

    fun addTag(tag: Tag) {
        val date = tag.createdDate.toLocalDate(zoneId)
        years.getOrPut(date.year, ::MutableYearStatistics).addCreatedTag(date)
        activeDates += date
    }

    fun freeze(): PreparedNoteStatistics {
        val immutableYears = years.mapValues { (_, value) -> value.freeze() }
        val earliestYear = (immutableYears.keys.minOrNull() ?: today.year)
            .coerceAtMost(today.year)
        val latestYear = (immutableYears.keys.maxOrNull() ?: today.year)
            .coerceAtLeast(today.year)
        return PreparedNoteStatistics(
            currentYear = today.year,
            earliestYear = earliestYear,
            latestYear = latestYear,
            today = today,
            latestNoteYear = noteYears.maxOrNull(),
            currentStreakDays = activeDates.currentStreakEndingOn(today),
            years = immutableYears
        )
    }
}

/** Preprocessed statistics source whose yearly snapshots require no note-text parsing. */
internal class PreparedNoteStatistics internal constructor(
    val currentYear: Int,
    val earliestYear: Int,
    val latestYear: Int,
    val today: LocalDate,
    private val latestNoteYear: Int?,
    private val currentStreakDays: Int,
    private val years: Map<Int, YearStatistics>
) {
    /** Uses the requested year when it has notes, otherwise the latest year that does. */
    fun initialYearFor(requestedYear: Int): Int {
        val requested = requestedYear.coerceIn(earliestYear, latestYear)
        return if ((years[requested]?.noteCount ?: 0) > 0) {
            requested
        } else {
            latestNoteYear?.coerceIn(earliestYear, latestYear) ?: requested
        }
    }

    /** Returns the requested year, clamped to the available calendar range. */
    fun statisticsFor(selectedYear: Int): NoteStatistics {
        val year = selectedYear.coerceIn(earliestYear, latestYear)
        val yearStatistics = years[year] ?: YearStatistics.EMPTY
        val days = year.calendarDates().map { date ->
            yearStatistics.days[date]?.toActivity(date) ?: DailyWritingActivity(date = date)
        }
        val activeDates = days.asSequence()
            .filter(DailyWritingActivity::hasActivity)
            .map(DailyWritingActivity::date)
            .toSet()

        return NoteStatistics(
            selectedYear = year,
            currentYear = currentYear,
            earliestYear = earliestYear,
            latestYear = latestYear,
            today = today,
            days = days,
            totalNotes = yearStatistics.noteCount,
            totalWords = yearStatistics.wordCount,
            totalCharacters = yearStatistics.characterCount,
            totalTagsCreated = yearStatistics.tagsCreated,
            activeDays = activeDates.size,
            averageWordsPerNote = yearStatistics.averageWordsPerNote,
            longestNoteWords = yearStatistics.longestNoteWords,
            currentStreakDays = currentStreakDays,
            longestStreakDays = activeDates.longestStreak(),
            busiestDay = days.asSequence()
                .filter(DailyWritingActivity::hasActivity)
                .maxWithOrNull(DAILY_ACTIVITY_COMPARATOR),
            notesByWeekday = DayOfWeek.entries.associateWith { weekday ->
                yearStatistics.notesByWeekday[weekday] ?: 0
            },
            topTags = yearStatistics.tagNoteCounts.entries
                .asSequence()
                .sortedWith(
                    compareByDescending<Map.Entry<String, Int>> { it.value }
                        .thenBy(Map.Entry<String, Int>::key)
                )
                .take(TOP_TAGS_LIMIT)
                .map { (name, count) -> TagUsageStatistic(name = name, noteCount = count) }
                .toList()
        )
    }
}

private class MutableYearStatistics {
    private val days = mutableMapOf<LocalDate, MutableDailyActivity>()
    private val notesByWeekday = mutableMapOf<DayOfWeek, Int>()
    private val tagNoteCounts = mutableMapOf<String, Int>()
    private var noteCount = 0
    private var wordCount = 0L
    private var characterCount = 0L
    private var tagsCreated = 0
    private var longestNoteWords = 0

    fun addNote(
        date: LocalDate,
        wordCount: Int,
        characterCount: Int,
        tagNames: Set<String>
    ) = addNote(date, wordCount.toLong(), characterCount.toLong(), tagNames)

    fun addNote(
        date: LocalDate,
        wordCount: Long,
        characterCount: Long,
        tagNames: Set<String>
    ) {
        noteCount += 1
        this.wordCount += wordCount
        this.characterCount += characterCount
        longestNoteWords = maxOf(
            longestNoteWords,
            wordCount.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        )
        notesByWeekday.merge(date.dayOfWeek, 1, Int::plus)
        tagNames.forEach { tagName -> tagNoteCounts.merge(tagName, 1, Int::plus) }
        days.getOrPut(date, ::MutableDailyActivity).addNote(wordCount, characterCount)
    }

    fun addCreatedTag(date: LocalDate) {
        tagsCreated += 1
        days.getOrPut(date, ::MutableDailyActivity).addCreatedTag()
    }

    fun freeze(): YearStatistics = YearStatistics(
        days = days.mapValues { (_, value) -> value.freeze() },
        noteCount = noteCount,
        wordCount = wordCount,
        characterCount = characterCount,
        tagsCreated = tagsCreated,
        longestNoteWords = longestNoteWords,
        notesByWeekday = notesByWeekday.toMap(),
        tagNoteCounts = tagNoteCounts.toMap()
    )
}

private class MutableDailyActivity {
    private var noteCount = 0
    private var wordCount = 0L
    private var characterCount = 0L
    private var tagsCreated = 0

    fun addNote(wordCount: Long, characterCount: Long) {
        noteCount += 1
        this.wordCount += wordCount
        this.characterCount += characterCount
    }

    fun addCreatedTag() {
        tagsCreated += 1
    }

    fun freeze(): DailyActivityAggregate = DailyActivityAggregate(
        noteCount = noteCount,
        wordCount = wordCount,
        characterCount = characterCount,
        tagsCreated = tagsCreated
    )
}

internal data class YearStatistics(
    val days: Map<LocalDate, DailyActivityAggregate>,
    val noteCount: Int,
    val wordCount: Long,
    val characterCount: Long,
    val tagsCreated: Int,
    val longestNoteWords: Int,
    val notesByWeekday: Map<DayOfWeek, Int>,
    val tagNoteCounts: Map<String, Int>
) {
    val averageWordsPerNote: Int
        get() = if (noteCount == 0) 0 else (wordCount.toDouble() / noteCount).roundToInt()

    companion object {
        val EMPTY = YearStatistics(
            days = emptyMap(),
            noteCount = 0,
            wordCount = 0,
            characterCount = 0,
            tagsCreated = 0,
            longestNoteWords = 0,
            notesByWeekday = emptyMap(),
            tagNoteCounts = emptyMap()
        )
    }
}

internal data class DailyActivityAggregate(
    val noteCount: Int,
    val wordCount: Long,
    val characterCount: Long,
    val tagsCreated: Int
) {
    fun toActivity(date: LocalDate): DailyWritingActivity = DailyWritingActivity(
        date = date,
        noteCount = noteCount,
        wordCount = wordCount,
        characterCount = characterCount,
        tagsCreated = tagsCreated,
        activityLevel = activityLevel(
            noteCount = noteCount,
            wordCount = wordCount,
            tagsCreated = tagsCreated
        )
    )
}

/**
 * Maps activity to four stable levels. Word buckets are capped so a long note
 * enriches the signal without overwhelming note and tag creation.
 */
private fun activityLevel(noteCount: Int, wordCount: Long, tagsCreated: Int): Int {
    if (noteCount == 0 && tagsCreated == 0) return 0
    val wordPoints = ceil(wordCount / WORD_BUCKET_SIZE.toDouble())
        .coerceAtMost(MAX_WORD_POINTS.toDouble())
        .toInt()
    val score = noteCount * NOTE_POINTS + wordPoints + tagsCreated
    return when (score) {
        in 0..2 -> 1
        in 3..5 -> 2
        in 6..9 -> 3
        else -> 4
    }
}

private fun Int.calendarDates(): List<LocalDate> {
    val first = LocalDate.of(this, 1, 1)
    return List(first.lengthOfYear()) { offset -> first.plusDays(offset.toLong()) }
}

private fun Set<LocalDate>.currentStreakEndingOn(today: LocalDate): Int {
    var date = today
    var streak = 0
    while (date in this) {
        streak += 1
        date = date.minusDays(1)
    }
    return streak
}

private fun Set<LocalDate>.longestStreak(): Int {
    if (isEmpty()) return 0
    var longest = 1
    var current = 1
    sorted().zipWithNext().forEach { (previous, next) ->
        current = if (previous.plusDays(1) == next) current + 1 else 1
        longest = maxOf(longest, current)
    }
    return longest
}

private fun String.wordCount(): Int = WORD_PATTERN.findAll(this).count()

private fun Long.toLocalDate(zoneId: ZoneId): LocalDate =
    Instant.ofEpochMilli(this).atZone(zoneId).toLocalDate()

private fun String.toZoneIdOrUtc(): ZoneId =
    runCatching { ZoneId.of(this) }.getOrDefault(ZoneId.of("UTC"))

private val DAILY_ACTIVITY_COMPARATOR =
    compareBy<DailyWritingActivity> { day -> day.activityLevel }
        .thenBy { day -> day.noteCount }
        .thenBy { day -> day.wordCount }
private val WORD_PATTERN = Regex("""[\p{L}\p{N}]+(?:['’][\p{L}\p{N}]+)*""")

private const val TOP_TAGS_LIMIT = 5
private const val WORD_BUCKET_SIZE = 250
private const val MAX_WORD_POINTS = 4
private const val NOTE_POINTS = 2
private const val TARGET_PROGRESS_UPDATES = 40
private const val MIN_PROGRESS_BATCH_SIZE = 1
private const val MAX_PROGRESS_BATCH_SIZE = 250

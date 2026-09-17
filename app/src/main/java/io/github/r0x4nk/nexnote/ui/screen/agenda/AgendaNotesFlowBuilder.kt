package io.github.r0x4nk.nexnote.ui.screen.agenda

import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.NotePinnedFilter
import io.github.r0x4nk.nexnote.domain.model.NoteSearchScope
import io.github.r0x4nk.nexnote.domain.model.NoteSearchSort
import io.github.r0x4nk.nexnote.domain.model.ScoredNote
import io.github.r0x4nk.nexnote.domain.usecase.ObserveDistinctLocalDaysUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveFilteredNoteIdsUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveNotesByDateRangeUseCase
import io.github.r0x4nk.nexnote.ui.common.SortOrder
import io.github.r0x4nk.nexnote.util.DateUtils
import io.github.r0x4nk.nexnote.util.SearchUtils
import io.github.r0x4nk.nexnote.util.TagParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

internal fun buildAgendaDaysWithNotesFlow(
    observeDistinctLocalDays: ObserveDistinctLocalDaysUseCase,
    scope: CoroutineScope
): StateFlow<Set<Long>> {
    return observeDistinctLocalDays().stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptySet()
    )
}

@OptIn(ExperimentalCoroutinesApi::class)
internal fun buildAgendaRawNotesForDayFlow(
    selectedDate: Flow<SelectedDate>,
    observeNotesByDateRange: ObserveNotesByDateRangeUseCase
): Flow<List<Note>> {
    return selectedDate.flatMapLatest { date ->
        val noon = DateUtils.toMillis(date.year, date.month, date.day)
        observeNotesByDateRange(
            startMs = DateUtils.startOfDay(noon),
            endMs = DateUtils.startOfNextDay(noon)
        )
    }
}

/**
 * Groups already filtered, searched and sorted notes into the iPhone-Notes
 * style sections (Today, Yesterday, the last 7 and 30 days, then calendar
 * months for the last year and years before that). Sections are always emitted
 * most-recent first; the notes inside a section keep the order applied by
 * [buildAgendaProcessedNotesFlow].
 *
 * [nowProvider] is injectable so the relative boundaries can be tested without
 * depending on the wall clock.
 */
internal fun buildAgendaTimelineFlow(
    processedNotes: Flow<AgendaProcessedNotes>,
    nowProvider: () -> Long = System::currentTimeMillis
): Flow<List<AgendaTimelineSection>> {
    return processedNotes.map { processed ->
        val displayItems = processed.scoredResults.ifEmpty {
            processed.notes.map { note ->
                ScoredNote(note, score = 0, titleRanges = emptyList(), contentRanges = emptyList())
            }
        }
        agendaTimelineSections(displayItems, nowProvider())
    }
}

/**
 * Bucket of the Agenda timeline before it is turned into a labeled
 * [AgendaTimelineSection]. [month] is 0-based (Calendar convention).
 */
internal data class AgendaTimelineBucket(
    val kind: AgendaSectionKind,
    val year: Int = 0,
    val month: Int = 0
)

/**
 * Resolves the relative/calendar bucket for a note created at [creationDate],
 * evaluated against [nowMillis] in the device timezone.
 */
internal fun agendaTimelineBucketFor(
    creationDate: Long,
    nowMillis: Long,
    zoneId: ZoneId = ZoneId.systemDefault()
): AgendaTimelineBucket {
    val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
    val creationDay = Instant.ofEpochMilli(creationDate).atZone(zoneId).toLocalDate()
    val daysAgo = ChronoUnit.DAYS.between(creationDay, today)
    return when {
        daysAgo <= 0L -> AgendaTimelineBucket(AgendaSectionKind.TODAY)
        daysAgo == 1L -> AgendaTimelineBucket(AgendaSectionKind.YESTERDAY)
        daysAgo <= 7L -> AgendaTimelineBucket(AgendaSectionKind.LAST_7_DAYS)
        daysAgo <= 30L -> AgendaTimelineBucket(AgendaSectionKind.LAST_30_DAYS)
        creationDay.isBefore(today.minusMonths(12)) -> AgendaTimelineBucket(
            kind = AgendaSectionKind.YEAR,
            year = creationDay.year
        )
        else -> AgendaTimelineBucket(
            kind = AgendaSectionKind.MONTH,
            year = creationDay.year,
            month = creationDay.monthValue - 1
        )
    }
}

/**
 * Builds the ordered timeline sections from already processed display items.
 */
internal fun agendaTimelineSections(
    items: List<ScoredNote>,
    nowMillis: Long
): List<AgendaTimelineSection> {
    if (items.isEmpty()) return emptyList()

    val buckets = items.groupBy { item -> agendaTimelineBucketFor(item.note.creationDate, nowMillis) }
    val sections = mutableListOf<AgendaTimelineSection>()

    fun add(bucket: AgendaTimelineBucket, key: String) {
        val bucketItems = buckets[bucket] ?: return
        sections += AgendaTimelineSection(
            key = key,
            kind = bucket.kind,
            year = bucket.year,
            month = bucket.month,
            items = bucketItems
        )
    }

    add(AgendaTimelineBucket(AgendaSectionKind.TODAY), "today")
    add(AgendaTimelineBucket(AgendaSectionKind.YESTERDAY), "yesterday")
    add(AgendaTimelineBucket(AgendaSectionKind.LAST_7_DAYS), "last_7_days")
    add(AgendaTimelineBucket(AgendaSectionKind.LAST_30_DAYS), "last_30_days")

    buckets.keys
        .filter { bucket -> bucket.kind == AgendaSectionKind.MONTH }
        .sortedByDescending { bucket -> bucket.year * MONTHS_PER_YEAR + bucket.month }
        .forEach { bucket ->
            add(bucket, "month_${bucket.year}_${bucket.month}")
        }

    buckets.keys
        .filter { bucket -> bucket.kind == AgendaSectionKind.YEAR }
        .sortedByDescending { bucket -> bucket.year }
        .forEach { bucket ->
            add(bucket, "year_${bucket.year}")
        }

    return sections
}

private const val MONTHS_PER_YEAR = 12


@OptIn(ExperimentalCoroutinesApi::class)
internal fun buildAgendaFilteredNoteIdsFlow(
    selectedTagFilters: Flow<Set<String>>,
    observeFilteredNoteIds: ObserveFilteredNoteIdsUseCase
): Flow<Set<Long>> {
    return selectedTagFilters.flatMapLatest { filters ->
        if (filters.isEmpty()) flowOf(emptySet())
        else observeFilteredNoteIds(filters)
    }
}

@OptIn(FlowPreview::class)
internal fun buildAgendaProcessedNotesFlow(
    rawNotesForDay: Flow<List<Note>>,
    filteredNoteIds: Flow<Set<Long>>,
    searchQuery: Flow<String>,
    sortOrder: Flow<SortOrder>,
    searchSort: Flow<NoteSearchSort>,
    searchScope: Flow<NoteSearchScope>,
    pinnedFilter: Flow<NotePinnedFilter>,
    searchDebounceMs: Long
): Flow<AgendaProcessedNotes> {
    val searchOptions = combine(
        searchQuery.debounce(searchDebounceMs),
        searchSort,
        searchScope,
        pinnedFilter,
        ::AgendaSearchOptions
    )
    return combine(
        rawNotesForDay,
        filteredNoteIds,
        sortOrder,
        searchOptions
    ) { notes, tagIds, order, options ->
        val availableTagNames = notes.asSequence()
            .flatMap { note -> TagParser.extractTags(note.content).asSequence() }
            .toSet()
        val tagFiltered = if (tagIds.isEmpty()) notes else notes.filter { it.id in tagIds }
        val pinnedFiltered = tagFiltered.filter { note ->
            when (options.pinnedFilter) {
                NotePinnedFilter.ALL -> true
                NotePinnedFilter.PINNED -> note.isPinned
                NotePinnedFilter.UNPINNED -> !note.isPinned
            }
        }
        if (options.query.isBlank()) {
            return@combine AgendaProcessedNotes(
                notes = pinnedFiltered.sortedByPinnedAndModifiedDate(order),
                availableTagNames = availableTagNames,
                appliedSortOrder = order,
                appliedSearchSort = options.searchSort
            )
        }

        val scored = SearchUtils.searchAndSort(
            notes = pinnedFiltered,
            query = options.query,
            scope = options.searchScope,
            sort = options.searchSort
        )
        AgendaProcessedNotes(
            notes = scored.map(ScoredNote::note),
            scoredResults = scored,
            availableTagNames = availableTagNames,
                appliedSortOrder = order,
                appliedSearchSort = options.searchSort
        )
    }
}

internal data class AgendaProcessedNotes(
    val notes: List<Note>,
    val scoredResults: List<ScoredNote> = emptyList(),
    val availableTagNames: Set<String> = emptySet(),
    val appliedSortOrder: SortOrder = SortOrder.MODIFIED_DESC,
    val appliedSearchSort: NoteSearchSort = NoteSearchSort.RELEVANCE
)

private data class AgendaSearchOptions(
    val query: String,
    val searchSort: NoteSearchSort,
    val searchScope: NoteSearchScope,
    val pinnedFilter: NotePinnedFilter
)

private fun List<Note>.sortedByPinnedAndModifiedDate(order: SortOrder): List<Note> {
    val comparator = when (order) {
        SortOrder.MODIFIED_DESC ->
            compareByDescending<Note> { it.isPinned }
                .thenByDescending { it.lastModifiedDate }
                .thenBy { it.id }

        SortOrder.MODIFIED_ASC ->
            compareByDescending<Note> { it.isPinned }
                .thenBy { it.lastModifiedDate }
                .thenBy { it.id }
    }

    return sortedWith(comparator)
}

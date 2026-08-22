package io.github.r0x4nk.nexnote.ui.screen.statistics

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.r0x4nk.nexnote.di.requireAppDependencies
import io.github.r0x4nk.nexnote.domain.model.DailyWritingActivity
import io.github.r0x4nk.nexnote.domain.model.NoteStatistics
import io.github.r0x4nk.nexnote.domain.model.IndexedNoteStatistics
import io.github.r0x4nk.nexnote.domain.model.NoteStatisticsIndexState
import io.github.r0x4nk.nexnote.domain.model.Tag
import io.github.r0x4nk.nexnote.domain.usecase.BuildNoteStatisticsUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveIndexedNoteStatisticsUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveNoteStatisticsIndexStateUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveTagsByUsageDescUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveTimezoneIdUseCase
import io.github.r0x4nk.nexnote.domain.usecase.StatisticsPreparationProgress
import io.github.r0x4nk.nexnote.util.NexNoteDebugLog
import java.time.LocalDate
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn

@Immutable
data class StatisticsUiState(
    val statistics: NoteStatistics? = null,
    val selectedDay: DailyWritingActivity? = null,
    val isCalculating: Boolean = false,
    val processedNotes: Int = 0,
    val totalNotes: Int = 0,
    val isRetryingAfterError: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class StatisticsViewModel(
    observeIndexedNotes: ObserveIndexedNoteStatisticsUseCase,
    observeIndexState: ObserveNoteStatisticsIndexStateUseCase,
    observeTags: ObserveTagsByUsageDescUseCase,
    observeTimezoneId: ObserveTimezoneIdUseCase,
    buildStatistics: BuildNoteStatisticsUseCase,
    private val nowMillis: () -> Long = System::currentTimeMillis,
    computationDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    private val selectedYear = MutableStateFlow(LocalDate.now().year)
    private val selectedEpochDay = MutableStateFlow<Long?>(null)
    private var hasResolvedInitialYear = false

    private val coherentIndexSnapshots = combine(
        observeIndexedNotes(),
        observeIndexState(),
        ::StatisticsIndexSnapshot
    ).filter { snapshot ->
        snapshot.notes.size == snapshot.indexState.indexedNotes
    }

    private val preparationProgress = combine(
        coherentIndexSnapshots,
        observeTags(),
        observeTimezoneId()
    ) { snapshot, tags, timezoneId ->
        StatisticsSourceData(snapshot.notes, tags, timezoneId, snapshot.indexState)
    }
        .conflate()
        .flatMapLatest { source ->
            val startedAtNanos = System.nanoTime()
            val history = buildStatistics.prepareIndexed(
                notes = source.notes,
                tags = source.tags,
                timezoneId = source.timezoneId,
                nowMillis = nowMillis()
            )
            val progress = StatisticsPreparationProgress(
                history = history,
                processedNotes = source.notes.size,
                totalNotes = source.indexState.totalNotes,
                isComplete = source.notes.size >= source.indexState.totalNotes
            )
            val elapsedMs = (System.nanoTime() - startedAtNanos) / NANOS_PER_MILLISECOND
            NexNoteDebugLog.viewModel(event = "statisticsIndexSnapshot") {
                "elapsedMs=$elapsedMs processed=${progress.processedNotes} " +
                    "total=${progress.totalNotes}"
            }
            kotlinx.coroutines.flow.flowOf(progress)
        }
        .conflate()
        .flowOn(computationDispatcher)
        .onEach { progress ->
            if (!hasResolvedInitialYear && progress.isComplete && progress.totalNotes > 0) {
                hasResolvedInitialYear = true
                selectedYear.value = progress.history.initialYearFor(selectedYear.value)
            }
        }

    private val calculationCandidates = combine(
        preparationProgress,
        selectedYear
    ) { progress, year ->
        val startedAtNanos = System.nanoTime()
        val statistics = progress.history.statisticsFor(year)
        if (progress.isComplete) {
            NexNoteDebugLog.viewModel(event = "statisticsYearSnapshotReady") {
                val elapsedMicros = (System.nanoTime() - startedAtNanos) / NANOS_PER_MICROSECOND
                "year=${statistics.selectedYear} elapsedMicros=$elapsedMicros"
            }
        }
        StatisticsCalculationCandidate(
            statistics = statistics,
            requestedYear = year,
            processedNotes = progress.processedNotes,
            totalNotes = progress.totalNotes,
            isComplete = progress.isComplete
        )
    }.flowOn(computationDispatcher)

    private val renderedStatistics = calculationCandidates.scan(
        initial = StatisticsRenderState()
    ) { previous, candidate ->
        previous.accept(candidate)
    }

    val uiState: StateFlow<StatisticsUiState> = combine(
        renderedStatistics,
        selectedEpochDay,
        observeIndexState()
    ) { renderState, epochDay, indexState ->
        val snapshot = renderState.statistics
        StatisticsUiState(
            statistics = snapshot,
            selectedDay = snapshot?.resolveSelectedDay(epochDay),
            isCalculating = renderState.isCalculating,
            processedNotes = renderState.processedNotes,
            totalNotes = renderState.totalNotes,
            isRetryingAfterError = indexState.isRetryingAfterError
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = StatisticsUiState()
    )

    fun showPreviousYear() {
        val snapshot = uiState.value.statistics ?: return
        if (snapshot.selectedYear > snapshot.earliestYear) {
            selectYear(snapshot.selectedYear - 1)
        }
    }

    fun showNextYear() {
        val snapshot = uiState.value.statistics ?: return
        if (snapshot.selectedYear < snapshot.latestYear) {
            selectYear(snapshot.selectedYear + 1)
        }
    }

    fun selectDay(date: LocalDate) {
        val snapshot = uiState.value.statistics ?: return
        if (date.year == snapshot.selectedYear) {
            selectedEpochDay.value = date.toEpochDay()
        }
    }

    private fun selectYear(year: Int) {
        hasResolvedInitialYear = true
        selectedEpochDay.value = null
        selectedYear.value = year
    }

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L
        private const val NANOS_PER_MILLISECOND = 1_000_000L
        private const val NANOS_PER_MICROSECOND = 1_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val useCases = requireAppDependencies().useCases
                StatisticsViewModel(
                    observeIndexedNotes = useCases.statistics.observeIndexedNotes,
                    observeIndexState = useCases.statistics.observeIndexState,
                    observeTags = useCases.tags.observeTagsByUsageDesc,
                    observeTimezoneId = useCases.preferences.observeTimezoneId,
                    buildStatistics = useCases.notes.buildNoteStatistics
                )
            }
        }
    }
}

private data class StatisticsCalculationCandidate(
    val statistics: NoteStatistics,
    val requestedYear: Int,
    val processedNotes: Int,
    val totalNotes: Int,
    val isComplete: Boolean
)

private data class StatisticsRenderState(
    val statistics: NoteStatistics? = null,
    val requestedYear: Int? = null,
    val displayedProcessedNotes: Int = 0,
    val processedNotes: Int = 0,
    val totalNotes: Int = 0,
    val isCalculating: Boolean = false
) {
    fun accept(candidate: StatisticsCalculationCandidate): StatisticsRenderState {
        val shouldReplaceSnapshot = statistics == null ||
            requestedYear != candidate.requestedYear ||
            candidate.isComplete ||
            candidate.processedNotes >= displayedProcessedNotes
        return copy(
            statistics = if (shouldReplaceSnapshot) candidate.statistics else statistics,
            requestedYear = candidate.requestedYear,
            displayedProcessedNotes = if (shouldReplaceSnapshot) {
                candidate.processedNotes
            } else {
                displayedProcessedNotes
            },
            processedNotes = candidate.processedNotes,
            totalNotes = candidate.totalNotes,
            isCalculating = !candidate.isComplete
        )
    }
}

private data class StatisticsSourceData(
    val notes: List<IndexedNoteStatistics>,
    val tags: List<Tag>,
    val timezoneId: String,
    val indexState: NoteStatisticsIndexState
)

/** Rows and counters emitted from Room only become a UI source when they agree. */
private data class StatisticsIndexSnapshot(
    val notes: List<IndexedNoteStatistics>,
    val indexState: NoteStatisticsIndexState
)

private fun NoteStatistics.resolveSelectedDay(epochDay: Long?): DailyWritingActivity? {
    epochDay?.let { value ->
        activityOn(LocalDate.ofEpochDay(value))?.let { return it }
    }
    if (selectedYear == currentYear) {
        activityOn(today)?.let { return it }
    }
    return busiestDay ?: days.firstOrNull()
}

package io.github.r0x4nk.nexnote.ui.screen.editor

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditorTagNavigatorTest {

    @Test
    fun `clicking the selected tag cycles through all occurrences`() = runTest {
        val navigator = EditorTagNavigator(
            uiState = MutableStateFlow(
                EditorUiState(content = "#tag one #tag two #tag")
            ),
            onError = {}
        )
        val events = mutableListOf<TagSearchState>()
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            navigator.tagSearchEvent.take(4).toList(events)
        }

        navigator.onTagChipClick("tag")
        navigator.onTagChipClick("tag")
        navigator.onTagChipClick("tag")
        navigator.onTagChipClick("tag")

        collectJob.join()

        assertEquals(listOf(0, 9, 18, 0), events.map { it.charOffset })
        assertEquals(listOf(0, 1, 2, 0), events.map { it.occurrenceIndex })
        assertTrue(events.all { it.totalOccurrences == 3 })
        assertEquals("tag", navigator.selectedTagsInEditor.value)
    }

    @Test
    fun `clicking a different tag starts from its first occurrence`() = runTest {
        val navigator = EditorTagNavigator(
            uiState = MutableStateFlow(
                EditorUiState(content = "#one #one #two #two")
            ),
            onError = {}
        )
        val events = mutableListOf<TagSearchState>()
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            navigator.tagSearchEvent.take(3).toList(events)
        }

        navigator.onTagChipClick("one")
        navigator.onTagChipClick("one")
        navigator.onTagChipClick("two")

        collectJob.join()

        assertEquals(
            listOf(
                TagSearchState(tagName = "one", charOffset = 0, occurrenceIndex = 0, totalOccurrences = 2),
                TagSearchState(tagName = "one", charOffset = 5, occurrenceIndex = 1, totalOccurrences = 2),
                TagSearchState(tagName = "two", charOffset = 10, occurrenceIndex = 0, totalOccurrences = 2)
            ),
            events
        )
        assertEquals("two", navigator.selectedTagsInEditor.value)
    }
}

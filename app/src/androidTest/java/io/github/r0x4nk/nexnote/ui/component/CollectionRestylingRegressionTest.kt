package io.github.r0x4nk.nexnote.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.r0x4nk.nexnote.ui.common.NoteCollectionSortEffect
import io.github.r0x4nk.nexnote.ui.theme.NexNoteTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CollectionRestylingRegressionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun sortResetsScrollButEntryPreservesRestoredPosition() {
        val sort = mutableStateOf(false)
        val list = LazyListState(firstVisibleItemIndex = 12)
        compose.setContent {
            NoteCollectionSortEffect(sort.value, list)
            LazyColumn(state = list, modifier = Modifier.fillMaxSize()) {
                items(40) { Box(Modifier.height(100.dp)) }
            }
        }
        compose.runOnIdle { assertEquals(12, list.firstVisibleItemIndex) }
        compose.runOnIdle { sort.value = true }
        compose.runOnIdle { assertEquals(0, list.firstVisibleItemIndex) }
    }

    @Test fun progressDoesNotFlashForFastOperationsButAppearsForSlowOnes() {
        val label = mutableStateOf<String?>(null)
        compose.mainClock.autoAdvance = false
        compose.setContent { NexNoteTheme { OperationProgressDialog(label.value) } }
        compose.runOnUiThread { label.value = "Deleting notes" }
        compose.mainClock.advanceTimeBy(100)
        compose.onNodeWithText("Deleting notes").assertDoesNotExist()
        compose.runOnUiThread { label.value = null }
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithText("Deleting notes").assertDoesNotExist()
        compose.runOnUiThread { label.value = "Restoring notes" }
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithText("Restoring notes").assertIsDisplayed()
    }
}

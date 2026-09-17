package io.github.r0x4nk.nexnote.ui.screen.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performSemanticsAction
import android.content.Intent
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import io.github.r0x4nk.nexnote.NexNoteApp
import org.junit.Before
import org.junit.After
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.r0x4nk.nexnote.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeveloperGenerationNavigationTest {
    @get:Rule val compose = createEmptyComposeRule()
    private lateinit var activity: MainActivity

    @Before fun launch() {
        val app = ApplicationProvider.getApplicationContext<NexNoteApp>()
        activity = InstrumentationRegistry.getInstrumentation().startActivitySync(
            Intent(app, MainActivity::class.java).setAction(Intent.ACTION_MAIN)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        ) as MainActivity
    }

    @After fun close() {
        if (::activity.isInitialized) {
            InstrumentationRegistry.getInstrumentation().runOnMainSync { activity.finishAndRemoveTask() }
        }
    }

    @Test fun generationContinuesWhileAgendaReceivesNewNotes() {
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNodeWithTag(SETTINGS_LIST_TAG).performScrollToNode(
            hasTestTag(SETTINGS_DEVELOPER_NOTE_COUNT_FIELD_TAG)
        )
        compose.onNodeWithTag(SETTINGS_DEVELOPER_NOTE_COUNT_FIELD_TAG).performTextReplacement("200")
        compose.onNodeWithTag(SETTINGS_LIST_TAG).performScrollToNode(
            hasTestTag(SETTINGS_DEVELOPER_GENERATE_BUTTON_TAG)
        )
        // The settings list scrolls under the floating bottom navigation, so a
        // coordinate tap can be swallowed by a navigation item. Invoke the
        // button's semantics action directly to exercise generation instead of
        // the overlay hit testing.
        compose.onNodeWithTag(SETTINGS_DEVELOPER_GENERATE_BUTTON_TAG)
            .assertIsDisplayed()
            .assertIsEnabled()
            .performSemanticsAction(SemanticsActions.OnClick)
        lateinit var generator: DeveloperToolsViewModel
        compose.runOnUiThread {
            generator = ViewModelProvider(activity, DeveloperToolsViewModel.Factory)[DeveloperToolsViewModel::class.java]
        }
        assertTrue("Generation must still be active when navigating", generator.uiState.value.isGenerating)
        compose.onNodeWithContentDescription("Agenda").performClick()
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText("MONTH").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("MONTH").assertIsDisplayed()
        compose.onNodeWithContentDescription("Previous month").performClick()
        compose.onNodeWithContentDescription("Next month").performClick()
        compose.waitUntil(180_000) { !generator.uiState.value.isGenerating }
        assertNull(generator.uiState.value.error)
        assertEquals(200, generator.uiState.value.lastGeneratedCount)
        compose.onNodeWithText("MONTH").assertIsDisplayed()
    }
}

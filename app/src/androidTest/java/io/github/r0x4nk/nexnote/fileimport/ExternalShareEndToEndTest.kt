package io.github.r0x4nk.nexnote.fileimport

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.r0x4nk.nexnote.MainActivity
import io.github.r0x4nk.nexnote.NexNoteApp
import io.github.r0x4nk.nexnote.ui.screen.editor.EDITOR_CONTENT_FIELD_TAG
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExternalShareEndToEndTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun manifestResolvesTextAndImageShares() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        for (mime in listOf("text/plain", "image/png", "image/jpeg")) {
            val intent = Intent(Intent.ACTION_SEND).setType(mime).setPackage(context.packageName)
                .addCategory(Intent.CATEGORY_DEFAULT)
            assertNotNull("Missing share target for $mime", intent.resolveActivity(context.packageManager))
        }
    }

    @Test
    fun coldAndWarmSharesOpenNewNotesAndRecreationDoesNotDuplicate() {
        val app = ApplicationProvider.getApplicationContext<NexNoteApp>()
        app.database.clearAllTables()
        fun share(body: String) = Intent(Intent.ACTION_SEND).setType("text/plain")
            .setPackage(app.packageName).putExtra(Intent.EXTRA_TEXT, body)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val launchIntent = share("First shared body").setClass(app, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        val firstActivity = instrumentation.startActivitySync(launchIntent) as MainActivity
        try {
            waitForNotes(app, 1)
            waitForEditorContent("First shared body")
            composeRule.onNodeWithTag(EDITOR_CONTENT_FIELD_TAG).assertTextContains("First shared body")
            instrumentation.runOnMainSync { firstActivity.recreate() }
            composeRule.waitUntil(10_000L) {
                var recreated = false
                instrumentation.runOnMainSync {
                    recreated = ActivityLifecycleMonitorRegistry.getInstance()
                        .getActivitiesInStage(Stage.RESUMED)
                        .any { it is MainActivity && it !== firstActivity }
                }
                recreated
            }
            composeRule.waitForIdle()
            assertEquals(1, runBlocking { app.noteRepository.allNotes.first().size })
            app.startActivity(share("Second shared body"))
            waitForNotes(app, 2)
            waitForEditorContent("Second shared body")
            composeRule.onNodeWithTag(EDITOR_CONTENT_FIELD_TAG)
                .assertIsDisplayed().assertTextContains("Second shared body")
            assertEquals(setOf("First shared body", "Second shared body"),
                runBlocking { app.noteRepository.allNotes.first().map { it.content }.toSet() })
        } finally {
            instrumentation.runOnMainSync {
                ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                    .filterIsInstance<MainActivity>().forEach { it.finishAndRemoveTask() }
            }
            instrumentation.waitForIdleSync()
            app.database.clearAllTables()
        }
    }

    private fun waitForEditorContent(body: String) {
        composeRule.waitUntil(10_000L) {
            composeRule.onAllNodes(hasTestTag(EDITOR_CONTENT_FIELD_TAG) and hasText(body, substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForNotes(app: NexNoteApp, count: Int) {
        composeRule.waitUntil(10_000L) {
            runBlocking { app.noteRepository.allNotes.first().size == count }
        }
        composeRule.waitForIdle()
    }
}

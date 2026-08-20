package io.github.r0x4nk.nexnote.fileimport

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.r0x4nk.nexnote.MainActivity
import io.github.r0x4nk.nexnote.NexNoteApp
import io.github.r0x4nk.nexnote.ui.screen.editor.EDITOR_CONTENT_FIELD_TAG
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Covers intent -> ContentResolver -> parser -> Room save -> editor navigation. */
@RunWith(AndroidJUnit4::class)
class ExternalFileOpenEndToEndTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun viewContentIntentImportsAndOpensTheSavedNoteInEditor() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val app = context.applicationContext as NexNoteApp
        app.database.clearAllTables()
        val source = File(context.cacheDir, "exports/open-end-to-end.md").apply {
            parentFile?.mkdirs()
            writeText("# Imported heading\n\nEnd-to-end body")
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            source
        )
        val launchIntent = Intent(context, MainActivity::class.java)
            .setAction(Intent.ACTION_VIEW)
            .setDataAndType(uri, "text/markdown")
            .addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK
            )

        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val activity = instrumentation.startActivitySync(launchIntent) as MainActivity
        try {
            composeRule.waitUntil(timeoutMillis = 10_000L) {
                runBlocking {
                    app.noteRepository.allNotes.first().any { note ->
                        note.title == "open-end-to-end" &&
                            note.content == "# Imported heading\n\nEnd-to-end body"
                    }
                }
            }

            composeRule.onNodeWithTag(EDITOR_CONTENT_FIELD_TAG).assertIsDisplayed()
        } finally {
            instrumentation.runOnMainSync { activity.finishAndRemoveTask() }
            instrumentation.waitForIdleSync()
        }

        source.delete()
        app.database.clearAllTables()
    }
}

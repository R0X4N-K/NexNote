package io.github.r0x4nk.nexnote.fileimport

import android.app.ActivityManager
import android.content.ClipData
import android.content.ComponentName
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import io.github.r0x4nk.nexnote.MainActivity
import io.github.r0x4nk.nexnote.NexNoteApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExternalIntentTaskReuseTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val app = ApplicationProvider.getApplicationContext<NexNoteApp>()

    @Before fun prepare() {
        finishMainActivities()
        app.database.clearAllTables()
    }
    @After fun cleanup() {
        finishMainActivities()
        app.startActivity(senderIntent().putExtra("finish_sender", true))
        app.database.clearAllTables()
    }

    @Test fun foreignSharesReuseExistingMainAcrossCallerFlags() {
        app.startActivity(Intent(app, MainActivity::class.java).setAction(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        val original = awaitMain()
        val originalTask = original.taskId
        val flags = listOf(
            0,
            Intent.FLAG_ACTIVITY_NEW_TASK,
            Intent.FLAG_ACTIVITY_NEW_DOCUMENT,
            Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK,
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK,
            Intent.FLAG_ACTIVITY_CLEAR_TOP,
            Intent.FLAG_ACTIVITY_REORDER_TO_FRONT,
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT
        )
        flags.forEachIndexed { index, flag ->
            send(share("Share $index").addFlags(flag))
            awaitNotes(index + 1)
            assertSingleMain(originalTask, original)
        }
        // Selecting the launcher again must not replay the last share.
        app.startActivity(Intent(app, MainActivity::class.java).setAction(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        awaitResumedMain()
        assertSingleMain(originalTask, original)
        assertEquals(flags.size, notesCount())
    }

    @Test fun coldShareStartsAppTaskAndRepeatedIdenticalSharesAreIntentionalNewNotes() {
        send(share("Repeated share"))
        awaitNotes(1)
        val first = awaitMain()
        assertTrue("Cold share must give NexNote its own root", first.isTaskRoot)
        send(share("Repeated share"))
        awaitNotes(2)
        assertSingleMain(first.taskId, first)
        // Configuration recreation must not import the original intent again.
        instrumentation.runOnMainSync { first.recreate() }
        await { mainActivities().singleOrNull()?.let { it !== first && !it.isDestroyed } == true }
        val recreated = awaitResumedMain()
        assertEquals(first.taskId, recreated.taskId)
        assertEquals(2, notesCount())
        send(share("After recreation"))
        awaitNotes(3)
        assertSingleMain(recreated.taskId, recreated)
    }

    @Test fun callerAboveMainAndViewAndImageIntentsReuseTheSameTask() {
        send(share("Initial"))
        awaitNotes(1)
        val first = awaitMain()
        // A foreign activity is now pushed on top of NexNote's own task.
        send(share("From overlay"), overlayAbove = first)
        awaitNotes(2)
        assertSingleMain(first.taskId, first)
        val uri = android.net.Uri.parse("content://${instrumentation.context.packageName}.fixture/fixture.md")
        send(Intent(Intent.ACTION_VIEW).setClass(app, MainActivity::class.java)
            .setDataAndType(uri, "text/markdown")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK))
        awaitNotes(3)
        assertSingleMain(first.taskId, first)
        val imageUri = android.net.Uri.parse("content://${instrumentation.context.packageName}.fixture/fixture.png")
        send(Intent(Intent.ACTION_SEND).setClass(app, MainActivity::class.java).setType("image/png")
            .putExtra(Intent.EXTRA_STREAM, imageUri).apply { clipData = ClipData.newRawUri("test", imageUri) }
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK), overlayAbove = first)
        awaitNotes(4)
        await { runBlocking { app.noteRepository.allNotes.first().any { it.imagePaths.isNotEmpty() } } }
        assertSingleMain(first.taskId, first)


    }

    @Test fun rapidSharesAndUnsupportedIntentsDoNotCreateMoreActivities() {
        send(share("Initial rapid"))
        awaitNotes(1)
        val first = awaitMain()
        instrumentation.runOnMainSync {
            repeat(6) { first.startActivity(share("Rapid $it")) }
        }
        awaitNotes(7)
        assertSingleMain(first.taskId, first)
        send(Intent(app, MainActivity::class.java).setAction(Intent.ACTION_SEND_MULTIPLE)
            .setType("text/plain").addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK))
        awaitResumedMain()
        assertSingleMain(first.taskId, first)
        assertEquals(7, notesCount())
    }

    private fun share(body: String) = Intent(Intent.ACTION_SEND).setType("text/plain")
        .setClass(app, MainActivity::class.java).putExtra(Intent.EXTRA_TEXT, body)

    private fun senderIntent(overlay: Boolean = false) = Intent().setComponent(ComponentName(
        instrumentation.context.packageName,
        if (overlay) ExternalOverlaySenderActivity::class.java.name else ExternalTaskSenderActivity::class.java.name
    )).apply { if (!overlay) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }

    private fun send(outbound: Intent, overlayAbove: MainActivity? = null) {
        val wrapper = senderIntent(overlayAbove != null).putExtra("forward_intent", outbound)
        // The foreign sender owns these fixture URIs and grants them when forwarding.
        wrapper.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (overlayAbove == null) app.startActivity(wrapper)
        else instrumentation.runOnMainSync { overlayAbove.startActivity(wrapper) }
    }

    private fun notesCount() = runBlocking { app.noteRepository.allNotes.first().size }
    private fun awaitNotes(count: Int) { await { notesCount() == count }; awaitResumedMain() }
    private fun awaitMain(): MainActivity { await { mainActivities().isNotEmpty() }; return awaitResumedMain() }
    private fun awaitResumedMain(): MainActivity {
        var result: MainActivity? = null
        await {
            instrumentation.runOnMainSync {
                result = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                    .filterIsInstance<MainActivity>().singleOrNull()
            }
            result != null
        }
        return result!!
    }
    private fun assertSingleMain(taskId: Int, activity: MainActivity) {
        instrumentation.waitForIdleSync()
        val live = mainActivities()
        assertEquals("MainActivity instances: ${live.map { it.taskId }}", 1, live.size)
        assertTrue("A different live instance must be a completed configuration recreation",
            activity === live.single() || activity.isDestroyed)
        assertEquals(taskId, live.single().taskId)
        val tasks = app.getSystemService(ActivityManager::class.java).appTasks
            .map { it.taskInfo }.filter { it.baseActivity?.packageName == app.packageName }
        assertEquals("NexNote entries in Recents", 1, tasks.size)
    }
    private fun mainActivities(): List<MainActivity> {
        var result = emptyList<MainActivity>()
        instrumentation.runOnMainSync {
            result = Stage.values().filter { it != Stage.DESTROYED }.flatMap {
                ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(it)
            }.filterIsInstance<MainActivity>().filterNot { it.isFinishing || it.isDestroyed }.distinct()
        }
        return result
    }
    private fun finishMainActivities() {
        val activities = mainActivities()
        instrumentation.runOnMainSync { activities.forEach { it.finishAndRemoveTask() } }
        instrumentation.waitForIdleSync()
    }
    private fun await(predicate: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 15_000
        while (System.currentTimeMillis() < deadline) {
            if (predicate()) return
            Thread.sleep(50)
        }
        error("External intent did not reach the expected activity/note state")
    }
}

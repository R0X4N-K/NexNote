package io.github.r0x4nk.nexnote.ui.screen.editor

import android.content.ContentValues
import android.provider.MediaStore
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.ComponentActivity
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.r0x4nk.nexnote.NexNoteApp
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.VaultState
import io.github.r0x4nk.nexnote.domain.model.VaultAutoLockTimeout
import io.github.r0x4nk.nexnote.ui.navigation.VaultAutoLockOnStopEffect
import io.github.r0x4nk.nexnote.ui.navigation.VaultAutoLockOnResumeEffect
import io.github.r0x4nk.nexnote.ui.theme.NexNoteTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Uses Android's real DocumentsUI and ActivityResult callback, not an injected URI. */
@RunWith(AndroidJUnit4::class)
class EditorVaultAttachmentPickerTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun documentPickerRequiresReauthenticationWithBackgroundLock() = exercisePicker(true)
    @Test fun documentPickerRequiresReauthenticationWithResumeTimeoutOnly() = exercisePicker(false)

    @Test fun cancelAfterSelectionKeepsVaultLockedAndDiscardsAttachment() = exercisePicker(true, cancel = true)

    private fun exercisePicker(lockOnStop: Boolean, cancel: Boolean = false) {
        val app = ApplicationProvider.getApplicationContext<NexNoteApp>()
        val pin = "135790"
        val id = runBlocking {
            if (app.vaultRepository.state.first() == VaultState.NOT_CONFIGURED) {
                app.vaultRepository.configurePin(pin.toCharArray())
            }
            assertTrue(app.vaultRepository.unlockWithPin(pin.toCharArray()))
            app.vaultNoteRepository.saveVaultNote(Note(content = "Private original", isInVault = true))
        }
        val name = "vault-picker-${System.nanoTime()}.txt"
        val resolver = app.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
        })!!
        resolver.openOutputStream(uri)!!.use { it.write("Selected document".toByteArray()) }
        val lockCalls = java.util.concurrent.atomic.AtomicInteger()
        lateinit var editor: EditorViewModel
        rule.setContent {
            NexNoteTheme {
                val vaultState by app.vaultRepository.state.collectAsStateWithLifecycle(VaultState.LOCKED)
                VaultAutoLockOnStopEffect(vaultState, lockOnStop) { lockCalls.incrementAndGet(); app.vaultRepository.lock() }
                VaultAutoLockOnResumeEffect(vaultState, VaultAutoLockTimeout.IMMEDIATELY) { lockCalls.incrementAndGet(); app.vaultRepository.lock() }
                editor = viewModel(factory = EditorViewModel.factory(EditorMode.VaultNote(id)))
                val state = rememberEditorScreenState(EditorMode.VaultNote(id))
                val launch = rememberLaunchAttachmentPicker(LocalContext.current, state, editor)
                EditorAttachmentResumeEffect(editor)
                Button(onClick = launch) { Text("Choose test attachment") }
            }
        }
        try {
            rule.waitUntil(10_000) { !editor.uiState.value.isLoading }
            rule.runOnIdle { editor.onContentChange("Saved before opening picker") }
            rule.onNodeWithText("Choose test attachment").performClick()
            rule.onNodeWithText("Document or other file").performClick()
            // A warm DocumentsUI can return before ON_STOP if clicked immediately.
            // Exercise an actual background interval, as when a person browses files.
            rule.waitUntil(10_000) {
                rule.activity.lifecycle.currentState == androidx.lifecycle.Lifecycle.State.CREATED
            }
            // DocumentsUI is outside the Compose tree. Select the actual MediaStore file.
            clickSystemNode(waitForDisappearance = false) { it.contentDescription?.toString() == "Show roots" }
            clickSystemNode(waitForDisappearance = false) { it.text?.toString() == "Downloads" }
            clickSystemNode { it.text?.toString() == name }
            try {
                rule.waitUntil(10_000) { editor.pendingAttachment.hasResult.value }
            } catch (error: Throwable) {
                throw AssertionError("Pending result missing: locks=${lockCalls.get()}, " +
                    "repo=${runBlocking { app.vaultRepository.state.first() }}, " +
                    "editorLocked=${editor.uiState.value.isVaultLocked}, " +
                    "images=${editor.uiState.value.imagePaths.size}, " +
                    "error=${editor.uiState.value.errorMessage}, lifecycle=${rule.activity.lifecycle.currentState}", error)
            }
            rule.waitUntil(10_000) { editor.uiState.value.isVaultLocked }
            assertEquals(VaultState.LOCKED, runBlocking { app.vaultRepository.state.first() })
            assertEquals("", editor.uiState.value.content)
            assertTrue(editor.uiState.value.imagePaths.isEmpty())
            if (cancel) {
                rule.onNodeWithText("Cancel attachment").performClick()
                rule.waitUntil(5_000) { !editor.pendingAttachment.hasResult.value }
                assertEquals(VaultState.LOCKED, runBlocking { app.vaultRepository.state.first() })
                assertTrue(editor.uiState.value.imagePaths.isEmpty())
                return
            }
            rule.onNodeWithText("PIN").performTextInput("000000")
            rule.onNodeWithText("Unlock", useUnmergedTree = true).performClick()
            rule.waitUntil(10_000) {
                rule.onAllNodes(androidx.compose.ui.test.hasText("Wrong PIN.")).fetchSemanticsNodes().isNotEmpty()
            }
            assertTrue(editor.pendingAttachment.hasResult.value)
            assertTrue(editor.uiState.value.imagePaths.isEmpty())
            rule.onNodeWithText("PIN").performTextInput(pin)
            rule.onNodeWithText("Unlock", useUnmergedTree = true).performClick()
            rule.waitUntil(15_000) { editor.uiState.value.imagePaths.size == 1 && !editor.uiState.value.isDirty }
            assertTrue(editor.uiState.value.content.contains("Saved before opening picker"))
            assertTrue(editor.uiState.value.content.contains(name))
            val saved = runBlocking { app.vaultNoteRepository.getVaultNoteById(id) }!!
            assertEquals(1, saved.imagePaths.size)
            assertArrayEquals("Selected document".toByteArray(),
                runBlocking { app.vaultNoteRepository.decryptVaultImageBytes(saved.imagePaths.single()) })
            assertFalse(java.io.File(app.filesDir, saved.imagePaths.single()).readText()
                .contains("Selected document"))
            assertFalse(editor.pendingAttachment.hasResult.value)
        } finally {
            resolver.delete(uri, null, null)
            app.vaultRepository.lock()
        }
    }

    private fun clickSystemNode(waitForDisappearance: Boolean = true, matches: (AccessibilityNodeInfo) -> Boolean) {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        fun find(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
            if (node == null) return null
            if (matches(node)) return node
            for (i in 0 until node.childCount) find(node.getChild(i))?.let { return it }
            return null
        }
        automation.waitForIdle(500, 5_000)
        val deadline = System.currentTimeMillis() + 15_000
        while (System.currentTimeMillis() < deadline) {
            val node = find(automation.rootInActiveWindow)
            if (node != null && node.isVisibleToUser && node.isEnabled) {
                val bounds = android.graphics.Rect()
                node.getBoundsInScreen(bounds)
                if (waitForDisappearance) {
                    var focusNode: AccessibilityNodeInfo? = node
                    while (focusNode != null && !focusNode.isFocusable) focusNode = focusNode.parent
                    if (focusNode?.performAction(AccessibilityNodeInfo.ACTION_FOCUS) == true) {
                        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_ENTER)
                        automation.waitForIdle(500, 5_000)
                        if (find(automation.rootInActiveWindow) == null) return
                    }
                }
                val time = android.os.SystemClock.uptimeMillis()
                val down = android.view.MotionEvent.obtain(time, time, android.view.MotionEvent.ACTION_DOWN,
                    bounds.exactCenterX(), bounds.exactCenterY(), 0)
                val up = android.view.MotionEvent.obtain(time, time + 50, android.view.MotionEvent.ACTION_UP,
                    bounds.exactCenterX(), bounds.exactCenterY(), 0)
                try {
                    down.source = android.view.InputDevice.SOURCE_TOUCHSCREEN
                    up.source = android.view.InputDevice.SOURCE_TOUCHSCREEN
                    check(automation.injectInputEvent(down, true))
                    Thread.sleep(60)
                    check(automation.injectInputEvent(up, true))
                } finally {
                    down.recycle()
                    up.recycle()
                }
                automation.waitForIdle(500, 5_000)
                if (!waitForDisappearance || find(automation.rootInActiveWindow) == null) return
            }
            Thread.sleep(100)
        }
        error("Selected test document not visible in Android DocumentsUI")
    }
}

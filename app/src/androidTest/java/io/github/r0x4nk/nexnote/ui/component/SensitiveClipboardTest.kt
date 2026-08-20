package io.github.r0x4nk.nexnote.ui.component

import android.content.ClipDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SensitiveClipboardTest {

    @Test
    fun noteClipDataContainsTextAndSensitivePreviewFlag() {
        val clipData = sensitiveNoteClipData("private note")

        assertEquals("private note", clipData.getItemAt(0).text.toString())
        assertTrue(
            clipData.description.extras?.getBoolean(ClipDescription.EXTRA_IS_SENSITIVE) == true
        )
    }
}

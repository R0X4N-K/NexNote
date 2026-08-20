package io.github.r0x4nk.nexnote.ui.screen.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.IntentCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.r0x4nk.nexnote.domain.model.Note
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExportManagerTest {

    @Test
    fun shareIntentsUseUniqueUrisClipDataAndReadGrant() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = ExportManager(
            context = context,
            imageFileProvider = { File("unused") }
        )
        val notes = listOf(Note(id = 1L, title = "Private note", content = "Body"))

        val first = manager.buildShareIntent(notes, ExportFormat.TXT)
        val second = manager.buildShareIntent(notes, ExportFormat.TXT)
        val firstUri = IntentCompat.getParcelableExtra(first, Intent.EXTRA_STREAM, Uri::class.java)
        val secondUri = IntentCompat.getParcelableExtra(second, Intent.EXTRA_STREAM, Uri::class.java)

        assertNotEquals(firstUri, secondUri)
        assertEquals(firstUri, first.clipData?.getItemAt(0)?.uri)
        assertTrue(first.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        val exported = requireNotNull(firstUri).let(context.contentResolver::openInputStream)
            ?.bufferedReader()
            ?.use { it.readText() }
            .orEmpty()
        assertTrue(exported.contains("Private note"))
        assertTrue(exported.contains("Body"))
    }
}

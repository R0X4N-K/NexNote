package io.github.r0x4nk.nexnote.ui.theme

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import io.github.r0x4nk.nexnote.domain.model.AccentColor
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.ui.component.NoteCard
import io.github.r0x4nk.nexnote.domain.model.NOTE_COLOR_PALETTE
import io.github.r0x4nk.nexnote.ui.screen.editor.NoteColorPicker
import java.io.File
import org.junit.Rule
import org.junit.Test

/** Real-device render matrix. Images are review artifacts, not self-approving golden files. */
class ThemeGalleryTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun renderEveryManualAccentInLightDarkAndOled() {
        val accent = mutableStateOf(AccentColor.VIOLET)
        val mode = mutableStateOf(0)
        compose.setContent {
            NexNoteTheme(darkTheme = mode.value > 0, trueDark = mode.value == 2, accentColor = accent.value) {
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Your notes", style = MaterialTheme.typography.headlineMedium)
                        Text("Ideas worth keeping", style = MaterialTheme.typography.bodyLarge)
                        NOTE_COLOR_PALETTE.chunked(2).forEachIndexed { row, colors ->
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                colors.forEachIndexed { column, color ->
                                    NoteCard(
                                        note = Note(id = (row * 2 + column + 1).toLong(),
                                            title = listOf("Fresh ideas", "Weekend plans")[column],
                                            content = "A little space to think. **Make it yours.**",
                                            isMarkdown = true, backgroundColor = color,
                                            isPinned = row == 0 && column == 0,
                                            lastModifiedDate = 1_783_080_000_000L),
                                        onClick = {}, onTrash = {}, modifier = Modifier.weight(1f),
                                        selected = row == 1 && column == 0,
                                        selectionMode = row == 1
                                    )
                                }
                            }
                        }
                        NoteColorPicker(NOTE_COLOR_PALETTE[4], {}, MaterialTheme.colorScheme.surface)
                    }
                }
            }
        }
        val directory = File(InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
            ?: File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "theme-gallery").path)
        directory.mkdirs()
        for (choice in AccentColor.entries) for (displayMode in 0..2) {
            compose.runOnIdle { accent.value = choice; mode.value = displayMode }
            compose.waitForIdle()
            File(directory, "${choice.name.lowercase()}-$displayMode.png").outputStream().use {
                compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }
}

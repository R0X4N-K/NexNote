package io.github.r0x4nk.nexnote.ui.component

import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.domain.model.TableLayoutMode
import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownPreviewTableTest {

    @Test
    fun `fit screen mode always uses viewport width`() {
        assertEquals(
            320.dp,
            markdownTableWidth(
                viewportWidth = 320.dp,
                columnCount = 4,
                layoutMode = TableLayoutMode.FIT_SCREEN
            )
        )
    }

    @Test
    fun `horizontal scroll mode expands wide tables`() {
        assertEquals(
            608.dp,
            markdownTableWidth(
                viewportWidth = 320.dp,
                columnCount = 4,
                layoutMode = TableLayoutMode.HORIZONTAL_SCROLL
            )
        )
    }

    @Test
    fun `horizontal scroll mode still fills viewport for narrow tables`() {
        assertEquals(
            320.dp,
            markdownTableWidth(
                viewportWidth = 320.dp,
                columnCount = 1,
                layoutMode = TableLayoutMode.HORIZONTAL_SCROLL
            )
        )
    }
}

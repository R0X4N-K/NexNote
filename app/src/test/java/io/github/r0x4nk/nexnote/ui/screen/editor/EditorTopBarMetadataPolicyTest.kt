package io.github.r0x4nk.nexnote.ui.screen.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EditorTopBarMetadataPolicyTest {

    @Test
    fun `metadata maps visible note state`() {
        val metadata = editorTopBarMetadata(
            EditorUiState(
                content = "note body",
                creationDate = 10L,
                lastModifiedDate = 20L
            )
        )

        assertEquals(
            EditorNoteMetadata(
                characterCount = 9,
                creationDate = 10L,
                lastModifiedDate = 20L
            ),
            metadata
        )
    }

    @Test
    fun `metadata is absent when editor content must stay hidden`() {
        assertNull(editorTopBarMetadata(EditorUiState(isLoading = true)))
        assertNull(editorTopBarMetadata(EditorUiState(isTemplateMode = true)))
        assertNull(editorTopBarMetadata(EditorUiState(isVaultLocked = true)))
    }
}

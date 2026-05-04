package io.github.r0x4nk.nexnote.ui.screen.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EditorContentCommitPolicyTest {

    @Test
    fun `resolve keeps remembered content when field exposes stale empty buffer`() {
        val remembered = TextFieldValue("Loaded content", TextRange(14))

        val resolved = EditorContentCommitPolicy.resolve(
            input(
                fieldValue = TextFieldValue(""),
                rememberedValue = remembered,
                modelContent = "Loaded content",
                modelContentVersion = 1,
                syncedContentVersion = 1
            )
        )

        assertEquals(remembered, resolved)
    }

    @Test
    fun `resolve skips empty field while loaded model has not synced yet`() {
        val resolved = EditorContentCommitPolicy.resolve(
            input(
                fieldValue = TextFieldValue(""),
                rememberedValue = TextFieldValue(""),
                modelContent = "Loaded content",
                modelContentVersion = 1,
                syncedContentVersion = 0
            )
        )

        assertNull(resolved)
    }

    @Test
    fun `resolve accepts intentional clear after remembered value is empty`() {
        val emptyValue = TextFieldValue("")

        val resolved = EditorContentCommitPolicy.resolve(
            input(
                fieldValue = emptyValue,
                rememberedValue = emptyValue,
                modelContent = "Loaded content",
                modelContentVersion = 1,
                syncedContentVersion = 1
            )
        )

        assertEquals(emptyValue, resolved)
    }

    @Test
    fun `resolve accepts regular non-empty field edits`() {
        val fieldValue = TextFieldValue("Edited content", TextRange(7))

        val resolved = EditorContentCommitPolicy.resolve(
            input(
                fieldValue = fieldValue,
                rememberedValue = TextFieldValue("Loaded content"),
                modelContent = "Loaded content",
                modelContentVersion = 1,
                syncedContentVersion = 1
            )
        )

        assertEquals(fieldValue, resolved)
    }

    private fun input(
        fieldValue: TextFieldValue,
        rememberedValue: TextFieldValue,
        modelContent: String,
        modelContentVersion: Int,
        syncedContentVersion: Int
    ): EditorContentCommitInput {
        return EditorContentCommitInput(
            fieldValue = fieldValue,
            rememberedValue = rememberedValue,
            modelContent = modelContent,
            modelContentVersion = modelContentVersion,
            syncedContentVersion = syncedContentVersion
        )
    }
}

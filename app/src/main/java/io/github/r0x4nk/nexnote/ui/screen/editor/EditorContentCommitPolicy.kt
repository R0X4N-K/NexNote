package io.github.r0x4nk.nexnote.ui.screen.editor

import androidx.compose.ui.text.input.TextFieldValue

internal data class EditorContentCommitInput(
    val fieldValue: TextFieldValue,
    val rememberedValue: TextFieldValue,
    val modelContent: String,
    val modelContentVersion: Int,
    val syncedContentVersion: Int
)

internal object EditorContentCommitPolicy {

    /**
     * Chooses the safest content value to flush from Compose into the ViewModel.
     *
     * During preview/edit transitions the state-based text field can briefly
     * expose its empty construction buffer. A non-empty remembered value is the
     * last content we explicitly synced or accepted from user input, so prefer
     * it over a destructive empty field snapshot. A real full clear still saves:
     * once user input reaches the remembered value, both buffers are empty.
     */
    fun resolve(input: EditorContentCommitInput): TextFieldValue? {
        if (input.modelContent.isEmpty() || input.fieldValue.text.isNotEmpty()) {
            return input.fieldValue
        }

        if (input.rememberedValue.text.isNotEmpty()) {
            return input.rememberedValue
        }

        val isStillWaitingForModelSync =
            input.syncedContentVersion != input.modelContentVersion
        return if (isStillWaitingForModelSync) null else input.fieldValue
    }
}

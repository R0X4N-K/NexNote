package io.github.r0x4nk.nexnote.ui.screen.editor

import androidx.compose.ui.text.input.TextFieldValue

internal data class EditorContentCommitInput(
    val fieldValue: TextFieldValue,
    val rememberedValue: TextFieldValue,
    val modelContent: String,
    val modelContentVersion: Int,
    val syncedContentVersion: Int,
    val hasPendingFieldEdit: Boolean
)

internal object EditorContentCommitPolicy {

    /**
     * Chooses the safest content value to flush from Compose into the ViewModel.
     *
     * During preview/edit transitions the state-based text field can briefly expose
     * its empty construction buffer. Without a pending user edit, a non-empty
     * remembered value is safer than that destructive empty snapshot.
     *
     * A user clear is different: while the edit pipeline has a pending field edit,
     * the field buffer is the latest user-authored value and must be allowed to
     * replace the model with an empty string.
     */
    fun resolve(input: EditorContentCommitInput): TextFieldValue? {
        if (input.modelContent.isEmpty() || input.fieldValue.text.isNotEmpty()) {
            return input.fieldValue
        }

        val isStillWaitingForModelSync =
            input.syncedContentVersion != input.modelContentVersion
        if (isStillWaitingForModelSync) {
            return null
        }

        if (input.hasPendingFieldEdit) {
            return input.fieldValue
        }

        if (input.rememberedValue.text.isNotEmpty()) {
            return input.rememberedValue
        }

        return input.fieldValue
    }
}

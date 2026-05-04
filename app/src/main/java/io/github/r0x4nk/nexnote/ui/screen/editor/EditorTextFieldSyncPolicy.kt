package io.github.r0x4nk.nexnote.ui.screen.editor

import androidx.compose.ui.text.input.TextFieldValue

internal object EditorTextFieldSyncPolicy {

    /**
     * Keeps recomposition from replaying stale text into the state-based editor.
     *
     * Loaded content and editor actions already update both the remembered
     * TextFieldValue and TextFieldState through EditorScreenState.setContentFieldValue.
     * This late recomposition sync is only allowed to reconcile selection for
     * matching text; replacing text here can erase freshly loaded content with an
     * older empty construction value.
     */
    fun canApplyRecomposedValue(
        currentValue: TextFieldValue,
        recomposedValue: TextFieldValue
    ): Boolean {
        return currentValue.text == recomposedValue.text
    }
}

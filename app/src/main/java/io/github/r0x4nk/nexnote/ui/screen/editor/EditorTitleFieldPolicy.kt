package io.github.r0x4nk.nexnote.ui.screen.editor

internal object EditorTitleFieldPolicy {
    private const val COLLAPSED_MAX_LINES = 1
    private const val FOCUSED_MAX_LINES = 3
    private val LineBreaks = Regex("[\\r\\n]+")

    fun isExpanded(hasFocus: Boolean, readOnly: Boolean): Boolean {
        return hasFocus && !readOnly
    }

    fun maxLines(isExpanded: Boolean): Int {
        return if (isExpanded) FOCUSED_MAX_LINES else COLLAPSED_MAX_LINES
    }

    fun normalizeInput(value: String): String {
        return value.replace(LineBreaks, " ")
    }
}

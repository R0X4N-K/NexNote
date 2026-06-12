package io.github.r0x4nk.nexnote.ui.common

/** Direction used to sort the notes list. Pinned notes always appear first. */
enum class SortOrder { MODIFIED_DESC, MODIFIED_ASC }

/** Layout mode for the notes list. */
enum class NoteListViewMode {
    LIST,
    GRID,
    TAGS;

    companion object {
        val noteModes: List<NoteListViewMode> = listOf(LIST, GRID, TAGS)
        val listGridModes: List<NoteListViewMode> = listOf(LIST, GRID)
    }
}

internal fun NoteListViewMode.nextIn(
    modes: List<NoteListViewMode> = NoteListViewMode.noteModes
): NoteListViewMode {
    if (modes.isEmpty()) return this

    val currentIndex = modes.indexOf(this).takeIf { it >= 0 } ?: 0
    return modes[(currentIndex + 1) % modes.size]
}

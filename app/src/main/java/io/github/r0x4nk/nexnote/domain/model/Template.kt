package io.github.r0x4nk.nexnote.domain.model

/**
 * Domain model for a template.
 * [isPredefined] = true → read-only, cannot be deleted by the user.
 * [category] is a display label seeded from localized resources; the reserved
 * value "custom" marks user-created templates.
 * [iconName] maps to a Material icon name (used in the UI).
 *
 * The {{date}} placeholder in the content is replaced with the current date
 * when a note is created from the template (resolved in EditorViewModel).
 */
data class Template(
    val id: Long = 0,
    val name: String = "",
    val content: String = "",
    val isMarkdown: Boolean = true,
    val category: String = "custom",
    val isPredefined: Boolean = false,
    val iconName: String = "note"
)

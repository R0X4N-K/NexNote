package com.example.nexnote.domain.model

/**
 * Domain model for a template.
 * [isPredefined] = true → read-only, cannot be deleted by the user.
 * [category] = "custom" | "productivity" | "work" | "personal" | "general"
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

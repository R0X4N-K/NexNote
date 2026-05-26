package io.github.r0x4nk.nexnote.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for templates.
 *
 * [isPredefined]: built-in template seeded by the app.
 * [category]: "custom" | "productivity" | "work" | "personal" | "general"
 * [iconName]: Material icon name displayed in the list.
 */
@Entity(tableName = "templates")
data class TemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "",
    val content: String = "",
    val isMarkdown: Boolean = true,
    val category: String = "custom",
    val isPredefined: Boolean = false,
    val iconName: String = "note"
)

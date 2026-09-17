package io.github.r0x4nk.nexnote.data.repository

import io.github.r0x4nk.nexnote.R
import io.github.r0x4nk.nexnote.di.StringProvider
import io.github.r0x4nk.nexnote.domain.model.Template

/**
 * Built-in templates seeded on first app launch, resolved from localized
 * resources. The {{date}} placeholder is resolved in EditorViewModel when a
 * note is created from a template.
 */
internal object PredefinedTemplates {

    fun all(strings: StringProvider): List<Template> = listOf(

        Template(
            name = strings.get(R.string.predefined_template_shopping_list_name),
            content = strings.get(R.string.predefined_template_shopping_list_content),
            isMarkdown = true,
            category = strings.get(R.string.predefined_category_productivity),
            isPredefined = true,
            iconName = "shopping_cart"
        ),

        Template(
            name = strings.get(R.string.predefined_template_new_project_name),
            content = strings.get(R.string.predefined_template_new_project_content),
            isMarkdown = true,
            category = strings.get(R.string.predefined_category_work),
            isPredefined = true,
            iconName = "work"
        ),

        Template(
            name = strings.get(R.string.predefined_template_checklist_name),
            content = strings.get(R.string.predefined_template_checklist_content),
            isMarkdown = true,
            category = strings.get(R.string.predefined_category_productivity),
            isPredefined = true,
            iconName = "check_box"
        ),

        Template(
            name = strings.get(R.string.predefined_template_journal_name),
            content = strings.get(R.string.predefined_template_journal_content),
            isMarkdown = true,
            category = strings.get(R.string.predefined_category_personal),
            isPredefined = true,
            iconName = "book"
        ),

        Template(
            name = strings.get(R.string.predefined_template_meeting_notes_name),
            content = strings.get(R.string.predefined_template_meeting_notes_content),
            isMarkdown = true,
            category = strings.get(R.string.predefined_category_work),
            isPredefined = true,
            iconName = "groups"
        ),

        Template(
            name = strings.get(R.string.predefined_template_blank_note_name),
            content = "",
            isMarkdown = false,
            category = strings.get(R.string.predefined_category_general),
            isPredefined = true,
            iconName = "note"
        )
    )
}

package io.github.r0x4nk.nexnote.domain.model

/**
 * Built-in templates seeded on first app launch.
 * Not user-editable. The {{date}} placeholder is resolved in EditorViewModel
 * when a note is created from a template.
 */
object PredefinedTemplates {

    val all: List<Template> = listOf(

        Template(
            name = "Shopping list",
            content = """# Shopping list

## Produce
- [ ]
- [ ]

## Dairy
- [ ]
- [ ]

## Meat & fish
- [ ]
- [ ]

## Other
- [ ]
- [ ]

---
*Estimated total: €*""",
            isMarkdown = true,
            category = "productivity",
            isPredefined = true,
            iconName = "shopping_cart"
        ),

        Template(
            name = "New project",
            content = """# Project name

## Objective
Describe the main objective of the project.

## Requirements
-
-
-

## Phases
1. [ ] Phase 1 —
2. [ ] Phase 2 —
3. [ ] Phase 3 —

## Notes & resources
-

## Deadline
Date:

---
*Created on: {{date}}*""",
            isMarkdown = true,
            category = "work",
            isPredefined = true,
            iconName = "work"
        ),

        Template(
            name = "Checklist",
            content = """# Checklist

## To do
- [ ]
- [ ]
- [ ]

## In progress
- [ ]
- [ ]

## Done
- [x]

---
*Updated on: {{date}}*""",
            isMarkdown = true,
            category = "productivity",
            isPredefined = true,
            iconName = "check_box"
        ),

        Template(
            name = "Journal",
            content = """# {{date}}

## How I feel today


## What happened today


## What I learned


## Goals for tomorrow
-
-
- """,
            isMarkdown = true,
            category = "personal",
            isPredefined = true,
            iconName = "book"
        ),

        Template(
            name = "Meeting notes",
            content = """# Meeting — {{date}}

## Participants
-
-

## Agenda
1.
2.
3.

## Notes & discussion


## Decisions made
-

## Action items
- [ ] Action — owner:
- [ ] Action — owner:

## Next meeting
Date: """,
            isMarkdown = true,
            category = "work",
            isPredefined = true,
            iconName = "groups"
        ),

        Template(
            name = "Blank note",
            content = "",
            isMarkdown = false,
            category = "general",
            isPredefined = true,
            iconName = "note"
        )
    )
}

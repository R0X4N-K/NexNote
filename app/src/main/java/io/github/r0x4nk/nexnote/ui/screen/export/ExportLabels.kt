package io.github.r0x4nk.nexnote.ui.screen.export

import androidx.compose.runtime.Composable
import io.github.r0x4nk.nexnote.ui.component.NexSectionLabel

@Composable
internal fun SectionLabel(text: String) {
    NexSectionLabel(text = text)
}

internal val ExportScope.label: String
    get() = when (this) {
        ExportScope.SingleNote -> "This note"
        ExportScope.DateRange -> "Date range"
        ExportScope.AllNotes -> "All"
    }

internal val ExportFormat.label: String
    get() = when (this) {
        ExportFormat.TXT -> "TXT"
        ExportFormat.MD -> "MD"
        ExportFormat.PDF -> "PDF"
        ExportFormat.PRINT -> "Print"
    }

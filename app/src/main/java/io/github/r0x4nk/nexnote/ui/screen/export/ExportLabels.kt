package io.github.r0x4nk.nexnote.ui.screen.export

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
internal fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary
    )
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

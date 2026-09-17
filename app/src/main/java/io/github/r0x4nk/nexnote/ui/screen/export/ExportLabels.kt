package io.github.r0x4nk.nexnote.ui.screen.export

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.r0x4nk.nexnote.R
import io.github.r0x4nk.nexnote.ui.component.NexSectionLabel

@Composable
internal fun SectionLabel(text: String) {
    NexSectionLabel(text = text)
}

internal val ExportScope.label: String
    @Composable get() = when (this) {
        ExportScope.SingleNote -> stringResource(R.string.export_scope_single)
        ExportScope.DateRange -> stringResource(R.string.export_scope_date_range)
        ExportScope.AllNotes -> stringResource(R.string.export_scope_all)
    }

internal val ExportFormat.label: String
    @Composable get() = when (this) {
        ExportFormat.TXT -> "TXT"
        ExportFormat.MD -> "MD"
        ExportFormat.PDF -> "PDF"
        ExportFormat.PRINT -> stringResource(R.string.export_format_print)
    }

package com.example.nexnote.ui.screen.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.nexnote.domain.model.FontScale
import com.example.nexnote.domain.model.NoteCardStyle
import com.example.nexnote.domain.model.ThemeMode

@Composable
internal fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
internal fun ThemeModePicker(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit
) {
    val themeModes = ThemeMode.entries
    val themeLabels = listOf("Light", "Dark", "System", "Black")

    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        themeModes.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = selected == mode,
                onClick = { onSelect(mode) },
                shape = SegmentedButtonDefaults.itemShape(index, themeModes.size),
                label = { Text(themeLabels[index]) }
            )
        }
    }
}

@Composable
internal fun FontScalePicker(
    selected: FontScale,
    onSelect: (FontScale) -> Unit
) {
    val fontScales = FontScale.entries
    val fontLabels = listOf("Small", "Normal", "Large")

    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        fontScales.forEachIndexed { index, scale ->
            SegmentedButton(
                selected = selected == scale,
                onClick = { onSelect(scale) },
                shape = SegmentedButtonDefaults.itemShape(index, fontScales.size),
                label = { Text(fontLabels[index]) }
            )
        }
    }
}

@Composable
internal fun NoteCardStylePicker(
    selected: NoteCardStyle,
    onSelect: (NoteCardStyle) -> Unit
) {
    val styles = NoteCardStyle.entries
    val labels = listOf("Compact", "Preview", "With date")

    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        styles.forEachIndexed { index, style ->
            SegmentedButton(
                selected = selected == style,
                onClick = { onSelect(style) },
                shape = SegmentedButtonDefaults.itemShape(index, styles.size),
                label = { Text(labels[index]) }
            )
        }
    }
}

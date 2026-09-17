package io.github.r0x4nk.nexnote.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.R
import io.github.r0x4nk.nexnote.domain.model.AppFont
import io.github.r0x4nk.nexnote.domain.model.FontScale
import io.github.r0x4nk.nexnote.domain.model.NoteCardStyle
import io.github.r0x4nk.nexnote.domain.model.TableLayoutMode
import io.github.r0x4nk.nexnote.domain.model.ThemeMode
import io.github.r0x4nk.nexnote.ui.component.NexSectionLabel
import io.github.r0x4nk.nexnote.ui.theme.fontFamily

@Composable
internal fun SettingsSectionHeader(title: String) {
    NexSectionLabel(text = title)
}

@Composable
internal fun ThemeModePicker(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit
) {
    val themeModes = ThemeMode.entries
    val themeLabels = listOf(
        stringResource(R.string.settings_theme_light),
        stringResource(R.string.settings_theme_dark),
        stringResource(R.string.settings_theme_system),
        stringResource(R.string.settings_theme_black)
    )

    SettingsOptions(
        labels = themeLabels,
        selectedIndex = themeModes.indexOf(selected),
        onSelect = { onSelect(themeModes[it]) }
    )
}

@Composable
internal fun FontScalePicker(
    selected: FontScale,
    onSelect: (FontScale) -> Unit
) {
    val fontScales = FontScale.entries
    val fontLabels = listOf(
        stringResource(R.string.settings_font_small),
        stringResource(R.string.settings_font_normal),
        stringResource(R.string.settings_font_large)
    )

    SettingsOptions(
        labels = fontLabels,
        selectedIndex = fontScales.indexOf(selected),
        onSelect = { onSelect(fontScales[it]) }
    )
}

/** Font-family picker; each option is previewed in its own typeface. */
@Composable
internal fun AppFontPicker(
    selected: AppFont,
    onSelect: (AppFont) -> Unit
) {
    val fonts = AppFont.entries
    SettingsOptionChips(
        labels = fonts.map { stringResource(it.labelRes()) },
        labelFontFamilies = fonts.map { it.fontFamily() },
        selectedIndex = fonts.indexOf(selected),
        onSelect = { onSelect(fonts[it]) }
    )
}

private fun AppFont.labelRes(): Int = when (this) {
    AppFont.SYSTEM -> R.string.settings_font_system
    AppFont.INTER -> R.string.settings_font_inter
    AppFont.LORA -> R.string.settings_font_lora
    AppFont.FIRA_CODE -> R.string.settings_font_fira_code
    AppFont.JETBRAINS_MONO -> R.string.settings_font_jetbrains_mono
}

@Composable
internal fun NoteCardStylePicker(
    selected: NoteCardStyle,
    onSelect: (NoteCardStyle) -> Unit
) {
    val styles = NoteCardStyle.entries
    val labels = listOf(
        stringResource(R.string.settings_card_compact),
        stringResource(R.string.settings_card_preview),
        stringResource(R.string.settings_card_information)
    )

    SettingsOptions(
        labels = labels,
        selectedIndex = styles.indexOf(selected),
        onSelect = { onSelect(styles[it]) }
    )
}

@Composable
internal fun TableLayoutModePicker(
    selected: TableLayoutMode,
    onSelect: (TableLayoutMode) -> Unit
) {
    val modes = TableLayoutMode.entries
    val labels = listOf(
        stringResource(R.string.settings_table_wrap),
        stringResource(R.string.settings_table_scroll)
    )

    SettingsOptions(
        labels = labels,
        selectedIndex = modes.indexOf(selected),
        onSelect = { onSelect(modes[it]) }
    )
}

/** Wrap choices at large font scales instead of clipping text inside fixed-width segments. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsOptions(labels: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    SettingsOptionChips(
        labels = labels,
        labelFontFamilies = null,
        selectedIndex = selectedIndex,
        onSelect = onSelect
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsOptionChips(
    labels: List<String>,
    labelFontFamilies: List<FontFamily>?,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        labels.forEachIndexed { index, label ->
            FilterChip(
                selected = selectedIndex == index,
                onClick = { onSelect(index) },
                modifier = Modifier.heightIn(min = 48.dp),
                label = {
                    Text(
                        text = label,
                        fontFamily = labelFontFamilies?.getOrNull(index) ?: FontFamily.Default
                    )
                },
                leadingIcon = if (selectedIndex == index) {
                    { Icon(Icons.Default.Check, contentDescription = null) }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            )
        }
    }
}

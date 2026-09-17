package io.github.r0x4nk.nexnote.ui.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.github.r0x4nk.nexnote.ui.theme.seedColor
import java.util.concurrent.ConcurrentHashMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.R
import io.github.r0x4nk.nexnote.domain.model.AccentColor
import io.github.r0x4nk.nexnote.ui.theme.LocalNexNoteDarkTheme
import io.github.r0x4nk.nexnote.ui.theme.buildColorScheme

private data class AccentSwatch(
    val accent: AccentColor,
    val color: Color,
    val selectedIconColor: Color = Color.Unspecified
)

private val accentSwatches = ConcurrentHashMap<Boolean, List<AccentSwatch>>()

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AccentColorPicker(
    selected: AccentColor,
    onSelect: (AccentColor) -> Unit
) {
    val dark = LocalNexNoteDarkTheme.current
    val swatches by produceState(
        initialValue = accentSwatches[dark] ?: AccentColor.entries.map { AccentSwatch(it, it.seedColor()) },
        key1 = dark
    ) {
        value = withContext(Dispatchers.Default) {
            accentSwatches.getOrPut(dark) {
                AccentColor.entries.map { accent ->
                    val scheme = buildColorScheme(dark, false, accent)
                    AccentSwatch(accent, scheme.primary, scheme.onPrimary)
                }
            }
        }
    }

    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        swatches.forEach { swatch ->
            AccentColorSwatch(
                swatch = swatch,
                isSelected = swatch.accent == selected,
                onSelect = onSelect
            )
        }
    }
}

@Composable
private fun AccentColorSwatch(
    swatch: AccentSwatch,
    isSelected: Boolean,
    onSelect: (AccentColor) -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val swatchDescription = stringResource(
        R.string.settings_accent_swatch_content_description,
        swatch.accent.displayName()
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .semantics {
                contentDescription = swatchDescription
            }
            .accentColorSwatchStyle(
                isSelected = isSelected,
                borderColor = borderColor,
                swatch = swatch.color,
                onClick = { onSelect(swatch.accent) }
            )
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = swatch.selectedIconColor.takeIf { it.isSpecified }
                    ?: MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun Modifier.accentColorSwatchStyle(
    isSelected: Boolean,
    borderColor: Color,
    swatch: Color,
    onClick: () -> Unit
): Modifier {
    return size(48.dp)
        .clip(CircleShape)
        .border(
            width = if (isSelected) 2.dp else 0.dp,
            color = borderColor,
            shape = CircleShape
        )
        .selectable(
            selected = isSelected,
            role = Role.RadioButton,
            onClick = onClick
        )
        .padding(4.dp)
        .clip(CircleShape)
        .background(swatch)
}

@Composable
private fun AccentColor.displayName(): String = when (this) {
    AccentColor.VIOLET -> stringResource(R.string.accent_violet)
    AccentColor.BLUE -> stringResource(R.string.accent_blue)
    AccentColor.GREEN -> stringResource(R.string.accent_green)
    AccentColor.ORANGE -> stringResource(R.string.accent_orange)
    AccentColor.RED -> stringResource(R.string.accent_red)
    AccentColor.TEAL -> stringResource(R.string.accent_teal)
    AccentColor.SAGE -> stringResource(R.string.accent_sage)
    AccentColor.ROSE -> stringResource(R.string.accent_rose)
    AccentColor.AMBER -> stringResource(R.string.accent_amber)
}

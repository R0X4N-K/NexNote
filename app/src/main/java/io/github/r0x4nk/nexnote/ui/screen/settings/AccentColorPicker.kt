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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.domain.model.AccentColor
import io.github.r0x4nk.nexnote.ui.theme.NexAmberPrimary
import io.github.r0x4nk.nexnote.ui.theme.NexBluePrimary
import io.github.r0x4nk.nexnote.ui.theme.NexGreenPrimary
import io.github.r0x4nk.nexnote.ui.theme.NexOrangePrimary
import io.github.r0x4nk.nexnote.ui.theme.NexPrimary
import io.github.r0x4nk.nexnote.ui.theme.NexRedPrimary
import io.github.r0x4nk.nexnote.ui.theme.NexRosePrimary
import io.github.r0x4nk.nexnote.ui.theme.NexSageOnPrimaryContainer
import io.github.r0x4nk.nexnote.ui.theme.NexSageSwatch
import io.github.r0x4nk.nexnote.ui.theme.NexTealPrimary

private data class AccentSwatch(
    val accent: AccentColor,
    val color: Color,
    val selectedIconColor: Color = Color.White
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AccentColorPicker(
    selected: AccentColor,
    onSelect: (AccentColor) -> Unit
) {
    val swatches = listOf(
        AccentSwatch(AccentColor.VIOLET, NexPrimary),
        AccentSwatch(AccentColor.BLUE, NexBluePrimary),
        AccentSwatch(AccentColor.GREEN, NexGreenPrimary),
        AccentSwatch(AccentColor.ORANGE, NexOrangePrimary),
        AccentSwatch(AccentColor.RED, NexRedPrimary),
        AccentSwatch(AccentColor.TEAL, NexTealPrimary),
        AccentSwatch(
            accent = AccentColor.SAGE,
            color = NexSageSwatch,
            selectedIconColor = NexSageOnPrimaryContainer
        ),
        AccentSwatch(AccentColor.ROSE, NexRosePrimary),
        AccentSwatch(AccentColor.AMBER, NexAmberPrimary)
    )

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

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .semantics {
                contentDescription = "${swatch.accent.displayName()} accent"
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
                tint = swatch.selectedIconColor,
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
    return size(46.dp)
        .clip(CircleShape)
        .border(
            width = if (isSelected) 2.dp else 0.dp,
            color = borderColor,
            shape = CircleShape
        )
        .padding(if (isSelected) 4.dp else 0.dp)
        .clip(CircleShape)
        .background(swatch)
        .selectable(
            selected = isSelected,
            role = Role.RadioButton,
            onClick = onClick
        )
}

private fun AccentColor.displayName(): String =
    name.lowercase().replaceFirstChar { it.uppercaseChar() }

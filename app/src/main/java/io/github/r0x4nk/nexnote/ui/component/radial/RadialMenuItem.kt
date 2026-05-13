package io.github.r0x4nk.nexnote.ui.component.radial

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Declarative action item registered with the shared radial menu.
 *
 * Screens provide the icon, label, accessibility description, enabled state,
 * and action; the radial menu owns placement and gesture hit-testing. Keeping
 * actions as values lets [RadialMenuEffect] replace a screen's whole menu in
 * one immutable snapshot.
 */
data class RadialMenuItem(
    val icon: ImageVector,
    val label: String,
    val contentDescription: String = label,
    val enabled: Boolean = true,
    val action: () -> Unit
)

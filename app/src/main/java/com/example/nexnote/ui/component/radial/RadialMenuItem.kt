package com.example.nexnote.ui.component.radial

import androidx.compose.ui.graphics.vector.ImageVector

data class RadialMenuItem(
    val icon: ImageVector,
    val label: String,
    val contentDescription: String = label,
    val enabled: Boolean = true,
    val action: () -> Unit
)

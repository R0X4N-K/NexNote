package com.example.nexnote.ui.screen.editor

import com.example.nexnote.ui.component.MarkdownPreviewBlockLayout

internal fun List<MarkdownPreviewBlockLayout>.previewYForSourceOffset(sourceOffset: Int): Float? {
    if (isEmpty()) return null
    val sortedLayouts = sortedBy { it.top }
    val layout = sortedLayouts.firstOrNull { it.containsSourceOffset(sourceOffset) }
        ?: sortedLayouts.lastOrNull { sourceOffset >= it.sourceStart }
        ?: sortedLayouts.firstOrNull()
        ?: return null

    val progress = ((sourceOffset - layout.sourceStart).toFloat() / layout.sourceLength)
        .coerceIn(0f, 1f)
    return layout.top + layout.height * progress
}

internal fun List<MarkdownPreviewBlockLayout>.sourceOffsetForPreviewY(previewY: Float): Int? {
    if (isEmpty()) return null
    val layout = sortedBy { it.top }.firstOrNull { previewY >= it.top && previewY <= it.bottom }
        ?: minByOrNull { block ->
            when {
                previewY < block.top -> block.top - previewY
                previewY > block.bottom -> previewY - block.bottom
                else -> 0f
            }
        }
        ?: return null

    val progress = ((previewY - layout.top) / layout.height).coerceIn(0f, 1f)
    return (layout.sourceStart + layout.sourceLength * progress)
        .toInt()
        .coerceIn(layout.sourceStart, layout.sourceEnd)
}

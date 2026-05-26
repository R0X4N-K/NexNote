package io.github.r0x4nk.nexnote.ui.common

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.saveable.Saver

/**
 * Transient selection state for list-like UI surfaces.
 *
 * Selection is a view concern: the selected ids should survive lightweight
 * recomposition/restoration, but they do not belong in repositories or domain
 * models. The state intentionally stays id-based so screens can derive the
 * selected items from their current visible collections.
 */
@Immutable
data class SelectionUiState(
    val isActive: Boolean = false,
    val selectedIds: Set<Long> = emptySet()
) {
    val selectedCount: Int
        get() = selectedIds.size

    fun isSelected(id: Long): Boolean = id in selectedIds

    fun enter(): SelectionUiState = copy(isActive = true)

    fun select(id: Long): SelectionUiState =
        if (id <= 0L) enter() else copy(isActive = true, selectedIds = selectedIds + id)

    fun toggle(id: Long): SelectionUiState {
        if (id <= 0L) return enter()
        val nextIds = if (id in selectedIds) selectedIds - id else selectedIds + id
        return copy(isActive = true, selectedIds = nextIds)
    }

    fun selectAll(ids: Iterable<Long>): SelectionUiState =
        copy(isActive = true, selectedIds = ids.filter { it > 0L }.toSet())

    fun deselectAll(): SelectionUiState = copy(isActive = true, selectedIds = emptySet())

    fun retainSelectableIds(ids: Iterable<Long>): SelectionUiState {
        if (!isActive || selectedIds.isEmpty()) return this
        val availableIds = ids.toSet()
        return copy(selectedIds = selectedIds.intersect(availableIds))
    }

    fun exit(): SelectionUiState = SelectionUiState()

    companion object {
        val Saver: Saver<SelectionUiState, Any> = Saver(
            save = { state ->
                arrayListOf(
                    state.isActive,
                    ArrayList(state.selectedIds)
                )
            },
            restore = { restored ->
                val values = restored as? List<*> ?: return@Saver SelectionUiState()
                val isActive = values.getOrNull(0) as? Boolean ?: false
                val ids = (values.getOrNull(1) as? List<*>)
                    ?.mapNotNull { it as? Long }
                    ?.toSet()
                    ?: emptySet()
                SelectionUiState(isActive = isActive, selectedIds = ids)
            }
        )
    }
}

internal fun <T> SelectionUiState.selectedItems(
    items: Iterable<T>,
    idSelector: (T) -> Long
): List<T> =
    if (!isActive || selectedIds.isEmpty()) {
        emptyList()
    } else {
        items.filter { idSelector(it) in selectedIds }
    }

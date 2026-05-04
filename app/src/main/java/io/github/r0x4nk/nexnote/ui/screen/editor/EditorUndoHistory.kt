package io.github.r0x4nk.nexnote.ui.screen.editor

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Immutable
internal data class EditorUndoRedoState(
    val canUndo: Boolean = false,
    val canRedo: Boolean = false
)

internal data class EditorContentSnapshot(
    val text: String,
    val selectionOffset: Int? = null
)

/**
 * Keeps a bounded, in-memory content history for one active editor session.
 *
 * Text typing is grouped by a debounce window so fast input creates one logical
 * undo step instead of one snapshot per character. Programmatic changes can be
 * committed immediately when they already represent one complete user action.
 */
internal class EditorUndoHistory(
    private val scope: CoroutineScope,
    private val debounceMs: Long,
    maxStackSize: Int,
    initialSnapshot: EditorContentSnapshot = EditorContentSnapshot("")
) {
    private val maxSnapshots = maxStackSize.coerceAtLeast(1)
    private val undoStack = ArrayDeque<EditorContentSnapshot>()
    private val redoStack = ArrayDeque<EditorContentSnapshot>()
    private val _state = MutableStateFlow(EditorUndoRedoState())

    val state: StateFlow<EditorUndoRedoState> = _state.asStateFlow()

    private var currentSnapshot = initialSnapshot.normalized()
    private var pendingUndoSnapshot: EditorContentSnapshot? = null
    private var debounceJob: Job? = null

    fun reset(snapshot: EditorContentSnapshot) {
        cancelDebounce()
        pendingUndoSnapshot = null
        undoStack.clear()
        redoStack.clear()
        currentSnapshot = snapshot.normalized()
        publishState()
    }

    fun clear() {
        reset(EditorContentSnapshot(""))
    }

    fun recordUserChange(snapshot: EditorContentSnapshot) {
        val normalizedSnapshot = snapshot.normalized()
        if (normalizedSnapshot.text == currentSnapshot.text) {
            currentSnapshot = normalizedSnapshot
            return
        }

        if (pendingUndoSnapshot?.text == normalizedSnapshot.text) {
            currentSnapshot = normalizedSnapshot
            pendingUndoSnapshot = null
            cancelDebounce()
            redoStack.clear()
            publishState()
            return
        }

        if (pendingUndoSnapshot == null) {
            pendingUndoSnapshot = currentSnapshot
        }
        currentSnapshot = normalizedSnapshot
        redoStack.clear()
        schedulePendingCommit()
        publishState()
    }

    fun recordImmediateChange(
        previous: EditorContentSnapshot,
        next: EditorContentSnapshot
    ) {
        val normalizedPrevious = previous.normalized()
        val normalizedNext = next.normalized()
        if (normalizedPrevious.text == normalizedNext.text) {
            currentSnapshot = normalizedNext
            publishState()
            return
        }

        commitPendingSnapshot()
        pushUndoSnapshot(normalizedPrevious)
        currentSnapshot = normalizedNext
        redoStack.clear()
        publishState()
    }

    fun updateCurrentSelection(selectionOffset: Int?) {
        currentSnapshot = currentSnapshot.copy(selectionOffset = selectionOffset)
    }

    fun undo(): EditorContentSnapshot? {
        commitPendingSnapshot()
        val previous = undoStack.removeLastOrNull() ?: return null
        pushRedoSnapshot(currentSnapshot)
        currentSnapshot = previous
        publishState()
        return previous
    }

    fun redo(): EditorContentSnapshot? {
        commitPendingSnapshot()
        val next = redoStack.removeLastOrNull() ?: return null
        pushUndoSnapshot(currentSnapshot)
        currentSnapshot = next
        publishState()
        return next
    }

    private fun schedulePendingCommit() {
        cancelDebounce()
        debounceJob = scope.launch {
            delay(debounceMs)
            commitPendingSnapshot()
        }
    }

    private fun commitPendingSnapshot() {
        val pending = pendingUndoSnapshot ?: return
        cancelDebounce()
        pendingUndoSnapshot = null
        if (pending.text != currentSnapshot.text) {
            pushUndoSnapshot(pending)
        }
        publishState()
    }

    private fun pushUndoSnapshot(snapshot: EditorContentSnapshot) {
        pushBoundedSnapshot(undoStack, snapshot)
    }

    private fun pushRedoSnapshot(snapshot: EditorContentSnapshot) {
        pushBoundedSnapshot(redoStack, snapshot)
    }

    private fun pushBoundedSnapshot(
        stack: ArrayDeque<EditorContentSnapshot>,
        snapshot: EditorContentSnapshot
    ) {
        val last = stack.lastOrNull()
        if (last?.text == snapshot.text) {
            stack.removeLast()
        }
        stack.addLast(snapshot)
        while (stack.size > maxSnapshots) {
            stack.removeFirst()
        }
    }

    private fun cancelDebounce() {
        debounceJob?.cancel()
        debounceJob = null
    }

    private fun publishState() {
        _state.value = EditorUndoRedoState(
            canUndo = undoStack.isNotEmpty() || pendingUndoSnapshot != null,
            canRedo = redoStack.isNotEmpty()
        )
    }

}

internal fun EditorUiState.toContentSnapshot(
    selectionOffset: Int? = contentSelectionOffset
): EditorContentSnapshot {
    return EditorContentSnapshot(
        text = content,
        selectionOffset = selectionOffset?.coerceIn(0, content.length)
    )
}

private fun EditorContentSnapshot.normalized(): EditorContentSnapshot {
    return copy(selectionOffset = selectionOffset?.coerceIn(0, text.length))
}

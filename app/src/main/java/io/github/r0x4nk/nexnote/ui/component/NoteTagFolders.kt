package io.github.r0x4nk.nexnote.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.R
import io.github.r0x4nk.nexnote.domain.model.ScoredNote
import io.github.r0x4nk.nexnote.ui.common.NoteCollectionLayoutDefaults
import io.github.r0x4nk.nexnote.ui.common.animateNoteItem
import io.github.r0x4nk.nexnote.util.TagParser

@Immutable
internal data class NoteTagFolder(
    val id: String,
    val title: String,
    val isUntagged: Boolean,
    val items: List<ScoredNote>
) {
    val noteCount: Int get() = items.size
}

/**
 * Holds the collapsed state for the tag-folder view and the set of folders the
 * current collection exposes.
 *
 * The state is hoisted so a top-bar action can collapse or expand every folder
 * at once while the collection still owns the folder list. [syncFolderIds]
 * keeps the two in step and prunes folders that disappeared after a filter or
 * search change.
 */
@Stable
internal class NoteTagFolderExpansionState(
    initialCollapsedFolderIds: Set<String> = emptySet()
) {
    var collapsedFolderIds by mutableStateOf(initialCollapsedFolderIds)
        private set

    private var knownFolderIds by mutableStateOf<List<String>>(emptyList())

    val isAllCollapsed: Boolean
        get() = knownFolderIds.isNotEmpty() && knownFolderIds.all { it in collapsedFolderIds }

    fun syncFolderIds(folderIds: List<String>) {
        if (knownFolderIds != folderIds) knownFolderIds = folderIds
        val retained = collapsedFolderIds.intersect(folderIds.toSet())
        if (retained.size != collapsedFolderIds.size) collapsedFolderIds = retained
    }

    fun onToggleFolder(folderId: String) {
        collapsedFolderIds = if (folderId in collapsedFolderIds) {
            collapsedFolderIds - folderId
        } else {
            collapsedFolderIds + folderId
        }
    }

    /** Collapses every folder, or expands them all when everything is collapsed. */
    fun toggleAll() {
        collapsedFolderIds = if (isAllCollapsed) emptySet() else knownFolderIds.toSet()
    }

    companion object {
        val Saver: Saver<NoteTagFolderExpansionState, Any> = listSaver(
            save = { it.collapsedFolderIds.toList() },
            restore = { NoteTagFolderExpansionState(it.toSet()) }
        )
    }
}

@Composable
internal fun rememberNoteTagFolderExpansionState(): NoteTagFolderExpansionState =
    rememberSaveable(saver = NoteTagFolderExpansionState.Saver) {
        NoteTagFolderExpansionState()
    }

/**
 * Top-bar affordance that collapses every tag folder at once, or expands them
 * all when the collection is fully collapsed.
 */
@Composable
internal fun TagFolderExpandAllButton(
    isAllCollapsed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    NexIconButton(
        imageVector = if (isAllCollapsed) Icons.Default.UnfoldMore else Icons.Default.UnfoldLess,
        contentDescription = stringResource(
            if (isAllCollapsed) R.string.tag_folders_expand_all
            else R.string.tag_folders_collapse_all
        ),
        onClick = onClick,
        modifier = modifier
    )
}

/** Convenience overload for callers that already build the folder list. */
@Composable
internal fun rememberNoteTagFolderExpansionState(
    folders: List<NoteTagFolder>
): NoteTagFolderExpansionState {
    val state = rememberNoteTagFolderExpansionState()
    val folderIds = remember(folders) { folders.map { it.id } }
    LaunchedEffect(folderIds) { state.syncFolderIds(folderIds) }
    return state
}

@Composable
internal fun NoteTagFolderCollection(
    displayItems: List<ScoredNote>,
    listState: LazyListState,
    bottomContentPadding: Dp,
    modifier: Modifier = Modifier,
    expansionState: NoteTagFolderExpansionState = rememberNoteTagFolderExpansionState(),
    noteItemContent: @Composable (ScoredNote, Modifier) -> Unit
) {
    val folders = remember(displayItems) { buildNoteTagFolders(displayItems) }
    val folderIds = remember(folders) { folders.map { it.id } }
    LaunchedEffect(folderIds) { expansionState.syncFolderIds(folderIds) }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize().clipToBounds(),
        contentPadding = NoteCollectionLayoutDefaults.listContentPadding(
            bottomPadding = bottomContentPadding
        ),
        verticalArrangement = Arrangement.spacedBy(NoteCollectionLayoutDefaults.itemSpacing)
    ) {
        noteTagFolderItems(
            folders = folders,
            expansionState = expansionState,
            noteItemContent = noteItemContent
        )
    }
}

internal fun LazyListScope.noteTagFolderItems(
    folders: List<NoteTagFolder>,
    expansionState: NoteTagFolderExpansionState,
    horizontalPadding: Dp = 0.dp,
    noteItemContent: @Composable (ScoredNote, Modifier) -> Unit
) {
    folders.forEach { folder ->
        item(
            key = "tag_folder_header_${folder.id}",
            contentType = "tag_folder_header"
        ) {
            val isExpanded = folder.id !in expansionState.collapsedFolderIds
            NoteTagFolderHeader(
                folder = folder,
                isExpanded = isExpanded,
                onToggle = { expansionState.onToggleFolder(folder.id) },
                modifier = Modifier
                    .padding(horizontal = horizontalPadding)
                    .then(animateNoteItem())
            )
        }

        if (folder.id !in expansionState.collapsedFolderIds) {
            items(
                items = folder.items,
                key = { scored -> "tag_folder_${folder.id}_${scored.note.id}" },
                contentType = { "tag_folder_note" }
            ) { scored ->
                noteItemContent(
                    scored,
                    Modifier
                        .padding(
                            start = horizontalPadding + 8.dp,
                            end = horizontalPadding + 8.dp
                        )
                        .then(animateNoteItem())
                )
            }
        }
    }
}

internal fun buildNoteTagFolders(displayItems: List<ScoredNote>): List<NoteTagFolder> {
    if (displayItems.isEmpty()) return emptyList()

    val taggedItems = linkedMapOf<String, MutableList<ScoredNote>>()
    val untaggedItems = mutableListOf<ScoredNote>()
    displayItems.forEach { item ->
        val tags = TagParser.extractTags(item.note.content).sorted()
        if (tags.isEmpty()) {
            untaggedItems += item
        } else {
            tags.forEach { tag ->
                taggedItems.getOrPut(tag) { mutableListOf() } += item
            }
        }
    }

    val tagFolders = taggedItems.toSortedMap().map { (tag, notes) ->
        NoteTagFolder(
            id = tag,
            title = "#$tag",
            isUntagged = false,
            items = notes
        )
    }
    if (untaggedItems.isEmpty()) return tagFolders

    return tagFolders + NoteTagFolder(
        id = UNTAGGED_FOLDER_ID,
        title = "",
        isUntagged = true,
        items = untaggedItems
    )
}

@Composable
private fun NoteTagFolderHeader(
    folder: NoteTagFolder,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        onClick = onToggle,
        modifier = modifier
            .fillMaxWidth()
            .testTag(noteTagFolderHeaderTag(folder.id)),
        shape = MaterialTheme.shapes.medium,
        color = colorScheme.surfaceContainerHigh,
        contentColor = colorScheme.onSurface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NoteTagFolderIcon(isUntagged = folder.isUntagged)
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = if (folder.isUntagged) {
                        stringResource(R.string.tag_folder_untagged)
                    } else {
                        folder.title
                    },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = pluralStringResource(
                        R.plurals.tag_folder_note_count,
                        folder.noteCount,
                        folder.noteCount
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = if (isExpanded) {
                    Icons.Default.KeyboardArrowDown
                } else {
                    Icons.AutoMirrored.Filled.KeyboardArrowRight
                },
                contentDescription = null,
                tint = colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun NoteTagFolderIcon(isUntagged: Boolean) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (isUntagged) {
            colorScheme.secondaryContainer
        } else {
            colorScheme.primaryContainer
        },
        contentColor = if (isUntagged) {
            colorScheme.onSecondaryContainer
        } else {
            colorScheme.onPrimaryContainer
        }
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

private const val UNTAGGED_FOLDER_ID = "__untagged__"

/** Stable test hook: note cards now also render tags, so tests scope to the folder header. */
internal fun noteTagFolderHeaderTag(folderId: String): String = "note_tag_folder_header_$folderId"

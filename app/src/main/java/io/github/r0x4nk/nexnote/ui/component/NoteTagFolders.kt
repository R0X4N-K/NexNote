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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.domain.model.ScoredNote
import io.github.r0x4nk.nexnote.ui.common.NoteCollectionLayoutDefaults
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

@Immutable
internal data class NoteTagFolderExpansionState(
    val collapsedFolderIds: Set<String>,
    val onToggleFolder: (String) -> Unit
)

@Composable
internal fun NoteTagFolderCollection(
    displayItems: List<ScoredNote>,
    listState: LazyListState,
    bottomContentPadding: Dp,
    modifier: Modifier = Modifier,
    noteItemContent: @Composable (ScoredNote, Modifier) -> Unit
) {
    val folders = remember(displayItems) { buildNoteTagFolders(displayItems) }
    val expansionState = rememberNoteTagFolderExpansionState(folders)

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
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

@Composable
internal fun rememberNoteTagFolderExpansionState(
    folders: List<NoteTagFolder>
): NoteTagFolderExpansionState {
    var collapsedFolderIdList by rememberSaveable { mutableStateOf(emptyList<String>()) }
    val folderIds = remember(folders) { folders.map { it.id }.toSet() }

    LaunchedEffect(folderIds) {
        collapsedFolderIdList = collapsedFolderIdList.filter { it in folderIds }
    }

    val collapsedFolderIds = remember(collapsedFolderIdList) {
        collapsedFolderIdList.toSet()
    }
    val onToggleFolder: (String) -> Unit = { folderId ->
        collapsedFolderIdList = if (folderId in collapsedFolderIds) {
            collapsedFolderIdList.filterNot { it == folderId }
        } else {
            collapsedFolderIdList + folderId
        }
    }

    return NoteTagFolderExpansionState(
        collapsedFolderIds = collapsedFolderIds,
        onToggleFolder = onToggleFolder
    )
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
                    .animateItem()
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
                        .animateItem()
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
        title = "Untagged",
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
        modifier = modifier.fillMaxWidth(),
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
                    text = folder.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = folder.noteCount.toNoteCountLabel(),
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

private fun Int.toNoteCountLabel(): String =
    if (this == 1) "1 note" else "$this notes"

private const val UNTAGGED_FOLDER_ID = "__untagged__"

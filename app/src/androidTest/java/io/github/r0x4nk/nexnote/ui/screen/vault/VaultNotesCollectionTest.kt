package io.github.r0x4nk.nexnote.ui.screen.vault

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.NoteCardStyle
import io.github.r0x4nk.nexnote.domain.model.Tag
import io.github.r0x4nk.nexnote.ui.common.NoteListViewMode
import io.github.r0x4nk.nexnote.ui.theme.NexNoteTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VaultNotesCollectionTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun activeVaultList_usesNormalNoteCardWithPinAffordance() {
        var clickedNoteId: Long? = null
        var actionRequestedNoteId: Long? = null
        var pinnedNoteId: Long? = null
        composeRule.setVaultNotesCollection(
            notes = listOf(
                Note(
                    id = 11L,
                    title = "Active Vault note",
                    content = "Active body preview",
                    isInVault = true,
                    isDeleted = false
                )
            ),
            isTrashVisible = false,
            onNoteClick = { clickedNoteId = it },
            onRequestNoteActions = { actionRequestedNoteId = it.id },
            onTogglePin = { pinnedNoteId = it.id }
        )

        composeRule.onNodeWithText("Active Vault note").assertIsDisplayed()
        composeRule.onNodeWithText("Active body preview").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Pin to top")
            .assertIsDisplayed()
            .performClick()
        assertEquals(11L, pinnedNoteId)

        composeRule.onNodeWithText("Active Vault note").performClick()
        assertEquals(11L, clickedNoteId)

        composeRule.onNodeWithText("Active Vault note")
            .performTouchInput { longClick() }
        assertEquals(11L, actionRequestedNoteId)
    }

    @Test
    fun activeVaultList_honorsTitleOnlyNoteCardStyle() {
        composeRule.setVaultNotesCollection(
            notes = listOf(
                Note(
                    id = 12L,
                    title = "Compact Vault note",
                    content = "Hidden compact preview",
                    isInVault = true,
                    isDeleted = false
                )
            ),
            noteCardStyle = NoteCardStyle.TITLE_ONLY,
            isTrashVisible = false
        )

        composeRule.onNodeWithText("Compact Vault note").assertIsDisplayed()
        composeRule.onAllNodesWithText("Hidden compact preview").assertCountEquals(0)
        composeRule.onNodeWithContentDescription("Pin to top").assertIsDisplayed()
    }

    @Test
    fun activeVaultList_showsTopTagsAndInvokesToggleCallback() {
        val toggledTags = mutableListOf<String>()
        composeRule.setVaultNotesCollection(
            notes = listOf(
                Note(
                    id = 16L,
                    title = "Tagged Vault note",
                    content = "Body with #alpha",
                    isInVault = true,
                    isDeleted = false
                )
            ),
            isTrashVisible = false,
            topTags = listOf(
                Tag(
                    name = "alpha",
                    noteCount = 1,
                    createdDate = 1L,
                    lastUpdatedDate = 2L
                ),
                Tag(
                    name = "beta",
                    noteCount = 1,
                    createdDate = 1L,
                    lastUpdatedDate = 2L
                )
            ),
            onToggleTagFilter = { toggledTags += it }
        )

        composeRule.onNodeWithText("#alpha")
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithText("#beta").assertIsDisplayed()

        assertEquals(listOf("alpha"), toggledTags)
    }

    @Test
    fun activeVaultList_selectedTagFiltersCanBeRemovedAndCleared() {
        val removedTags = mutableListOf<String>()
        var clearCount = 0
        composeRule.setVaultNotesCollection(
            notes = listOf(
                Note(
                    id = 17L,
                    title = "Filtered Vault note",
                    content = "Body with #alpha #beta",
                    isInVault = true,
                    isDeleted = false
                )
            ),
            isTrashVisible = false,
            selectedTagFilters = setOf("alpha", "beta"),
            onRemoveTagFilter = { removedTags += it },
            onClearTagFilters = { clearCount++ }
        )

        composeRule.onNodeWithText("Filter").assertIsDisplayed()
        composeRule.onNodeWithText("#alpha").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Remove #alpha filter")
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithText("Clear all")
            .assertIsDisplayed()
            .performClick()

        assertEquals(listOf("alpha"), removedTags)
        assertEquals(1, clearCount)
    }

    @Test
    fun activeVaultList_searchHidesTopTagsButKeepsSelectedTagFiltersRemovable() {
        val toggledTags = mutableListOf<String>()
        val removedTags = mutableListOf<String>()
        var clearCount = 0
        composeRule.setVaultNotesCollection(
            notes = listOf(
                Note(
                    id = 19L,
                    title = "Search filtered Vault note",
                    content = "Body with #alpha",
                    isInVault = true,
                    isDeleted = false
                )
            ),
            isTrashVisible = false,
            isSearchActive = true,
            topTags = listOf(
                Tag(
                    name = "alpha",
                    noteCount = 1,
                    createdDate = 1L,
                    lastUpdatedDate = 2L
                ),
                Tag(
                    name = "beta",
                    noteCount = 1,
                    createdDate = 1L,
                    lastUpdatedDate = 2L
                )
            ),
            selectedTagFilters = setOf("alpha"),
            onToggleTagFilter = { toggledTags += it },
            onRemoveTagFilter = { removedTags += it },
            onClearTagFilters = { clearCount++ }
        )

        composeRule.onNodeWithText("Search filtered Vault note").assertIsDisplayed()
        composeRule.onAllNodesWithText("#beta").assertCountEquals(0)
        composeRule.onNodeWithText("Filter").assertIsDisplayed()
        composeRule.onNodeWithText("#alpha").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Remove #alpha filter")
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithText("Clear all")
            .assertIsDisplayed()
            .performClick()

        assertEquals(emptyList<String>(), toggledTags)
        assertEquals(listOf("alpha"), removedTags)
        assertEquals(1, clearCount)
    }

    @Test
    fun activeVaultList_swipeLeftMovesNoteToTrash() {
        val movedNoteIds = mutableListOf<Long>()
        composeRule.setVaultNotesCollection(
            notes = listOf(
                Note(
                    id = 13L,
                    title = "Swipe Vault list note",
                    content = "Swipe body preview",
                    isInVault = true,
                    isDeleted = false
                )
            ),
            isTrashVisible = false,
            onMoveToTrash = { movedNoteIds += it.id }
        )

        composeRule.onNodeWithTag(VAULT_NOTE_ROW_TAG)
            .performTouchInput { swipeLeft() }

        composeRule.waitUntil(timeoutMillis = 3_000) {
            movedNoteIds == listOf(13L)
        }
    }

    @Test
    fun activeVaultGrid_swipeLeftMovesNoteToTrash() {
        val movedNoteIds = mutableListOf<Long>()
        composeRule.setVaultNotesCollection(
            notes = listOf(
                Note(
                    id = 14L,
                    title = "Swipe Vault grid note",
                    content = "Swipe body preview",
                    isInVault = true,
                    isDeleted = false
                )
            ),
            viewMode = NoteListViewMode.GRID,
            isTrashVisible = false,
            onMoveToTrash = { movedNoteIds += it.id }
        )

        composeRule.onNodeWithTag(VAULT_NOTE_ROW_TAG)
            .performTouchInput { swipeLeft() }

        composeRule.waitUntil(timeoutMillis = 3_000) {
            movedNoteIds == listOf(14L)
        }
    }

    @Test
    fun vaultTrashList_showsDeletedVaultNoteWithRestoreAndDeleteActions() {
        var restoredNoteId: Long? = null
        var deleteRequestedNoteId: Long? = null
        composeRule.setVaultNotesCollection(
            notes = listOf(
                Note(
                    id = 7L,
                    title = "Deleted Vault note",
                    content = "Deleted body preview",
                    isInVault = true,
                    isDeleted = true
                )
            ),
            isTrashVisible = true,
            onRestoreFromTrash = { restoredNoteId = it.id },
            onRequestDeletePermanentlyFromTrash = { deleteRequestedNoteId = it.id }
        )

        composeRule.onNodeWithText("Deleted Vault note").assertIsDisplayed()
        composeRule.onNodeWithText("Deleted body preview").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Delete permanently")
            .assertIsDisplayed()
            .performClick()
        assertEquals(7L, deleteRequestedNoteId)

        composeRule.onNodeWithContentDescription("Restore note")
            .assertIsDisplayed()
            .performClick()

        assertEquals(7L, restoredNoteId)
    }

    @Test
    fun vaultTrashList_swipeLeftDoesNotMoveNoteToTrashAgain() {
        val movedNoteIds = mutableListOf<Long>()
        composeRule.setVaultNotesCollection(
            notes = listOf(
                Note(
                    id = 15L,
                    title = "Deleted Vault swipe note",
                    content = "Deleted body preview",
                    isInVault = true,
                    isDeleted = true
                )
            ),
            isTrashVisible = true,
            onMoveToTrash = { movedNoteIds += it.id }
        )

        composeRule.onNodeWithText("Deleted Vault swipe note")
            .performTouchInput { swipeLeft() }
        composeRule.waitForIdle()

        assertEquals(emptyList<Long>(), movedNoteIds)
    }

    @Test
    fun vaultTrashList_doesNotShowVaultTagFilters() {
        composeRule.setVaultNotesCollection(
            notes = listOf(
                Note(
                    id = 18L,
                    title = "Deleted tagged Vault note",
                    content = "Deleted body with #alpha",
                    isInVault = true,
                    isDeleted = true
                )
            ),
            isTrashVisible = true,
            topTags = listOf(
                Tag(
                    name = "alpha",
                    noteCount = 1,
                    createdDate = 1L,
                    lastUpdatedDate = 2L
                )
            ),
            selectedTagFilters = setOf("alpha")
        )

        composeRule.onNodeWithText("Deleted tagged Vault note").assertIsDisplayed()
        composeRule.onAllNodesWithText("#alpha").assertCountEquals(0)
        composeRule.onAllNodesWithText("Filter").assertCountEquals(0)
        composeRule.onAllNodesWithText("Clear all").assertCountEquals(0)
    }

    @Test
    fun vaultTrashList_emptyStateDoesNotExposeActions() {
        composeRule.setVaultNotesCollection(
            notes = emptyList(),
            isTrashVisible = true
        )

        composeRule.onNodeWithText("Vault trash empty").assertIsDisplayed()
        composeRule.onNodeWithText("No deleted Vault notes.").assertIsDisplayed()
    }

    @Test
    fun activeVaultList_emptyStateUsesAlignedCopyWithHome() {
        composeRule.setVaultNotesCollection(
            notes = emptyList(),
            isTrashVisible = false
        )

        composeRule.onNodeWithText("No Vault notes").assertIsDisplayed()
        composeRule
            .onNodeWithText("Use the + button below to create your first Vault note")
            .assertIsDisplayed()
        // Ensure the previous, replaced copy is no longer rendered to avoid
        // visual drift between Home and Vault empty states.
        composeRule.onAllNodesWithText("Vault unlocked").assertCountEquals(0)
        composeRule
            .onAllNodesWithText("No notes in your Vault yet.")
            .assertCountEquals(0)
    }

    @Test
    fun activeVaultList_emptyStateShowsSearchAlignedCopyForEmptyResults() {
        composeRule.setVaultNotesCollection(
            notes = emptyList(),
            isTrashVisible = false,
            isSearchActive = true
        )

        composeRule.onNodeWithText("No results").assertIsDisplayed()
        composeRule.onNodeWithText("Try different words").assertIsDisplayed()
        // Older Vault-only punctuation must not regress alongside Home's copy.
        composeRule.onAllNodesWithText("Try different words.").assertCountEquals(0)
    }

    @Test
    fun activeVaultList_emptyStateUsesHomeCopyForTagFilters() {
        composeRule.setVaultNotesCollection(
            notes = emptyList(),
            isTrashVisible = false,
            selectedTagFilters = setOf("alpha", "beta")
        )

        composeRule.onNodeWithText("No notes with these tags").assertIsDisplayed()
        composeRule.onNodeWithText("Try removing some tag filters").assertIsDisplayed()
        composeRule.onAllNodesWithText("No results").assertCountEquals(0)
        composeRule.onAllNodesWithText("Try different tags").assertCountEquals(0)
    }

    @Test
    fun activeVaultList_emptyStateKeepsSearchPriorityWhenSearchAndTagFiltersAreActive() {
        composeRule.setVaultNotesCollection(
            notes = emptyList(),
            isTrashVisible = false,
            isSearchActive = true,
            selectedTagFilters = setOf("alpha")
        )

        composeRule.onNodeWithText("No results").assertIsDisplayed()
        composeRule.onNodeWithText("Try different words").assertIsDisplayed()
        composeRule.onAllNodesWithText("Try different tags or words").assertCountEquals(0)
        composeRule.onAllNodesWithText("No notes with these tags").assertCountEquals(0)
    }

    private fun ComposeContentTestRule.setVaultNotesCollection(
        notes: List<Note>,
        isTrashVisible: Boolean,
        viewMode: NoteListViewMode = NoteListViewMode.LIST,
        noteCardStyle: NoteCardStyle = NoteCardStyle.TITLE_AND_PREVIEW,
        isSearchActive: Boolean = false,
        topTags: List<Tag> = emptyList(),
        selectedTagFilters: Set<String> = emptySet(),
        onNoteClick: (Long) -> Unit = {},
        onRequestNoteActions: (Note) -> Unit = {},
        onMoveToTrash: (Note) -> Unit = {},
        onTogglePin: (Note) -> Unit = {},
        onToggleTagFilter: (String) -> Unit = {},
        onRemoveTagFilter: (String) -> Unit = {},
        onClearTagFilters: () -> Unit = {},
        onRestoreFromTrash: (Note) -> Unit = {},
        onRequestDeletePermanentlyFromTrash: (Note) -> Unit = {}
    ) {
        setContent {
            NexNoteTheme {
                Box(Modifier.fillMaxSize()) {
                    VaultNotesCollection(
                        notes = notes,
                        viewMode = viewMode,
                        noteCardStyle = noteCardStyle,
                        isTrashVisible = isTrashVisible,
                        isSearchActive = isSearchActive,
                        topTags = topTags,
                        selectedTagFilters = selectedTagFilters,
                        onNoteClick = onNoteClick,
                        onRequestNoteActions = onRequestNoteActions,
                        onMoveToTrash = onMoveToTrash,
                        onTogglePin = onTogglePin,
                        onToggleTagFilter = onToggleTagFilter,
                        onRemoveTagFilter = onRemoveTagFilter,
                        onClearTagFilters = onClearTagFilters,
                        onRestoreFromTrash = onRestoreFromTrash,
                        onRequestDeletePermanentlyFromTrash =
                            onRequestDeletePermanentlyFromTrash,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

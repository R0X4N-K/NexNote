package io.github.r0x4nk.nexnote.data.repository

import androidx.room.RoomDatabase
import androidx.room.withTransaction
import io.github.r0x4nk.nexnote.data.db.NoteContentPatchDao
import io.github.r0x4nk.nexnote.data.db.TagDao
import io.github.r0x4nk.nexnote.data.db.TagWithCount
import io.github.r0x4nk.nexnote.data.db.entity.NoteTagCrossRef
import io.github.r0x4nk.nexnote.data.db.entity.TagEntity
import io.github.r0x4nk.nexnote.domain.model.Tag
import io.github.r0x4nk.nexnote.domain.repository.TagRepository
import io.github.r0x4nk.nexnote.util.NexNoteDebugLog
import io.github.r0x4nk.nexnote.util.TagParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Room-backed implementation of the domain [TagRepository] contract.
 *
 * Role: data layer — bridges the DAO layer and the domain/ViewModel layer.
 *
 * [noteContentPatchDao] is injected alongside [tagDao] because tag deletion
 * must patch note content directly (stripping '#' prefixes) without going
 * through [NoteRepositoryImpl], which would trigger a full note-save cycle and
 * re-index tags unnecessarily.
 *
 * All Flow-returning methods are observed reactively by the UI. Room re-emits
 * whenever the underlying `tags` or `note_tag_cross_ref` tables change.
 */
class TagRepositoryImpl(
    private val database: RoomDatabase,
    private val tagDao: TagDao,
    private val noteContentPatchDao: NoteContentPatchDao
) : TagRepository {

    // ── Queries ───────────────────────────────────────────────────────────────

    /** All tags sorted by active-note usage count, most used first. */
    override fun getAllTagsByUsageDesc(): Flow<List<Tag>> =
        tagDao.getAllTagsByUsageDesc().map { it.toDomain() }

    /** All tags sorted by active-note usage count, least used first. */
    override fun getAllTagsByUsageAsc(): Flow<List<Tag>> =
        tagDao.getAllTagsByUsageAsc().map { it.toDomain() }

    /** All tags sorted by last-updated date, most recently used first. */
    override fun getAllTagsByDateDesc(): Flow<List<Tag>> =
        tagDao.getAllTagsByDateDesc().map { it.toDomain() }

    /** All tags sorted by last-updated date, oldest first. */
    override fun getAllTagsByDateAsc(): Flow<List<Tag>> =
        tagDao.getAllTagsByDateAsc().map { it.toDomain() }

    /**
     * Tags whose name contains [query], sorted by usage. Falls back to
     * [getAllTagsByUsageDesc] when [query] is blank.
     */
    override fun searchTags(query: String): Flow<List<Tag>> =
        if (query.isBlank()) getAllTagsByUsageDesc()
        else tagDao.searchTagsByName(query).map { it.toDomain() }

    /**
     * Tags associated with a specific note.
     * Returns an empty flow when [noteId] == 0 (unsaved note).
     */
    override fun getTagsForNote(noteId: Long): Flow<List<Tag>> =
        if (noteId == 0L) flowOf(emptyList())
        else tagDao.getTagsForNote(noteId).map { it.toDomain() }

    /**
     * The [limit] most-used tags, for compact display in [AutoScrollingTagRow].
     */
    override fun getMostUsedTags(limit: Int): Flow<List<Tag>> =
        tagDao.getMostUsedTags(limit).map { it.toDomain() }

    /**
     * Note IDs that contain ALL of the specified tags (intersection / AND logic).
     * A note must have every tag in [tagNames] to appear in the result.
     * Emits an empty set immediately when [tagNames] is empty (no filter active).
     */
    override fun getFilteredNoteIds(tagNames: Set<String>): Flow<Set<Long>> {
        if (tagNames.isEmpty()) return flowOf(emptySet())
        return tagDao.getNoteIdsWithAllTags(tagNames.toList(), tagNames.size)
            .map { it.toSet() }
    }

    // ── Tag indexing ──────────────────────────────────────────────────────────

    /**
     * Re-indexes tags for a note after its content changes.
     *
     * This is a differential update:
     * 1. Parse new tags from [content] via [TagParser].
     * 2. Compare with the existing cross-refs for [noteId].
     * 3. Delete cross-refs for tags no longer present in the note.
     * 4. Insert [TagEntity] rows for brand-new tags (IGNORE on conflict).
     * 5. Insert new cross-refs for tags newly added to the note.
     * 6. Touch [TagEntity.lastUpdatedDate] for tags that remain in the note.
     * 7. Prune orphan [TagEntity] rows (tags referenced by no note at all).
     *
     * Runs on whichever dispatcher the caller provides. ViewModels call this
     * from [viewModelScope.launch], and Room handles its own IO dispatching
     * internally for each DAO suspend function.
     *
     * @param noteId Persisted database ID of the note (must be > 0).
     * @param content Current note content to parse for tags.
     */
    override suspend fun indexNoteTags(noteId: Long, content: String) {
        if (noteId <= 0L) {
            NexNoteDebugLog.repositoryWarning(event = "indexNoteTagsSkipped") {
                "reason=unsavedNote noteId=$noteId"
            }
            return
        }

        val newTags = TagParser.extractTags(content)

        database.withTransaction {
            val existingTags = tagDao.getCrossRefsForNote(noteId).map { it.tagName }.toSet()

            val toAdd    = newTags - existingTags
            val toRemove = existingTags - newTags
            val toTouch  = newTags intersect existingTags

            val now = System.currentTimeMillis()

            for (tagName in toRemove) {
                tagDao.deleteCrossRef(noteId, tagName)
            }

            for (tagName in toAdd) {
                tagDao.insertTag(TagEntity(name = tagName, createdDate = now, lastUpdatedDate = now))
                tagDao.insertCrossRef(NoteTagCrossRef(noteId = noteId, tagName = tagName))
            }

            for (tagName in toTouch) {
                tagDao.touchTag(tagName, now)
            }

            // Clean up tags that became orphaned after this re-index.
            if (toRemove.isNotEmpty()) {
                tagDao.pruneOrphanTags()
            }
        }
    }

    // ── Tag deletion ──────────────────────────────────────────────────────────

    /**
     * Deletes a tag from the index and removes its '#' prefix from all note content.
     *
     * Deletion behaviour (deliberate choice):
     *   `#tagName` → `tagName`
     * The '#' symbol is stripped but the word is preserved in the note text.
     * This minimises data loss: the user's original wording survives; only the
     * tag semantics are removed. Notes are never deleted as a side effect.
     *
     * Steps:
     * 1. Find all notes (active and trashed) that reference [tagName].
     * 2. Replace `#tagName` with `tagName` in each note's content.
     * 3. Update the note row with the new content (and a fresh lastModifiedDate).
     * 4. Remove all [NoteTagCrossRef] rows for this tag.
     * 5. Remove the [TagEntity] row.
     *
     * The '#' replacement uses a word-boundary regex to avoid partial matches:
     *   - `#todo` in `#todo list` becomes `todo list` ✅
     *   - `#to` does NOT match `#todo` ✅
     *
     * @param tagName Lowercase name of the tag to delete (without '#').
     */
    override suspend fun deleteTag(tagName: String) {
        // Word-boundary on the right ensures `#tag` does not partially match `#tags`.
        val tagRegex = Regex(
            pattern = """(?:^|\s)#${Regex.escape(tagName)}(?=\s|${'$'})""",
            options = setOf(RegexOption.MULTILINE)
        )

        database.withTransaction {
            val patches = noteContentPatchDao.getPatchesForTag(tagName)
            val now = System.currentTimeMillis()

            for (patch in patches) {
                val newContent = tagRegex.replace(patch.content) { match ->
                    // Replace '#tagName' with 'tagName' while preserving the leading whitespace.
                    match.value.replace("#$tagName", tagName)
                }
                if (newContent != patch.content) {
                    noteContentPatchDao.updateContent(patch.id, newContent, now)
                }
            }

            tagDao.deleteAllCrossRefsForTag(tagName)
            tagDao.deleteTagByName(tagName)
        }
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private fun List<TagWithCount>.toDomain(): List<Tag> = map { it.toDomain() }

    private fun TagWithCount.toDomain(): Tag = Tag(
        name            = name,
        noteCount       = noteCount,
        createdDate     = createdDate,
        lastUpdatedDate = lastUpdatedDate
    )

    companion object {
        /** Default limit for the top-tags display in Home/Agenda rows. */
        const val TOP_TAGS_LIMIT = 15
    }
}

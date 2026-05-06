package io.github.r0x4nk.nexnote.ui.screen.editor

/**
 * Deduplicates redundant tag-index requests across consecutive autosaves.
 *
 * Background: every save flushed by [EditorSaveDelegate] re-runs
 * `IndexNoteTagsUseCase`, which walks the entire note content with the tag
 * regex parser and runs a Room transaction that touches every existing tag
 * (`tagDao.touchTag(...)`). Most autosaves, however, are triggered by edits
 * that cannot affect the hashtag set — title changes, background colour
 * changes, markdown-mode toggles, preview toggles, etc. Re-indexing on those
 * saves is wasted work and, on very long notes, the hot path that scales worst
 * with note size after content persistence itself.
 *
 * This policy keeps a hash of the content that was last successfully indexed
 * for a given note id. Subsequent indexing attempts with the same content are
 * suppressed; any change in content (or in note id) re-enables indexing on the
 * next save. Hash collisions only delay re-indexing for one save — the next
 * legitimately different content will hash differently and be re-indexed.
 *
 * Thread-safety: the holder relies on the caller serialising indexing through
 * a save mutex, which mirrors how [EditorSaveDelegate] already gates persistence.
 */
internal class TagIndexDedupPolicy {

    private var lastIndexedNoteId: Long = INVALID_NOTE_ID
    private var lastIndexedContentHash: Int? = null

    /**
     * Returns `true` if [content] for [noteId] differs from what was last
     * indexed. When `true`, the policy atomically remembers the new hash so
     * that the next call with the same inputs returns `false`.
     *
     * Returns `false` for invalid [noteId]s (≤ 0): unsaved notes have no row
     * to attach tag cross-refs to, so indexing is a no-op upstream anyway.
     */
    fun shouldIndexAndRemember(noteId: Long, content: String): Boolean {
        if (noteId <= INVALID_NOTE_ID) return false

        val hash = content.hashCode()
        if (noteId == lastIndexedNoteId && hash == lastIndexedContentHash) return false

        lastIndexedNoteId = noteId
        lastIndexedContentHash = hash
        return true
    }

    /**
     * Forces the next [shouldIndexAndRemember] call to return `true` even when
     * inputs match the previous successful index.
     *
     * Callers must invoke this after an indexing failure so that the next save
     * retries; otherwise a transient DB error would leave tags permanently
     * out of sync with content.
     */
    fun forgetLastIndex() {
        lastIndexedContentHash = null
    }

    companion object {
        const val INVALID_NOTE_ID = 0L
    }
}

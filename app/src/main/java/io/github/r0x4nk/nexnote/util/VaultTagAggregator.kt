package io.github.r0x4nk.nexnote.util

import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.Tag

/**
 * Pure in-memory aggregation of hashtags extracted from already-decrypted
 * active Vault notes. The object has no Room, Android, Keystore, DataStore or
 * filesystem dependency, so it can be reused by the data repository and by UI
 * state that already holds unlocked Vault notes without introducing another
 * plaintext observation path.
 *
 * Security contract:
 * - The function is pure: no DB write, no log, no I/O, no global state.
 * - The caller is responsible for ensuring the [notes] list only contains
 *   notes that the user is currently allowed to see (i.e. emitted while the
 *   Vault key is in memory). When the repository emits an empty list because
 *   the Vault is locked, this function trivially returns an empty list too.
 * - Deleted (trash) Vault notes are skipped even if they slip through the
 *   input, so the aggregation cannot expose tags from the Vault trash via a
 *   surface that should only see active notes.
 * - The aggregator never reads or writes the normal `tags` or
 *   `note_tag_cross_refs` tables.
 */
internal object VaultTagAggregator {

    /**
     * Aggregate hashtags from the [notes] active Vault notes. Returns the same
     * [Tag] domain model used by the normal tag pipeline so the UI can share
     * components without persisting anything.
     *
     * Aggregation rules:
     * - Tag names come from [TagParser.extractTags] applied to note content;
     *   the parser already lowercases names, ignores fenced code
     *   blocks and rejects numeric-only tokens.
     * - [Tag.noteCount] counts notes that contain the tag at least once.
     * - [Tag.createdDate] is the minimum [Note.creationDate] across notes
     *   referencing the tag.
     * - [Tag.lastUpdatedDate] is the maximum [Note.lastModifiedDate] across
     *   notes referencing the tag.
     * - Output sorted by descending [Tag.noteCount] then ascending name, to be
     *   stable across recompositions.
     */
    fun aggregate(notes: List<Note>): List<Tag> {
        if (notes.isEmpty()) return emptyList()

        // Mutable accumulator kept local to this function call.
        class Acc(var count: Int, var minCreated: Long, var maxUpdated: Long)

        val accumulators = LinkedHashMap<String, Acc>()
        for (note in notes) {
            if (note.isDeleted) continue
            val tags = TagParser.extractTags(note.content)
            if (tags.isEmpty()) continue
            for (tagName in tags) {
                val acc = accumulators.getOrPut(tagName) {
                    Acc(
                        count = 0,
                        minCreated = note.creationDate,
                        maxUpdated = note.lastModifiedDate
                    )
                }
                acc.count += 1
                if (note.creationDate < acc.minCreated) acc.minCreated = note.creationDate
                if (note.lastModifiedDate > acc.maxUpdated) acc.maxUpdated = note.lastModifiedDate
            }
        }

        return accumulators.entries
            .map { (name, acc) ->
                Tag(
                    name = name,
                    noteCount = acc.count,
                    createdDate = acc.minCreated,
                    lastUpdatedDate = acc.maxUpdated
                )
            }
            .sortedWith(compareByDescending<Tag> { it.noteCount }.thenBy { it.name })
    }
}

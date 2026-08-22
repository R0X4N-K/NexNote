package io.github.r0x4nk.nexnote.domain.model

/** Bounded query for the notes currently materialised by Home. */
data class HomeNotesQuery(
    val text: String = "",
    val sortAscending: Boolean = false,
    val searchSort: NoteSearchSort = NoteSearchSort.RELEVANCE,
    val searchScope: NoteSearchScope = NoteSearchScope.TITLE_AND_CONTENT,
    val pinnedFilter: NotePinnedFilter = NotePinnedFilter.ALL,
    val tagNames: Set<String> = emptySet(),
    val limit: Int
)

/** Note fields included by a text-search surface. */
enum class NoteSearchScope {
    TITLE_AND_CONTENT,
    TITLE,
    CONTENT
}

/** Optional pinned-state constraint applied to note search results. */
enum class NotePinnedFilter {
    ALL,
    PINNED,
    UNPINNED
}

/** Ordering available while a note search is active. */
enum class NoteSearchSort {
    RELEVANCE,
    MODIFIED_DESC,
    MODIFIED_ASC,
    TITLE_ASC,
    TITLE_DESC
}

typealias HomeSearchScope = NoteSearchScope
typealias HomePinnedFilter = NotePinnedFilter
typealias HomeSearchSort = NoteSearchSort

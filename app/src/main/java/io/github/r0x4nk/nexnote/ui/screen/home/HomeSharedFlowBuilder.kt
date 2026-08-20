package io.github.r0x4nk.nexnote.ui.screen.home

import io.github.r0x4nk.nexnote.domain.model.Tag
import io.github.r0x4nk.nexnote.domain.model.Template
import io.github.r0x4nk.nexnote.domain.usecase.ObserveMostUsedTagsUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveTemplatesUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

internal fun buildHomeTemplatesFlow(
    observeTemplates: ObserveTemplatesUseCase,
    scope: CoroutineScope
): StateFlow<List<Template>> {
    return observeTemplates()
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )
}

internal fun buildHomeTopTagsFlow(
    observeMostUsedTags: ObserveMostUsedTagsUseCase,
    scope: CoroutineScope
): StateFlow<List<Tag>> {
    return observeMostUsedTags()
        .stateIn(
            scope = scope,
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )
}

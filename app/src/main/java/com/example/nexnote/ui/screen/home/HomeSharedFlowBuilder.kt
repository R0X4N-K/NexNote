package com.example.nexnote.ui.screen.home

import com.example.nexnote.domain.model.Tag
import com.example.nexnote.domain.model.Template
import com.example.nexnote.domain.usecase.ObserveMostUsedTagsUseCase
import com.example.nexnote.domain.usecase.ObserveTemplatesUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

internal fun buildHomeTemplatesFlow(
    observeTemplates: ObserveTemplatesUseCase?,
    scope: CoroutineScope
): StateFlow<List<Template>> {
    return (observeTemplates?.invoke() ?: flowOf(emptyList()))
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )
}

internal fun buildHomeTopTagsFlow(
    observeMostUsedTags: ObserveMostUsedTagsUseCase?,
    scope: CoroutineScope
): StateFlow<List<Tag>> {
    return (observeMostUsedTags?.invoke() ?: flowOf(emptyList()))
        .stateIn(
            scope = scope,
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )
}

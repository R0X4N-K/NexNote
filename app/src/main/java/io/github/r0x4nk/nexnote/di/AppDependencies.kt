package io.github.r0x4nk.nexnote.di

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import io.github.r0x4nk.nexnote.ui.screen.editor.EditorSaveCoordinator

/**
 * Narrow dependency entry point implemented by the application composition
 * root. UI factories depend on this contract rather than on the concrete
 * [android.app.Application] subclass.
 */
internal interface AppDependencies {
    val useCases: AppUseCases
    val editorSaveCoordinator: EditorSaveCoordinator
}

internal fun CreationExtras.requireAppDependencies(): AppDependencies =
    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as? AppDependencies
        ?: error("Application does not provide NexNote dependencies")

internal fun Context.requireAppDependencies(): AppDependencies =
    applicationContext as? AppDependencies
        ?: error("Application does not provide NexNote dependencies")

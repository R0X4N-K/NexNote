package io.github.r0x4nk.nexnote.di

import androidx.annotation.StringRes

/**
 * Resolves localized strings for view-model generated messages without leaking
 * an Android [android.content.Context] into the UI or domain layers.
 *
 * ViewModels receive this through [AppDependencies] and use it for snackbar and
 * progress copy that otherwise has no composable call site to resolve a
 * resource from.
 */
fun interface StringProvider {
    fun get(@StringRes id: Int, vararg formatArgs: Any): String
}

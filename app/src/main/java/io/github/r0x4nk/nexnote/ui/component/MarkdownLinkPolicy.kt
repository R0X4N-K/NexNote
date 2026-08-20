package io.github.r0x4nk.nexnote.ui.component

import java.net.URI
import java.net.URISyntaxException

private val SUPPORTED_MARKDOWN_LINK_SCHEMES = setOf("http", "https", "mailto")

internal fun isSupportedMarkdownLink(target: String): Boolean {
    return try {
        val uri = URI(target.trim())
        val scheme = uri.scheme?.lowercase()
            ?: return false
        when (scheme) {
            "http", "https" -> !uri.rawAuthority.isNullOrBlank()
            "mailto" -> uri.schemeSpecificPart.isNotBlank()
            else -> scheme in SUPPORTED_MARKDOWN_LINK_SCHEMES
        }
    } catch (_: URISyntaxException) {
        false
    }
}

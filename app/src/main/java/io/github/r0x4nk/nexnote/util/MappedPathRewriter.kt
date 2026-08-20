package io.github.r0x4nk.nexnote.util

/**
 * Rewrites mapped paths against the original text in one pass.
 *
 * A sequence of [String.replace] calls can cascade when one replacement is
 * also a source key. Matching longer paths first also prevents a path that is
 * a prefix of another from consuming only the shorter prefix.
 */
internal fun String.rewriteMappedPaths(pathMap: Map<String, String>): String {
    val replacements = pathMap.filter { (source, target) ->
        source.isNotEmpty() && source != target
    }
    if (replacements.isEmpty()) return this

    val sourcePattern = replacements.keys
        .sortedByDescending(String::length)
        .joinToString(separator = "|") { source -> Regex.escape(source) }
    return Regex(sourcePattern).replace(this) { match ->
        replacements.getValue(match.value)
    }
}

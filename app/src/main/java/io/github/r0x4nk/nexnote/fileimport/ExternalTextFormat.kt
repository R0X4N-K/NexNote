package io.github.r0x4nk.nexnote.fileimport

internal object ExternalTextFormat {
    private val extensions = setOf(
        "md", "markdown", "txt", "text", "log", "csv", "tsv",
        "json", "xml", "yaml", "yml", "toml"
    )

    private val applicationMimeTypes = setOf(
        "application/json",
        "application/xml",
        "application/yaml",
        "application/x-yaml",
        "application/markdown",
        "application/x-markdown",
        "application/toml",
        "application/x-toml"
    )

    fun isSupported(mimeType: String?, displayName: String?): Boolean {
        val normalizedMime = mimeType
            ?.substringBefore(';')
            ?.trim()
            ?.lowercase()
            .orEmpty()
        if (normalizedMime.startsWith("text/")) return true
        if (normalizedMime in applicationMimeTypes) return true

        val extension = displayName
            ?.substringAfterLast('/')
            ?.substringAfterLast(':')
            ?.substringAfterLast('.', missingDelimiterValue = "")
            ?.lowercase()
            .orEmpty()
        return normalizedMime in setOf("", "*/*", "application/octet-stream") &&
            extension in extensions
    }
}

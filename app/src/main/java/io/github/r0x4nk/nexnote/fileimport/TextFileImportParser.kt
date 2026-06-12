package io.github.r0x4nk.nexnote.fileimport

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

internal data class ImportedTextFile(
    val title: String,
    val content: String
)

internal sealed interface TextFileImportParseResult {
    data class Parsed(val file: ImportedTextFile) : TextFileImportParseResult
    data class Rejected(val message: String) : TextFileImportParseResult
}

internal object TextFileImportParser {
    const val MAX_CONTENT_CHARS = 500_000
    private const val UTF8_BOM_BYTE_COUNT = 3
    const val MAX_CONTENT_BYTES = MAX_CONTENT_CHARS * 4 + UTF8_BOM_BYTE_COUNT

    private const val DEFAULT_TITLE = "Imported note"
    private const val MAX_TITLE_CHARS = 160
    private const val UTF8_BOM = "\uFEFF"

    fun parse(displayName: String?, bytes: ByteArray): TextFileImportParseResult {
        val content = decodeUtf8(bytes)?.removePrefix(UTF8_BOM)
            ?: return TextFileImportParseResult.Rejected("Unsupported file encoding")

        if (content.length > MAX_CONTENT_CHARS) {
            return TextFileImportParseResult.Rejected("File is too large")
        }
        if (!content.hasOnlyTextControlCharacters()) {
            return TextFileImportParseResult.Rejected("File does not look like text")
        }

        return TextFileImportParseResult.Parsed(
            ImportedTextFile(
                title = titleFromDisplayName(displayName),
                content = content
            )
        )
    }

    fun titleFromDisplayName(displayName: String?): String {
        val normalized = displayName
            ?.substringAfterLast('/')
            ?.substringAfterLast(':')
            ?.trim()
            .orEmpty()

        val withoutExtension = normalized.removeFinalExtension()
        return withoutExtension
            .ifBlank { DEFAULT_TITLE }
            .take(MAX_TITLE_CHARS)
    }

    private fun decodeUtf8(bytes: ByteArray): String? {
        return try {
            StandardCharsets.UTF_8
                .newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString()
        } catch (_: CharacterCodingException) {
            null
        }
    }

    private fun String.hasOnlyTextControlCharacters(): Boolean =
        all { char ->
            !char.isISOControl() || char == '\n' || char == '\r' || char == '\t'
        }

    private fun String.removeFinalExtension(): String {
        val extensionSeparator = lastIndexOf('.')
        if (extensionSeparator <= 0) return this

        return substring(0, extensionSeparator).ifBlank { this }
    }
}

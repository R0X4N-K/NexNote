package io.github.r0x4nk.nexnote.ui.component

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownLinkPolicyTest {

    @Test
    fun `allows explicit web and email schemes`() {
        assertTrue(isSupportedMarkdownLink("https://example.com/path"))
        assertTrue(isSupportedMarkdownLink("HTTP://example.com"))
        assertTrue(isSupportedMarkdownLink("mailto:person@example.com"))
    }

    @Test
    fun `rejects local privileged and executable schemes`() {
        assertFalse(isSupportedMarkdownLink("file:///data/data/app/databases/nexnote.db"))
        assertFalse(isSupportedMarkdownLink("content://provider/private"))
        assertFalse(isSupportedMarkdownLink("intent://scan/#Intent;scheme=zxing;end"))
        assertFalse(isSupportedMarkdownLink("javascript:alert(1)"))
        assertFalse(isSupportedMarkdownLink("data:text/plain,secret"))
    }

    @Test
    fun `rejects relative malformed and hostless web links`() {
        assertFalse(isSupportedMarkdownLink("/relative/path"))
        assertFalse(isSupportedMarkdownLink("https:/missing-host"))
        assertFalse(isSupportedMarkdownLink("not a uri"))
        assertFalse(isSupportedMarkdownLink("mailto:"))
    }
}

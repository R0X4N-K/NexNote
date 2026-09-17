package io.github.r0x4nk.nexnote.ui.screen.tags

import android.content.res.Configuration
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.r0x4nk.nexnote.domain.model.Tag
import io.github.r0x4nk.nexnote.ui.theme.NexNoteTheme
import java.util.Locale
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TagTreemapAccessibilityTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun selectionAndPluralDescriptions_followLocaleChanges() {
        val locale = mutableStateOf("en")
        val selected = mutableStateOf<String?>(null)
        val baseContext = InstrumentationRegistry.getInstrumentation().targetContext
        composeRule.setContent {
            val configuration = Configuration(baseContext.resources.configuration).apply {
                setLocale(Locale.forLanguageTag(locale.value))
            }
            val context = baseContext.createConfigurationContext(configuration)
            CompositionLocalProvider(LocalContext provides context, LocalConfiguration provides configuration) {
                NexNoteTheme {
                    TagsTreemapList(
                        tags = listOf(Tag("work", 1, 0, 0), Tag("home", 2, 0, 0)),
                        maxCount = 2,
                        selectedTagName = selected.value,
                        notesForSelectedTag = emptyList(),
                        actions = TagsActions(
                            onSearchOpen = {}, onSearchClose = {}, onSearchQueryChange = {},
                            onSortMenuOpen = {}, onSortMenuDismiss = {}, onSortSelect = {},
                            onViewModeToggle = {}, onTagClick = { selected.value = it },
                            onNoteClick = {}, onRequestNoteActions = {}, onDeleteClick = {},
                            onConfirmDelete = {}, onDismissDialog = {}
                        ),
                        listState = rememberLazyListState(),
                        bottomContentPadding = 0.dp
                    )
                }
            }
        }
        composeRule.onNodeWithContentDescription("#work, 1 note")
            .assertIsNotSelected()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .performClick()
        val expected = listOf(
            Triple("it", "#work, 1 nota", "#home, 2 note"),
            Triple("es", "#work, 1 nota", "#home, 2 notas"),
            Triple("de", "#work, 1 Notiz", "#home, 2 Notizen"),
            Triple("fr", "#work, 1 note", "#home, 2 notes")
        )
        expected.forEach { (language, single, plural) ->
            composeRule.runOnIdle { locale.value = language }
            composeRule.onNodeWithContentDescription(single).assertIsSelected()
            composeRule.onNodeWithContentDescription(plural).assertIsNotSelected()
        }
    }
}

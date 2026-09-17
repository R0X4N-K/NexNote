package io.github.r0x4nk.nexnote.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.r0x4nk.nexnote.ui.component.radial.RadialMenu
import io.github.r0x4nk.nexnote.ui.component.radial.RadialMenuItem
import io.github.r0x4nk.nexnote.ui.component.radial.RadialMenuState
import io.github.r0x4nk.nexnote.ui.component.radial.StaticMenuButton
import io.github.r0x4nk.nexnote.ui.theme.NexNoteTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RadialMenuAccessibilityTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun semanticActions_activateEnabledItemsAndDismiss_withoutPointerCoordinates() {
        val clicks = mutableListOf<Int>()
        var dismissals = 0
        composeRule.setContent {
            NexNoteTheme {
                RadialMenu(
                    state = RadialMenuState(isOpen = true, center = Offset(500f, 700f), arcStartDeg = 285f, arcEndDeg = 360f),
                    items = listOf(
                        RadialMenuItem(Icons.Default.Add, "Create", action = {}),
                        RadialMenuItem(Icons.Default.Add, "Unavailable", enabled = false, action = {})
                    ),
                    onItemClick = { clicks += it },
                    onDismiss = { dismissals++ },
                    modifier = Modifier.testTag("menu")
                )
            }
        }
        composeRule.onNodeWithContentDescription("Create")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .performClick()
        composeRule.onAllNodesWithContentDescription("Create", useUnmergedTree = true)
            .assertCountEquals(1)
        composeRule.onNodeWithContentDescription("Unavailable")
            .assertIsNotEnabled()
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
        composeRule.onNodeWithTag("menu")
            .performSemanticsAction(SemanticsActions.Dismiss) { it() }
        composeRule.runOnIdle {
            assertEquals(listOf(0), clicks)
            assertEquals(1, dismissals)
        }
    }

    @Test
    fun menuButton_reportsExpansion_butDirectActionHasNoExpansionState() {
        val opensMenu = mutableStateOf(true)
        val isOpen = mutableStateOf(false)
        composeRule.setContent {
            NexNoteTheme {
                Box(Modifier.fillMaxSize()) {
                    StaticMenuButton(
                        isMenuOpen = isOpen.value,
                        fabX = 0f,
                        fabY = 0f,
                        buttonSizePx = 150f,
                        onClick = { isOpen.value = !isOpen.value },
                        opensMenu = opensMenu.value,
                        modifier = Modifier.testTag("button")
                    )
                }
            }
        }
        val collapsed = composeRule.onNodeWithTag("button").fetchSemanticsNode()
            .config[SemanticsProperties.StateDescription]
        composeRule.onNodeWithTag("button").performClick()
        composeRule.onNodeWithTag("button").assert(
            SemanticsMatcher("expanded state differs from collapsed") {
                it.config[SemanticsProperties.StateDescription] != collapsed
            }
        )
        composeRule.runOnIdle { opensMenu.value = false }
        composeRule.onNodeWithTag("button").assert(
            SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription)
        )
    }
}

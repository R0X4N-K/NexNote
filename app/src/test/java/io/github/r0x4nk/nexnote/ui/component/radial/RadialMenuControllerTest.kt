package io.github.r0x4nk.nexnote.ui.component.radial

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RadialMenuControllerTest {

    @Test
    fun `direct fab registration exposes action without menu items`() {
        val controller = RadialMenuController()
        val owner = Any()
        var clicks = 0

        controller.register(
            owner = owner,
            items = emptyList(),
            fabIcon = null,
            fabContentDescription = "Create note",
            fabAction = { clicks++ }
        )

        assertEquals(emptyList<RadialMenuItem>(), controller.items)
        assertEquals("Create note", controller.fabContentDescription)
        assertNotNull(controller.fabAction)

        controller.fabAction?.invoke()
        assertEquals(1, clicks)

        controller.clearRegistration(owner)
        assertNull(controller.fabAction)
        assertNull(controller.fabContentDescription)
    }

    @Test
    fun `stale registration cannot clear active fab state`() {
        val controller = RadialMenuController()
        val staleOwner = Any()
        val activeOwner = Any()
        var activeClicks = 0

        controller.register(
            owner = staleOwner,
            items = emptyList(),
            fabIcon = null,
            fabContentDescription = "Old action",
            fabAction = {}
        )
        controller.register(
            owner = activeOwner,
            items = emptyList(),
            fabIcon = null,
            fabContentDescription = "New action",
            fabAction = { activeClicks++ }
        )

        controller.clearRegistration(staleOwner)
        controller.fabAction?.invoke()

        assertEquals("New action", controller.fabContentDescription)
        assertEquals(1, activeClicks)
    }
}

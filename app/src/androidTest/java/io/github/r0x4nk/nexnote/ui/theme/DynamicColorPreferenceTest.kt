package io.github.r0x4nk.nexnote.ui.theme

import androidx.test.platform.app.InstrumentationRegistry
import io.github.r0x4nk.nexnote.data.preferences.UserPreferencesRepository
import io.github.r0x4nk.nexnote.domain.model.AccentColor
import io.github.r0x4nk.nexnote.domain.usecase.ObserveDynamicColorUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SetDynamicColorUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class DynamicColorPreferenceTest {
    @Test
    fun deviceColorsPersistWithoutOverwritingManualAccent() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = UserPreferencesRepository(context)
        val originalAccent = repository.accentColor.first()
        val originalDynamic = repository.dynamicColor.first()
        try {
            repository.setAccentColor(AccentColor.SAGE)
            SetDynamicColorUseCase(repository)(true)
            val reopened = UserPreferencesRepository(context)
            assertEquals(true, ObserveDynamicColorUseCase(reopened)().first())
            assertEquals(AccentColor.SAGE, reopened.accentColor.first())
            SetDynamicColorUseCase(reopened)(false)
            assertEquals(false, repository.dynamicColor.first())
            assertEquals(AccentColor.SAGE, repository.accentColor.first())
        } finally {
            repository.setAccentColor(originalAccent)
            repository.setDynamicColor(originalDynamic)
        }
    }
}

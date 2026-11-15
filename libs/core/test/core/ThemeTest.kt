package dev.justinsmith.bjj.core

import kotlin.test.Test
import kotlin.test.assertEquals

class ThemeTest {
    @Test
    fun `is the default until one is stored`() {
        assertEquals(DEFAULT_THEME_CHOICE, themeChoice(emptyList()))
    }

    @Test
    fun `reads the stored choice`() {
        assertEquals(ThemeChoice.Dark, themeChoice(listOf(Setting(SettingId.Theme, "dark", 1))))
    }

    @Test
    fun `reads a value it does not know as the default`() {
        assertEquals(DEFAULT_THEME_CHOICE, themeChoice(listOf(Setting(SettingId.Theme, "sepia", 1))))
    }

    @Test
    fun `follows the device only when asked to`() {
        assertEquals(Theme.Dark, resolveTheme(ThemeChoice.Device, true))
        assertEquals(Theme.Light, resolveTheme(ThemeChoice.Device, false))
        assertEquals(Theme.Light, resolveTheme(ThemeChoice.Light, true))
        assertEquals(Theme.Dark, resolveTheme(ThemeChoice.Dark, false))
    }
}

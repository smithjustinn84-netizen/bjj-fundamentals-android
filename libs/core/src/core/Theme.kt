package dev.justinsmith.bjj.core

/** What a library that has never chosen gets. */
val DEFAULT_THEME_CHOICE = ThemeChoice.Device

/** The stored theme choice. A missing row, or a value this version does not know, reads as the default. */
fun themeChoice(settings: List<Setting>): ThemeChoice {
    val stored = settings.firstOrNull { it.setting == SettingId.Theme }?.value
    return ThemeChoice.entries.firstOrNull { it.key == stored } ?: DEFAULT_THEME_CHOICE
}

/** The theme to draw: the choice itself, or the device's when the choice is to follow it. */
fun resolveTheme(choice: ThemeChoice, deviceIsDark: Boolean): Theme = when (choice) {
    ThemeChoice.Device -> if (deviceIsDark) Theme.Dark else Theme.Light
    ThemeChoice.Light -> Theme.Light
    ThemeChoice.Dark -> Theme.Dark
}

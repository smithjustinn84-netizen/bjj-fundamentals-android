package dev.justinsmith.bjj.core.services

import dev.justinsmith.bjj.core.CoreEnv
import dev.justinsmith.bjj.core.Setting
import dev.justinsmith.bjj.core.SettingId
import dev.justinsmith.bjj.core.ThemeChoice

class SettingsService(env: CoreEnv) {
    private val storage = env.storage
    private val now = env.now

    suspend fun setTheme(choice: ThemeChoice) = storage.settings.put(Setting(SettingId.Theme, choice.key, now()))
}

package dev.justinsmith.bjj.di

import dev.justinsmith.bjj.bindings.AppData
import dev.justinsmith.bjj.core.Core
import dev.justinsmith.bjj.ui.backup.BackupViewModel
import dev.justinsmith.bjj.ui.common.AppViewModel
import dev.justinsmith.bjj.ui.editor.EditorViewModel
import dev.justinsmith.bjj.ui.log.LogViewModel
import dev.justinsmith.bjj.ui.review.ReviewSessionViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * The composition root. [platform] supplies what only the device has: the `CoreEnv` (the storage engine, ids,
 * the clock, the time zone) and `Links`. Everything else is built here from those.
 */
fun appModules(platform: Module): List<Module> = listOf(platform, appModule)

private val appModule = module {
    single { Core(get()) }
    // Lives as long as the process: the app reads its data for as long as it runs.
    single { AppData(get(), CoroutineScope(SupervisorJob() + Dispatchers.Default)) }
    viewModelOf(::AppViewModel)
    viewModelOf(::ReviewSessionViewModel)
    viewModelOf(::EditorViewModel)
    viewModelOf(::LogViewModel)
    viewModelOf(::BackupViewModel)
}

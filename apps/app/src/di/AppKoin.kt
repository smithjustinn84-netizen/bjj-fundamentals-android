package dev.justinsmith.bjj.di

import dev.justinsmith.bjj.bindings.AppData
import dev.justinsmith.bjj.bindings.Clock
import dev.justinsmith.bjj.core.Core
import dev.justinsmith.bjj.ui.backup.BackupViewModel
import dev.justinsmith.bjj.ui.common.VideoCommands
import dev.justinsmith.bjj.ui.editor.EditorViewModel
import dev.justinsmith.bjj.ui.home.HomeViewModel
import dev.justinsmith.bjj.ui.learn.BlueBeltViewModel
import dev.justinsmith.bjj.ui.learn.ConceptDetailViewModel
import dev.justinsmith.bjj.ui.learn.ConceptsViewModel
import dev.justinsmith.bjj.ui.learn.LessonsViewModel
import dev.justinsmith.bjj.ui.learn.PositionDetailViewModel
import dev.justinsmith.bjj.ui.learn.PositionsViewModel
import dev.justinsmith.bjj.ui.learn.TransitionDetailViewModel
import dev.justinsmith.bjj.ui.log.LogViewModel
import dev.justinsmith.bjj.ui.more.MoreViewModel
import dev.justinsmith.bjj.ui.review.ReviewSessionViewModel
import dev.justinsmith.bjj.ui.review.ReviewViewModel
import dev.justinsmith.bjj.ui.shell.ShellViewModel
import dev.justinsmith.bjj.ui.videos.VideosViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
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
    single { Clock(get()) }
    // Each of the core's services by itself, so a ViewModel asks for the one it uses and no more.
    single { get<Core>().items }
    single { get<Core>().videos }
    single { get<Core>().reviews }
    single { get<Core>().trainingLog }
    single { get<Core>().settings }
    single { get<Core>().backups }
    singleOf(::VideoCommands)
    // One ViewModel per screen. Those of a page about one thing take its NavKey as a parameter.
    viewModelOf(::ShellViewModel)
    viewModelOf(::HomeViewModel)
    viewModelOf(::ConceptsViewModel)
    viewModelOf(::ConceptDetailViewModel)
    viewModelOf(::PositionsViewModel)
    viewModelOf(::PositionDetailViewModel)
    viewModelOf(::TransitionDetailViewModel)
    viewModelOf(::LessonsViewModel)
    viewModelOf(::BlueBeltViewModel)
    viewModelOf(::ReviewViewModel)
    viewModelOf(::ReviewSessionViewModel)
    viewModelOf(::LogViewModel)
    viewModelOf(::MoreViewModel)
    viewModelOf(::VideosViewModel)
    viewModelOf(::BackupViewModel)
    viewModelOf(::EditorViewModel)
}

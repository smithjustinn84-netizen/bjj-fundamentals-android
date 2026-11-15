package dev.justinsmith.bjj.core

import dev.justinsmith.bjj.core.services.BackupService
import dev.justinsmith.bjj.core.services.ItemService
import dev.justinsmith.bjj.core.services.Queries
import dev.justinsmith.bjj.core.services.ReviewService
import dev.justinsmith.bjj.core.services.SettingsService
import dev.justinsmith.bjj.core.services.TrainingLogService
import dev.justinsmith.bjj.core.services.VideoService
import kotlinx.coroutines.flow.Flow

/**
 * Everything the core needs from the outside world. Ids, the clock and the time zone are passed in rather than
 * read from globals, so the core runs anywhere Kotlin does and is predictable in tests.
 */
class CoreEnv(
    val storage: AppStorage,
    /** A new unique id each call. */
    val newId: () -> String,
    /** The current time in epoch milliseconds. */
    val now: () -> Long,
    /** How far local time is from UTC: which calendar day a moment belongs to. */
    val zone: LocalZone,
)

/** The whole application minus its user interface. */
class Core(env: CoreEnv) {
    val items = ItemService(env)
    val videos = VideoService(env)
    val reviews = ReviewService(env)
    val trainingLog = TrainingLogService(env)
    val settings = SettingsService(env)
    val backups = BackupService(env)
    val queries = Queries(env)

    /** Emits after any stored change, so a screen knows when to read again. */
    val changes: Flow<Unit> = env.storage.changes
}

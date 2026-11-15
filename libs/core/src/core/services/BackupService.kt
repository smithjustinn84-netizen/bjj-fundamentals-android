package dev.justinsmith.bjj.core.services

import dev.justinsmith.bjj.core.BACKUP_SCHEMA_VERSION
import dev.justinsmith.bjj.core.BackupData
import dev.justinsmith.bjj.core.BackupFile
import dev.justinsmith.bjj.core.BackupTable
import dev.justinsmith.bjj.core.CoreEnv

/** Everything in, everything out: export, import, counts and erasing. */
class BackupService(env: CoreEnv) {
    private val storage = env.storage
    private val now = env.now

    /** Every table as it stands at one moment. */
    private suspend fun everything(): BackupData = storage.transaction {
        BackupData(
            positions = storage.positions.all(),
            transitions = storage.transitions.all(),
            concepts = storage.concepts.all(),
            videos = storage.videos.all(),
            reviews = storage.reviews.all(),
            reviewLog = storage.reviewLog.all(),
            starterDismissals = storage.starterDismissals.all(),
            trainingLog = storage.trainingLog.all(),
            settings = storage.settings.all(),
        )
    }

    suspend fun exportBackup(): BackupFile = BackupFile(BACKUP_SCHEMA_VERSION, exportedAt = now(), data = everything())

    /** Replaces everything stored with what [file] holds. There is no merge. */
    suspend fun importBackup(file: BackupFile) {
        val data = file.data
        storage.transaction {
            storage.positions.clear()
            storage.positions.putMany(data.positions)
            storage.transitions.clear()
            storage.transitions.putMany(data.transitions)
            storage.concepts.clear()
            storage.concepts.putMany(data.concepts)
            storage.videos.clear()
            storage.videos.putMany(data.videos)
            storage.reviews.clear()
            storage.reviews.putMany(data.reviews)
            storage.reviewLog.clear()
            storage.reviewLog.putMany(data.reviewLog)
            storage.starterDismissals.clear()
            storage.starterDismissals.putMany(data.starterDismissals)
            storage.trainingLog.clear()
            storage.trainingLog.putMany(data.trainingLog)
            storage.settings.clear()
            storage.settings.putMany(data.settings)
        }
    }

    /** How many rows each table holds, for the Backup page. */
    suspend fun tableCounts(): Map<BackupTable, Int> = tableCounts(everything())

    /** Deletes everything and puts the starter content back. */
    suspend fun eraseAll() = storage.reset()
}

/** How many rows each table of [data] holds. */
fun tableCounts(data: BackupData): Map<BackupTable, Int> = BackupTable.entries.associateWith { table ->
    when (table) {
        BackupTable.Positions -> data.positions.size
        BackupTable.Transitions -> data.transitions.size
        BackupTable.Concepts -> data.concepts.size
        BackupTable.Videos -> data.videos.size
        BackupTable.Reviews -> data.reviews.size
        BackupTable.ReviewLog -> data.reviewLog.size
        BackupTable.StarterDismissals -> data.starterDismissals.size
        BackupTable.TrainingLog -> data.trainingLog.size
        BackupTable.Settings -> data.settings.size
    }
}

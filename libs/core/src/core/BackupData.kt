package dev.justinsmith.bjj.core

import kotlinx.serialization.Serializable

/** Everything the app stores, table by table: what a backup holds and what a new installation starts from. */
@Serializable
data class BackupData(
    val positions: List<Position> = emptyList(),
    val transitions: List<Transition> = emptyList(),
    val concepts: List<Concept> = emptyList(),
    val videos: List<VideoBookmark> = emptyList(),
    val reviews: List<ReviewState> = emptyList(),
    val reviewLog: List<ReviewLogEntry> = emptyList(),
    val starterDismissals: List<StarterDismissal> = emptyList(),
    val trainingLog: List<TrainingEntry> = emptyList(),
    val settings: List<Setting> = emptyList(),
)

/** The tables, in the order a backup lists them. [key] is the table's name in the file. */
enum class BackupTable(val key: String) {
    Positions("positions"),
    Transitions("transitions"),
    Concepts("concepts"),
    Videos("videos"),
    Reviews("reviews"),
    ReviewLog("reviewLog"),
    StarterDismissals("starterDismissals"),
    TrainingLog("trainingLog"),
    Settings("settings"),
}

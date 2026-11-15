package dev.justinsmith.bjj.core.services

import dev.justinsmith.bjj.core.BackupTable
import dev.justinsmith.bjj.core.Concept
import dev.justinsmith.bjj.core.CoreEnv
import dev.justinsmith.bjj.core.DAY_MS
import dev.justinsmith.bjj.core.Deck
import dev.justinsmith.bjj.core.ItemKind
import dev.justinsmith.bjj.core.Library
import dev.justinsmith.bjj.core.LocalZone
import dev.justinsmith.bjj.core.ThemeChoice
import dev.justinsmith.bjj.core.TrainingEntry
import dev.justinsmith.bjj.core.Transition
import dev.justinsmith.bjj.core.VideoBookmark
import dev.justinsmith.bjj.core.buildLibrary
import dev.justinsmith.bjj.core.buildQueue
import dev.justinsmith.bjj.core.findMissingStarterConcepts
import dev.justinsmith.bjj.core.findMissingStarterTechniques
import dev.justinsmith.bjj.core.linkedKind
import dev.justinsmith.bjj.core.newestFirst
import dev.justinsmith.bjj.core.seed.starterContent
import dev.justinsmith.bjj.core.streakDays
import dev.justinsmith.bjj.core.themeChoice

/** How far back to look when computing the streak. Streaks longer than this are capped. */
private const val STREAK_WINDOW_DAYS = 400

data class StudyStats(
    val dueCount: Int,
    /** Days in a row with a review or a logged class. */
    val streakDays: Int,
)

/**
 * Everything a screen shows, read at one moment so its parts agree with each other: the app reads one of these
 * after every stored change and every screen draws from it.
 */
data class Snapshot(
    val library: Library,
    /** Newest first. */
    val videos: List<VideoBookmark>,
    /** Most recent class first. */
    val trainingLog: List<TrainingEntry>,
    val themeChoice: ThemeChoice,
    /** Starter concepts and techniques to offer: ones the library does not hold and has not dismissed. */
    val missingStarterConcepts: List<Concept>,
    val missingStarterTechniques: List<Transition>,
    /** When each review of the last [STREAK_WINDOW_DAYS] days happened, for the streak. */
    val recentReviews: List<Long>,
    val tableCounts: Map<BackupTable, Int>,
)

/** How many cards are due and how long the streak is, from a snapshot, at [now]. */
fun studyStats(snapshot: Snapshot, now: Long, zone: LocalZone): StudyStats = StudyStats(
    dueCount = buildQueue(snapshot.library, Deck.All, 0, now).due.size,
    streakDays = streakDays(snapshot.recentReviews, now, zone, snapshot.trainingLog.map { it.day }),
)

/** Read-only views of the stored data, shaped for display. */
class Queries(env: CoreEnv) {
    private val storage = env.storage
    private val now = env.now
    private val zone = env.zone

    /** One consistent read of everything. */
    suspend fun snapshot(): Snapshot {
        val timestamp = now()
        return storage.transaction {
            val concepts = storage.concepts.all()
            val positions = storage.positions.all()
            val transitions = storage.transitions.all()
            val reviews = storage.reviews.all()
            val videos = storage.videos.all()
            val entries = storage.trainingLog.all()
            val dismissed = storage.starterDismissals.all()
            val settings = storage.settings.all()
            val starter = starterContent(timestamp)
            Snapshot(
                library = buildLibrary(concepts, positions, transitions, reviews),
                videos = videos.sortedByDescending { it.createdAt },
                trainingLog = newestFirst(entries),
                themeChoice = themeChoice(settings),
                missingStarterConcepts = findMissingStarterConcepts(starter.concepts, concepts, dismissed),
                missingStarterTechniques =
                findMissingStarterTechniques(starter.transitions, transitions, positions, dismissed),
                recentReviews = storage.reviewLog.since(timestamp - STREAK_WINDOW_DAYS * DAY_MS).map { it.reviewedAt },
                tableCounts = mapOf(
                    BackupTable.Positions to positions.size,
                    BackupTable.Transitions to transitions.size,
                    BackupTable.Concepts to concepts.size,
                    BackupTable.Videos to videos.size,
                    BackupTable.Reviews to reviews.size,
                    BackupTable.ReviewLog to storage.reviewLog.count(),
                    BackupTable.StarterDismissals to dismissed.size,
                    BackupTable.TrainingLog to entries.size,
                    BackupTable.Settings to settings.size,
                ),
            )
        }
    }

    suspend fun library(): Library =
        buildLibrary(storage.concepts.all(), storage.positions.all(), storage.transitions.all(), storage.reviews.all())

    /**
     * Counted from the queue a review would show, not from the stored cards: a situation card whose side has
     * lost its last technique stays stored but can no longer be studied, and must not be counted as due.
     */
    suspend fun dueCount(): Int = buildQueue(library(), Deck.All, 0, now()).due.size

    suspend fun studyStats(): StudyStats {
        val timestamp = now()
        val recent = storage.reviewLog.since(timestamp - STREAK_WINDOW_DAYS * DAY_MS)
        val classes = storage.trainingLog.all()
        return StudyStats(
            dueCount = dueCount(),
            streakDays = streakDays(recent.map { it.reviewedAt }, timestamp, zone, classes.map { it.day }),
        )
    }

    /** Starter techniques to offer this library: ones it could hold but does not, and has not dismissed. */
    suspend fun missingStarterTechniques(): List<Transition> = findMissingStarterTechniques(
        starterContent(now()).transitions,
        storage.transitions.all(),
        storage.positions.all(),
        storage.starterDismissals.all(),
    )

    /** Starter concepts to offer this library: ones it does not hold and has not dismissed. */
    suspend fun missingStarterConcepts(): List<Concept> =
        findMissingStarterConcepts(
            starterContent(now()).concepts,
            storage.concepts.all(),
            storage.starterDismissals.all(),
        )

    /** Newest first. */
    suspend fun videos(): List<VideoBookmark> = storage.videos.all().sortedByDescending { it.createdAt }

    /** Every class written up, most recent day first. */
    suspend fun trainingLog(): List<TrainingEntry> = newestFirst(storage.trainingLog.all())

    /** The colours the user asked for; the device's own setting until they choose. */
    suspend fun themeChoice(): ThemeChoice = themeChoice(storage.settings.all())

    /** Oldest first, for one item. */
    suspend fun linkedVideos(kind: ItemKind, id: String): List<VideoBookmark> =
        linkedVideos(storage.videos.all(), kind, id)
}

/** The bookmarks about one item, oldest first. */
fun linkedVideos(videos: List<VideoBookmark>, kind: ItemKind, id: String): List<VideoBookmark> =
    videos.filter { it.linkedKind == kind.linkedKind && it.linkedId == id }.sortedBy { it.createdAt }

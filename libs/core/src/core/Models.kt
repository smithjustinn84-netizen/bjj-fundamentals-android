package dev.justinsmith.bjj.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Every stored thing, as it is kept and as a backup file holds it: the same shapes and names as the web app's
// `core/models.ts`, so one backup opens in both. All timestamps are epoch milliseconds. A field marked
// "older rows have none" is null when absent and is left out of the file, never written as null.

/** Anything a table holds by id. */
interface HasId {
    val id: String
}

@Serializable
enum class PositionCategory(val key: String) {
    @SerialName("standing")
    Standing("standing"),

    @SerialName("guard")
    Guard("guard"),

    @SerialName("top")
    Top("top"),

    @SerialName("back")
    Back("back"),

    @SerialName("scramble")
    Scramble("scramble"),
}

/** What a new position starts as: most of what beginners add is a guard. */
val DEFAULT_POSITION_CATEGORY = PositionCategory.Guard

/** A pin is a position where the person underneath is held down and has to get out before anything else. */
fun isPin(category: PositionCategory): Boolean = when (category) {
    PositionCategory.Top, PositionCategory.Back -> true
    PositionCategory.Standing, PositionCategory.Guard, PositionCategory.Scramble -> false
}

@Serializable
data class Position(
    override val id: String,
    val name: String,
    val category: PositionCategory,
    /** One-line plain-language summary. */
    val summary: String,
    /** Short goals for the person on top / with the advantage. */
    val topGoals: List<String>,
    /** Short goals for the person on the bottom / defending. */
    val bottomGoals: List<String>,
    /** Ids of concepts that matter most here. */
    val conceptIds: List<String>,
    /** Suggested learning order (lower = earlier). */
    val order: Int,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long,
) : HasId

/**
 * Which of the two people in a position someone is. "Top" is the person with the advantage and "bottom" the
 * one defending, as in `topGoals` and `bottomGoals`; the labels name them per kind of position.
 */
@Serializable
enum class PositionRole(val key: String) {
    @SerialName("top")
    Top("top"),

    @SerialName("bottom")
    Bottom("bottom"),
}

@Serializable
enum class TransitionType(val key: String) {
    @SerialName("sweep")
    Sweep("sweep"),

    @SerialName("pass")
    Pass("pass"),

    @SerialName("escape")
    Escape("escape"),

    @SerialName("submission")
    Submission("submission"),

    @SerialName("takedown")
    Takedown("takedown"),

    @SerialName("transition")
    Transition("transition"),
}

val DEFAULT_TRANSITION_TYPE = TransitionType.Sweep

/** A submission ends the roll: it has no destination position, so its `toId` is empty. */
fun endsTheRoll(type: TransitionType): Boolean = type == TransitionType.Submission

/** An escape gets its performer out of trouble. It is the first thing to learn from any position. */
fun isEscape(type: TransitionType): Boolean = type == TransitionType.Escape

/** A technique: a way from one position to another, or to the tap. */
@Serializable
data class Transition(
    override val id: String,
    val name: String,
    val type: TransitionType,
    val fromId: String,
    /** Destination position id. Empty for a type that [endsTheRoll]. */
    val toId: String,
    /** Who does it, as their role in the starting position. Read it through `performerRole`: older rows have none. */
    val role: PositionRole? = null,
    /**
     * Which side of the destination position the person who did the technique ends up on. Read it through
     * `endRole`: older rows have none, and one that [endsTheRoll] never has.
     */
    val endRole: PositionRole? = null,
    val steps: List<String>,
    val commonMistakes: List<String>,
    /** A short solo or partner drill. Read it through `techniqueDrill`: older rows have none. */
    val drill: String? = null,
    /** Whether it is on the blue belt test list. Read it through `isOnBlueBeltTest`: older rows have none. */
    val blueBeltTest: Boolean? = null,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long,
) : HasId

@Serializable
data class Concept(
    override val id: String,
    val name: String,
    val summary: String,
    val keyPoints: List<String>,
    /** A tiny solo or partner drill to practise the idea. */
    val drill: String,
    val order: Int,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long,
) : HasId

/** One class or open mat, written up afterwards. */
@Serializable
data class TrainingEntry(
    override val id: String,
    /** The day of the class as a local calendar day, `YYYY-MM-DD` (see `isDayKey`): a day, not an instant. */
    val day: String,
    /** Whole minutes on the mat. */
    val minutes: Int,
    /** What was taught or drilled. */
    val taught: String,
    /** What worked in rolling. */
    val worked: String,
    /** What went wrong in rolling. */
    val struggled: String,
    /** The one thing to work on next time. */
    val focus: String,
    /** Techniques drilled or used, by id. */
    val techniqueIds: List<String>,
    /**
     * Situations (see `situationId`) the student could not get out of or make progress from. Read it through
     * `stuckSituationIds`: older rows have none.
     */
    val stuckIds: List<String>? = null,
    val createdAt: Long,
    val updatedAt: Long,
) : HasId

/** The kinds of thing with a page, a form and a row of their own. */
@Serializable
enum class ItemKind(val key: String) {
    @SerialName("concept")
    Concept("concept"),

    @SerialName("position")
    Position("position"),

    @SerialName("transition")
    Transition("transition"),
}

/**
 * What a study card can be about: one item, or a situation (one side of one position, see Situations.kt).
 * A situation is not an item: it has no page, form or row of its own, only a card.
 */
@Serializable
enum class CardKind(val key: String) {
    @SerialName("concept")
    Concept("concept"),

    @SerialName("position")
    Position("position"),

    @SerialName("transition")
    Transition("transition"),

    @SerialName("situation")
    Situation("situation"),
}

val ItemKind.cardKind: CardKind
    get() = when (this) {
        ItemKind.Concept -> CardKind.Concept
        ItemKind.Position -> CardKind.Position
        ItemKind.Transition -> CardKind.Transition
    }

/** What a review session can draw from: everything, one kind of card, or the techniques on the blue belt test. */
enum class Deck(val key: String) {
    All("all"),
    Concept("concept"),
    Position("position"),
    Transition("transition"),
    Situation("situation"),
    BlueBelt("blueBelt"),
}

/** What a video is about: a kind of item, or nothing (kept in a backup as the empty text). */
@Serializable
enum class LinkedKind(val itemKind: ItemKind?) {
    @SerialName("")
    None(null),

    @SerialName("concept")
    Concept(ItemKind.Concept),

    @SerialName("position")
    Position(ItemKind.Position),

    @SerialName("transition")
    Transition(ItemKind.Transition),
}

val ItemKind.linkedKind: LinkedKind
    get() = when (this) {
        ItemKind.Concept -> LinkedKind.Concept
        ItemKind.Position -> LinkedKind.Position
        ItemKind.Transition -> LinkedKind.Transition
    }

@Serializable
data class VideoBookmark(
    override val id: String,
    val title: String,
    val youtubeId: String,
    val startSeconds: Int,
    /** What this video is about. An empty [linkedId] means unlinked. */
    val linkedKind: LinkedKind,
    val linkedId: String,
    val note: String,
    val createdAt: Long,
) : HasId

/** Spaced-repetition state for one study card. Its id is `kind:itemId` (see `cardId`). */
@Serializable
data class ReviewState(
    override val id: String,
    val kind: CardKind,
    val itemId: String,
    /** A fraction that builds up by small steps, so it is kept exactly as the web app keeps it. */
    val ease: Double,
    val intervalDays: Int,
    val repetitions: Int,
    val lapses: Int,
    val due: Long,
    val lastReviewed: Long,
) : HasId

@Serializable
enum class Grade(val key: String) {
    @SerialName("again")
    Again("again"),

    @SerialName("hard")
    Hard("hard"),

    @SerialName("good")
    Good("good"),

    @SerialName("easy")
    Easy("easy"),
}

/** One review as it happened. The id is the storage engine's; an entry not yet stored has none. */
@Serializable
data class ReviewLogEntry(
    val id: Long? = null,
    val cardId: String,
    val grade: Grade,
    val reviewedAt: Long,
)

/**
 * A starter concept or technique the user chose not to be offered again ("Dismiss" on the Concepts or Blue
 * belt page). The id is the starter item's; starter ids are unique across kinds.
 */
@Serializable
data class StarterDismissal(
    override val id: String,
    val dismissedAt: Long,
) : HasId

/** Which colours the app uses: the device's own light or dark setting, or one of them regardless. */
enum class ThemeChoice(val key: String) {
    Device("device"),
    Light("light"),
    Dark("dark"),
}

/** The two themes there are. A [ThemeChoice] always comes down to one of these (Theme.kt). */
enum class Theme { Light, Dark }

/** The preferences the app remembers. Each is one row of the settings table, keyed by its name. */
@Serializable
enum class SettingId(val key: String) {
    @SerialName("theme")
    Theme("theme"),
}

/**
 * One remembered preference. The value is text so every setting fits one table; read it through the
 * setting's own function (`themeChoice`), which also supplies the default when the row is absent.
 */
@Serializable
data class Setting(
    /** Which preference this is. Kept in a backup as the row's `id`. */
    @SerialName("id") val setting: SettingId,
    val value: String,
    val updatedAt: Long,
) : HasId {
    override val id: String
        get() = setting.key
}

/** Study ranks shown as a belt, lowest first. A motivational label for progress, not a real grading. */
enum class BeltRank { White, Blue, Purple, Brown, Black }

/** Which way the position map flows: across the screen on wide windows, down it on narrow ones. */
enum class MapOrientation { Horizontal, Vertical }

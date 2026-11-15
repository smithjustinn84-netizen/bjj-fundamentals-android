package dev.justinsmith.bjj.core

// The names core's own logic uses (search, card prompts, a situation's name), as the web app has them.
// A screen's own wording is in its string resources.

val PositionCategory.label: String
    get() = when (this) {
        PositionCategory.Standing -> "Standing"
        PositionCategory.Guard -> "Guards"
        PositionCategory.Top -> "Top positions"
        PositionCategory.Back -> "Back"
        PositionCategory.Scramble -> "Scrambles"
    }

/** What a role is called in one kind of position: who "top" and "bottom" mean differs by position. */
fun roleLabel(category: PositionCategory, role: PositionRole): String = when (category) {
    PositionCategory.Standing, PositionCategory.Scramble -> role.pick("Attacking", "Defending")
    PositionCategory.Guard -> role.pick("Top (passing)", "Bottom (guard player)")
    PositionCategory.Top -> role.pick("Top", "Bottom")
    PositionCategory.Back -> role.pick("Taking the back", "Defending")
}

/** How to say which side of a position someone is on, after its name: "Mount, on top". */
fun endSideLabel(category: PositionCategory, role: PositionRole): String = when (category) {
    PositionCategory.Standing -> role.pick("attacking", "defending")
    PositionCategory.Guard -> role.pick("passing", "playing guard")
    PositionCategory.Top, PositionCategory.Scramble -> role.pick("on top", "underneath")
    PositionCategory.Back -> role.pick("on their back", "defending your back")
}

private fun PositionRole.pick(top: String, bottom: String): String = when (this) {
    PositionRole.Top -> top
    PositionRole.Bottom -> bottom
}

val TransitionType.label: String
    get() = when (this) {
        TransitionType.Sweep -> "Sweep"
        TransitionType.Pass -> "Guard pass"
        TransitionType.Escape -> "Escape"
        TransitionType.Submission -> "Submission"
        TransitionType.Takedown -> "Takedown"
        TransitionType.Transition -> "Transition"
    }

/** The heading for a list of techniques of one type. */
val TransitionType.groupLabel: String
    get() = when (this) {
        TransitionType.Sweep -> "Sweeps"
        TransitionType.Pass -> "Guard passes"
        TransitionType.Escape -> "Escapes"
        TransitionType.Submission -> "Submissions"
        TransitionType.Takedown -> "Takedowns"
        TransitionType.Transition -> "Transitions"
    }

val ItemKind.label: String
    get() = when (this) {
        ItemKind.Concept -> "Concept"
        ItemKind.Position -> "Position"
        ItemKind.Transition -> "Technique"
    }

/** The heading for a list of items of one kind. */
val ItemKind.groupLabel: String
    get() = when (this) {
        ItemKind.Concept -> "Concepts"
        ItemKind.Position -> "Positions"
        ItemKind.Transition -> "Techniques"
    }

val Grade.label: String
    get() = when (this) {
        Grade.Again -> "Again"
        Grade.Hard -> "Hard"
        Grade.Good -> "Good"
        Grade.Easy -> "Easy"
    }

val BackupTable.label: String
    get() = when (this) {
        BackupTable.Concepts -> "Concepts"
        BackupTable.Positions -> "Positions"
        BackupTable.Transitions -> "Techniques"
        BackupTable.Videos -> "Video bookmarks"
        BackupTable.Reviews -> "Cards studied"
        BackupTable.ReviewLog -> "Review history"
        BackupTable.StarterDismissals -> "Dismissed starter techniques"
        BackupTable.TrainingLog -> "Training log entries"
        BackupTable.Settings -> "Settings"
    }

val ThemeChoice.label: String
    get() = when (this) {
        ThemeChoice.Device -> "Device"
        ThemeChoice.Light -> "Light"
        ThemeChoice.Dark -> "Dark"
    }

val BeltRank.label: String
    get() = when (this) {
        BeltRank.White -> "White belt"
        BeltRank.Blue -> "Blue belt"
        BeltRank.Purple -> "Purple belt"
        BeltRank.Brown -> "Brown belt"
        BeltRank.Black -> "Black belt"
    }

/** "1 card", "2 cards". Only for nouns that take a plain "s". */
fun plural(count: Int, noun: String): String = "$count $noun${if (count == 1) "" else "s"}"

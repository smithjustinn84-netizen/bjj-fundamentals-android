package dev.justinsmith.bjj.design.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import dev.justinsmith.bjj.core.Grade
import dev.justinsmith.bjj.core.TransitionType

val LocalBjjPalette = staticCompositionLocalOf { LightPalette }

/**
 * The colour text and icons take where they are: ink on paper, the on-colour inside a painted button or tag.
 * A component that paints its own ground provides it for what it holds.
 */
val LocalContentColor = compositionLocalOf { Color.Unspecified }

/** The app's theme: [dark] is already resolved from the user's choice and the device. */
@Composable
fun BjjTheme(dark: Boolean, content: @Composable () -> Unit) {
    val palette = if (dark) DarkPalette else LightPalette
    CompositionLocalProvider(
        LocalBjjPalette provides palette,
        LocalContentColor provides palette.text,
        content = content,
    )
}

object BjjTheme {
    val palette: BjjPalette
        @Composable @ReadOnlyComposable
        get() = LocalBjjPalette.current
}

/** One colour per technique type, shared by tags, map arrows and the map's legend. */
fun BjjPalette.typeColor(type: TransitionType): Color = when (type) {
    TransitionType.Sweep -> warn
    TransitionType.Pass -> accent
    TransitionType.Escape -> ok
    TransitionType.Submission -> typeSubmission
    TransitionType.Takedown -> typeTakedown
    TransitionType.Transition -> typeTransition
}

/** A grade button's ground. */
fun BjjPalette.gradeColor(grade: Grade): Color = when (grade) {
    Grade.Again -> belt
    Grade.Hard -> cta
    Grade.Good -> ok
    Grade.Easy -> fill
}

/** The text on a grade button's ground. */
fun BjjPalette.onGradeColor(grade: Grade): Color = when (grade) {
    Grade.Again -> onBelt
    Grade.Hard -> onCta
    Grade.Good -> onStatus
    Grade.Easy -> onFill
}

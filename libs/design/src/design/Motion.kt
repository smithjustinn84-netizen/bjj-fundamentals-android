package dev.justinsmith.bjj.design

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Composable

/** True when the system has animations switched off: anything that moves appears already in place instead. */
@Composable
expect fun reducedMotion(): Boolean

/** The web app's easing curves (`--ease-*` in `tokens.css`). */
object Easings {
    /** Quick away, slow to settle: things arriving. */
    val out: Easing = CubicBezierEasing(a = 0.2f, b = 0.8f, c = 0.2f, d = 1f)

    /** Overshoots a little before settling: things popping in. */
    val spring: Easing = CubicBezierEasing(a = 0.34f, b = 1.56f, c = 0.64f, d = 1f)
}

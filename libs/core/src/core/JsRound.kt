package dev.justinsmith.bjj.core

import kotlin.math.floor

/**
 * Rounds the way the web app's `Math.round` does: a half goes up, towards positive infinity, whatever the sign.
 * Every rounding in core goes through here so a schedule, a percentage or a drawing's coordinate comes out the
 * same in both apps. Never `kotlin.math.round`, which sends a half to the even neighbour (2.5 to 2).
 */
fun jsRound(value: Double): Long = floor(value + HALF).toLong()

/** [jsRound] for values known to fit an Int: days, percentages. */
fun jsRoundToInt(value: Double): Int = jsRound(value).toInt()

private const val HALF = 0.5

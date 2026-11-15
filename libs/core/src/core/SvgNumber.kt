package dev.justinsmith.bjj.core

/** One decimal place: precise enough for a drawing's coordinate, and what the web app's drawings are rounded to. */
private const val SCALE = 10.0

/** Rounds a coordinate or length to one decimal place. */
fun roundSvg(value: Double): Double = jsRound(value * SCALE) / SCALE

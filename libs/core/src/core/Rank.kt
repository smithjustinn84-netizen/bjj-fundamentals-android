package dev.justinsmith.bjj.core

import kotlin.math.ceil

/** Stripes earned on a belt before moving up to the next one. */
const val MAX_STRIPES = 4

/** The black belt is reserved for having learned everything. */
private const val BLACK_BELT_PERCENT = 100.0

/** The belts below black share the range under 100% equally. */
private val PERCENT_PER_BELT = BLACK_BELT_PERCENT / (BeltRank.entries.size - 1)

/** Each belt is split into a plain belt plus one step per stripe. */
private val PERCENT_PER_STRIPE = PERCENT_PER_BELT / (MAX_STRIPES + 1)

data class BeltProgress(
    val belt: BeltRank,
    /** 0 to [MAX_STRIPES]. */
    val stripes: Int,
    /** The belt after this one, or null at black. */
    val nextBelt: BeltRank?,
    /** Whole percentage points of learning still needed for [nextBelt]; 0 at black. */
    val percentToNextBelt: Int,
)

/**
 * Turns "percent of cards learned" into a study rank: the belts below black split the range evenly, each belt
 * splits again into stripes, and black comes only at 100%. Out-of-range input is clamped.
 */
fun beltRank(percentLearned: Int): BeltProgress {
    val percent = percentLearned.coerceIn(0, BLACK_BELT_PERCENT.toInt()).toDouble()
    val beltIndex = (percent / PERCENT_PER_BELT).toInt()
    val belt = BeltRank.entries[beltIndex]
    val nextBelt = BeltRank.entries.getOrNull(beltIndex + 1)
        ?: return BeltProgress(belt, stripes = 0, nextBelt = null, percentToNextBelt = 0)
    val intoBelt = percent - beltIndex * PERCENT_PER_BELT
    return BeltProgress(
        belt = belt,
        stripes = (intoBelt / PERCENT_PER_STRIPE).toInt(),
        nextBelt = nextBelt,
        percentToNextBelt = ceil(PERCENT_PER_BELT - intoBelt).toInt(),
    )
}

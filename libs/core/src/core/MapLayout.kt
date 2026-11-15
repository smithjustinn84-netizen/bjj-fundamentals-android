package dev.justinsmith.bjj.core

import kotlin.math.abs
import kotlin.math.hypot

// Geometry for the position map: where each position sits and how each technique's arrow curves. Pure numbers,
// so the layout can be tested without drawing anything.
//
// Positions are grouped into lanes by category. Lanes run in "dominance" order, from standing through guard
// and scrambles to top control and the back, so most techniques point forwards.

/** Lane order along the map's main axis, lowest first. */
private val PositionCategory.laneRank: Int
    get() = when (this) {
        PositionCategory.Standing -> 0
        PositionCategory.Guard -> 1
        PositionCategory.Scramble -> 2
        PositionCategory.Top -> 3
        PositionCategory.Back -> 4
    }

private class MapGeometry(
    val nodeWidth: Double,
    val nodeHeight: Double,
    /** Space between neighbouring lanes, where most arrows travel. */
    val laneGap: Double,
    /** Space between positions in the same lane. */
    val nodeGap: Double,
    /** Margin around the whole map, leaving room for arrows that bow outwards. */
    val padding: Double,
    /** Room kept before each lane's positions for the lane's heading. */
    val headingBand: Double,
)

/** Lanes are laid out at least this many positions wide, so arrows have room to pass around a lone position. */
private const val MIN_LANE_SLOTS = 3

/** Sizes in the map's own units. The vertical map is narrower so it fits a phone without shrinking the text. */
private val MapOrientation.geometry: MapGeometry
    get() = when (this) {
        MapOrientation.Horizontal -> MapGeometry(
            nodeWidth = 132.0,
            nodeHeight = 58.0,
            laneGap = 68.0,
            nodeGap = 34.0,
            padding = 32.0,
            headingBand = 26.0,
        )

        MapOrientation.Vertical -> MapGeometry(
            nodeWidth = 108.0,
            nodeHeight = 60.0,
            laneGap = 52.0,
            nodeGap = 10.0,
            padding = 14.0,
            headingBand = 22.0,
        )
    }

/** The smallest distance an arrow's midpoint bows away from the straight line between two positions. */
private const val EDGE_BOW = 16.0

/** The largest bow tried, as a multiple of [EDGE_BOW]. */
private const val MAX_BOW_STEPS = 8

/**
 * Bows to try for each arrow, gentlest first and alternating sides, in half steps of [EDGE_BOW]. The one that
 * crosses the least of other positions, and is not already taken by a parallel arrow, wins.
 */
private val BOW_CANDIDATES: List<Double> =
    (0 until MAX_BOW_STEPS * 2 - 1).map { (1 + it / 2.0) * EDGE_BOW }.flatMap { listOf(it, -it) }

/** How close an arrow may pass to a position it does not belong to. */
private const val EDGE_CLEARANCE = 6.0

/** Points checked along each candidate curve when looking for collisions. */
private const val CURVE_SAMPLES = 24

/** Gap between a position's box and the ends of its arrows. */
private const val EDGE_GAP = 5.0

data class MapNode(
    val id: String,
    val category: PositionCategory,
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double,
    /** Submissions available from here. They end the match, so they are counted rather than drawn as arrows. */
    val submissions: Int,
)

data class MapEdge(
    /** The transition's id. */
    val id: String,
    val fromId: String,
    val toId: String,
    val type: TransitionType,
    /** How far the arrow's midpoint bows to the right of its direction of travel; negative bows left. */
    val bow: Double,
    /** From the edge of one position's box to the edge of the other's. */
    val curve: Quad,
)

data class MapLane(
    val category: PositionCategory,
    /**
     * Top of the lane's heading. In a horizontal map x is the centre of the lane's column; in a vertical map
     * it is the left edge of the lane's row.
     */
    val labelX: Double,
    val labelY: Double,
)

data class MapLayout(
    val width: Double,
    val height: Double,
    val lanes: List<MapLane>,
    val nodes: List<MapNode>,
    val edges: List<MapEdge>,
)

private val MapNode.centre: Point
    get() = Point(x + width / 2, y + height / 2)

/** The point where a line from the node's centre towards [target] leaves the node's box (plus a small gap). */
private fun exitPoint(node: MapNode, target: Point): Point {
    val centre = node.centre
    val dx = target.x - centre.x
    val dy = target.y - centre.y
    val halfWidth = node.width / 2 + EDGE_GAP
    val halfHeight = node.height / 2 + EDGE_GAP
    val scale = minOf(
        if (dx == 0.0) Double.POSITIVE_INFINITY else halfWidth / abs(dx),
        if (dy == 0.0) Double.POSITIVE_INFINITY else halfHeight / abs(dy),
    )
    // Both zero only when the target is the centre itself, which callers rule out.
    return if (scale.isFinite()) Point(centre.x + dx * scale, centre.y + dy * scale) else centre
}

/** A curve between the edges of two boxes whose midpoint sits [bow] to the right of the straight line. */
private fun curveBetween(from: MapNode, to: MapNode, bow: Double): Quad {
    val a = from.centre
    val b = to.centre
    val length = hypot(b.x - a.x, b.y - a.y)
    // Unit vector pointing to the right of the direction of travel.
    val right = Point(-(b.y - a.y) / length, (b.x - a.x) / length)
    // A quadratic curve reaches half of its control point's offset, hence the factor of two.
    val control = Point((a.x + b.x) / 2 + right.x * bow * 2, (a.y + b.y) / 2 + right.y * bow * 2)
    return Quad(exitPoint(from, control), control, exitPoint(to, control))
}

private fun pointOn(curve: Quad, t: Double): Point {
    val u = 1 - t
    return Point(
        u * u * curve.start.x + 2 * u * t * curve.control.x + t * t * curve.end.x,
        u * u * curve.start.y + 2 * u * t * curve.control.y + t * t * curve.end.y,
    )
}

/** How many sample points along the curve leave the map or come too close to a box in [obstacles]. 0 is a clean run. */
private fun collisions(curve: Quad, obstacles: List<MapNode>, width: Double, height: Double): Int =
    (0..CURVE_SAMPLES).count { sample ->
        val (x, y) = pointOn(curve, sample.toDouble() / CURVE_SAMPLES)
        val outside = x < 0 || y < 0 || x > width || y > height
        outside || obstacles.any { box ->
            x > box.x - EDGE_CLEARANCE &&
                x < box.x + box.width + EDGE_CLEARANCE &&
                y > box.y - EDGE_CLEARANCE &&
                y < box.y + box.height + EDGE_CLEARANCE
        }
    }

private fun Quad.rounded() = Quad(
    Point(roundSvg(start.x), roundSvg(start.y)),
    Point(roundSvg(control.x), roundSvg(control.y)),
    Point(roundSvg(end.x), roundSvg(end.y)),
)

/**
 * Lays out the map. [positions] should already be in display order (as in [Library.positions]); that order is
 * kept within each lane.
 *
 * Not drawn: submissions (counted on their starting position), techniques whose start or end position does not
 * exist, and techniques that start and end in the same position.
 */
fun layoutPositionMap(
    positions: List<Position>,
    transitions: List<Transition>,
    orientation: MapOrientation,
): MapLayout {
    val g = orientation.geometry
    val horizontal = orientation == MapOrientation.Horizontal
    // Along the main axis lanes follow one another; along the cross axis a lane's positions sit side by side.
    val mainSize = if (horizontal) g.nodeWidth else g.nodeHeight
    val crossSize = if (horizontal) g.nodeHeight else g.nodeWidth
    // Headings sit above the positions: one shared band over the columns, or a band at the top of every row.
    val mainHeading = if (horizontal) 0.0 else g.headingBand
    val crossHeading = if (horizontal) g.headingBand else 0.0

    val categories = positions.map { it.category }.distinct().sortedBy { it.laneRank }
    val laneMembers = categories.map { category -> positions.filter { it.category == category } }
    val longestLane = maxOf(MIN_LANE_SLOTS, laneMembers.maxOfOrNull { it.size } ?: 0)
    val crossExtent = crossHeading + longestLane * crossSize + maxOf(0, longestLane - 1) * g.nodeGap
    val mainExtent = categories.size * (mainHeading + mainSize) + maxOf(0, categories.size - 1) * g.laneGap

    val submissionsFrom = transitions.filter { endsTheRoll(it.type) }.groupingBy { it.fromId }.eachCount()

    val lanes = mutableListOf<MapLane>()
    val nodes = mutableListOf<MapNode>()
    laneMembers.forEachIndexed { laneIndex, members ->
        val category = categories[laneIndex]
        val laneStart = g.padding + laneIndex * (mainHeading + mainSize + g.laneGap)
        val main = laneStart + mainHeading
        val laneCross = members.size * crossSize + (members.size - 1) * g.nodeGap
        // Shorter lanes are centred against the longest one.
        val crossStart = g.padding + crossHeading + (crossExtent - crossHeading - laneCross) / 2
        lanes += MapLane(
            category = category,
            labelX = roundSvg(if (horizontal) main + mainSize / 2 else g.padding),
            labelY = roundSvg(if (horizontal) g.padding else laneStart),
        )
        members.forEachIndexed { index, position ->
            val cross = crossStart + index * (crossSize + g.nodeGap)
            nodes += MapNode(
                id = position.id,
                category = category,
                x = roundSvg(if (horizontal) main else cross),
                y = roundSvg(if (horizontal) cross else main),
                width = g.nodeWidth,
                height = g.nodeHeight,
                submissions = submissionsFrom[position.id] ?: 0,
            )
        }
    }

    val width = roundSvg((if (horizontal) mainExtent else crossExtent) + g.padding * 2)
    val height = roundSvg((if (horizontal) crossExtent else mainExtent) + g.padding * 2)
    return MapLayout(width, height, lanes, nodes, layoutEdges(nodes, transitions, width, height))
}

private fun layoutEdges(
    nodes: List<MapNode>,
    transitions: List<Transition>,
    width: Double,
    height: Double,
): List<MapEdge> {
    val nodeById = nodes.associateBy { it.id }
    // Bows already used between each pair of positions, measured in one shared direction so that an arrow and
    // its return trip cannot land on the same curve.
    val takenBows = HashMap<Pair<String, String>, MutableSet<Double>>()
    return transitions.filter { !endsTheRoll(it.type) && it.fromId != it.toId }.mapNotNull { t ->
        val from = nodeById[t.fromId]
        val to = nodeById[t.toId]
        if (from == null || to == null) return@mapNotNull null
        val forwards = t.fromId < t.toId
        val shared = { bow: Double -> if (forwards) bow else -bow }
        val taken = takenBows.getOrPut(if (forwards) t.fromId to t.toId else t.toId to t.fromId) { HashSet() }
        val obstacles = nodes.filter { it !== from && it !== to }
        val bow = cleanestBow(BOW_CANDIDATES.filter { shared(it) !in taken }) { candidate ->
            collisions(curveBetween(from, to, candidate), obstacles, width, height)
        }
        taken += shared(bow)
        MapEdge(t.id, t.fromId, t.toId, t.type, bow, curveBetween(from, to, bow).rounded())
    }
}

/**
 * The first of [candidates] with no collisions, or failing that the first with the fewest. They come gentlest
 * first, so among equally clean curves the straightest wins.
 */
private fun cleanestBow(candidates: List<Double>, collisions: (Double) -> Int): Double {
    var best = EDGE_BOW
    var fewest = Int.MAX_VALUE
    for (candidate in candidates) {
        val hits = collisions(candidate)
        if (hits < fewest) {
            best = candidate
            fewest = hits
        }
        if (hits == 0) break
    }
    return best
}

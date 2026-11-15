package dev.justinsmith.bjj.core

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

// Geometry for the jointed figures in the diagrams. A pose is a handful of joint positions; this turns it into
// the curves, lines and points a composable can draw without doing any maths itself. Coordinates are a
// diagram's own units with y pointing down, rounded as the web app's drawings are.

data class Point(val x: Double, val y: Double) {
    constructor(x: Int, y: Int) : this(x.toDouble(), y.toDouble())
}

private fun Point.rounded() = Point(roundSvg(x), roundSvg(y))

/** A curve from [start] to [end] pulled towards [control]. */
data class Quad(val start: Point, val control: Point, val end: Point)

data class Segment(val from: Point, val to: Point)

/** A two-segment limb, given by its middle joint and its end: elbow then hand, or knee then foot. */
data class Limb(val joint: Point, val end: Point)

/** The far limb is drawn behind the body, the near limb in front of it. */
data class LimbPair(val far: Limb, val near: Limb)

data class FigurePose(
    /** Centre of the head. */
    val head: Point,
    /** Top of the spine, between the shoulders. */
    val neck: Point,
    /** Bottom of the spine, between the hips. */
    val hip: Point,
    /**
     * Which side the face is on: turn the neck-to-head direction a quarter turn clockwise (1) or anticlockwise
     * (-1) on screen. A standing figure with 1 looks right; lying with its head to the left, up.
     */
    val face: Int,
    val arms: LimbPair,
    val legs: LimbPair,
    /** Pulls the spine into a curve (a rounded back, a bridge). Left out, the spine is straight. */
    val spine: Point? = null,
)

/** A figure's limbs, in the order they are drawn when nothing says otherwise. */
enum class LimbName { FarLeg, FarArm, NearLeg, NearArm }

/** Body measurements, in the same units as the pose. */
object FigureSize {
    const val HEAD_RADIUS = 11.0
    const val TORSO_WIDTH = 22.0
    const val ARM_WIDTH = 10.0
    const val LEG_WIDTH = 13.0
    const val HAND_RADIUS = 4.5
    const val FOOT_RADIUS = 5.5
}

/** How far down the spine the shoulders sit, as a fraction of its length. */
private const val SHOULDER_AT = 0.06

/** The belt covers this stretch of the spine, as fractions of its length. */
private const val BELT_FROM = 0.74
private const val BELT_TO = 0.86

/** Hair covers the part of the head within this angle of its middle: a little under half the head. */
private const val HAIR_HALF_ANGLE = 78 * PI / 180

/** Hair sits a little towards the back of the head rather than dead on top. */
private const val HAIR_TILT = 25 * PI / 180

/** A limb as drawn: from the body through the joint to the end, where the hand or foot goes. */
data class LimbParts(val root: Point, val joint: Point, val end: Point)

/**
 * A cap of hair: the part of the head's circle from [from] clockwise to [to], closed by the straight line
 * between them. [startAngle] and [sweepAngle] say the same in radians, clockwise from the positive x axis.
 */
data class HairCap(val from: Point, val to: Point, val startAngle: Double, val sweepAngle: Double)

data class FigureParts(
    val torso: Quad,
    val belt: Segment,
    val head: Point,
    val hair: HairCap,
    val farLeg: LimbParts,
    val farArm: LimbParts,
    val nearLeg: LimbParts,
    val nearArm: LimbParts,
) {
    fun limb(name: LimbName): LimbParts = when (name) {
        LimbName.FarLeg -> farLeg
        LimbName.FarArm -> farArm
        LimbName.NearLeg -> nearLeg
        LimbName.NearArm -> nearArm
    }
}

/** The control point that makes a straight spine: halfway between neck and hip. */
private fun spineControl(pose: FigurePose): Point =
    pose.spine ?: Point((pose.neck.x + pose.hip.x) / 2, (pose.neck.y + pose.hip.y) / 2)

/** A point on the spine, t = 0 at the neck and 1 at the hip. The spine is a quadratic curve. */
fun pointOnSpine(pose: FigurePose, t: Double): Point {
    val c = spineControl(pose)
    val a = (1 - t) * (1 - t)
    val b = 2 * (1 - t) * t
    val d = t * t
    return Point(a * pose.neck.x + b * c.x + d * pose.hip.x, a * pose.neck.y + b * c.y + d * pose.hip.y)
}

private fun limbParts(root: Point, limb: Limb) = LimbParts(root.rounded(), limb.joint.rounded(), limb.end.rounded())

/**
 * A cap of hair on the side of the head away from the neck, tilted towards the back of the head. Angles are
 * measured clockwise on screen because y points down.
 */
private fun hairCap(pose: FigurePose): HairCap {
    val r = FigureSize.HEAD_RADIUS
    val away = atan2(pose.head.y - pose.neck.y, pose.head.x - pose.neck.x)
    val mid = away - pose.face * HAIR_TILT
    val at = { angle: Double -> Point(pose.head.x + r * cos(angle), pose.head.y + r * sin(angle)).rounded() }
    return HairCap(at(mid - HAIR_HALF_ANGLE), at(mid + HAIR_HALF_ANGLE), mid - HAIR_HALF_ANGLE, 2 * HAIR_HALF_ANGLE)
}

/** Everything needed to draw one figure. */
fun figureParts(pose: FigurePose): FigureParts {
    val shoulder = pointOnSpine(pose, SHOULDER_AT)
    return FigureParts(
        torso = Quad(pose.neck.rounded(), spineControl(pose).rounded(), pose.hip.rounded()),
        belt = Segment(pointOnSpine(pose, BELT_FROM).rounded(), pointOnSpine(pose, BELT_TO).rounded()),
        head = pose.head,
        hair = hairCap(pose),
        farLeg = limbParts(pose.hip, pose.legs.far),
        farArm = limbParts(shoulder, pose.arms.far),
        nearLeg = limbParts(pose.hip, pose.legs.near),
        nearArm = limbParts(shoulder, pose.arms.near),
    )
}

/** Length of each side of an arrowhead. */
private const val ARROW_HEAD = 9.0

/** Angle between the shaft and each side of the head. */
private const val ARROW_HEAD_ANGLE = 32 * PI / 180

/** An arrow as drawn: the shaft, then a head of two strokes from [headLeft] to the tip and on to [headRight]. */
data class ArrowParts(val shaft: Quad, val headLeft: Point, val headRight: Point)

/**
 * The two back corners of an arrowhead whose tip is at [tip], for a line arriving from [from]: the head lines up
 * with the last stretch of a curve, which leaves its control point.
 */
fun arrowHead(tip: Point, from: Point): Pair<Point, Point> {
    val back = atan2(from.y - tip.y, from.x - tip.x)
    val side = { turn: Double -> Point(tip.x + ARROW_HEAD * cos(back + turn), tip.y + ARROW_HEAD * sin(back + turn)) }
    return side(ARROW_HEAD_ANGLE).rounded() to side(-ARROW_HEAD_ANGLE).rounded()
}

/**
 * An arrow from one point to another. [bend] bows the shaft sideways by that many units (positive bows to the
 * left of travel on screen).
 */
fun arrowParts(from: Point, to: Point, bend: Double = 0.0): ArrowParts {
    val dx = to.x - from.x
    val dy = to.y - from.y
    val length = hypot(dx, dy).takeIf { it != 0.0 } ?: 1.0
    // Doubling the offset puts the curve's midpoint `bend` away from the straight line.
    val control = Point((from.x + to.x) / 2 + dy / length * bend * 2, (from.y + to.y) / 2 - dx / length * bend * 2)
    val (left, right) = arrowHead(to, control)
    return ArrowParts(Quad(from.rounded(), control.rounded(), to.rounded()), left, right)
}

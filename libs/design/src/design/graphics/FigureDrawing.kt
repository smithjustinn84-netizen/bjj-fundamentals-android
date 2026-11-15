package dev.justinsmith.bjj.design.graphics

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import dev.justinsmith.bjj.core.FigureParts
import dev.justinsmith.bjj.core.FigurePose
import dev.justinsmith.bjj.core.FigureSize
import dev.justinsmith.bjj.core.HairCap
import dev.justinsmith.bjj.core.LimbName
import dev.justinsmith.bjj.core.figureParts
import kotlin.math.PI

/** The cloth a figure wears: the light gi with the red belt, the dark gi with a black one, in every scene. */
enum class Gi { Light, Dark }

/** What can be drawn of a figure in one pass: a limb, or the torso, belt and head together. */
enum class BodyPart(internal val limb: LimbName?) {
    FarLeg(LimbName.FarLeg),
    FarArm(LimbName.FarArm),
    Body(null),
    NearLeg(LimbName.NearLeg),
    NearArm(LimbName.NearArm),
    ;

    companion object {
        val All: Set<BodyPart> = entries.toSet()
    }
}

/** Width of the ink line round every shape. */
private const val OUTLINE = 2.25f

/** A half turn in degrees: the hair cap's angles come in radians. */
private const val HALF_TURN_DEGREES = 180.0

private fun limbWidth(name: LimbName): Double = when (name) {
    LimbName.FarLeg, LimbName.NearLeg -> FigureSize.LEG_WIDTH
    LimbName.FarArm, LimbName.NearArm -> FigureSize.ARM_WIDTH
}

private fun limbEndRadius(name: LimbName): Double = when (name) {
    LimbName.FarLeg, LimbName.NearLeg -> FigureSize.FOOT_RADIUS
    LimbName.FarArm, LimbName.NearArm -> FigureSize.HAND_RADIUS
}

private fun isFar(name: LimbName): Boolean = when (name) {
    LimbName.FarLeg, LimbName.FarArm -> true
    LimbName.NearLeg, LimbName.NearArm -> false
}

/** A thick line with an ink edge: the ink goes down first, slightly wider, then the cloth on top. */
private fun DiagramScope.cloth(path: Path, width: Double, color: Color) {
    stroke(path, art.ink, width.toFloat() + OUTLINE * 2, StrokeCap.Round, StrokeJoin.Round)
    stroke(path, color, width.toFloat(), StrokeCap.Round, StrokeJoin.Round)
}

private fun DiagramScope.limb(shape: FigureParts, name: LimbName, gi: Gi) {
    val limb = shape.limb(name)
    val radius = limbEndRadius(name).toFloat()
    disc(limb.end.toOffset(), radius, fill = art.skin, line = art.ink, lineWidth = OUTLINE)
    val path = Path().apply {
        moveTo(limb.root)
        lineTo(limb.joint)
        lineTo(limb.end)
    }
    val color = when (gi) {
        Gi.Light -> if (isFar(name)) art.giLightFar else art.giLight
        Gi.Dark -> if (isFar(name)) art.giDarkFar else art.giDark
    }
    cloth(path, limbWidth(name), color)
}

private fun hairPath(shape: FigureParts, hair: HairCap): Path {
    val radius = FigureSize.HEAD_RADIUS.toFloat()
    val toDegrees = { radians: Double -> (radians * HALF_TURN_DEGREES / PI).toFloat() }
    return Path().apply {
        moveTo(hair.from)
        arcTo(Rect(shape.head.toOffset(), radius), toDegrees(hair.startAngle), toDegrees(hair.sweepAngle), false)
        lineTo(hair.to)
        close()
    }
}

private fun DiagramScope.body(shape: FigureParts, gi: Gi) {
    val torsoWidth = FigureSize.TORSO_WIDTH
    val torso = Path().apply {
        moveTo(shape.torso.start)
        with(shape.torso) { quadraticTo(control.x.toFloat(), control.y.toFloat(), end.x.toFloat(), end.y.toFloat()) }
    }
    cloth(torso, torsoWidth, if (gi == Gi.Light) art.giLight else art.giDark)
    val belt = Path().apply {
        moveTo(shape.belt.from)
        lineTo(shape.belt.to)
    }
    // Cut square (the default cap), so it reads as a band across the waist.
    stroke(belt, if (gi == Gi.Light) art.belt else art.ink, torsoWidth.toFloat())
    val headRadius = FigureSize.HEAD_RADIUS.toFloat()
    disc(shape.head.toOffset(), headRadius, fill = art.skin, line = art.ink, lineWidth = OUTLINE)
    val hair = hairPath(shape, shape.hair)
    fill(hair, art.hair)
    stroke(hair, art.ink, OUTLINE, StrokeCap.Round, StrokeJoin.Round)
}

/**
 * One jointed grappler. [parts] draws only some of the body, so a scene can put one person's leg in front of
 * the other person (half guard, back control hooks). Whatever is drawn goes down far side first: far leg, far
 * arm, body, near leg, near arm.
 */
fun DiagramScope.figure(pose: FigurePose, gi: Gi, parts: Set<BodyPart> = BodyPart.All) {
    val shape = figureParts(pose)
    for (part in BodyPart.entries) {
        if (part !in parts) continue
        val limb = part.limb
        if (limb == null) body(shape, gi) else limb(shape, limb, gi)
    }
}

// The gi jacket laid flat, for showing where to grip it.
private const val GI_BELT_WIDTH = 12f

/** A piece of the jacket: cloth with an ink line round it, along the SVG path [d]. */
fun DiagramScope.gi(d: String) {
    val path = svgPath(d)
    fill(path, art.giLight)
    stroke(path, art.ink, OUTLINE, join = StrokeJoin.Round)
}

/** A seam or the edge of a lapel, along the SVG path [d]. */
fun DiagramScope.seam(d: String) = stroke(svgPath(d), art.ink, OUTLINE, StrokeCap.Round)

/** The jacket's belt, along the SVG path [d]. */
fun DiagramScope.giBelt(d: String) = stroke(svgPath(d), art.belt, GI_BELT_WIDTH)

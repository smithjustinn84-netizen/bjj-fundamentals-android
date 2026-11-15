@file:Suppress("detekt.MagicNumber") // Drawing coordinates, copied from the web app's diagrams.

package dev.justinsmith.bjj.design.graphics.scenes

import dev.justinsmith.bjj.core.FigurePose
import dev.justinsmith.bjj.core.Point
import dev.justinsmith.bjj.design.graphics.BodyPart
import dev.justinsmith.bjj.design.graphics.BodyPart.Body
import dev.justinsmith.bjj.design.graphics.BodyPart.FarArm
import dev.justinsmith.bjj.design.graphics.BodyPart.FarLeg
import dev.justinsmith.bjj.design.graphics.BodyPart.NearArm
import dev.justinsmith.bjj.design.graphics.BodyPart.NearLeg
import dev.justinsmith.bjj.design.graphics.DiagramScope
import dev.justinsmith.bjj.design.graphics.DiagramView
import dev.justinsmith.bjj.design.graphics.Gi
import dev.justinsmith.bjj.design.graphics.arrow
import dev.justinsmith.bjj.design.graphics.figure
import dev.justinsmith.bjj.design.graphics.ring

/** One pass of the brush: some or all of one person. Later layers sit in front of earlier ones. */
internal class SceneLayer(val gi: Gi, val parts: Set<BodyPart> = BodyPart.All) {
    constructor(gi: Gi, vararg parts: BodyPart) : this(gi, parts.toSet())
}

/**
 * A position as data, so that another picture can reuse its figures (a concept drawn on a position). The light
 * gi is always the person underneath or being controlled.
 */
internal class PositionScene(
    val light: FigurePose,
    val dark: FigurePose,
    /** Drawing the same person twice with different parts puts one limb in front of the other person. */
    val layers: List<SceneLayer>,
    val view: DiagramView = DiagramView.Side,
    /** Marks that belong to the still picture: rings and arrows. */
    val extras: (DiagramScope.() -> Unit)? = null,
)

/** The scene for each built-in position, by its starter id. A position the user added has none. */
internal val POSITION_SCENES: Map<String, PositionScene> = mapOf(
    "standing" to PositionScene(
        light = STANDING_RIGHT,
        dark = STANDING_LEFT,
        // The collar grip reaches across in front of the other person.
        layers = listOf(SceneLayer(Gi.Dark), SceneLayer(Gi.Light), SceneLayer(Gi.Dark, NearArm)),
    ),
    "closed-guard" to PositionScene(
        light = CLOSED_GUARD_BOTTOM,
        dark = CLOSED_GUARD_TOP,
        // The near leg wraps the waist; the ankles cross behind the back.
        layers = listOf(
            SceneLayer(Gi.Light, FarLeg, FarArm, Body),
            SceneLayer(Gi.Dark),
            SceneLayer(Gi.Light, NearLeg, NearArm),
        ),
    ),
    "open-guard" to PositionScene(
        light = OPEN_GUARD_BOTTOM,
        dark = OPEN_GUARD_TOP,
        // Feet on the hips keep the distance.
        layers = listOf(
            SceneLayer(Gi.Dark, FarLeg, FarArm, Body, NearLeg),
            SceneLayer(Gi.Light),
            SceneLayer(Gi.Dark, NearArm),
        ),
    ),
    "side-control" to PositionScene(
        view = DiagramView.Above,
        light = SIDE_CONTROL_BOTTOM,
        dark = SIDE_CONTROL_TOP,
        layers = listOf(SceneLayer(Gi.Light), SceneLayer(Gi.Dark)),
    ),
    "mount" to PositionScene(
        light = MOUNT_BOTTOM,
        dark = MOUNT_TOP,
        layers = listOf(
            SceneLayer(Gi.Light, FarLeg, FarArm, Body, NearLeg),
            SceneLayer(Gi.Dark),
            SceneLayer(Gi.Light, NearArm),
        ),
    ),
    "half-guard" to PositionScene(
        light = HALF_GUARD_BOTTOM,
        dark = HALF_GUARD_TOP,
        // The bottom person's top leg closes over the trapped shin.
        layers = listOf(
            SceneLayer(Gi.Light, FarLeg, FarArm, Body),
            SceneLayer(Gi.Dark),
            SceneLayer(Gi.Light, NearLeg, NearArm),
        ),
        extras = { ring(cx = 216, cy = 180, r = 27) },
    ),
    "back-control" to PositionScene(
        light = BACK_TAKEN,
        dark = ON_THE_BACK,
        // The seatbelt arm crosses the chest and the hook sits inside the thigh, so both go on top.
        layers = listOf(
            SceneLayer(Gi.Dark, FarLeg, FarArm, Body),
            SceneLayer(Gi.Light, FarLeg, FarArm, Body),
            SceneLayer(Gi.Dark, NearLeg, NearArm),
            SceneLayer(Gi.Light, NearLeg, NearArm),
        ),
    ),
    "turtle" to PositionScene(
        light = TURTLED,
        dark = ON_THE_TURTLE,
        layers = listOf(SceneLayer(Gi.Light), SceneLayer(Gi.Dark)),
        extras = { arrow(from = Point(200, 62), to = Point(200, 92)) },
    ),
)

/** A scene's figures and marks, without the paper behind it. */
internal fun DiagramScope.still(scene: PositionScene) {
    for (layer in scene.layers) {
        val pose = when (layer.gi) {
            Gi.Light -> scene.light
            Gi.Dark -> scene.dark
        }
        figure(pose, layer.gi, layer.parts)
    }
    scene.extras?.invoke(this)
}

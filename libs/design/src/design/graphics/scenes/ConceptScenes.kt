@file:Suppress("detekt.MagicNumber") // Drawing coordinates, copied from the web app's diagrams.

package dev.justinsmith.bjj.design.graphics.scenes

import dev.justinsmith.bjj.core.Point
import dev.justinsmith.bjj.design.graphics.BodyPart.Body
import dev.justinsmith.bjj.design.graphics.BodyPart.FarArm
import dev.justinsmith.bjj.design.graphics.BodyPart.FarLeg
import dev.justinsmith.bjj.design.graphics.BodyPart.NearArm
import dev.justinsmith.bjj.design.graphics.BodyPart.NearLeg
import dev.justinsmith.bjj.design.graphics.DiagramScope
import dev.justinsmith.bjj.design.graphics.DiagramView
import dev.justinsmith.bjj.design.graphics.Gi
import dev.justinsmith.bjj.design.graphics.arrow
import dev.justinsmith.bjj.design.graphics.bad
import dev.justinsmith.bjj.design.graphics.bone
import dev.justinsmith.bjj.design.graphics.dot
import dev.justinsmith.bjj.design.graphics.figure
import dev.justinsmith.bjj.design.graphics.ghost
import dev.justinsmith.bjj.design.graphics.gi
import dev.justinsmith.bjj.design.graphics.giBelt
import dev.justinsmith.bjj.design.graphics.good
import dev.justinsmith.bjj.design.graphics.guide
import dev.justinsmith.bjj.design.graphics.ring
import dev.justinsmith.bjj.design.graphics.seam
import dev.justinsmith.bjj.design.graphics.zone

/** A concept's picture: what to draw, and whether the mat is seen from the side or from above. */
internal class ConceptScene(val view: DiagramView = DiagramView.Side, val draw: DiagramScope.() -> Unit)

/** A built-in position's still picture, for a concept best shown inside that position. */
private fun DiagramScope.stillPosition(id: String) {
    POSITION_SCENES[id]?.let { still(it) }
}

/** Tapping is both the safety concept's picture and the end of every submission. */
internal val TAP_SCENE = ConceptScene {
    figure(TAPPING, Gi.Light)
    // The tap: rings spreading across the mat from the hand.
    ring(cx = 122, cy = 196, rx = 20, ry = 5)
    ring(cx = 122, cy = 196, rx = 36, ry = 9)
    arrow(from = Point(104, 140), to = Point(116, 174), bend = 8)
}

/** The scene for each built-in concept, by its starter id. A concept the user added has none. */
internal val CONCEPT_SCENES: Map<String, ConceptScene> = mapOf(
    "safety" to TAP_SCENE,
    "posture" to ConceptScene {
        guide(x1 = 112, y1 = 62, x2 = 112, y2 = 192)
        figure(KNEELING_TALL, Gi.Dark)
        good("M146 78 l8 9 l15 -19")
        figure(KNEELING_HUNCHED, Gi.Light)
        bad("M342 92 l15 15 M357 92 l-15 15")
    },
    "base" to ConceptScene {
        // The triangle between the feet and the hips: stay inside it and you stay up.
        zone("200,122 140,195 260,195")
        figure(WIDE_STANCE, Gi.Dark)
        guide(x1 = 200, y1 = 126, x2 = 200, y2 = 194)
        dot(cx = 146, cy = 196, r = 5)
        dot(cx = 254, cy = 196, r = 5)
    },
    "frames" to ConceptScene {
        figure(FRAMING, Gi.Light, setOf(FarLeg, FarArm, Body))
        figure(LEANING_IN, Gi.Dark)
        figure(FRAMING, Gi.Light, setOf(NearLeg, NearArm))
        // The forearm is the frame: bone, not muscle, holds the weight.
        bone("M112 159 L136 137")
        arrow(from = Point(186, 88), to = Point(160, 110))
    },
    "hip-escape" to ConceptScene(DiagramView.Above) {
        ghost { figure(BEFORE_SHRIMP, Gi.Light) }
        figure(AFTER_SHRIMP, Gi.Light)
        arrow(from = Point(204, 76), to = Point(176, 164), bend = -34)
    },
    "bridge" to ConceptScene {
        figure(BRIDGING, Gi.Light)
        arrow(from = Point(158, 190), to = Point(158, 164))
    },
    "elbows-in" to ConceptScene {
        figure(ELBOWS_TUCKED, Gi.Light)
        zone(x = 170, y = 60, width = 60, height = 50, rx = 10)
        arrow(from = Point(132, 92), to = Point(164, 92))
        arrow(from = Point(268, 92), to = Point(236, 92))
    },
    "underhooks" to ConceptScene {
        figure(UNDERHOOKED, Gi.Dark)
        figure(UNDERHOOKING, Gi.Light)
        bone("M176 78 L200 98 L236 96")
        arrow(from = Point(212, 142), to = Point(212, 114))
    },
    "grips" to ConceptScene {
        gi(
            "M152 68 L186 56 L200 100 L214 56 L248 68 L268 116 L242 130 L236 110 L236 190 L164 190 L164 110 " +
                "L158 130 L132 116 Z",
        )
        seam("M186 56 L206 146 M214 56 L200 100")
        giBelt("M164 152 L236 152")
        // The two grips a beginner needs first: collar and sleeve.
        ring(cx = 190, cy = 96, r = 13)
        dot(cx = 190, cy = 96, r = 5)
        ring(cx = 252, cy = 120, r = 13)
        dot(cx = 252, cy = 120, r = 5)
    },
    "pressure" to ConceptScene {
        figure(PINNED, Gi.Light)
        figure(PRESSING, Gi.Dark)
        arrow(from = Point(158, 104), to = Point(158, 134))
        arrow(from = Point(184, 98), to = Point(184, 128))
    },
    "inside-position" to ConceptScene {
        stillPosition("open-guard")
        // Knees and feet inside the hips: the bottom person owns the space between them.
        ring(cx = 210, cy = 142, r = 28)
        good("M262 64 l8 9 l15 -19")
    },
    "distance" to ConceptScene {
        // The middle distance, where both people can reach and neither controls.
        zone(x = 168, y = 62, width = 64, height = 132, rx = 10)
        figure(SQUARED_UP_LEFT, Gi.Dark)
        figure(SQUARED_UP_RIGHT, Gi.Light)
        bad("M193 40 l15 15 M208 40 l-15 15")
    },
    "position-before-submission" to ConceptScene {
        // A settled base under the hips comes before any attack.
        zone("150,158 112,195 186,195")
        stillPosition("mount")
        good("M184 72 l8 9 l15 -19")
    },
    "head-position" to ConceptScene {
        figure(PINNED, Gi.Light)
        figure(PRESSING, Gi.Dark)
        // Where the head goes, the weight follows.
        ring(cx = 128, cy = 158, r = 16)
        arrow(from = Point(110, 110), to = Point(122, 138), bend = -6)
    },
    "off-balancing" to ConceptScene {
        // The base they had: once the hips leave it, they have to post or fall.
        zone("200,122 140,195 260,195")
        ghost { figure(WIDE_STANCE, Gi.Light) }
        figure(TIPPING, Gi.Light)
        arrow(from = Point(138, 40), to = Point(98, 66), bend = -8)
    },
    "breathing" to ConceptScene(DiagramView.Above) {
        stillPosition("side-control")
        // A long, slow breath out from underneath: calm before any escape.
        ring("M116 90 q-8 10 0 20")
        ring("M104 82 q-14 18 0 36")
        ring("M92 74 q-20 26 0 52")
    },
    "face-your-opponent" to ConceptScene {
        stillPosition("half-guard")
        // On the side and turned in toward them, never flat and never away.
        arrow(from = Point(78, 150), to = Point(104, 128), bend = 10)
        good("M262 72 l8 9 l15 -19")
    },
    "positional-hierarchy" to ConceptScene {
        stillPosition("back-control")
        // Each step up the ladder is a better position; back control is the top step.
        guide("M282 194 h18 v-30 h18 v-30 h18 v-30 h18")
        dot(cx = 345, cy = 104, r = 5)
    },
    "defend-early" to ConceptScene {
        stillPosition("back-control")
        // Win the hands before they reach the neck.
        ring(cx = 188, cy = 136, r = 16)
        arrow(from = Point(222, 128), to = Point(220, 160))
    },
    "angles" to ConceptScene {
        // Their centre line: attacks straight up it meet their strength.
        guide(x1 = 190, y1 = 56, x2 = 190, y2 = 194)
        stillPosition("closed-guard")
        arrow(from = Point(86, 150), to = Point(70, 112), bend = -10)
    },
    "two-on-one" to ConceptScene {
        stillPosition("closed-guard")
        // Both hands on one arm: that fight is always won.
        ring(cx = 153, cy = 149, r = 17)
        dot(cx = 156, cy = 152, r = 4)
        dot(cx = 150, cy = 146, r = 4)
    },
    "action-reaction" to ConceptScene {
        stillPosition("closed-guard")
        // The pull is the action; posturing up is the reaction the next attack waits for.
        arrow(from = Point(150, 72), to = Point(136, 104), bend = -8)
        arrow(from = Point(198, 96), to = Point(214, 62), bend = -8)
    },
    "hip-control" to ConceptScene {
        stillPosition("open-guard")
        // Pin the hips and the legs have nothing to push from.
        ring(cx = 162, cy = 180, r = 18)
        arrow(from = Point(150, 128), to = Point(158, 156))
    },
    "training-partner" to ConceptScene {
        stillPosition("standing")
        // Two people who both get to train again tomorrow.
        good("M188 34 l9 10 l17 -21")
    },
    "roll-to-learn" to ConceptScene {
        stillPosition("open-guard")
        // Give and take: each person moves, and lets the other move.
        arrow(from = Point(150, 60), to = Point(250, 60), bend = -18)
        arrow(from = Point(250, 78), to = Point(150, 78), bend = -18)
    },
    "breakfalls" to ConceptScene(DiagramView.Above) {
        figure(BREAKFALL, Gi.Light)
        // The slap: the whole arm meets the mat at once and spreads the fall.
        ring(cx = 184, cy = 76, r = 13)
        ring(cx = 184, cy = 160, r = 13)
        good("M276 62 l8 9 l15 -19")
    },
    "crossface" to ConceptScene(DiagramView.Above) {
        stillPosition("side-control")
        // The shoulder under the jaw turns the head away, and the spine follows it.
        ring(cx = 136, cy = 100, r = 17)
        arrow(from = Point(112, 92), to = Point(116, 126), bend = -10)
    },
    "wedges" to ConceptScene(DiagramView.Above) {
        stillPosition("side-control")
        // The wedge: a block against the hip, close to the body, that takes no strength to hold.
        ring(cx = 212, cy = 106, r = 13)
        dot(cx = 212, cy = 106, r = 5)
        // The lever: the far end of the leg, where a small push turns everything.
        ring(cx = 270, cy = 126, r = 11)
        arrow(from = Point(288, 124), to = Point(274, 156), bend = 10)
    },
)

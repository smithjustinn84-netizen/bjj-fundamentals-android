package dev.justinsmith.bjj.core

import dev.justinsmith.bjj.core.golden.ARROW_GOLDEN
import dev.justinsmith.bjj.core.golden.FIGURE_GOLDEN
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals

private fun Point.numbers() = listOf(x, y)

private fun Quad.numbers() = start.numbers() + control.numbers() + end.numbers()

private fun LimbParts.numbers() = root.numbers() + joint.numbers() + end.numbers()

class FigureTest {
    @Test
    fun `turns a pose into the same drawing as the web app`() {
        for ((pose, expected) in FIGURE_GOLDEN) {
            val parts = figureParts(pose)
            val actual = parts.torso.numbers() +
                parts.belt.from.numbers() + parts.belt.to.numbers() +
                parts.hair.from.numbers() + parts.hair.to.numbers() +
                LimbName.entries.flatMap { parts.limb(it).numbers() }
            assertEquals(expected, actual, "$pose")
        }
    }

    @Test
    fun `draws an arrow as the web app does`() {
        for ((arrow, expected) in ARROW_GOLDEN) {
            val (from, to, bend) = arrow
            val parts = arrowParts(from, to, bend)
            assertEquals(
                expected,
                parts.shaft.numbers() + parts.headLeft.numbers() + parts.headRight.numbers(),
                "$arrow",
            )
        }
    }

    @Test
    fun `a spine with no curve runs straight from neck to hip`() {
        val pose = FIGURE_GOLDEN.first().first
        assertEquals(pose.neck, pointOnSpine(pose, 0.0))
        assertEquals(pose.hip, pointOnSpine(pose, 1.0))
        assertEquals(Point(121, 105), pointOnSpine(pose, 0.5))
    }

    @Test
    fun `hair covers a little under half the head and starts and ends on it`() {
        val (pose, _) = FIGURE_GOLDEN.first()
        val hair = figureParts(pose).hair
        assertEquals(156 * PI / 180, hair.sweepAngle, 1e-12)
        for (end in listOf(hair.from, hair.to)) {
            val distance = kotlin.math.hypot(end.x - pose.head.x, end.y - pose.head.y)
            assertEquals(FigureSize.HEAD_RADIUS, distance, 0.1)
        }
    }
}

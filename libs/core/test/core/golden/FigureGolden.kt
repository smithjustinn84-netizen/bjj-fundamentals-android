// Generated from the web app by tools/web-fixtures. Do not edit: change the web app and run it again.
@file:Suppress("ktlint:standard:max-line-length", "detekt.MaxLineLength") // Prose, one sentence a line.

package dev.justinsmith.bjj.core.golden

import dev.justinsmith.bjj.core.FigurePose
import dev.justinsmith.bjj.core.Limb
import dev.justinsmith.bjj.core.LimbPair
import dev.justinsmith.bjj.core.Point

/**
 * Poses, and what the web app draws for each: the torso's curve, the belt's line, where the hair's arc starts
 * and ends, then the far leg, far arm, near leg and near arm, each from the body through its joint to its end.
 */
internal val FIGURE_GOLDEN: List<Pair<FigurePose, List<Double>>> = listOf(
    FigurePose(
        head = Point(120.0, 60.0),
        neck = Point(120.0, 78.0),
        hip = Point(122.0, 132.0),
        face = 1,
        arms = LimbPair(Limb(Point(136.0, 96.0), Point(150.0, 110.0)), Limb(Point(108.0, 100.0), Point(112.0, 124.0))),
        legs = LimbPair(Limb(Point(134.0, 164.0), Point(130.0, 196.0)), Limb(Point(112.0, 164.0), Point(108.0, 196.0))),
        spine = null,
    ) to
        listOf(120.0, 78.0, 121.0, 105.0, 122.0, 132.0, 121.5, 118.0, 121.7, 124.4, 109.3, 62.5, 128.8, 53.4, 122.0, 132.0, 134.0, 164.0, 130.0, 196.0, 120.1, 81.2, 136.0, 96.0, 150.0, 110.0, 122.0, 132.0, 112.0, 164.0, 108.0, 196.0, 120.1, 81.2, 108.0, 100.0, 112.0, 124.0),
    FigurePose(
        head = Point(70.5, 181.0),
        neck = Point(88.0, 178.25),
        hip = Point(150.0, 172.0),
        face = -1,
        arms = LimbPair(Limb(Point(104.0, 160.0), Point(122.0, 149.5)), Limb(Point(100.0, 190.0), Point(118.0, 192.0))),
        legs = LimbPair(Limb(Point(178.0, 150.0), Point(196.0, 190.0)), Limb(Point(182.0, 156.0), Point(201.5, 191.0))),
        spine = Point(118.0, 150.5),
    ) to
        listOf(88.0, 178.3, 118.0, 150.5, 150.0, 172.0, 133.5, 164.1, 141.1, 166.9, 65.3, 190.7, 71.3, 170.0, 150.0, 172.0, 178.0, 150.0, 196.0, 190.0, 91.6, 175.1, 104.0, 160.0, 122.0, 149.5, 150.0, 172.0, 182.0, 156.0, 201.5, 191.0, 91.6, 175.1, 100.0, 190.0, 118.0, 192.0),
    FigurePose(
        head = Point(233.3, 96.7),
        neck = Point(246.0, 110.0),
        hip = Point(262.0, 160.0),
        face = -1,
        arms = LimbPair(Limb(Point(230.0, 130.0), Point(214.0, 142.0)), Limb(Point(238.0, 138.0), Point(222.0, 156.0))),
        legs = LimbPair(Limb(Point(240.0, 176.0), Point(270.0, 191.0)), Limb(Point(236.0, 180.0), Point(266.0, 192.0))),
        spine = Point(266.6, 128.4),
    ) to
        listOf(246.0, 110.0, 266.6, 128.4, 262.0, 160.0, 262.7, 144.5, 262.8, 151.4, 222.4, 98.0, 242.8, 91.1, 262.0, 160.0, 240.0, 176.0, 270.0, 191.0, 248.4, 112.3, 230.0, 130.0, 214.0, 142.0, 262.0, 160.0, 236.0, 180.0, 266.0, 192.0, 248.4, 112.3, 238.0, 138.0, 222.0, 156.0),
)

/** Arrows as from, to and bend, and what the web app draws: the shaft's curve, then the head's two back corners. */
internal val ARROW_GOLDEN: List<Pair<Triple<Point, Point, Double>, List<Double>>> = listOf(
    Triple(Point(100.0, 100.0), Point(180.0, 100.0), 0.0) to
        listOf(100.0, 100.0, 140.0, 100.0, 180.0, 100.0, 172.4, 95.2, 172.4, 104.8),
    Triple(Point(100.0, 100.0), Point(180.0, 60.0), 12.0) to
        listOf(100.0, 100.0, 129.3, 58.5, 180.0, 60.0, 172.5, 55.0, 172.2, 64.5),
    Triple(Point(220.5, 40.0), Point(150.0, 130.5), -18.0) to
        listOf(220.5, 40.0, 156.9, 63.1, 150.0, 130.5, 155.5, 123.4, 146.0, 122.4),
    Triple(Point(50.0, 50.0), Point(50.0, 50.0), 0.0) to
        listOf(50.0, 50.0, 50.0, 50.0, 50.0, 50.0, 57.6, 54.8, 57.6, 45.2),
)

@file:Suppress("detekt.MagicNumber") // Drawing coordinates, copied from the web app's diagrams.

package dev.justinsmith.bjj.design.graphics.scenes

import dev.justinsmith.bjj.core.FigurePose
import dev.justinsmith.bjj.core.Limb
import dev.justinsmith.bjj.core.LimbPair
import dev.justinsmith.bjj.core.Point

// Poses are joint positions in the shared diagram space (see Diagram.kt): the mat at y = 196, the action centred
// on x = 200. Lying figures keep their spine about 13 above the mat and their feet 6 above it, so cloth rests on
// the line. The numbers are the web app's (PositionSvg.tsx, ConceptSvg.tsx); change them there first.

/** A limb from its middle joint ([jointX], [jointY]) to its end ([endX], [endY]). */
private fun limb(jointX: Int, jointY: Int, endX: Int, endY: Int) = Limb(Point(jointX, jointY), Point(endX, endY))

// The eight positions.

internal val STANDING_LEFT = FigurePose(
    head = Point(166, 58),
    neck = Point(158, 72),
    hip = Point(142, 124),
    face = 1,
    arms = LimbPair(far = limb(182, 98, 206, 108), near = limb(186, 88, 216, 80)),
    legs = LimbPair(far = limb(126, 158, 110, 190), near = limb(156, 156, 152, 190)),
)

internal val STANDING_RIGHT = FigurePose(
    head = Point(234, 58),
    neck = Point(242, 72),
    hip = Point(258, 124),
    face = -1,
    arms = LimbPair(far = limb(218, 88, 196, 82), near = limb(214, 98, 186, 102)),
    legs = LimbPair(far = limb(274, 158, 290, 190), near = limb(244, 156, 248, 190)),
)

internal val CLOSED_GUARD_BOTTOM = FigurePose(
    head = Point(92, 182),
    neck = Point(107, 183),
    hip = Point(159, 184),
    face = 1,
    arms = LimbPair(far = limb(128, 164, 156, 152), near = limb(124, 160, 150, 146)),
    legs = LimbPair(far = limb(198, 154, 232, 158), near = limb(200, 146, 230, 150)),
)

internal val CLOSED_GUARD_TOP = FigurePose(
    head = Point(166, 100),
    neck = Point(176, 112),
    hip = Point(206, 158),
    face = -1,
    arms = LimbPair(far = limb(158, 134, 140, 156), near = limb(162, 138, 146, 162)),
    legs = LimbPair(far = limb(184, 188, 224, 190), near = limb(180, 188, 220, 190)),
)

internal val OPEN_GUARD_BOTTOM = FigurePose(
    head = Point(96, 178),
    neck = Point(110, 176),
    hip = Point(160, 184),
    face = 1,
    arms = LimbPair(far = limb(134, 162, 158, 154), near = limb(130, 158, 154, 148)),
    legs = LimbPair(far = limb(194, 158, 226, 140), near = limb(190, 150, 222, 130)),
)

internal val OPEN_GUARD_TOP = FigurePose(
    head = Point(183, 98),
    neck = Point(196, 104),
    hip = Point(240, 126),
    face = -1,
    arms = LimbPair(far = limb(186, 126, 198, 156), near = limb(190, 130, 192, 152)),
    legs = LimbPair(far = limb(262, 158, 270, 190), near = limb(232, 160, 238, 190)),
)

// Seen from above: one body along the mat, the other lying across its chest.
internal val SIDE_CONTROL_BOTTOM = FigurePose(
    head = Point(136, 100),
    neck = Point(151, 100),
    hip = Point(205, 100),
    face = 1,
    arms = LimbPair(far = limb(160, 80, 180, 70), near = limb(162, 120, 146, 136)),
    legs = LimbPair(far = limb(240, 90, 276, 88), near = limb(238, 112, 270, 126)),
)

internal val SIDE_CONTROL_TOP = FigurePose(
    head = Point(168, 67),
    neck = Point(170, 82),
    hip = Point(178, 134),
    face = -1,
    arms = LimbPair(far = limb(196, 92, 214, 108), near = limb(148, 84, 126, 92)),
    legs = LimbPair(far = limb(204, 158, 224, 184), near = limb(160, 162, 144, 188)),
)

internal val MOUNT_BOTTOM = FigurePose(
    head = Point(96, 182),
    neck = Point(111, 183),
    hip = Point(163, 184),
    face = 1,
    arms = LimbPair(far = limb(128, 166, 146, 154), near = limb(132, 168, 152, 160)),
    legs = LimbPair(far = limb(200, 166, 224, 190), near = limb(196, 160, 216, 190)),
)

internal val MOUNT_TOP = FigurePose(
    head = Point(136, 93),
    neck = Point(140, 108),
    hip = Point(150, 160),
    face = -1,
    arms = LimbPair(far = limb(124, 126, 116, 150), near = limb(128, 130, 122, 156)),
    legs = LimbPair(far = limb(130, 184, 170, 190), near = limb(126, 184, 166, 190)),
)

internal val HALF_GUARD_BOTTOM = FigurePose(
    head = Point(92, 180),
    neck = Point(106, 182),
    hip = Point(158, 183),
    face = 1,
    arms = LimbPair(far = limb(124, 164, 146, 150), near = limb(126, 168, 150, 158)),
    legs = LimbPair(far = limb(196, 188, 228, 190), near = limb(194, 168, 226, 180)),
)

internal val HALF_GUARD_TOP = FigurePose(
    head = Point(128, 121),
    neck = Point(142, 128),
    hip = Point(190, 146),
    face = -1,
    arms = LimbPair(far = limb(130, 152, 112, 168), near = limb(136, 154, 116, 172)),
    legs = LimbPair(far = limb(228, 160, 250, 190), near = limb(204, 180, 244, 184)),
)

internal val TURTLED = FigurePose(
    head = Point(118, 172),
    neck = Point(130, 162),
    hip = Point(196, 156),
    face = -1,
    arms = LimbPair(far = limb(150, 184, 128, 190), near = limb(146, 182, 124, 190)),
    legs = LimbPair(far = limb(176, 188, 212, 190), near = limb(172, 188, 208, 190)),
    spine = Point(160, 128),
)

internal val ON_THE_TURTLE = FigurePose(
    head = Point(150, 104),
    neck = Point(164, 110),
    hip = Point(216, 120),
    face = -1,
    arms = LimbPair(far = limb(152, 136, 140, 158), near = limb(160, 140, 150, 166)),
    legs = LimbPair(far = limb(250, 156, 282, 190), near = limb(246, 160, 276, 190)),
)

// Both sitting, facing right: the attacker behind, chest to back.
internal val BACK_TAKEN = FigurePose(
    head = Point(186, 116),
    neck = Point(181, 131),
    hip = Point(200, 181),
    face = 1,
    arms = LimbPair(far = limb(208, 150, 200, 138), near = limb(206, 160, 198, 146)),
    legs = LimbPair(far = limb(242, 168, 272, 190), near = limb(238, 162, 268, 190)),
)

internal val ON_THE_BACK = FigurePose(
    head = Point(156, 110),
    neck = Point(150, 126),
    hip = Point(160, 181),
    face = 1,
    arms = LimbPair(far = limb(180, 150, 200, 160), near = limb(184, 134, 198, 152)),
    legs = LimbPair(far = limb(208, 168, 232, 188), near = limb(206, 160, 230, 186)),
)

// The concepts.

// Face down with one arm free to slap the mat: the tap has to be seen as well as felt.
internal val TAPPING = FigurePose(
    head = Point(158, 180),
    neck = Point(173, 183),
    hip = Point(225, 184),
    face = -1,
    arms = LimbPair(far = limb(160, 188, 142, 190), near = limb(150, 170, 124, 189)),
    legs = LimbPair(far = limb(262, 186, 298, 190), near = limb(258, 187, 294, 190)),
)

internal val KNEELING_TALL = FigurePose(
    head = Point(113, 90),
    neck = Point(112, 105),
    hip = Point(112, 157),
    face = 1,
    arms = LimbPair(far = limb(124, 128, 142, 144), near = limb(120, 132, 138, 152)),
    legs = LimbPair(far = limb(140, 188, 104, 190), near = limb(134, 188, 98, 190)),
)

internal val KNEELING_HUNCHED = FigurePose(
    head = Point(324, 138),
    neck = Point(311, 128),
    hip = Point(272, 157),
    face = 1,
    arms = LimbPair(far = limb(320, 152, 336, 172), near = limb(314, 154, 328, 176)),
    legs = LimbPair(far = limb(300, 188, 264, 190), near = limb(294, 188, 258, 190)),
    spine = Point(282, 116),
)

internal val WIDE_STANCE = FigurePose(
    head = Point(200, 52),
    neck = Point(200, 67),
    hip = Point(200, 122),
    face = 1,
    arms = LimbPair(far = limb(176, 90, 166, 114), near = limb(224, 90, 234, 114)),
    legs = LimbPair(far = limb(170, 152, 146, 190), near = limb(230, 152, 254, 190)),
)

internal val FRAMING = FigurePose(
    head = Point(80, 182),
    neck = Point(95, 183),
    hip = Point(147, 184),
    face = 1,
    arms = LimbPair(far = limb(118, 166, 148, 158), near = limb(112, 159, 136, 137)),
    legs = LimbPair(far = limb(178, 162, 208, 182), near = limb(176, 156, 206, 176)),
)

internal val LEANING_IN = FigurePose(
    head = Point(137, 118),
    neck = Point(150, 125),
    hip = Point(196, 157),
    face = -1,
    arms = LimbPair(far = limb(142, 150, 126, 170), near = limb(152, 152, 134, 176)),
    legs = LimbPair(far = limb(176, 188, 214, 190), near = limb(172, 188, 210, 190)),
)

// Seen from above: flat on the back, then curled with the hips slid away.
internal val BEFORE_SHRIMP = FigurePose(
    head = Point(126, 84),
    neck = Point(140, 90),
    hip = Point(196, 100),
    face = 1,
    arms = LimbPair(far = limb(158, 72, 180, 62), near = limb(160, 110, 184, 116)),
    legs = LimbPair(far = limb(234, 92, 270, 96), near = limb(234, 110, 270, 116)),
)

internal val AFTER_SHRIMP = FigurePose(
    head = Point(126, 84),
    neck = Point(140, 90),
    hip = Point(184, 136),
    face = 1,
    arms = LimbPair(far = limb(162, 72, 186, 62), near = limb(170, 84, 196, 78)),
    legs = LimbPair(far = limb(222, 118, 252, 100), near = limb(222, 150, 258, 158)),
    spine = Point(164, 100),
)

internal val BRIDGING = FigurePose(
    head = Point(96, 182),
    neck = Point(110, 176),
    hip = Point(160, 142),
    face = 1,
    arms = LimbPair(far = limb(128, 186, 150, 190), near = limb(130, 188, 154, 191)),
    legs = LimbPair(far = limb(196, 152, 204, 190), near = limb(192, 154, 198, 190)),
    spine = Point(128, 146),
)

internal val ELBOWS_TUCKED = FigurePose(
    head = Point(200, 46),
    neck = Point(200, 61),
    hip = Point(200, 116),
    face = 1,
    arms = LimbPair(far = limb(182, 94, 190, 70), near = limb(218, 94, 210, 70)),
    legs = LimbPair(far = limb(180, 152, 176, 190), near = limb(220, 152, 224, 190)),
)

internal val UNDERHOOKING = FigurePose(
    head = Point(182, 60),
    neck = Point(173, 73),
    hip = Point(150, 122),
    face = 1,
    arms = LimbPair(far = limb(196, 92, 216, 80), near = limb(200, 98, 236, 96)),
    legs = LimbPair(far = limb(136, 156, 118, 190), near = limb(164, 154, 156, 190)),
)

internal val UNDERHOOKED = FigurePose(
    head = Point(226, 60),
    neck = Point(235, 73),
    hip = Point(258, 122),
    face = -1,
    arms = LimbPair(far = limb(214, 88, 196, 104), near = limb(210, 82, 188, 84)),
    legs = LimbPair(far = limb(272, 156, 290, 190), near = limb(244, 154, 252, 190)),
)

internal val PINNED = FigurePose(
    head = Point(96, 182),
    neck = Point(111, 183),
    hip = Point(163, 184),
    face = 1,
    arms = LimbPair(far = limb(130, 170, 150, 160), near = limb(128, 186, 150, 190)),
    legs = LimbPair(far = limb(198, 174, 230, 190), near = limb(196, 170, 226, 190)),
)

internal val PRESSING = FigurePose(
    head = Point(128, 158),
    neck = Point(142, 162),
    hip = Point(194, 150),
    face = -1,
    arms = LimbPair(far = limb(150, 180, 130, 188), near = limb(156, 182, 134, 190)),
    legs = LimbPair(far = limb(232, 168, 268, 190), near = limb(230, 166, 264, 190)),
)

// Two people squared up out of reach, with the gap between them where hands would meet.
internal val SQUARED_UP_LEFT = FigurePose(
    head = Point(126, 58),
    neck = Point(118, 72),
    hip = Point(102, 124),
    face = 1,
    arms = LimbPair(far = limb(142, 98, 166, 108), near = limb(146, 88, 176, 80)),
    legs = LimbPair(far = limb(86, 158, 70, 190), near = limb(116, 156, 112, 190)),
)

internal val SQUARED_UP_RIGHT = FigurePose(
    head = Point(274, 58),
    neck = Point(282, 72),
    hip = Point(298, 124),
    face = -1,
    arms = LimbPair(far = limb(258, 88, 236, 82), near = limb(254, 98, 226, 102)),
    legs = LimbPair(far = limb(314, 158, 330, 190), near = limb(284, 156, 288, 190)),
)

// WIDE_STANCE tipped over its far foot, the near foot lifting off the mat.
internal val TIPPING = FigurePose(
    head = Point(160, 42),
    neck = Point(164, 57),
    hip = Point(179, 110),
    face = 1,
    arms = LimbPair(far = limb(147, 86, 144, 111), near = limb(193, 72, 210, 93)),
    legs = LimbPair(far = limb(159, 147, 146, 190), near = limb(216, 130, 250, 160)),
)

// Flat on the back seen from above, chin tucked, both arms landing palm down at 45 degrees from the body.
internal val BREAKFALL = FigurePose(
    head = Point(128, 118),
    neck = Point(142, 118),
    hip = Point(198, 118),
    face = 1,
    arms = LimbPair(far = limb(162, 96, 184, 76), near = limb(162, 140, 184, 160)),
    legs = LimbPair(far = limb(234, 104, 264, 112), near = limb(234, 132, 264, 124)),
)

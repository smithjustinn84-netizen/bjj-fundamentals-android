package dev.justinsmith.bjj.design.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Paper and ink, printed like a woodblock: flat colour, an ink line round everything you can press or open,
 * and a flat ink edge underneath in place of a soft shadow. The same colours as the web app's `tokens.css`;
 * every colour a screen uses comes from here.
 */
@Immutable
data class BjjPalette(
    val isDark: Boolean,
    val bg: Color,
    /** The tatami weave drawn faintly over [bg]. */
    val weave: Color,
    val surface: Color,
    val text: Color,
    val muted: Color,
    /** Hairlines: dividers and quiet edges. */
    val border: Color,
    /** The printed line round cards, pictures and controls. */
    val outline: Color,
    /** Raised above a surface: the picked segment of a switch. */
    val raised: Color,
    /** Sunk below a surface: a switch's track. */
    val well: Color,
    /** The edge of something you press or type in. */
    val borderControl: Color,
    /** For text, icons and outlines; an area painted in the accent uses [fill] with [onFill]. */
    val accent: Color,
    val accentSoft: Color,
    val fill: Color,
    val onFill: Color,
    /** Quiet text on a fill. */
    val fillSoft: Color,
    /** The large faint mark on a fill. */
    val fillMark: Color,
    /** The one loud button on a screen. */
    val cta: Color,
    val onCta: Color,
    /** Text on a tag or block painted in a status or type colour. */
    val onStatus: Color,
    val danger: Color,
    val ok: Color,
    val warn: Color,
    /** The streak stamp, the due badge and the current tab. */
    val belt: Color,
    val onBelt: Color,
    val beltWhite: Color,
    val beltBlue: Color,
    val beltPurple: Color,
    val beltBrown: Color,
    val beltBlack: Color,
    val beltOutline: Color,
    val typeSubmission: Color,
    val typeTakedown: Color,
    val typeTransition: Color,
)

private val Ink = Color(0xFF17140F)
private val Cream = Color(0xFFFFFAF0)

val LightPalette = BjjPalette(
    isDark = false,
    bg = Color(0xFFEFE6D2),
    weave = Color(red = 120, green = 95, blue = 50, alpha = 18),
    surface = Cream,
    text = Ink,
    muted = Color(0xFF5F574A),
    border = Color(0xFFD2C4A6),
    outline = Ink,
    raised = Cream,
    well = Color(0xFFE0D3B8),
    borderControl = Ink,
    accent = Color(0xFF17407A),
    accentSoft = Color(0xFFDCE4F0),
    fill = Color(0xFF17407A),
    onFill = Cream,
    fillSoft = Color(0xFFC9D6EA),
    fillMark = Color(0xFF1D4C8E),
    cta = Color(0xFFE3A31F),
    onCta = Ink,
    onStatus = Cream,
    danger = Color(0xFFB3261E),
    ok = Color(0xFF2F7A3A),
    warn = Color(0xFF96580A),
    belt = Color(0xFFD23B2A),
    onBelt = Color(0xFFFFFFFF),
    beltWhite = Cream,
    beltBlue = Color(0xFF1F5FBF),
    beltPurple = Color(0xFF6B3FA0),
    beltBrown = Color(0xFF7A4A25),
    beltBlack = Ink,
    beltOutline = Ink,
    typeSubmission = Color(0xFFB8322A),
    typeTakedown = Color(0xFF6B3FA0),
    typeTransition = Color(0xFF5C6670),
)

private val Night = Color(0xFF110F0C)
private val Pit = Color(0xFF0A0908)

/** Warm ink, not grey: the night version of the paper, with layers that get lighter as they rise. */
val DarkPalette = BjjPalette(
    isDark = true,
    bg = Night,
    weave = Color(red = 255, green = 236, blue = 200, alpha = 9),
    surface = Color(0xFF201D19),
    text = Color(0xFFF1EBDF),
    muted = Color(0xFFAAA192),
    border = Color(0xFF3A352E),
    outline = Color(0xFF6A6256),
    raised = Color(0xFF322D27),
    well = Pit,
    borderControl = Color(0xFF736B5E),
    accent = Color(0xFF8FB3EA),
    accentSoft = Color(0xFF1F2C42),
    fill = Color(0xFF3468B0),
    onFill = Color(0xFFFFFFFF),
    fillSoft = Color(0xFFD5E0F2),
    fillMark = Color(0xFF3C72BD),
    cta = Color(0xFFE3A31F),
    onCta = Ink,
    onStatus = Night,
    danger = Color(0xFFF08B7F),
    ok = Color(0xFF8AC793),
    warn = Color(0xFFE9B450),
    belt = Color(0xFFC8443C),
    onBelt = Color(0xFFFFFFFF),
    beltWhite = Color(0xFFF1EBDF),
    beltBlue = Color(0xFF4F8FE6),
    beltPurple = Color(0xFF9D74D6),
    beltBrown = Color(0xFFA8703F),
    beltBlack = Pit,
    beltOutline = Color(0xFF8F877B),
    typeSubmission = Color(0xFFF08B7F),
    typeTakedown = Color(0xFFC3A4EF),
    typeTransition = Color(0xFFB4AEA3),
)

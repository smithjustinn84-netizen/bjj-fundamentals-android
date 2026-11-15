package dev.justinsmith.bjj.design.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/** The text styles a screen uses, in the system's own font. Colour is the caller's (see `BjjText`). */
object Type {
    /** A page's title. */
    val h1 =
        TextStyle(
            fontSize = TextSizes.xl,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.03).em,
            lineHeight = 1.1.em,
        )

    /** A section's heading. */
    val h2 = TextStyle(fontSize = TextSizes.lg, fontWeight = FontWeight.Bold, lineHeight = 1.2.em)

    val h3 = TextStyle(fontSize = TextSizes.md, fontWeight = FontWeight.Bold, lineHeight = 1.2.em)

    val body = TextStyle(fontSize = TextSizes.md, fontWeight = FontWeight.Normal, lineHeight = 1.5.em)

    /** The words under a title, a touch larger than body text. */
    val lead = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Normal, lineHeight = 1.5.em)

    val small = TextStyle(fontSize = TextSizes.sm, fontWeight = FontWeight.Normal, lineHeight = 1.45.em)

    /** A row's name, a chip, a link: text that is tapped. */
    val strong = TextStyle(fontSize = TextSizes.md, fontWeight = FontWeight.SemiBold, lineHeight = 1.35.em)

    val button = TextStyle(fontSize = TextSizes.md, fontWeight = FontWeight.Bold, lineHeight = 1.2.em)

    /** The small capitals over a title or a card. Give it upper-case text. */
    val eyebrow =
        TextStyle(
            fontSize = TextSizes.xs,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.1.em,
            lineHeight = 1.3.em,
        )

    /** A solid block naming a type. Give it upper-case text. */
    val tag =
        TextStyle(fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.05.em, lineHeight = 1.3.em)

    /** A field's label. */
    val label = TextStyle(fontSize = TextSizes.sm, fontWeight = FontWeight.Medium, lineHeight = 1.3.em)

    /** The one big number on Today. */
    val count =
        TextStyle(fontSize = 104.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.04).em, lineHeight = 1.em)

    val display =
        TextStyle(
            fontSize = TextSizes.display,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.03).em,
            lineHeight = 1.1.em,
        )
}

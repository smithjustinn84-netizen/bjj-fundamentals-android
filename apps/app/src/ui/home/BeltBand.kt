package dev.justinsmith.bjj.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.justinsmith.bjj.core.BeltRank
import dev.justinsmith.bjj.core.MAX_STRIPES
import dev.justinsmith.bjj.core.label
import dev.justinsmith.bjj.design.components.block
import dev.justinsmith.bjj.design.theme.BjjPalette
import dev.justinsmith.bjj.design.theme.BjjTheme
import dev.justinsmith.bjj.design.theme.Radius
import dev.justinsmith.bjj.resources.Res
import dev.justinsmith.bjj.resources.today_belt_description
import org.jetbrains.compose.resources.pluralStringResource

fun BjjPalette.beltColor(rank: BeltRank): Color = when (rank) {
    BeltRank.White -> beltWhite
    BeltRank.Blue -> beltBlue
    BeltRank.Purple -> beltPurple
    BeltRank.Brown -> beltBrown
    BeltRank.Black -> beltBlack
}

/**
 * The study rank as a strip of belt: its colour, and near one end the rank bar holding a slot per stripe with
 * the earned ones filled. The bar is black, or red on a black belt.
 */
@Composable
fun BeltBand(rank: BeltRank, stripes: Int, modifier: Modifier = Modifier) {
    val p = BjjTheme.palette
    val description = pluralStringResource(Res.plurals.today_belt_description, stripes, rank.label, stripes)
    Box(
        modifier.fillMaxWidth().height(BAND_HEIGHT).semantics { contentDescription = description }.drawBehind {
            block(p.beltColor(rank), p.beltOutline, Radius.sm)
            val barWidth = BAR_WIDTH.toPx()
            val barStart = size.width - barWidth - BAR_INSET.toPx()
            drawRect(
                if (rank ==
                    BeltRank.Black
                ) {
                    p.belt
                } else {
                    p.beltBlack
                },
                Offset(barStart, 0f),
                Size(barWidth, size.height),
            )
            val slot = barWidth / (MAX_STRIPES * 2 + 1)
            repeat(stripes.coerceIn(0, MAX_STRIPES)) { index ->
                drawRect(p.beltWhite, Offset(barStart + slot * (index * 2 + 1), 0f), Size(slot, size.height))
            }
        },
    )
}

private val BAND_HEIGHT = 22.dp
private val BAR_WIDTH = 72.dp
private val BAR_INSET = 28.dp

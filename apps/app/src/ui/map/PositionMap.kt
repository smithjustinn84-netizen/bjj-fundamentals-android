package dev.justinsmith.bjj.ui.map

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import dev.justinsmith.bjj.core.MapEdge
import dev.justinsmith.bjj.core.MapLayout
import dev.justinsmith.bjj.core.MapNode
import dev.justinsmith.bjj.core.MapOrientation
import dev.justinsmith.bjj.core.Point
import dev.justinsmith.bjj.core.TransitionType
import dev.justinsmith.bjj.core.arrowHead
import dev.justinsmith.bjj.core.endsTheRoll
import dev.justinsmith.bjj.core.label
import dev.justinsmith.bjj.design.components.BjjText
import dev.justinsmith.bjj.design.components.Card
import dev.justinsmith.bjj.design.components.Divider
import dev.justinsmith.bjj.design.components.Eyebrow
import dev.justinsmith.bjj.design.components.LinkButton
import dev.justinsmith.bjj.design.components.ProgressDot
import dev.justinsmith.bjj.design.components.Wrap
import dev.justinsmith.bjj.design.components.block
import dev.justinsmith.bjj.design.components.pressShrink
import dev.justinsmith.bjj.design.reducedMotion
import dev.justinsmith.bjj.design.theme.BjjTheme
import dev.justinsmith.bjj.design.theme.Breakpoints
import dev.justinsmith.bjj.design.theme.Motion
import dev.justinsmith.bjj.design.theme.Radius
import dev.justinsmith.bjj.design.theme.Sizes
import dev.justinsmith.bjj.design.theme.Spacing
import dev.justinsmith.bjj.design.theme.TextSizes
import dev.justinsmith.bjj.design.theme.Type
import dev.justinsmith.bjj.design.theme.typeColor
import dev.justinsmith.bjj.nav.Nav
import dev.justinsmith.bjj.nav.PositionDetail
import dev.justinsmith.bjj.nav.TransitionDetail
import dev.justinsmith.bjj.resources.Res
import dev.justinsmith.bjj.resources.map_description
import dev.justinsmith.bjj.resources.map_from_here
import dev.justinsmith.bjj.resources.map_no_techniques_from_here
import dev.justinsmith.bjj.resources.map_select_prompt
import dev.justinsmith.bjj.resources.map_submissions
import dev.justinsmith.bjj.resources.map_ways_in
import dev.justinsmith.bjj.ui.common.SideBySide
import dev.justinsmith.bjj.ui.common.TransitionList
import dev.justinsmith.bjj.ui.common.hasRail
import dev.justinsmith.bjj.ui.common.windowWidth
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** The map is hard to read on a phone, so Positions opens on the list there unless asked. */
@Composable
fun roomForMap(): Boolean = hasRail()

/** Submissions are counted on the position instead of drawn, so they get no line in the legend. */
private val ARROW_TYPES = TransitionType.entries.filterNot(::endsTheRoll)

/**
 * The positions as a graph: boxes in lanes by category, joined by an arrow per technique. Selecting a position
 * highlights its arrows and lists its techniques underneath. All geometry comes from `layoutPositionMap`; this
 * only draws it.
 */
@Composable
fun PositionMap(map: MapUi, nav: Nav, modifier: Modifier = Modifier) {
    val orientation = if (windowWidth() >= Breakpoints.mapAcross) MapOrientation.Horizontal else MapOrientation.Vertical
    val layout = map.layouts.getValue(orientation)
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = selectedId?.let { map.positions[it] }
    val sheet = @Composable {
        Card {
            MapSheet(map, layout, orientation, selected?.id) { id -> selectedId = if (selectedId == id) null else id }
            Divider()
            Legend()
        }
    }
    val panel = @Composable {
        Card {
            if (selected != null) {
                SelectedPosition(selected, nav)
            } else {
                BjjText(stringResource(Res.string.map_select_prompt), color = BjjTheme.palette.muted)
            }
        }
    }
    // Across the page the map needs the whole width, so its details go underneath; down the page they sit beside it.
    if (orientation == MapOrientation.Horizontal) {
        Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.s4)) {
            sheet()
            panel()
        }
    } else {
        SideBySide(sheet, panel, modifier)
    }
}

/** The map itself: arrows on a canvas, with the lanes' headings and the positions' boxes laid over it. */
@Composable
private fun MapSheet(
    map: MapUi,
    layout: MapLayout,
    orientation: MapOrientation,
    activeId: String?,
    onToggle: (String) -> Unit,
) {
    val neighbours = remember(layout, activeId) {
        layout.edges.filter { it.touches(activeId) }.flatMap { listOf(it.fromId, it.toId) }.toSet()
    }
    val palette = BjjTheme.palette
    val flow = flowPhase(running = activeId != null)
    val description = stringResource(Res.string.map_description)
    BoxWithConstraints(Modifier.fillMaxWidth().semantics { contentDescription = description }) {
        // One of the map's own units, on this screen.
        val unit = maxWidth / layout.width.toFloat()
        Spacer(
            Modifier.fillMaxWidth().height(unit * layout.height.toFloat()).drawWithCache {
                // The arrows' shapes are worked out once for this layout, size and selection, not on every frame
                // the dashes move.
                val scale = unit.toPx()
                val arrows = layout.edges.map { edge ->
                    val active = edge.touches(activeId)
                    Arrow(edge, palette.typeColor(edge.type), scale, active, dim = activeId != null && !active)
                }
                val flowStroke = FLOW_WIDTH * scale
                val dash = FLOW_DASH * scale
                val gap = FLOW_GAP * scale
                onDrawBehind {
                    for (arrow in arrows) drawArrow(arrow)
                    // Read here, while drawing, so the dashes moving redraw the map without recomposing it.
                    val offset = -flow() * (dash + gap)
                    val effect = PathEffect.dashPathEffect(floatArrayOf(dash, gap), offset)
                    val style = Stroke(flowStroke, cap = StrokeCap.Round, pathEffect = effect)
                    for (arrow in arrows) if (arrow.active) drawPath(arrow.line, palette.surface, style = style)
                }
            },
        )
        // Across the page a lane's heading is centred over its column; down it, it starts at the lane's edge.
        val centred = orientation == MapOrientation.Horizontal
        val laneStyle = Type.eyebrow.copy(fontSize = mapSp(unit, TextSizes.mapLane))
        for (lane in layout.lanes) {
            BjjText(
                lane.category.label.uppercase(),
                Modifier.layout { measurable, constraints ->
                    val label = measurable.measure(constraints)
                    val x = (unit * lane.labelX.toFloat()).roundToPx() - if (centred) label.width / 2 else 0
                    val y = (unit * lane.labelY.toFloat()).roundToPx()
                    layout(label.width, label.height) { label.place(x, y) }
                },
                style = laneStyle,
                color = palette.muted,
            )
        }
        for (node in layout.nodes) {
            val position = map.positions[node.id] ?: continue
            MapNodeButton(
                node = node,
                position = position,
                selected = node.id == activeId,
                dimmed = activeId != null && node.id != activeId && node.id !in neighbours,
                onToggle = { onToggle(node.id) },
                unit = unit,
                modifier = Modifier
                    .offset(unit * node.x.toFloat(), unit * node.y.toFloat())
                    .size(unit * node.width.toFloat(), unit * node.height.toFloat()),
            )
        }
    }
}

/** Which colour of arrow is which type of technique. */
@Composable
private fun Legend(modifier: Modifier = Modifier) {
    val palette = BjjTheme.palette
    Wrap(modifier) {
        for (type in ARROW_TYPES) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val color = palette.typeColor(type)
                Box(
                    Modifier.width(Sizes.legendLine).height(Sizes.legendWeight).drawBehind {
                        block(color, color, Radius.pill, 0.dp)
                    },
                )
                BjjText(type.label, style = Type.small, color = palette.muted)
            }
        }
    }
}

/**
 * How far along its loop the dashes on a highlighted arrow are, from 0 to 1, read while drawing so it does not
 * recompose. It runs only while something is [running], and is still when animations are off.
 */
@Composable
private fun flowPhase(running: Boolean): () -> Float {
    if (!running || reducedMotion()) return { 0f }
    val phase = rememberInfiniteTransition()
        .animateFloat(0f, 1f, infiniteRepeatable(tween(Motion.FLOW, easing = LinearEasing)))
    return { phase.value }
}

private fun MapEdge.touches(positionId: String?) = positionId != null && (fromId == positionId || toId == positionId)

private fun Point.at(scale: Float) = Offset((x * scale).toFloat(), (y * scale).toFloat())

private fun MapEdge.path(scale: Float) = Path().apply {
    val (start, control, end) = curve
    moveTo(start.at(scale).x, start.at(scale).y)
    quadraticTo(control.at(scale).x, control.at(scale).y, end.at(scale).x, end.at(scale).y)
}

/**
 * An arrow for one technique, ready to draw, in its type's colour: heavier when its position is selected, faint
 * when another is.
 */
private class Arrow(edge: MapEdge, color: Color, scale: Float, val active: Boolean, dim: Boolean) {
    val ink = if (dim) color.copy(alpha = DIM_EDGE) else color
    val line = edge.path(scale)
    val stroke = Stroke((if (active) ACTIVE_EDGE else EDGE) * scale, cap = StrokeCap.Round)
    val head = Path().apply {
        val (left, right) = arrowHead(edge.curve.end, edge.curve.control)
        val tip = edge.curve.end.at(scale)
        moveTo(left.at(scale).x, left.at(scale).y)
        lineTo(tip.x, tip.y)
        lineTo(right.at(scale).x, right.at(scale).y)
        close()
    }
}

private fun DrawScope.drawArrow(arrow: Arrow) {
    drawPath(arrow.line, arrow.ink, style = arrow.stroke)
    drawPath(arrow.head, arrow.ink)
}

// Stroke widths and dashes in the map's own units, as the web app draws them.
private const val EDGE = 2f
private const val ACTIVE_EDGE = 3f
private const val DIM_EDGE = 0.12f
private const val FLOW_WIDTH = 1.5f
private const val FLOW_DASH = 2f
private const val FLOW_GAP = 14f
private const val DIM_NODE = 0.35f

/**
 * A text size on the map, where [unit] is one of the map's units on screen: the map's text is in its own units, as
 * in the web app's drawing, so it grows and shrinks with the boxes and arrows instead of following the system's
 * font size and overflowing a box drawn below its full size.
 */
@Composable
private fun mapSp(unit: Dp, size: TextUnit): TextUnit = with(LocalDensity.current) { (unit * size.value).toSp() }

/** A box's lines, as a multiple of their text size: the web app's map, so two lines and a count fit its height. */
private const val MAP_LINE_HEIGHT = 1.2f

@Composable
private fun MapNodeButton(
    node: MapNode,
    position: MapPosition,
    selected: Boolean,
    dimmed: Boolean,
    onToggle: () -> Unit,
    unit: Dp,
    modifier: Modifier = Modifier,
) {
    val interactions = remember { MutableInteractionSource() }
    val p = BjjTheme.palette
    // Padding is in the map's units too, so two lines of name and a count fit the box at any size.
    val nameSize = mapSp(unit, TextSizes.mapName)
    val countSize = mapSp(unit, TextSizes.mapCount)
    Column(
        modifier
            .alpha(if (dimmed) DIM_NODE else 1f)
            .pressShrink(interactions, Radius.lg)
            .drawBehind {
                block(if (selected) p.accentSoft else p.surface, if (selected) p.accent else p.borderControl, Radius.lg)
            }
            .clickable(interactions, indication = null, role = Role.Button, onClick = onToggle)
            .semantics { this.selected = selected }
            .padding(horizontal = unit * Spacing.s2.value, vertical = unit * Spacing.s1.value),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s1), verticalAlignment = Alignment.CenterVertically) {
            ProgressDot(position.progress)
            BjjText(
                position.name,
                style = Type.small.copy(
                    fontSize = nameSize,
                    lineHeight = nameSize * MAP_LINE_HEIGHT,
                    fontWeight = FontWeight.SemiBold,
                ),
                maxLines = 2,
            )
        }
        if (node.submissions > 0) {
            BjjText(
                pluralStringResource(Res.plurals.map_submissions, node.submissions, node.submissions),
                style = Type.small.copy(
                    fontSize = countSize,
                    lineHeight = countSize * MAP_LINE_HEIGHT,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = p.typeSubmission,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun SelectedPosition(position: MapPosition, nav: Nav, modifier: Modifier = Modifier) {
    val outgoing = position.outgoing
    val incoming = position.incoming
    val open = { id: String -> nav.open(TransitionDetail(id)) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
        Eyebrow(position.category.label)
        LinkButton(position.name, { nav.open(PositionDetail(position.id)) })
        BjjText(position.summary, style = Type.small, color = BjjTheme.palette.muted)
        Eyebrow(stringResource(Res.string.map_from_here))
        if (outgoing.isNotEmpty()) {
            TransitionList(outgoing, open)
        } else {
            BjjText(
                stringResource(Res.string.map_no_techniques_from_here),
                style = Type.small,
                color = BjjTheme.palette.muted,
            )
        }
        if (incoming.isNotEmpty()) {
            Eyebrow(stringResource(Res.string.map_ways_in))
            TransitionList(incoming, open)
        }
    }
}

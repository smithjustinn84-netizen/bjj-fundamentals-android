package dev.justinsmith.bjj.ui.map

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.justinsmith.bjj.core.CardKind
import dev.justinsmith.bjj.core.Library
import dev.justinsmith.bjj.core.MapEdge
import dev.justinsmith.bjj.core.MapLayout
import dev.justinsmith.bjj.core.MapNode
import dev.justinsmith.bjj.core.MapOrientation
import dev.justinsmith.bjj.core.Point
import dev.justinsmith.bjj.core.Position
import dev.justinsmith.bjj.core.TransitionType
import dev.justinsmith.bjj.core.arrowHead
import dev.justinsmith.bjj.core.connections
import dev.justinsmith.bjj.core.endsTheRoll
import dev.justinsmith.bjj.core.label
import dev.justinsmith.bjj.core.layoutPositionMap
import dev.justinsmith.bjj.core.reviewOf
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
import dev.justinsmith.bjj.design.theme.Motion
import dev.justinsmith.bjj.design.theme.Radius
import dev.justinsmith.bjj.design.theme.Spacing
import dev.justinsmith.bjj.design.theme.Type
import dev.justinsmith.bjj.design.theme.typeColor
import dev.justinsmith.bjj.nav.PositionDetail
import dev.justinsmith.bjj.nav.TransitionDetail
import dev.justinsmith.bjj.resources.Res
import dev.justinsmith.bjj.resources.map_description
import dev.justinsmith.bjj.resources.map_from_here
import dev.justinsmith.bjj.resources.map_no_techniques_from_here
import dev.justinsmith.bjj.resources.map_select_prompt
import dev.justinsmith.bjj.resources.map_submissions
import dev.justinsmith.bjj.resources.map_ways_in
import dev.justinsmith.bjj.ui.common.Direction
import dev.justinsmith.bjj.ui.common.Nav
import dev.justinsmith.bjj.ui.common.SideBySide
import dev.justinsmith.bjj.ui.common.TransitionList
import dev.justinsmith.bjj.ui.common.hasRail
import dev.justinsmith.bjj.ui.common.progressOf
import dev.justinsmith.bjj.ui.common.windowWidth
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** The map is hard to read on a phone, so Positions opens on the list there unless asked. */
@Composable
fun roomForMap(): Boolean = hasRail()

/**
 * The across-the-page map needs a column about 1000dp wide to stay readable; narrower, the map runs down the
 * page instead.
 */
private val MAP_ACROSS = 1280.dp

/** Submissions are counted on the position instead of drawn, so they get no line in the legend. */
private val ARROW_TYPES = TransitionType.entries.filterNot(::endsTheRoll)

/**
 * The positions as a graph: boxes in lanes by category, joined by an arrow per technique. Selecting a position
 * highlights its arrows and lists its techniques underneath. All geometry comes from `layoutPositionMap`; this
 * only draws it.
 */
@Composable
fun PositionMap(lib: Library, nav: Nav, modifier: Modifier = Modifier) {
    val orientation = if (windowWidth() >= MAP_ACROSS) MapOrientation.Horizontal else MapOrientation.Vertical
    val layout = remember(lib.positions, lib.transitions, orientation) {
        layoutPositionMap(lib.positions, lib.transitions, orientation)
    }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = selectedId?.let { lib.positionById[it] }
    val sheet = @Composable {
        Card {
            MapSheet(lib, layout, orientation, selected?.id) { id -> selectedId = if (selectedId == id) null else id }
            Divider()
            Legend()
        }
    }
    val panel = @Composable {
        Card {
            if (selected != null) {
                SelectedPosition(selected, lib, nav)
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
    lib: Library,
    layout: MapLayout,
    orientation: MapOrientation,
    activeId: String?,
    onToggle: (String) -> Unit,
) {
    val touches = { edge: MapEdge -> edge.fromId == activeId || edge.toId == activeId }
    val neighbours = layout.edges.filter(touches).flatMap { listOf(it.fromId, it.toId) }.toSet()
    val palette = BjjTheme.palette
    val flow = flowPhase()
    val description = stringResource(Res.string.map_description)
    BoxWithConstraints(Modifier.fillMaxWidth().semantics { contentDescription = description }) {
        // One of the map's own units, on this screen.
        val unit = maxWidth / layout.width.toFloat()
        Canvas(Modifier.fillMaxWidth().height(unit * layout.height.toFloat())) {
            val scale = unit.toPx()
            for (edge in layout.edges) {
                val active = activeId != null && touches(edge)
                drawEdge(edge, palette.typeColor(edge.type), scale, active, dim = activeId != null && !active)
                if (active) drawFlow(edge, palette.surface, scale, flow)
            }
        }
        // Across the page a lane's heading is centred over its column.
        val nudge = if (orientation == MapOrientation.Horizontal) -LANE_LABEL_HALF else 0.dp
        for (lane in layout.lanes) {
            BjjText(
                lane.category.label.uppercase(),
                Modifier.offset(unit * lane.labelX.toFloat() + nudge, unit * lane.labelY.toFloat()),
                style = Type.eyebrow.copy(fontSize = 11.sp),
                color = palette.muted,
            )
        }
        for (node in layout.nodes) {
            val position = lib.positionById[node.id] ?: continue
            MapNodeButton(
                node = node,
                position = position,
                lib = lib,
                selected = node.id == activeId,
                dimmed = activeId != null && node.id != activeId && node.id !in neighbours,
                onToggle = { onToggle(node.id) },
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
                    Modifier.width(LEGEND_LINE).height(LEGEND_WEIGHT).drawBehind {
                        block(color, color, Radius.pill, 0.dp)
                    },
                )
                BjjText(type.label, style = Type.small, color = palette.muted)
            }
        }
    }
}

private val LANE_LABEL_HALF = 40.dp
private val LEGEND_LINE = 22.dp
private val LEGEND_WEIGHT = 3.dp

/** How far along its loop the dashes on a highlighted arrow are, from 0 to 1; still when animations are off. */
@Composable
private fun flowPhase(): Float {
    if (reducedMotion()) return 0f
    val transition = rememberInfiniteTransition()
    val phase by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(Motion.FLOW, easing = LinearEasing)))
    return phase
}

private fun Point.at(scale: Float) = Offset((x * scale).toFloat(), (y * scale).toFloat())

private fun MapEdge.path(scale: Float) = Path().apply {
    val (start, control, end) = curve
    moveTo(start.at(scale).x, start.at(scale).y)
    quadraticTo(control.at(scale).x, control.at(scale).y, end.at(scale).x, end.at(scale).y)
}

/** An arrow for one technique, in its type's colour: heavier when its position is selected, faint when another is. */
private fun DrawScope.drawEdge(edge: MapEdge, color: Color, scale: Float, active: Boolean, dim: Boolean) {
    val ink = if (dim) color.copy(alpha = DIM_EDGE) else color
    val width = (if (active) ACTIVE_EDGE else EDGE) * scale
    drawPath(edge.path(scale), ink, style = Stroke(width, cap = StrokeCap.Round))
    val (left, right) = arrowHead(edge.curve.end, edge.curve.control)
    val head = Path().apply {
        val tip = edge.curve.end.at(scale)
        moveTo(left.at(scale).x, left.at(scale).y)
        lineTo(tip.x, tip.y)
        lineTo(right.at(scale).x, right.at(scale).y)
        close()
    }
    drawPath(head, ink)
}

/** Dashes running along a highlighted arrow, to show which way it flows. */
private fun DrawScope.drawFlow(edge: MapEdge, color: Color, scale: Float, phase: Float) {
    val dash = FLOW_DASH * scale
    val gap = FLOW_GAP * scale
    val effect = PathEffect.dashPathEffect(floatArrayOf(dash, gap), -phase * (dash + gap))
    drawPath(edge.path(scale), color, style = Stroke(FLOW_WIDTH * scale, cap = StrokeCap.Round, pathEffect = effect))
}

// Stroke widths and dashes in the map's own units, as the web app draws them.
private const val EDGE = 2f
private const val ACTIVE_EDGE = 3f
private const val DIM_EDGE = 0.12f
private const val FLOW_WIDTH = 1.5f
private const val FLOW_DASH = 2f
private const val FLOW_GAP = 14f
private const val DIM_NODE = 0.35f

@Composable
private fun MapNodeButton(
    node: MapNode,
    position: Position,
    lib: Library,
    selected: Boolean,
    dimmed: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactions = remember { MutableInteractionSource() }
    val p = BjjTheme.palette
    Column(
        modifier
            .alpha(if (dimmed) DIM_NODE else 1f)
            .pressShrink(interactions, Radius.lg)
            .drawBehind {
                block(if (selected) p.accentSoft else p.surface, if (selected) p.accent else p.borderControl, Radius.lg)
            }
            .clickable(interactions, indication = null, role = Role.Button, onClick = onToggle)
            .semantics { this.selected = selected }
            .padding(horizontal = Spacing.s2, vertical = Spacing.s1),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s1), verticalAlignment = Alignment.CenterVertically) {
            ProgressDot(progressOf(reviewOf(lib, CardKind.Position, position.id)))
            BjjText(
                position.name,
                style = Type.small.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                maxLines = 2,
            )
        }
        if (node.submissions > 0) {
            BjjText(
                pluralStringResource(Res.plurals.map_submissions, node.submissions, node.submissions),
                style = Type.small.copy(fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold),
                color = p.typeSubmission,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun SelectedPosition(position: Position, lib: Library, nav: Nav, modifier: Modifier = Modifier) {
    val (outgoing, incoming) = connections(lib, position.id)
    val open = { id: String -> nav.open(TransitionDetail(id)) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
        Eyebrow(position.category.label)
        LinkButton(position.name, { nav.open(PositionDetail(position.id)) })
        BjjText(position.summary, style = Type.small, color = BjjTheme.palette.muted)
        Eyebrow(stringResource(Res.string.map_from_here))
        if (outgoing.isNotEmpty()) {
            TransitionList(outgoing, lib.positionById, Direction.Out, open)
        } else {
            BjjText(
                stringResource(Res.string.map_no_techniques_from_here),
                style = Type.small,
                color = BjjTheme.palette.muted,
            )
        }
        if (incoming.isNotEmpty()) {
            Eyebrow(stringResource(Res.string.map_ways_in))
            TransitionList(incoming, lib.positionById, Direction.In, open)
        }
    }
}

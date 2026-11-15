package dev.justinsmith.bjj.design.graphics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp
import dev.justinsmith.bjj.design.graphics.scenes.CONCEPT_SCENES
import dev.justinsmith.bjj.design.graphics.scenes.POSITION_SCENES
import dev.justinsmith.bjj.design.graphics.scenes.TAP_SCENE
import dev.justinsmith.bjj.design.graphics.scenes.still

// The pictures of the app's own study items. Each fills the width it is given at the web app's aspect ratio,
// inside its frame, and is decorative: a caller that wants it read out says so through `modifier`.

/** Whether [conceptId] is a built-in concept with a picture. A concept the user added has none. */
fun hasConceptArtwork(conceptId: String): Boolean = conceptId in CONCEPT_SCENES

/** The picture for a built-in concept, 16:9. Draws nothing for a concept the user added. */
@Composable
fun ConceptArtwork(conceptId: String, modifier: Modifier = Modifier) {
    val scene = CONCEPT_SCENES[conceptId] ?: return
    Diagram(DiagramShape.Wide, modifier, scene.view, scene.draw)
}

/** Whether [positionId] is a built-in position with a picture. A position the user added has none. */
fun hasPositionArtwork(positionId: String): Boolean = positionId in POSITION_SCENES

/** The picture for a built-in position, 4:3. Draws nothing for a position the user added. */
@Composable
fun PositionArtwork(positionId: String, modifier: Modifier = Modifier) {
    val scene = POSITION_SCENES[positionId] ?: return
    Diagram(DiagramShape.Tall, modifier, scene.view) { still(scene) }
}

/** Someone tapping out, framed like a position picture: where a submission ends. */
@Composable
fun TapArtwork(modifier: Modifier = Modifier) {
    Diagram(DiagramShape.Tall, modifier, TAP_SCENE.view, TAP_SCENE.draw)
}

// The arrow between the two pictures of a technique, as the web app draws it.
private const val TRANSITION_ARROW_WIDTH = 40f
private const val TRANSITION_ARROW_HEIGHT = 24f
private const val TRANSITION_ARROW_PATH = "M3 12h32M26 4l9 8-9 8"
private const val TRANSITION_ARROW_LINE = 3f

/**
 * The arrow from where a technique starts to where it ends, in [color]: the colour of the technique's type,
 * like its tag. As wide as the web app's unless [modifier] sets a width.
 */
@Composable
fun TransitionArrow(color: Color, modifier: Modifier = Modifier) {
    Canvas(
        modifier.width(TRANSITION_ARROW_WIDTH.dp).aspectRatio(TRANSITION_ARROW_WIDTH / TRANSITION_ARROW_HEIGHT),
    ) {
        scale(size.width / TRANSITION_ARROW_WIDTH, pivot = Offset.Zero) {
            val line = Stroke(TRANSITION_ARROW_LINE, cap = StrokeCap.Round, join = StrokeJoin.Round)
            drawPath(svgPath(TRANSITION_ARROW_PATH), color, style = line)
        }
    }
}

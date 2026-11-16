package dev.justinsmith.bjj.design.graphics

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.justinsmith.bjj.design.graphics.scenes.CONCEPT_SCENES
import dev.justinsmith.bjj.design.graphics.scenes.POSITION_SCENES
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

package dev.justinsmith.bjj.ui.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import dev.justinsmith.bjj.core.CardKind
import dev.justinsmith.bjj.core.ItemKind
import dev.justinsmith.bjj.core.Library
import dev.justinsmith.bjj.core.PositionRole
import dev.justinsmith.bjj.core.StudyItem
import dev.justinsmith.bjj.core.VideoBookmark
import dev.justinsmith.bjj.core.findSituation
import dev.justinsmith.bjj.core.label
import dev.justinsmith.bjj.core.roleGoals
import dev.justinsmith.bjj.core.roleLabel
import dev.justinsmith.bjj.core.services.linkedVideos
import dev.justinsmith.bjj.core.situationName
import dev.justinsmith.bjj.core.techniqueDrill
import dev.justinsmith.bjj.design.components.BjjText
import dev.justinsmith.bjj.design.components.SoftBlock
import dev.justinsmith.bjj.design.graphics.ConceptArtwork
import dev.justinsmith.bjj.design.graphics.PositionArtwork
import dev.justinsmith.bjj.design.graphics.hasConceptArtwork
import dev.justinsmith.bjj.design.graphics.hasPositionArtwork
import dev.justinsmith.bjj.design.theme.BjjTheme
import dev.justinsmith.bjj.design.theme.Spacing
import dev.justinsmith.bjj.design.theme.Type
import dev.justinsmith.bjj.ui.common.CARD_PICTURE_WIDTH
import dev.justinsmith.bjj.ui.common.Direction
import dev.justinsmith.bjj.ui.common.ItemPicture
import dev.justinsmith.bjj.ui.common.TechniqueLinks
import dev.justinsmith.bjj.ui.common.TransitionArtwork
import dev.justinsmith.bjj.ui.common.TransitionList
import dev.justinsmith.bjj.ui.learn.PointList

/** The question side of a review card. */
class CardPrompt(val eyebrow: String, val question: String)

/** The question side of a review card, or null if the item no longer exists. */
fun cardPrompt(item: StudyItem, lib: Library): CardPrompt? = when (item.kind) {
    CardKind.Concept -> lib.conceptById[item.id]?.let {
        CardPrompt("Concept", "What is \"${it.name}\", and what are its key points?")
    }

    CardKind.Position -> lib.positionById[item.id]?.let {
        CardPrompt("Position", "${it.name}: what are the goals for each person?")
    }

    CardKind.Transition -> lib.transitionById[item.id]?.let {
        val from = lib.positionById[it.fromId]?.name ?: "unknown position"
        CardPrompt("${it.type.label} · from $from", "How do you do the ${it.name}?")
    }

    CardKind.Situation -> findSituation(lib, item.id)?.let {
        CardPrompt("Situation", "${situationName(it)}: what are your options?")
    }
}

/** A drawing on a card, kept to the width a card's picture gets. */
@Composable
private fun CardPicture(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(Modifier.widthIn(max = CARD_PICTURE_WIDTH).fillMaxWidth()) { content() }
    }
}

/**
 * A picture for the question side that does not give the answer away: where a technique starts. Other kinds of
 * card show nothing, because their picture is the answer.
 */
@Composable
fun CardCue(item: StudyItem, lib: Library, modifier: Modifier = Modifier) {
    if (item.kind != CardKind.Transition) return
    val from = lib.transitionById[item.id]?.let { lib.positionById[it.fromId] } ?: return
    if (!hasPositionArtwork(from.id)) return
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.s1)) {
        CardPicture { PositionArtwork(from.id) }
        BjjText(
            "You start here: ${from.name}.",
            Modifier.fillMaxWidth(),
            style = Type.small,
            color = BjjTheme.palette.muted,
            textAlign = TextAlign.Center,
        )
    }
}

/** The answer side of a review card: the item's picture, then what the student should have recalled. */
@Composable
fun CardAnswer(
    item: StudyItem,
    lib: Library,
    videos: List<VideoBookmark>,
    onOpenTechnique: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.s3)) {
        when (item.kind) {
            CardKind.Concept -> lib.conceptById[item.id]?.let { concept ->
                ItemPicture(linkedVideos(videos, ItemKind.Concept, concept.id), maxWidth = CARD_PICTURE_WIDTH) {
                    if (hasConceptArtwork(concept.id)) ConceptArtwork(concept.id)
                }
                BjjText(concept.summary, style = Type.lead)
                PointList(concept.keyPoints)
                if (concept.drill.isNotEmpty()) SoftBlock("Drill: ${concept.drill}")
            }

            CardKind.Position -> lib.positionById[item.id]?.let { position ->
                ItemPicture(linkedVideos(videos, ItemKind.Position, position.id), maxWidth = CARD_PICTURE_WIDTH) {
                    if (hasPositionArtwork(position.id)) PositionArtwork(position.id)
                }
                BjjText(position.summary, style = Type.lead)
                for (role in PositionRole.entries) {
                    BjjText(roleLabel(position.category, role), style = Type.h3)
                    PointList(roleGoals(position, role))
                }
            }

            CardKind.Transition -> lib.transitionById[item.id]?.let { transition ->
                val to = lib.positionById[transition.toId]?.name
                val drill = techniqueDrill(transition)
                ItemPicture(linkedVideos(videos, ItemKind.Transition, transition.id), maxWidth = CARD_PICTURE_WIDTH) {
                    TransitionArtwork(transition, lib.positionById)
                }
                if (to != null) BjjText("Ends in: $to", style = Type.lead)
                PointList(transition.steps, numbered = true)
                if (transition.commonMistakes.isNotEmpty()) {
                    BjjText("Common mistakes", style = Type.h3)
                    PointList(transition.commonMistakes)
                }
                if (drill.isNotEmpty()) SoftBlock("Drill: $drill")
                TechniqueLinks(lib, transition.id, onOpenTechnique)
            }

            CardKind.Situation -> findSituation(lib, item.id)?.let { situation ->
                if (hasPositionArtwork(situation.position.id)) CardPicture { PositionArtwork(situation.position.id) }
                BjjText("What you want", style = Type.h3)
                PointList(roleGoals(situation.position, situation.role))
                BjjText("What you can do", style = Type.h3)
                TransitionList(situation.options, lib.positionById, Direction.Out, onOpenTechnique)
            }
        }
    }
}

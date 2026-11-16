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
import dev.justinsmith.bjj.resources.Res
import dev.justinsmith.bjj.resources.review_card_concept
import dev.justinsmith.bjj.resources.review_card_position
import dev.justinsmith.bjj.resources.review_card_situation
import dev.justinsmith.bjj.resources.review_card_technique_from
import dev.justinsmith.bjj.resources.review_card_unknown_position
import dev.justinsmith.bjj.resources.review_common_mistakes
import dev.justinsmith.bjj.resources.review_drill
import dev.justinsmith.bjj.resources.review_ends_in
import dev.justinsmith.bjj.resources.review_question_concept
import dev.justinsmith.bjj.resources.review_question_position
import dev.justinsmith.bjj.resources.review_question_situation
import dev.justinsmith.bjj.resources.review_question_technique
import dev.justinsmith.bjj.resources.review_start_here
import dev.justinsmith.bjj.resources.review_what_you_can_do
import dev.justinsmith.bjj.resources.review_what_you_want
import dev.justinsmith.bjj.ui.common.CARD_PICTURE_WIDTH
import dev.justinsmith.bjj.ui.common.Direction
import dev.justinsmith.bjj.ui.common.ItemPicture
import dev.justinsmith.bjj.ui.common.TechniqueLinks
import dev.justinsmith.bjj.ui.common.TransitionArtwork
import dev.justinsmith.bjj.ui.common.TransitionList
import dev.justinsmith.bjj.ui.learn.PointList
import org.jetbrains.compose.resources.stringResource

/** The question side of a review card. */
class CardPrompt(val eyebrow: String, val question: String)

/** The question side of a review card, or null if the item no longer exists. */
@Composable
fun cardPrompt(item: StudyItem, lib: Library): CardPrompt? = when (item.kind) {
    CardKind.Concept -> lib.conceptById[item.id]?.let {
        CardPrompt(
            stringResource(Res.string.review_card_concept),
            stringResource(Res.string.review_question_concept, it.name),
        )
    }

    CardKind.Position -> lib.positionById[item.id]?.let {
        CardPrompt(
            stringResource(Res.string.review_card_position),
            stringResource(Res.string.review_question_position, it.name),
        )
    }

    CardKind.Transition -> lib.transitionById[item.id]?.let {
        val from = lib.positionById[it.fromId]?.name ?: stringResource(Res.string.review_card_unknown_position)
        CardPrompt(
            stringResource(Res.string.review_card_technique_from, it.type.label, from),
            stringResource(Res.string.review_question_technique, it.name),
        )
    }

    CardKind.Situation -> findSituation(lib, item.id)?.let {
        CardPrompt(
            stringResource(Res.string.review_card_situation),
            stringResource(Res.string.review_question_situation, situationName(it)),
        )
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
            stringResource(Res.string.review_start_here, from.name),
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
                if (concept.drill.isNotEmpty()) SoftBlock(stringResource(Res.string.review_drill, concept.drill))
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
                if (to != null) BjjText(stringResource(Res.string.review_ends_in, to), style = Type.lead)
                PointList(transition.steps, numbered = true)
                if (transition.commonMistakes.isNotEmpty()) {
                    BjjText(stringResource(Res.string.review_common_mistakes), style = Type.h3)
                    PointList(transition.commonMistakes)
                }
                if (drill.isNotEmpty()) SoftBlock(stringResource(Res.string.review_drill, drill))
                TechniqueLinks(lib, transition.id, onOpenTechnique)
            }

            CardKind.Situation -> findSituation(lib, item.id)?.let { situation ->
                if (hasPositionArtwork(situation.position.id)) CardPicture { PositionArtwork(situation.position.id) }
                BjjText(stringResource(Res.string.review_what_you_want), style = Type.h3)
                PointList(roleGoals(situation.position, situation.role))
                BjjText(stringResource(Res.string.review_what_you_can_do), style = Type.h3)
                TransitionList(situation.options, lib.positionById, Direction.Out, onOpenTechnique)
            }
        }
    }
}

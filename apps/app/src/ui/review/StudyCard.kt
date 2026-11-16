package dev.justinsmith.bjj.ui.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.justinsmith.bjj.core.CardKind
import dev.justinsmith.bjj.core.Concept
import dev.justinsmith.bjj.core.ItemKind
import dev.justinsmith.bjj.core.Library
import dev.justinsmith.bjj.core.Position
import dev.justinsmith.bjj.core.PositionRole
import dev.justinsmith.bjj.core.StudyItem
import dev.justinsmith.bjj.core.Transition
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
import dev.justinsmith.bjj.resources.review_what_you_can_do
import dev.justinsmith.bjj.resources.review_what_you_want
import dev.justinsmith.bjj.ui.common.CARD_PICTURE_WIDTH
import dev.justinsmith.bjj.ui.common.Direction
import dev.justinsmith.bjj.ui.common.ItemPicture
import dev.justinsmith.bjj.ui.common.TechniqueLinks
import dev.justinsmith.bjj.ui.common.TechniqueLinksUi
import dev.justinsmith.bjj.ui.common.TechniqueRow
import dev.justinsmith.bjj.ui.common.TransitionList
import dev.justinsmith.bjj.ui.common.techniqueLinksUi
import dev.justinsmith.bjj.ui.common.techniqueRows
import dev.justinsmith.bjj.ui.learn.PointList
import org.jetbrains.compose.resources.stringResource

/** A study card with everything either side of it shows, looked up already. */
sealed interface CardFace {
    val item: StudyItem

    data class ConceptCard(override val item: StudyItem, val concept: Concept, val videos: List<VideoBookmark>) :
        CardFace

    data class PositionCard(override val item: StudyItem, val position: Position, val videos: List<VideoBookmark>) :
        CardFace

    data class TechniqueCard(
        override val item: StudyItem,
        val transition: Transition,
        /** The name of where it starts, or null when that position is gone. */
        val fromName: String?,
        /** The name of where it leads, or null when that position is gone or it leads nowhere. */
        val toName: String?,
        val drill: String,
        val links: TechniqueLinksUi,
        val videos: List<VideoBookmark>,
    ) : CardFace

    data class SituationCard(
        override val item: StudyItem,
        val name: String,
        val goals: List<String>,
        val options: List<TechniqueRow>,
    ) : CardFace
}

/** The card for a study item, or null if what it is about no longer exists. */
fun cardFace(lib: Library, videos: List<VideoBookmark>, item: StudyItem): CardFace? = when (item.kind) {
    CardKind.Concept -> lib.conceptById[item.id]?.let {
        CardFace.ConceptCard(item, it, linkedVideos(videos, ItemKind.Concept, it.id))
    }

    CardKind.Position -> lib.positionById[item.id]?.let {
        CardFace.PositionCard(item, it, linkedVideos(videos, ItemKind.Position, it.id))
    }

    CardKind.Transition -> lib.transitionById[item.id]?.let { transition ->
        val from = lib.positionById[transition.fromId]
        CardFace.TechniqueCard(
            item = item,
            transition = transition,
            fromName = from?.name,
            toName = lib.positionById[transition.toId]?.name,
            drill = techniqueDrill(transition),
            links = techniqueLinksUi(lib, transition.id),
            videos = linkedVideos(videos, ItemKind.Transition, transition.id),
        )
    }

    CardKind.Situation -> findSituation(lib, item.id)?.let {
        CardFace.SituationCard(
            item = item,
            name = situationName(it),
            goals = roleGoals(it.position, it.role),
            options = techniqueRows(lib, it.options, Direction.Out),
        )
    }
}

/** The question side of a review card. */
class CardPrompt(val eyebrow: String, val question: String)

@Composable
fun cardPrompt(card: CardFace): CardPrompt = when (card) {
    is CardFace.ConceptCard -> CardPrompt(
        stringResource(Res.string.review_card_concept),
        stringResource(Res.string.review_question_concept, card.concept.name),
    )

    is CardFace.PositionCard -> CardPrompt(
        stringResource(Res.string.review_card_position),
        stringResource(Res.string.review_question_position, card.position.name),
    )

    is CardFace.TechniqueCard -> {
        val from = card.fromName ?: stringResource(Res.string.review_card_unknown_position)
        CardPrompt(
            stringResource(Res.string.review_card_technique_from, card.transition.type.label, from),
            stringResource(Res.string.review_question_technique, card.transition.name),
        )
    }

    is CardFace.SituationCard -> CardPrompt(
        stringResource(Res.string.review_card_situation),
        stringResource(Res.string.review_question_situation, card.name),
    )
}

/** The answer side of a review card: the item's video when it has one, then what the student should have recalled. */
@Composable
fun CardAnswer(card: CardFace, onOpenTechnique: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.s3)) {
        when (card) {
            is CardFace.ConceptCard -> {
                val concept = card.concept
                ItemPicture(card.videos, maxWidth = CARD_PICTURE_WIDTH)
                BjjText(concept.summary, style = Type.lead)
                PointList(concept.keyPoints)
                if (concept.drill.isNotEmpty()) SoftBlock(stringResource(Res.string.review_drill, concept.drill))
            }

            is CardFace.PositionCard -> {
                val position = card.position
                ItemPicture(card.videos, maxWidth = CARD_PICTURE_WIDTH)
                BjjText(position.summary, style = Type.lead)
                for (role in PositionRole.entries) {
                    BjjText(roleLabel(position.category, role), style = Type.h3)
                    PointList(roleGoals(position, role))
                }
            }

            is CardFace.TechniqueCard -> {
                val transition = card.transition
                ItemPicture(card.videos, maxWidth = CARD_PICTURE_WIDTH)
                if (card.toName != null) {
                    BjjText(stringResource(Res.string.review_ends_in, card.toName), style = Type.lead)
                }
                PointList(transition.steps, numbered = true)
                if (transition.commonMistakes.isNotEmpty()) {
                    BjjText(stringResource(Res.string.review_common_mistakes), style = Type.h3)
                    PointList(transition.commonMistakes)
                }
                if (card.drill.isNotEmpty()) SoftBlock(stringResource(Res.string.review_drill, card.drill))
                TechniqueLinks(card.links, onOpenTechnique)
            }

            is CardFace.SituationCard -> {
                BjjText(stringResource(Res.string.review_what_you_want), style = Type.h3)
                PointList(card.goals)
                BjjText(stringResource(Res.string.review_what_you_can_do), style = Type.h3)
                TransitionList(card.options, onOpenTechnique)
            }
        }
    }
}

package dev.justinsmith.bjj.ui.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.justinsmith.bjj.core.PositionRole
import dev.justinsmith.bjj.core.roleGoals
import dev.justinsmith.bjj.core.roleLabel
import dev.justinsmith.bjj.design.components.BjjText
import dev.justinsmith.bjj.design.components.SoftBlock
import dev.justinsmith.bjj.design.theme.Spacing
import dev.justinsmith.bjj.design.theme.Type
import dev.justinsmith.bjj.resources.Res
import dev.justinsmith.bjj.resources.review_common_mistakes
import dev.justinsmith.bjj.resources.review_drill
import dev.justinsmith.bjj.resources.review_ends_in
import dev.justinsmith.bjj.resources.review_what_you_can_do
import dev.justinsmith.bjj.resources.review_what_you_want
import dev.justinsmith.bjj.ui.common.CARD_PICTURE_WIDTH
import dev.justinsmith.bjj.ui.common.ItemPicture
import dev.justinsmith.bjj.ui.common.PointList
import dev.justinsmith.bjj.ui.common.TechniqueLinks
import dev.justinsmith.bjj.ui.common.TransitionList
import org.jetbrains.compose.resources.stringResource

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

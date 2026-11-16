package dev.justinsmith.bjj.ui.review

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.justinsmith.bjj.core.Deck
import dev.justinsmith.bjj.design.components.BjjText
import dev.justinsmith.bjj.design.components.Button
import dev.justinsmith.bjj.design.components.ButtonKind
import dev.justinsmith.bjj.design.components.ButtonSize
import dev.justinsmith.bjj.design.components.Card
import dev.justinsmith.bjj.design.components.Divider
import dev.justinsmith.bjj.design.components.NumberSlider
import dev.justinsmith.bjj.design.components.PageTitle
import dev.justinsmith.bjj.design.components.ToggleChip
import dev.justinsmith.bjj.design.icons.BjjIcons
import dev.justinsmith.bjj.design.theme.BjjTheme
import dev.justinsmith.bjj.design.theme.Sizes
import dev.justinsmith.bjj.design.theme.Spacing
import dev.justinsmith.bjj.design.theme.Type
import dev.justinsmith.bjj.nav.ReviewSession
import dev.justinsmith.bjj.nav.ReviewTab
import dev.justinsmith.bjj.resources.Res
import dev.justinsmith.bjj.resources.common_blue_belt_test
import dev.justinsmith.bjj.resources.common_concepts
import dev.justinsmith.bjj.resources.common_positions
import dev.justinsmith.bjj.resources.review_cards_ready
import dev.justinsmith.bjj.resources.review_deck_all
import dev.justinsmith.bjj.resources.review_deck_situations
import dev.justinsmith.bjj.resources.review_deck_techniques
import dev.justinsmith.bjj.resources.review_empty_deck
import dev.justinsmith.bjj.resources.review_empty_raise_slider
import dev.justinsmith.bjj.resources.review_empty_title
import dev.justinsmith.bjj.resources.review_from_class
import dev.justinsmith.bjj.resources.review_how_to
import dev.justinsmith.bjj.resources.review_lesson_chip
import dev.justinsmith.bjj.resources.review_new_cards
import dev.justinsmith.bjj.resources.review_new_cards_value
import dev.justinsmith.bjj.resources.review_split
import dev.justinsmith.bjj.resources.review_split_more
import dev.justinsmith.bjj.resources.review_start_session
import dev.justinsmith.bjj.resources.review_title
import dev.justinsmith.bjj.resources.review_what_to_study
import dev.justinsmith.bjj.ui.common.EmptyState
import dev.justinsmith.bjj.ui.common.Loading
import dev.justinsmith.bjj.ui.common.Nav
import dev.justinsmith.bjj.ui.common.Page
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

val Deck.label: StringResource
    get() = when (this) {
        Deck.All -> Res.string.review_deck_all
        Deck.Concept -> Res.string.common_concepts
        Deck.Position -> Res.string.common_positions
        Deck.Transition -> Res.string.review_deck_techniques
        Deck.Situation -> Res.string.review_deck_situations
        Deck.BlueBelt -> Res.string.common_blue_belt_test
    }

/** The most new cards a session can be asked to bring in. */
private const val MAX_NEW_CARDS = 20

@Composable
fun ReviewRoute(key: ReviewTab, nav: Nav) {
    val viewModel = koinViewModel<ReviewViewModel> { parametersOf(key) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    when (val ready = state) {
        ReviewUiState.Loading -> Loading()

        is ReviewUiState.Ready -> ReviewScreen(
            state = ready,
            onDeck = viewModel::chooseDeck,
            onNewLimit = viewModel::setNewLimit,
            onStart = {
                val choice = ready.choice
                nav.open(ReviewSession(choice.deck.key, choice.lessonId, choice.newLimit))
            },
        )
    }
}

/** Review before a session: how many cards are ready, what to study, and how many new cards to bring in. */
@Composable
fun ReviewScreen(
    state: ReviewUiState.Ready,
    onDeck: (Deck) -> Unit,
    onNewLimit: (Int) -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val deck = state.choice.deck
    val newLimit = state.choice.newLimit
    val lesson = state.lesson
    val muted = BjjTheme.palette.muted
    Page(modifier, maxWidth = Sizes.readingMax) {
        PageTitle(stringResource(Res.string.review_title))
        Card {
            if (state.ready == 0) {
                EmptyState(
                    icon = BjjIcons.Check,
                    title = stringResource(Res.string.review_empty_title),
                    text = stringResource(
                        if (state.newCount + state.moreNew > 0) {
                            Res.string.review_empty_raise_slider
                        } else {
                            Res.string.review_empty_deck
                        },
                    ),
                )
            } else {
                val newCount = state.newCount
                val more = state.moreNew
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.s1)) {
                    val ready = state.ready
                    BjjText(pluralStringResource(Res.plurals.review_cards_ready, ready, ready), style = Type.display)
                    val split = if (more > 0) {
                        stringResource(Res.string.review_split_more, state.due, newCount, more)
                    } else {
                        stringResource(Res.string.review_split, state.due, newCount)
                    }
                    BjjText(split, color = muted)
                    if (state.fromClass > 0) {
                        val fromClass = state.fromClass
                        val line = pluralStringResource(Res.plurals.review_from_class, fromClass, fromClass)
                        BjjText(line, style = Type.small, color = muted)
                    }
                }
                val start = stringResource(Res.string.review_start_session)
                Button(start, onStart, Modifier.fillMaxWidth(), ButtonKind.Cta, ButtonSize.Large)
            }
            Divider()
            BjjText(stringResource(Res.string.review_what_to_study), style = Type.label)
            // One row at every width: on a phone it scrolls sideways instead of wrapping.
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
            ) {
                if (lesson != null) {
                    ToggleChip(
                        stringResource(Res.string.review_lesson_chip, lesson.number, lesson.title),
                        checked = true,
                        onChange = {
                        },
                    )
                }
                for (option in Deck.entries) {
                    ToggleChip(
                        stringResource(option.label),
                        checked = lesson == null && deck == option,
                        onChange = { onDeck(option) },
                    )
                }
            }
            BjjText(stringResource(Res.string.review_new_cards_value, newLimit), style = Type.label)
            val slider = stringResource(Res.string.review_new_cards)
            NumberSlider(newLimit, onNewLimit, min = 0, max = MAX_NEW_CARDS, label = slider)
        }
        BjjText(stringResource(Res.string.review_how_to), style = Type.small, color = muted)
    }
}

package dev.justinsmith.bjj.ui.review

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.justinsmith.bjj.core.DEFAULT_NEW_CARDS
import dev.justinsmith.bjj.core.Deck
import dev.justinsmith.bjj.core.FOUNDATION_LESSONS
import dev.justinsmith.bjj.core.RECENT_CLASS_COUNT
import dev.justinsmith.bjj.core.StudyQueue
import dev.justinsmith.bjj.core.buildLessonQueue
import dev.justinsmith.bjj.core.buildQueue
import dev.justinsmith.bjj.core.findLesson
import dev.justinsmith.bjj.core.services.Snapshot
import dev.justinsmith.bjj.core.studyFirst
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
import dev.justinsmith.bjj.ui.common.Nav
import dev.justinsmith.bjj.ui.common.Page
import dev.justinsmith.bjj.ui.common.WithSnapshot
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

val Deck.label: StringResource
    get() = when (this) {
        Deck.All -> Res.string.review_deck_all
        Deck.Concept -> Res.string.common_concepts
        Deck.Position -> Res.string.common_positions
        Deck.Transition -> Res.string.review_deck_techniques
        Deck.Situation -> Res.string.review_deck_situations
        Deck.BlueBelt -> Res.string.common_blue_belt_test
    }

/** The deck a key names; anything unrecognised is everything. */
fun deckOf(key: String?): Deck = Deck.entries.firstOrNull { it.key == key } ?: Deck.All

/** The most new cards a session can be asked to bring in. */
private const val MAX_NEW_CARDS = 20

/** The session a deck, or a lesson in its place, would hold now. */
fun queueFor(snapshot: Snapshot, deck: Deck, lessonId: String?, newLimit: Int, now: Long): StudyQueue {
    val lib = snapshot.library
    val lesson = findLesson(lessonId)
    return if (lesson != null) {
        buildLessonQueue(lib, lesson, newLimit, now)
    } else {
        buildQueue(lib, deck, newLimit, now, studyFirst(lib, snapshot.trainingLog, RECENT_CLASS_COUNT))
    }
}

@Composable
fun ReviewRoute(deckKey: String?, lessonId: String?, nav: Nav) {
    WithSnapshot { snapshot, viewModel ->
        // A list page can open the review on its own deck, and Lessons on one lesson; picking a deck leaves it.
        var deck by rememberSaveable { mutableStateOf(deckOf(deckKey)) }
        var lesson by rememberSaveable { mutableStateOf(lessonId) }
        var newLimit by rememberSaveable { mutableIntStateOf(DEFAULT_NEW_CARDS) }
        ReviewScreen(
            preview = queueFor(snapshot, deck, lesson, newLimit, viewModel.now()),
            deck = deck,
            lessonId = lesson,
            newLimit = newLimit,
            onDeck = {
                lesson = null
                deck = it
            },
            onNewLimit = { newLimit = it },
            onStart = { nav.open(ReviewSession(deck.key, lesson, newLimit)) },
        )
    }
}

/** Review before a session: how many cards are ready, what to study, and how many new cards to bring in. */
@Composable
fun ReviewScreen(
    preview: StudyQueue,
    deck: Deck,
    lessonId: String?,
    newLimit: Int,
    onDeck: (Deck) -> Unit,
    onNewLimit: (Int) -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val muted = BjjTheme.palette.muted
    val lesson = findLesson(lessonId)
    Page(modifier, maxWidth = Sizes.readingMax) {
        PageTitle(stringResource(Res.string.review_title))
        Card {
            if (preview.queue.isEmpty()) {
                EmptyState(
                    icon = BjjIcons.Check,
                    title = stringResource(Res.string.review_empty_title),
                    text = stringResource(
                        if (preview.fresh.isNotEmpty()) {
                            Res.string.review_empty_raise_slider
                        } else {
                            Res.string.review_empty_deck
                        },
                    ),
                )
            } else {
                val newCount = minOf(newLimit, preview.fresh.size)
                val more = preview.fresh.size - newCount
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.s1)) {
                    val ready = preview.queue.size
                    BjjText(pluralStringResource(Res.plurals.review_cards_ready, ready, ready), style = Type.display)
                    val split = if (more > 0) {
                        stringResource(Res.string.review_split_more, preview.due.size, newCount, more)
                    } else {
                        stringResource(Res.string.review_split, preview.due.size, newCount)
                    }
                    BjjText(split, color = muted)
                    if (preview.fromClass > 0) {
                        val fromClass = preview.fromClass
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
                    val number = FOUNDATION_LESSONS.indexOf(lesson) + 1
                    ToggleChip(
                        stringResource(Res.string.review_lesson_chip, number, lesson.title),
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

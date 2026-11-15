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
import dev.justinsmith.bjj.core.plural
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
import dev.justinsmith.bjj.ui.common.EmptyState
import dev.justinsmith.bjj.ui.common.Nav
import dev.justinsmith.bjj.ui.common.Page
import dev.justinsmith.bjj.ui.common.WithSnapshot

val Deck.label: String
    get() = when (this) {
        Deck.All -> "Everything"
        Deck.Concept -> "Concepts"
        Deck.Position -> "Positions"
        Deck.Transition -> "Techniques"
        Deck.Situation -> "Situations"
        Deck.BlueBelt -> "Blue belt test"
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
        PageTitle("Review")
        Card {
            if (preview.queue.isEmpty()) {
                EmptyState(
                    icon = BjjIcons.Check,
                    title = "Nothing to review right now",
                    text = if (preview.fresh.isNotEmpty()) {
                        "No cards are due. Raise the new-cards slider to learn something new."
                    } else {
                        "No cards are due in this deck. Come back later, or pick another deck."
                    },
                )
            } else {
                val newCount = minOf(newLimit, preview.fresh.size)
                val more = preview.fresh.size - newCount
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.s1)) {
                    BjjText("${plural(preview.queue.size, "card")} ready", style = Type.display)
                    BjjText(
                        "${preview.due.size} due · $newCount new" + if (more > 0) " · $more more not started" else "",
                        color = muted,
                    )
                    if (preview.fromClass > 0) {
                        val line = if (preview.fromClass == 1) {
                            "1 new card is from your recent classes, so it comes first."
                        } else {
                            "${preview.fromClass} of the new cards are from your recent classes, so they come first."
                        }
                        BjjText(line, style = Type.small, color = muted)
                    }
                }
                Button("Start session", onStart, Modifier.fillMaxWidth(), ButtonKind.Cta, ButtonSize.Large)
            }
            Divider()
            BjjText("What to study", style = Type.label)
            // One row at every width: on a phone it scrolls sideways instead of wrapping.
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
            ) {
                if (lesson != null) {
                    ToggleChip(
                        "Lesson ${FOUNDATION_LESSONS.indexOf(
                            lesson,
                        ) + 1}: ${lesson.title}",
                        checked = true,
                        onChange = {
                        },
                    )
                }
                for (option in Deck.entries) {
                    ToggleChip(option.label, checked = lesson == null && deck == option, onChange = { onDeck(option) })
                }
            }
            BjjText("New cards this session: $newLimit", style = Type.label)
            NumberSlider(newLimit, onNewLimit, min = 0, max = MAX_NEW_CARDS, label = "New cards this session")
        }
        BjjText(
            "Read the question, answer it in your head (or out loud), then reveal and rate how well you knew it. " +
                "Cards you know well come back less often.",
            style = Type.small,
            color = muted,
        )
    }
}

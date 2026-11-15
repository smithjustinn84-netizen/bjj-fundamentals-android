package dev.justinsmith.bjj.ui.review

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.justinsmith.bjj.core.Grade
import dev.justinsmith.bjj.core.Library
import dev.justinsmith.bjj.core.ReviewState
import dev.justinsmith.bjj.core.StudyItem
import dev.justinsmith.bjj.core.cardName
import dev.justinsmith.bjj.core.describeInterval
import dev.justinsmith.bjj.core.label
import dev.justinsmith.bjj.core.newReviewState
import dev.justinsmith.bjj.core.plural
import dev.justinsmith.bjj.core.reviewOf
import dev.justinsmith.bjj.core.services.Snapshot
import dev.justinsmith.bjj.core.services.studyStats
import dev.justinsmith.bjj.design.components.BjjText
import dev.justinsmith.bjj.design.components.Button
import dev.justinsmith.bjj.design.components.ButtonKind
import dev.justinsmith.bjj.design.components.ButtonSize
import dev.justinsmith.bjj.design.components.Card
import dev.justinsmith.bjj.design.components.Eyebrow
import dev.justinsmith.bjj.design.components.Icon
import dev.justinsmith.bjj.design.components.LinkButton
import dev.justinsmith.bjj.design.components.PageTitle
import dev.justinsmith.bjj.design.components.ProgressBar
import dev.justinsmith.bjj.design.components.Wrap
import dev.justinsmith.bjj.design.components.block
import dev.justinsmith.bjj.design.components.pressBlock
import dev.justinsmith.bjj.design.components.pressShrink
import dev.justinsmith.bjj.design.icons.BjjIcons
import dev.justinsmith.bjj.design.reducedMotion
import dev.justinsmith.bjj.design.theme.BjjTheme
import dev.justinsmith.bjj.design.theme.Motion
import dev.justinsmith.bjj.design.theme.Radius
import dev.justinsmith.bjj.design.theme.Sizes
import dev.justinsmith.bjj.design.theme.Spacing
import dev.justinsmith.bjj.design.theme.Type
import dev.justinsmith.bjj.design.theme.gradeColor
import dev.justinsmith.bjj.design.theme.onGradeColor
import dev.justinsmith.bjj.nav.LearnSection
import dev.justinsmith.bjj.nav.LearnTab
import dev.justinsmith.bjj.nav.ReviewSession
import dev.justinsmith.bjj.nav.TodayTab
import dev.justinsmith.bjj.nav.TransitionDetail
import dev.justinsmith.bjj.nav.studyItemKey
import dev.justinsmith.bjj.ui.common.Loading
import dev.justinsmith.bjj.ui.common.Nav
import dev.justinsmith.bjj.ui.common.Page
import dev.justinsmith.bjj.ui.common.WithSnapshot
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ReviewSessionRoute(key: ReviewSession, nav: Nav) {
    val session = koinViewModel<ReviewSessionViewModel>()
    val state by session.state.collectAsStateWithLifecycle()
    WithSnapshot { snapshot, app ->
        val lib = snapshot.library
        val queueNow = { queueFor(snapshot, deckOf(key.deck), key.lessonId, key.newCards, app.now()).queue }
        LaunchedEffect(Unit) { session.startIfIdle(queueNow()) }
        val current = state.current
        // If the current card's item was deleted mid-session, drop it.
        val gone = current != null && cardName(lib, current) == null
        LaunchedEffect(current, gone) { if (current != null && gone) session.drop(current) }
        when {
            state.finished -> SessionComplete(
                doneCount = state.doneCount,
                tally = state.tally,
                streakDays = studyStats(snapshot, app.now(), app.zone).streakDays,
                moreWaiting = queueNow().size,
                onAgain = { session.restart(queueNow()) },
                onDone = { nav.selectTab(TodayTab) },
                onPositions = { nav.selectTab(LearnTab(LearnSection.Positions)) },
            )

            current == null || gone -> Loading()

            else -> {
                val now = app.now()
                ActiveSession(
                    state = state,
                    current = current,
                    snapshot = snapshot,
                    card = reviewOf(lib, current.kind, current.id) ?: newReviewState(current.kind, current.id, now),
                    now = now,
                    onEnd = nav.back,
                    onReveal = session::reveal,
                    onGrade = session::grade,
                    onOpen = { nav.open(it) },
                )
            }
        }
    }
}

@Composable
private fun ActiveSession(
    state: SessionUiState,
    current: StudyItem,
    snapshot: Snapshot,
    card: ReviewState,
    now: Long,
    onEnd: () -> Unit,
    onReveal: () -> Unit,
    onGrade: (Grade) -> Unit,
    onOpen: (androidx.navigation3.runtime.NavKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lib = snapshot.library
    val total = state.doneCount + state.queue.orEmpty().size
    val p = BjjTheme.palette
    Column(modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            Modifier.widthIn(
                max = Sizes.readingMax,
            ).fillMaxWidth().padding(horizontal = Spacing.pageGutter, vertical = Spacing.s2),
            horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val interactions = remember { MutableInteractionSource() }
            Box(
                Modifier
                    .size(Sizes.tap)
                    .pressShrink(interactions)
                    .clickable(interactions, indication = null, role = Role.Button, onClick = onEnd)
                    .semantics { contentDescription = "End session" },
                contentAlignment = Alignment.Center,
            ) { Icon(BjjIcons.Close, description = null) }
            ProgressBar(state.doneCount.toFloat() / total.coerceAtLeast(1), Modifier.weight(1f))
            BjjText("${state.doneCount + 1} of $total", style = Type.small, color = p.muted)
        }
        Column(
            Modifier
                .weight(1f)
                .widthIn(max = Sizes.readingMax)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.pageGutter, vertical = Spacing.s2),
        ) {
            val still = reducedMotion()
            // Keyed per card and per answer given, so each new card (or a repeated one) slides in afresh.
            AnimatedContent(
                targetState = Triple(current, state.doneCount, state.revealed),
                transitionSpec = {
                    if (still) {
                        fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                    } else if (initialState.second != targetState.second) {
                        (
                            slideInHorizontally(tween(Motion.BASE)) {
                                it / ENTER_FROM
                            } + fadeIn(tween(Motion.BASE))
                            ) togetherWith
                            (slideOutHorizontally(tween(Motion.BASE)) { -it / LEAVE_TO } + fadeOut(tween(Motion.BASE)))
                    } else {
                        fadeIn(tween(Motion.BASE)) togetherWith fadeOut(tween(Motion.FAST))
                    }
                },
            ) { (item, _, revealed) ->
                FlashCard(item, revealed, lib, snapshot, onOpen)
            }
        }
        // Pinned to the bottom of the screen, under the thumb, however long the card is.
        Dock(state, card, now, onReveal, onGrade)
    }
}

@Composable
private fun FlashCard(
    item: StudyItem,
    revealed: Boolean,
    lib: Library,
    snapshot: Snapshot,
    onOpen: (androidx.navigation3.runtime.NavKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    val prompt = cardPrompt(item, lib) ?: return
    Card(modifier) {
        Eyebrow(prompt.eyebrow)
        BjjText(
            prompt.question,
            Modifier.semantics { heading() },
            style = Type.h2.copy(
                fontSize =
                Type.display.fontSize * QUESTION_SCALE,
            ),
        )
        if (revealed) {
            CardAnswer(
                item,
                lib,
                snapshot.videos,
                { onOpen(TransitionDetail(it)) },
                Modifier.semantics {
                    liveRegion =
                        LiveRegionMode.Polite
                },
            )
            LinkButton("Open full page", { onOpen(studyItemKey(item)) })
        } else {
            CardCue(item, lib)
        }
    }
}

private const val QUESTION_SCALE = 0.7f

// A new card comes in from a quarter of the way across, and the old one leaves by a third.
private const val ENTER_FROM = 4
private const val LEAVE_TO = 3

@Composable
private fun Dock(state: SessionUiState, card: ReviewState, now: Long, onReveal: () -> Unit, onGrade: (Grade) -> Unit) {
    val p = BjjTheme.palette
    Column(Modifier.fillMaxWidth().background(p.surface), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().height(Sizes.line).background(p.outline))
        Column(
            Modifier
                .widthIn(max = Sizes.readingMax)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                .padding(horizontal = Spacing.pageGutter, vertical = Spacing.s3),
            verticalArrangement = Arrangement.spacedBy(Spacing.s2),
        ) {
            if (state.revealed) {
                BjjText("How well did you know it?", style = Type.small, color = p.muted)
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                    for (grade in Grade.entries) {
                        GradeButton(grade, describeInterval(card, grade, now), !state.saving, {
                            onGrade(grade)
                        }, Modifier.weight(1f))
                    }
                }
            } else {
                BjjText("Answer in your head or out loud, then check.", style = Type.small, color = p.muted)
                Button("Show answer", onReveal, Modifier.fillMaxWidth(), ButtonKind.Primary, ButtonSize.Large)
            }
        }
    }
}

/** A grade as a solid block in its colour, with when the card would come back underneath. */
@Composable
private fun GradeButton(
    grade: Grade,
    interval: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = BjjTheme.palette
    val interactions = remember { MutableInteractionSource() }
    Column(
        modifier
            .pressBlock(interactions, p.gradeColor(grade), p.outline, Radius.md, Sizes.edgeLift)
            .clickable(interactions, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .defaultMinSize(minHeight = GRADE_HEIGHT)
            .padding(vertical = Spacing.s2),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BjjText(grade.label, style = Type.button, color = p.onGradeColor(grade), maxLines = 1)
        BjjText(interval, style = Type.small, color = p.onGradeColor(grade), maxLines = 1)
    }
}

private val GRADE_HEIGHT = 56.dp

/** How the session's answers split across the four grades, as one stacked bar with a legend. */
@Composable
private fun GradeBreakdown(tally: Map<Grade, Int>, modifier: Modifier = Modifier) {
    val p = BjjTheme.palette
    val total = tally.values.sum().coerceAtLeast(1)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
        Box(
            Modifier.fillMaxWidth().height(Sizes.bar).drawBehind {
                var x = 0f
                for (grade in Grade.entries) {
                    val width = size.width * (tally[grade] ?: 0) / total
                    drawRect(p.gradeColor(grade), Offset(x, 0f), Size(width, size.height))
                    x += width
                }
                block(Color.Transparent, p.outline, 0.dp)
            },
        )
        Wrap {
            for (grade in Grade.entries) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s1),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val color = p.gradeColor(grade)
                    Box(Modifier.size(Sizes.dot).drawBehind { block(color, p.outline, Radius.sm, Sizes.hairline) })
                    BjjText("${grade.label} ${tally[grade] ?: 0}", style = Type.small)
                }
            }
        }
    }
}

@Composable
private fun SessionComplete(
    doneCount: Int,
    tally: Map<Grade, Int>,
    streakDays: Int,
    /** How many cards another round with the same deck and setting would hold. */
    moreWaiting: Int,
    onAgain: () -> Unit,
    onDone: () -> Unit,
    onPositions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = BjjTheme.palette
    Page(modifier, maxWidth = Sizes.readingMax) {
        Card {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(MARK).drawBehind {
                        block(p.ok, p.outline, Radius.pill)
                    },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(BjjIcons.Check, description = null, tint = p.onStatus, size = MARK_ICON)
                }
            }
            PageTitle("Session complete")
            BjjText("You reviewed ${plural(doneCount, "card")}. Nice work.", style = Type.lead)
            GradeBreakdown(tally)
            if (streakDays > 0) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s1),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(BjjIcons.Flame, description = null, tint = p.belt)
                    BjjText("${plural(streakDays, "day")} in a row", style = Type.strong)
                }
            }
            BjjText(
                if (moreWaiting > 0) {
                    "${plural(moreWaiting, "more card")} ready if you want another round."
                } else {
                    "Nothing else is due. Come back tomorrow."
                },
                color = p.muted,
                textAlign = TextAlign.Start,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                // Another round is offered only when one exists; otherwise the next step is to leave.
                if (moreWaiting > 0) {
                    Button("Keep going", onAgain, Modifier.weight(1f), ButtonKind.Primary)
                    Button("Done for now", onDone, Modifier.weight(1f))
                } else {
                    Button("Done", onDone, Modifier.weight(1f), ButtonKind.Primary)
                    Button("Positions", onPositions, Modifier.weight(1f))
                }
            }
        }
    }
}

private val MARK = 72.dp
private val MARK_ICON = 40.dp

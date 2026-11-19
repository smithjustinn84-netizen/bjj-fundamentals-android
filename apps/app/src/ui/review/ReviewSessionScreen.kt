package dev.justinsmith.bjj.ui.review

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
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
import androidx.compose.ui.graphics.graphicsLayer
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
import dev.justinsmith.bjj.core.label
import dev.justinsmith.bjj.design.components.BjjText
import dev.justinsmith.bjj.design.components.Button
import dev.justinsmith.bjj.design.components.ButtonKind
import dev.justinsmith.bjj.design.components.ButtonSize
import dev.justinsmith.bjj.design.components.Card
import dev.justinsmith.bjj.design.components.Divider
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
import dev.justinsmith.bjj.nav.Nav
import dev.justinsmith.bjj.nav.ReviewSession
import dev.justinsmith.bjj.nav.TodayTab
import dev.justinsmith.bjj.nav.TransitionDetail
import dev.justinsmith.bjj.nav.studyItemKey
import dev.justinsmith.bjj.resources.Res
import dev.justinsmith.bjj.resources.common_done
import dev.justinsmith.bjj.resources.common_positions
import dev.justinsmith.bjj.resources.review_answer_hint
import dev.justinsmith.bjj.resources.review_complete_reviewed
import dev.justinsmith.bjj.resources.review_complete_title
import dev.justinsmith.bjj.resources.review_days_in_a_row
import dev.justinsmith.bjj.resources.review_done_for_now
import dev.justinsmith.bjj.resources.review_end_session
import dev.justinsmith.bjj.resources.review_how_well
import dev.justinsmith.bjj.resources.review_keep_going
import dev.justinsmith.bjj.resources.review_more_ready
import dev.justinsmith.bjj.resources.review_nothing_else_due
import dev.justinsmith.bjj.resources.review_open_full_page
import dev.justinsmith.bjj.resources.review_position
import dev.justinsmith.bjj.resources.review_show_answer
import dev.justinsmith.bjj.ui.common.Loading
import dev.justinsmith.bjj.ui.common.Page
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ReviewSessionRoute(key: ReviewSession, nav: Nav) {
    val viewModel = koinViewModel<ReviewSessionViewModel> { parametersOf(key) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    when (val session = state) {
        ReviewSessionUiState.Loading -> Loading()

        is ReviewSessionUiState.Complete -> SessionComplete(
            state = session,
            onAgain = viewModel::restart,
            onDone = { nav.selectTab(TodayTab) },
            onPositions = { nav.selectTab(LearnTab(LearnSection.Positions)) },
        )

        is ReviewSessionUiState.Active -> ActiveSession(
            state = session,
            onEnd = nav.back,
            onReveal = viewModel::reveal,
            onGrade = viewModel::grade,
            onOpen = { nav.open(it) },
        )
    }
}

@Composable
private fun ActiveSession(
    state: ReviewSessionUiState.Active,
    onEnd: () -> Unit,
    onReveal: () -> Unit,
    onGrade: (Grade) -> Unit,
    onOpen: (androidx.navigation3.runtime.NavKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    val total = state.total
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
            val endSession = stringResource(Res.string.review_end_session)
            Box(
                Modifier
                    .size(Sizes.tap)
                    .pressShrink(interactions)
                    .clickable(interactions, indication = null, role = Role.Button, onClick = onEnd)
                    .semantics { contentDescription = endSession },
                contentAlignment = Alignment.Center,
            ) { Icon(BjjIcons.Close, description = null) }
            ProgressBar(state.doneCount.toFloat() / total.coerceAtLeast(1), Modifier.weight(1f))
            val position = stringResource(Res.string.review_position, state.doneCount + 1, total)
            BjjText(position, style = Type.small, color = p.muted)
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
                targetState = state.card to state.doneCount,
                // The card and the answer count, not the card's content: a bookmark added mid-session is no new card.
                contentKey = { (card, done) -> card.item to done },
                transitionSpec = {
                    if (still) {
                        EnterTransition.None togetherWith ExitTransition.None
                    } else {
                        (
                            slideInHorizontally(tween(Motion.BASE)) {
                                it / ENTER_FROM
                            } + fadeIn(tween(Motion.BASE))
                            ) togetherWith
                            (slideOutHorizontally(tween(Motion.BASE)) { -it / LEAVE_TO } + fadeOut(tween(Motion.BASE)))
                    }
                },
            ) { (card, _) ->
                // The side showing is read live, so revealing the answer turns this card over, not a new one in.
                FlashCard(
                    card = card,
                    revealed = state.revealed && card.item == state.card.item,
                    onReveal = onReveal,
                    onOpen = onOpen,
                    still = still,
                )
            }
        }
        // Pinned to the bottom of the screen, under the thumb, however long the card is.
        Dock(state, onReveal, onGrade)
    }
}

@Composable
private fun FlashCard(
    card: CardFace,
    revealed: Boolean,
    onReveal: () -> Unit,
    onOpen: (androidx.navigation3.runtime.NavKey) -> Unit,
    still: Boolean,
    modifier: Modifier = Modifier,
) {
    val prompt = cardPrompt(card)
    // Turning to the answer, the card swings in from edge-on, as if flipped over.
    val turn = remember { Animatable(0f) }
    LaunchedEffect(revealed) {
        if (revealed && !still) {
            turn.snapTo(FLIP_FROM)
            turn.animateTo(0f, tween(Motion.SLOW))
        }
    }
    val cardModifier = modifier
        .graphicsLayer {
            rotationY = turn.value
            cameraDistance = FLIP_DISTANCE * density
        }
        .then(
            if (revealed) {
                Modifier
            } else {
                Modifier
                    .defaultMinSize(minHeight = CARD_MIN_HEIGHT)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClick = onReveal,
                    )
            },
        )
    Card(cardModifier) {
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
            Divider()
            CardAnswer(
                card,
                { onOpen(TransitionDetail(it)) },
                Modifier.semantics {
                    liveRegion =
                        LiveRegionMode.Polite
                },
            )
            LinkButton(stringResource(Res.string.review_open_full_page), { onOpen(studyItemKey(card.item)) })
        }
    }
}

private val CARD_MIN_HEIGHT = 200.dp

// The card starts a quarter turn away, seen from far enough that it does not loom as it comes round.
private const val FLIP_FROM = -90f
private const val FLIP_DISTANCE = 16f

private const val QUESTION_SCALE = 0.7f

// A new card comes in from a quarter of the way across, and the old one leaves by a third.
private const val ENTER_FROM = 4
private const val LEAVE_TO = 3

@Composable
private fun Dock(state: ReviewSessionUiState.Active, onReveal: () -> Unit, onGrade: (Grade) -> Unit) {
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
                BjjText(stringResource(Res.string.review_how_well), style = Type.small, color = p.muted)
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                    for (grade in Grade.entries) {
                        GradeButton(grade, state.intervals[grade].orEmpty(), !state.saving, {
                            onGrade(grade)
                        }, Modifier.weight(1f))
                    }
                }
            } else {
                BjjText(stringResource(Res.string.review_answer_hint), style = Type.small, color = p.muted)
                val show = stringResource(Res.string.review_show_answer)
                Button(show, onReveal, Modifier.fillMaxWidth(), ButtonKind.Primary, ButtonSize.Large)
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
        BjjText(
            interval,
            style = Type.small,
            color = p.onGradeColor(grade).copy(alpha = INTERVAL_ALPHA),
            maxLines = 1,
        )
    }
}

private val GRADE_HEIGHT = 56.dp
private const val INTERVAL_ALPHA = 0.85f

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
    state: ReviewSessionUiState.Complete,
    onAgain: () -> Unit,
    onDone: () -> Unit,
    onPositions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val doneCount = state.doneCount
    val streakDays = state.streakDays
    val moreWaiting = state.moreWaiting
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
            PageTitle(stringResource(Res.string.review_complete_title))
            val reviewed = pluralStringResource(Res.plurals.review_complete_reviewed, doneCount, doneCount)
            BjjText(reviewed, style = Type.lead)
            GradeBreakdown(state.tally)
            if (streakDays > 0) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s1),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(BjjIcons.Flame, description = null, tint = p.belt)
                    val streak = pluralStringResource(Res.plurals.review_days_in_a_row, streakDays, streakDays)
                    BjjText(streak, style = Type.strong)
                }
            }
            BjjText(
                if (moreWaiting > 0) {
                    pluralStringResource(Res.plurals.review_more_ready, moreWaiting, moreWaiting)
                } else {
                    stringResource(Res.string.review_nothing_else_due)
                },
                color = p.muted,
                textAlign = TextAlign.Start,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                // Another round is offered only when one exists; otherwise the next step is to leave.
                if (moreWaiting > 0) {
                    val again = stringResource(Res.string.review_keep_going)
                    Button(again, onAgain, Modifier.weight(1f), ButtonKind.Primary)
                    Button(stringResource(Res.string.review_done_for_now), onDone, Modifier.weight(1f))
                } else {
                    Button(stringResource(Res.string.common_done), onDone, Modifier.weight(1f), ButtonKind.Primary)
                    Button(stringResource(Res.string.common_positions), onPositions, Modifier.weight(1f))
                }
            }
        }
    }
}

private val MARK = 72.dp
private val MARK_ICON = 40.dp

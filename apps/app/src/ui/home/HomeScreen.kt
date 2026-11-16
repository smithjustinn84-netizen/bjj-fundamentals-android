package dev.justinsmith.bjj.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.justinsmith.bjj.core.Deck
import dev.justinsmith.bjj.core.DrillPick
import dev.justinsmith.bjj.core.DrillReason
import dev.justinsmith.bjj.core.PathStep
import dev.justinsmith.bjj.core.PathStepKind
import dev.justinsmith.bjj.core.Situation
import dev.justinsmith.bjj.core.TrainingSummary
import dev.justinsmith.bjj.core.formatClassCount
import dev.justinsmith.bjj.core.formatMatTime
import dev.justinsmith.bjj.core.label
import dev.justinsmith.bjj.core.situationName
import dev.justinsmith.bjj.design.components.BjjText
import dev.justinsmith.bjj.design.components.Button
import dev.justinsmith.bjj.design.components.ButtonKind
import dev.justinsmith.bjj.design.components.ButtonSize
import dev.justinsmith.bjj.design.components.Card
import dev.justinsmith.bjj.design.components.Chip
import dev.justinsmith.bjj.design.components.Eyebrow
import dev.justinsmith.bjj.design.components.Icon
import dev.justinsmith.bjj.design.components.LinkButton
import dev.justinsmith.bjj.design.components.PageTitle
import dev.justinsmith.bjj.design.components.ProgressDot
import dev.justinsmith.bjj.design.components.SectionHead
import dev.justinsmith.bjj.design.components.Wrap
import dev.justinsmith.bjj.design.components.block
import dev.justinsmith.bjj.design.components.pressBlock
import dev.justinsmith.bjj.design.graphics.PositionArtwork
import dev.justinsmith.bjj.design.graphics.hasPositionArtwork
import dev.justinsmith.bjj.design.icons.BjjIcons
import dev.justinsmith.bjj.design.reducedMotion
import dev.justinsmith.bjj.design.theme.BjjTheme
import dev.justinsmith.bjj.design.theme.LocalContentColor
import dev.justinsmith.bjj.design.theme.Motion
import dev.justinsmith.bjj.design.theme.Radius
import dev.justinsmith.bjj.design.theme.Sizes
import dev.justinsmith.bjj.design.theme.Spacing
import dev.justinsmith.bjj.design.theme.Type
import dev.justinsmith.bjj.nav.LearnSection
import dev.justinsmith.bjj.nav.LearnTab
import dev.justinsmith.bjj.nav.LogTab
import dev.justinsmith.bjj.nav.PositionDetail
import dev.justinsmith.bjj.nav.ReviewTab
import dev.justinsmith.bjj.nav.TransitionDetail
import dev.justinsmith.bjj.nav.Videos
import dev.justinsmith.bjj.nav.itemKey
import dev.justinsmith.bjj.nav.studyItemKey
import dev.justinsmith.bjj.resources.Res
import dev.justinsmith.bjj.resources.common_done
import dev.justinsmith.bjj.resources.common_training_log
import dev.justinsmith.bjj.resources.common_videos
import dev.justinsmith.bjj.resources.today_all_lessons
import dev.justinsmith.bjj.resources.today_belt_stripes
import dev.justinsmith.bjj.resources.today_cards_ready
import dev.justinsmith.bjj.resources.today_concepts_all
import dev.justinsmith.bjj.resources.today_concepts_all_learned
import dev.justinsmith.bjj.resources.today_concepts_none
import dev.justinsmith.bjj.resources.today_concepts_title
import dev.justinsmith.bjj.resources.today_day_streak
import dev.justinsmith.bjj.resources.today_drill_eyebrow
import dev.justinsmith.bjj.resources.today_drill_reason_next
import dev.justinsmith.bjj.resources.today_drill_reason_practised
import dev.justinsmith.bjj.resources.today_drill_reason_slipping
import dev.justinsmith.bjj.resources.today_drill_reason_stuck
import dev.justinsmith.bjj.resources.today_focus_eyebrow
import dev.justinsmith.bjj.resources.today_learned
import dev.justinsmith.bjj.resources.today_learned_to_next
import dev.justinsmith.bjj.resources.today_lesson_cards_learned
import dev.justinsmith.bjj.resources.today_lesson_number
import dev.justinsmith.bjj.resources.today_log_a_class
import dev.justinsmith.bjj.resources.today_log_prompt
import dev.justinsmith.bjj.resources.today_open_review
import dev.justinsmith.bjj.resources.today_path_attack_body
import dev.justinsmith.bjj.resources.today_path_attack_title
import dev.justinsmith.bjj.resources.today_path_concepts_body
import dev.justinsmith.bjj.resources.today_path_concepts_title
import dev.justinsmith.bjj.resources.today_path_next_up
import dev.justinsmith.bjj.resources.today_path_positions_body
import dev.justinsmith.bjj.resources.today_path_positions_title
import dev.justinsmith.bjj.resources.today_path_see_map
import dev.justinsmith.bjj.resources.today_path_survive_body
import dev.justinsmith.bjj.resources.today_path_survive_title
import dev.justinsmith.bjj.resources.today_path_title
import dev.justinsmith.bjj.resources.today_path_videos_body
import dev.justinsmith.bjj.resources.today_path_videos_title
import dev.justinsmith.bjj.resources.today_review_first
import dev.justinsmith.bjj.resources.today_review_nothing_due
import dev.justinsmith.bjj.resources.today_review_split
import dev.justinsmith.bjj.resources.today_review_split_from_class
import dev.justinsmith.bjj.resources.today_start_review
import dev.justinsmith.bjj.resources.today_sticking_note
import dev.justinsmith.bjj.resources.today_sticking_title
import dev.justinsmith.bjj.resources.today_stuck_note
import dev.justinsmith.bjj.resources.today_study_lesson
import dev.justinsmith.bjj.resources.today_study_rank_note
import dev.justinsmith.bjj.resources.today_title
import dev.justinsmith.bjj.resources.today_training_line
import dev.justinsmith.bjj.ui.common.CardLink
import dev.justinsmith.bjj.ui.common.LibrarySearch
import dev.justinsmith.bjj.ui.common.Loading
import dev.justinsmith.bjj.ui.common.Nav
import dev.justinsmith.bjj.ui.common.Page
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Why today's drill was chosen, said to the student. */
private val DrillReason.text: StringResource
    get() = when (this) {
        DrillReason.Stuck -> Res.string.today_drill_reason_stuck
        DrillReason.Slipping -> Res.string.today_drill_reason_slipping
        DrillReason.Practised -> Res.string.today_drill_reason_practised
        DrillReason.Next -> Res.string.today_drill_reason_next
    }

@Composable
fun HomeRoute(nav: Nav) {
    val viewModel = koinViewModel<HomeViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    when (val ready = state) {
        HomeUiState.Loading -> Loading()
        is HomeUiState.Ready -> HomeScreen(ready, nav, viewModel::search)
    }
}

/** Today: what to review, where the student stands, and what to do next. */
@Composable
fun HomeScreen(state: HomeUiState.Ready, nav: Nav, onSearch: (String) -> Unit, modifier: Modifier = Modifier) {
    val rank = state.rank
    Page(modifier) {
        Hero(state.dateLabel, state.streakDays, state.review, onReview = { nav.selectTab(ReviewTab()) })
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s1)) {
            BeltBand(rank.belt, rank.stripes)
            val nextBelt = rank.nextBelt
            // A black belt has no next belt to count stripes or a percentage towards.
            val belt = if (nextBelt != null) {
                pluralStringResource(Res.plurals.today_belt_stripes, rank.stripes, rank.belt.label, rank.stripes)
            } else {
                rank.belt.label
            }
            BjjText(belt, style = Type.strong)
            val learned = if (nextBelt != null) {
                stringResource(
                    Res.string.today_learned_to_next,
                    state.learned,
                    state.total,
                    rank.percentToNextBelt,
                    nextBelt.label.lowercase(),
                )
            } else {
                stringResource(Res.string.today_learned, state.learned, state.total)
            }
            BjjText(learned, color = BjjTheme.palette.muted)
            val note = stringResource(Res.string.today_study_rank_note)
            BjjText(note, style = Type.small, color = BjjTheme.palette.muted)
        }
        state.drill?.let { DrillCard(it, nav) }
        TrainingCard(state.training, state.stuck, nav)
        LibrarySearch(state.search, onSearch, { kind, id -> nav.open(itemKey(kind, id)) })
        state.lesson?.let { LessonCard(it, nav) }
        Card {
            BjjText(stringResource(Res.string.today_path_title), style = Type.h2)
            PathSteps(state.path, nav)
        }
        StickingCard(state.sticking, nav)
        ConceptsCard(state.concepts, state.conceptCount, nav)
    }
}

/** What the review block says under its number: how the session splits, or what to do when nothing is ready. */
@Composable
private fun reviewLine(review: ReviewOffer): String = when {
    review.ready == 0 -> stringResource(Res.string.today_review_nothing_due)

    review.started == 0 -> stringResource(Res.string.today_review_first)

    review.fromClass > 0 -> stringResource(
        Res.string.today_review_split_from_class,
        review.due,
        review.fresh,
        review.fromClass,
    )

    else -> stringResource(Res.string.today_review_split, review.due, review.fresh)
}

/** The one thing to do today: how many cards a session would hold, and the button that starts it. */
@Composable
private fun Hero(dateLabel: String, streakDays: Int, review: ReviewOffer, onReview: () -> Unit) {
    val ready = review.ready
    val p = BjjTheme.palette
    Box(Modifier.fillMaxWidth().drawBehind { block(p.fill, p.outline, Radius.lg) }) {
        // 柔, "ju": the gentle of jiu-jitsu. Decoration only.
        BjjText(
            "柔",
            Modifier.align(Alignment.TopEnd).padding(end = Spacing.s2).clearAndSetSemantics {},
            style = Type.count.copy(fontSize = 200.sp, fontWeight = FontWeight.Normal),
            color = p.fillMark,
        )
        // Everything on the painted block takes its on-colour.
        HeroContent {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Eyebrow(dateLabel, color = p.fillSoft)
                    PageTitle(stringResource(Res.string.today_title))
                }
                if (streakDays > 0) StreakStamp(streakDays)
            }
            Row(
                Modifier.padding(vertical = Spacing.s3),
                horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BjjText("${countUp(ready)}", style = Type.count)
                Column {
                    BjjText(pluralStringResource(Res.plurals.today_cards_ready, ready), style = Type.strong)
                    BjjText(reviewLine(review), style = Type.small, color = p.fillSoft)
                }
            }
            Button(
                stringResource(if (ready > 0) Res.string.today_start_review else Res.string.today_open_review),
                onReview,
                Modifier.fillMaxWidth(),
                kind = ButtonKind.Cta,
                size = ButtonSize.Large,
            )
        }
    }
}

@Composable
private fun HeroContent(content: @Composable ColumnScope.() -> Unit) {
    CompositionLocalProvider(LocalContentColor provides BjjTheme.palette.onFill) {
        Column(Modifier.fillMaxWidth().padding(Spacing.cardPad), content = content)
    }
}

/** A number counting up to [target] as the page opens, or simply [target] when animations are off. */
@Composable
private fun countUp(target: Int): Int {
    if (reducedMotion()) return target
    val shown = remember { Animatable(0f) }
    LaunchedEffect(target) { shown.animateTo(target.toFloat(), tween(Motion.COUNT_UP)) }
    return shown.value.toInt()
}

/** The streak, stamped on the hero in belt red and a little askew. */
@Composable
private fun StreakStamp(days: Int, modifier: Modifier = Modifier) {
    val p = BjjTheme.palette
    Row(
        modifier
            .rotate(STAMP_TILT)
            .drawBehind { block(p.belt, p.outline, Radius.sm) }
            .padding(horizontal = Spacing.s3, vertical = Spacing.s1),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BjjText("$days", style = Type.h2, color = p.onBelt)
        BjjText(
            pluralStringResource(Res.plurals.today_day_streak, days),
            style = Type.small.copy(fontWeight = FontWeight.Bold),
            color = p.onBelt,
        )
    }
}

private const val STAMP_TILT = -5f

@Composable
private fun DrillCard(drill: DrillPick, nav: Nav, modifier: Modifier = Modifier) {
    val interactions = remember { MutableInteractionSource() }
    Row(
        modifier
            .fillMaxWidth()
            .pressBlock(interactions, outline = BjjTheme.palette.outline, radius = Radius.lg)
            .clickable(interactions, indication = null, role = Role.Button) {
                nav.open(TransitionDetail(drill.technique.id))
            }
            .padding(Spacing.s3),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (hasPositionArtwork(drill.technique.fromId)) {
            PositionArtwork(drill.technique.fromId, Modifier.width(Sizes.thumb))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Eyebrow(stringResource(Res.string.today_drill_eyebrow))
            BjjText(drill.technique.name, style = Type.strong)
            BjjText(stringResource(drill.reason.text), style = Type.small, color = BjjTheme.palette.muted)
            BjjText(drill.drill, style = Type.small)
        }
        Icon(BjjIcons.Chevron, description = null, tint = BjjTheme.palette.muted)
    }
}

@Composable
private fun TrainingCard(training: TrainingSummary, stuck: List<Situation>, nav: Nav, modifier: Modifier = Modifier) {
    val muted = BjjTheme.palette.muted
    Card(modifier) {
        if (training.classes == 0) {
            Eyebrow(stringResource(Res.string.common_training_log))
            BjjText(stringResource(Res.string.today_log_prompt), color = muted)
            Button(stringResource(Res.string.today_log_a_class), { nav.selectTab(LogTab) })
        } else {
            val hasFocus = training.focus.isNotEmpty()
            Eyebrow(stringResource(if (hasFocus) Res.string.today_focus_eyebrow else Res.string.common_training_log))
            if (hasFocus) BjjText(training.focus, style = Type.h2)
            val line = stringResource(
                Res.string.today_training_line,
                formatClassCount(training.classes),
                formatMatTime(training.minutes),
            )
            BjjText(line, style = Type.small, color = muted)
            if (stuck.isNotEmpty()) {
                BjjText(stringResource(Res.string.today_stuck_note), style = Type.small, color = muted)
                Wrap {
                    for (situation in stuck) {
                        Chip(situationName(situation), {
                            nav.open(PositionDetail(situation.position.id, situation.role))
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun LessonCard(offer: LessonOffer, nav: Nav, modifier: Modifier = Modifier) {
    val lesson = offer.lesson
    val progress = offer.progress
    Card(modifier) {
        Eyebrow(stringResource(Res.string.today_lesson_number, offer.number, offer.of))
        SectionHead(lesson.title) {
            Button(
                stringResource(Res.string.today_all_lessons),
                { nav.selectTab(LearnTab(LearnSection.Lessons)) },
                size = ButtonSize.Small,
            )
        }
        BjjText(lesson.summary, color = BjjTheme.palette.muted)
        val learned = pluralStringResource(
            Res.plurals.today_lesson_cards_learned,
            progress.total,
            progress.learned,
            progress.total,
        )
        BjjText(learned, style = Type.small)
        Button(stringResource(Res.string.today_study_lesson), { nav.selectTab(ReviewTab(Deck.All.key, lesson.id)) })
    }
}

@Composable
private fun StickingCard(sticking: List<CardLink>, nav: Nav, modifier: Modifier = Modifier) {
    if (sticking.isEmpty()) return
    Card(modifier) {
        BjjText(stringResource(Res.string.today_sticking_title), style = Type.h2)
        BjjText(stringResource(Res.string.today_sticking_note), color = BjjTheme.palette.muted)
        Wrap {
            for (card in sticking) {
                Chip(card.name, { nav.open(studyItemKey(card.item)) }) { ProgressDot(card.progress) }
            }
        }
    }
}

@Composable
private fun ConceptsCard(next: List<CardLink>, conceptCount: Int, nav: Nav, modifier: Modifier = Modifier) {
    Card(modifier) {
        SectionHead(stringResource(Res.string.today_concepts_title)) {
            Button(
                stringResource(Res.string.today_concepts_all, conceptCount),
                { nav.selectTab(LearnTab(LearnSection.Concepts)) },
                size = ButtonSize.Small,
            )
        }
        if (next.isEmpty()) {
            val text = if (conceptCount == 0) {
                Res.string.today_concepts_none
            } else {
                Res.string.today_concepts_all_learned
            }
            BjjText(stringResource(text), color = BjjTheme.palette.muted)
        } else {
            Wrap {
                for (concept in next) {
                    Chip(concept.name, { nav.open(studyItemKey(concept.item)) }) { ProgressDot(concept.progress) }
                }
            }
        }
    }
}

/** What a step of the path is called and what it asks for. */
private val PathStepKind.title: StringResource
    get() = when (this) {
        PathStepKind.Survive -> Res.string.today_path_survive_title
        PathStepKind.Concepts -> Res.string.today_path_concepts_title
        PathStepKind.Positions -> Res.string.today_path_positions_title
        PathStepKind.Attack -> Res.string.today_path_attack_title
        PathStepKind.Videos -> Res.string.today_path_videos_title
    }

private val PathStepKind.body: StringResource
    get() = when (this) {
        PathStepKind.Survive -> Res.string.today_path_survive_body
        PathStepKind.Concepts -> Res.string.today_path_concepts_body
        PathStepKind.Positions -> Res.string.today_path_positions_body
        PathStepKind.Attack -> Res.string.today_path_attack_body
        PathStepKind.Videos -> Res.string.today_path_videos_body
    }

/**
 * The link under a step: the name of what comes next in it while something is left to learn. With nothing
 * left, the attack step points at the map and the videos step at the bookmarks; the others have no link.
 */
@Composable
private fun PathStep.link(nav: Nav): Pair<String, () -> Unit>? {
    val next = next
    return when {
        next != null -> stringResource(Res.string.today_path_next_up, nextName) to { nav.open(studyItemKey(next)) }

        kind == PathStepKind.Attack -> stringResource(Res.string.today_path_see_map) to
            { nav.selectTab(LearnTab(LearnSection.Positions)) }

        kind == PathStepKind.Videos -> stringResource(Res.string.common_videos) to { nav.open(Videos) }

        else -> null
    }
}

/** The suggested path as a stepper down the card. The first unfinished step is marked as the current one. */
@Composable
private fun PathSteps(steps: List<PathStep>, nav: Nav, modifier: Modifier = Modifier) {
    val p = BjjTheme.palette
    val currentIndex = steps.indexOfFirst { !it.done }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.s3)) {
        steps.forEachIndexed { index, step ->
            val current = index == currentIndex
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s3)) {
                Box(
                    Modifier.size(MARKER).drawBehind {
                        val fill = when {
                            step.done -> p.ok
                            current -> p.fill
                            else -> p.well
                        }
                        block(fill, p.outline, Radius.pill)
                    },
                    contentAlignment = Alignment.Center,
                ) {
                    if (step.done) {
                        val done = stringResource(Res.string.common_done)
                        Icon(BjjIcons.Check, description = done, tint = p.onStatus, size = Sizes.iconSmall)
                    } else {
                        BjjText(
                            "${index + 1}",
                            style = Type.small.copy(fontWeight = FontWeight.Bold),
                            color = if (current) p.onFill else p.muted,
                        )
                    }
                }
                Column(Modifier.weight(1f)) {
                    BjjText(stringResource(step.kind.title), style = Type.strong)
                    BjjText(stringResource(step.kind.body), color = p.muted)
                    step.link(nav)?.let { (label, open) -> LinkButton(label, open) }
                }
            }
        }
    }
}

private val MARKER = 30.dp

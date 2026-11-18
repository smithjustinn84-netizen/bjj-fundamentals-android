package dev.justinsmith.bjj.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.justinsmith.bjj.core.label
import dev.justinsmith.bjj.design.components.BjjText
import dev.justinsmith.bjj.design.components.Button
import dev.justinsmith.bjj.design.components.ButtonKind
import dev.justinsmith.bjj.design.components.ButtonSize
import dev.justinsmith.bjj.design.components.Card
import dev.justinsmith.bjj.design.components.Eyebrow
import dev.justinsmith.bjj.design.components.PageTitle
import dev.justinsmith.bjj.design.components.block
import dev.justinsmith.bjj.design.reducedMotion
import dev.justinsmith.bjj.design.theme.BjjTheme
import dev.justinsmith.bjj.design.theme.LocalContentColor
import dev.justinsmith.bjj.design.theme.Motion
import dev.justinsmith.bjj.design.theme.Radius
import dev.justinsmith.bjj.design.theme.Spacing
import dev.justinsmith.bjj.design.theme.Type
import dev.justinsmith.bjj.nav.Nav
import dev.justinsmith.bjj.nav.ReviewTab
import dev.justinsmith.bjj.nav.itemKey
import dev.justinsmith.bjj.resources.Res
import dev.justinsmith.bjj.resources.today_belt_stripes
import dev.justinsmith.bjj.resources.today_cards_ready
import dev.justinsmith.bjj.resources.today_day_streak
import dev.justinsmith.bjj.resources.today_learned
import dev.justinsmith.bjj.resources.today_learned_to_next
import dev.justinsmith.bjj.resources.today_open_review
import dev.justinsmith.bjj.resources.today_path_title
import dev.justinsmith.bjj.resources.today_review_first
import dev.justinsmith.bjj.resources.today_review_nothing_due
import dev.justinsmith.bjj.resources.today_review_split
import dev.justinsmith.bjj.resources.today_review_split_from_class
import dev.justinsmith.bjj.resources.today_start_review
import dev.justinsmith.bjj.resources.today_study_rank_note
import dev.justinsmith.bjj.resources.today_title
import dev.justinsmith.bjj.ui.common.LibrarySearch
import dev.justinsmith.bjj.ui.common.Loading
import dev.justinsmith.bjj.ui.common.Page
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

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
                CountUp(ready)
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

/**
 * [target] in large figures, counting up to it as the page opens. A composable of its own, so each step of the
 * count redraws the number and not the card round it.
 */
@Composable
private fun CountUp(target: Int, modifier: Modifier = Modifier) {
    BjjText("${countUp(target)}", modifier, style = Type.count)
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

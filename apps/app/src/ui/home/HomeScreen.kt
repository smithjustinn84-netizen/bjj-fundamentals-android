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
import dev.justinsmith.bjj.core.CardKind
import dev.justinsmith.bjj.core.DEFAULT_NEW_CARDS
import dev.justinsmith.bjj.core.Deck
import dev.justinsmith.bjj.core.DrillReason
import dev.justinsmith.bjj.core.FOUNDATION_LESSONS
import dev.justinsmith.bjj.core.Library
import dev.justinsmith.bjj.core.RECENT_CLASS_COUNT
import dev.justinsmith.bjj.core.STICKING_POINT_LIMIT
import dev.justinsmith.bjj.core.StudyQueue
import dev.justinsmith.bjj.core.beltRank
import dev.justinsmith.bjj.core.buildQueue
import dev.justinsmith.bjj.core.cardId
import dev.justinsmith.bjj.core.cardName
import dev.justinsmith.bjj.core.conceptsToLearn
import dev.justinsmith.bjj.core.currentLesson
import dev.justinsmith.bjj.core.findSituation
import dev.justinsmith.bjj.core.formatClassCount
import dev.justinsmith.bjj.core.formatMatTime
import dev.justinsmith.bjj.core.isEscape
import dev.justinsmith.bjj.core.label
import dev.justinsmith.bjj.core.lessonOrder
import dev.justinsmith.bjj.core.lessonProgress
import dev.justinsmith.bjj.core.lessonTechniques
import dev.justinsmith.bjj.core.pickDrill
import dev.justinsmith.bjj.core.progressSummary
import dev.justinsmith.bjj.core.recentlyStuck
import dev.justinsmith.bjj.core.reviewOf
import dev.justinsmith.bjj.core.services.Snapshot
import dev.justinsmith.bjj.core.services.studyStats
import dev.justinsmith.bjj.core.situationName
import dev.justinsmith.bjj.core.stickingPoints
import dev.justinsmith.bjj.core.studyFirst
import dev.justinsmith.bjj.core.trainingSummary
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
import dev.justinsmith.bjj.nav.ConceptDetail
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
import dev.justinsmith.bjj.ui.common.LibrarySearch
import dev.justinsmith.bjj.ui.common.Nav
import dev.justinsmith.bjj.ui.common.Page
import dev.justinsmith.bjj.ui.common.WithSnapshot
import dev.justinsmith.bjj.ui.common.progressOf
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** Today shows only the next few concepts; the Concepts page has them all. */
private const val HOME_CONCEPT_LIMIT = 6

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
    WithSnapshot { snapshot, viewModel ->
        val now = viewModel.now()
        HomeScreen(
            snapshot = snapshot,
            now = now,
            dateLabel = todayLabel(now, viewModel.zone),
            streakDays = studyStats(snapshot, now, viewModel.zone).streakDays,
            nav = nav,
        )
    }
}

/** Today: what to review, where the student stands, and what to do next. */
@Composable
fun HomeScreen(
    snapshot: Snapshot,
    now: Long,
    dateLabel: String,
    streakDays: Int,
    nav: Nav,
    modifier: Modifier = Modifier,
) {
    val lib = snapshot.library
    val log = snapshot.trainingLog
    val progress = progressSummary(lib)
    val rank = beltRank(progress.percentLearned)
    // The same session the Review page would offer with its default settings.
    val preview = buildQueue(lib, Deck.All, DEFAULT_NEW_CARDS, now, studyFirst(lib, log, RECENT_CLASS_COUNT))
    Page(modifier) {
        Hero(dateLabel, streakDays, preview, progress.started, onReview = { nav.selectTab(ReviewTab()) })
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
                    progress.learned,
                    progress.total,
                    rank.percentToNextBelt,
                    nextBelt.label.lowercase(),
                )
            } else {
                stringResource(Res.string.today_learned, progress.learned, progress.total)
            }
            BjjText(learned, color = BjjTheme.palette.muted)
            val note = stringResource(Res.string.today_study_rank_note)
            BjjText(note, style = Type.small, color = BjjTheme.palette.muted)
        }
        DrillCard(lib, snapshot, nav)
        TrainingCard(snapshot, nav)
        LibrarySearch(lib, { kind, id -> nav.open(itemKey(kind, id)) })
        LessonCard(lib, nav)
        Card {
            BjjText(stringResource(Res.string.today_path_title), style = Type.h2)
            PathSteps(pathSteps(snapshot, nav))
        }
        StickingCard(lib, nav)
        ConceptsCard(lib, nav)
    }
}

/** What the review block says under its number: how the session splits, or what to do when nothing is ready. */
@Composable
private fun reviewLine(preview: StudyQueue, started: Int): String {
    val due = preview.due.size
    val newCount = preview.queue.size - due
    val fromClass = preview.fromClass
    return when {
        preview.queue.isEmpty() -> stringResource(Res.string.today_review_nothing_due)
        started == 0 -> stringResource(Res.string.today_review_first)
        fromClass > 0 -> stringResource(Res.string.today_review_split_from_class, due, newCount, fromClass)
        else -> stringResource(Res.string.today_review_split, due, newCount)
    }
}

/** The one thing to do today: how many cards a session would hold, and the button that starts it. */
@Composable
private fun Hero(dateLabel: String, streakDays: Int, preview: StudyQueue, started: Int, onReview: () -> Unit) {
    val ready = preview.queue.size
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
                    BjjText(reviewLine(preview, started), style = Type.small, color = p.fillSoft)
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
private fun DrillCard(lib: Library, snapshot: Snapshot, nav: Nav, modifier: Modifier = Modifier) {
    val drill = pickDrill(lib, snapshot.trainingLog) ?: return
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
private fun TrainingCard(snapshot: Snapshot, nav: Nav, modifier: Modifier = Modifier) {
    val lib = snapshot.library
    val training = trainingSummary(snapshot.trainingLog)
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
            val stuck = recentlyStuck(snapshot.trainingLog, RECENT_CLASS_COUNT).mapNotNull { findSituation(lib, it) }
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
private fun LessonCard(lib: Library, nav: Nav, modifier: Modifier = Modifier) {
    val lesson = currentLesson(lib) ?: return
    val progress = lessonProgress(lib, lesson)
    Card(modifier) {
        val number = FOUNDATION_LESSONS.indexOf(lesson) + 1
        Eyebrow(stringResource(Res.string.today_lesson_number, number, FOUNDATION_LESSONS.size))
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
private fun StickingCard(lib: Library, nav: Nav, modifier: Modifier = Modifier) {
    val sticking = stickingPoints(lib, STICKING_POINT_LIMIT)
    if (sticking.isEmpty()) return
    Card(modifier) {
        BjjText(stringResource(Res.string.today_sticking_title), style = Type.h2)
        BjjText(stringResource(Res.string.today_sticking_note), color = BjjTheme.palette.muted)
        Wrap {
            for (item in sticking) {
                Chip(cardName(lib, item).orEmpty(), { nav.open(studyItemKey(item)) }) {
                    ProgressDot(progressOf(reviewOf(lib, item.kind, item.id)))
                }
            }
        }
    }
}

@Composable
private fun ConceptsCard(lib: Library, nav: Nav, modifier: Modifier = Modifier) {
    val next = conceptsToLearn(lib, HOME_CONCEPT_LIMIT)
    Card(modifier) {
        SectionHead(stringResource(Res.string.today_concepts_title)) {
            Button(
                stringResource(Res.string.today_concepts_all, lib.concepts.size),
                { nav.selectTab(LearnTab(LearnSection.Concepts)) },
                size = ButtonSize.Small,
            )
        }
        if (next.isEmpty()) {
            val text = if (lib.concepts.isEmpty()) {
                Res.string.today_concepts_none
            } else {
                Res.string.today_concepts_all_learned
            }
            BjjText(stringResource(text), color = BjjTheme.palette.muted)
        } else {
            Wrap {
                for (concept in next) {
                    Chip(concept.name, { nav.open(ConceptDetail(concept.id)) }) {
                        ProgressDot(progressOf(reviewOf(lib, CardKind.Concept, concept.id)))
                    }
                }
            }
        }
    }
}

/** One step of the suggested path: what it is, whether it is done, and where its link goes. */
private class PathStep(
    val title: String,
    val done: Boolean,
    val body: String,
    val link: Pair<String, () -> Unit>? = null,
)

/** The link under a step of the path that has something left to learn: the name of what comes next. */
@Composable
private fun nextUp(name: String): String = stringResource(Res.string.today_path_next_up, name)

@Composable
private fun pathSteps(snapshot: Snapshot, nav: Nav): List<PathStep> {
    val lib = snapshot.library
    val unstudied = { kind: CardKind, id: String -> cardId(kind, id) !in lib.reviews }
    val nextConcept = lib.concepts.firstOrNull { unstudied(CardKind.Concept, it.id) }
    val nextPosition = lessonOrder(lib)
        .filter { it.kind == CardKind.Position && unstudied(it.kind, it.id) }
        .firstNotNullOfOrNull { lib.positionById[it.id] }
    val notStarted = lessonTechniques(lib).filter { unstudied(CardKind.Transition, it.id) }
    val nextEscape = notStarted.firstOrNull { isEscape(it.type) }
    val nextAttack = notStarted.firstOrNull { !isEscape(it.type) }
    return listOf(
        PathStep(
            stringResource(Res.string.today_path_survive_title),
            done = lib.transitions.any { isEscape(it.type) } && nextEscape == null,
            body = stringResource(Res.string.today_path_survive_body),
            link = nextEscape?.let { nextUp(it.name) to { nav.open(TransitionDetail(it.id)) } },
        ),
        PathStep(
            stringResource(Res.string.today_path_concepts_title),
            done = lib.concepts.isNotEmpty() && nextConcept == null,
            body = stringResource(Res.string.today_path_concepts_body),
            link = nextConcept?.let { nextUp(it.name) to { nav.open(ConceptDetail(it.id)) } },
        ),
        PathStep(
            stringResource(Res.string.today_path_positions_title),
            done = lib.positions.isNotEmpty() && nextPosition == null,
            body = stringResource(Res.string.today_path_positions_body),
            link = nextPosition?.let { nextUp(it.name) to { nav.open(PositionDetail(it.id)) } },
        ),
        PathStep(
            stringResource(Res.string.today_path_attack_title),
            done = lib.transitions.any { !isEscape(it.type) } && nextAttack == null,
            body = stringResource(Res.string.today_path_attack_body),
            link = nextAttack?.let { nextUp(it.name) to { nav.open(TransitionDetail(it.id)) } }
                ?: (
                    stringResource(Res.string.today_path_see_map) to
                        { nav.selectTab(LearnTab(LearnSection.Positions)) }
                    ),
        ),
        PathStep(
            stringResource(Res.string.today_path_videos_title),
            done = snapshot.videos.isNotEmpty(),
            body = stringResource(Res.string.today_path_videos_body),
            link = stringResource(Res.string.common_videos) to { nav.open(Videos) },
        ),
    )
}

/** The suggested path as a stepper down the card. The first unfinished step is marked as the current one. */
@Composable
private fun PathSteps(steps: List<PathStep>, modifier: Modifier = Modifier) {
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
                    BjjText(step.title, style = Type.strong)
                    BjjText(step.body, color = p.muted)
                    step.link?.let { (label, open) -> LinkButton(label, open) }
                }
            }
        }
    }
}

private val MARKER = 30.dp

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
import dev.justinsmith.bjj.core.plural
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
import dev.justinsmith.bjj.ui.common.LibrarySearch
import dev.justinsmith.bjj.ui.common.Nav
import dev.justinsmith.bjj.ui.common.Page
import dev.justinsmith.bjj.ui.common.WithSnapshot
import dev.justinsmith.bjj.ui.common.progressOf

/** Today shows only the next few concepts; the Concepts page has them all. */
private const val HOME_CONCEPT_LIMIT = 6

/** Why today's drill was chosen, said to the student. */
private val DrillReason.text: String
    get() = when (this) {
        DrillReason.Stuck -> "A way out of where you got stuck in a recent class."
        DrillReason.Slipping -> "This one keeps slipping in review."
        DrillReason.Practised -> "You practised this in a recent class."
        DrillReason.Next -> "The next technique you have not learned yet."
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
            val stripes = if (rank.nextBelt != null) " · ${plural(rank.stripes, "stripe")}" else ""
            BjjText(rank.belt.label + stripes, style = Type.strong)
            val toNext = rank.nextBelt?.let {
                " · ${rank.percentToNextBelt}% more for ${it.label.lowercase()}"
            }.orEmpty()
            BjjText("${progress.learned} of ${progress.total} learned$toNext", color = BjjTheme.palette.muted)
            BjjText("A study rank, not a real one.", style = Type.small, color = BjjTheme.palette.muted)
        }
        DrillCard(lib, snapshot, nav)
        TrainingCard(snapshot, nav)
        LibrarySearch(lib, { kind, id -> nav.open(itemKey(kind, id)) })
        LessonCard(lib, nav)
        Card {
            BjjText("Suggested path", style = Type.h2)
            PathSteps(pathSteps(snapshot, nav))
        }
        StickingCard(lib, nav)
        ConceptsCard(lib, nav)
    }
}

/** What the review block says under its number: how the session splits, or what to do when nothing is ready. */
private fun reviewLine(preview: StudyQueue, started: Int): String {
    val newCount = preview.queue.size - preview.due.size
    val split = "${preview.due.size} due · $newCount new"
    return when {
        preview.queue.isEmpty() -> "Nothing is due. Come back tomorrow, or browse something new."
        started == 0 -> "Start with how to survive underneath: escapes come first."
        preview.fromClass > 0 -> "$split, ${preview.fromClass} from your recent classes"
        else -> split
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
                    PageTitle("Today")
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
                    BjjText(if (ready == 1) "card ready" else "cards ready", style = Type.strong)
                    BjjText(reviewLine(preview, started), style = Type.small, color = p.fillSoft)
                }
            }
            Button(
                if (ready > 0) "Start review" else "Open review",
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
        BjjText("day streak", style = Type.small.copy(fontWeight = FontWeight.Bold), color = p.onBelt)
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
            Eyebrow("Drill for today")
            BjjText(drill.technique.name, style = Type.strong)
            BjjText(drill.reason.text, style = Type.small, color = BjjTheme.palette.muted)
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
            Eyebrow("Training log")
            BjjText(
                "Write up each class while it is fresh: what was taught, what worked in rolling, and one thing to " +
                    "work on next time.",
                color = muted,
            )
            Button("Log a class", { nav.selectTab(LogTab) })
        } else {
            Eyebrow(if (training.focus.isNotEmpty()) "Focus for your next class" else "Training log")
            if (training.focus.isNotEmpty()) BjjText(training.focus, style = Type.h2)
            val line = "${formatClassCount(training.classes)} logged · ${formatMatTime(training.minutes)} on the mat"
            BjjText(line, style = Type.small, color = muted)
            val stuck = recentlyStuck(snapshot.trainingLog, RECENT_CLASS_COUNT).mapNotNull { findSituation(lib, it) }
            if (stuck.isNotEmpty()) {
                BjjText(
                    "Where you got stuck. Cards from here that you have not studied yet come first in review.",
                    style = Type.small,
                    color = muted,
                )
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
        Eyebrow("Lesson ${FOUNDATION_LESSONS.indexOf(lesson) + 1} of ${FOUNDATION_LESSONS.size}")
        SectionHead(lesson.title) {
            Button("All lessons", { nav.selectTab(LearnTab(LearnSection.Lessons)) }, size = ButtonSize.Small)
        }
        BjjText(lesson.summary, color = BjjTheme.palette.muted)
        BjjText("${progress.learned} of ${plural(progress.total, "card")} learned", style = Type.small)
        Button("Study this lesson", { nav.selectTab(ReviewTab(Deck.All.key, lesson.id)) })
    }
}

@Composable
private fun StickingCard(lib: Library, nav: Nav, modifier: Modifier = Modifier) {
    val sticking = stickingPoints(lib, STICKING_POINT_LIMIT)
    if (sticking.isEmpty()) return
    Card(modifier) {
        BjjText("Sticking points", style = Type.h2)
        BjjText(
            "These have slipped more than once in review. Reread the page or watch the video, then try one in your " +
                "next class.",
            color = BjjTheme.palette.muted,
        )
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
        SectionHead("Concepts to learn next") {
            Button("All ${lib.concepts.size}", {
                nav.selectTab(LearnTab(LearnSection.Concepts))
            }, size = ButtonSize.Small)
        }
        if (next.isEmpty()) {
            val learned = "Every concept is learned. Keep reviewing to stay sharp."
            val text = if (lib.concepts.isEmpty()) "No concepts yet." else learned
            BjjText(text, color = BjjTheme.palette.muted)
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
            "Survive first",
            done = lib.transitions.any { isEscape(it.type) } && nextEscape == null,
            body = "Learn the way out of side control and mount before anything else. The lessons start there.",
            link = nextEscape?.let { "Next up: ${it.name}" to { nav.open(TransitionDetail(it.id)) } },
        ),
        PathStep(
            "Core concepts",
            done = lib.concepts.isNotEmpty() && nextConcept == null,
            body = "Posture, base, frames, and the shrimp show up everywhere. Each arrives with the escape that " +
                "uses it.",
            link = nextConcept?.let { "Next up: ${it.name}" to { nav.open(ConceptDetail(it.id)) } },
        ),
        PathStep(
            "The main positions",
            done = lib.positions.isNotEmpty() && nextPosition == null,
            body = "Know what each person wants in each one.",
            link = nextPosition?.let { "Next up: ${it.name}" to { nav.open(PositionDetail(it.id)) } },
        ),
        PathStep(
            "Then attack",
            done = lib.transitions.any { !isEscape(it.type) } && nextAttack == null,
            body = "Sweeps and passes improve your position; submissions come last.",
            link = nextAttack?.let { "Next up: ${it.name}" to { nav.open(TransitionDetail(it.id)) } }
                ?: ("See the position map" to { nav.selectTab(LearnTab(LearnSection.Positions)) }),
        ),
        PathStep(
            "Save the best explanations",
            done = snapshot.videos.isNotEmpty(),
            body = "Bookmark the exact moment in a YouTube video on any item, or on the Videos page.",
            link = "Videos" to { nav.open(Videos) },
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
                        Icon(BjjIcons.Check, description = "Done", tint = p.onStatus, size = Sizes.iconSmall)
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

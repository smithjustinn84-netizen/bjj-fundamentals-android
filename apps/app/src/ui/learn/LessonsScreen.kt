package dev.justinsmith.bjj.ui.learn

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.justinsmith.bjj.core.Deck
import dev.justinsmith.bjj.core.FOUNDATION_LESSONS
import dev.justinsmith.bjj.core.Lesson
import dev.justinsmith.bjj.core.Library
import dev.justinsmith.bjj.core.cardName
import dev.justinsmith.bjj.core.currentLesson
import dev.justinsmith.bjj.core.lessonCards
import dev.justinsmith.bjj.core.lessonProgress
import dev.justinsmith.bjj.core.plural
import dev.justinsmith.bjj.core.reviewOf
import dev.justinsmith.bjj.design.components.BjjText
import dev.justinsmith.bjj.design.components.Button
import dev.justinsmith.bjj.design.components.ButtonKind
import dev.justinsmith.bjj.design.components.Card
import dev.justinsmith.bjj.design.components.Chip
import dev.justinsmith.bjj.design.components.Eyebrow
import dev.justinsmith.bjj.design.components.ProgressBar
import dev.justinsmith.bjj.design.components.ProgressDot
import dev.justinsmith.bjj.design.components.SoftBlock
import dev.justinsmith.bjj.design.components.Wrap
import dev.justinsmith.bjj.design.theme.BjjTheme
import dev.justinsmith.bjj.design.theme.Spacing
import dev.justinsmith.bjj.design.theme.Type
import dev.justinsmith.bjj.nav.LearnSection
import dev.justinsmith.bjj.nav.LearnTab
import dev.justinsmith.bjj.nav.ReviewTab
import dev.justinsmith.bjj.nav.studyItemKey
import dev.justinsmith.bjj.ui.common.LearnSwitch
import dev.justinsmith.bjj.ui.common.Nav
import dev.justinsmith.bjj.ui.common.Page
import dev.justinsmith.bjj.ui.common.PageHead
import dev.justinsmith.bjj.ui.common.ProgressLegend
import dev.justinsmith.bjj.ui.common.WithSnapshot
import dev.justinsmith.bjj.ui.common.progressOf

@Composable
fun LessonsRoute(nav: Nav) {
    WithSnapshot { snapshot, _ -> LessonsScreen(snapshot.library, nav) }
}

@Composable
fun LessonsScreen(lib: Library, nav: Nav, modifier: Modifier = Modifier) {
    val current = currentLesson(lib)
    Page(modifier) {
        LearnSwitch(LearnSection.Lessons, { nav.selectTab(LearnTab(it)) })
        PageHead(
            "Lessons",
            about = "${FOUNDATION_LESSONS.size} class-sized lessons, in the order to take them: survive underneath " +
                "first, then the guard, then the rest. Review brings new cards in this order too.",
        )
        ProgressLegend()
        FOUNDATION_LESSONS.forEachIndexed { index, lesson ->
            LessonCard(lib, lesson, number = index + 1, current = lesson == current, nav = nav)
        }
    }
}

@Composable
private fun LessonCard(
    lib: Library,
    lesson: Lesson,
    number: Int,
    current: Boolean,
    nav: Nav,
    modifier: Modifier = Modifier,
) {
    val cards = lessonCards(lib, lesson)
    val progress = lessonProgress(lib, lesson)
    Card(modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s1)) {
            Eyebrow("Lesson $number" + if (current) " · you are here" else "")
            BjjText(lesson.title, style = Type.h2)
            BjjText(lesson.summary, color = BjjTheme.palette.muted)
        }
        if (cards.isEmpty()) {
            BjjText("Nothing from this lesson is in your library.", color = BjjTheme.palette.muted)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s1)) {
                BjjText("${progress.learned} of ${plural(progress.total, "card")} learned", style = Type.small)
                ProgressBar(progress.learned.toFloat() / progress.total)
            }
            Wrap {
                for (card in cards) {
                    Chip(cardName(lib, card).orEmpty(), { nav.open(studyItemKey(card)) }) {
                        ProgressDot(progressOf(reviewOf(lib, card.kind, card.id)))
                    }
                }
            }
            SoftBlock("Practise with a partner, about 15 minutes. ${lesson.practice}")
            Button(
                "Study this lesson",
                { nav.selectTab(ReviewTab(Deck.All.key, lesson.id)) },
                Modifier.fillMaxWidth(),
                kind = if (current) ButtonKind.Primary else ButtonKind.Plain,
            )
        }
    }
}

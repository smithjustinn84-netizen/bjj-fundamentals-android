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
import dev.justinsmith.bjj.resources.Res
import dev.justinsmith.bjj.resources.common_lessons
import dev.justinsmith.bjj.resources.learn_lesson_empty
import dev.justinsmith.bjj.resources.learn_lesson_learned
import dev.justinsmith.bjj.resources.learn_lesson_number
import dev.justinsmith.bjj.resources.learn_lesson_number_current
import dev.justinsmith.bjj.resources.learn_lesson_practice
import dev.justinsmith.bjj.resources.learn_lesson_study
import dev.justinsmith.bjj.resources.learn_lessons_about
import dev.justinsmith.bjj.ui.common.Columns
import dev.justinsmith.bjj.ui.common.LearnSwitch
import dev.justinsmith.bjj.ui.common.Nav
import dev.justinsmith.bjj.ui.common.Page
import dev.justinsmith.bjj.ui.common.PageHead
import dev.justinsmith.bjj.ui.common.ProgressLegend
import dev.justinsmith.bjj.ui.common.WithSnapshot
import dev.justinsmith.bjj.ui.common.progressOf
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

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
            stringResource(Res.string.common_lessons),
            about = stringResource(Res.string.learn_lessons_about, FOUNDATION_LESSONS.size),
        )
        ProgressLegend()
        Columns(FOUNDATION_LESSONS.withIndex().toList(), spacing = Spacing.s4) { (index, lesson) ->
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
    val heading = if (current) Res.string.learn_lesson_number_current else Res.string.learn_lesson_number
    Card(modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s1)) {
            Eyebrow(stringResource(heading, number))
            BjjText(lesson.title, style = Type.h2)
            BjjText(lesson.summary, color = BjjTheme.palette.muted)
        }
        if (cards.isEmpty()) {
            BjjText(stringResource(Res.string.learn_lesson_empty), color = BjjTheme.palette.muted)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s1)) {
                BjjText(
                    pluralStringResource(
                        Res.plurals.learn_lesson_learned,
                        progress.total,
                        progress.learned,
                        progress.total,
                    ),
                    style = Type.small,
                )
                ProgressBar(progress.learned.toFloat() / progress.total)
            }
            Wrap {
                for (card in cards) {
                    Chip(cardName(lib, card).orEmpty(), { nav.open(studyItemKey(card)) }) {
                        ProgressDot(progressOf(reviewOf(lib, card.kind, card.id)))
                    }
                }
            }
            SoftBlock(stringResource(Res.string.learn_lesson_practice, lesson.practice))
            Button(
                stringResource(Res.string.learn_lesson_study),
                { nav.selectTab(ReviewTab(Deck.All.key, lesson.id)) },
                Modifier.fillMaxWidth(),
                kind = if (current) ButtonKind.Primary else ButtonKind.Plain,
            )
        }
    }
}

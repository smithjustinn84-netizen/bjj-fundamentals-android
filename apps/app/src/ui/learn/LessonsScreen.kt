package dev.justinsmith.bjj.ui.learn

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.justinsmith.bjj.core.Deck
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
import dev.justinsmith.bjj.nav.Nav
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
import dev.justinsmith.bjj.ui.common.Loading
import dev.justinsmith.bjj.ui.common.Page
import dev.justinsmith.bjj.ui.common.PageHead
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LessonsRoute(nav: Nav) {
    val viewModel = koinViewModel<LessonsViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    when (val ready = state) {
        LessonsUiState.Loading -> Loading()
        is LessonsUiState.Ready -> LessonsScreen(ready, nav)
    }
}

@Composable
fun LessonsScreen(state: LessonsUiState.Ready, nav: Nav, modifier: Modifier = Modifier) {
    Page(modifier) {
        LearnSwitch(LearnSection.Lessons, { nav.selectTab(LearnTab(it)) })
        PageHead(
            stringResource(Res.string.common_lessons),
            about = stringResource(Res.string.learn_lessons_about, state.lessons.size),
        )
        ProgressLegend()
        Columns(state.lessons, spacing = Spacing.s4) { LessonCard(it, nav) }
    }
}

@Composable
private fun LessonCard(card: LessonCardUi, nav: Nav, modifier: Modifier = Modifier) {
    val lesson = card.lesson
    val cards = card.cards
    val progress = card.progress
    val current = card.current
    val heading = if (current) Res.string.learn_lesson_number_current else Res.string.learn_lesson_number
    Card(modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s1)) {
            Eyebrow(stringResource(heading, card.number))
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
                for (link in cards) {
                    Chip(link.name, { nav.open(studyItemKey(link.item)) }) { ProgressDot(link.progress) }
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

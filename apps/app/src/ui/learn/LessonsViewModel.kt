package dev.justinsmith.bjj.ui.learn

import androidx.lifecycle.ViewModel
import dev.justinsmith.bjj.bindings.AppData
import dev.justinsmith.bjj.core.FOUNDATION_LESSONS
import dev.justinsmith.bjj.core.Lesson
import dev.justinsmith.bjj.core.LessonProgress
import dev.justinsmith.bjj.core.Library
import dev.justinsmith.bjj.core.currentLesson
import dev.justinsmith.bjj.core.lessonCards
import dev.justinsmith.bjj.core.lessonProgress
import dev.justinsmith.bjj.ui.common.CardLink
import dev.justinsmith.bjj.ui.common.cardLink
import dev.justinsmith.bjj.ui.common.screenState
import kotlinx.coroutines.flow.StateFlow

/** One lesson as its card shows it. */
data class LessonCardUi(
    val lesson: Lesson,
    /** Counted from 1. */
    val number: Int,
    /** True for the first lesson with a card still to learn. */
    val current: Boolean,
    /** Its cards in the order they are taught; empty when the library holds none of them. */
    val cards: List<CardLink>,
    val progress: LessonProgress,
)

sealed interface LessonsUiState {
    data object Loading : LessonsUiState

    data class Ready(val lessons: List<LessonCardUi>) : LessonsUiState
}

fun lessonsUiState(lib: Library): LessonsUiState.Ready {
    val current = currentLesson(lib)
    return LessonsUiState.Ready(
        FOUNDATION_LESSONS.mapIndexed { index, lesson ->
            LessonCardUi(
                lesson = lesson,
                number = index + 1,
                current = lesson == current,
                cards = lessonCards(lib, lesson).map { cardLink(lib, it) },
                progress = lessonProgress(lib, lesson),
            )
        },
    )
}

class LessonsViewModel(appData: AppData) : ViewModel() {
    val uiState: StateFlow<LessonsUiState> = screenState(appData) { snapshot ->
        snapshot?.let { lessonsUiState(it.library) } ?: LessonsUiState.Loading
    }
}

package dev.justinsmith.bjj.ui.home

import androidx.lifecycle.ViewModel
import dev.justinsmith.bjj.bindings.AppData
import dev.justinsmith.bjj.bindings.Clock
import dev.justinsmith.bjj.core.BeltProgress
import dev.justinsmith.bjj.core.CardKind
import dev.justinsmith.bjj.core.DEFAULT_NEW_CARDS
import dev.justinsmith.bjj.core.Deck
import dev.justinsmith.bjj.core.DrillPick
import dev.justinsmith.bjj.core.FOUNDATION_LESSONS
import dev.justinsmith.bjj.core.Lesson
import dev.justinsmith.bjj.core.LessonProgress
import dev.justinsmith.bjj.core.LocalZone
import dev.justinsmith.bjj.core.PathStep
import dev.justinsmith.bjj.core.RECENT_CLASS_COUNT
import dev.justinsmith.bjj.core.STICKING_POINT_LIMIT
import dev.justinsmith.bjj.core.Situation
import dev.justinsmith.bjj.core.StudyItem
import dev.justinsmith.bjj.core.TrainingSummary
import dev.justinsmith.bjj.core.beltRank
import dev.justinsmith.bjj.core.buildQueue
import dev.justinsmith.bjj.core.conceptsToLearn
import dev.justinsmith.bjj.core.currentLesson
import dev.justinsmith.bjj.core.findSituation
import dev.justinsmith.bjj.core.lessonProgress
import dev.justinsmith.bjj.core.pickDrill
import dev.justinsmith.bjj.core.progressSummary
import dev.justinsmith.bjj.core.recentlyStuck
import dev.justinsmith.bjj.core.services.Snapshot
import dev.justinsmith.bjj.core.services.studyStats
import dev.justinsmith.bjj.core.stickingPoints
import dev.justinsmith.bjj.core.studyFirst
import dev.justinsmith.bjj.core.todaysPath
import dev.justinsmith.bjj.core.trainingSummary
import dev.justinsmith.bjj.ui.common.CardLink
import dev.justinsmith.bjj.ui.common.SearchUi
import dev.justinsmith.bjj.ui.common.cardLink
import dev.justinsmith.bjj.ui.common.screenState
import dev.justinsmith.bjj.ui.common.searchUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Today shows only the next few concepts; the Concepts page has them all. */
private const val HOME_CONCEPT_LIMIT = 6

/** The session the Review page would offer with its default settings. */
data class ReviewOffer(
    /** Cards a session would hold. */
    val ready: Int,
    val due: Int,
    /** New cards among them, and how many of those the training log put first. */
    val fresh: Int,
    val fromClass: Int,
    /** How many cards have ever been studied: none means this would be the first session. */
    val started: Int,
)

/** The lesson being worked through, with which one of how many it is. */
data class LessonOffer(val lesson: Lesson, val number: Int, val of: Int, val progress: LessonProgress)

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Ready(
        /** Today's date, in words. */
        val dateLabel: String,
        val streakDays: Int,
        val review: ReviewOffer,
        val rank: BeltProgress,
        val learned: Int,
        val total: Int,
        /** Null when no technique has a drill to suggest. */
        val drill: DrillPick?,
        val training: TrainingSummary,
        /** Where the student got stuck in recent classes. */
        val stuck: List<Situation>,
        val search: SearchUi,
        /** Null once every lesson is learned. */
        val lesson: LessonOffer?,
        val path: List<PathStep>,
        val sticking: List<CardLink>,
        /** The next few concepts to learn, and how many the library has in all. */
        val concepts: List<CardLink>,
        val conceptCount: Int,
    ) : HomeUiState
}

/** Everything Today shows, from what is stored, at [now]. */
fun homeUiState(snapshot: Snapshot, query: String, now: Long, zone: LocalZone): HomeUiState.Ready {
    val lib = snapshot.library
    val log = snapshot.trainingLog
    val progress = progressSummary(lib)
    val preview = buildQueue(lib, Deck.All, DEFAULT_NEW_CARDS, now, studyFirst(lib, log, RECENT_CLASS_COUNT))
    val lesson = currentLesson(lib)
    return HomeUiState.Ready(
        dateLabel = todayLabel(now, zone),
        streakDays = studyStats(snapshot, now, zone).streakDays,
        review = ReviewOffer(
            ready = preview.queue.size,
            due = preview.due.size,
            fresh = preview.queue.size - preview.due.size,
            fromClass = preview.fromClass,
            started = progress.started,
        ),
        rank = beltRank(progress.percentLearned),
        learned = progress.learned,
        total = progress.total,
        drill = pickDrill(lib, log),
        training = trainingSummary(log),
        stuck = recentlyStuck(log, RECENT_CLASS_COUNT).mapNotNull { findSituation(lib, it) },
        search = searchUi(lib, query),
        lesson = lesson?.let {
            LessonOffer(it, FOUNDATION_LESSONS.indexOf(it) + 1, FOUNDATION_LESSONS.size, lessonProgress(lib, it))
        },
        path = todaysPath(lib, snapshot.videos.size),
        sticking = stickingPoints(lib, STICKING_POINT_LIMIT).map { cardLink(lib, it) },
        concepts = conceptsToLearn(lib, HOME_CONCEPT_LIMIT).map { cardLink(lib, StudyItem(CardKind.Concept, it.id)) },
        conceptCount = lib.concepts.size,
    )
}

class HomeViewModel(appData: AppData, private val clock: Clock) : ViewModel() {
    private val query = MutableStateFlow("")

    val uiState: StateFlow<HomeUiState> = screenState(appData, query) { snapshot, query ->
        snapshot?.let { homeUiState(it, query, clock.now(), clock.zone) } ?: HomeUiState.Loading
    }

    fun search(text: String) {
        query.value = text
    }
}

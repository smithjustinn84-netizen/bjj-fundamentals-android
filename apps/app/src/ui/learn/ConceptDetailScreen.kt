package dev.justinsmith.bjj.ui.learn

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.justinsmith.bjj.core.Concept
import dev.justinsmith.bjj.core.ItemKind
import dev.justinsmith.bjj.design.components.BjjText
import dev.justinsmith.bjj.design.components.Card
import dev.justinsmith.bjj.design.components.Chip
import dev.justinsmith.bjj.design.components.Divider
import dev.justinsmith.bjj.design.components.Eyebrow
import dev.justinsmith.bjj.design.components.Section
import dev.justinsmith.bjj.design.components.StatusPill
import dev.justinsmith.bjj.design.components.Wrap
import dev.justinsmith.bjj.design.theme.Sizes
import dev.justinsmith.bjj.nav.ConceptDetail
import dev.justinsmith.bjj.nav.Editor
import dev.justinsmith.bjj.nav.LearnSection
import dev.justinsmith.bjj.nav.LearnTab
import dev.justinsmith.bjj.nav.Nav
import dev.justinsmith.bjj.nav.PositionDetail
import dev.justinsmith.bjj.resources.Res
import dev.justinsmith.bjj.resources.common_concepts
import dev.justinsmith.bjj.resources.learn_concept
import dev.justinsmith.bjj.resources.learn_concept_key_points
import dev.justinsmith.bjj.resources.learn_concept_on_to_positions
import dev.justinsmith.bjj.resources.learn_concept_where_it_matters
import dev.justinsmith.bjj.resources.learn_try_this_drill
import dev.justinsmith.bjj.ui.common.ItemPicture
import dev.justinsmith.bjj.ui.common.ItemRef
import dev.justinsmith.bjj.ui.common.Loading
import dev.justinsmith.bjj.ui.common.Missing
import dev.justinsmith.bjj.ui.common.Page
import dev.justinsmith.bjj.ui.common.PointList
import dev.justinsmith.bjj.ui.common.VideoActions
import dev.justinsmith.bjj.ui.common.label
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ConceptDetailRoute(key: ConceptDetail, nav: Nav) {
    val viewModel = koinViewModel<ConceptDetailViewModel> { parametersOf(key) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    when (val ready = state) {
        ConceptDetailUiState.Loading -> Loading()

        ConceptDetailUiState.Missing -> Missing(
            stringResource(Res.string.learn_concept),
            stringResource(Res.string.common_concepts),
            { nav.selectTab(LearnTab(LearnSection.Concepts)) },
        )

        is ConceptDetailUiState.Ready -> ConceptDetailScreen(ready, nav, viewModel.videoActions)
    }
}

@Composable
fun ConceptDetailScreen(
    state: ConceptDetailUiState.Ready,
    nav: Nav,
    videoActions: VideoActions,
    modifier: Modifier = Modifier,
) {
    val concept = state.concept
    val videos = state.videos
    val onToPositions = stringResource(Res.string.learn_concept_on_to_positions)
    Page(modifier, maxWidth = Sizes.readingMax) {
        DetailHeader(
            backLabel = stringResource(Res.string.common_concepts),
            onBack = nav.back,
            onEdit = { nav.open(Editor(ItemKind.Concept, concept.id)) },
            title = concept.name,
            eyebrow = {
                Eyebrow(stringResource(Res.string.learn_concept))
                StatusPill(state.progress.label, state.progress)
            },
            lead = concept.summary,
        )
        Card {
            ItemPicture(videos)
            Section(stringResource(Res.string.learn_concept_key_points)) { PointList(concept.keyPoints) }
            if (concept.drill.isNotEmpty()) {
                Divider()
                Section(stringResource(Res.string.learn_try_this_drill)) { BjjText(concept.drill) }
            }
            if (state.usedIn.isNotEmpty()) {
                Divider()
                Section(stringResource(Res.string.learn_concept_where_it_matters)) {
                    Wrap {
                        for (position in state.usedIn) Chip(position.name, { nav.open(PositionDetail(position.id)) })
                    }
                }
            }
            if (concept.notes.isNotBlank()) Divider()
            NotesBlock(concept.notes)
            Divider()
            LinkedVideos(ItemRef(ItemKind.Concept, concept.id), videos, videoActions)
        }
        Pager(
            previous = state.previous?.let { PagerTarget(it.name) { nav.replace(ConceptDetail(it.id)) } },
            next = state.next?.let { PagerTarget(it.name) { nav.replace(ConceptDetail(it.id)) } }
                ?: PagerTarget(onToPositions) { nav.selectTab(LearnTab(LearnSection.Positions)) },
        )
    }
}

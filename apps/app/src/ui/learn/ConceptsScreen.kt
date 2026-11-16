package dev.justinsmith.bjj.ui.learn

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.justinsmith.bjj.core.Concept
import dev.justinsmith.bjj.core.Deck
import dev.justinsmith.bjj.core.ItemKind
import dev.justinsmith.bjj.design.components.BjjText
import dev.justinsmith.bjj.design.components.Bullets
import dev.justinsmith.bjj.design.components.Button
import dev.justinsmith.bjj.design.components.ButtonKind
import dev.justinsmith.bjj.design.components.Card
import dev.justinsmith.bjj.design.components.Chip
import dev.justinsmith.bjj.design.components.Divider
import dev.justinsmith.bjj.design.components.Eyebrow
import dev.justinsmith.bjj.design.components.ProgressDot
import dev.justinsmith.bjj.design.components.Section
import dev.justinsmith.bjj.design.components.StatusPill
import dev.justinsmith.bjj.design.components.Tile
import dev.justinsmith.bjj.design.components.Wrap
import dev.justinsmith.bjj.design.graphics.ConceptArtwork
import dev.justinsmith.bjj.design.graphics.hasConceptArtwork
import dev.justinsmith.bjj.design.theme.BjjTheme
import dev.justinsmith.bjj.design.theme.Sizes
import dev.justinsmith.bjj.design.theme.Spacing
import dev.justinsmith.bjj.nav.ConceptDetail
import dev.justinsmith.bjj.nav.Editor
import dev.justinsmith.bjj.nav.LearnSection
import dev.justinsmith.bjj.nav.LearnTab
import dev.justinsmith.bjj.nav.PositionDetail
import dev.justinsmith.bjj.nav.ReviewTab
import dev.justinsmith.bjj.nav.itemKey
import dev.justinsmith.bjj.resources.Res
import dev.justinsmith.bjj.resources.common_concepts
import dev.justinsmith.bjj.resources.learn_concept
import dev.justinsmith.bjj.resources.learn_concept_key_points
import dev.justinsmith.bjj.resources.learn_concept_on_to_positions
import dev.justinsmith.bjj.resources.learn_concept_where_it_matters
import dev.justinsmith.bjj.resources.learn_concepts_about
import dev.justinsmith.bjj.resources.learn_concepts_empty
import dev.justinsmith.bjj.resources.learn_concepts_new
import dev.justinsmith.bjj.resources.learn_concepts_study
import dev.justinsmith.bjj.resources.learn_nothing_here
import dev.justinsmith.bjj.resources.learn_starter_concepts_missing
import dev.justinsmith.bjj.resources.learn_try_this_drill
import dev.justinsmith.bjj.ui.common.Columns
import dev.justinsmith.bjj.ui.common.DetailHeader
import dev.justinsmith.bjj.ui.common.ItemPicture
import dev.justinsmith.bjj.ui.common.ItemRef
import dev.justinsmith.bjj.ui.common.LearnSwitch
import dev.justinsmith.bjj.ui.common.LibrarySearch
import dev.justinsmith.bjj.ui.common.LinkedVideos
import dev.justinsmith.bjj.ui.common.Loading
import dev.justinsmith.bjj.ui.common.Missing
import dev.justinsmith.bjj.ui.common.Nav
import dev.justinsmith.bjj.ui.common.NotesBlock
import dev.justinsmith.bjj.ui.common.Page
import dev.justinsmith.bjj.ui.common.PageHead
import dev.justinsmith.bjj.ui.common.Pager
import dev.justinsmith.bjj.ui.common.PagerTarget
import dev.justinsmith.bjj.ui.common.ProgressLegend
import dev.justinsmith.bjj.ui.common.StarterOffer
import dev.justinsmith.bjj.ui.common.VideoActions
import dev.justinsmith.bjj.ui.common.label
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ConceptsRoute(nav: Nav) {
    val viewModel = koinViewModel<ConceptsViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    when (val ready = state) {
        ConceptsUiState.Loading -> Loading()

        is ConceptsUiState.Ready -> ConceptsScreen(
            state = ready,
            nav = nav,
            onSearch = viewModel::search,
            onAddStarter = viewModel::addStarter,
            onDismissStarter = viewModel::dismissStarter,
        )
    }
}

/** Every concept in learning order: its picture, its name, what it is, and how well it is known. */
@Composable
fun ConceptsScreen(
    state: ConceptsUiState.Ready,
    nav: Nav,
    onSearch: (String) -> Unit,
    onAddStarter: () -> Unit,
    onDismissStarter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val concepts = state.concepts
    Page(modifier) {
        LearnSwitch(LearnSection.Concepts, { nav.selectTab(LearnTab(it)) })
        PageHead(
            stringResource(Res.string.common_concepts),
            about = stringResource(Res.string.learn_concepts_about),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            if (concepts.isNotEmpty()) {
                Button(
                    stringResource(Res.string.learn_concepts_study),
                    { nav.selectTab(ReviewTab(Deck.Concept.key)) },
                    Modifier.weight(1f),
                    ButtonKind.Primary,
                )
            }
            Button(
                stringResource(Res.string.learn_concepts_new),
                { nav.open(Editor(ItemKind.Concept)) },
                Modifier.weight(1f),
            )
        }
        LibrarySearch(state.search, onSearch, { kind, id -> nav.open(itemKey(kind, id)) })
        StarterOffer(Res.plurals.learn_starter_concepts_missing, state.missingStarter, onAddStarter, onDismissStarter)
        if (concepts.isEmpty()) {
            BjjText(stringResource(Res.string.learn_concepts_empty), color = BjjTheme.palette.muted)
        } else {
            ProgressLegend()
            Columns(concepts) { concept ->
                Tile(
                    name = concept.name,
                    onClick = { nav.open(ConceptDetail(concept.id)) },
                    detail = concept.summary,
                    picture = if (hasConceptArtwork(concept.id)) ({ ConceptArtwork(concept.id) }) else null,
                ) { ProgressDot(concept.progress) }
            }
        }
    }
}

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
            ItemPicture(videos) { if (hasConceptArtwork(concept.id)) ConceptArtwork(concept.id) }
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

/** A list of points, or a line saying there are none yet. */
@Composable
fun PointList(items: List<String>, modifier: Modifier = Modifier, numbered: Boolean = false) {
    if (items.isEmpty()) {
        BjjText(stringResource(Res.string.learn_nothing_here), modifier, color = BjjTheme.palette.muted)
    } else {
        Bullets(items, modifier, numbered)
    }
}

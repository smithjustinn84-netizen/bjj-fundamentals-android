package dev.justinsmith.bjj.ui.learn

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.justinsmith.bjj.core.ItemKind
import dev.justinsmith.bjj.core.Position
import dev.justinsmith.bjj.core.PositionRole
import dev.justinsmith.bjj.core.label
import dev.justinsmith.bjj.core.roleGoals
import dev.justinsmith.bjj.core.roleLabel
import dev.justinsmith.bjj.design.components.BjjText
import dev.justinsmith.bjj.design.components.Button
import dev.justinsmith.bjj.design.components.ButtonSize
import dev.justinsmith.bjj.design.components.Card
import dev.justinsmith.bjj.design.components.Chip
import dev.justinsmith.bjj.design.components.Divider
import dev.justinsmith.bjj.design.components.Eyebrow
import dev.justinsmith.bjj.design.components.Section
import dev.justinsmith.bjj.design.components.SectionHead
import dev.justinsmith.bjj.design.components.Segmented
import dev.justinsmith.bjj.design.components.StatusPill
import dev.justinsmith.bjj.design.components.Wrap
import dev.justinsmith.bjj.design.theme.BjjTheme
import dev.justinsmith.bjj.design.theme.Sizes
import dev.justinsmith.bjj.design.theme.Spacing
import dev.justinsmith.bjj.design.theme.Type
import dev.justinsmith.bjj.nav.ConceptDetail
import dev.justinsmith.bjj.nav.Editor
import dev.justinsmith.bjj.nav.LearnSection
import dev.justinsmith.bjj.nav.LearnTab
import dev.justinsmith.bjj.nav.Nav
import dev.justinsmith.bjj.nav.PositionDetail
import dev.justinsmith.bjj.nav.TransitionDetail
import dev.justinsmith.bjj.resources.Res
import dev.justinsmith.bjj.resources.common_positions
import dev.justinsmith.bjj.resources.learn_add_technique
import dev.justinsmith.bjj.resources.learn_no_techniques_from_here
import dev.justinsmith.bjj.resources.learn_position
import dev.justinsmith.bjj.resources.learn_position_concepts
import dev.justinsmith.bjj.resources.learn_position_goals
import dev.justinsmith.bjj.resources.learn_position_learned
import dev.justinsmith.bjj.resources.learn_position_options
import dev.justinsmith.bjj.resources.learn_position_side_empty
import dev.justinsmith.bjj.resources.learn_position_ways_in
import dev.justinsmith.bjj.ui.common.ItemPicture
import dev.justinsmith.bjj.ui.common.ItemRef
import dev.justinsmith.bjj.ui.common.Loading
import dev.justinsmith.bjj.ui.common.Missing
import dev.justinsmith.bjj.ui.common.Page
import dev.justinsmith.bjj.ui.common.PointList
import dev.justinsmith.bjj.ui.common.TransitionList
import dev.justinsmith.bjj.ui.common.VideoActions
import dev.justinsmith.bjj.ui.common.label
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun PositionDetailRoute(key: PositionDetail, nav: Nav) {
    val viewModel = koinViewModel<PositionDetailViewModel> { parametersOf(key) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    when (val ready = state) {
        PositionDetailUiState.Loading -> Loading()

        PositionDetailUiState.Missing -> Missing(
            stringResource(Res.string.learn_position),
            stringResource(Res.string.common_positions),
            { nav.selectTab(LearnTab(LearnSection.Positions)) },
        )

        is PositionDetailUiState.Ready -> PositionDetailScreen(ready, nav, viewModel.videoActions)
    }
}

@Composable
fun PositionDetailScreen(
    state: PositionDetailUiState.Ready,
    nav: Nav,
    videoActions: VideoActions,
    modifier: Modifier = Modifier,
) {
    val position = state.position
    val videos = state.videos
    val concepts = state.concepts
    var side by rememberSaveable { mutableStateOf(state.openOn) }
    val openTechnique = { technique: String -> nav.open(TransitionDetail(technique)) }
    Page(modifier, maxWidth = Sizes.readingMax) {
        DetailHeader(
            backLabel = stringResource(Res.string.common_positions),
            onBack = nav.back,
            onEdit = { nav.open(Editor(ItemKind.Position, position.id)) },
            title = position.name,
            eyebrow = {
                Eyebrow(position.category.label)
                StatusPill(state.progress.label, state.progress)
            },
            lead = position.summary,
        )
        Card {
            ItemPicture(videos)
            // One side at a time: in a roll you are only ever on one of them.
            Segmented(PositionRole.entries, side, {
                side = it
            }, { roleLabel(position.category, it) }, Modifier.fillMaxWidth())
            Section(stringResource(Res.string.learn_position_goals)) { PointList(roleGoals(position, side)) }
            Divider()
            SideOptions(state, side, nav)
            if (concepts.isNotEmpty()) {
                Divider()
                Section(stringResource(Res.string.learn_position_concepts)) {
                    Wrap { for (concept in concepts) Chip(concept.name, { nav.open(ConceptDetail(concept.id)) }) }
                }
            }
            if (state.waysIn.isNotEmpty()) {
                Divider()
                Section(stringResource(Res.string.learn_position_ways_in)) {
                    TransitionList(state.waysIn, openTechnique)
                }
            }
            if (position.notes.isNotBlank()) Divider()
            NotesBlock(position.notes)
            Divider()
            LinkedVideos(ItemRef(ItemKind.Position, position.id), videos, videoActions)
        }
        Pager(
            previous = state.previous?.let { PagerTarget(it.name) { nav.replace(PositionDetail(it.id)) } },
            next = state.next?.let { PagerTarget(it.name) { nav.replace(PositionDetail(it.id)) } },
        )
    }
}

/** What one side of a position can do: how much of it is learned, then the techniques open to that side. */
@Composable
private fun SideOptions(
    state: PositionDetailUiState.Ready,
    side: PositionRole,
    nav: Nav,
    modifier: Modifier = Modifier,
) {
    val options = state.options[side].orEmpty()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
        SectionHead(stringResource(Res.string.learn_position_options)) {
            Button(
                stringResource(Res.string.learn_add_technique),
                { nav.open(Editor(ItemKind.Transition, fromPositionId = state.position.id)) },
                size = ButtonSize.Small,
            )
        }
        if (state.total > 0) {
            val line = stringResource(Res.string.learn_position_learned, state.learned, state.total)
            BjjText(line, style = Type.small, color = BjjTheme.palette.muted)
        }
        when {
            !state.hasTechniques -> BjjText(
                stringResource(Res.string.learn_no_techniques_from_here),
                color = BjjTheme.palette.muted,
            )

            options.isNotEmpty() -> TransitionList(options, { nav.open(TransitionDetail(it)) })

            else -> BjjText(
                stringResource(Res.string.learn_position_side_empty),
                color = BjjTheme.palette.muted,
            )
        }
    }
}

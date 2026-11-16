package dev.justinsmith.bjj.ui.learn

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import dev.justinsmith.bjj.design.components.ButtonKind
import dev.justinsmith.bjj.design.components.ButtonSize
import dev.justinsmith.bjj.design.components.Card
import dev.justinsmith.bjj.design.components.Chip
import dev.justinsmith.bjj.design.components.Divider
import dev.justinsmith.bjj.design.components.Eyebrow
import dev.justinsmith.bjj.design.components.GroupTitle
import dev.justinsmith.bjj.design.components.Icon
import dev.justinsmith.bjj.design.components.ProgressDot
import dev.justinsmith.bjj.design.components.Section
import dev.justinsmith.bjj.design.components.SectionHead
import dev.justinsmith.bjj.design.components.Segmented
import dev.justinsmith.bjj.design.components.StatusPill
import dev.justinsmith.bjj.design.components.Tile
import dev.justinsmith.bjj.design.components.Wrap
import dev.justinsmith.bjj.design.graphics.PositionArtwork
import dev.justinsmith.bjj.design.graphics.hasPositionArtwork
import dev.justinsmith.bjj.design.icons.BjjIcons
import dev.justinsmith.bjj.design.theme.BjjTheme
import dev.justinsmith.bjj.design.theme.Sizes
import dev.justinsmith.bjj.design.theme.Spacing
import dev.justinsmith.bjj.design.theme.Type
import dev.justinsmith.bjj.nav.ConceptDetail
import dev.justinsmith.bjj.nav.Editor
import dev.justinsmith.bjj.nav.LearnSection
import dev.justinsmith.bjj.nav.LearnTab
import dev.justinsmith.bjj.nav.PositionDetail
import dev.justinsmith.bjj.nav.TransitionDetail
import dev.justinsmith.bjj.nav.itemKey
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
import dev.justinsmith.bjj.resources.learn_positions_about
import dev.justinsmith.bjj.resources.learn_positions_empty_add
import dev.justinsmith.bjj.resources.learn_positions_empty_text
import dev.justinsmith.bjj.resources.learn_positions_empty_title
import dev.justinsmith.bjj.resources.learn_positions_new
import dev.justinsmith.bjj.resources.learn_positions_orphans
import dev.justinsmith.bjj.resources.learn_positions_techniques_learned
import dev.justinsmith.bjj.resources.learn_positions_view_list
import dev.justinsmith.bjj.resources.learn_positions_view_map
import dev.justinsmith.bjj.ui.common.Columns
import dev.justinsmith.bjj.ui.common.DetailHeader
import dev.justinsmith.bjj.ui.common.EmptyState
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
import dev.justinsmith.bjj.ui.common.RowCard
import dev.justinsmith.bjj.ui.common.TransitionList
import dev.justinsmith.bjj.ui.common.VideoActions
import dev.justinsmith.bjj.ui.common.label
import dev.justinsmith.bjj.ui.map.PositionMap
import dev.justinsmith.bjj.ui.map.roomForMap
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** The two ways to look at the positions. */
enum class PositionsView { List, Map }

private val PositionsView.label: StringResource
    get() = when (this) {
        PositionsView.List -> Res.string.learn_positions_view_list
        PositionsView.Map -> Res.string.learn_positions_view_map
    }

@Composable
fun PositionsRoute(nav: Nav) {
    val viewModel = koinViewModel<PositionsViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    when (val ready = state) {
        PositionsUiState.Loading -> Loading()
        is PositionsUiState.Ready -> PositionsScreen(ready, nav, viewModel::search)
    }
}

@Composable
fun PositionsScreen(
    state: PositionsUiState.Ready,
    nav: Nav,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Null until the user picks a view; until then the window decides: the map is hard to read on a phone.
    var chosen by rememberSaveable { mutableStateOf<PositionsView?>(null) }
    val view = chosen ?: if (roomForMap()) PositionsView.Map else PositionsView.List
    val orphans = state.orphans
    Page(modifier) {
        LearnSwitch(LearnSection.Positions, { nav.selectTab(LearnTab(it)) })
        PageHead(
            stringResource(Res.string.common_positions),
            about = stringResource(Res.string.learn_positions_about),
        )
        Segmented(
            PositionsView.entries,
            view,
            { chosen = it },
            { stringResource(it.label) },
            Modifier.fillMaxWidth(),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            Button(
                stringResource(Res.string.learn_positions_new),
                { nav.open(Editor(ItemKind.Position)) },
                Modifier.weight(1f),
            )
            Button(
                stringResource(Res.string.learn_add_technique),
                { nav.open(Editor(ItemKind.Transition)) },
                Modifier.weight(1f),
            )
        }
        LibrarySearch(state.search, onSearch, { kind, id -> nav.open(itemKey(kind, id)) })
        when {
            state.groups.isEmpty() -> Card {
                EmptyState(
                    icon = BjjIcons.Map,
                    title = stringResource(Res.string.learn_positions_empty_title),
                    text = stringResource(Res.string.learn_positions_empty_text),
                ) {
                    Button(
                        stringResource(Res.string.learn_positions_empty_add),
                        { nav.open(Editor(ItemKind.Position)) },
                        kind = ButtonKind.Primary,
                    )
                }
            }

            view == PositionsView.Map -> PositionMap(state.map, nav)

            else -> {
                ProgressLegend()
                PositionList(state.groups, nav)
            }
        }
        if (orphans.isNotEmpty()) {
            GroupTitle(stringResource(Res.string.learn_positions_orphans))
            RowCard { TransitionList(orphans, { nav.open(TransitionDetail(it)) }) }
        }
    }
}

/** Every position as a row, grouped by category: its picture, its name and how much of it is learned. */
@Composable
private fun PositionList(groups: List<PositionGroup>, nav: Nav, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
        for (group in groups) {
            GroupTitle(group.category.label)
            Columns(group.positions) { position ->
                Tile(
                    name = position.name,
                    onClick = { nav.open(PositionDetail(position.id)) },
                    detail = if (position.total > 0) {
                        stringResource(
                            Res.string.learn_positions_techniques_learned,
                            position.learned,
                            position.total,
                        )
                    } else {
                        stringResource(Res.string.learn_no_techniques_from_here)
                    },
                    picture = if (hasPositionArtwork(position.id)) ({ PositionArtwork(position.id) }) else null,
                ) {
                    ProgressDot(position.progress)
                    Icon(BjjIcons.Chevron, description = null, tint = BjjTheme.palette.muted)
                }
            }
        }
    }
}

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

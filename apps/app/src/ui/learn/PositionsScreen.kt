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
import dev.justinsmith.bjj.core.label
import dev.justinsmith.bjj.design.components.Button
import dev.justinsmith.bjj.design.components.ButtonKind
import dev.justinsmith.bjj.design.components.Card
import dev.justinsmith.bjj.design.components.GroupTitle
import dev.justinsmith.bjj.design.components.Icon
import dev.justinsmith.bjj.design.components.ProgressDot
import dev.justinsmith.bjj.design.components.Segmented
import dev.justinsmith.bjj.design.components.Tile
import dev.justinsmith.bjj.design.graphics.PositionArtwork
import dev.justinsmith.bjj.design.graphics.hasPositionArtwork
import dev.justinsmith.bjj.design.icons.BjjIcons
import dev.justinsmith.bjj.design.theme.BjjTheme
import dev.justinsmith.bjj.design.theme.Spacing
import dev.justinsmith.bjj.nav.Editor
import dev.justinsmith.bjj.nav.LearnSection
import dev.justinsmith.bjj.nav.LearnTab
import dev.justinsmith.bjj.nav.Nav
import dev.justinsmith.bjj.nav.PositionDetail
import dev.justinsmith.bjj.nav.TransitionDetail
import dev.justinsmith.bjj.nav.itemKey
import dev.justinsmith.bjj.resources.Res
import dev.justinsmith.bjj.resources.common_positions
import dev.justinsmith.bjj.resources.learn_add_technique
import dev.justinsmith.bjj.resources.learn_no_techniques_from_here
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
import dev.justinsmith.bjj.ui.common.EmptyState
import dev.justinsmith.bjj.ui.common.LibrarySearch
import dev.justinsmith.bjj.ui.common.Loading
import dev.justinsmith.bjj.ui.common.Page
import dev.justinsmith.bjj.ui.common.PageHead
import dev.justinsmith.bjj.ui.common.RowCard
import dev.justinsmith.bjj.ui.common.TransitionList
import dev.justinsmith.bjj.ui.common.label
import dev.justinsmith.bjj.ui.map.PositionMap
import dev.justinsmith.bjj.ui.map.roomForMap
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

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

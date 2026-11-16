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
import dev.justinsmith.bjj.core.CardKind
import dev.justinsmith.bjj.core.ItemKind
import dev.justinsmith.bjj.core.Library
import dev.justinsmith.bjj.core.Position
import dev.justinsmith.bjj.core.PositionCategory
import dev.justinsmith.bjj.core.PositionRole
import dev.justinsmith.bjj.core.VideoBookmark
import dev.justinsmith.bjj.core.connections
import dev.justinsmith.bjj.core.label
import dev.justinsmith.bjj.core.positionOptions
import dev.justinsmith.bjj.core.positionProgress
import dev.justinsmith.bjj.core.reviewOf
import dev.justinsmith.bjj.core.roleGoals
import dev.justinsmith.bjj.core.roleLabel
import dev.justinsmith.bjj.core.services.linkedVideos
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
import dev.justinsmith.bjj.ui.common.Direction
import dev.justinsmith.bjj.ui.common.EmptyState
import dev.justinsmith.bjj.ui.common.ItemPicture
import dev.justinsmith.bjj.ui.common.ItemRef
import dev.justinsmith.bjj.ui.common.LearnSwitch
import dev.justinsmith.bjj.ui.common.LibrarySearch
import dev.justinsmith.bjj.ui.common.LinkedVideos
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
import dev.justinsmith.bjj.ui.common.WithSnapshot
import dev.justinsmith.bjj.ui.common.label
import dev.justinsmith.bjj.ui.common.progressOf
import dev.justinsmith.bjj.ui.common.videoActions
import dev.justinsmith.bjj.ui.map.PositionMap
import dev.justinsmith.bjj.ui.map.roomForMap
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** The two ways to look at the positions. */
enum class PositionsView { List, Map }

private val PositionsView.label: StringResource
    get() = when (this) {
        PositionsView.List -> Res.string.learn_positions_view_list
        PositionsView.Map -> Res.string.learn_positions_view_map
    }

@Composable
fun PositionsRoute(nav: Nav) {
    WithSnapshot { snapshot, _ -> PositionsScreen(snapshot.library, nav) }
}

@Composable
fun PositionsScreen(lib: Library, nav: Nav, modifier: Modifier = Modifier) {
    // Null until the user picks a view; until then the window decides: the map is hard to read on a phone.
    var chosen by rememberSaveable { mutableStateOf<PositionsView?>(null) }
    val view = chosen ?: if (roomForMap()) PositionsView.Map else PositionsView.List
    val orphans = lib.transitions.filter { it.fromId !in lib.positionById }
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
        LibrarySearch(lib, { kind, id -> nav.open(itemKey(kind, id)) })
        when {
            lib.positions.isEmpty() -> Card {
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

            view == PositionsView.Map -> PositionMap(lib, nav)

            else -> {
                ProgressLegend()
                PositionList(lib, nav)
            }
        }
        if (orphans.isNotEmpty()) {
            GroupTitle(stringResource(Res.string.learn_positions_orphans))
            RowCard { TransitionList(orphans, lib.positionById, Direction.Out, { nav.open(TransitionDetail(it)) }) }
        }
    }
}

/** Every position as a row, grouped by category: its picture, its name and how much of it is learned. */
@Composable
private fun PositionList(lib: Library, nav: Nav, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
        for (category in PositionCategory.entries) {
            val inCategory = lib.positions.filter { it.category == category }
            if (inCategory.isEmpty()) continue
            GroupTitle(category.label)
            Columns(inCategory) { position ->
                val progress = positionProgress(lib, position.id)
                Tile(
                    name = position.name,
                    onClick = { nav.open(PositionDetail(position.id)) },
                    detail = if (progress.total > 0) {
                        stringResource(
                            Res.string.learn_positions_techniques_learned,
                            progress.learned,
                            progress.total,
                        )
                    } else {
                        stringResource(Res.string.learn_no_techniques_from_here)
                    },
                    picture = if (hasPositionArtwork(position.id)) ({ PositionArtwork(position.id) }) else null,
                ) {
                    ProgressDot(progressOf(reviewOf(lib, CardKind.Position, position.id)))
                    Icon(BjjIcons.Chevron, description = null, tint = BjjTheme.palette.muted)
                }
            }
        }
    }
}

@Composable
fun PositionDetailRoute(id: String, side: PositionRole?, nav: Nav) {
    WithSnapshot { snapshot, viewModel ->
        val position = snapshot.library.positionById[id]
        if (position == null) {
            Missing(
                stringResource(Res.string.learn_position),
                stringResource(Res.string.common_positions),
                { nav.selectTab(LearnTab(LearnSection.Positions)) },
            )
        } else {
            PositionDetailScreen(
                position = position,
                lib = snapshot.library,
                videos = linkedVideos(snapshot.videos, ItemKind.Position, id),
                // A link from the log or Today can open the page on the side the student was stuck on.
                openOn = side ?: PositionRole.Top,
                nav = nav,
                videoActions = viewModel.videoActions(),
            )
        }
    }
}

@Composable
fun PositionDetailScreen(
    position: Position,
    lib: Library,
    videos: List<VideoBookmark>,
    openOn: PositionRole,
    nav: Nav,
    videoActions: VideoActions,
    modifier: Modifier = Modifier,
) {
    var side by rememberSaveable { mutableStateOf(openOn) }
    val incoming = connections(lib, position.id).incoming
    val concepts = position.conceptIds.mapNotNull { lib.conceptById[it] }
    val index = lib.positions.indexOfFirst { it.id == position.id }
    val state = reviewOf(lib, CardKind.Position, position.id)
    val openTechnique = { technique: String -> nav.open(TransitionDetail(technique)) }
    Page(modifier, maxWidth = Sizes.readingMax) {
        DetailHeader(
            backLabel = stringResource(Res.string.common_positions),
            onBack = nav.back,
            onEdit = { nav.open(Editor(ItemKind.Position, position.id)) },
            title = position.name,
            eyebrow = {
                Eyebrow(position.category.label)
                StatusPill(progressOf(state).label, progressOf(state))
            },
            lead = position.summary,
        )
        Card {
            ItemPicture(videos) { if (hasPositionArtwork(position.id)) PositionArtwork(position.id) }
            // One side at a time: in a roll you are only ever on one of them.
            Segmented(PositionRole.entries, side, {
                side = it
            }, { roleLabel(position.category, it) }, Modifier.fillMaxWidth())
            Section(stringResource(Res.string.learn_position_goals)) { PointList(roleGoals(position, side)) }
            Divider()
            SideOptions(position, lib, side, nav)
            if (concepts.isNotEmpty()) {
                Divider()
                Section(stringResource(Res.string.learn_position_concepts)) {
                    Wrap { for (concept in concepts) Chip(concept.name, { nav.open(ConceptDetail(concept.id)) }) }
                }
            }
            if (incoming.isNotEmpty()) {
                Divider()
                Section(stringResource(Res.string.learn_position_ways_in)) {
                    TransitionList(incoming, lib.positionById, Direction.In, openTechnique)
                }
            }
            if (position.notes.isNotBlank()) Divider()
            NotesBlock(position.notes)
            Divider()
            LinkedVideos(ItemRef(ItemKind.Position, position.id), videos, videoActions)
        }
        Pager(
            previous = lib.positions.getOrNull(index - 1)?.let {
                PagerTarget(it.name) { nav.replace(PositionDetail(it.id)) }
            },
            next = lib.positions.getOrNull(index + 1)?.let {
                PagerTarget(it.name) { nav.replace(PositionDetail(it.id)) }
            },
        )
    }
}

/** What one side of a position can do: how much of it is learned, then the techniques open to that side. */
@Composable
private fun SideOptions(position: Position, lib: Library, side: PositionRole, nav: Nav, modifier: Modifier = Modifier) {
    val outgoing = connections(lib, position.id).outgoing
    val options = positionOptions(lib, position.id)
    val progress = positionProgress(lib, position.id)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
        SectionHead(stringResource(Res.string.learn_position_options)) {
            Button(
                stringResource(Res.string.learn_add_technique),
                { nav.open(Editor(ItemKind.Transition, fromPositionId = position.id)) },
                size = ButtonSize.Small,
            )
        }
        if (progress.total > 0) {
            val line = stringResource(Res.string.learn_position_learned, progress.learned, progress.total)
            BjjText(line, style = Type.small, color = BjjTheme.palette.muted)
        }
        when {
            outgoing.isEmpty() -> BjjText(
                stringResource(Res.string.learn_no_techniques_from_here),
                color = BjjTheme.palette.muted,
            )

            options[side].isNotEmpty() -> TransitionList(
                options[side],
                lib.positionById,
                Direction.Out,
                { nav.open(TransitionDetail(it)) },
            )

            else -> BjjText(
                stringResource(Res.string.learn_position_side_empty),
                color = BjjTheme.palette.muted,
            )
        }
    }
}

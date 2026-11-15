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
import dev.justinsmith.bjj.ui.common.DetailHeader
import dev.justinsmith.bjj.ui.common.Direction
import dev.justinsmith.bjj.ui.common.EmptyState
import dev.justinsmith.bjj.ui.common.ItemPicture
import dev.justinsmith.bjj.ui.common.ItemRef
import dev.justinsmith.bjj.ui.common.LearnSwitch
import dev.justinsmith.bjj.ui.common.LibrarySearch
import dev.justinsmith.bjj.ui.common.LinkedVideos
import dev.justinsmith.bjj.ui.common.Missing
import dev.justinsmith.bjj.ui.common.NO_TECHNIQUES_FROM_HERE
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

/** The two ways to look at the positions. */
enum class PositionsView { List, Map }

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
        PageHead("Positions", about = "Each position, and the techniques that move you between them.")
        Segmented(PositionsView.entries, view, { chosen = it }, { it.name }, Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            Button("+ Position", { nav.open(Editor(ItemKind.Position)) }, Modifier.weight(1f))
            Button("+ Technique", { nav.open(Editor(ItemKind.Transition)) }, Modifier.weight(1f))
        }
        LibrarySearch(lib, { kind, id -> nav.open(itemKey(kind, id)) })
        when {
            lib.positions.isEmpty() -> Card {
                EmptyState(
                    icon = BjjIcons.Map,
                    title = "No positions yet",
                    text = "Add a position, then the techniques that connect it to others, and the map will draw " +
                        "itself.",
                ) {
                    Button("Add a position", { nav.open(Editor(ItemKind.Position)) }, kind = ButtonKind.Primary)
                }
            }

            view == PositionsView.Map -> PositionMap(lib, nav)

            else -> {
                ProgressLegend()
                PositionList(lib, nav)
            }
        }
        if (orphans.isNotEmpty()) {
            GroupTitle("Techniques without a starting position")
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
            for (position in inCategory) {
                val progress = positionProgress(lib, position.id)
                Tile(
                    name = position.name,
                    onClick = { nav.open(PositionDetail(position.id)) },
                    detail = if (progress.total > 0) {
                        "${progress.learned} of ${progress.total} techniques learned"
                    } else {
                        NO_TECHNIQUES_FROM_HERE
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
            Missing("Position", "Positions", { nav.selectTab(LearnTab(LearnSection.Positions)) })
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
    val (outgoing, incoming) = connections(lib, position.id)
    val options = positionOptions(lib, position.id)
    val progress = positionProgress(lib, position.id)
    val concepts = position.conceptIds.mapNotNull { lib.conceptById[it] }
    val index = lib.positions.indexOfFirst { it.id == position.id }
    val state = reviewOf(lib, CardKind.Position, position.id)
    val openTechnique = { technique: String -> nav.open(TransitionDetail(technique)) }
    Page(modifier, maxWidth = Sizes.readingMax) {
        DetailHeader(
            backLabel = "Positions",
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
            Section("What you want") { PointList(roleGoals(position, side)) }
            Divider()
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                SectionHead("What you can do") {
                    Button("+ Technique", {
                        nav.open(Editor(ItemKind.Transition, fromPositionId = position.id))
                    }, size = ButtonSize.Small)
                }
                if (progress.total > 0) {
                    val line = "${progress.learned} of ${progress.total} learned from this position"
                    BjjText(line, style = Type.small, color = BjjTheme.palette.muted)
                }
                when {
                    outgoing.isEmpty() -> BjjText(NO_TECHNIQUES_FROM_HERE, color = BjjTheme.palette.muted)

                    options[side].isNotEmpty() -> TransitionList(
                        options[side],
                        lib.positionById,
                        Direction.Out,
                        openTechnique,
                    )

                    else -> BjjText(
                        "Nothing for this side yet. Add a technique so you have an answer here.",
                        color = BjjTheme.palette.muted,
                    )
                }
            }
            if (concepts.isNotEmpty()) {
                Divider()
                Section("Concepts to remember") {
                    Wrap { for (concept in concepts) Chip(concept.name, { nav.open(ConceptDetail(concept.id)) }) }
                }
            }
            if (incoming.isNotEmpty()) {
                Divider()
                Section("Ways to get here") { TransitionList(incoming, lib.positionById, Direction.In, openTechnique) }
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

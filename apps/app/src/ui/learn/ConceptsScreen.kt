package dev.justinsmith.bjj.ui.learn

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.justinsmith.bjj.core.CardKind
import dev.justinsmith.bjj.core.Concept
import dev.justinsmith.bjj.core.Deck
import dev.justinsmith.bjj.core.ItemKind
import dev.justinsmith.bjj.core.Library
import dev.justinsmith.bjj.core.VideoBookmark
import dev.justinsmith.bjj.core.reviewOf
import dev.justinsmith.bjj.core.services.linkedVideos
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
import dev.justinsmith.bjj.ui.common.DetailHeader
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
import dev.justinsmith.bjj.ui.common.StarterOffer
import dev.justinsmith.bjj.ui.common.VideoActions
import dev.justinsmith.bjj.ui.common.WithSnapshot
import dev.justinsmith.bjj.ui.common.label
import dev.justinsmith.bjj.ui.common.progressOf
import dev.justinsmith.bjj.ui.common.videoActions

@Composable
fun ConceptsRoute(nav: Nav) {
    WithSnapshot { snapshot, viewModel ->
        ConceptsScreen(
            lib = snapshot.library,
            missing = snapshot.missingStarterConcepts.map { it.name },
            nav = nav,
            onAddStarter = viewModel::addMissingStarterConcepts,
            onDismissStarter = viewModel::dismissMissingStarterConcepts,
        )
    }
}

/** Every concept in learning order: its picture, its name, what it is, and how well it is known. */
@Composable
fun ConceptsScreen(
    lib: Library,
    missing: List<String>,
    nav: Nav,
    onAddStarter: () -> Unit,
    onDismissStarter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Page(modifier) {
        LearnSwitch(LearnSection.Concepts, { nav.selectTab(LearnTab(it)) })
        PageHead(
            "Concepts",
            about = "The big ideas that make every technique work. Review brings each one in beside the escapes " +
                "that use it.",
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            if (lib.concepts.isNotEmpty()) {
                Button("Study concepts", {
                    nav.selectTab(ReviewTab(Deck.Concept.key))
                }, Modifier.weight(1f), ButtonKind.Primary)
            }
            Button("+ New concept", { nav.open(Editor(ItemKind.Concept)) }, Modifier.weight(1f))
        }
        LibrarySearch(lib, { kind, id -> nav.open(itemKey(kind, id)) })
        StarterOffer("concept", missing, onAddStarter, onDismissStarter)
        if (lib.concepts.isEmpty()) {
            BjjText("No concepts yet.", color = BjjTheme.palette.muted)
        } else {
            ProgressLegend()
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                for (concept in lib.concepts) {
                    Tile(
                        name = concept.name,
                        onClick = { nav.open(ConceptDetail(concept.id)) },
                        detail = concept.summary,
                        picture = if (hasConceptArtwork(concept.id)) ({ ConceptArtwork(concept.id) }) else null,
                    ) {
                        ProgressDot(progressOf(reviewOf(lib, CardKind.Concept, concept.id)))
                    }
                }
            }
        }
    }
}

@Composable
fun ConceptDetailRoute(id: String, nav: Nav) {
    WithSnapshot { snapshot, viewModel ->
        val concept = snapshot.library.conceptById[id]
        if (concept == null) {
            Missing("Concept", "Concepts", { nav.selectTab(LearnTab(LearnSection.Concepts)) })
        } else {
            ConceptDetailScreen(
                concept = concept,
                lib = snapshot.library,
                videos = linkedVideos(snapshot.videos, ItemKind.Concept, id),
                nav = nav,
                videoActions = viewModel.videoActions(),
            )
        }
    }
}

@Composable
fun ConceptDetailScreen(
    concept: Concept,
    lib: Library,
    videos: List<VideoBookmark>,
    nav: Nav,
    videoActions: VideoActions,
    modifier: Modifier = Modifier,
) {
    val usedIn = lib.positions.filter { concept.id in it.conceptIds }
    val index = lib.concepts.indexOfFirst { it.id == concept.id }
    val previous = lib.concepts.getOrNull(index - 1)
    val next = lib.concepts.getOrNull(index + 1)
    val state = reviewOf(lib, CardKind.Concept, concept.id)
    Page(modifier, maxWidth = Sizes.readingMax) {
        DetailHeader(
            backLabel = "Concepts",
            onBack = nav.back,
            onEdit = { nav.open(Editor(ItemKind.Concept, concept.id)) },
            title = concept.name,
            eyebrow = {
                Eyebrow("Concept")
                StatusPill(progressOf(state).label, progressOf(state))
            },
            lead = concept.summary,
        )
        Card {
            ItemPicture(videos) { if (hasConceptArtwork(concept.id)) ConceptArtwork(concept.id) }
            Section("Key points") { PointList(concept.keyPoints) }
            if (concept.drill.isNotEmpty()) {
                Divider()
                Section("Try this drill") { BjjText(concept.drill) }
            }
            if (usedIn.isNotEmpty()) {
                Divider()
                Section("Where it matters") {
                    Wrap { for (position in usedIn) Chip(position.name, { nav.open(PositionDetail(position.id)) }) }
                }
            }
            if (concept.notes.isNotBlank()) Divider()
            NotesBlock(concept.notes)
            Divider()
            LinkedVideos(ItemRef(ItemKind.Concept, concept.id), videos, videoActions)
        }
        Pager(
            previous = previous?.let { PagerTarget(it.name) { nav.replace(ConceptDetail(it.id)) } },
            next = next?.let { PagerTarget(it.name) { nav.replace(ConceptDetail(it.id)) } }
                ?: PagerTarget("On to positions") { nav.selectTab(LearnTab(LearnSection.Positions)) },
        )
    }
}

/** A list of points, or a line saying there are none yet. */
@Composable
fun PointList(items: List<String>, modifier: Modifier = Modifier, numbered: Boolean = false) {
    if (items.isEmpty()) {
        BjjText("Nothing here yet.", modifier, color = BjjTheme.palette.muted)
    } else {
        Bullets(items, modifier, numbered)
    }
}

package dev.justinsmith.bjj.ui.learn

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.justinsmith.bjj.core.CardKind
import dev.justinsmith.bjj.core.ItemKind
import dev.justinsmith.bjj.core.Library
import dev.justinsmith.bjj.core.Transition
import dev.justinsmith.bjj.core.VideoBookmark
import dev.justinsmith.bjj.core.connections
import dev.justinsmith.bjj.core.endRole
import dev.justinsmith.bjj.core.endSideLabel
import dev.justinsmith.bjj.core.endsTheRoll
import dev.justinsmith.bjj.core.isOnBlueBeltTest
import dev.justinsmith.bjj.core.label
import dev.justinsmith.bjj.core.performerRole
import dev.justinsmith.bjj.core.reviewOf
import dev.justinsmith.bjj.core.roleLabel
import dev.justinsmith.bjj.core.services.linkedVideos
import dev.justinsmith.bjj.core.techniqueDrill
import dev.justinsmith.bjj.design.components.BjjText
import dev.justinsmith.bjj.design.components.Card
import dev.justinsmith.bjj.design.components.Chip
import dev.justinsmith.bjj.design.components.Divider
import dev.justinsmith.bjj.design.components.Icon
import dev.justinsmith.bjj.design.components.Section
import dev.justinsmith.bjj.design.components.SoftBlock
import dev.justinsmith.bjj.design.components.StatusPill
import dev.justinsmith.bjj.design.components.Tag
import dev.justinsmith.bjj.design.components.Wrap
import dev.justinsmith.bjj.design.icons.BjjIcons
import dev.justinsmith.bjj.design.theme.BjjTheme
import dev.justinsmith.bjj.design.theme.Sizes
import dev.justinsmith.bjj.design.theme.Type
import dev.justinsmith.bjj.design.theme.typeColor
import dev.justinsmith.bjj.nav.Editor
import dev.justinsmith.bjj.nav.LearnSection
import dev.justinsmith.bjj.nav.LearnTab
import dev.justinsmith.bjj.nav.PositionDetail
import dev.justinsmith.bjj.nav.TransitionDetail
import dev.justinsmith.bjj.ui.common.DetailHeader
import dev.justinsmith.bjj.ui.common.ItemPicture
import dev.justinsmith.bjj.ui.common.ItemRef
import dev.justinsmith.bjj.ui.common.LinkedVideos
import dev.justinsmith.bjj.ui.common.Missing
import dev.justinsmith.bjj.ui.common.Nav
import dev.justinsmith.bjj.ui.common.NotesBlock
import dev.justinsmith.bjj.ui.common.Page
import dev.justinsmith.bjj.ui.common.Pager
import dev.justinsmith.bjj.ui.common.PagerTarget
import dev.justinsmith.bjj.ui.common.TechniqueLinks
import dev.justinsmith.bjj.ui.common.TransitionArtwork
import dev.justinsmith.bjj.ui.common.VideoActions
import dev.justinsmith.bjj.ui.common.WithSnapshot
import dev.justinsmith.bjj.ui.common.label
import dev.justinsmith.bjj.ui.common.progressOf
import dev.justinsmith.bjj.ui.common.videoActions

@Composable
fun TransitionDetailRoute(id: String, nav: Nav) {
    WithSnapshot { snapshot, viewModel ->
        val transition = snapshot.library.transitionById[id]
        if (transition == null) {
            Missing("Technique", "Positions", { nav.selectTab(LearnTab(LearnSection.Positions)) })
        } else {
            TransitionDetailScreen(
                transition = transition,
                lib = snapshot.library,
                videos = linkedVideos(snapshot.videos, ItemKind.Transition, id),
                nav = nav,
                videoActions = viewModel.videoActions(),
            )
        }
    }
}

@Composable
fun TransitionDetailScreen(
    transition: Transition,
    lib: Library,
    videos: List<VideoBookmark>,
    nav: Nav,
    videoActions: VideoActions,
    modifier: Modifier = Modifier,
) {
    val from = lib.positionById[transition.fromId]
    // Paging stays among the techniques that start where this one does, the way the position page lists them.
    val siblings = from?.let { connections(lib, it.id).outgoing }.orEmpty()
    val index = siblings.indexOfFirst { it.id == transition.id }
    val state = reviewOf(lib, CardKind.Transition, transition.id)
    Page(modifier, maxWidth = Sizes.readingMax) {
        DetailHeader(
            backLabel = from?.name ?: "Positions",
            onBack = nav.back,
            onEdit = { nav.open(Editor(ItemKind.Transition, transition.id)) },
            title = transition.name,
            eyebrow = {
                Tag(transition.type.label, BjjTheme.palette.typeColor(transition.type))
                if (isOnBlueBeltTest(transition)) {
                    Tag(
                        "Blue belt test",
                        BjjTheme.palette.beltBlue,
                        Modifier.clickable { nav.selectTab(LearnTab(LearnSection.BlueBelt)) },
                        onColor = BjjTheme.palette.onBelt,
                    )
                }
                StatusPill(progressOf(state).label, progressOf(state))
            },
        ) { Ends(transition, lib, nav) }
        Body(transition, lib, videos, nav, videoActions)
        Pager(
            previous = siblings.getOrNull(index - 1)?.let {
                PagerTarget(it.name) { nav.replace(TransitionDetail(it.id)) }
            },
            next = siblings.getOrNull(index + 1)?.let { PagerTarget(it.name) { nav.replace(TransitionDetail(it.id)) } },
        )
    }
}

/** Where a technique starts and ends, as two chips with an arrow between, then who does it and where they end up. */
@Composable
private fun Ends(transition: Transition, lib: Library, nav: Nav) {
    val from = lib.positionById[transition.fromId]
    val to = lib.positionById[transition.toId]
    val endSide = endRole(lib, transition)
    val muted = BjjTheme.palette.muted
    Wrap {
        if (from != null) Chip(from.name, { nav.open(PositionDetail(from.id)) }) else BjjText("?", color = muted)
        Icon(BjjIcons.Arrow, description = "leads to", tint = muted)
        when {
            endsTheRoll(transition.type) -> BjjText("Tap", style = Type.strong)
            to != null -> Chip(to.name, { nav.open(PositionDetail(to.id)) })
            else -> BjjText("?", color = muted)
        }
    }
    if (from != null) {
        val who = "Who does it: ${roleLabel(from.category, performerRole(lib, transition))}"
        val ends = if (to != null && endSide != null) " · You end up: ${endSideLabel(to.category, endSide)}" else ""
        BjjText(who + ends, style = Type.small, color = muted)
    }
}

@Composable
private fun Body(
    transition: Transition,
    lib: Library,
    videos: List<VideoBookmark>,
    nav: Nav,
    videoActions: VideoActions,
) {
    val drill = techniqueDrill(transition)
    Card {
        ItemPicture(videos) { TransitionArtwork(transition, lib.positionById) }
        Section("Steps") { PointList(transition.steps, numbered = true) }
        if (transition.commonMistakes.isNotEmpty()) {
            Divider()
            Section("Common mistakes") { PointList(transition.commonMistakes) }
        }
        if (drill.isNotEmpty()) {
            Divider()
            Section("Try this drill") { BjjText(drill) }
        }
        if (endsTheRoll(transition.type)) {
            SoftBlock("Apply submissions slowly in training and let go as soon as your partner taps.")
        }
        TechniqueLinks(lib, transition.id, { nav.open(TransitionDetail(it)) })
        if (transition.notes.isNotBlank()) Divider()
        NotesBlock(transition.notes)
        Divider()
        LinkedVideos(ItemRef(ItemKind.Transition, transition.id), videos, videoActions)
    }
}

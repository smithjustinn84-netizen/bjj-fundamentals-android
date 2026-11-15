package dev.justinsmith.bjj.ui.learn

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.justinsmith.bjj.core.BeltRank
import dev.justinsmith.bjj.core.BeltTestList
import dev.justinsmith.bjj.core.CardKind
import dev.justinsmith.bjj.core.Deck
import dev.justinsmith.bjj.core.ItemKind
import dev.justinsmith.bjj.core.Library
import dev.justinsmith.bjj.core.blueBeltTestList
import dev.justinsmith.bjj.core.groupLabel
import dev.justinsmith.bjj.core.reviewOf
import dev.justinsmith.bjj.design.components.BjjText
import dev.justinsmith.bjj.design.components.Button
import dev.justinsmith.bjj.design.components.ButtonKind
import dev.justinsmith.bjj.design.components.ButtonSize
import dev.justinsmith.bjj.design.components.Card
import dev.justinsmith.bjj.design.components.Divider
import dev.justinsmith.bjj.design.components.GroupTitle
import dev.justinsmith.bjj.design.components.PageTitle
import dev.justinsmith.bjj.design.components.ProgressBar
import dev.justinsmith.bjj.design.components.ProgressDot
import dev.justinsmith.bjj.design.components.RowLink
import dev.justinsmith.bjj.design.graphics.BeltGraphic
import dev.justinsmith.bjj.design.icons.BjjIcons
import dev.justinsmith.bjj.design.theme.BjjTheme
import dev.justinsmith.bjj.design.theme.Spacing
import dev.justinsmith.bjj.design.theme.Type
import dev.justinsmith.bjj.nav.Editor
import dev.justinsmith.bjj.nav.LearnSection
import dev.justinsmith.bjj.nav.LearnTab
import dev.justinsmith.bjj.nav.ReviewTab
import dev.justinsmith.bjj.nav.TransitionDetail
import dev.justinsmith.bjj.ui.common.EmptyState
import dev.justinsmith.bjj.ui.common.LearnSwitch
import dev.justinsmith.bjj.ui.common.Nav
import dev.justinsmith.bjj.ui.common.Page
import dev.justinsmith.bjj.ui.common.ProgressLegend
import dev.justinsmith.bjj.ui.common.RowCard
import dev.justinsmith.bjj.ui.common.StarterOffer
import dev.justinsmith.bjj.ui.common.WithSnapshot
import dev.justinsmith.bjj.ui.common.progressOf

@Composable
fun BlueBeltRoute(nav: Nav) {
    WithSnapshot { snapshot, viewModel ->
        BlueBeltScreen(
            lib = snapshot.library,
            missing = snapshot.missingStarterTechniques.map { it.name },
            nav = nav,
            onAddStarter = viewModel::addMissingStarterTechniques,
            onDismissStarter = viewModel::dismissMissingStarterTechniques,
        )
    }
}

/** The techniques a blue belt test asks for, by type, with how many are learned. */
@Composable
fun BlueBeltScreen(
    lib: Library,
    missing: List<String>,
    nav: Nav,
    onAddStarter: () -> Unit,
    onDismissStarter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val list = blueBeltTestList(lib)
    Page(modifier) {
        LearnSwitch(LearnSection.BlueBelt, { nav.selectTab(LearnTab(it)) })
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s3), verticalAlignment = Alignment.CenterVertically) {
            PageTitle("Blue belt test", Modifier.weight(1f))
            Button("+ Technique", { nav.open(Editor(ItemKind.Transition)) }, size = ButtonSize.Small)
        }
        BjjText(
            "The techniques you will be asked to show. Every gym tests differently, so edit a technique to add it " +
                "to this list or take it off.",
            color = BjjTheme.palette.muted,
        )
        StarterOffer("technique", missing, onAddStarter, onDismissStarter)
        if (list.total == 0) {
            Card {
                EmptyState(
                    icon = BjjIcons.Belt,
                    title = "Nothing on the list yet",
                    text = "Open a technique, choose Edit and tick “On the blue belt test” to add it here.",
                ) {
                    Button("Browse techniques", {
                        nav.selectTab(LearnTab(LearnSection.Positions))
                    }, kind = ButtonKind.Primary)
                }
            }
        } else {
            Summary(list, onStudy = { nav.selectTab(ReviewTab(Deck.BlueBelt.key)) })
            ProgressLegend()
            Groups(lib, list, nav)
        }
    }
}

@Composable
private fun Summary(list: BeltTestList, onStudy: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s4), verticalAlignment = Alignment.CenterVertically) {
            BeltGraphic(BeltRank.Blue, stripes = 0, Modifier.width(BELT_WIDTH))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.s1)) {
                BjjText("${list.learned} of ${list.total} learned", style = Type.strong)
                ProgressBar(list.learned.toFloat() / list.total)
            }
        }
        Button("Study techniques", onStudy, Modifier.fillMaxWidth(), kind = ButtonKind.Primary)
    }
}

private val BELT_WIDTH = 96.dp

@Composable
private fun Groups(lib: Library, list: BeltTestList, nav: Nav, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
        for (group in list.groups) {
            GroupTitle("${group.type.groupLabel} · ${group.techniques.size}")
            RowCard {
                group.techniques.forEachIndexed { index, technique ->
                    if (index > 0) Divider()
                    val from = lib.positionById[technique.fromId]
                    RowLink(
                        technique.name,
                        { nav.open(TransitionDetail(technique.id)) },
                        BjjIcons.Chevron,
                        leading = { ProgressDot(progressOf(reviewOf(lib, CardKind.Transition, technique.id))) },
                    ) {
                        if (from !=
                            null
                        ) {
                            BjjText("from ${from.name}", style = Type.small, color = BjjTheme.palette.muted)
                        }
                    }
                }
            }
        }
    }
}

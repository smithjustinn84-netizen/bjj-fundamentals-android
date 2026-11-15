package dev.justinsmith.bjj.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.justinsmith.bjj.core.plural
import dev.justinsmith.bjj.design.components.BjjText
import dev.justinsmith.bjj.design.components.Icon
import dev.justinsmith.bjj.design.components.block
import dev.justinsmith.bjj.design.components.pressShrink
import dev.justinsmith.bjj.design.icons.BjjIcons
import dev.justinsmith.bjj.design.theme.BjjTheme
import dev.justinsmith.bjj.design.theme.Radius
import dev.justinsmith.bjj.design.theme.Sizes
import dev.justinsmith.bjj.design.theme.Spacing
import dev.justinsmith.bjj.design.theme.Type
import dev.justinsmith.bjj.nav.LearnTab
import dev.justinsmith.bjj.nav.LogTab
import dev.justinsmith.bjj.nav.MoreTab
import dev.justinsmith.bjj.nav.ReviewTab
import dev.justinsmith.bjj.nav.TABS
import dev.justinsmith.bjj.nav.Tab
import dev.justinsmith.bjj.nav.TodayTab
import dev.justinsmith.bjj.nav.sameDestination

private val Tab.label: String
    get() = when (this) {
        TodayTab -> "Today"
        is LearnTab -> "Learn"
        is ReviewTab -> "Review"
        LogTab -> "Log"
        MoreTab -> "More"
    }

private val Tab.icon: ImageVector
    get() = when (this) {
        TodayTab -> BjjIcons.Home
        is LearnTab -> BjjIcons.Concepts
        is ReviewTab -> BjjIcons.Review
        LogTab -> BjjIcons.Log
        MoreTab -> BjjIcons.More
    }

/** The five tabs along the bottom of a phone, with the ink line over them. */
@Composable
fun TabBar(current: Tab, dueCount: Int, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().background(BjjTheme.palette.surface)) {
        Box(Modifier.fillMaxWidth().height(Sizes.line).background(BjjTheme.palette.outline))
        Row(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                .height(Sizes.tabBar)
                .selectableGroup(),
        ) {
            for (tab in TABS) {
                TabItem(tab, tab.sameDestination(current), dueCount, onSelect, Modifier.weight(1f).fillMaxHeight())
            }
        }
    }
}

/** The five tabs down the left of a wider window, with the ink line beside them. */
@Composable
fun TabRail(current: Tab, dueCount: Int, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxHeight().background(BjjTheme.palette.surface)) {
        Column(
            Modifier
                .width(Sizes.rail)
                .fillMaxHeight()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                .padding(vertical = Spacing.s4)
                .selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(Spacing.s2),
        ) {
            for (tab in TABS) {
                TabItem(
                    tab,
                    tab.sameDestination(current),
                    dueCount,
                    onSelect,
                    Modifier.fillMaxWidth().height(Sizes.tabBar),
                )
            }
        }
        Box(Modifier.width(Sizes.line).fillMaxHeight().background(BjjTheme.palette.outline))
    }
}

@Composable
private fun TabItem(
    tab: Tab,
    selected: Boolean,
    dueCount: Int,
    onSelect: (Tab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = BjjTheme.palette
    val interactions = remember { MutableInteractionSource() }
    val due = if (tab is ReviewTab) dueCount else 0
    val color = if (selected) p.text else p.muted
    Box(
        modifier
            .pressShrink(interactions)
            .selectable(selected, interactions, indication = null, role = Role.Tab, onClick = { onSelect(tab) })
            .semantics { if (due > 0) contentDescription = "${tab.label}, ${plural(due, "card")} ready" },
        contentAlignment = Alignment.Center,
    ) {
        // The belt strip that hangs from the bar's edge over the tab you are on.
        if (selected) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .size(STRIP_WIDTH, STRIP_HEIGHT)
                    .clip(RoundedCornerShape(bottomStart = Radius.sm, bottomEnd = Radius.sm))
                    .background(p.belt),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Box {
                Icon(tab.icon, description = null, tint = color, size = TAB_ICON)
                if (due > 0) DueBadge(due, Modifier.align(Alignment.TopEnd).offset(x = BADGE_OUT, y = -BADGE_UP))
            }
            BjjText(
                tab.label,
                style = Type.eyebrow.copy(
                    letterSpacing = Type.small.letterSpacing,
                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                ),
                color = color,
                maxLines = 1,
            )
        }
    }
}

private val STRIP_WIDTH = 40.dp
private val STRIP_HEIGHT = 5.dp
private val TAB_ICON = 24.dp
private val BADGE_OUT = 12.dp
private val BADGE_UP = 4.dp

/** How many cards are ready, in belt red on the Review tab. The tab's own description says it aloud. */
@Composable
private fun DueBadge(count: Int, modifier: Modifier = Modifier) {
    val p = BjjTheme.palette
    Box(
        modifier
            .defaultMinSize(minWidth = BADGE_SIZE, minHeight = BADGE_SIZE)
            .drawBehind { block(p.belt, p.belt, Radius.pill, 0.dp) }
            .padding(horizontal = 5.dp)
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        BjjText(if (count > MAX_BADGE) "$MAX_BADGE+" else "$count", style = Type.tag, color = p.onBelt, maxLines = 1)
    }
}

private val BADGE_SIZE = 18.dp
private const val MAX_BADGE = 99

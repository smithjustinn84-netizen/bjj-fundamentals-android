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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.justinsmith.bjj.design.components.BjjText
import dev.justinsmith.bjj.design.components.Icon
import dev.justinsmith.bjj.design.components.block
import dev.justinsmith.bjj.design.components.pressShrink
import dev.justinsmith.bjj.design.graphics.BrandMark
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
import dev.justinsmith.bjj.resources.Res
import dev.justinsmith.bjj.resources.app_name
import dev.justinsmith.bjj.resources.tab_cards_ready
import dev.justinsmith.bjj.resources.tab_learn
import dev.justinsmith.bjj.resources.tab_log
import dev.justinsmith.bjj.resources.tab_more
import dev.justinsmith.bjj.resources.tab_review
import dev.justinsmith.bjj.resources.tab_today
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

private val Tab.label: StringResource
    get() = when (this) {
        TodayTab -> Res.string.tab_today
        is LearnTab -> Res.string.tab_learn
        is ReviewTab -> Res.string.tab_review
        LogTab -> Res.string.tab_log
        MoreTab -> Res.string.tab_more
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

/**
 * The five tabs down the left of a wider window, with the ink line beside them. Where there is room ([wide]) the
 * brand sits on top and each tab's label beside its icon.
 */
@Composable
fun TabRail(current: Tab, dueCount: Int, wide: Boolean, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxHeight().background(BjjTheme.palette.surface)) {
        Column(
            Modifier
                .width(if (wide) Sizes.railWide else Sizes.rail)
                .fillMaxHeight()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                .padding(vertical = Spacing.s4)
                .selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(Spacing.s2),
        ) {
            if (wide) {
                Row(
                    Modifier.padding(horizontal = Spacing.s4).padding(bottom = Spacing.s3),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BrandMark()
                    BjjText(stringResource(Res.string.app_name), style = Type.strong, maxLines = 1)
                }
            }
            for (tab in TABS) {
                val selected = tab.sameDestination(current)
                if (wide) {
                    WideTabItem(tab, selected, dueCount, onSelect)
                } else {
                    TabItem(tab, selected, dueCount, onSelect, Modifier.fillMaxWidth().height(Sizes.tabBar))
                }
            }
        }
        Box(Modifier.width(Sizes.line).fillMaxHeight().background(BjjTheme.palette.outline))
    }
}

/** A tab on the wide rail: its icon, its label beside it, and the count of cards ready at the far end. */
@Composable
private fun WideTabItem(
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
    val label = stringResource(tab.label)
    val ready = pluralStringResource(Res.plurals.tab_cards_ready, due, label, due)
    Row(
        modifier
            .fillMaxWidth()
            .height(Sizes.tap)
            .pressShrink(interactions)
            .selectable(selected, interactions, indication = null, role = Role.Tab, onClick = { onSelect(tab) })
            .semantics { if (due > 0) contentDescription = ready },
        horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The belt strip hangs from the rail's edge beside the tab you are on.
        Box(
            Modifier
                .size(STRIP_HEIGHT, STRIP_LENGTH)
                .clip(RoundedCornerShape(topEnd = Radius.sm, bottomEnd = Radius.sm))
                .background(if (selected) p.belt else Color.Transparent),
        )
        Icon(tab.icon, description = null, tint = color, size = TAB_ICON)
        BjjText(
            label,
            Modifier.weight(1f),
            style = Type.strong.copy(fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold),
            color = color,
            maxLines = 1,
        )
        if (due > 0) DueBadge(due, Modifier.padding(end = Spacing.s4))
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
    val label = stringResource(tab.label)
    val ready = pluralStringResource(Res.plurals.tab_cards_ready, due, label, due)
    Box(
        modifier
            .pressShrink(interactions)
            .selectable(selected, interactions, indication = null, role = Role.Tab, onClick = { onSelect(tab) })
            .semantics { if (due > 0) contentDescription = ready },
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
                label,
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
private val STRIP_LENGTH = 28.dp
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

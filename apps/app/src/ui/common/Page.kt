package dev.justinsmith.bjj.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.navigation3.runtime.NavKey
import dev.justinsmith.bjj.core.ReviewState
import dev.justinsmith.bjj.core.isLearned
import dev.justinsmith.bjj.design.components.BjjText
import dev.justinsmith.bjj.design.components.Button
import dev.justinsmith.bjj.design.components.ButtonSize
import dev.justinsmith.bjj.design.components.Card
import dev.justinsmith.bjj.design.components.Icon
import dev.justinsmith.bjj.design.components.PageTitle
import dev.justinsmith.bjj.design.components.PagerLink
import dev.justinsmith.bjj.design.components.Progress
import dev.justinsmith.bjj.design.components.ProgressDot
import dev.justinsmith.bjj.design.components.Section
import dev.justinsmith.bjj.design.components.SoftBlock
import dev.justinsmith.bjj.design.components.Wrap
import dev.justinsmith.bjj.design.components.pressShrink
import dev.justinsmith.bjj.design.icons.BjjIcons
import dev.justinsmith.bjj.design.theme.BjjTheme
import dev.justinsmith.bjj.design.theme.Sizes
import dev.justinsmith.bjj.design.theme.Spacing
import dev.justinsmith.bjj.design.theme.Type
import dev.justinsmith.bjj.nav.Tab
import dev.justinsmith.bjj.resources.Res
import dev.justinsmith.bjj.resources.common_edit
import dev.justinsmith.bjj.resources.page_loading
import dev.justinsmith.bjj.resources.page_my_notes
import dev.justinsmith.bjj.resources.page_next
import dev.justinsmith.bjj.resources.page_not_found
import dev.justinsmith.bjj.resources.page_not_found_about
import dev.justinsmith.bjj.resources.page_previous
import dev.justinsmith.bjj.resources.page_progress_learned
import dev.justinsmith.bjj.resources.page_progress_learning
import dev.justinsmith.bjj.resources.page_progress_new
import org.jetbrains.compose.resources.stringResource

/** How a page moves to another: what the app's one back stack offers a screen. */
class Nav(
    /** Opens a page over this one. */
    val open: (NavKey) -> Unit,
    /** Leaves this page for the one under it. */
    val back: () -> Unit,
    /** Shows one of the bar's five destinations, or another page of it. */
    val selectTab: (Tab) -> Unit,
    /** Puts a page where this one is, so Back skips this one. */
    val replace: (NavKey) -> Unit,
)

/**
 * A page: its content down the middle of the window, scrolling, with the page's gutters. Pages of prose and
 * forms pass [maxWidth] `Sizes.readingMax`.
 */
@Composable
fun Page(modifier: Modifier = Modifier, maxWidth: Dp = Sizes.contentMax, content: @Composable ColumnScope.() -> Unit) {
    Box(modifier.fillMaxSize().imePadding(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .widthIn(max = maxWidth)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.pageGutter)
                .padding(top = Spacing.s4, bottom = Spacing.s6),
            verticalArrangement = Arrangement.spacedBy(Spacing.s4),
            content = content,
        )
    }
}

/** A page's title with a quieter line about the page under it. */
@Composable
fun PageHead(title: String, modifier: Modifier = Modifier, about: String = "") {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.s1)) {
        PageTitle(title)
        if (about.isNotEmpty()) BjjText(about, color = BjjTheme.palette.muted)
    }
}

/** While the first read of the library is on its way. */
@Composable
fun Loading(modifier: Modifier = Modifier) {
    Page(modifier) { BjjText(stringResource(Res.string.page_loading), color = BjjTheme.palette.muted) }
}

/** In place of a page whose item is gone. */
@Composable
fun Missing(what: String, backLabel: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Page(modifier, maxWidth = Sizes.readingMax) {
        Card {
            PageTitle(stringResource(Res.string.page_not_found, what))
            BjjText(stringResource(Res.string.page_not_found_about), color = BjjTheme.palette.muted)
            BackLink(backLabel, onBack)
        }
    }
}

/** The way back at the top of a page, named for where it goes. */
@Composable
fun BackLink(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interactions = remember { MutableInteractionSource() }
    Row(
        modifier
            .pressShrink(interactions)
            .clickable(interactions, indication = null, role = Role.Button, onClick = onClick)
            .defaultMinSize(minHeight = Sizes.tap),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(BjjIcons.Back, description = null, tint = BjjTheme.palette.accent)
        BjjText(label, style = Type.strong, color = BjjTheme.palette.accent)
    }
}

/**
 * The top of a page about one item: the way back and Edit on one row, then what the item is. [eyebrow] is the
 * small line above the name; [content] is anything that belongs under the summary.
 */
@Composable
fun DetailHeader(
    backLabel: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    title: String,
    eyebrow: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    lead: String = "",
    content: @Composable ColumnScope.() -> Unit = {},
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            BackLink(backLabel, onBack, Modifier.weight(1f))
            Button(stringResource(Res.string.common_edit), onEdit, size = ButtonSize.Small)
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
            verticalAlignment = Alignment.CenterVertically,
            content = eyebrow,
        )
        PageTitle(title)
        if (lead.isNotEmpty()) BjjText(lead, style = Type.lead)
        content()
    }
}

fun progressOf(state: ReviewState?): Progress = when {
    state == null -> Progress.New
    isLearned(state) -> Progress.Learned
    else -> Progress.Learning
}

/** How far a card has got, in words. */
val Progress.label: String
    @Composable get() = stringResource(
        when (this) {
            Progress.New -> Res.string.page_progress_new
            Progress.Learning -> Res.string.page_progress_learning
            Progress.Learned -> Res.string.page_progress_learned
        },
    )

/** What the three progress dots mean, shown once above a list that uses them. */
@Composable
fun ProgressLegend(modifier: Modifier = Modifier) {
    Wrap(modifier) {
        for (progress in Progress.entries) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ProgressDot(progress)
                BjjText(progress.label, style = Type.small, color = BjjTheme.palette.muted)
            }
        }
    }
}

/** The user's own notes on an item, when there are any. */
@Composable
fun NotesBlock(notes: String, modifier: Modifier = Modifier) {
    if (notes.isBlank()) return
    Section(stringResource(Res.string.page_my_notes), modifier) { SoftBlock(notes) }
}

/** A name and where it leads, for the two links at the foot of a page. */
class PagerTarget(val label: String, val onClick: () -> Unit)

/** Previous and next at the foot of a page about one item. Either may be absent. */
@Composable
fun Pager(previous: PagerTarget?, next: PagerTarget?, modifier: Modifier = Modifier) {
    if (previous == null && next == null) return
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
        if (previous != null) {
            val label = stringResource(Res.string.page_previous)
            PagerLink(label, previous.label, previous.onClick, Modifier.fillMaxWidth())
        }
        if (next != null) {
            val label = stringResource(Res.string.page_next)
            PagerLink(label, next.label, next.onClick, Modifier.fillMaxWidth(), alignEnd = true)
        }
    }
}

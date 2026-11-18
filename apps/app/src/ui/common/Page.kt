package dev.justinsmith.bjj.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import dev.justinsmith.bjj.core.ReviewState
import dev.justinsmith.bjj.core.isLearned
import dev.justinsmith.bjj.design.components.BjjText
import dev.justinsmith.bjj.design.components.Button
import dev.justinsmith.bjj.design.components.Card
import dev.justinsmith.bjj.design.components.Icon
import dev.justinsmith.bjj.design.components.PageTitle
import dev.justinsmith.bjj.design.components.Progress
import dev.justinsmith.bjj.design.components.pressShrink
import dev.justinsmith.bjj.design.icons.BjjIcons
import dev.justinsmith.bjj.design.theme.BjjTheme
import dev.justinsmith.bjj.design.theme.Sizes
import dev.justinsmith.bjj.design.theme.Spacing
import dev.justinsmith.bjj.design.theme.Type
import dev.justinsmith.bjj.resources.Res
import dev.justinsmith.bjj.resources.page_loading
import dev.justinsmith.bjj.resources.page_not_found
import dev.justinsmith.bjj.resources.page_not_found_about
import dev.justinsmith.bjj.resources.page_progress_learned
import dev.justinsmith.bjj.resources.page_progress_learning
import dev.justinsmith.bjj.resources.page_progress_new
import org.jetbrains.compose.resources.stringResource

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

/**
 * The same page for a long list, laid out only as far as it is scrolled: [content] adds the list's items, each
 * spaced as a [Page]'s content is.
 */
@Composable
fun LazyPage(modifier: Modifier = Modifier, maxWidth: Dp = Sizes.contentMax, content: LazyListScope.() -> Unit) {
    Box(modifier.fillMaxSize().imePadding(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            Modifier.widthIn(max = maxWidth).fillMaxWidth(),
            contentPadding = PaddingValues(
                start = Spacing.pageGutter,
                end = Spacing.pageGutter,
                top = Spacing.s4,
                bottom = Spacing.s6,
            ),
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

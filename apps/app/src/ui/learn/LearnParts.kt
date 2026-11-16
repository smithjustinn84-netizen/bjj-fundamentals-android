package dev.justinsmith.bjj.ui.learn

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.justinsmith.bjj.core.VideoBookmark
import dev.justinsmith.bjj.core.label
import dev.justinsmith.bjj.design.components.BjjText
import dev.justinsmith.bjj.design.components.Button
import dev.justinsmith.bjj.design.components.ButtonKind
import dev.justinsmith.bjj.design.components.ButtonSize
import dev.justinsmith.bjj.design.components.Card
import dev.justinsmith.bjj.design.components.PageTitle
import dev.justinsmith.bjj.design.components.PagerLink
import dev.justinsmith.bjj.design.components.Progress
import dev.justinsmith.bjj.design.components.ProgressDot
import dev.justinsmith.bjj.design.components.Section
import dev.justinsmith.bjj.design.components.SectionHead
import dev.justinsmith.bjj.design.components.Segmented
import dev.justinsmith.bjj.design.components.SoftBlock
import dev.justinsmith.bjj.design.components.Wrap
import dev.justinsmith.bjj.design.theme.BjjTheme
import dev.justinsmith.bjj.design.theme.Spacing
import dev.justinsmith.bjj.design.theme.Type
import dev.justinsmith.bjj.nav.LearnSection
import dev.justinsmith.bjj.resources.Res
import dev.justinsmith.bjj.resources.common_concepts
import dev.justinsmith.bjj.resources.common_edit
import dev.justinsmith.bjj.resources.common_lessons
import dev.justinsmith.bjj.resources.common_positions
import dev.justinsmith.bjj.resources.learn_starter_add
import dev.justinsmith.bjj.resources.learn_starter_dismiss
import dev.justinsmith.bjj.resources.learn_switch_blue_belt
import dev.justinsmith.bjj.resources.page_my_notes
import dev.justinsmith.bjj.resources.page_next
import dev.justinsmith.bjj.resources.page_previous
import dev.justinsmith.bjj.resources.videos_add
import dev.justinsmith.bjj.resources.videos_bookmarks
import dev.justinsmith.bjj.resources.videos_linked_empty
import dev.justinsmith.bjj.ui.common.BackLink
import dev.justinsmith.bjj.ui.common.ItemRef
import dev.justinsmith.bjj.ui.common.VideoActions
import dev.justinsmith.bjj.ui.common.VideoForm
import dev.justinsmith.bjj.ui.common.VideoRow
import dev.justinsmith.bjj.ui.common.label
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

// The parts the pages behind the Learn tab share with each other and with no other screen.

private val LearnSection.label: StringResource
    get() = when (this) {
        LearnSection.Concepts -> Res.string.common_concepts
        LearnSection.Positions -> Res.string.common_positions
        LearnSection.Lessons -> Res.string.common_lessons
        LearnSection.BlueBelt -> Res.string.learn_switch_blue_belt
    }

/** The switch at the top of every page behind the Learn tab. Techniques are reached from positions. */
@Composable
fun LearnSwitch(section: LearnSection, onSelect: (LearnSection) -> Unit, modifier: Modifier = Modifier) {
    Segmented(LearnSection.entries, section, onSelect, { stringResource(it.label) }, modifier.fillMaxWidth())
}

/**
 * Offers starter items the library does not have, such as ones added to the app after it was set up. [missing]
 * is the heading that counts them, which names their kind. Dismissing hides those items for good; one that goes
 * missing later is offered again.
 */
@Composable
fun StarterOffer(
    missing: PluralStringResource,
    names: List<String>,
    onAdd: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (names.isEmpty()) return
    Card(modifier) {
        BjjText(pluralStringResource(missing, names.size, names.size), style = Type.strong)
        BjjText("${names.joinToString(", ")}.")
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            Button(pluralStringResource(Res.plurals.learn_starter_add, names.size), onAdd, kind = ButtonKind.Primary)
            Button(stringResource(Res.string.learn_starter_dismiss), onDismiss)
        }
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

/** The bookmarks attached to one item, with a form to add another. */
@Composable
fun LinkedVideos(item: ItemRef, videos: List<VideoBookmark>, actions: VideoActions, modifier: Modifier = Modifier) {
    var adding by rememberSaveable { mutableStateOf(false) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
        SectionHead(stringResource(Res.string.videos_bookmarks)) {
            if (!adding) Button(stringResource(Res.string.videos_add), { adding = true }, size = ButtonSize.Small)
        }
        if (adding) VideoForm(actions.add, onDone = { adding = false }, linkedTo = item)
        if (videos.isEmpty() && !adding) {
            BjjText(stringResource(Res.string.videos_linked_empty), color = BjjTheme.palette.muted)
        }
        for (video in videos) VideoRow(video, actions)
    }
}

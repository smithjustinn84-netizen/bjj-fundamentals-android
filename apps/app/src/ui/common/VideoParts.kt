package dev.justinsmith.bjj.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.justinsmith.bjj.core.ItemKind
import dev.justinsmith.bjj.core.Library
import dev.justinsmith.bjj.core.LinkedKind
import dev.justinsmith.bjj.core.VideoBookmark
import dev.justinsmith.bjj.core.formatTime
import dev.justinsmith.bjj.core.groupLabel
import dev.justinsmith.bjj.core.itemName
import dev.justinsmith.bjj.core.label
import dev.justinsmith.bjj.core.linkedKind
import dev.justinsmith.bjj.core.parseTime
import dev.justinsmith.bjj.core.parseYouTubeId
import dev.justinsmith.bjj.core.parseYouTubeStart
import dev.justinsmith.bjj.core.services.VideoInput
import dev.justinsmith.bjj.core.watchUrl
import dev.justinsmith.bjj.design.components.BjjText
import dev.justinsmith.bjj.design.components.Button
import dev.justinsmith.bjj.design.components.ButtonKind
import dev.justinsmith.bjj.design.components.ButtonSize
import dev.justinsmith.bjj.design.components.ChoiceField
import dev.justinsmith.bjj.design.components.ConfirmDialog
import dev.justinsmith.bjj.design.components.LinkButton
import dev.justinsmith.bjj.design.components.Notice
import dev.justinsmith.bjj.design.components.SectionHead
import dev.justinsmith.bjj.design.components.TextField
import dev.justinsmith.bjj.design.components.block
import dev.justinsmith.bjj.design.components.pressShrink
import dev.justinsmith.bjj.design.theme.BjjTheme
import dev.justinsmith.bjj.design.theme.Radius
import dev.justinsmith.bjj.design.theme.Sizes
import dev.justinsmith.bjj.design.theme.Spacing
import dev.justinsmith.bjj.design.theme.Type

/** What a page needs to list, open, add and delete video bookmarks. */
class VideoActions(val open: (String) -> Unit, val add: (VideoInput) -> Unit, val remove: (String) -> Unit)

/** An item a bookmark can be about. */
data class ItemRef(val kind: ItemKind, val id: String)

/**
 * One bookmark: a link that opens the video outside the app, with Delete beside it. [linked] names the item it
 * is about and where tapping that goes, on the page that lists every bookmark.
 */
@Composable
fun VideoRow(
    video: VideoBookmark,
    actions: VideoActions,
    modifier: Modifier = Modifier,
    linked: Pair<String, () -> Unit>? = null,
) {
    var confirming by remember { mutableStateOf(false) }
    val interactions = remember { MutableInteractionSource() }
    val p = BjjTheme.palette
    Column(
        modifier
            .fillMaxWidth()
            .drawBehind { block(Color.Transparent, p.border, Radius.md, line = Sizes.hairline) }
            .padding(start = Spacing.s4, end = Spacing.s2, top = Spacing.s2, bottom = Spacing.s2),
        verticalArrangement = Arrangement.spacedBy(Spacing.s1),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s2), verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier
                    .weight(1f)
                    .pressShrink(interactions)
                    .clickable(interactions, indication = null, role = Role.Button) {
                        actions.open(watchUrl(video.youtubeId, video.startSeconds))
                    }
                    .defaultMinSize(minHeight = Sizes.tap)
                    .semantics { contentDescription = "Play ${video.title} on YouTube" },
                horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BjjText("▶", color = p.belt)
                BjjText(video.title, Modifier.weight(1f, fill = false), style = Type.strong)
                BjjText("· ${formatTime(video.startSeconds)}", color = p.muted)
            }
            Button("Delete", { confirming = true }, kind = ButtonKind.Danger, size = ButtonSize.Small)
        }
        if (video.note.isNotEmpty()) BjjText(video.note, style = Type.small, color = p.muted)
        val kind = video.linkedKind.itemKind
        if (linked != null && kind != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BjjText("${kind.label}:", style = Type.small)
                LinkButton(linked.first, linked.second)
            }
        }
    }
    if (confirming) {
        ConfirmDialog(
            title = "Delete bookmark?",
            text = "Delete the bookmark \"${video.title}\"?",
            confirm = "Delete",
            cancel = "Cancel",
            onConfirm = {
                confirming = false
                actions.remove(video.id)
            },
            onDismiss = { confirming = false },
        )
    }
}

/**
 * The form for a new bookmark. With [linkedTo] it is attached to that item and the "About" picker is hidden;
 * otherwise [lib] fills the picker. [onDone] is called after saving or cancelling.
 */
@Composable
fun VideoForm(
    onSave: (VideoInput) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    linkedTo: ItemRef? = null,
    lib: Library? = null,
    showCancel: Boolean = true,
) {
    var url by rememberSaveable { mutableStateOf("") }
    var title by rememberSaveable { mutableStateOf("") }
    var time by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var picked by remember { mutableStateOf<ItemRef?>(null) }
    var error by rememberSaveable { mutableStateOf("") }

    fun save() {
        val youtubeId = parseYouTubeId(url)
        val startSeconds = if (time.isBlank()) 0 else parseTime(time)
        when {
            youtubeId == null -> error = "That does not look like a YouTube link."

            startSeconds == null -> error = "Use a time like 1:30, 90, or 1m30s."

            else -> {
                val link = linkedTo ?: picked
                onSave(
                    VideoInput(
                        title = title,
                        youtubeId = youtubeId,
                        startSeconds = startSeconds,
                        linkedKind = link?.kind?.linkedKind ?: LinkedKind.None,
                        linkedId = link?.id.orEmpty(),
                        note = note.trim(),
                    ),
                )
                url = ""
                title = ""
                time = ""
                note = ""
                picked = null
                error = ""
                onDone()
            }
        }
    }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.s3)) {
        TextField(
            "YouTube link",
            url,
            {
                url = it
                // A link that names a moment fills in the start time, unless one is typed already.
                val start = parseYouTubeStart(it)
                if (start != null && time.isEmpty()) time = formatTime(start)
            },
            hint = "For example https://youtu.be/…?t=95",
            keyboardType = KeyboardType.Uri,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s3)) {
            TextField("Title", title, { title = it }, Modifier.weight(1f))
            TextField("Start at", time, { time = it }, Modifier.width(START_WIDTH), hint = "1:35")
        }
        if (linkedTo == null && lib != null) {
            val options: List<ItemRef?> = listOf(null) + ItemKind.entries.flatMap { kind ->
                when (kind) {
                    ItemKind.Concept -> lib.concepts.map { it.id }
                    ItemKind.Position -> lib.positions.map { it.id }
                    ItemKind.Transition -> lib.transitions.map { it.id }
                }.map { ItemRef(kind, it) }
            }
            ChoiceField(
                label = "About",
                options = options,
                selected = picked,
                optionLabel = { ref ->
                    if (ref ==
                        null
                    ) {
                        "— Not linked —"
                    } else {
                        "${ref.kind.groupLabel}: ${itemName(lib, ref.kind, ref.id).orEmpty()}"
                    }
                },
                onSelect = { picked = it },
                placeholder = "— Not linked —",
            )
        }
        TextField("Note", note, { note = it }, hint = "What to watch for")
        if (error.isNotEmpty()) Notice(error, isError = true)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            Button("Save bookmark", ::save, kind = ButtonKind.Primary)
            if (showCancel) Button("Cancel", onDone)
        }
    }
}

private val START_WIDTH = 112.dp

/** The bookmarks attached to one item, with a form to add another. */
@Composable
fun LinkedVideos(item: ItemRef, videos: List<VideoBookmark>, actions: VideoActions, modifier: Modifier = Modifier) {
    var adding by rememberSaveable { mutableStateOf(false) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
        SectionHead("Video bookmarks") {
            if (!adding) Button("+ Add", { adding = true }, size = ButtonSize.Small)
        }
        if (adding) VideoForm(actions.add, onDone = { adding = false }, linkedTo = item)
        if (videos.isEmpty() && !adding) {
            BjjText("Save a YouTube link at the exact moment that explains this well.", color = BjjTheme.palette.muted)
        }
        for (video in videos) VideoRow(video, actions)
    }
}

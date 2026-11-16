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
import dev.justinsmith.bjj.design.components.TextField
import dev.justinsmith.bjj.design.components.block
import dev.justinsmith.bjj.design.components.pressShrink
import dev.justinsmith.bjj.design.theme.BjjTheme
import dev.justinsmith.bjj.design.theme.Radius
import dev.justinsmith.bjj.design.theme.Sizes
import dev.justinsmith.bjj.design.theme.Spacing
import dev.justinsmith.bjj.design.theme.Type
import dev.justinsmith.bjj.resources.Res
import dev.justinsmith.bjj.resources.common_cancel
import dev.justinsmith.bjj.resources.common_delete
import dev.justinsmith.bjj.resources.videos_delete_text
import dev.justinsmith.bjj.resources.videos_delete_title
import dev.justinsmith.bjj.resources.videos_error_link
import dev.justinsmith.bjj.resources.videos_error_time
import dev.justinsmith.bjj.resources.videos_form_about
import dev.justinsmith.bjj.resources.videos_form_link
import dev.justinsmith.bjj.resources.videos_form_link_hint
import dev.justinsmith.bjj.resources.videos_form_not_linked
import dev.justinsmith.bjj.resources.videos_form_note
import dev.justinsmith.bjj.resources.videos_form_note_hint
import dev.justinsmith.bjj.resources.videos_form_save
import dev.justinsmith.bjj.resources.videos_form_start
import dev.justinsmith.bjj.resources.videos_form_title
import dev.justinsmith.bjj.resources.videos_play
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** What a page needs to list, open, add and delete video bookmarks. */
class VideoActions(val open: (String) -> Unit, val add: (VideoInput) -> Unit, val remove: (String) -> Unit)

/** An item a bookmark can be about. */
data class ItemRef(val kind: ItemKind, val id: String)

/** An item offered in the bookmark form's "About" picker, with its name. */
data class AboutOption(val item: ItemRef, val name: String)

/** Every item a bookmark can be about, by kind and then in the library's own order. */
fun aboutOptions(lib: Library): List<AboutOption> = ItemKind.entries.flatMap { kind ->
    when (kind) {
        ItemKind.Concept -> lib.concepts.map { Named(it.id, it.name) }
        ItemKind.Position -> lib.positions.map { Named(it.id, it.name) }
        ItemKind.Transition -> lib.transitions.map { Named(it.id, it.name) }
    }.map { AboutOption(ItemRef(kind, it.id), it.name) }
}

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
    val play = stringResource(Res.string.videos_play, video.title)
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
                    .semantics { contentDescription = play },
                horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BjjText("▶", color = p.belt)
                BjjText(video.title, Modifier.weight(1f, fill = false), style = Type.strong)
                BjjText("· ${formatTime(video.startSeconds)}", color = p.muted)
            }
            Button(
                stringResource(Res.string.common_delete),
                { confirming = true },
                kind = ButtonKind.Danger,
                size = ButtonSize.Small,
            )
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
            title = stringResource(Res.string.videos_delete_title),
            text = stringResource(Res.string.videos_delete_text, video.title),
            confirm = stringResource(Res.string.common_delete),
            cancel = stringResource(Res.string.common_cancel),
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
 * otherwise [about] fills the picker. [onDone] is called after saving or cancelling.
 */
@Composable
fun VideoForm(
    onSave: (VideoInput) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    linkedTo: ItemRef? = null,
    about: List<AboutOption> = emptyList(),
    showCancel: Boolean = true,
) {
    var url by rememberSaveable { mutableStateOf("") }
    var title by rememberSaveable { mutableStateOf("") }
    var time by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var picked by remember { mutableStateOf<ItemRef?>(null) }
    var error by rememberSaveable { mutableStateOf<VideoFormError?>(null) }

    fun save() {
        val youtubeId = parseYouTubeId(url)
        val startSeconds = if (time.isBlank()) 0 else parseTime(time)
        when {
            youtubeId == null -> error = VideoFormError.Link

            startSeconds == null -> error = VideoFormError.Time

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
                error = null
                onDone()
            }
        }
    }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.s3)) {
        TextField(
            stringResource(Res.string.videos_form_link),
            url,
            {
                url = it
                // A link that names a moment fills in the start time, unless one is typed already.
                val start = parseYouTubeStart(it)
                if (start != null && time.isEmpty()) time = formatTime(start)
            },
            hint = stringResource(Res.string.videos_form_link_hint),
            keyboardType = KeyboardType.Uri,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s3)) {
            TextField(stringResource(Res.string.videos_form_title), title, { title = it }, Modifier.weight(1f))
            TextField(
                stringResource(Res.string.videos_form_start),
                time,
                { time = it },
                Modifier.width(START_WIDTH),
                hint = START_EXAMPLE,
            )
        }
        if (linkedTo == null && about.isNotEmpty()) {
            val names = remember(about) { about.associate { it.item to it.name } }
            ChoiceField(
                label = stringResource(Res.string.videos_form_about),
                options = listOf<ItemRef?>(null) + about.map { it.item },
                selected = picked,
                optionLabel = { ref ->
                    if (ref == null) {
                        stringResource(Res.string.videos_form_not_linked)
                    } else {
                        "${ref.kind.groupLabel}: ${names[ref].orEmpty()}"
                    }
                },
                onSelect = { picked = it },
                placeholder = stringResource(Res.string.videos_form_not_linked),
            )
        }
        TextField(
            stringResource(Res.string.videos_form_note),
            note,
            { note = it },
            hint = stringResource(Res.string.videos_form_note_hint),
        )
        error?.let { Notice(stringResource(it.message), isError = true) }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            Button(stringResource(Res.string.videos_form_save), ::save, kind = ButtonKind.Primary)
            if (showCancel) Button(stringResource(Res.string.common_cancel), onDone)
        }
    }
}

private val START_WIDTH = 112.dp

/** A start time as the field wants it typed: digits only, so the same in every language. */
private const val START_EXAMPLE = "1:35"

/** What is wrong with what was typed into the bookmark form. */
private enum class VideoFormError(val message: StringResource) {
    Link(Res.string.videos_error_link),
    Time(Res.string.videos_error_time),
}

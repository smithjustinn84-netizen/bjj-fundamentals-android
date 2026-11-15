package dev.justinsmith.bjj.ui.videos

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import dev.justinsmith.bjj.core.Library
import dev.justinsmith.bjj.core.VideoBookmark
import dev.justinsmith.bjj.core.itemName
import dev.justinsmith.bjj.design.components.BjjText
import dev.justinsmith.bjj.design.components.Button
import dev.justinsmith.bjj.design.components.ButtonSize
import dev.justinsmith.bjj.design.components.Card
import dev.justinsmith.bjj.design.components.SectionHead
import dev.justinsmith.bjj.design.components.TextInput
import dev.justinsmith.bjj.design.theme.BjjTheme
import dev.justinsmith.bjj.design.theme.Sizes
import dev.justinsmith.bjj.nav.itemKey
import dev.justinsmith.bjj.ui.common.BackLink
import dev.justinsmith.bjj.ui.common.Nav
import dev.justinsmith.bjj.ui.common.Page
import dev.justinsmith.bjj.ui.common.PageHead
import dev.justinsmith.bjj.ui.common.VideoActions
import dev.justinsmith.bjj.ui.common.VideoForm
import dev.justinsmith.bjj.ui.common.VideoRow
import dev.justinsmith.bjj.ui.common.WithSnapshot
import dev.justinsmith.bjj.ui.common.videoActions

/** The search box only shows once there are enough bookmarks to need it. */
private const val SEARCH_THRESHOLD = 4

@Composable
fun VideosRoute(nav: Nav) {
    WithSnapshot { snapshot, viewModel ->
        VideosScreen(snapshot.library, snapshot.videos, nav, viewModel.videoActions())
    }
}

/** Every video bookmark, newest first, with a form to add one about anything or nothing. */
@Composable
fun VideosScreen(
    lib: Library,
    all: List<VideoBookmark>,
    nav: Nav,
    actions: VideoActions,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var adding by rememberSaveable { mutableStateOf(false) }
    val named = all.map { video -> video to video.linkedKind.itemKind?.let { itemName(lib, it, video.linkedId) } }
    val needle = query.trim().lowercase()
    val shown = named.filter { (video, name) ->
        needle.isEmpty() || listOf(video.title, video.note, name.orEmpty()).any { needle in it.lowercase() }
    }
    Page(modifier, maxWidth = Sizes.readingMax) {
        BackLink("More", nav.back)
        PageHead(
            "Video bookmarks",
            about = "Save YouTube links at the exact moment a detail is explained. Paste a link with ?t= and the " +
                "start time fills in. Links open on YouTube.",
        )
        Card {
            SectionHead("Saved (${all.size})") {
                if (!adding) Button("+ Add bookmark", { adding = true }, size = ButtonSize.Small)
            }
            // Behind a button so the saved list, not an empty form, is what the page opens on.
            if (adding) VideoForm(actions.add, onDone = { adding = false }, lib = lib)
            if (all.size >= SEARCH_THRESHOLD) {
                TextInput(query, {
                    query = it
                }, "Search bookmarks", placeholder = "Search…", imeAction = ImeAction.Search)
            }
            if (shown.isEmpty()) {
                BjjText(if (all.isEmpty()) "No bookmarks yet." else "No matches.", color = BjjTheme.palette.muted)
            }
            for ((video, name) in shown) {
                val kind = video.linkedKind.itemKind
                val linked = if (name != null &&
                    kind != null
                ) {
                    name to { nav.open(itemKey(kind, video.linkedId)) }
                } else {
                    null
                }
                VideoRow(video, actions, linked = linked)
            }
        }
    }
}

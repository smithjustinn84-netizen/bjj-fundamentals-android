package dev.justinsmith.bjj.ui.videos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.justinsmith.bjj.design.components.BjjText
import dev.justinsmith.bjj.design.components.Button
import dev.justinsmith.bjj.design.components.ButtonSize
import dev.justinsmith.bjj.design.components.Card
import dev.justinsmith.bjj.design.components.SectionHead
import dev.justinsmith.bjj.design.components.TextInput
import dev.justinsmith.bjj.design.theme.BjjTheme
import dev.justinsmith.bjj.design.theme.Radius
import dev.justinsmith.bjj.design.theme.Sizes
import dev.justinsmith.bjj.design.theme.Spacing
import dev.justinsmith.bjj.nav.Nav
import dev.justinsmith.bjj.nav.itemKey
import dev.justinsmith.bjj.resources.Res
import dev.justinsmith.bjj.resources.tab_more
import dev.justinsmith.bjj.resources.videos_about
import dev.justinsmith.bjj.resources.videos_add_bookmark
import dev.justinsmith.bjj.resources.videos_bookmarks
import dev.justinsmith.bjj.resources.videos_no_matches
import dev.justinsmith.bjj.resources.videos_none
import dev.justinsmith.bjj.resources.videos_saved
import dev.justinsmith.bjj.resources.videos_search
import dev.justinsmith.bjj.resources.videos_search_placeholder
import dev.justinsmith.bjj.ui.common.BackLink
import dev.justinsmith.bjj.ui.common.LazyPage
import dev.justinsmith.bjj.ui.common.Loading
import dev.justinsmith.bjj.ui.common.PageHead
import dev.justinsmith.bjj.ui.common.VideoActions
import dev.justinsmith.bjj.ui.common.VideoForm
import dev.justinsmith.bjj.ui.common.VideoRow
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** The search box only shows once there are enough bookmarks to need it. */
private const val SEARCH_THRESHOLD = 4

@Composable
fun VideosRoute(nav: Nav) {
    val viewModel = koinViewModel<VideosViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    when (val ready = state) {
        VideosUiState.Loading -> Loading()
        is VideosUiState.Ready -> VideosScreen(ready, nav, viewModel.videoActions, viewModel::search)
    }
}

/** Every video bookmark, newest first, with a form to add one about anything or nothing. */
@Composable
fun VideosScreen(
    state: VideosUiState.Ready,
    nav: Nav,
    actions: VideoActions,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // What is typed stays here; the ViewModel is told, and answers with the bookmarks that match.
    var query by rememberSaveable { mutableStateOf("") }
    val currentOnSearch by rememberUpdatedState(onSearch)
    LaunchedEffect(Unit) { if (query.isNotEmpty()) currentOnSearch(query) }
    var adding by rememberSaveable { mutableStateOf(false) }
    val shown = state.shown
    // Lazy: the starter content alone has nearly two hundred bookmarks, too many to lay out before the page shows.
    LazyPage(modifier, maxWidth = Sizes.readingMax) {
        item(key = HEAD_KEY) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s4)) {
                BackLink(stringResource(Res.string.tab_more), nav.back)
                PageHead(
                    stringResource(Res.string.videos_bookmarks),
                    about = stringResource(Res.string.videos_about),
                )
                Head(
                    state,
                    actions,
                    query,
                    onQuery = {
                        query = it
                        onSearch(it)
                    },
                    adding = adding,
                    onAdding = { adding = it },
                )
            }
        }
        items(shown, key = { it.video.id }) { (video, about) ->
            val linked = about?.let { it.name to { nav.open(itemKey(it.item.kind, it.item.id)) } }
            // Off the card now, so each row brings its own paper.
            val paper = Modifier.background(BjjTheme.palette.surface, RoundedCornerShape(Radius.md))
            VideoRow(video, actions, paper, linked = linked)
        }
    }
}

/** The list's own card: how many there are, the way to add one, and the search. */
@Composable
private fun Head(
    state: VideosUiState.Ready,
    actions: VideoActions,
    query: String,
    onQuery: (String) -> Unit,
    adding: Boolean,
    onAdding: (Boolean) -> Unit,
) {
    val shown = state.shown
    Card {
        SectionHead(stringResource(Res.string.videos_saved, state.total)) {
            if (!adding) {
                Button(stringResource(Res.string.videos_add_bookmark), { onAdding(true) }, size = ButtonSize.Small)
            }
        }
        // Behind a button so the saved list, not an empty form, is what the page opens on.
        if (adding) VideoForm(actions.add, onDone = { onAdding(false) }, about = state.about)
        if (state.total >= SEARCH_THRESHOLD) {
            TextInput(
                query,
                onQuery,
                stringResource(Res.string.videos_search),
                placeholder = stringResource(Res.string.videos_search_placeholder),
                imeAction = ImeAction.Search,
            )
        }
        if (shown.isEmpty()) {
            val nothing = if (state.total == 0) Res.string.videos_none else Res.string.videos_no_matches
            BjjText(stringResource(nothing), color = BjjTheme.palette.muted)
        }
    }
}

/** The list's first item, so the rows' keys, which are bookmark ids, never meet it. */
private const val HEAD_KEY = "head"

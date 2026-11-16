package dev.justinsmith.bjj.ui.videos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.justinsmith.bjj.bindings.AppData
import dev.justinsmith.bjj.core.VideoBookmark
import dev.justinsmith.bjj.core.itemName
import dev.justinsmith.bjj.core.services.Snapshot
import dev.justinsmith.bjj.ui.common.AboutOption
import dev.justinsmith.bjj.ui.common.ItemRef
import dev.justinsmith.bjj.ui.common.VideoActions
import dev.justinsmith.bjj.ui.common.VideoCommands
import dev.justinsmith.bjj.ui.common.aboutOptions
import dev.justinsmith.bjj.ui.common.screenState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** A bookmark with the item it is about, when that item still exists. */
data class VideoListRow(val video: VideoBookmark, val about: AboutOption?)

sealed interface VideosUiState {
    data object Loading : VideosUiState

    data class Ready(
        /** The bookmarks that match what was searched for, newest first. */
        val shown: List<VideoListRow>,
        /** How many bookmarks there are in all. */
        val total: Int,
        /** The items a new bookmark can be about. */
        val about: List<AboutOption>,
    ) : VideosUiState
}

fun videosUiState(snapshot: Snapshot, query: String): VideosUiState.Ready {
    val lib = snapshot.library
    val rows = snapshot.videos.map { video ->
        val kind = video.linkedKind.itemKind
        val name = kind?.let { itemName(lib, it, video.linkedId) }
        VideoListRow(
            video,
            if (kind != null &&
                name != null
            ) {
                AboutOption(ItemRef(kind, video.linkedId), name)
            } else {
                null
            },
        )
    }
    val needle = query.trim().lowercase()
    return VideosUiState.Ready(
        shown = rows.filter { (video, about) ->
            needle.isEmpty() ||
                listOf(video.title, video.note, about?.name.orEmpty()).any { needle in it.lowercase() }
        },
        total = rows.size,
        about = aboutOptions(lib),
    )
}

class VideosViewModel(appData: AppData, videos: VideoCommands) : ViewModel() {
    private val query = MutableStateFlow("")

    val uiState: StateFlow<VideosUiState> = screenState(appData, query) { snapshot, query ->
        snapshot?.let { videosUiState(it, query) } ?: VideosUiState.Loading
    }

    val videoActions: VideoActions = videos.actions(viewModelScope)

    fun search(text: String) {
        query.value = text
    }
}

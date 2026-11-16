package dev.justinsmith.bjj.ui.common

import dev.justinsmith.bjj.core.services.VideoService
import dev.justinsmith.bjj.platform.Links
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Opening, adding and deleting video bookmarks, for the ViewModel of any page that lists them. */
class VideoCommands(private val videos: VideoService, private val links: Links) {
    /** The actions a page hands its bookmark rows, with their writes running in [scope]. */
    fun actions(scope: CoroutineScope) = VideoActions(
        open = links::open,
        add = { input -> scope.launch { videos.add(input) } },
        remove = { id -> scope.launch { videos.remove(id) } },
    )
}

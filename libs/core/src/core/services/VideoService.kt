package dev.justinsmith.bjj.core.services

import dev.justinsmith.bjj.core.CoreEnv
import dev.justinsmith.bjj.core.LinkedKind
import dev.justinsmith.bjj.core.VideoBookmark

data class VideoInput(
    val title: String,
    val youtubeId: String,
    val startSeconds: Int,
    val linkedKind: LinkedKind,
    val linkedId: String,
    val note: String,
)

/** The title a bookmark saved without one gets, so every row in a list has something to tap. */
const val UNTITLED_VIDEO = "Untitled clip"

class VideoService(env: CoreEnv) {
    private val storage = env.storage
    private val newId = env.newId
    private val now = env.now

    suspend fun add(input: VideoInput): String {
        val video = VideoBookmark(
            id = newId(),
            title = input.title.trim().ifEmpty { UNTITLED_VIDEO },
            youtubeId = input.youtubeId,
            startSeconds = input.startSeconds,
            linkedKind = input.linkedKind,
            linkedId = input.linkedId,
            note = input.note,
            createdAt = now(),
        )
        storage.videos.put(video)
        return video.id
    }

    suspend fun remove(id: String) = storage.videos.remove(id)
}

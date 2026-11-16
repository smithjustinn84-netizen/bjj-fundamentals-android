@file:OptIn(ExperimentalTestApi::class)

package dev.justinsmith.bjj.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.justinsmith.bjj.core.LinkedKind
import dev.justinsmith.bjj.core.VideoBookmark
import dev.justinsmith.bjj.design.theme.BjjTheme
import dev.justinsmith.bjj.design.theme.Motion
import dev.justinsmith.bjj.ui.common.VideoOverDrawing
import kotlin.test.Test

private const val DRAWING = "drawing"
private const val TITLE = "Video: Frames"

/** Long enough for the page to have arrived and the player to be made. */
private const val PLAYER_SHOWN_MS = Motion.BASE * 3L

private val DRAWING_SIZE = 40.dp

class ItemPictureTest {
    private val video = VideoBookmark(
        id = "v1",
        title = "Frames",
        youtubeId = "abcdefghijk",
        startSeconds = 0,
        linkedKind = LinkedKind.Concept,
        linkedId = "frames",
        note = "",
        createdAt = 0,
    )

    @Test
    fun `a video opens on its drawing and the player joins it once the page has arrived`() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent {
            BjjTheme(dark = false) {
                VideoOverDrawing(video, TITLE) { Box(Modifier.size(DRAWING_SIZE).testTag(DRAWING)) }
            }
        }

        onNodeWithTag(DRAWING).assertExists()
        onNodeWithContentDescription(TITLE).assertDoesNotExist()

        mainClock.advanceTimeBy(PLAYER_SHOWN_MS)

        onNodeWithContentDescription(TITLE).assertExists()
        // Still there underneath: the player is see-through until YouTube has drawn itself.
        onNodeWithTag(DRAWING).assertExists()
    }
}

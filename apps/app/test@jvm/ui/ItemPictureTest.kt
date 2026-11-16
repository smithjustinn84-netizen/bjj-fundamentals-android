@file:OptIn(ExperimentalTestApi::class)

package dev.justinsmith.bjj.ui

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.v2.runComposeUiTest
import dev.justinsmith.bjj.core.LinkedKind
import dev.justinsmith.bjj.core.VideoBookmark
import dev.justinsmith.bjj.design.theme.BjjTheme
import dev.justinsmith.bjj.design.theme.Motion
import dev.justinsmith.bjj.ui.common.VideoOnPaper
import kotlin.test.Test

private const val TITLE = "Video: Frames"

/** Long enough for the page to have arrived and the player to be made. */
private const val PLAYER_SHOWN_MS = Motion.BASE * 3L

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
    fun `the player is made only once the page has arrived`() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent {
            BjjTheme(dark = false) { VideoOnPaper(video, TITLE) }
        }

        onNodeWithContentDescription(TITLE).assertDoesNotExist()

        mainClock.advanceTimeBy(PLAYER_SHOWN_MS)

        onNodeWithContentDescription(TITLE).assertExists()
    }

    @Test
    fun `the paper pulses as loading until the player says it is ready`() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent {
            BjjTheme(dark = false) { VideoOnPaper(video, TITLE) }
        }

        val loading = hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)
        onNode(loading).assertExists()

        mainClock.advanceTimeBy(PLAYER_SHOWN_MS)

        onNode(loading).assertDoesNotExist()
    }
}

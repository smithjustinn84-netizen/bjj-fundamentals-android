package dev.justinsmith.bjj.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private const val ID = "dQw4w9WgXcQ"

class YouTubeTest {
    @Test
    fun `extracts the id from every shape of link`() {
        val links = listOf(
            "https://www.youtube.com/watch?v=$ID",
            "https://youtube.com/watch?v=$ID&list=abc",
            "https://m.youtube.com/watch?v=$ID",
            "https://youtu.be/$ID?t=30",
            "https://www.youtube.com/shorts/$ID",
            "https://www.youtube.com/embed/$ID?start=10",
            "https://www.youtube.com/live/$ID",
            "youtube.com/watch?v=$ID",
            "HTTPS://WWW.YouTube.com/watch?feature=share&v=$ID#t=5",
            "  https://youtu.be/$ID  ",
            ID,
        )
        for (link in links) assertEquals(ID, parseYouTubeId(link), link)
    }

    @Test
    fun `rejects what is not a YouTube video`() {
        val links = listOf(
            "https://vimeo.com/123",
            "not a url",
            "https://youtube.com/watch?v=short",
            "https://evil.example/watch?v=dQw4w9WgXcQ",
            "https://youtube.com.evil.example/watch?v=dQw4w9WgXcQ",
            "https://example.com/?u=youtube.com/watch?v=dQw4w9WgXcQ",
            "",
        )
        for (link in links) assertNull(parseYouTubeId(link), link)
    }

    @Test
    fun `parses a time in every notation`() {
        val times = mapOf(
            "90" to 90,
            "90s" to 90,
            "1m30s" to 90,
            "1h2m3s" to 3723,
            "1:30" to 90,
            "01:02:03" to 3723,
            "2m" to 120,
        )
        for ((input, expected) in times) assertEquals(expected, parseTime(input), input)
    }

    @Test
    fun `rejects what is not a time`() {
        for (input in listOf("", "abc", "1:2:3:4", null, "99999999999999999999")) assertNull(parseTime(input), input)
    }

    @Test
    fun `reads the start from t, start and the hash`() {
        assertEquals(95, parseYouTubeStart("https://youtu.be/$ID?t=95"))
        assertEquals(95, parseYouTubeStart("https://www.youtube.com/watch?v=$ID&t=1m35s"))
        assertEquals(42, parseYouTubeStart("https://www.youtube.com/embed/$ID?start=42"))
        assertEquals(60, parseYouTubeStart("https://www.youtube.com/watch?v=$ID#t=1m"))
        assertNull(parseYouTubeStart("https://www.youtube.com/watch?v=$ID"))
    }

    @Test
    fun `formats seconds`() {
        assertEquals("0:05", formatTime(5))
        assertEquals("1:35", formatTime(95))
        assertEquals("1:02:03", formatTime(3723))
    }

    @Test
    fun `builds a watch address with a timestamp`() {
        assertEquals("https://www.youtube.com/watch?v=$ID&t=95s", watchUrl(ID, 95))
        assertEquals("https://www.youtube.com/watch?v=$ID", watchUrl(ID, 0))
    }

    @Test
    fun `builds a player address that starts at the bookmarked moment`() {
        assertEquals("https://www.youtube-nocookie.com/embed/$ID?start=95", embedUrl(ID, 95))
        assertEquals("https://www.youtube-nocookie.com/embed/$ID", embedUrl(ID, 0))
    }
}

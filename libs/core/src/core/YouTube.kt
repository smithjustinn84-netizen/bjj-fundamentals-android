package dev.justinsmith.bjj.core

// Parses YouTube links and timestamps without calling any YouTube API.

private const val SECONDS_PER_MINUTE = 60
private const val SECONDS_PER_HOUR = 60 * SECONDS_PER_MINUTE

private val ID_RE = Regex("^[A-Za-z0-9_-]{11}$")

fun isYouTubeId(value: String): Boolean = ID_RE.matches(value)

private class ParsedLink(val host: String, val path: String, val query: Map<String, String>, val hash: String)

private val LINK_RE =
    Regex("""^(?:https?://)?([^/?#\s]+)([^?#\s]*)(?:\?([^#\s]*))?(?:#(\S*))?$""", RegexOption.IGNORE_CASE)

/** Splits a link into its parts by hand: core has no URL class to lean on. */
private fun parseLink(input: String): ParsedLink? {
    val match = LINK_RE.find(input.trim()) ?: return null
    val query = LinkedHashMap<String, String>()
    for (pair in match.groupValues[QUERY_GROUP].split('&')) {
        val parts = pair.split('=')
        val key = parts[0]
        // The first occurrence wins.
        if (key.isNotEmpty() && key !in query) query[key] = parts.getOrElse(1) { "" }
    }
    return ParsedLink(
        host = match.groupValues[HOST_GROUP].lowercase(),
        path = match.groupValues[PATH_GROUP],
        query = query,
        hash = match.groupValues[HASH_GROUP],
    )
}

private const val HOST_GROUP = 1
private const val PATH_GROUP = 2
private const val QUERY_GROUP = 3
private const val HASH_GROUP = 4

private val SUBDOMAIN_RE = Regex("""^(www\.|m\.|music\.)""")
private val PATH_ID_RE = Regex("""^/(?:shorts|embed|live|v)/([^/]+)""")

fun parseYouTubeId(input: String): String? {
    val raw = input.trim()
    if (isYouTubeId(raw)) return raw
    val link = parseLink(raw) ?: return null
    val id = when (link.host.replace(SUBDOMAIN_RE, "")) {
        "youtu.be" -> link.path.split('/').getOrNull(1)

        "youtube.com", "youtube-nocookie.com" ->
            if (link.path == "/watch") link.query["v"] else PATH_ID_RE.find(link.path)?.groupValues?.get(1)

        else -> null
    }
    return id?.takeIf(::isYouTubeId)
}

private val DIGITS_RE = Regex("""^\d+$""")
private val CLOCK_RE = Regex("""^\d+(:\d{1,2}){1,2}$""")
private val UNITS_RE = Regex("""^(?:(\d+)h)?(?:(\d+)m)?(?:(\d+)s)?$""")

/**
 * Parses a time like "90", "90s", "1m30s", "1h2m3s", "1:30" or "1:02:03" into seconds. Null if it cannot be
 * understood, or is too long to be a time.
 */
fun parseTime(input: String?): Int? {
    val raw = input?.trim()?.lowercase().orEmpty()
    val seconds: Long? = when {
        raw.isEmpty() -> null

        DIGITS_RE.matches(raw) -> raw.toLongOrNull()

        CLOCK_RE.matches(raw) -> raw.split(':').fold(0L as Long?) { total, part ->
            val value = part.toLongOrNull()
            if (total == null || value == null) null else total * SECONDS_PER_MINUTE + value
        }

        else -> UNITS_RE.find(raw)?.groupValues?.drop(1)?.takeIf { parts ->
            parts.any { it.isNotEmpty() }
        }?.let { parts ->
            val (hours, minutes, secs) = parts.map { if (it.isEmpty()) 0L else it.toLongOrNull() }
            if (hours == null || minutes == null || secs == null) {
                null
            } else {
                hours * SECONDS_PER_HOUR + minutes * SECONDS_PER_MINUTE + secs
            }
        }
    }
    return seconds?.takeIf { it <= Int.MAX_VALUE }?.toInt()
}

private val HASH_TIME_RE = Regex("""(?:^|&)t=([^&]+)""")

/** Pulls the start time out of a YouTube link (`t=`, `start=`, or `#t=`). */
fun parseYouTubeStart(input: String): Int? {
    val link = parseLink(input) ?: return null
    val fromQuery = link.query["t"] ?: link.query["start"]
    if (!fromQuery.isNullOrEmpty()) return parseTime(fromQuery)
    return HASH_TIME_RE.find(link.hash)?.let { parseTime(it.groupValues[1]) }
}

fun formatTime(totalSeconds: Int): String {
    val all = maxOf(0, totalSeconds)
    val hours = all / SECONDS_PER_HOUR
    val minutes = all % SECONDS_PER_HOUR / SECONDS_PER_MINUTE
    val seconds = (all % SECONDS_PER_MINUTE).toString().padStart(2, '0')
    return if (hours > 0) "$hours:${minutes.toString().padStart(2, '0')}:$seconds" else "$minutes:$seconds"
}

// A YouTube id is letters, digits, `_` and `-` (`isYouTubeId`, which a backup's check and the video form both
// apply), none of which a link needs escaped.

fun watchUrl(youtubeId: String, startSeconds: Int): String {
    val time = if (startSeconds > 0) "&t=${startSeconds}s" else ""
    return "https://www.youtube.com/watch?v=$youtubeId$time"
}

/**
 * The address of the embedded player for a video, starting at the bookmarked moment. It is on YouTube's
 * "no cookie" domain, which sets no cookies until the video is played.
 */
fun embedUrl(youtubeId: String, startSeconds: Int): String {
    val start = if (startSeconds > 0) "?start=$startSeconds" else ""
    return "https://www.youtube-nocookie.com/embed/$youtubeId$start"
}

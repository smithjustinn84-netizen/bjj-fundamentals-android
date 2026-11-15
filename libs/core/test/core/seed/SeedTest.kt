package dev.justinsmith.bjj.core.seed

import dev.justinsmith.bjj.core.Concept
import dev.justinsmith.bjj.core.CoreJson
import dev.justinsmith.bjj.core.ItemKind
import dev.justinsmith.bjj.core.Position
import dev.justinsmith.bjj.core.Transition
import dev.justinsmith.bjj.core.VideoBookmark
import dev.justinsmith.bjj.core.endsTheRoll
import dev.justinsmith.bjj.core.golden.SEED_GOLDEN
import dev.justinsmith.bjj.core.isYouTubeId
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * A fingerprint of a JSON value: every string, number and boolean in it, keys in sorted order, hashed with
 * 32-bit FNV-1a over UTF-16 code units. The fixture script (tools/web-fixtures) computes the same over the web
 * app's starter content.
 */
private fun fingerprint(value: JsonElement): Int {
    var hash = FNV_OFFSET
    fun feed(text: String) {
        for (char in text) hash = (hash xor char.code) * FNV_PRIME
        hash = (hash xor SEPARATOR) * FNV_PRIME
    }

    fun walk(node: JsonElement) {
        when (node) {
            is JsonArray -> node.forEach(::walk)

            is JsonObject -> for (key in node.keys.sorted()) {
                feed(key)
                walk(node.getValue(key))
            }

            is JsonPrimitive -> feed(node.content)
        }
    }
    walk(value)
    return hash
}

private const val FNV_OFFSET = 0x811c9dc5.toInt()
private const val FNV_PRIME = 0x01000193
private const val SEPARATOR = 0x1f

class SeedTest {
    private val starter = starterContent(0)

    @Test
    fun `is the web app's starter content to the letter`() {
        val tables = mapOf(
            "concepts" to CoreJson.encodeToJsonElement(ListSerializer(Concept.serializer()), starter.concepts),
            "positions" to CoreJson.encodeToJsonElement(ListSerializer(Position.serializer()), starter.positions),
            "transitions" to CoreJson.encodeToJsonElement(ListSerializer(Transition.serializer()), starter.transitions),
            "videos" to CoreJson.encodeToJsonElement(ListSerializer(VideoBookmark.serializer()), starter.videos),
        )
        val actual = tables.mapValues { (_, rows) -> (rows as JsonArray).size to fingerprint(rows) }
        assertEquals(SEED_GOLDEN, actual)
    }

    @Test
    fun `uses each id once across concepts and positions and techniques since dismissals are keyed by id alone`() {
        val ids = starter.concepts.map { it.id } + starter.positions.map { it.id } + starter.transitions.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `gives every starter video a unique id and a unique valid YouTube id`() {
        assertEquals(starter.videos.size, starter.videos.map { it.id }.toSet().size)
        assertEquals(starter.videos.size, starter.videos.map { it.youtubeId }.toSet().size)
        assertEquals(emptyList(), starter.videos.filterNot { isYouTubeId(it.youtubeId) }.map { it.id })
    }

    @Test
    fun `only links positions to starter concepts`() {
        val conceptIds = starter.concepts.map { it.id }.toSet()
        assertEquals(emptyList(), starter.positions.flatMap { p -> p.conceptIds.filterNot { it in conceptIds } })
    }

    @Test
    fun `starts and ends every technique at a starter position`() {
        val positionIds = starter.positions.map { it.id }.toSet()
        val broken = starter.transitions.filter {
            it.fromId !in positionIds || (!endsTheRoll(it.type) && it.toId !in positionIds)
        }
        assertEquals(emptyList(), broken.map { it.id })
    }

    @Test
    fun `links every starter video to a starter item`() {
        val itemIds = mapOf(
            ItemKind.Concept to starter.concepts.map { it.id }.toSet(),
            ItemKind.Position to starter.positions.map { it.id }.toSet(),
            ItemKind.Transition to starter.transitions.map { it.id }.toSet(),
        )
        val unlinked = starter.videos.filter { video ->
            val kind = video.linkedKind.itemKind
            kind == null || video.linkedId !in itemIds.getValue(kind)
        }
        assertEquals(emptyList(), unlinked.map { it.id })
    }

    @Test
    fun `bookmarks at least two videos for every starter concept`() {
        val counts = starter.videos.filter {
            it.linkedKind.itemKind == ItemKind.Concept
        }.groupingBy { it.linkedId }.eachCount()
        assertEquals(emptyList(), starter.concepts.filter { (counts[it.id] ?: 0) < 2 }.map { it.id })
    }

    @Test
    fun `has a drill for every starter technique`() {
        assertEquals(emptyList(), starter.transitions.filter { it.drill.isNullOrEmpty() }.map { it.id })
    }
}

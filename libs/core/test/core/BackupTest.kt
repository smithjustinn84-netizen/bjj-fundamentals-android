package dev.justinsmith.bjj.core

import dev.justinsmith.bjj.core.golden.WEB_BACKUP
import dev.justinsmith.bjj.core.testing.concept
import dev.justinsmith.bjj.core.testing.position
import dev.justinsmith.bjj.core.testing.review
import dev.justinsmith.bjj.core.testing.trainingEntry
import dev.justinsmith.bjj.core.testing.transition
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull

/** A backup's JSON with one thing changed, addressed by a path of keys and list positions. */
private fun JsonElement.with(vararg path: Any, change: (JsonElement?) -> JsonElement?): JsonElement {
    if (path.isEmpty()) return change(this) ?: JsonNull
    val rest = path.drop(1).toTypedArray()
    return when (val step = path.first()) {
        is String -> {
            val fields = (this as JsonObject).toMutableMap()
            val changed = if (rest.isEmpty()) {
                change(
                    fields[step],
                )
            } else {
                fields.getValue(step).with(*rest, change = change)
            }
            if (changed == null) fields.remove(step) else fields[step] = changed
            JsonObject(fields)
        }

        is Int -> JsonArray(
            (this as JsonArray).mapIndexed { i, e ->
                if (i ==
                    step
                ) {
                    e.with(*rest, change = change)
                } else {
                    e
                }
            },
        )

        else -> error("a path is keys and positions")
    }
}

/** Numbers by value, so 3 and 3.0 are the same number as they are to anything that reads the file. */
private fun JsonElement.normalized(): Any? = when (this) {
    is JsonNull -> null
    is JsonPrimitive -> if (isString) content else content.toDoubleOrNull() ?: content
    is JsonArray -> map { it.normalized() }
    is JsonObject -> mapValues { it.value.normalized() }
}

private fun everyValue(element: JsonElement): List<JsonElement> = when (element) {
    is JsonArray -> element.flatMap(::everyValue)
    is JsonObject -> element.values.flatMap(::everyValue)
    else -> listOf(element)
}

class BackupTest {
    private val data = BackupData(
        concepts = listOf(concept()),
        positions = listOf(position()),
        transitions = listOf(transition(), transition(id = "t2", role = PositionRole.Bottom, blueBeltTest = true)),
        videos = listOf(VideoBookmark("v1", "Clip", "dQw4w9WgXcQ", 95, LinkedKind.Position, "p1", "", 1)),
        reviews = listOf(review(CardKind.Situation, "mount:bottom", lapses = 2)),
        reviewLog = listOf(ReviewLogEntry(null, "concept:c1", Grade.Good, 5)),
        starterDismissals = listOf(StarterDismissal("scissor-sweep", 9)),
        trainingLog = listOf(trainingEntry(), trainingEntry(id = "log2", stuckIds = listOf("mount:bottom"))),
        settings = listOf(Setting(SettingId.Theme, "dark", 3)),
    )
    private val file = BackupFile(BACKUP_SCHEMA_VERSION, exportedAt = 123, data = data)
    private val json = CoreJson.parseToJsonElement(encodeBackup(file))

    private fun problemOf(bad: JsonElement) = assertFailsWith<BackupException> { validateBackup(bad) }.problem

    private fun bad(table: BackupTable, field: String, value: JsonElement?) =
        problemOf(json.with("data", table.key, 0, field) { value })

    @Test
    fun `reads back exactly what it wrote`() {
        assertEquals(file, parseBackup(encodeBackup(file)))
    }

    @Test
    fun `writes every table and leaves absent fields out rather than writing null`() {
        val tables = (json as JsonObject).getValue("data") as JsonObject
        assertEquals(BackupTable.entries.map { it.key }, tables.keys.toList())
        assertFalse(everyValue(json).any { it is JsonNull })
        val plain = (tables.getValue("transitions") as JsonArray).first() as JsonObject
        assertEquals(emptySet(), plain.keys intersect setOf("role", "endRole", "drill", "blueBeltTest"))
    }

    @Test
    fun `reads a backup the web app wrote and writes the same thing back`() {
        val read = parseBackup(WEB_BACKUP)
        assertEquals(
            CoreJson.parseToJsonElement(WEB_BACKUP).normalized(),
            CoreJson.parseToJsonElement(encodeBackup(read)).normalized(),
        )
        // The technique from before roles and drills existed still has none of them.
        val old = read.data.transitions.first { it.id == "old-sweep" }
        assertEquals(listOf(null, null, null, null), listOf(old.role, old.endRole, old.drill, old.blueBeltTest))
        assertEquals(LinkedKind.None, read.data.videos.first { it.linkedId.isEmpty() }.linkedKind)
        assertNull(read.data.trainingLog.first { it.id == "before-stuck" }.stuckIds)
    }

    @Test
    fun `treats missing tables as empty and a missing export time as zero`() {
        val only = buildJsonObject {
            put("app", BACKUP_APP)
            put("schemaVersion", 1)
            put(
                "data",
                buildJsonObject {
                    put(
                        "concepts",
                        (json as JsonObject).getValue("data").let {
                            (it as JsonObject).getValue("concepts")
                        },
                    )
                },
            )
        }
        val read = validateBackup(only)
        assertEquals(1, read.data.concepts.size)
        assertEquals(emptyList(), read.data.videos)
        assertEquals(emptyList(), read.data.starterDismissals)
        assertEquals(0, read.exportedAt)
        assertEquals(1, read.schemaVersion)
    }

    @Test
    fun `refuses a file that is not a backup of this app`() {
        assertEquals(BackupProblem.NotJson, assertFailsWith<BackupException> { parseBackup("{ not json") }.problem)
        assertEquals(BackupProblem.NotAnObject, problemOf(JsonPrimitive("nope")))
        assertEquals(BackupProblem.NotOurs, problemOf(json.with("app") { JsonPrimitive("other") }))
        assertEquals(BackupProblem.NoSchemaVersion, problemOf(json.with("schemaVersion") { null }))
        assertEquals(BackupProblem.Newer("99"), problemOf(json.with("schemaVersion") { JsonPrimitive(99) }))
        assertEquals(BackupProblem.NoData, problemOf(json.with("data") { null }))
    }

    @Test
    fun `says which table and entry and field is wrong`() {
        val notAList = problemOf(json.with("data", "positions") { JsonObject(emptyMap()) })
        assertEquals(BackupProblem.NotAList(BackupTable.Positions), notAList)
        assertEquals("\"positions\" should be a list.", notAList.message)
        val notARow = problemOf(json.with("data", "concepts") { JsonArray(listOf(JsonPrimitive("x"))) })
        assertEquals(BackupProblem.NotARow(BackupTable.Concepts, 1), notARow)
        val second = problemOf(json.with("data", "transitions", 1, "name") { JsonPrimitive(7) })
        assertEquals(BackupProblem.BadField(BackupTable.Transitions, 2, "name"), second)
        assertEquals("\"transitions\" entry 2: \"name\" is missing or invalid.", second.message)
    }

    @Test
    fun `refuses a bad value in any field`() {
        val cases = listOf(
            Triple(BackupTable.Positions, "id", JsonPrimitive("")),
            Triple(BackupTable.Positions, "category", JsonPrimitive("flying")),
            Triple(BackupTable.Concepts, "keyPoints", null),
            Triple(BackupTable.Concepts, "order", JsonPrimitive("1")),
            Triple(BackupTable.Concepts, "order", JsonPrimitive(1.5)),
            Triple(BackupTable.Transitions, "steps", JsonArray(listOf(JsonPrimitive(1)))),
            Triple(BackupTable.Transitions, "blueBeltTest", JsonPrimitive("yes")),
            Triple(BackupTable.Transitions, "role", JsonPrimitive("sideways")),
            Triple(BackupTable.Transitions, "endRole", JsonPrimitive("sideways")),
            Triple(BackupTable.Transitions, "drill", JsonPrimitive(5)),
            // The web app refuses a null where a field may be absent; so does this one.
            Triple(BackupTable.Transitions, "drill", JsonNull),
            Triple(BackupTable.Videos, "youtubeId", JsonPrimitive("javascript:1")),
            Triple(BackupTable.Videos, "startSeconds", JsonPrimitive(-1)),
            Triple(BackupTable.Videos, "linkedKind", JsonPrimitive("situation")),
            Triple(BackupTable.Reviews, "kind", JsonPrimitive("mood")),
            Triple(BackupTable.Reviews, "ease", JsonPrimitive("2.5")),
            Triple(BackupTable.ReviewLog, "grade", JsonPrimitive("meh")),
            Triple(BackupTable.StarterDismissals, "dismissedAt", null),
            Triple(BackupTable.TrainingLog, "day", JsonPrimitive("yesterday")),
            Triple(BackupTable.TrainingLog, "minutes", JsonPrimitive(-1)),
            Triple(BackupTable.TrainingLog, "minutes", JsonPrimitive(45.5)),
            Triple(BackupTable.Settings, "id", JsonPrimitive("font")),
            Triple(BackupTable.Settings, "updatedAt", JsonPrimitive(true)),
        )
        for ((table, field, value) in cases) {
            assertEquals(BackupProblem.BadField(table, 1, field), bad(table, field, value), "$table $field $value")
        }
    }

    @Test
    fun `names the file after the local day`() {
        val east = LocalZone { 10 * HOUR_MS }
        // 5 January 2026, 20:00 UTC: already the 6th ten hours east.
        val evening = 1_767_643_200_000
        assertEquals("bjj-fundamentals-backup-2026-01-05.json", backupFilename(evening, LocalZone.Utc))
        assertEquals("bjj-fundamentals-backup-2026-01-06.json", backupFilename(evening, east))
    }
}

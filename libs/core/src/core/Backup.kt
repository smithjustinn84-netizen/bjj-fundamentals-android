package dev.justinsmith.bjj.core

import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// The backup file format, the same file the web app writes and reads. A backup is untrusted input (the user
// can pick any file), so every field is checked before anything is written to storage.

const val BACKUP_APP = "bjj-fundamentals"

/**
 * Bump when the shape of any table changes, together with the web app's, and teach [validateBackup] to accept
 * the older shape.
 */
const val BACKUP_SCHEMA_VERSION = 9

data class BackupFile(val schemaVersion: Int, val exportedAt: Long, val data: BackupData)

/** What is wrong with a file offered as a backup. [message] says it in words, as the web app does. */
sealed class BackupProblem(val message: String) {
    data object NotJson : BackupProblem("The file is not valid JSON.")

    data object NotAnObject : BackupProblem("The file is not a JSON object.")

    data object NotOurs : BackupProblem("This file is not a BJJ Fundamentals backup.")

    data object NoSchemaVersion : BackupProblem("The backup is missing its schema version.")

    data class Newer(val schemaVersion: String) :
        BackupProblem("This backup was made by a newer version of the app (schema $schemaVersion).")

    data object NoData : BackupProblem("The backup has no data section.")

    data class NotAList(val table: BackupTable) : BackupProblem("\"${table.key}\" should be a list.")

    /** [entry] counts from 1. */
    data class NotARow(val table: BackupTable, val entry: Int) :
        BackupProblem("\"${table.key}\" entry $entry is not an object.")

    data class BadField(val table: BackupTable, val entry: Int, val field: String) :
        BackupProblem("\"${table.key}\" entry $entry: \"$field\" is missing or invalid.")
}

class BackupException(val problem: BackupProblem) : Exception(problem.message)

/** Whether a value, or null where the field is absent, is acceptable. */
private typealias Check = (JsonElement?) -> Boolean

private val JsonElement?.string: String?
    get() = (this as? JsonPrimitive)?.takeIf { it.isString }?.content

private val JsonElement?.number: Double?
    get() = (this as? JsonPrimitive)?.takeIf { !it.isString }?.content?.toDoubleOrNull()?.takeIf { it.isFinite() }

private val text: Check = { it.string != null }
private val id: Check = { !it.string.isNullOrEmpty() }

/** A fraction is allowed: only a card's ease is one. */
private val fraction: Check = { it.number != null }

// The web app takes any number wherever it wants one, but only ever writes whole ones in these fields; here a
// count is an Int and a moment a Long, so a file with a fraction or a number out of range is refused.
private val whole: Check = {
    it.number?.let { n -> n % 1.0 == 0.0 && n >= Int.MIN_VALUE && n <= Int.MAX_VALUE } == true
}
private val wholeFromZero: Check = { whole(it) && (it.number ?: -1.0) >= 0 }
private val moment: Check = { it.number?.let { n -> n % 1.0 == 0.0 && n >= -MAX_MOMENT && n <= MAX_MOMENT } == true }

/** The largest whole number JSON carries exactly, which is far beyond any date. */
private const val MAX_MOMENT = 9_007_199_254_740_991.0

private val bool: Check = {
    (it as? JsonPrimitive)?.let { p ->
        !p.isString &&
            (p.content == "true" || p.content == "false")
    } ==
        true
}
private val textList: Check = { (it as? JsonArray)?.all(text) == true }

private fun oneOf(allowed: Collection<String>): Check = { it.string in allowed }

/** An absent field passes; one written as null does not, as in the web app. */
private fun optional(check: Check): Check = { it == null || check(it) }

/** One check per field of each table. A field with no check here is not read. */
private val SCHEMAS: Map<BackupTable, Map<String, Check>> = mapOf(
    BackupTable.Positions to mapOf(
        "id" to id,
        "name" to text,
        "category" to oneOf(PositionCategory.entries.map { it.key }),
        "summary" to text,
        "topGoals" to textList,
        "bottomGoals" to textList,
        "conceptIds" to textList,
        "order" to whole,
        "notes" to text,
        "createdAt" to moment,
        "updatedAt" to moment,
    ),
    BackupTable.Transitions to mapOf(
        "id" to id,
        "name" to text,
        "type" to oneOf(TransitionType.entries.map { it.key }),
        "fromId" to text,
        "toId" to text,
        // Absent before schema 4; `performerRole` fills the gap on read.
        "role" to optional(oneOf(PositionRole.entries.map { it.key })),
        // Absent before schema 8; `endRole` fills the gap on read.
        "endRole" to optional(oneOf(PositionRole.entries.map { it.key })),
        "steps" to textList,
        "commonMistakes" to textList,
        // Absent before schema 6; `techniqueDrill` fills the gap on read.
        "drill" to optional(text),
        // Absent in schema 1 backups, which predate the blue belt list.
        "blueBeltTest" to optional(bool),
        "notes" to text,
        "createdAt" to moment,
        "updatedAt" to moment,
    ),
    BackupTable.Concepts to mapOf(
        "id" to id,
        "name" to text,
        "summary" to text,
        "keyPoints" to textList,
        "drill" to text,
        "order" to whole,
        "notes" to text,
        "createdAt" to moment,
        "updatedAt" to moment,
    ),
    BackupTable.Videos to mapOf(
        "id" to id,
        "title" to text,
        "youtubeId" to { it.string?.let(::isYouTubeId) == true },
        "startSeconds" to wholeFromZero,
        "linkedKind" to oneOf(listOf("") + ItemKind.entries.map { it.key }),
        "linkedId" to text,
        "note" to text,
        "createdAt" to moment,
    ),
    BackupTable.Reviews to mapOf(
        "id" to id,
        // Situation cards arrived with schema 7.
        "kind" to oneOf(CardKind.entries.map { it.key }),
        "itemId" to id,
        "ease" to fraction,
        "intervalDays" to whole,
        "repetitions" to whole,
        "lapses" to whole,
        "due" to moment,
        "lastReviewed" to moment,
    ),
    BackupTable.ReviewLog to mapOf(
        "id" to optional(moment),
        "cardId" to id,
        "grade" to oneOf(Grade.entries.map { it.key }),
        "reviewedAt" to moment,
    ),
    // Absent from schema 1 and 2 backups; a missing table loads as empty.
    BackupTable.StarterDismissals to mapOf(
        "id" to id,
        "dismissedAt" to moment,
    ),
    // Absent before schema 5; a missing table loads as empty.
    BackupTable.TrainingLog to mapOf(
        "id" to id,
        "day" to { it.string?.let(::isDayKey) == true },
        "minutes" to wholeFromZero,
        "taught" to text,
        "worked" to text,
        "struggled" to text,
        "focus" to text,
        "techniqueIds" to textList,
        // Absent before schema 8; `stuckSituationIds` reads a missing list as empty.
        "stuckIds" to optional(textList),
        "createdAt" to moment,
        "updatedAt" to moment,
    ),
    // Absent before schema 9; a missing table loads as empty, so every setting takes its default.
    BackupTable.Settings to mapOf(
        "id" to oneOf(SettingId.entries.map { it.key }),
        // What a value may be is each setting's own business: `themeChoice` reads an unknown one as the default.
        "value" to text,
        "updatedAt" to moment,
    ),
)

private fun fail(problem: BackupProblem): Nothing = throw BackupException(problem)

/** The table's rows, each checked field by field. A missing table is empty, so partial backups still load. */
private fun validRows(table: BackupTable, rows: JsonElement?): JsonArray {
    if (rows == null) return JsonArray(emptyList())
    if (rows !is JsonArray) fail(BackupProblem.NotAList(table))
    val schema = SCHEMAS.getValue(table)
    rows.forEachIndexed { index, row ->
        if (row !is JsonObject) fail(BackupProblem.NotARow(table, index + 1))
        for ((field, check) in schema) {
            if (!check(row[field])) fail(BackupProblem.BadField(table, index + 1, field))
        }
    }
    return rows
}

/** Checks that parsed JSON is one of our backups and reads it. Throws [BackupException] saying what is wrong. */
fun validateBackup(value: JsonElement): BackupFile {
    if (value !is JsonObject) fail(BackupProblem.NotAnObject)
    if (value["app"].string != BACKUP_APP) fail(BackupProblem.NotOurs)
    val version = value["schemaVersion"].number ?: fail(BackupProblem.NoSchemaVersion)
    if (version > BACKUP_SCHEMA_VERSION) fail(BackupProblem.Newer(value["schemaVersion"].toString()))
    val data = value["data"] as? JsonObject ?: fail(BackupProblem.NoData)
    val checked = JsonObject(BackupTable.entries.associate { it.key to validRows(it, data[it.key]) })
    return BackupFile(
        schemaVersion = version.toInt(),
        exportedAt = value["exportedAt"].takeIf(moment).number?.toLong() ?: 0,
        // Every row of every table was just checked field by field, so reading them as models cannot fail.
        data = CoreJson.decodeFromJsonElement(BackupData.serializer(), checked),
    )
}

/** Reads a backup from the text of a file. Throws [BackupException] saying what is wrong. */
fun parseBackup(text: String): BackupFile {
    val json = try {
        CoreJson.parseToJsonElement(text)
    } catch (_: SerializationException) {
        fail(BackupProblem.NotJson)
    }
    return validateBackup(json)
}

private val PRETTY = Json(CoreJson) { prettyPrint = true }

/** The text of a backup file, every table present even when empty, laid out to be read. */
fun encodeBackup(file: BackupFile): String {
    fun <T> rows(serializer: kotlinx.serialization.KSerializer<T>, rows: List<T>) =
        CoreJson.encodeToJsonElement(ListSerializer(serializer), rows)
    val json = buildJsonObject {
        put("app", BACKUP_APP)
        put("schemaVersion", file.schemaVersion)
        put("exportedAt", file.exportedAt)
        put(
            "data",
            buildJsonObject {
                put(BackupTable.Positions.key, rows(Position.serializer(), file.data.positions))
                put(BackupTable.Transitions.key, rows(Transition.serializer(), file.data.transitions))
                put(BackupTable.Concepts.key, rows(Concept.serializer(), file.data.concepts))
                put(BackupTable.Videos.key, rows(VideoBookmark.serializer(), file.data.videos))
                put(BackupTable.Reviews.key, rows(ReviewState.serializer(), file.data.reviews))
                put(BackupTable.ReviewLog.key, rows(ReviewLogEntry.serializer(), file.data.reviewLog))
                put(BackupTable.StarterDismissals.key, rows(StarterDismissal.serializer(), file.data.starterDismissals))
                put(BackupTable.TrainingLog.key, rows(TrainingEntry.serializer(), file.data.trainingLog))
                put(BackupTable.Settings.key, rows(Setting.serializer(), file.data.settings))
            },
        )
    }
    return PRETTY.encodeToString(JsonObject.serializer(), json)
}

fun backupFilename(now: Long, zone: LocalZone): String = "bjj-fundamentals-backup-${localDayKey(now, zone)}.json"

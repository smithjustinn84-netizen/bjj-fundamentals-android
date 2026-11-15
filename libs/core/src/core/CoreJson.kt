package dev.justinsmith.bjj.core

import kotlinx.serialization.json.Json

/**
 * How a stored thing becomes JSON, in a backup and in a storage engine that keeps documents. A field that is
 * absent is left out, never written as null: the web app's backup check takes a missing field and refuses a
 * null one. A field this version does not know is skipped.
 */
val CoreJson: Json = Json {
    explicitNulls = false
    ignoreUnknownKeys = true
}

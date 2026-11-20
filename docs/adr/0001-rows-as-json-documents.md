# ADR-0001: Room stores every row as a JSON document

**Status:** Accepted
**Date:** 2026-11-15 (made in the initial port; recorded 2026-11-19)
**Deciders:** Maintainer

## Context

The web app (`../bjj`) keeps its data in IndexedDB through Dexie: each table is a set of JavaScript objects keyed by id, with no columns. Its backup file is those objects, table by table. This app must read and write the same backup file (same shape, same `BACKUP_SCHEMA_VERSION`), and its models follow the web app's field by field. When the web app adds a field to a concept or a review state, this app's model gains it too.

Room is the database. Room is relational: an entity is a table with typed columns, and every column change is a new schema version with a migration. A database in someone's pocket is at the version it was created at, so a committed schema can never be edited.

The app reads everything at once (`Queries.snapshot()`, one transaction), and filters and sorts in core. Nothing queries by field in SQL.

## Decision

Two tables, and they change only when the engine itself must do something new:

- `rows(tbl, id, json)`: every stored thing of every kind, keyed by its table and id. `json` is the same JSON the backup holds, encoded with `CoreJson`.
- `review_log(id, card_id, grade, reviewed_at)`: the review history, append-only, under an id the engine assigns. It has columns because it is the one thing read by range (`since(timestamp)`) and because its id is the engine's, not the app's.

A new field on a model is not a schema change.

## Options considered

### Option A: one document table (chosen)

| Dimension | Assessment |
| --- | --- |
| Migrations | None for model changes. Version 1 has held since the port. |
| Backup parity | A row is the backup's JSON, so a backup is the rows and a restore is putting them back. One shape, not two. |
| Keeping up with the web app | A new web field means a new Kotlin field and its check. Nothing in storage. |
| Querying | By table and id only. Fine while everything is read into one snapshot. |

**Pros:** model changes never touch storage; one serialisation path for backup and storage; the engine is small enough that `MemoryStorage` and `RoomStorage` are easy to keep in step under one contract.
**Cons:** no SQL filtering, indexing or constraints on fields; the whole row is decoded to read any field; the database file is opaque without decoding JSON.

### Option B: an entity per model, a column per field

| Dimension | Assessment |
| --- | --- |
| Migrations | A schema version and a migration for every field added or changed, written and tested for each. |
| Backup parity | A second shape (entities) beside the backup JSON, with a mapping both ways that can drift. |
| Keeping up with the web app | Every web model change becomes a model change, an entity change, a migration and a mapping change. |
| Querying | Full SQL, indexes, foreign keys. |

**Pros:** the database can filter, join and enforce integrity; rows are readable in a database inspector.
**Cons:** the migration and mapping work is paid on every model change, which is the most frequent change this app makes; integrity rules would sit in two places, since an imported backup must be checked in core anyway.

### Option C: a store outside Room (DataStore, files)

Rejected without much weighing: all persistence goes through `AppStorage` and one transactional engine, and DataStore and files give no multi-table transactions.

## Trade-off analysis

What Option B buys is querying in SQL. This app never asks for it: every screen is worked out from one snapshot read in one transaction, and the library is hundreds of rows, not millions. What Option B costs is paid on the most common change there is, a model following the web app. Option A gives up a capability that is unused, to remove a cost that is constant.

The review log is the exception that shows the rule: it is the one thing read by range and the one with an engine-assigned id, so it is the one thing with columns.

## Consequences

- Easier: adding or changing a model field (no schema version, no migration); backup and restore, which are the same JSON in and out; a new engine, which only stores documents.
- Harder: a query by field. If one is ever needed, it is done in core over the snapshot, or the engine gains a new table and a new schema version.
- Rules that follow from it:
  - `schemas/…/1.json` is never edited. A change to a table is version 2 with a migration.
  - Rows are read `ORDER BY id`, the order IndexedDB hands them back in, so a tie in a sort breaks the same way in both apps.
  - Every stored field is checked when a backup is imported, in core, because the database checks nothing.
- Revisit if: the snapshot read becomes slow enough to show (that would mean reading less per change, not columns per se), or a feature needs a query over a field that core cannot do over the snapshot.

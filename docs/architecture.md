# Architecture

BJJ Fundamentals for Android is the web app (`../bjj`) again, in Kotlin. The web app's one rule carries over unchanged: imports only point inward, `ui → bindings → storage → core`, and `libs/core` is the app; everything else is a replaceable shell round it.

## Modules

| Module | What it is | Depends on |
| --- | --- | --- |
| `libs/core` | Models, rules, starter content, `AppStorage`, services, backup format. Pure Kotlin. | serialization, coroutines |
| `libs/core-testing` | The storage contract, `testCore()`, entity builders. | `core` |
| `libs/storage-room` | `AppStorage` over Room. | `core`, Room |
| `libs/design` | Theme, tokens, components, icons, drawings. | `core` |
| `apps/app` | Composition root, navigation, every screen. | `design`, `core`, `storage-room` |
| `apps/androidApp` | Manifest, resources, `MainActivity`, `BjjApplication`. | `app` |
| `tooling/*` | The ktlint and detekt build plugins. | |

Every library targets `android` and `jvm`. There is no desktop app: the `jvm` target is there so tests run on the plain JVM in seconds, including Room over a real database file and Compose screens drawn off-screen.

## Core

Each file is the Kotlin of the web file with the same name (`srs.ts` → `Srs.kt`). What differs, and why:

- **Time, ids and the time zone come from `CoreEnv`.** Core has no clock and no calendar; `Days.kt` works out calendar days from a day number and the offset `LocalZone` supplies.
- **`jsRound`.** The web app rounds a half up; Kotlin's own `round` sends it to the even neighbour. Everything that rounds goes through `jsRound`.
- **Geometry, not SVG.** `Figure.kt` and `MapLayout.kt` return points and curves where the web app returns path strings; the numbers are the same.
- **A backup's problems are typed** (`BackupProblem`), with the web app's wording as their message.
- **`Queries.snapshot()`** reads everything in one transaction. The web app runs one query per hook; here every screen reads the one snapshot.

Starter content (`seed/Starter*.kt`) and the lessons are generated from the web app by `tools/web-fixtures`. So are the golden values under `libs/core/test/core/golden/`, which the tests must reproduce: the schedule, the teaching order, each lesson's cards, technique links, the map's layout, figure geometry and a backup file the web app wrote.

## Storage

`AppStorage` is nine repositories, `transaction { }`, a `changes` flow and `reset()`. Two engines pass the contract in `core-testing`:

- `MemoryStorage` (in core): for tests, and the reference for a new engine.
- `RoomStorage`: two tables. `rows(tbl, id, json)` holds every stored thing as a document, the same JSON a backup holds; `review_log` holds the history under an auto-incremented id. A new field on a model is not a schema change. Starter content is inserted as the database is created, in the same transaction.

`transaction { }` runs on one writer connection (`useWriterConnection { immediateTransaction { } }`); a transaction inside a transaction joins it, and `changes` emits once when the outermost commits.

## From storage to a screen

```
RoomStorage ──changes──▶ AppData.snapshot: StateFlow<Snapshot?>
                               │
                 AppViewModel (shared) ──▶ XRoute ──▶ XScreen(lib, …)   draws
                               │
                     pure functions in core decide what to show
```

`AppData` (one per process) reads a new `Snapshot` after every change. Pages that only show the library start with `WithSnapshot` and work out what to show with core's functions. Pages with a form or a session (review, the editor, the log, backup) have a ViewModel of their own for that state and for their writes, which go through core's services.

## Navigation

One back stack of `@Serializable` keys (`nav/Routes.kt`), shown by Navigation 3 in `App.kt`. Today is the root; another tab replaces whatever is above it. A review session is a pushed page, and the bar goes while it is on top. Pages move through the `Nav` they are handed.

## What only Android can do

Each is an `expect` in `apps/app/src` with its `actual` in `src@android` and a stand-in for tests in `src@jvm`:

- `VideoPlayer`: YouTube's player in a WebView, the one thing that contacts another site.
- `rememberOnline`: whether to show that player or the drawing.
- `rememberBackupFiles`: the system's file pickers.
- `SystemBars`: the status and navigation icons, kept readable over the app's theme.
- `reducedMotion` (in `design`): the system's "remove animations" setting.

The storage engine, ids, the clock and the time zone are bound in `src@android/di/AppKoin.android.kt`, the one place that names Room.

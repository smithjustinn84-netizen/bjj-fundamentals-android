# Architecture

BJJ Fundamentals for Android is the web app (`../bjj`) again, in Kotlin. The web app's one rule carries over unchanged: imports only point inward, `ui → bindings → storage → core`, and `libs/core` is the app; everything else is a replaceable shell round it.

## Modules

| Module | What it is | Depends on |
| --- | --- | --- |
| `libs/core` | Models, rules, starter content, `AppStorage`, services, backup format. Pure Kotlin. | serialization, coroutines |
| `libs/core-testing` | The storage contract, `testCore()`, entity builders. | `core` |
| `libs/storage-room` | `AppStorage` over Room. | `core`, Room |
| `libs/design` | Theme, tokens, components, icons, drawings. | `core` |
| `apps/app` | Composition root, navigation, every screen and its ViewModel. | `design`, `core`, `storage-room` (named only in `di/`), Navigation 3, lifecycle ViewModel, Koin |
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
- **`TodaysPath.kt`** has no web file of its name: the web app works out Today's suggested path inside its Home page. Here it is a core function with a test, like every other rule.

Starter content (`seed/Starter*.kt`) and the lessons are generated from the web app by `tools/web-fixtures`. So are the golden values under `libs/core/test/core/golden/`, which the tests must reproduce: the schedule, the teaching order, each lesson's cards, technique links, the map's layout, figure geometry and a backup file the web app wrote.

## Storage

`AppStorage` is nine repositories, `transaction { }`, a `changes` flow and `reset()`. Two engines pass the contract in `core-testing`:

- `MemoryStorage` (in core): for tests, and the reference for a new engine.
- `RoomStorage`: two tables. `rows(tbl, id, json)` holds every stored thing as a document, the same JSON a backup holds; `review_log` holds the history under an auto-incremented id. A new field on a model is not a schema change. Starter content is inserted as the database is created, in the same transaction.

`transaction { }` runs on one writer connection (`useWriterConnection { immediateTransaction { } }`); a transaction inside a transaction joins it, and `changes` emits once when the outermost commits, and only if something in it wrote. A transaction that only read reports nothing: the app reads everything again after each change, so a reported read would never stop.

## From storage to a screen

This is the layering Android's architecture guide recommends, with the dependency between the last two turned round so core stays pure: core declares `AppStorage` and the engine implements it.

| The guide's layer | Here |
| --- | --- |
| UI layer: screens and their state holders | `apps/app/src/ui/<screen>/`: `XRoute`, `XScreen`, `XViewModel` |
| Domain layer: reusable rules | `libs/core`: pure functions and `services/` |
| Data layer: the single source of truth | `AppStorage` (declared in core), `RoomStorage`, and `bindings/AppData` |

```
RoomStorage ──changes──▶ AppData.snapshot: StateFlow<Snapshot?>          one per process
                               │
            XViewModel: xUiState(snapshot, …) ──▶ uiState: StateFlow<XUiState>   one per screen
                               │                         │
              pure functions in core decide       XRoute collects it ──▶ XScreen draws it
```

`AppData` reads a new `Snapshot` after every stored change. Each screen has its own ViewModel, which turns every snapshot into that screen's state with a pure function (`homeUiState`, `positionDetailUiState`, …). That function is where a screen's content is decided, by calling core: which cards are due, what comes next, what a position's two sides can do, whether the item still exists. The state it returns holds values ready to draw, with every lookup done, so a composable never sees the library. `screenState` (`ui/common/ScreenState.kt`) builds the `StateFlow`: it starts from the snapshot already read, so a page opens without a flash of "loading", and stops working five seconds after its page stops showing.

Data flows down as state and up as calls. A screen calls a function on its ViewModel (`grade`, `search`, `saveConcept`); the ViewModel calls one core service; the write changes what is stored; a new snapshot arrives; the state changes. A ViewModel never calls its screen back: when the editor has saved, its state says so (`outcome`), and the route leaves.

What stays in a composable is what only that composable knows: the draft of a form before it is saved, the text in a search box, which side of a position is showing, which map box is selected.

A ViewModel takes only what it uses. The container (`di/AppKoin.kt`) offers each core service by itself, `Clock` for the time and time zone, and `VideoCommands` for the pages that list bookmarks; a page about one thing gets its `NavKey` as a parameter.

**Time.** What is due moves on without anything stored changing. A screen's state reads the clock when it is worked out: on every stored change, and again when a page is come back to after more than five seconds. A page left open across the moment a card comes due shows it once either happens.

## Inside the app module

```
apps/app/src/
  App.kt        the theme, the back stack, the bar; names every screen
  di/           the container: the only other place that names every screen, and the only one that names the engine
  bindings/     AppData (the one snapshot) and Clock
  nav/          the keys (Routes.kt) and Nav, what a page is handed to move with
  platform/     what the host supplies: Links, SystemBars
  ui/
    common/     shared by screens: Page, the row models, pictures, video rows, dates, window helpers
    map/        the position map, a drawing with its own model
    video/      the YouTube player
    <screen>/   backup, editor, home, learn, log, more, review, shell, videos
```

A screen imports `ui/common`, `ui/map`, `ui/video` and itself, never another screen; the shared three never import a screen; `bindings/`, `nav/` and `platform/` import nothing from `ui/` or `di/`. `tools/check_layers.py` fails the build otherwise. This is the isolation a module per feature would give. Modules were not used for it because every screen reads the same snapshot, row models and string resources, so each would hang off one shared base module and gain nothing more.

Each screen is two files named for it, `XScreen.kt` (`XRoute`, `XScreen`) and `XViewModel.kt` (`XUiState`, `xUiState`, `XViewModel`); parts only that package uses sit beside them (`learn/LearnParts.kt`, `home/HomeCards.kt`, `review/CardFace.kt`). Tests mirror the folders under `test@jvm/`.

## Navigation

One back stack of `@Serializable` keys (`nav/Routes.kt`), shown by Navigation 3 in `App.kt`. Today is the root; another tab replaces whatever is above it. A review session is a pushed page, and the bar goes while it is on top. Pages move through the `Nav` they are handed.

The system's Back, the predictive back gesture included, is `NavDisplay`'s: it calls `back()`, which never pops the root, so Back on Today leaves the app. A page going back slides off the one underneath (`pageLeave`), away from the edge the gesture came from, and the gesture scrubs that same animation; each page is given an opaque ground in `App.kt` so the one underneath does not show through it. No page handles Back itself.

## What only Android can do

Each is an `expect` in `apps/app/src` with its `actual` in `src@android` and a stand-in for tests in `src@jvm`, except opening a link outside the app, which is an interface (`platform/Links`) the Android container binds:

- `VideoPlayer`: YouTube's player in a WebView, the one thing that contacts another site. `ItemPicture` holds the player's place on plain paper until the page has arrived and the player says it is ready (`VideoOnPaper`).
- `rememberOnline`: whether to show that player at all.
- `rememberBackupFiles`: the system's file pickers.
- `SystemBars`: the status and navigation icons, kept readable over the app's theme.
- `reducedMotion` (in `design`): the system's "remove animations" setting.

The storage engine, ids, the clock and the time zone are bound in `src@android/di/AppKoin.android.kt`, the one place that names Room.

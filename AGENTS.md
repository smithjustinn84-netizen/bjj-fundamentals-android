# Working in this codebase

Rules for anyone changing this app, human or AI. Most are enforced by the check command below; the rest are on you.

## What this app is

The native Android version of BJJ Fundamentals, an app for learning beginner BJJ concepts with spaced repetition. It is a port of the web app in the sibling folder `../bjj`, which stays the reference for behaviour and starter content. No backend, no accounts, and no network calls except YouTube's player on a study item that has a video. All data is in a Room database on the device and can be exported and imported as the same JSON backup file the web app reads and writes.

`docs/parity.md` lists every place this app deliberately differs from the web app. Add to it when you add a difference.

## Build

This project uses the **Kotlin Toolchain (Amper)** through the `./kotlin` wrapper. **Never run or suggest Gradle**: there is no `gradlew` and no `build.gradle.kts`.

```sh
./kotlin build                 # everything
./kotlin test -m core -p jvm   # one module's tests, fast
./kotlin check lint detekt     # ktlint and detekt
./kotlin do format             # ktlint's auto-fixes
```

- A module's code is in `src/` (shared), `src@android/` and `src@jvm/`; tests in `test/` and `test@jvm/`. **Never create `src/commonMain/kotlin/`**: Amper silently ignores it.
- `src/` is the root package `dev.justinsmith.bjj`, so `libs/core/src/core/Srs.kt` is `dev.justinsmith.bjj.core`.
- The `jvm` platform exists so tests run on the desktop JVM; there is no desktop app. Screen tests (`runComposeUiTest`) and Room tests live in `test@jvm/` only, because `test/` also runs as Android local tests, which have no Compose host and cannot load the bundled SQLite.
- Versions are in `libs.versions.toml` and follow the sibling project `interval`. Compose is versioned by the toolchain (`$compose.*`).
- Warnings are errors. Opt in per file (`@file:OptIn`), never module-wide.
- To run it, use the `run-on-device` skill (`.claude/skills/run-on-device`).

## The one architectural rule

Imports only point inward: `ui → bindings → storage → core`. **`libs/core` is the app; everything else is a replaceable shell around it.**

| Module | Contains | Must not contain |
| --- | --- | --- |
| `libs/core` | Data models, spaced-repetition logic, queue building, validation, map layout, figure geometry, starter content, the storage interface (`AppStorage`) and all app logic (`services/`). | Anything from Android, Compose, Koin or the JVM's own library (`java.*`): no `System.currentTimeMillis()`, no `UUID`, no `java.time`. |
| `libs/core-testing` | The storage contract every engine must pass, `testCore()`, entity builders. | Anything core may not contain. |
| `libs/storage-room` | The implementation of `AppStorage` over Room. The only module that imports `androidx.room`. | App rules, Compose, UI text. |
| `libs/design` | Theme, tokens, components, icons, drawings. Draws what core calculates. | Storage, ViewModels, navigation, Koin. |
| `apps/app` | `di/` (the composition root: picks the engine, builds the core), `bindings/` (the shared snapshot screens read), `nav/`, `ui/<screen>/`. | Business rules. The storage engine anywhere but `di/`. |
| `apps/androidApp` | Manifest, resources, `MainActivity`, the application class. | Code that could live in `app`. |

`tools/check_layers.py` enforces the import rules.

Where does new code go?

- A rule or calculation ("when is a card due", "is this link valid") → a pure function in `core`, taking `now` as a parameter, with a test.
- Anything that reads or writes stored data → a method on a service in `core/services/`, written against `AppStorage`. It gets ids, time and the time zone from `CoreEnv`, never from globals.
- Something the storage engine itself must do → add it to `AppStorage`, implement it in **every** engine (`MemoryStorage`, the Room engine), and add a case to the storage contract in `core-testing`. Do this rarely: prefer composing existing methods in a service.
- Anything the user sees or taps → `apps/app/src/ui/`, built from `design` components.
- Anything only Android can do (a WebView, a file picker, connectivity) → an `expect` composable or an injected interface, with the `actual` in `src@android/` and a stand-in in `src@jvm/` for tests.

## Hard constraints

- **No network calls, with one exception.** The only thing that may contact another site is YouTube's player in `ui/video/VideoPlayer` (on `youtube-nocookie.com`, address built by `embedUrl` in core), shown for a study item's bookmarked video on its page and its review card. No other WebView and no player anywhere else without asking the user; everywhere else YouTube bookmarks open outside the app. The manifest's only permissions are `INTERNET` and `ACCESS_NETWORK_STATE`, both for this.
- **Core stays pure.** If `check_layers.py` rejects something in core, inject it through `CoreEnv` or move the code out.
- **Behaviour matches the web app.** Every rounding in core goes through `jsRound` (a half rounds up), never `kotlin.math.round`. Golden values generated from the web app (`tools/web-fixtures`) are the test of this; when they disagree with the Kotlin, the Kotlin is wrong.
- **The backup file is shared with the web app.** Same shape, same `BACKUP_SCHEMA_VERSION`. An absent optional field is left out, never written as `null` (the web app rejects `null`). Imported backups are untrusted input: every stored field has a check.
- **No new dependencies without asking the user.** Say what it is for and what it costs in app size.
- **No `!!`, no unchecked casts without a comment saying why they are safe, no `@Suppress` without a reason** on the same line. Never weaken `detekt.yml` or `.editorconfig` to make a finding go away.
- **Never edit a committed Room schema** (`libs/storage-room/schemas/`). A model field is not a schema change: rows are stored whole as JSON documents.
- **All persistence goes through `AppStorage`** so it is covered by backup. No SharedPreferences, no DataStore, no files.

## Conventions

- An enum is the list of its values; rules about a value are functions beside it (`endsTheRoll(type)`, not `type == Submission`). Every `when` over an enum or sealed type is exhaustive with no `else`, so a new member fails to compile until handled.
- Each screen is `XRoute` (resolves the ViewModel, collects state) and a stateless `XScreen`. Pages that only show the library share one `AppViewModel` and start with `WithSnapshot`, which hands them the one `Snapshot` every screen reads (`bindings/AppData`); they work out what to show with pure core functions. A page with a form or a session of its own (review, the editor, the log, backup) has its own ViewModel exposing a `StateFlow`. ViewModels hold no rules of their own.
- A page moves to another through the `Nav` it is handed, never the back stack itself.
- Every destination is a `@Serializable` `NavKey` in `nav/Routes.kt`, registered in `navConfiguration` and given an `entry<…>` (both in `App.kt`). The bar has five tabs, the same on every window size.
- No raw `dp`, `sp`, colour or duration in a screen: they come from `design/theme` (`Spacing`, `Radius`, `Sizes`, `TextSizes`, `Motion`, `BjjTheme.palette`).
- Screen text is in string resources: `apps/app/composeResources/values/`, one file per area (`strings.xml` for the shell and shared words, then `strings_<area>.xml` with names prefixed `<area>_`), read with `stringResource(Res.string.x)`. Counts are `<plurals>`, never core's `plural`. A ViewModel exposes an enum, a sealed type or a `StringResource`, and the screen resolves it. Write `'` and `"` plainly in the XML and a single `%` for a percent sign: this toolchain does not unescape `\'` or `%%`. Still in code: labels that core's own logic uses (search, card prompts, `core/Labels.kt`), core's formatters (`formatMatTime`, `describeInterval`) and the weekday and month names in `ui/home/TodayLabel.kt`.
- Services set `id`, `createdAt`, `updatedAt`. Callers pass an `...Input` type without them. Multi-step writes go in one `storage.transaction`.
- No magic numbers: name the constant and say what it means. Comments explain why, not what. No commented-out code.
- Tests use `kotlin.test` with hand-written fakes and `testCore()`; no mocking library. Test names are backticked sentences without commas.

## Look and motion

- A woodblock print: flat colour, an ink line round every card, picture and control, and a flat ink edge underneath where another app would put a soft shadow. No gradients, no blurred shadows, no Material components, no dynamic colour. A button sinks onto its edge when pressed. Each screen has at most one call-to-action button (ochre).
- The theme is the user's choice (Light, Dark or Device) from the `settings` table, resolved by `resolveTheme` in core.
- Drawings share one sheet of paper: they use only the art palette, which has one value for both themes; the dark theme dims it as a whole.
- People in diagrams are `Figure`s posed by joint positions; a scene is poses and a draw order, not hand-drawn body shapes. Anything a drawing has to calculate is a pure function in core with a test; composables only draw the result.
- Anything tapped is at least `Sizes.tap` tall. Layout changes at the widths in `Breakpoints` only: use `Columns` and `SideBySide` (`ui/common/Window.kt`) so a list or a pair of cards follows the window.
- Motion respects the system's "remove animations" setting.

## Keeping in step with the web app

Starter content, lessons, technique chains and the backup schema version live in both apps. After the web app changes any of them, rerun `tools/web-fixtures` (see its README): the regenerated golden values fail this app's tests until the Kotlin follows.

## Version control

This repo uses [Jujutsu](https://jj-vcs.github.io/jj/) (`jj`) on a git backend, colocated. Use `jj`, not `git`, for anything that changes history.

- The working copy is always a change; there is no staging area. `jj st` and `jj diff` show it.
- Finish a change with `jj commit -m "..."`. `main` is a bookmark and does not move by itself: `jj bookmark move main --to @-`.
- jj does not run git hooks. Run the check command before every `jj commit`.
- Commit or push only when the user asks.

## Definition of done

1. The check passes. Run it; do not assume:
   ```sh
   ./kotlin check lint detekt && python3 tools/check_layers.py && ./kotlin build && ./kotlin test
   ```
   CI (`.github/workflows/ci.yml`) runs the same stages. If you change one, change the other.
2. New or changed logic in `core` or `storage-room` has a test that would fail without the change.
3. UI changes were exercised on the emulator (`run-on-device`), including the empty and error states, as a phone and as a tablet, in light and dark.
4. Nothing left behind: no unused code, dead branches, stray logs, or files that only existed for debugging.
5. If you changed a rule or the structure, this file and `docs/architecture.md` say so; if you added a difference from the web app, `docs/parity.md` says so.

If a rule here blocks something the user asked for, stop and say so instead of working around it.

# Testing

```sh
./kotlin test                    # everything
./kotlin test -m core -p jvm     # one module, fast
```

## What is tested where

| Module | Tests | Where |
| --- | --- | --- |
| `core` | Every rule, the services over `MemoryStorage`, the backup format, and the golden values from the web app. | `libs/core/test/` |
| `storage-room` | The storage contract over a real database file, including a restart. | `libs/storage-room/test@jvm/` |
| `design` | Every drawing composes and draws in both themes; the palette and view-box maths. | `libs/design/test@jvm/` |
| `app` | The ViewModels over a real core, the container, navigation helpers, dates in words. | `apps/app/test@jvm/` |

`test/` runs on both platforms; `test@jvm/` on the desktop JVM only. Anything that needs a Compose host or the bundled SQLite goes in `test@jvm/`.

## Conventions

- `kotlin.test`, hand-written fakes, no mocking library. `testCore()` gives a core on in-memory storage with a clock the test moves and ids `id-1`, `id-2`, ….
- Test names are backticked sentences without commas.
- A new storage engine extends `StorageContract`; when that passes, the app runs on it.
- Golden values are generated, never edited. When one disagrees with the Kotlin, the Kotlin is wrong. Regenerate them with `tools/web-fixtures` (see its README) after the web app changes.

## Static checks

`tools/check.sh` runs all of these and then the build and the tests; `--fast` stops after the static ones.

| Check | Catches |
| --- | --- |
| actionlint, zizmor | Mistakes in the workflows and unsafe patterns in them: an action not pinned to a SHA, a token left in the checkout. |
| `tools/check_layers.py` | An import that points outward: core reaching for Android or the JVM, a screen naming the storage engine. |
| `tools/check_strings.py` | English written into a screen; a string that is unused, duplicated or in the wrong file; escapes and placeholders the resources library does not handle; plural forms that disagree. |
| `./kotlin check lint` | ktlint formatting. |
| `./kotlin check detekt` | detekt and the Compose rules. |

Both scripts have a `--self-test` that CI runs first, so a broken checker cannot pass silently.

## What is not automated

- Screens are exercised by hand on the emulator (the `run-on-device` skill): as a phone and a tablet, in light and dark, online and in airplane mode.
- Video playback, the file pickers and process death can only be checked on a device.
- A backup exported from the device can be checked against the web app's own validator by reading it with `validateBackup` from `../bjj/src/core/backup.ts`.

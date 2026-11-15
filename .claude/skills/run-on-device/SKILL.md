---
name: run-on-device
description: Build, install and launch the app on the amper-37 Android emulator (resizable: phone, foldable, tablet, desktop), and inspect its UI there.
---

```sh
./kotlin build -m androidApp
android emulator start amper-37
android run --apks=build/tasks/_androidApp_buildAndroidDebug/gradle-project-debug.apk
android layout -p                   # UI tree as JSON, faster than a screenshot
android docs search "<topic>"       # Official Android docs
```

To check a page survives process death: Home, then `adb shell am kill dev.justinsmith.bjj` (never
`force-stop`, which drops saved state), and reopen through the launcher intent (`adb shell monkey -p
dev.justinsmith.bjj -c android.intent.category.LAUNCHER 1`, the icon or Recents). `am start -n` stacks a
fresh `MainActivity` over the restored one.

`amper-37` is a **resizable** emulator on API 37 (the app's target SDK): one device for every form factor.
It boots as a phone; switch with `adb -s <serial> emu resize-display <mode>`:

| mode | form factor | pixels | dp |
|---|---|---|---|
| 0 | phone | 1080×2400 @ 420 | 411×914 |
| 1 | foldable, unfolded | 2208×1840 @ 420 | 841×701 |
| 2 | tablet | 1920×1200 @ 240 | 1280×800 |
| 3 | desktop | 1920×1080 @ 160 | 1920×1080 |

Check the rail and two-column layouts in modes 1 and 2, and put it back to 0 for phone checks. It has two
displays, so a plain `screencap` writes a "[Warning] Multiple displays…" text file instead of a PNG: capture
`adb -s <serial> exec-out screencap -p -d <id> > shot.png` with the id of `EMU_display_0` from
`adb shell dumpsys SurfaceFlinger --display-id`.

Dark theme: `adb shell cmd uimode night yes` (and `no`). Offline: `adb shell cmd connectivity airplane-mode
enable` (and `disable`), which is how to check a video falls back to its drawing.

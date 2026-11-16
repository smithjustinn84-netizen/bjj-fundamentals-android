#!/usr/bin/env python3
"""Imports only point inward: ui -> bindings -> storage -> core.

The module graph already stops most of it (core cannot see Compose because it does not depend on it). This
catches what the graph cannot: core reaching for the JVM's own library or Android, anything but the
composition root naming the storage engine, and one screen of the app reaching into another.

    python3 tools/check_layers.py              check the repository
    python3 tools/check_layers.py --self-test  check the checker
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
IMPORT = re.compile(r"^import\s+([\w.]+)", re.MULTILINE)

# What core may not import: it runs anywhere Kotlin does, and takes time, ids and storage through CoreEnv.
CORE_FORBIDDEN = ("android.", "androidx.", "java.", "javax.", "org.koin.", "dev.justinsmith.bjj.storage.")
# What only the composition root (apps/app/src*/di/) may import: the engine, and the library it is built on.
ENGINE = ("dev.justinsmith.bjj.storage.", "androidx.room.", "androidx.sqlite.")
# What the design system may not import: it draws what core calculates and knows nothing of screens or storage.
DESIGN_FORBIDDEN = ENGINE + ("org.koin.", "androidx.navigation3.", "androidx.lifecycle.viewmodel.")

APP = "dev.justinsmith.bjj."
# The packages under apps/app's ui/ that any screen may use. Every other package there is a screen, which
# imports these and itself and never another screen: what two screens need goes in one of these. Adding a
# name here is a decision, not a way round the check.
SHARED_UI = ("common", "map", "video")
# What the app's plumbing is, below every screen: it may not import a screen or the container.
BELOW_UI = ("bindings", "nav", "platform")


def source_dirs(module: Path) -> list[Path]:
    """A module's source and test folders: src, src@android, test, test@jvm, ..."""
    return sorted(p for p in module.iterdir() if p.is_dir() and re.fullmatch(r"(src|test)(@\w+)?", p.name))


def rule_for(path: Path) -> tuple[tuple[str, ...], str] | None:
    """The imports forbidden in the file at [path] (relative to the repository), and why."""
    parts = path.parts
    if parts[:2] == ("libs", "core") or parts[:2] == ("libs", "core-testing"):
        return CORE_FORBIDDEN, "core is pure Kotlin: inject it through CoreEnv or move the code out"
    if parts[:2] == ("libs", "design"):
        return DESIGN_FORBIDDEN, "the design system knows nothing of storage, screens or the container"
    if parts[:2] == ("apps", "app") and "di" not in parts[3:-1]:
        return ENGINE, "only the composition root (di/) names the storage engine"
    if parts[:2] == ("apps", "androidApp"):
        return ENGINE, "the host starts the app; the composition root picks the engine"
    return None


def app_rule(path: Path, name: str) -> str | None:
    """Why the file at [path] in apps/app may not import [name], if it may not. Tests may cross screens."""
    parts = path.parts
    if parts[:2] != ("apps", "app") or not parts[2].startswith("src") or not name.startswith(APP):
        return None
    here = parts[3:-1]
    there = tuple(name[len(APP):].split("."))
    if there[0] == "di" and here[:1] != ("di",) and here:
        return "only the composition root (di/) and App.kt reach into the container"
    if here[:1] == ("ui",) and len(here) > 1 and there[0] == "ui":
        mine, theirs = here[1], there[1]
        if theirs != mine and theirs not in SHARED_UI:
            if mine in SHARED_UI:
                return f"ui/{mine} is shared by every screen and may not depend on one"
            return f"a screen never imports another: what both need goes in ui/{SHARED_UI[0]}"
    if here[:1] and here[0] in BELOW_UI and there[0] == "ui":
        return f"{here[0]}/ is below the screens and may not import one"
    return None


def violations(path: Path, text: str) -> list[str]:
    names = IMPORT.findall(text)
    found = [f"{path}: imports {name} ({why})" for name in names if (why := app_rule(path, name))]
    rule = rule_for(path)
    if rule is None:
        return found
    forbidden, why = rule
    return found + [
        f"{path}: imports {name} ({why})"
        for name in names
        if any((name + ".").startswith(prefix) for prefix in forbidden)
    ]


def check() -> list[str]:
    found: list[str] = []
    for group in ("apps", "libs"):
        for module in sorted((ROOT / group).iterdir()):
            if not (module / "module.yaml").exists():
                continue
            for folder in source_dirs(module):
                for file in sorted(folder.rglob("*.kt")):
                    found += violations(file.relative_to(ROOT), file.read_text(encoding="utf-8"))
    return found


def self_test() -> None:
    def bad(path: str, line: str) -> bool:
        return bool(violations(Path(path), line + "\n"))

    assert bad("libs/core/src/core/Srs.kt", "import java.time.LocalDate")
    assert bad("libs/core/src/core/Srs.kt", "import androidx.compose.runtime.Composable")
    assert bad("libs/core/test/core/SrsTest.kt", "import java.io.File")
    assert not bad("libs/core/src/core/Srs.kt", "import kotlinx.coroutines.flow.Flow")
    assert not bad("libs/core/src/core/Srs.kt", "import kotlin.math.floor")
    assert bad("libs/design/src/design/Card.kt", "import androidx.room.Room")
    assert bad("libs/design/src/design/Card.kt", "import org.koin.compose.koinInject")
    assert not bad("libs/design/src/design/Card.kt", "import dev.justinsmith.bjj.core.FigurePose")
    assert bad("apps/app/src/ui/home/HomeViewModel.kt", "import dev.justinsmith.bjj.storage.roomStorage")
    assert bad("apps/app/src/App.kt", "import androidx.room.Room")
    assert not bad("apps/app/src/di/AppKoin.kt", "import dev.justinsmith.bjj.storage.roomStorage")
    assert not bad("apps/app/src@android/di/AppKoin.android.kt", "import androidx.room.Room")
    assert bad("apps/androidApp/src/MainActivity.kt", "import androidx.room.Room")
    assert not bad("libs/storage-room/src/storage/BjjDatabase.kt", "import androidx.room.Room")
    assert bad("apps/app/src/ui/log/LogScreen.kt", "import dev.justinsmith.bjj.ui.home.HomeScreen")
    assert bad("apps/app/src@android/ui/backup/BackupFiles.android.kt", "import dev.justinsmith.bjj.ui.more.MoreScreen")
    assert not bad("apps/app/src/ui/log/LogScreen.kt", "import dev.justinsmith.bjj.ui.log.LogUiState")
    assert not bad("apps/app/src/ui/log/LogScreen.kt", "import dev.justinsmith.bjj.ui.common.Page")
    assert not bad("apps/app/src/ui/learn/PositionsScreen.kt", "import dev.justinsmith.bjj.ui.map.PositionMap")
    assert bad("apps/app/src/ui/common/Page.kt", "import dev.justinsmith.bjj.ui.learn.LearnSwitch")
    assert not bad("apps/app/src/ui/common/Pictures.kt", "import dev.justinsmith.bjj.ui.video.VideoPlayer")
    assert bad("apps/app/src/nav/Nav.kt", "import dev.justinsmith.bjj.ui.common.Page")
    assert bad("apps/app/src/bindings/AppData.kt", "import dev.justinsmith.bjj.di.appModules")
    assert bad("apps/app/src/ui/home/HomeViewModel.kt", "import dev.justinsmith.bjj.di.appModules")
    assert not bad("apps/app/src/di/AppKoin.kt", "import dev.justinsmith.bjj.ui.home.HomeViewModel")
    assert not bad("apps/app/src/App.kt", "import dev.justinsmith.bjj.ui.home.HomeRoute")
    assert not bad("apps/app/src/App.kt", "import dev.justinsmith.bjj.di.appModules")
    assert not bad("apps/app/test@jvm/ui/log/LogViewModelTest.kt", "import dev.justinsmith.bjj.ui.home.HomeUiState")
    print("check_layers: self-test passed")


def main() -> int:
    if "--self-test" in sys.argv[1:]:
        self_test()
        return 0
    found = check()
    for line in found:
        print(line)
    if found:
        print(f"check_layers: {len(found)} import(s) point the wrong way")
        return 1
    print("check_layers: imports only point inward")
    return 0


if __name__ == "__main__":
    sys.exit(main())

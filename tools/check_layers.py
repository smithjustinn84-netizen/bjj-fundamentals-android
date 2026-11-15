#!/usr/bin/env python3
"""Imports only point inward: ui -> bindings -> storage -> core.

The module graph already stops most of it (core cannot see Compose because it does not depend on it). This
catches what the graph cannot: core reaching for the JVM's own library or Android, and anything but the
composition root naming the storage engine.

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


def violations(path: Path, text: str) -> list[str]:
    rule = rule_for(path)
    if rule is None:
        return []
    forbidden, why = rule
    return [
        f"{path}: imports {name} ({why})"
        for name in IMPORT.findall(text)
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

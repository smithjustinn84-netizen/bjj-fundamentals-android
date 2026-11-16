#!/usr/bin/env python3
"""Screen text lives in string resources, and the resources are sound.

The compiler checks that a name used in a screen exists. This catches what it cannot:

- a name defined twice, or in the wrong file for its prefix;
- a string no screen uses any more;
- escapes this toolchain shows as written (`\\'`, `\\"`, `%%`) and placeholders it does not fill (`%s`, `%d`);
- plural forms that take different arguments;
- English written straight into a screen.

    python3 tools/check_strings.py              check the repository
    python3 tools/check_strings.py --self-test  check the checker
"""

from __future__ import annotations

import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
VALUES = ROOT / "apps" / "app" / "composeResources" / "values"
SCREENS = ROOT / "apps" / "app"

# strings.xml holds the shell's text and words several areas share; strings_<area>.xml holds `<area>_` names.
SHARED_PREFIXES = ("app_", "tab_", "common_")
# Compose resources fills `%1$s` and `%1$d` and nothing else, and unescapes neither quotes nor `%%`.
PLACEHOLDER = re.compile(r"%(\d+)\$[sd]")
ANY_PERCENT_ARG = re.compile(r"%(?!\d+\$[sd])[\d$.]*[a-zA-Z]")
SHOWN_AS_WRITTEN = ("\\'", '\\"', "%%")

# A literal with two words in a row is prose. One word is too often a key, a tag or a format to judge.
PROSE = re.compile(r'"(?:[^"\\\n]|\\.)*?[A-Za-z]{2,} [A-Za-z]{2,}(?:[^"\\\n]|\\.)*?"')
# Text that is not for the user: what a failed check says, what a log says, and what a test finds a node by.
NOT_SHOWN = re.compile(r"\b(require|check|error|requireNotNull|checkNotNull|testTag|Logger\.\w+|TODO)\s*\(")
# A line may keep its text in code if it says why, as a lint suppression must.
ALLOWED = re.compile(r"//\s*text-in-code:\s*\S")


def resource_problems(files: dict[str, str]) -> tuple[list[str], set[str]]:
    """Problems in the XML [files] (name -> content), and every resource name they define."""
    problems: list[str] = []
    seen: dict[str, str] = {}
    for filename, content in sorted(files.items()):
        area = re.fullmatch(r"strings(?:_(\w+))?\.xml", filename)
        if not area:
            problems.append(f"{filename}: name it strings.xml or strings_<area>.xml")
            continue
        prefixes = (f"{area.group(1)}_",) if area.group(1) else SHARED_PREFIXES
        try:
            root = ET.fromstring(content)
        except ET.ParseError as e:
            problems.append(f"{filename}: not valid XML ({e})")
            continue
        for node in root:
            name = node.get("name", "")
            where = f"{filename}: {name}"
            if name in seen:
                problems.append(f"{where}: already defined in {seen[name]}")
            seen[name] = filename
            if not name.startswith(prefixes):
                problems.append(f"{where}: names here start with {' or '.join(prefixes)}")
            texts = [node.text or ""] if node.tag == "string" else [item.text or "" for item in node]
            for text in texts:
                for bad in SHOWN_AS_WRITTEN:
                    if bad in text:
                        problems.append(f"{where}: `{bad}` is shown as written; write the character plainly")
                if ANY_PERCENT_ARG.search(text):
                    problems.append(f"{where}: placeholders are numbered, `%1$s` or `%1$d`")
            if node.tag == "plurals" and len({frozenset(PLACEHOLDER.findall(t)) for t in texts}) > 1:
                problems.append(f"{where}: every plural form takes the same arguments")
    return problems, set(seen)


def unused(names: set[str], sources: dict[str, str]) -> list[str]:
    """The [names] no Kotlin source mentions."""
    words = set(re.findall(r"\w+", "\n".join(sources.values())))
    return sorted(names - words)


def strip_comment(line: str) -> str:
    """[line] without a trailing `//` comment (a `//` inside a string literal stays)."""
    in_string = False
    for i, ch in enumerate(line):
        if ch == '"' and (i == 0 or line[i - 1] != "\\"):
            in_string = not in_string
        elif not in_string and line.startswith("//", i):
            return line[:i]
    return line


def prose_in_code(path: str, source: str) -> list[str]:
    """Lines of [source] that write English for the user straight into the code."""
    found: list[str] = []
    in_block_comment = False
    for number, line in enumerate(source.splitlines(), start=1):
        stripped = line.strip()
        if in_block_comment or stripped.startswith(("/*", "*")):
            in_block_comment = "*/" not in stripped and (in_block_comment or stripped.startswith("/*"))
            continue
        if stripped.startswith(("//", "import ", "package ", "@")) or ALLOWED.search(line):
            continue
        code = strip_comment(line)
        if PROSE.search(code) and not NOT_SHOWN.search(code):
            found.append(f"{path}:{number}: text for the user belongs in composeResources/values: {stripped}")
    return found


def is_screen(path: Path) -> bool:
    """Whether [path] (relative to apps/app) is screen code: a source folder's `ui/` tree or App.kt."""
    parts = path.parts
    return bool(re.fullmatch(r"src(@\w+)?", parts[0])) and (parts[1] == "ui" or path.name == "App.kt")


def check() -> list[str]:
    files = {p.name: p.read_text() for p in VALUES.glob("*.xml")}
    problems, names = resource_problems(files)
    kotlin = {str(p.relative_to(ROOT)): p.read_text() for p in SCREENS.rglob("*.kt") if "build" not in p.parts}
    problems += [f"{name}: no screen uses it; delete it" for name in unused(names, kotlin)]
    for path, source in sorted(kotlin.items()):
        if is_screen(Path(path).relative_to("apps/app")):
            problems += prose_in_code(path, source)
    return problems


def self_test() -> list[str]:
    failures: list[str] = []

    def expect(what: str, got: object, want: object) -> None:
        if got != want:
            failures.append(f"{what}: expected {want!r}, got {got!r}")

    def count(files: dict[str, str]) -> int:
        return len(resource_problems(files)[0])

    def xml(body: str) -> str:
        return f"<resources>{body}</resources>"

    expect("a sound file", count({"strings_log.xml": xml('<string name="log_title">Log %1$d</string>')}), 0)
    expect("a wrong prefix", count({"strings_log.xml": xml('<string name="more_title">Log</string>')}), 1)
    expect("a shared name", count({"strings.xml": xml('<string name="common_ok">OK</string>')}), 0)
    expect(
        "a name twice",
        count({
            "strings_a.xml": xml('<string name="a_x">X</string>'),
            "strings_b.xml": xml('<string name="a_x">X</string>'),
        }),
        2,
    )
    expect("an escaped quote", count({"strings_a.xml": xml("<string name=\"a_x\">It\\'s</string>")}), 1)
    expect("a doubled percent", count({"strings_a.xml": xml('<string name="a_x">5%% more</string>')}), 1)
    expect("a plain percent", count({"strings_a.xml": xml('<string name="a_x">5% more</string>')}), 0)
    expect("a bare placeholder", count({"strings_a.xml": xml('<string name="a_x">%d cards</string>')}), 1)
    plural = '<plurals name="a_n"><item quantity="one">%1$d card</item><item quantity="other">{}</item></plurals>'
    expect("plural forms that agree", count({"strings_a.xml": xml(plural.format("%1$d cards"))}), 0)
    expect("plural forms that differ", count({"strings_a.xml": xml(plural.format("cards"))}), 1)
    expect("a bad file name", count({"text.xml": xml("")}), 1)

    expect("an unused name", unused({"a_x", "a_y"}, {"A.kt": "stringResource(Res.string.a_x)"}), ["a_y"])

    def prose(source: str) -> int:
        return len(prose_in_code("A.kt", source))

    expect("prose in a screen", prose('BjjText("Start the review")'), 1)
    expect("one word", prose('BjjText("Review")'), 0)
    expect("a failed check's message", prose('require(n > 0) { "Must be positive" }'), 0)
    expect("a comment", prose('// "Start the review" was here'), 0)
    expect("a trailing comment", prose('val a = 1 // "Start the review"'), 0)
    expect("a doc comment", prose('/**\n * Says "Start the review".\n */\nval a = 1'), 0)
    expect("text with a reason", prose('val day = "Start the review" // text-in-code: shown only in a preview'), 0)
    expect("text with no reason", prose('val day = "Start the review" // text-in-code:'), 1)
    expect("a screen", is_screen(Path("src/ui/home/HomeScreen.kt")), True)
    expect("a platform screen", is_screen(Path("src@android/ui/video/VideoPlayer.android.kt")), True)
    expect("the container", is_screen(Path("src/di/AppKoin.kt")), False)
    expect("a test", is_screen(Path("test@jvm/ui/ViewModelsTest.kt")), False)
    return failures


def main() -> int:
    testing = "--self-test" in sys.argv[1:]
    problems = self_test() if testing else check()
    for problem in problems:
        print(problem)
    if problems:
        print(f"\ncheck_strings: {len(problems)} problem(s)")
        return 1
    print("check_strings: the checker works" if testing else "check_strings: screen text is in sound resources")
    return 0


if __name__ == "__main__":
    sys.exit(main())

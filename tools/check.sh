#!/bin/sh
# Every check CI runs, in CI's order, stopping at the first that fails. jj runs no hooks, so run this before
# `jj commit` and before `jj git push`. The stages are .github/workflows/ci.yml's: change one, change the other.
#
#     tools/check.sh          check everything
#     tools/check.sh --fast   skip the build and the tests (the static checks only)
set -eu
cd "$(dirname "$0")/.."

stage() { printf '\n== %s\n' "$1"; }

# The workflow linters are not part of the toolchain. CI always runs them (pinned: actionlint 1.7.12,
# zizmor 1.32.0); here they run when installed, and say so when not.
stage "workflows"
if command -v actionlint >/dev/null; then actionlint; else echo "actionlint not installed: skipped (CI runs it)"; fi
if command -v zizmor >/dev/null; then
    zizmor --offline .
elif command -v uvx >/dev/null; then
    uvx zizmor==1.32.0 --offline .
else
    echo "zizmor not installed: skipped (CI runs it)"
fi

stage "layers"
python3 tools/check_layers.py --self-test
python3 tools/check_layers.py

stage "strings"
python3 tools/check_strings.py --self-test
python3 tools/check_strings.py

stage "lint, detekt"
./kotlin check lint detekt

if [ "${1:-}" = "--fast" ]; then
    printf '\nStatic checks passed. The build and the tests were skipped.\n'
    exit 0
fi

stage "build"
./kotlin build

stage "test"
./kotlin test

printf '\nAll checks passed.\n'

# ADR-0003: One app module with checked screen boundaries, not a module per feature

**Status:** Accepted
**Date:** 2026-11-15 (made in the initial port; recorded 2026-11-19)
**Deciders:** Maintainer

## Context

The app has nine screen packages (backup, editor, home, learn, log, more, review, shell, videos) and three shared ones (`common`, `map`, `video`). A common way to keep screens apart in a larger Android app is a module per feature, so the compiler rejects one feature reaching into another.

Here every screen reads the same thing: the one `Snapshot` from `bindings/AppData`, the row models in `ui/common/Models.kt`, the `design` components and one set of string resources in `apps/app/composeResources/`. The build is Amper, and each module adds its own configuration, platforms and test setup.

## Decision

`apps/app` stays one module. Its packages are kept apart by `tools/check_layers.py`, which runs in `tools/check.sh` and CI:

- Under `ui/`, a screen imports `common`, `map`, `video` and itself, never another screen.
- The shared packages never import a screen.
- `bindings/`, `nav/` and `platform/` import nothing from `ui/` or `di/`.
- Only `di/` and `App.kt` name every screen.

What two screens need moves to `ui/common`; what one needs stays in its package.

## Options considered

### Option A: one module, boundaries checked by a script (chosen)

| Dimension | Assessment |
| --- | --- |
| Isolation | The same screen-to-screen rule a module split gives, failing the check rather than the compiler. |
| Build setup | One module's configuration, one resource set, one test setup. |
| Build time | No module-level caching or parallelism between screens. Small at this size. |
| Moving code | Moving a part between a screen and `common` is a file move. |

**Pros:** no base module to invent; strings stay in one place, which `check_strings.py` already checks; the layering rules for the rest of the app live in the same checker.
**Cons:** the boundary is enforced by a script, not the compiler, so an IDE will offer a cross-screen import before the check rejects it; no incremental build win per feature.

### Option B: a module per screen

| Dimension | Assessment |
| --- | --- |
| Isolation | Enforced by the compiler. |
| Build setup | Ten or more modules, each with its own configuration, platforms and tests. |
| Build time | Better incremental builds once there are many screens and many contributors. |
| Moving code | Moving a part to shared code means moving it across modules and their resources. |

**Pros:** isolation that cannot be bypassed; parallel and cached builds per feature.
**Cons:** every feature module would depend on one base module holding `Snapshot`, the row models, the `ui/common` parts and every string. The base would change with nearly every change, so the split isolates nothing the checker does not already, while every screen pays for a module.

## Trade-off analysis

A module per feature pays for itself when features are owned separately, depend on different things and are built often by many people. None of those holds: one maintainer, and every screen depends on the same snapshot and resources. The isolation is the part worth having, and the checker gives it at the cost of one script. The build-time gain is the part this app would not see at its size.

## Consequences

- Easier: adding a screen (a package, its two files, a key and an entry); sharing a part (move it to `common`); keeping strings and their checks in one place.
- Harder: nothing stops a cross-screen import until the check runs, so the check runs before every commit.
- Rules that follow from it: one screen per package and one screen per file, as AGENTS.md describes; a part used by one screen does not go in `common`.
- Revisit if: screens stop sharing one snapshot, the app grows enough that build times hurt, or parts of it are owned by different people.

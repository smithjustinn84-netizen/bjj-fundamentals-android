# ADR-0002: Core declares `AppStorage`; storage engines implement it

**Status:** Accepted
**Date:** 2026-11-15 (made in the initial port; recorded 2026-11-19)
**Deciders:** Maintainer

## Context

The web app's one rule is that imports point inward, `ui → bindings → storage → core`, and that core is the app: everything else is a replaceable shell. The port keeps that rule.

Android's architecture guide layers an app as UI, domain and data, with the domain layer depending on the data layer. Taken literally, core (the domain) would import the Room engine, and the rules that matter most (scheduling, queues, backup checks) could only be tested with a database behind them, on Android or with Room's bundled SQLite.

Core must also stay pure Kotlin: no Android, no Compose, no Koin and no `java.*`, so its golden tests against the web app run in seconds on the JVM.

## Decision

Core declares what it needs from storage, `AppStorage`: nine repositories, `transaction { }`, a `changes` flow and `reset()`. Every service in `core/services/` is written against `AppStorage` and gets its ids, clock and time zone from `CoreEnv`. Engines implement the interface: `MemoryStorage` in core (for tests, and the reference) and `RoomStorage` in `libs/storage-room`, the only module that imports Room. Both must pass the storage contract in `core-testing`.

The engine is chosen in one place, the container in `apps/app/src/di/`. `tools/check_layers.py` fails the build if anything else names it, or if core imports anything it may not.

## Options considered

### Option A: core owns the interface (chosen)

| Dimension | Assessment |
| --- | --- |
| Test speed | Core and services run on `MemoryStorage` through `testCore()`: no database, no Android. |
| Swapping the engine | A new class that passes the contract, and one line in `di/`. |
| Purity | Core imports nothing below it, which the checker enforces. |
| Cost | Every engine capability is written twice (memory and Room) and tested once (the contract). |

**Pros:** the rules are tested at full speed and in isolation; the engine cannot leak into core; the contract states what "correct storage" means, including the rule that a transaction that only read reports no change.
**Cons:** adding something to `AppStorage` means implementing it in every engine and adding a contract case, so the interface is grown rarely and services compose existing methods instead.

### Option B: the guide's layering as drawn, core depends on the data layer

| Dimension | Assessment |
| --- | --- |
| Test speed | Every service test needs Room, or a mock of Room's DAOs. |
| Swapping the engine | Touches every service. |
| Purity | Core would carry Room and its Android dependencies. |
| Cost | One implementation, no contract to keep. |

**Pros:** less code: no interface, no second engine.
**Cons:** the part of the app that must match the web app to the last rounded half would be the slowest and hardest part to test; the shell would no longer be replaceable.

### Option C: repositories in the app module, core takes plain data

Core as pure functions only, with the app module reading storage and passing values in. Rejected because multi-step writes (grading a card, restoring a backup) are rules too, and must happen in one transaction; they would move out of core into ViewModels or app-side services, which hold no rules here.

## Trade-off analysis

The cost of Option A is a second engine and a contract, a few hundred lines that change only when storage gains a capability. What it buys is that every rule and every service runs in a fast JVM test against an engine that is known to behave like the real one, because both pass the same contract. This app's main risk is behaving differently from the web app, and the guard against that is golden tests in core, so making core cheap to test is worth the extra engine.

The data still flows the way the guide describes: storage is the single source of truth and the UI reads state built from it. Only the direction of the compile-time dependency between domain and data is turned round.

## Consequences

- Easier: testing any rule or service; replacing Room; reasoning about what core can depend on.
- Harder: anything new the engine itself must do, which needs both engines and a contract case. Prefer composing existing `AppStorage` methods in a service.
- Rules that follow from it: no `System.currentTimeMillis()`, `UUID` or `java.time` in core, since time and ids come from `CoreEnv`; `storage-room` holds no app rules; nothing outside `di/` names the engine.
- Revisit if: a capability can only be done well inside one engine (full-text search, say) and would be awkward to mirror in `MemoryStorage`.

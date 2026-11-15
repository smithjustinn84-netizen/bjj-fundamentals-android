# Web fixtures

The web app (`../bjj`) is the reference for behaviour and starter content. This script reads its `src/core`
(without changing anything there) and writes two things into this repo:

- the starter content as Kotlin (`libs/core/src/core/seed/Starter*.kt`);
- golden values the Kotlin tests must reproduce (`libs/core/test/core/golden/*.kt`).

Run it from the repository root, with Node 24 or newer, after the web app changes its starter content, its
scheduling or its layout rules:

```sh
node --import ./tools/web-fixtures/resolve-ts.mjs tools/web-fixtures/dump.mjs ../bjj
./kotlin do format
./kotlin test -m core -p jvm
```

A failing test afterwards means the Kotlin has fallen behind the web app: fix the Kotlin, never the golden.
The generated files say so at the top; do not edit them by hand.

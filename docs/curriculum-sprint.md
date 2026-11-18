# Curriculum Curation Sprint

A coaching audit and curation plan for the starter BJJ curriculum (concepts, positions, techniques, lessons, and chains).

Because `libs/core/src/core/seed/Starter*.kt` and `libs/core/test/core/golden/*.kt` are generated from the sibling web app (`../bjj`), starter content changes are authored in `../bjj/src/core/` first and synced here with:

```sh
node --import ./tools/web-fixtures/resolve-ts.mjs tools/web-fixtures/dump.mjs ../bjj
./kotlin do format
./kotlin test -m core -p jvm
```

Changes to `Chains.kt` must be kept in sync with `../bjj/src/core/chains.ts`.

---

## Item 1 — Technical & Prose Corrections
Fix technical inaccuracies and copy issues in existing starter content without changing IDs or adding new items.

- [x] **`back-escape`:** Fix step 2, which currently says tipping to the underhook side pins the *choking* elbow to the mat. Tipping to the underhook side pins the *underhook* arm to the mat while putting the choking arm on top (which is why two-on-one hand control on the choking wrist is mandatory while sliding your shoulders to the mat).
- [x] **`kimura-guard`:** Fix step 5 to clamp the top leg heavy over their *upper* back / shoulder blade (not lower back), so they cannot posture or roll through.
- [x] **`armbar-guard`:** Clarify step 5 wording (*"cross nothing"* → *"do not cross your ankles"*).
- [x] **Typos:** Fix mid-sentence capitals in `front-choke-defence` (*"down Heavy"*) and `positional-hierarchy` (*"As Positions advance"*).

---

## Item 2 — Lesson Sequencing & Coverage
Reorganize `STARTER_LESSONS` so the foundation syllabus matches how beginners are actually taught on the mat.

- [x] **Move Day-1 habits to Lesson 1:** Bring `breakfalls`, `training-partner`, and `roll-to-learn` forward out of Lesson 10 (`turtle-and-standing`) so beginners learn safe falling, hygiene/etiquette, and rolling pace alongside `safety` (*Tap early, tap often*).
- [x] **Fix Lesson 8 (`attacking-from-the-pins`):** Include the core mount triad (`cross-collar-choke`, `armbar-mount`, `mount-to-back`), which are currently omitted from all lessons, ahead of `ezekiel-mount`.
- [x] **Split Lesson 4 (`closed-guard-attacks`):** Break the 6-submission overload into two natural dilemma-based lessons:
  1. Sit-up triad: `hip-bump-sweep`, `kimura-guard`, `guillotine-guard`
  2. Angle triad: `armbar-guard`, `triangle-choke`, `pendulum-sweep` (and `cross-collar-guard` / `omoplata-guard` as follow-ons or separate gi/transition focus)
- [x] **Adopt orphaned core techniques into lessons:** Fold `side-control-escape-knees`, `technical-stand-up`, `pendulum-sweep`, `single-leg`, `hip-throw`, and `turtle-breakdown` into their appropriate lessons so they aren't left out of the guided syllabus.

---

## Item 3 — Self-Defense & Blue Belt List Curation
Decide how the 5 self-defense techniques (`wrist-grab-release`, `front-choke-defence`, `bear-hug-escape`, `standing-headlock-escape`, `ground-headlock-escape`) fit the syllabus.

- [x] Either group them into a dedicated optional **Self-Defense** lesson (and revisit `ground-headlock-escape` to emphasize the higher-percentage frame/bridge-to-back escape alongside or instead of leg-over-face), **or** trim the standing grab/choke/bear-hug drills to keep the curriculum focused on live rolling.
- [x] Review whether all 57 starter techniques should default onto `STARTER_BLUE_BELT_TEST_IDS`, or whether self-defense / advanced transitions (e.g., `omoplata-guard`) should be excluded by default.

---

## Item 4 — High-Value Fundamental Additions
Fill the two biggest white-belt gaps (each new technique also needs a curated video bookmark in `StarterVideos`).

- [x] **Standing closed-guard break:** Add a standing guard break paired with the kneeling break in `break-closed-guard` (and link them as a dilemma in `Chains`).
- [x] **Arm triangle (head-and-arm) choke:** Add from mount or side control as a safe, high-percentage gi + no-gi pin submission.

---

## Item 5 — Tighten Chains & Defences (`Chains.kt` / `chains.ts`)
Strengthen the action–reaction graph for existing techniques.

- [x] Link `americana-mount` → `americana-defence` in `STARTER_DEFENCES`.
- [x] Add `pull-guard` to the standing dilemma (when they stall or win the wrestling exchange).
- [x] Pair `guard-retention` and `technical-stand-up` in open guard.
- [x] Pair `rear-naked-choke-defence` and `back-escape` as a defensive sequence.

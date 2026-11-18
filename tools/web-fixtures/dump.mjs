// Writes the starter content and the golden values from the web app's core. See README.md.
import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { pathToFileURL } from 'node:url';

const webRoot = resolve(process.argv[2] ?? '../bjj');
const web = (file) => import(pathToFileURL(resolve(webRoot, 'src/core', file)).href);
const out = resolve(import.meta.dirname, '../..');

const HEADER = '// Generated from the web app by tools/web-fixtures. Do not edit: change the web app and run it again.\n';
const LONG_LINES = '@file:Suppress("ktlint:standard:max-line-length", "detekt.MaxLineLength") // Prose, one sentence a line.\n';

function write(path, text) {
  const file = resolve(out, path);
  mkdirSync(dirname(file), { recursive: true });
  writeFileSync(file, text);
  console.log(`wrote ${path}`);
}

/** A Kotlin string literal. */
function str(value) {
  const escaped = value
    .replaceAll('\\', '\\\\')
    .replaceAll('"', '\\"')
    .replaceAll('$', '\\$')
    .replaceAll('\n', '\\n')
    .replaceAll('\t', '\\t');
  return `"${escaped}"`;
}

/** A Kotlin Double literal with every digit JavaScript prints, so the two parse to the same number. */
function double(value) {
  const text = String(value);
  return /[.e]/.test(text) ? text : `${text}.0`;
}

const pascal = (text) => text.charAt(0).toUpperCase() + text.slice(1);

function list(items, indent) {
  if (items.length === 0) return 'emptyList()';
  const pad = ' '.repeat(indent);
  return `listOf(\n${items.map((item) => `${pad}    ${item},\n`).join('')}${pad})`;
}

function call(name, fields, indent) {
  const pad = ' '.repeat(indent);
  return `${name}(\n${fields.map(([key, value]) => `${pad}    ${key} = ${value},\n`).join('')}${pad})`;
}

/**
 * A fingerprint of a JSON value: every string, number and boolean in it, keys in sorted order, hashed with
 * 32-bit FNV-1a over UTF-16 code units. The Kotlin test computes the same over its own seed.
 */
function fingerprint(value) {
  let hash = 0x811c9dc5;
  const feed = (text) => {
    for (let i = 0; i < text.length; i++) {
      hash ^= text.charCodeAt(i);
      hash = Math.imul(hash, 0x01000193);
    }
    hash ^= 0x1f;
    hash = Math.imul(hash, 0x01000193);
  };
  const walk = (node) => {
    if (Array.isArray(node)) node.forEach(walk);
    else if (node !== null && typeof node === 'object') {
      for (const key of Object.keys(node).sort()) {
        feed(key);
        walk(node[key]);
      }
    } else feed(String(node));
  };
  walk(value);
  return hash | 0;
}

// --- Starter content ---------------------------------------------------------------------------------------

const GRADES = ['again', 'hard', 'good', 'easy'];
const GRADES_BY_LETTER = Object.fromEntries(GRADES.map((grade) => [grade[0], grade]));

const { starterContent } = await web('seed.ts');
const seed = starterContent(0);

const seedFile = (name, type, rows) =>
  `${HEADER}${LONG_LINES}\npackage dev.justinsmith.bjj.core.seed\n\n${imports(rows)}internal val ${name}: List<${type}> = ${list(rows, 0)}\n`;

function imports(rows) {
  const text = rows.join('\n');
  const names = ['LinkedKind', 'PositionCategory', 'PositionRole', 'TransitionType'].filter((n) => text.includes(`${n}.`));
  return names.length ? `${names.map((n) => `import dev.justinsmith.bjj.core.${n}\n`).join('')}\n` : '';
}

const strings = (items) => list(items.map(str), 8);

write(
  'libs/core/src/core/seed/StarterConcepts.kt',
  seedFile(
    'STARTER_CONCEPTS',
    'StarterConcept',
    seed.concepts.map((c) =>
      call(
        'StarterConcept',
        [
          ['id', str(c.id)],
          ['name', str(c.name)],
          ['summary', str(c.summary)],
          ['keyPoints', strings(c.keyPoints)],
          ['drill', str(c.drill)],
          ['order', c.order],
        ],
        4,
      ),
    ),
  ),
);

write(
  'libs/core/src/core/seed/StarterPositions.kt',
  seedFile(
    'STARTER_POSITIONS',
    'StarterPosition',
    seed.positions.map((p) =>
      call(
        'StarterPosition',
        [
          ['id', str(p.id)],
          ['name', str(p.name)],
          ['category', `PositionCategory.${pascal(p.category)}`],
          ['summary', str(p.summary)],
          ['topGoals', strings(p.topGoals)],
          ['bottomGoals', strings(p.bottomGoals)],
          ['conceptIds', strings(p.conceptIds)],
          ['order', p.order],
        ],
        4,
      ),
    ),
  ),
);

write(
  'libs/core/src/core/seed/StarterTransitions.kt',
  seedFile(
    'STARTER_TRANSITIONS',
    'StarterTransition',
    seed.transitions.map((t) =>
      call(
        'StarterTransition',
        [
          ['id', str(t.id)],
          ['name', str(t.name)],
          ['type', `TransitionType.${pascal(t.type)}`],
          ['fromId', str(t.fromId)],
          ['toId', str(t.toId)],
          ['role', `PositionRole.${pascal(t.role)}`],
          ['endRole', t.endRole ? `PositionRole.${pascal(t.endRole)}` : 'null'],
          ['steps', strings(t.steps)],
          ['commonMistakes', strings(t.commonMistakes)],
          ['drill', str(t.drill)],
          ['blueBeltTest', String(t.blueBeltTest)],
        ],
        4,
      ),
    ),
  ),
);

write(
  'libs/core/src/core/seed/StarterVideos.kt',
  seedFile(
    'STARTER_VIDEOS',
    'StarterVideo',
    seed.videos.map((v) =>
      call(
        'StarterVideo',
        [
          ['id', str(v.id)],
          ['title', str(v.title)],
          ['youtubeId', str(v.youtubeId)],
          ['linkedKind', `LinkedKind.${v.linkedKind === '' ? 'None' : pascal(v.linkedKind)}`],
          ['linkedId', str(v.linkedId)],
          ['note', str(v.note)],
        ],
        4,
      ),
    ),
  ),
);

const golden = (name, body) =>
  write(`libs/core/test/core/golden/${name}.kt`, `${HEADER}${LONG_LINES}\npackage dev.justinsmith.bjj.core.golden\n\n${body}`);

golden(
  'SeedGolden',
  `/** How many rows each starter table has, and a fingerprint of everything in them (see \`fingerprint\`). */\ninternal val SEED_GOLDEN: Map<String, Pair<Int, Int>> = mapOf(\n${['concepts', 'positions', 'transitions', 'videos']
    .map((table) => `    ${str(table)} to (${seed[table].length} to ${fingerprint(seed[table])}),\n`)
    .join('')})\n`,
);

// --- Lessons, the teaching order and how techniques answer each other ---------------------------------------

const { FOUNDATION_LESSONS, lessonCards } = await web('lessons.ts');
write(
  'libs/core/src/core/seed/StarterLessons.kt',
  `${HEADER}${LONG_LINES}\npackage dev.justinsmith.bjj.core.seed\n\nimport dev.justinsmith.bjj.core.Lesson\n\ninternal val STARTER_LESSONS: List<Lesson> = ${list(
    FOUNDATION_LESSONS.map((l) =>
      call(
        'Lesson',
        [
          ['id', str(l.id)],
          ['title', str(l.title)],
          ['summary', str(l.summary)],
          ['conceptIds', strings(l.conceptIds)],
          ['positionIds', strings(l.positionIds)],
          ['transitionIds', strings(l.transitionIds)],
          ['practice', str(l.practice)],
        ],
        4,
      ),
    ),
    0,
  )}\n`,
);

const { buildLibrary } = await web('library.ts');
const { lessonOrder } = await web('curriculum.ts');
const { techniqueLinks } = await web('chains.ts');
const lib = buildLibrary(seed.concepts, seed.positions, seed.transitions, []);
const cardKey = (item) => str(`${item.kind}:${item.id}`);
const ids = (rows) => list(rows.map((row) => str(row.id)), 0);
golden(
  'CurriculumGolden',
  `/** The starter library in library order: concepts, positions, techniques. */
internal val LIBRARY_CONCEPTS: List<String> = ${ids(lib.concepts)}

internal val LIBRARY_POSITIONS: List<String> = ${ids(lib.positions)}

internal val LIBRARY_TRANSITIONS: List<String> = ${ids(lib.transitions)}

/** Every card of the starter library, by card id, in the order it is taught. */
internal val LESSON_ORDER: List<String> = ${list(lessonOrder(lib).map(cardKey), 0)}

/** Each lesson's cards, by card id, in the order they are taught. */
internal val LESSON_CARDS: Map<String, List<String>> = mapOf(
${FOUNDATION_LESSONS.map((l) => `    ${str(l.id)} to ${list(lessonCards(lib, l).map(cardKey), 4)},\n`).join('')})

/** For each starter technique with links: its defences, what it answers, and its follow-ups as \`id|when\`. */
internal val TECHNIQUE_LINKS: Map<String, Triple<List<String>, List<String>, List<String>>> = mapOf(
${lib.transitions
  .map((t) => [t.id, techniqueLinks(lib, t.id)])
  .filter(([, links]) => links.defences.length + links.answers.length + links.followUps.length > 0)
  .map(
    ([id, links]) =>
      `    ${str(id)} to Triple(\n        ${list(links.defences.map((d) => str(d.id)), 8)},\n        ${list(links.answers.map((a) => str(a.id)), 8)},\n        ${list(links.followUps.map((f) => str(`${f.technique.id}|${f.when}`)), 8)},\n    ),\n`,
  )
  .join('')})
`,
);

// --- Drawings: the position map and the figures -------------------------------------------------------------

const { layoutPositionMap } = await web('mapLayout.ts');
const numbers = (path) => (path.match(/-?\d+(\.\d+)?/g) ?? []).map(Number);
const doubles = (values) => `listOf(${values.map(double).join(', ')})`;
const mapGolden = (orientation) => {
  const layout = layoutPositionMap(lib.positions, lib.transitions, orientation);
  return `MapGolden(
    width = ${double(layout.width)},
    height = ${double(layout.height)},
    lanes = ${list(layout.lanes.map((l) => `${str(l.category)} to ${doubles([l.labelX, l.labelY])}`), 4)},
    nodes = ${list(layout.nodes.map((n) => `${str(n.id)} to ${doubles([n.x, n.y, n.width, n.height, n.submissions])}`), 4)},
    edges = ${list(layout.edges.map((e) => `${str(e.id)} to ${doubles([e.bow, ...numbers(e.path)])}`), 4)},
)`;
};
golden(
  'MapGolden',
  `/**
 * The starter library's map as the web app lays it out. A lane is its label's x and y; a node its x, y, width,
 * height and count of submissions; an edge its bow, then its curve's start, control and end.
 */
internal data class MapGolden(
    val width: Double,
    val height: Double,
    val lanes: List<Pair<String, List<Double>>>,
    val nodes: List<Pair<String, List<Double>>>,
    val edges: List<Pair<String, List<Double>>>,
)

internal val MAP_HORIZONTAL = ${mapGolden('horizontal')}

internal val MAP_VERTICAL = ${mapGolden('vertical')}
`,
);

const { figureParts, arrowPath } = await web('figure.ts');
const POSES = [
  // Standing, looking right.
  { head: [120, 60], neck: [120, 78], hip: [122, 132], face: 1, arms: { far: [[136, 96], [150, 110]], near: [[108, 100], [112, 124]] }, legs: { far: [[134, 164], [130, 196]], near: [[112, 164], [108, 196]] } },
  // Lying with a bridged spine, looking up.
  { head: [70.5, 181], neck: [88, 178.25], hip: [150, 172], face: -1, spine: [118, 150.5], arms: { far: [[104, 160], [122, 149.5]], near: [[100, 190], [118, 192]] }, legs: { far: [[178, 150], [196, 190]], near: [[182, 156], [201.5, 191]] } },
  // Kneeling and rounded, looking left.
  { head: [233.3, 96.7], neck: [246, 110], hip: [262, 160], face: -1, spine: [266.6, 128.4], arms: { far: [[230, 130], [214, 142]], near: [[238, 138], [222, 156]] }, legs: { far: [[240, 176], [270, 191]], near: [[236, 180], [266, 192]] } },
];
const point = ([x, y]) => `Point(${double(x)}, ${double(y)})`;
const limb = ([joint, end]) => `Limb(${point(joint)}, ${point(end)})`;
const pose = (p) =>
  `FigurePose(head = ${point(p.head)}, neck = ${point(p.neck)}, hip = ${point(p.hip)}, face = ${p.face}, arms = LimbPair(${limb(p.arms.far)}, ${limb(p.arms.near)}), legs = LimbPair(${limb(p.legs.far)}, ${limb(p.legs.near)}), spine = ${p.spine ? point(p.spine) : 'null'})`;
const figureNumbers = (p) => {
  const parts = figureParts(p);
  const hair = numbers(parts.hair);
  return [
    ...numbers(parts.torso),
    ...numbers(parts.belt),
    ...hair.slice(0, 2),
    ...hair.slice(-2),
    ...['farLeg', 'farArm', 'nearLeg', 'nearArm'].flatMap((name) => numbers(parts.limbs[name].path)),
  ];
};
const ARROWS = [
  [[100, 100], [180, 100], 0],
  [[100, 100], [180, 60], 12],
  [[220.5, 40], [150, 130.5], -18],
  [[50, 50], [50, 50], 0],
];
golden(
  'FigureGolden',
  `import dev.justinsmith.bjj.core.FigurePose
import dev.justinsmith.bjj.core.Limb
import dev.justinsmith.bjj.core.LimbPair
import dev.justinsmith.bjj.core.Point

/**
 * Poses, and what the web app draws for each: the torso's curve, the belt's line, where the hair's arc starts
 * and ends, then the far leg, far arm, near leg and near arm, each from the body through its joint to its end.
 */
internal val FIGURE_GOLDEN: List<Pair<FigurePose, List<Double>>> = ${list(POSES.map((p) => `${pose(p)} to ${doubles(figureNumbers(p))}`), 0)}

/** Arrows as from, to and bend, and what the web app draws: the shaft's curve, then the head's two back corners. */
internal val ARROW_GOLDEN: List<Pair<Triple<Point, Point, Double>, List<Double>>> = ${list(
    ARROWS.map(([from, to, bend]) => {
      const n = numbers(arrowPath(from, to, bend));
      return `Triple(${point(from)}, ${point(to)}, ${double(bend)}) to ${doubles([...n.slice(0, 8), ...n.slice(10, 12)])}`;
    }),
    0,
  )}
`,
);

// --- A backup file as the web app writes it ------------------------------------------------------------------

const { createMemoryStorage } = await web('memoryStorage.ts');
const { createCore } = await web('core.ts');
const { validateBackup } = await web('backup.ts');
const fixtures = await web('testing/testFixtures.ts');
{
  let clock = 1_700_000_000_000;
  let lastId = 0;
  const storage = createMemoryStorage({
    initialData: {
      concepts: [fixtures.concept({ id: 'frames', name: 'Frames', keyPoints: ['Bones, not muscles', 'Elbows "in"'] })],
      positions: [
        fixtures.position({ id: 'guard', name: 'Closed Guard', conceptIds: ['frames'], topGoals: ['Posture'] }),
        fixtures.position({ id: 'mount', name: 'Mount', category: 'top', order: 2 }),
      ],
      transitions: [
        // As stored before roles, drills and the blue belt list existed: none of the optional fields.
        fixtures.transition({ id: 'old-sweep', name: 'Sweep', fromId: 'guard', toId: 'mount' }),
        fixtures.transition({ id: 'choke', name: 'Choke', type: 'submission', fromId: 'mount', toId: '', role: 'top', drill: '', blueBeltTest: false, steps: ['Grip', 'Squeeze'] }),
        fixtures.transition({ id: 'escape', name: 'Escape', type: 'escape', fromId: 'mount', toId: 'guard', role: 'bottom', endRole: 'bottom', drill: 'Ten reps', blueBeltTest: true }),
      ],
      videos: [],
      reviews: [],
      reviewLog: [],
      starterDismissals: [{ id: 'scissor-sweep', dismissedAt: 5 }],
      trainingLog: [fixtures.trainingEntry({ id: 'before-stuck', day: '2026-10-31', techniqueIds: ['old-sweep'] })],
      settings: [],
    },
  });
  const core = createCore({ storage, newId: () => `id-${++lastId}`, now: () => clock });
  await core.videos.add({ title: 'Escape, slowly', youtubeId: 'dQw4w9WgXcQ', startSeconds: 95, linkedKind: 'transition', linkedId: 'escape', note: 'From 1:35' });
  await core.videos.add({ title: '', youtubeId: 'y5nAO-yYnQU', startSeconds: 0, linkedKind: '', linkedId: '', note: '' });
  for (const [kind, id, grades] of [['concept', 'frames', 'ggeh'], ['transition', 'escape', 'gag'], ['situation', 'mount:bottom', 'e'], ['position', 'guard', 'hhg']]) {
    for (const letter of grades) {
      clock += 3 * 86_400_000 + 1234;
      await core.reviews.recordReview({ kind, id }, GRADES_BY_LETTER[letter]);
    }
  }
  await core.trainingLog.save(null, { day: '2026-11-14', minutes: 90, taught: ' Mount escapes ', worked: '', struggled: 'Stuck under mount', focus: 'Elbows in', techniqueIds: ['escape', 'gone'], stuckIds: ['mount:bottom'] });
  await core.settings.setTheme('dark');
  const exported = validateBackup(JSON.parse(JSON.stringify(await core.backups.exportBackup())));
  const lines = JSON.stringify(exported, null, 2).split('\n');
  golden(
    'BackupGolden',
    `/** A backup the web app exported and its own check accepts, line by line. */\ninternal val WEB_BACKUP: String = ${list(lines.map(str), 0)}.joinToString("\\n")\n`,
  );
}

// --- Scheduling ---------------------------------------------------------------------------------------------

const { schedule, describeInterval } = await web('srs.ts');

const NOW = 1_000_000_000_000;
const cases = [];
for (const ease of [1.3, 1.45, 2.05, 2.5, 2.65, 3.1]) {
  for (const [intervalDays, repetitions] of [
    [0, 0],
    [1, 1],
    [3, 2],
    [4, 1],
    [5, 3],
    [8, 3],
    [15, 4],
    [25, 5],
    [130, 7],
    [400, 9],
  ]) {
    for (const grade of GRADES) {
      const prev = { id: 'concept:x', kind: 'concept', itemId: 'x', ease, intervalDays, repetitions, lapses: 1, due: NOW, lastReviewed: 0 };
      const next = schedule(prev, grade, NOW);
      cases.push(
        `ScheduleCase(${double(ease)}, ${intervalDays}, ${repetitions}, Grade.${pascal(grade)}, ${double(next.ease)}, ${next.intervalDays}, ${next.repetitions}, ${next.lapses}, ${next.due - NOW}, ${str(describeInterval(prev, grade, NOW))})`,
      );
    }
  }
}
// A long walk of one card, so the ease's small steps add up exactly as they do in JavaScript.
const walk = [];
let card = { id: 'concept:x', kind: 'concept', itemId: 'x', ease: 2.5, intervalDays: 0, repetitions: 0, lapses: 0, due: NOW, lastReviewed: 0 };
const pattern = 'ggegheghgagghegeehgghagggeeh';
for (const letter of pattern) {
  const grade = GRADES.find((g) => g.startsWith(letter));
  card = schedule(card, grade, NOW);
  walk.push(`WalkStep(Grade.${pascal(grade)}, ${double(card.ease)}, ${card.intervalDays}, ${card.repetitions}, ${card.lapses})`);
}
golden(
  'ScheduleGolden',
  `import dev.justinsmith.bjj.core.Grade

/** One review: a card with [ease], [intervalDays] and [repetitions] (and one lapse) graded [grade], and what the web app makes of it. */
internal data class ScheduleCase(
    val ease: Double,
    val intervalDays: Int,
    val repetitions: Int,
    val grade: Grade,
    val nextEase: Double,
    val nextIntervalDays: Int,
    val nextRepetitions: Int,
    val nextLapses: Int,
    val dueIn: Long,
    val label: String,
)

internal val SCHEDULE_GOLDEN: List<ScheduleCase> = ${list(cases, 0)}

/** One step of a new card graded again and again: the grade, and the card after it. */
internal data class WalkStep(val grade: Grade, val ease: Double, val intervalDays: Int, val repetitions: Int, val lapses: Int)

internal val SCHEDULE_WALK: List<WalkStep> = ${list(walk, 0)}
`,
);

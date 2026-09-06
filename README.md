# Word Bloom

A Wordscapes-style word puzzle game for Android. Swipe letters on a circular wheel to
fill a crossword grid, find bonus words along the way, and learn through 16 bilingual
English–Bengali categories. Each category has its own independently unlocked starting
point and progresses from easier to harder words.

Built with Kotlin, Jetpack Compose, and an MVI architecture.

---

## Contents

- [Running it](#running-it)
- [Architecture](#architecture)
- [The swipe gesture](#the-swipe-gesture)
- [Level data](#level-data)
- [Navigation and state retention](#navigation-and-state-retention)
- [Polish details](#polish-details)
- [Testing](#testing)
- [Project layout](#project-layout)

---

## Running it

Build and install the current category-based version yourself:

```bash
./gradlew :app:assembleRelease     # signed release APK
./gradlew :app:installDebug        # install debug build on a connected device
./gradlew :app:testDebugUnitTest   # run the unit test suite
```

Requires JDK 17+ (Android Studio's bundled JDK works), Android SDK 35. Minimum
device API is 24 (Android 7.0).

The release build is signed with a keystore checked into the repo
(`app/keystore/release.jks`) so the attached APK installs on any device without
needing my signing secrets. That is deliberate for an assessment build — a real
production app would keep signing material out of version control.

---

## Architecture

**MVI, single-Activity, Compose-only.**

```
UI (Composables)  ──intent──►  ViewModel  ──►  Repository  ──►  assets / DataStore
       ▲                           │
       └────── StateFlow<UiState> ─┘
       └────── Flow<Effect> ───────┘   (one-shot: haptics, navigation)
```

The gameplay screen is the interesting case, so it is worth being concrete about why
it is shaped this way.

**One immutable state object.** `GameUiState` holds everything on screen — the level,
found words, the in-flight finger selection, feedback, pause and completion flags.
The UI is a pure function of it. There is no state hidden in composables, which is
precisely why rotation and process death are non-events (see below).

**All mutation goes through one funnel.** Every interaction becomes a `GameIntent`
handled by a single `when` in `GameViewModel.onIntent`. There is exactly one place
where state can change, so every transition is traceable and testable. The UI never
mutates state directly.

**State vs. events are kept separate.** Things that *are* true (words found, level
paused) live in state. Things that *happen once* (play a haptic, navigate to the next
level) go out over a `Channel` as `GameEffect`s. This distinction matters: if
"navigate to level 3" lived in state, it would re-fire on every recomposition and on
rotation. As a channel effect it is delivered exactly once, and it buffers while the
app is backgrounded rather than being dropped.

**Dependency injection is done by hand.** `AppContainer` holds the two repositories;
ViewModels take their dependencies as constructor parameters and are built by
factories. Hilt would be the right call in a larger codebase, but with two singletons
and three ViewModels, a hand-rolled container keeps the whole graph visible in one
20-line file and avoids annotation processing in the build. The ViewModels are still
plain constructor-injected classes, so tests substitute fakes without any framework.

**Data and domain models are separate.** `data.model.LevelDto` mirrors the JSON
exactly; `domain.model.Level` is what the game logic reasons about. The mapper between
them is the only code that would need to change if the asset schema changed.

`domain.model.Level` also precomputes the things the UI asks for repeatedly
(`occupiedCells`, `keyWord`), so those are not recalculated on every recomposition.

---

## The swipe gesture

This is the heart of the game, and the part with the most non-obvious decisions.
It all lives in [`LetterWheel.kt`](app/src/main/java/com/ritik/wordpuzzle/ui/game/components/LetterWheel.kt).

### One gesture loop for the whole wheel, not one per tile

The obvious approach — give each tile its own `pointerInput` — does not work. Compose
delivers a pointer stream to the composable where the gesture **started**. Once your
finger leaves the first tile, that tile keeps receiving the events and its neighbours
never hear about them.

So the wheel owns a single `awaitEachGesture` loop and does its own hit-testing
against tile centres. One place sees the entire finger path, which is also what makes
backtracking and rapid flicks tractable.

### Interpolated sampling, so fast swipes cannot skip tiles

A flick can move 200px between two frames — straight over a tile without ever
generating an event inside it. Hit-testing only at event positions would silently drop
that letter.

Instead, each move event interpolates along the segment from the previous point and
hit-tests at intervals:

```kotlin
val distance = (position - previous).getDistance()
val steps = ((distance / (hitRadius * SAMPLE_FRACTION)).toInt() + 1)
    .coerceAtMost(MAX_INTERPOLATION_STEPS)
for (step in 1..steps) {
    val t = step.toFloat() / steps
    val sample = lerp(previous, position, t)
    tileAt(sample)?.let(onExtend)
}
```

The step count scales with distance and is capped, so a slow drag costs one sample and
a fast flick costs a bounded number.

### Selection rules

`extendSelection` handles four cases in order, and the ordering matters:

1. **Still on the current tile** → no-op. A finger resting inside a tile emits move
   events continuously; without this the other branches would fire repeatedly.
2. **Re-entered the second-to-last tile** → walk the trail back one step. This is the
   standard affordance for correcting a swipe without lifting your finger.
3. **Re-entered any other used tile** → ignore. A letter cannot be consumed twice.
4. **A fresh tile** → append.

### Rendering

Tiles are real composables (so text, animation, and accessibility come for free); the
trail and wheel face are drawn on a `Canvas` beneath them via `drawBehind`. Both the
renderer and the hit-tester read the *same* precomputed tile-centre list, so what the
player sees is exactly what they can hit.

The trail is a single `Path` through the selected centres plus a live segment to the
finger, stroked twice — a wide translucent pass for glow, then the solid line. That
gives depth without a blur pass.

### Rapid swiping

An early version set an `isResolvingWord` flag on every accepted word and cleared it
in a coroutine, blocking input until the word's animation finished. A player swiping
quickly would have their second and third words **silently swallowed**. A unit test
caught it (`words landed in quick succession are all accepted`).

Word acceptance is now fully synchronous. The only latch left is
`isCompletionPending`, which exists solely so the deferred level-complete celebration
cannot fire twice.

---

## Level data

16 categories and 160 levels in
[`app/src/main/assets/categories.json`](app/src/main/assets/categories.json), loaded
through an offline-first repository. Category introductions, completion passages,
hero words, target-word meanings, and short lesson stories include English and Bengali
text; gameplay answers remain English.

```json
{
  "id": "fish",
  "nameEn": "Fish",
  "nameBn": "মাছ",
  "introductionEn": "Fish live in water.",
  "introductionBn": "মাছ পানিতে বাস করে।",
  "levels": [{
  "id": 1,
  "order": 1,
  "difficulty": 1,
  "heroWord": {
    "word": "COD",
    "definitionEn": "A kind of sea fish.",
    "translationBn": "কড মাছ",
    "meaningBn": "এক ধরনের সামুদ্রিক মাছ।"
  },
  "wordMeanings": ["one bilingual record for every target word"],
  "lesson": {
    "sentenceEn": "The cod swims in cold water.",
    "sentenceBn": "কড মাছ ঠান্ডা পানিতে সাঁতার কাটে।",
    "usedWords": ["COD", "COLD"]
  }
  }]
}
```

- `letters` — the wheel, always an exact anagram of the level's longest word.
- `words` — must all be found to complete the level; each has a grid `placement`.
- `bonusWords` — valid, scored, but not on the grid.
- `placements` — origin cell and orientation; the grid is derived from these rather
  than stored as a character matrix, which keeps the file compact and makes the
  crossing constraints explicit.

Every category begins with Level 1 unlocked. Later levels unlock only inside that
category. The catalog contains 560 target-word occurrences across 10 levels per category, with
difficulty and age bands increasing inside each category. Educational copy remains
marked `draft` until a Bangladeshi Bengali reviewer and primary educator approve it.

### The levels are generated and verified, not hand-typed

Hand-authoring interlocking crosswords is error-prone — my first draft had words that
could not be spelled from their own wheel.
[`tools/generate_category_catalog.py`](tools/generate_category_catalog.py)
lays out each level by backtracking search and asserts every invariant before writing
the file: words spellable from the wheel, wheel matching the longest word, every word
placed and crossing an existing one, crossings agreeing on the shared letter, no
accidental adjacent letter runs, and a bounded grid size.

The same invariants are re-checked against the shipped JSON in
[`LevelDatasetTest`](app/src/test/java/com/ritik/wordpuzzle/data/LevelDatasetTest.kt),
so an unplayable level cannot reach a build even if someone edits the JSON by hand.

### Optional catalog API

The bundled asset is always available offline. A build can optionally pull a newer
approved catalog from the reference API:

```bash
cd server && npm test
node catalog-server.mjs

./gradlew :app:installDebug \
  -PWORD_BLOOM_API_URL=http://10.0.2.2:8787/v1/catalog
```

`10.0.2.2` lets the Android emulator reach the host machine. Production builds require
HTTPS. The client validates every downloaded puzzle, accepts only educator-approved
schema-v2 content, uses ETag/304 requests, writes an atomic app-private cache, rejects
older versions, and falls back to a valid cache or the bundled catalog after any
network or validation failure. See [`server/README.md`](server/README.md) for the
endpoint contract and deployment boundaries.

---

## Navigation and state retention

Single Activity, one Compose `NavHost`, one back stack. All the rules live in
[`WordPuzzleNavHost.kt`](app/src/main/java/com/ritik/wordpuzzle/ui/navigation/WordPuzzleNavHost.kt).

| Flow | Behaviour |
|---|---|
| Splash → Home | Splash is popped (`inclusive = true`); back from Home exits the app |
| Home → Game | Home stays beneath; back returns Home |
| Level Select → Game | Level Select stays beneath; back returns to the grid |
| Game → next level | **Replaces** the finished level rather than stacking it |
| Any navigation | `launchSingleTop`, so a double-tap cannot push two copies |

The "next level" rule matters: without `popUpTo(...) { inclusive = true }`, finishing
ten levels would leave ten game screens on the stack and back would walk through every
one of them.

**Pause is state, not a destination.** Modelling it as a route would mean the game
screen could be recreated behind the dialog and would complicate back handling. As
state, back from Pause is a plain state transition. Back in-game pauses rather than
leaving, so you are never dumped out of a level mid-way.

### Surviving rotation, backgrounding, and process death

Three separate mechanisms, because they are three separate problems:

1. **Rotation** — the Activity declares `configChanges` for orientation, and the game
   is portrait-locked anyway, so it is not even recreated. Even if it were, the
   ViewModel outlives it.
2. **Backgrounding** — a `LifecycleEventEffect` on `ON_STOP` auto-pauses, so returning
   from a phone call does not drop you into a live board.
3. **Process death** — found words, bonus words, hint reveals, and the current level
   id are mirrored into `SavedStateHandle` on every change and restored in the
   ViewModel's `init`. The in-flight finger selection deliberately is **not** saved; a
   half-drawn swipe is meaningless after the process is rebuilt.

Verified on device by killing the process outright with `adb shell am kill` and
relaunching — the board came back with progress intact.

---

## Polish details

- **Staggered word reveal.** When a word lands, its cells flip in one at a time
  (`REVEAL_STAGGER_MS` apart) with a Y-rotation and a spring scale pop, so the eye
  follows the word as it appears rather than everything blinking on at once.
- **Spring-based tile feedback.** Selected tiles scale up with a bouncy spring and
  gain a halo drawn outside their bounds.
- **Shake on rejection.** An invalid word shakes the preview chip horizontally via a
  keyframe animation — faster to read than any error text.
- **Colour-coded feedback.** Teal for a grid word, amber for a bonus word, coral for
  invalid, grey for already-found.
- **Haptics** go through `View.performHapticFeedback`, so they need no `VIBRATE`
  permission and automatically respect the user's system haptics setting. Richer
  constants (`CONFIRM`, `REJECT`) are used on API 30+ with older equivalents below.
- **Entry animations** on every screen. The home screen's three elements animate
  concurrently with offset delays rather than sequentially — chaining them left the
  buttons invisible and untappable for most of a second after the screen appeared.
- **Adaptive layout.** The wheel is sized from whichever of width/height is tighter,
  and grid cells are computed from the level's dimensions, so a 3×3 and a 10×10 level
  both fit without per-level tuning.
- **Accessibility.** Tiles and grid cells carry content descriptions; locked levels
  announce their state.

---

## Testing

27 unit tests, all passing:

```bash
./gradlew :app:testDebugUnitTest
```

**`GameViewModelTest`** drives the ViewModel purely through intents — the same surface
the UI uses — so the tests describe player-visible behaviour: word acceptance, bonus
words, rejection, backtracking, double-use prevention, hints, shuffle, pause blocking
input, restart, level completion and unlocking, `SavedStateHandle` restoration, and
the rapid-succession regression.

**`LevelDatasetTest`** parses the real shipped `categories.json` and re-verifies every
dataset invariant. **`ProgressRepositoryTest`** verifies independent category progress,
idempotent completion, and the bounded migration of legacy progress into Animals.

Manual verification on an API 35 emulator covered the category hub, bilingual labels,
independent Fish level selection, and Fish gameplay. The original linear build's wider
interaction checks remain documented by the existing test suite.

---

## Project layout

```
app/src/main/java/com/ritik/wordpuzzle/
├── data/
│   ├── local/ProgressRepository.kt      DataStore-backed progress
│   ├── model/LevelDto.kt                JSON wire format
│   └── repository/                      validation, asset/API/cache loading
├── di/ServiceLocator.kt                 AppContainer
├── domain/model/Level.kt                Level, Placement, LetterTile, GridPosition
├── ui/
│   ├── common/Buttons.kt
│   ├── game/
│   │   ├── GameContract.kt              UiState, Intent, Effect
│   │   ├── GameViewModel.kt             the state machine
│   │   ├── GameScreen.kt
│   │   ├── Overlays.kt                  pause + level complete
│   │   └── components/                  LetterWheel, WheelTile, CrosswordGrid, WordPreview
│   ├── category/ · learning/ · home/ · levelselect/ · splash/
│   ├── navigation/                      Destinations, NavHost
│   └── theme/                           Color, Type, Theme
└── util/Haptics.kt

app/src/main/assets/categories.json      16 categories and their levels
tools/generate_category_catalog.py       catalog generator + validator
tools/test_category_catalog.py           source-to-asset generation tests
server/                                  reference read-only catalog API
```

---

## Trade-offs I would revisit

- **No sound.** Haptics and animation carry the feedback; audio assets felt out of
  scope for an assessment build.
- **Portrait-locked.** Correct for this genre, but it does mean the rotation-handling
  code paths are only exercised under process death.
- **Manual DI.** Right for this size; I would move to Hilt as soon as a second feature
  module appeared.
- **Hints are deterministic** (first unrevealed cell of the first unsolved word)
  rather than weighted toward the word closest to completion.

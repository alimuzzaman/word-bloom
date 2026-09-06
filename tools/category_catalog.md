# Category catalogue pipeline

`generate_category_catalog.py` imports the 16 bilingual themes and the 955-word
learning dictionary from the Word Game Rida PWA. It deterministically creates ten
compact native-compatible puzzles for every category. A short super-anagram is
used as the wheel when a hero word has too few useful sub-anagrams of its own.

`category_learning_content.py` is the human-editable source for hero-word order,
category introductions, and compact completion passages. Carefully edited
bilingual stories live in three bounded review files:
`editorial_nature_copy.py`, `editorial_life_copy.py`, and
`editorial_daily_copy.py`. `editorial_story_copy.py` combines those batches for
the generator. The generated schema-v2 asset contains:

- 16 categories and 160 levels (10 per category)
- a five-step difficulty ramp and four age bands
- a distinct hero word, English definition, Bengali translation, and Bengali
  meaning for every level
- English and Bengali meaning records for every crossword target
- a bilingual two-to-four-sentence mini-story for every level; its English copy and exact
  `usedWords` list cover every playable target in that level
- bilingual introduction and completion copy for every category
- a multi-paragraph bilingual `storyEn`/`storyBn` for every category, plus a
  deterministic `storyWords` list containing every unique target from its ten
  levels in first-seen order
- an explicit `draft` review status and review notice

Generate the checked-in asset:

```bash
python3 tools/generate_category_catalog.py \
  --source-root /absolute/path/to/word-game-rida
```

Verify that the asset is reproducible and run the source-independent tests:

```bash
python3 tools/generate_category_catalog.py \
  --source-root /absolute/path/to/word-game-rida --check
python3 tools/test_category_catalog.py
```

Each editorial module can also be run directly to check its assigned category
and level keys against the current asset. The main validator covers schema and
catalogue versions, exact category/level counts,
bilingual field completeness, per-target meaning coverage, exact lesson-word
identity, whole-word English lesson coverage, exact category-story target
identity, whole-word English story coverage, multi-paragraph bilingual category
structure, normal-case narrative prose, stable ordering, composite level
identity, the difficulty/age ramp, wheel spelling, placements, crossings,
bounds, and a small blocklist of unsuitable dictionary forms. It proves
structural playability and mechanical content coverage. It does not prove
educational, narrative, cultural, or translation quality.

All current learning copy is deliberately marked `draft`. A Bangladeshi Bengali
native speaker and a primary educator must review wording, translations,
vocabulary, cultural fit, and age suitability before any release changes the
status to `approved`.

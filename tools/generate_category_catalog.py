#!/usr/bin/env python3
"""Generate and validate the 16-category bilingual Word Bloom catalogue."""

from __future__ import annotations

import argparse
from collections import Counter
import json
from pathlib import Path
import re
from typing import Any

from category_learning_content import (
    CATALOG_VERSION,
    CATEGORY_CONTENT,
    CONTENT_REVIEW_NOTICE,
)
from editorial_story_copy import CATEGORY_STORIES, LEVEL_STORIES
from generate_levels import layout, normalise


ICON_KEYS = {
    "animals": "paw", "fish": "fish", "birds": "bird",
    "flowers": "flower", "fruits": "fruit", "vegetables": "vegetable",
    "trees_plants": "plant", "body_parts": "body", "colors": "palette",
    "family": "family", "school": "school", "transport": "transport",
    "weather": "weather", "food_kitchen": "kitchen",
    "clothing": "clothing", "numbers": "numbers",
}
EXPECTED_LEVELS_PER_CATEGORY = 10
DIFFICULTY_BY_ORDER = (1, 1, 2, 2, 3, 3, 4, 4, 5, 5)
AGE_BAND_BY_ORDER = (
    "6-7", "6-7", "7-9", "7-9", "9-11",
    "9-11", "9-11", "11-14", "11-14", "11-14",
)
# Keep mechanically valid but unsuitable entries out of a children's release.
# ALE is alcohol-related; BOW is defined with a weapon sense in the source; ROB
# centers forced taking. The earlier entries are obscure dictionary fragments.
DISALLOWED_WORDS = {
    "AHI", "ALE", "APM", "BOW", "ICH", "OD", "REB", "ROB", "TAE",
}

# High-difficulty wheels can expose mechanically valid support words that make
# the learning story wander away from the category. These reviewed overrides
# keep only combinations that can carry a natural, child-friendly scene.
TARGET_WORD_OVERRIDES = {
    ("fish", "prawn"): ("prawn", "raw", "pan"),
    ("flowers", "aster"): ("aster", "star"),
    ("flowers", "pansy"): ("pansy", "any", "say"),
    ("flowers", "thorn"): ("thorn", "hot", "not", "rot"),
    ("flowers", "orchid"): ("orchid", "rich", "hid", "rid"),
    ("trees_plants", "bush"): ("bush", "bus"),
    ("food_kitchen", "bowl"): ("bowl", "low"),
    ("food_kitchen", "bread"): ("bread", "read"),
    ("clothing", "coat"): ("coat", "cat", "act"),
    ("clothing", "pants"): ("pants", "tan", "pat"),
}


def can_spell(word: str, letters: str) -> bool:
    return not (Counter(word.upper()) - Counter(letters.upper()))


def require_text(owner: str, value: Any) -> str:
    if not isinstance(value, str) or not value.strip():
        raise ValueError(f"{owner} must be non-empty")
    return value.strip()


def require_natural_story(owner: str, english: str, bengali: str, *, paragraphs: bool) -> None:
    """Reject mechanical coverage copy before it reaches the app."""
    uppercase_tokens = [
        token for token in re.findall(r"[A-Za-z]+", english)
        if len(token) > 1 and token.isupper()
    ]
    if uppercase_tokens:
        raise ValueError(f"{owner}: prose contains all-caps tokens {uppercase_tokens}")
    lowered = english.lower()
    if any(fragment in lowered for fragment in (" means ", "word trail", "word list", "glossary")):
        raise ValueError(f"{owner}: prose reads like a glossary instead of a story")
    if paragraphs:
        if len(english.split("\n\n")) < 2 or len(bengali.split("\n\n")) < 2:
            raise ValueError(f"{owner}: category story must be multi-paragraph and bilingual")
    else:
        if len(re.findall(r"[.!?](?:\s|$)", english)) < 2:
            raise ValueError(f"{owner}: English level story needs at least two sentences")
        if len(re.findall(r"[।!?](?:\s|$)", bengali)) < 2:
            raise ValueError(f"{owner}: Bengali level story needs at least two sentences")


def load_source(source_root: Path) -> tuple[dict[str, Any], list[dict[str, Any]]]:
    themes_path = source_root / "research" / "themed-word-groups.json"
    words_path = source_root / "public" / "data" / "words.json"
    if not themes_path.is_file():
        raise ValueError(f"missing theme source: {themes_path}")
    if not words_path.is_file():
        raise ValueError(f"missing dictionary source: {words_path}")
    themes = json.loads(themes_path.read_text(encoding="utf-8"))["categories"]
    dictionary = json.loads(words_path.read_text(encoding="utf-8"))
    if not isinstance(dictionary, list) or not dictionary:
        raise ValueError(f"dictionary source is empty: {words_path}")
    return themes, dictionary


def dictionary_entry_is_complete(entry: dict[str, Any]) -> bool:
    word = entry.get("id", "")
    return (
        isinstance(word, str)
        and 3 <= len(word) <= 8
        and word.isascii()
        and word.isalpha()
        and word.upper() not in DISALLOWED_WORDS
        and all(
            isinstance(entry.get(field), str) and entry[field].strip()
            for field in ("definitionEn", "translationBn", "meaningBn")
        )
    )


def source_priority(entry: dict[str, Any]) -> int:
    sources = set(entry.get("sources", []))
    if "themed-groups" in sources:
        return 0
    if sources & {"dolch", "fry", "cefr-a1"}:
        return 1
    return 2


def build_level_words(
    hero_word: str,
    dictionary: list[dict[str, Any]],
    all_theme_words: set[str],
    used_wheels: set[str],
    desired_word_count: int,
    target_override: tuple[str, ...] | None = None,
) -> tuple[str, list[dict[str, Any]], Any]:
    """Find a compact wheel and connected grid; use a super-anagram if needed."""
    hero_word = hero_word.lower()
    complete = [entry for entry in dictionary if dictionary_entry_is_complete(entry)]
    by_word = {entry["id"].lower(): entry for entry in complete}
    if hero_word not in by_word:
        raise ValueError(f"hero word lacks complete dictionary data: {hero_word}")
    if target_override is not None:
        if not target_override or target_override[0] != hero_word:
            raise ValueError(f"invalid target override for hero: {hero_word}")
        unknown = set(target_override) - set(by_word)
        if unknown:
            raise ValueError(f"target override lacks dictionary data: {sorted(unknown)}")
        if any(not can_spell(word, hero_word) for word in target_override):
            raise ValueError(f"target override cannot be spelled by hero wheel: {hero_word}")
        selected = [by_word[word] for word in target_override]
        grid = layout([entry["id"].upper() for entry in selected], 10)
        if grid is None:
            raise ValueError(f"target override cannot form a grid: {hero_word}")
        return hero_word.upper(), selected, grid
    wheels = [
        entry for entry in complete
        if len(hero_word) <= len(entry["id"]) <= min(8, len(hero_word) + 4)
        and can_spell(hero_word, entry["id"])
    ]
    wheels.sort(key=lambda entry: (
        entry["id"] in used_wheels,
        0 if entry["id"] == hero_word else 1,
        len(entry["id"]) - len(hero_word),
        source_priority(entry),
        entry.get("difficulty", 9),
        entry["id"],
    ))
    for wheel_entry in wheels:
        wheel = wheel_entry["id"].lower()
        candidates = [entry for entry in complete if can_spell(entry["id"], wheel)]
        candidates.sort(key=lambda entry: (
            0 if entry["id"] == hero_word else 1 if entry["id"] == wheel else 2,
            0 if entry["id"] in all_theme_words else 1,
            source_priority(entry), entry.get("difficulty", 9),
            -len(entry["id"]), entry["id"],
        ))
        selected: list[dict[str, Any]] = []
        for entry in candidates:
            trial = [item["id"].upper() for item in selected + [entry]]
            if layout(trial, 10) is not None:
                selected.append(entry)
            if len(selected) >= desired_word_count:
                break
        words = {entry["id"] for entry in selected}
        if hero_word not in words or wheel not in words or len(selected) < 2:
            continue
        grid = layout([entry["id"].upper() for entry in selected], 10)
        if grid is not None:
            return wheel.upper(), selected, grid
    raise ValueError(f"could not build a two-word crossword for hero: {hero_word}")


def word_meaning(entry: dict[str, Any]) -> dict[str, str]:
    return {
        "word": entry["id"].upper(),
        "definitionEn": require_text(f"{entry['id']}.definitionEn", entry["definitionEn"]),
        "translationBn": require_text(f"{entry['id']}.translationBn", entry["translationBn"]),
        "meaningBn": require_text(f"{entry['id']}.meaningBn", entry["meaningBn"]),
    }


def convert_level(
    category_id: str,
    local_id: int,
    hero: dict[str, Any],
    sentence_en: str,
    sentence_bn: str,
    dictionary: list[dict[str, Any]],
    all_theme_words: set[str],
    used_wheels: set[str],
) -> dict[str, Any]:
    difficulty = DIFFICULTY_BY_ORDER[local_id - 1]
    wheel, selected, grid = build_level_words(
        hero["word"], dictionary, all_theme_words, used_wheels,
        min(5, 2 + difficulty),
        TARGET_WORD_OVERRIDES.get((category_id, hero["word"].lower())),
    )
    used_wheels.add(wheel.lower())
    words = [entry["id"].upper() for entry in selected]
    rows, cols, placements = normalise(grid)
    hero_entry = {entry["id"].lower(): entry for entry in selected}[hero["word"].lower()]
    sentence_en = require_text(f"{category_id}.{hero['word']}.sentenceEn", sentence_en)
    sentence_bn = require_text(f"{category_id}.{hero['word']}.sentenceBn", sentence_bn)
    if not re.search(rf"\b{re.escape(hero['word'])}\b", sentence_en, re.IGNORECASE):
        raise ValueError(
            f"{category_id}.{hero['word']}: authored opening sentence must use hero word"
        )
    used_words = list(words)
    hero_upper = hero["word"].upper()
    if hero_upper not in used_words:
        raise ValueError(f"{category_id}.{hero['word']}: hero is not a playable target")
    return {
        "id": local_id,
        "categoryId": category_id,
        "order": local_id,
        "difficulty": difficulty,
        "ageBand": AGE_BAND_BY_ORDER[local_id - 1],
        "heroWord": {
            "word": hero_upper,
            "definitionEn": require_text(f"{category_id}.{hero['word']}.definition", hero["definition"]),
            "translationBn": require_text(f"{category_id}.{hero['word']}.bengali", hero["bengali"]),
            "meaningBn": require_text(f"{category_id}.{hero['word']}.meaningBn", hero_entry["meaningBn"]),
        },
        "wordMeanings": [word_meaning(entry) for entry in selected],
        "lesson": {
            "sentenceEn": sentence_en,
            "sentenceBn": sentence_bn,
            "usedWords": used_words,
        },
        "letters": wheel,
        "words": words,
        "bonusWords": [],
        "rows": rows,
        "cols": cols,
        "placements": placements,
    }


def build_catalog(source_root: Path) -> dict[str, Any]:
    themes, dictionary = load_source(source_root)
    if list(themes) != list(ICON_KEYS):
        raise ValueError("source category ids changed; review category content first")
    if set(CATEGORY_CONTENT) != set(ICON_KEYS):
        raise ValueError("learning-copy categories do not match category ids")
    if set(CATEGORY_STORIES) != set(ICON_KEYS):
        raise ValueError("category-story copy does not match category ids")
    if set(LEVEL_STORIES) != set(ICON_KEYS):
        raise ValueError("level-story copy does not match category ids")
    all_theme_words = {
        word["word"].lower() for theme in themes.values() for word in theme["words"]
    }
    used_wheels: set[str] = set()
    categories = []
    for order, (category_id, theme) in enumerate(themes.items(), start=1):
        content = CATEGORY_CONTENT[category_id]
        lessons = content["lessons"]
        if len(lessons) != EXPECTED_LEVELS_PER_CATEGORY:
            raise ValueError(f"{category_id}: expected 10 authored lessons")
        theme_by_word = {entry["word"].lower(): entry for entry in theme["words"]}
        unknown = set(lessons) - set(theme_by_word)
        if unknown:
            raise ValueError(f"{category_id}: unknown heroes {sorted(unknown)}")
        levels = [
            convert_level(
                category_id,
                local_id,
                theme_by_word[word],
                LEVEL_STORIES[category_id][local_id]["sentenceEn"],
                LEVEL_STORIES[category_id][local_id]["sentenceBn"],
                dictionary, all_theme_words, used_wheels,
            )
            for local_id, word in enumerate(lessons, start=1)
        ]
        story = CATEGORY_STORIES[category_id]
        story_words = list(dict.fromkeys(
            target
            for level in levels
            for target in level["words"]
        ))
        categories.append({
            "id": category_id,
            "nameEn": theme["display_name"],
            "nameBn": theme["display_name_bn"],
            "iconKey": ICON_KEYS[category_id],
            "order": order,
            "introductionEn": content["introductionEn"],
            "introductionBn": content["introductionBn"],
            "completionEn": content["completionEn"],
            "completionBn": content["completionBn"],
            "completionWords": content["completionWords"],
            "storyEn": require_text(f"{category_id}.storyEn", story.get("storyEn")),
            "storyBn": require_text(f"{category_id}.storyBn", story.get("storyBn")),
            "storyWords": story_words,
            "contentReviewStatus": "draft",
            "levels": levels,
        })
    catalog = {
        "schemaVersion": 2,
        "catalogVersion": CATALOG_VERSION,
        "contentReviewNotice": CONTENT_REVIEW_NOTICE,
        "categories": categories,
    }
    validate_catalog(catalog)
    return catalog


def validate_catalog(catalog: dict[str, Any]) -> None:
    if catalog.get("schemaVersion") != 2:
        raise ValueError("schemaVersion must be 2")
    require_text("catalogVersion", catalog.get("catalogVersion"))
    require_text("contentReviewNotice", catalog.get("contentReviewNotice"))
    categories = catalog.get("categories")
    if not isinstance(categories, list) or len(categories) != 16:
        raise ValueError(f"expected 16 categories, found {len(categories or [])}")
    ids = [category.get("id") for category in categories]
    if len(set(ids)) != len(ids):
        raise ValueError("category ids must be unique")
    if [category.get("order") for category in categories] != list(range(1, 17)):
        raise ValueError("category order must be sequential")
    if {category.get("iconKey") for category in categories} != set(ICON_KEYS.values()):
        raise ValueError("category icon keys changed")

    composite_ids: set[tuple[str, int]] = set()
    for category in categories:
        category_id = category.get("id")
        for field in (
            "id", "nameEn", "nameBn", "iconKey", "introductionEn",
            "introductionBn", "completionEn", "completionBn", "storyEn", "storyBn",
        ):
            require_text(f"category {category_id}.{field}", category.get(field))
        if category.get("contentReviewStatus") != "draft":
            raise ValueError(f"{category_id}: unreviewed copy must remain draft")
        levels = category.get("levels", [])
        if len(levels) != EXPECTED_LEVELS_PER_CATEGORY:
            raise ValueError(f"{category_id}: expected exactly 10 levels")
        expected_order = list(range(1, 11))
        if [level.get("order") for level in levels] != expected_order:
            raise ValueError(f"{category_id}: level order is not sequential")
        if [level.get("id") for level in levels] != expected_order:
            raise ValueError(f"{category_id}: level ids are not sequential")
        if [level.get("difficulty") for level in levels] != list(DIFFICULTY_BY_ORDER):
            raise ValueError(f"{category_id}: invalid difficulty ramp")
        if [level.get("ageBand") for level in levels] != list(AGE_BAND_BY_ORDER):
            raise ValueError(f"{category_id}: invalid age-band ramp")
        heroes = [level.get("heroWord", {}).get("word") for level in levels]
        if len(heroes) != len(set(heroes)):
            raise ValueError(f"{category_id}: hero words must be distinct")
        completion = category.get("completionWords")
        if not isinstance(completion, list) or not completion or not set(completion).issubset(heroes):
            raise ValueError(f"{category_id}: completionWords must be learned heroes")
        expected_story_words = list(dict.fromkeys(
            word
            for level in levels
            for word in level.get("words", [])
        ))
        story_words = category.get("storyWords")
        if story_words != expected_story_words:
            raise ValueError(
                f"{category_id}: storyWords must exactly match every unique target in first-seen order"
            )
        english_story_tokens = set(re.findall(r"[A-Za-z]+", category["storyEn"].upper()))
        missing_story_words = set(story_words) - english_story_tokens
        if missing_story_words:
            raise ValueError(
                f"{category_id}: English story omits targets {sorted(missing_story_words)}"
            )
        require_natural_story(
            f"{category_id}.story",
            category["storyEn"],
            category["storyBn"],
            paragraphs=True,
        )

        for level in levels:
            composite_id = (level.get("categoryId"), level.get("id"))
            if level.get("categoryId") != category_id or composite_id in composite_ids:
                raise ValueError(f"invalid or duplicate level id: {composite_id}")
            composite_ids.add(composite_id)
            letters = level.get("letters", "")
            words = level.get("words", [])
            bonus = level.get("bonusWords", [])
            hero = level.get("heroWord", {})
            if not letters or len(words) < 2 or not level.get("placements"):
                raise ValueError(f"{composite_id}: playable content needs two words")
            if len(words) != len(set(words)) or len(bonus) != len(set(bonus)):
                raise ValueError(f"{composite_id}: duplicate word")
            if set(words + bonus) & DISALLOWED_WORDS or set(words) & set(bonus):
                raise ValueError(f"{composite_id}: invalid learner/bonus word")
            for field in ("word", "definitionEn", "translationBn", "meaningBn"):
                require_text(f"{composite_id}.heroWord.{field}", hero.get(field))
            if hero.get("word") not in words:
                raise ValueError(f"{composite_id}: hero is not a target")

            meanings = level.get("wordMeanings")
            if not isinstance(meanings, list) or len(meanings) != len(words):
                raise ValueError(f"{composite_id}: every target needs one meaning")
            meaning_words = [meaning.get("word") for meaning in meanings]
            if len(meaning_words) != len(set(meaning_words)) or set(meaning_words) != set(words):
                raise ValueError(f"{composite_id}: meanings do not match targets")
            for meaning in meanings:
                for field in ("word", "definitionEn", "translationBn", "meaningBn"):
                    require_text(f"{composite_id}.wordMeaning.{field}", meaning.get(field))

            lesson = level.get("lesson", {})
            require_text(f"{composite_id}.lesson.sentenceEn", lesson.get("sentenceEn"))
            require_text(f"{composite_id}.lesson.sentenceBn", lesson.get("sentenceBn"))
            used = lesson.get("usedWords")
            if used != words:
                raise ValueError(f"{composite_id}: lesson usedWords must exactly match targets")
            lesson_tokens = set(re.findall(r"[A-Za-z]+", lesson["sentenceEn"].upper()))
            missing_lesson_words = set(words) - lesson_tokens
            if missing_lesson_words:
                raise ValueError(
                    f"{composite_id}: English lesson omits targets {sorted(missing_lesson_words)}"
                )
            require_natural_story(
                f"{composite_id}.lesson",
                lesson["sentenceEn"],
                lesson["sentenceBn"],
                paragraphs=False,
            )
            if not all(can_spell(word, letters) for word in words + bonus):
                raise ValueError(f"{composite_id}: word cannot be spelled")
            longest = max(map(len, words))
            if not any(len(word) == longest and Counter(word) == Counter(letters) for word in words):
                raise ValueError(f"{composite_id}: wheel is not a longest target")
            placements = level["placements"]
            if set(words) != {item.get("word") for item in placements} or len(placements) != len(words):
                raise ValueError(f"{composite_id}: placements do not match targets")
            board: dict[tuple[int, int], str] = {}
            for item in placements:
                if item.get("direction") not in ("HORIZONTAL", "VERTICAL"):
                    raise ValueError(f"{composite_id}: invalid direction")
                dr, dc = (0, 1) if item["direction"] == "HORIZONTAL" else (1, 0)
                for index, char in enumerate(item["word"]):
                    cell = (item["row"] + dr * index, item["col"] + dc * index)
                    if not (0 <= cell[0] < level["rows"] and 0 <= cell[1] < level["cols"]):
                        raise ValueError(f"{composite_id}: placement leaves grid")
                    if cell in board and board[cell] != char:
                        raise ValueError(f"{composite_id}: crossing conflict")
                    board[cell] = char
    if len(composite_ids) != 160:
        raise ValueError(f"expected 160 levels, found {len(composite_ids)}")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source-root", type=Path, required=True)
    parser.add_argument("--output", type=Path, default=Path("app/src/main/assets/categories.json"))
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    catalog = build_catalog(args.source_root.resolve())
    rendered = json.dumps(catalog, ensure_ascii=False, indent=2) + "\n"
    if args.check:
        if not args.output.is_file() or args.output.read_text(encoding="utf-8") != rendered:
            raise SystemExit(f"{args.output} is stale; regenerate it")
        print(f"Validated {args.output}: 16 categories, 160 playable levels")
        return
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(rendered, encoding="utf-8")
    print(f"Wrote {args.output}: 16 categories, 160 playable levels")


if __name__ == "__main__":
    main()

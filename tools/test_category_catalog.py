#!/usr/bin/env python3
"""Focused, source-independent tests for the shipped category asset."""

import copy
import json
from pathlib import Path
import re
import sys
import unittest

sys.path.insert(0, str(Path(__file__).parent))
from generate_category_catalog import DISALLOWED_WORDS, validate_catalog  # noqa: E402


ASSET = Path(__file__).parents[1] / "app" / "src" / "main" / "assets" / "categories.json"


class CategoryCatalogTest(unittest.TestCase):
    def setUp(self) -> None:
        self.catalog = json.loads(ASSET.read_text(encoding="utf-8"))

    def test_shipped_catalog_is_valid(self) -> None:
        validate_catalog(self.catalog)

    def test_reviewed_child_safety_blocklist_is_not_shipped(self) -> None:
        self.assertTrue({"ALE", "BOW", "ROB"}.issubset(DISALLOWED_WORDS))
        shipped_targets = {
            word
            for category in self.catalog["categories"]
            for level in category["levels"]
            for word in level["words"] + level["bonusWords"]
        }
        self.assertTrue(shipped_targets.isdisjoint(DISALLOWED_WORDS))

        banned_word = copy.deepcopy(self.catalog)
        banned_word["categories"][0]["levels"][0]["bonusWords"] = ["ALE"]
        with self.assertRaises(ValueError):
            validate_catalog(banned_word)

    def test_reviewed_story_friendly_target_sets_are_shipped(self) -> None:
        expected = {
            ("fish", 9): ["PRAWN", "RAW", "PAN"],
            ("flowers", 3): ["ASTER", "STAR"],
            ("flowers", 6): ["PANSY", "ANY", "SAY"],
            ("flowers", 8): ["THORN", "HOT", "NOT", "ROT"],
            ("flowers", 10): ["ORCHID", "RICH", "HID", "RID"],
            ("trees_plants", 8): ["BUSH", "BUS"],
            ("food_kitchen", 6): ["BOWL", "LOW"],
            ("food_kitchen", 9): ["BREAD", "READ"],
            ("clothing", 5): ["COAT", "CAT", "ACT"],
            ("clothing", 8): ["PANTS", "TAN", "PAT"],
        }
        actual = {
            (category["id"], level["id"]): level["words"]
            for category in self.catalog["categories"]
            for level in category["levels"]
        }
        for key, words in expected.items():
            with self.subTest(key=key):
                self.assertEqual(actual[key], words)

    def test_validator_rejects_schema_and_identity_drift(self) -> None:
        cases = []

        wrong_schema = copy.deepcopy(self.catalog)
        wrong_schema["schemaVersion"] = 1
        cases.append(wrong_schema)

        missing_version = copy.deepcopy(self.catalog)
        missing_version["catalogVersion"] = ""
        cases.append(missing_version)

        duplicate_category = copy.deepcopy(self.catalog)
        duplicate_category["categories"][1]["id"] = duplicate_category["categories"][0]["id"]
        cases.append(duplicate_category)

        wrong_parent = copy.deepcopy(self.catalog)
        wrong_parent["categories"][0]["levels"][0]["categoryId"] = "fish"
        cases.append(wrong_parent)

        for invalid in cases:
            with self.subTest():
                with self.assertRaises(ValueError):
                    validate_catalog(invalid)

    def test_validator_rejects_missing_bilingual_content_and_broken_ramp(self) -> None:
        missing_bengali = copy.deepcopy(self.catalog)
        missing_bengali["categories"][0]["nameBn"] = ""

        missing_intro = copy.deepcopy(self.catalog)
        missing_intro["categories"][0]["introductionBn"] = ""

        missing_story = copy.deepcopy(self.catalog)
        missing_story["categories"][0]["storyBn"] = ""

        missing_hero = copy.deepcopy(self.catalog)
        missing_hero["categories"][0]["levels"][0]["heroWord"]["translationBn"] = ""

        broken_ramp = copy.deepcopy(self.catalog)
        levels = broken_ramp["categories"][0]["levels"]
        levels[2]["difficulty"] = 1

        duplicate_hero = copy.deepcopy(self.catalog)
        levels = duplicate_hero["categories"][0]["levels"]
        levels[1]["heroWord"] = copy.deepcopy(levels[0]["heroWord"])
        levels[1]["heroWord"]["word"] = levels[0]["heroWord"]["word"]
        levels[1]["words"].append(levels[0]["heroWord"]["word"])
        levels[1]["placements"].append(copy.deepcopy(levels[1]["placements"][0]))
        levels[1]["placements"][-1]["word"] = levels[0]["heroWord"]["word"]

        for invalid in (
            missing_bengali,
            missing_intro,
            missing_story,
            missing_hero,
            broken_ramp,
            duplicate_hero,
        ):
            with self.subTest():
                with self.assertRaises(ValueError):
                    validate_catalog(invalid)

    def test_validator_rejects_incomplete_learning_content(self) -> None:
        missing_level = copy.deepcopy(self.catalog)
        missing_level["categories"][0]["levels"].pop()

        missing_meaning = copy.deepcopy(self.catalog)
        missing_meaning["categories"][0]["levels"][0]["wordMeanings"].pop()

        missing_bengali_meaning = copy.deepcopy(self.catalog)
        missing_bengali_meaning["categories"][0]["levels"][0]["wordMeanings"][0][
            "meaningBn"
        ] = ""

        lesson_uses_unknown_word = copy.deepcopy(self.catalog)
        lesson_uses_unknown_word["categories"][0]["levels"][0]["lesson"][
            "usedWords"
        ] = ["UNKNOWN"]

        lesson_omits_hero = copy.deepcopy(self.catalog)
        first_level = lesson_omits_hero["categories"][0]["levels"][0]
        first_level["lesson"]["usedWords"] = [
            word for word in first_level["words"] if word != first_level["heroWord"]["word"]
        ]

        completion_uses_unknown_word = copy.deepcopy(self.catalog)
        completion_uses_unknown_word["categories"][0]["completionWords"] = ["UNKNOWN"]

        story_words_miss_target = copy.deepcopy(self.catalog)
        story_words_miss_target["categories"][0]["storyWords"].pop()

        story_omits_target = copy.deepcopy(self.catalog)
        story_category = story_omits_target["categories"][0]
        required_story_word = story_category["storyWords"][-1]
        story_category["storyEn"] = re.sub(
            rf"\b{re.escape(required_story_word)}\b",
            "",
            story_category["storyEn"],
            flags=re.IGNORECASE,
        )

        lesson_text_omits_target = copy.deepcopy(self.catalog)
        lesson_level = lesson_text_omits_target["categories"][0]["levels"][0]
        required_lesson_word = lesson_level["words"][-1]
        lesson_level["lesson"]["sentenceEn"] = re.sub(
            rf"\b{re.escape(required_lesson_word)}\b",
            "",
            lesson_level["lesson"]["sentenceEn"],
            flags=re.IGNORECASE,
        )

        uppercase_story_target = copy.deepcopy(self.catalog)
        uppercase_category = uppercase_story_target["categories"][0]
        uppercase_word = uppercase_category["storyWords"][0]
        uppercase_category["storyEn"] = re.sub(
            rf"\b{re.escape(uppercase_word)}\b",
            uppercase_word,
            uppercase_category["storyEn"],
            count=1,
            flags=re.IGNORECASE,
        )

        one_sentence_lesson = copy.deepcopy(self.catalog)
        short_level = one_sentence_lesson["categories"][0]["levels"][0]
        short_level["lesson"]["sentenceEn"] = "The cat can act."
        short_level["lesson"]["sentenceBn"] = "বিড়ালটি অভিনয় করতে পারে।"

        falsely_approved = copy.deepcopy(self.catalog)
        falsely_approved["categories"][0]["contentReviewStatus"] = "approved"

        for invalid in (
            missing_level,
            missing_meaning,
            missing_bengali_meaning,
            lesson_uses_unknown_word,
            lesson_omits_hero,
            lesson_text_omits_target,
            one_sentence_lesson,
            completion_uses_unknown_word,
            story_words_miss_target,
            story_omits_target,
            uppercase_story_target,
            falsely_approved,
        ):
            with self.subTest():
                with self.assertRaises(ValueError):
                    validate_catalog(invalid)


if __name__ == "__main__":
    unittest.main()

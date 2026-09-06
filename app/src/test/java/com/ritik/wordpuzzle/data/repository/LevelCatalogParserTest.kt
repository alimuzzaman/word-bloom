package com.ritik.wordpuzzle.data.repository

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelCatalogParserTest {

    private val parser = LevelCatalogParser(Json { ignoreUnknownKeys = true })

    @Test
    fun `schema v2 parses bilingual learning content into domain models`() {
        val catalog = parser.parse(validCatalog())

        assertEquals("2.0", catalog.catalogVersion)
        assertEquals(listOf("fish"), catalog.categories.map { it.id })
        assertEquals("মাছ", catalog.categories.single().nameBn)
        val level = catalog.levelsByCategory.getValue("fish").single()
        assertEquals("CAT", level.heroWord.word)
        assertEquals("A small animal", level.heroWord.definitionEn)
        assertEquals(listOf("CAT"), level.wordMeanings.map { it.word })
        assertEquals("The cat sits.", level.lesson?.sentenceEn)
        assertEquals(listOf("CAT"), level.lesson?.usedWords)
        assertEquals("The cat visits the fish.", catalog.categories.single().storyEn)
        assertEquals(listOf("CAT"), catalog.categories.single().storyWords)
    }

    @Test
    fun `schema v2 rejects a level without complete learning metadata`() {
        val malformed = validCatalog().replace(
            Regex("\\\"wordMeanings\\\"\\s*:\\s*\\[[^]]*\\]"),
            "\"wordMeanings\":[]",
        )

        val failure = assertThrows(IllegalArgumentException::class.java) {
            parser.parse(malformed)
        }
        assertTrue(failure.message.orEmpty().contains("define every target"))
    }

    @Test
    fun `schema v2 rejects category story that omits a target or uses a substring`() {
        val missingStoryWord = validCatalog().replace(
            "\"storyWords\": [\"CAT\"]",
            "\"storyWords\": []",
        )
        val missingFailure = assertThrows(IllegalArgumentException::class.java) {
            parser.parse(missingStoryWord)
        }
        assertTrue(missingFailure.message.orEmpty().contains("story words"))

        val unknownStoryWord = validCatalog().replace(
            "\"storyWords\": [\"CAT\"]",
            "\"storyWords\": [\"FISH\"]",
        )
        val unknownFailure = assertThrows(IllegalArgumentException::class.java) {
            parser.parse(unknownStoryWord)
        }
        assertTrue(unknownFailure.message.orEmpty().contains("story words"))

        val missingBengaliStory = validCatalog().replace(
            "\"storyBn\": \"বিড়ালটি মাছের কাছে যায়।\"",
            "\"storyBn\": \"\"",
        )
        val bengaliFailure = assertThrows(IllegalArgumentException::class.java) {
            parser.parse(missingBengaliStory)
        }
        assertTrue(bengaliFailure.message.orEmpty().contains("bilingual story"))

        val substringStory = validCatalog().replace(
            "The cat visits the fish.",
            "The catapult visits the fish.",
        )
        val substringFailure = assertThrows(IllegalArgumentException::class.java) {
            parser.parse(substringStory)
        }
        assertTrue(substringFailure.message.orEmpty().contains("story"))
    }

    @Test
    fun `schema v2 requires each level target in usedWords and English lesson`() {
        val missingUsedWord = validCatalog().replace(
            "\"usedWords\": [\"CAT\"]",
            "\"usedWords\": []",
        )
        val usedWordsFailure = assertThrows(IllegalArgumentException::class.java) {
            parser.parse(missingUsedWord)
        }
        assertTrue(usedWordsFailure.message.orEmpty().contains("usedWords"))

        val substringLesson = validCatalog().replace(
            "The cat sits.",
            "The catapult sits.",
        )
        val lessonFailure = assertThrows(IllegalArgumentException::class.java) {
            parser.parse(substringLesson)
        }
        assertTrue(lessonFailure.message.orEmpty().contains("lesson"))
    }

    @Test
    fun `unsupported schema is rejected before exposing levels`() {
        val malformed = validCatalog().replace(
            Regex("\\\"schemaVersion\\\"\\s*:\\s*2"),
            "\"schemaVersion\":99",
        )

        val failure = assertThrows(IllegalArgumentException::class.java) {
            parser.parse(malformed)
        }
        assertTrue(failure.message.orEmpty().contains("Unsupported category schema"))
    }

    private fun validCatalog(): String =
        """
        {
          "schemaVersion": 2,
          "catalogVersion": "2.0",
          "contentReviewNotice": "Sample approved content",
          "categories": [{
            "id": "fish",
            "nameEn": "Fish",
            "nameBn": "মাছ",
            "iconKey": "fish",
            "order": 1,
            "introductionEn": "Fish live in water.",
            "introductionBn": "মাছ পানিতে বাস করে।",
            "completionEn": "Fish are useful animals.",
            "completionBn": "মাছ উপকারী প্রাণী।",
            "completionWords": ["CAT"],
            "storyEn": "The cat visits the fish.",
            "storyBn": "বিড়ালটি মাছের কাছে যায়।",
            "storyWords": ["CAT"],
            "contentReviewStatus": "approved",
            "levels": [{
              "id": 1,
              "categoryId": "fish",
              "order": 1,
              "difficulty": 1,
              "ageBand": "6-7",
              "heroWord": {
                "word": "CAT",
                "definitionEn": "A small animal",
                "translationBn": "বিড়াল",
                "meaningBn": "ছোট প্রাণী"
              },
              "wordMeanings": [{
                "word": "CAT",
                "definitionEn": "A small animal",
                "translationBn": "বিড়াল",
                "meaningBn": "ছোট প্রাণী"
              }],
              "lesson": {
                "sentenceEn": "The cat sits.",
                "sentenceBn": "বিড়ালটি বসে।",
                "usedWords": ["CAT"]
              },
              "letters": "CAT",
              "words": ["CAT"],
              "bonusWords": [],
              "rows": 1,
              "cols": 3,
              "placements": [{"word":"CAT","row":0,"col":0,"direction":"HORIZONTAL"}]
            }]
          }]
        }
        """.trimIndent()
}

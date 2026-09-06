package com.ritik.wordpuzzle.ui.learning

import org.junit.Assert.assertEquals
import org.junit.Test

class StoryContentTest {

    @Test
    fun `paragraphs split on blank lines and preserve a line break inside a paragraph`() {
        assertEquals(
            listOf("First line\ncontinues.", "Second paragraph."),
            storyParagraphs("  First line\ncontinues.\n\nSecond paragraph.  "),
        )
    }

    @Test
    fun `empty story produces no paragraphs`() {
        assertEquals(emptyList<String>(), storyParagraphs(" \r\n\r\n "))
    }

    @Test
    fun `merge story keeps the authored lead and body in order`() {
        assertEquals(
            "Fish live near water.\n\nAt the river, children watch the fish.",
            mergeStoryText(" Fish live near water. ", "At the river, children watch the fish."),
        )
    }

    @Test
    fun `merge story does not repeat an exact story opening`() {
        assertEquals(
            "Fish live near water.\n\nAt the river, children watch the fish.",
            mergeStoryText(
                "Fish live near water.",
                "Fish live near water.\n\nAt the river, children watch the fish.",
            ),
        )
    }

    @Test
    fun `glossary de-duplicates words case insensitively and keeps first entry`() {
        val first = LearningWordCardData("fish", "a water animal", "মাছ", "জলের প্রাণী")
        val duplicate = LearningWordCardData("FISH", "a different definition", "মাছ", "অন্য অর্থ")
        val second = LearningWordCardData("pond", "a small body of water", "পুকুর", "ছোট জলাশয়")

        assertEquals(listOf(first, second), dedupeLearningWords(listOf(first, duplicate, second)))
    }

    @Test
    fun `unfinished glossary only includes words already found`() {
        val cat = LearningWordCardData("CAT", "a pet", "বিড়াল", "বিড়াল")
        val act = LearningWordCardData("ACT", "to perform", "অভিনয়", "অভিনয় করা")

        assertEquals(
            listOf(act),
            selectLearningWords(
                foundWords = listOf("act"),
                allWords = listOf(cat, act),
                showAllWords = false,
            ),
        )
    }

    @Test
    fun `completion glossary includes every catalog word in order`() {
        val cat = LearningWordCardData("CAT", "a pet", "বিড়াল", "বিড়াল")
        val act = LearningWordCardData("ACT", "to perform", "অভিনয়", "অভিনয় করা")

        assertEquals(
            listOf(cat, act),
            selectLearningWords(
                foundWords = emptyList(),
                allWords = listOf(cat, act),
                showAllWords = true,
            ),
        )
    }

    @Test
    fun `lesson examples add bilingual usage context only to used words`() {
        val cat = LearningWordCardData("CAT", "a pet", "বিড়াল", "বিড়াল")
        val act = LearningWordCardData("ACT", "to perform", "অভিনয়", "অভিনয় করা")
        val lesson = LessonCardData(
            heroWord = cat,
            sentenceEn = "The cat can act.",
            sentenceBn = "বিড়ালটি অভিনয় করতে পারে।",
            usedWords = listOf("CAT"),
        )

        val enriched = addLessonExamples(listOf(cat, act), lesson)

        assertEquals("The cat can act.", enriched[0].exampleEn)
        assertEquals("বিড়ালটি অভিনয় করতে পারে।", enriched[0].exampleBn)
        assertEquals("", enriched[1].exampleEn)
        assertEquals("", enriched[1].exampleBn)
    }

    @Test
    fun `active lesson index only keeps words visible in the glossary`() {
        val cat = LearningWordCardData("CAT", "a pet", "বিড়াল", "বিড়াল")
        val act = LearningWordCardData("ACT", "to perform", "অভিনয়", "অভিনয় করা")
        val lesson = LessonCardData(
            heroWord = cat,
            sentenceEn = "The cat can act.",
            sentenceBn = "বিড়ালটি অভিনয় করতে পারে।",
            usedWords = listOf("CAT", "ACT"),
        )

        assertEquals(
            listOf("CAT"),
            restrictLessonWords(lesson, listOf(cat))?.usedWords,
        )
    }
}

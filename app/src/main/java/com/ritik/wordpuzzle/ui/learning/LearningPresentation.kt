package com.ritik.wordpuzzle.ui.learning

/**
 * Presentation-time bilingual copy. The catalog keeps the introduction and the
 * longer story as separate fields for compatibility with older servers; the UI
 * can still read them as one continuous passage.
 */
data class MergedStory(
    val english: String,
    val bengali: String,
)

/**
 * Merge two authored passages without repeating an introduction that is already
 * the exact opening of the longer story.
 *
 * This intentionally uses exact, trimmed text rather than fuzzy matching. A
 * human-authored paragraph that merely sounds similar must remain visible.
 */
fun mergeStoryText(introduction: String, story: String): String {
    val lead = introduction.normalizedStoryText()
    val body = story.normalizedStoryText()
    return when {
        lead.isBlank() -> body
        body.isBlank() -> lead
        body == lead || body.startsWith("$lead\n") -> body
        else -> "$lead\n\n$body"
    }
}

fun mergeStory(
    introductionEn: String,
    introductionBn: String,
    storyEn: String,
    storyBn: String,
): MergedStory = MergedStory(
    english = mergeStoryText(introductionEn, storyEn),
    bengali = mergeStoryText(introductionBn, storyBn),
)

/**
 * Stable, case-insensitive glossary de-duplication. The first authored entry
 * wins so catalog order and catalog definitions remain authoritative.
 */
fun dedupeLearningWords(words: List<LearningWordCardData>): List<LearningWordCardData> {
    val seen = mutableSetOf<String>()
    return words.filter { word ->
        val key = word.word.trim().uppercase()
        key.isNotBlank() && seen.add(key)
    }
}

/**
 * Choose the glossary entries a player is allowed to see at the current point
 * in a level. Keep the catalog order so the story and board teach the same path.
 */
fun selectLearningWords(
    foundWords: Collection<String>,
    allWords: List<LearningWordCardData>,
    showAllWords: Boolean,
): List<LearningWordCardData> {
    val uniqueWords = dedupeLearningWords(allWords)
    if (showAllWords) return uniqueWords

    val foundKeys = foundWords
        .asSequence()
        .map { it.trim().uppercase() }
        .filter(String::isNotBlank)
        .toSet()
    return uniqueWords.filter { it.word.trim().uppercase() in foundKeys }
}

/**
 * Add the authored bilingual level sentence to each word it explicitly names.
 * This gives the completion glossary useful usage context without inventing a
 * new definition or example sentence in the UI layer.
 */
fun addLessonExamples(
    words: List<LearningWordCardData>,
    lesson: LessonCardData?,
): List<LearningWordCardData> {
    if (lesson == null || (lesson.sentenceEn.isBlank() && lesson.sentenceBn.isBlank())) return words

    val usedKeys = lesson.usedWords
        .asSequence()
        .map { it.trim().uppercase() }
        .filter(String::isNotBlank)
        .toSet()
    if (usedKeys.isEmpty()) return words

    return words.map { word ->
        if (word.word.trim().uppercase() in usedKeys) {
            word.copy(
                exampleEn = lesson.sentenceEn,
                exampleBn = lesson.sentenceBn,
            )
        } else {
            word
        }
    }
}

/**
 * Keep the story's word index aligned with the glossary visibility for this
 * screen. The authored lesson can name every target word, but an active level
 * must not expose the unsolved entries through that index either.
 */
fun restrictLessonWords(
    lesson: LessonCardData?,
    visibleWords: Collection<LearningWordCardData>,
): LessonCardData? {
    if (lesson == null) return null

    val visibleKeys = visibleWords
        .asSequence()
        .map { it.word.trim().uppercase() }
        .filter(String::isNotBlank)
        .toSet()
    return lesson.copy(
        usedWords = lesson.usedWords.filter { it.trim().uppercase() in visibleKeys },
    )
}

private fun String.normalizedStoryText(): String =
    replace("\r\n", "\n").trim()

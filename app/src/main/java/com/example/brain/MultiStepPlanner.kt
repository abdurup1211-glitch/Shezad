package com.example.brain

/**
 * Single step inside a multi-step cognitive [TaskPlan].
 */
data class TaskStep(
  val index: Int,
  val commandText: String,
  val description: String,
)

/**
 * Multi-step plan decomposed by [MultiStepPlanner] when Sanju combines several goals.
 */
data class TaskPlan(
  val goalSummary: String,
  val steps: List<TaskStep>,
)

/**
 * Decomposes compound Bangla, English, and Banglish instructions into sequential steps
 * (up to 3 safe steps to protect CPU, RAM, and battery).
 */
class MultiStepPlanner {

  private val separators = listOf(" এবং ", " আর ", " তারপর ", " ও ", " and then ", " then ", " and ")

  fun decomposeIfCompound(rawInput: String): TaskPlan? {
    val cleaned = rawInput.replace(Regex("(?i)^max[,:\\s]*"), "").trim()
    if (cleaned.isBlank()) return null

    val lower = cleaned.lowercase()
    if (lower.startsWith("মনে রাখো")) {
      return null
    }

    var parts = listOf(cleaned)
    for (separator in separators) {
      val candidate = cleaned.split(Regex("(?i)$separator")).map { it.trim() }.filter { it.isNotBlank() }
      if (candidate.size in 2..3 && candidate.all { it.length >= 3 }) {
        val isSingleTranslationPhrase =
          (lower.contains("অনুবাদ") || lower.contains("translate")) &&
            candidate.first().none { it.isDigit() || it in listOf('+', '-', '*', '/', '×', '÷') } &&
            !candidate.first().lowercase().let { first ->
              first.contains("রুটিন") || first.contains("রিমাইন্ডার") || first.contains("নোট") || first.contains("ব্রিফিং")
            }
        if (!isSingleTranslationPhrase) {
          parts = candidate
          break
        }
      }
    }

    if (parts.size < 2) return null

    val steps =
      parts.mapIndexed { idx, segment ->
        TaskStep(
          index = idx + 1,
          commandText = segment,
          description = segment,
        )
      }
    return TaskPlan(goalSummary = cleaned, steps = steps)
  }
}

package com.example.conversation

/**
 * Defines MAX's core identity, respectful persona, and non-repetitive human-like phrasing for Sanju.
 */
class PersonalityManager {
  val assistantName: String = "MAX"
  val userNameBangla: String = "সানজু"
  val userNameEnglish: String = "Sanju"

  private val followUpPrompts =
    listOf(
      "সানজু, আপনাকে কি আর কোনোভাবে সাহায্য করতে পারি?",
      "সানজু, আর কোনো কাজ বা সাহায্য লাগলে আমাকে বলুন।",
      "সানজু, এর বাইরে কি আর কিছু করতে হবে?",
    )

  private var followUpIndex = -1

  fun nextFollowUpPrompt(): String {
    followUpIndex = (followUpIndex + 1) % followUpPrompts.size
    return followUpPrompts[followUpIndex]
  }

  fun taskAcknowledgement(taskSummary: String): String =
    "জ্বী সানজু, $taskSummary সম্পন্ন হয়েছে।"
}

package com.example.memory

import com.example.conversation.PersonalityManager
import com.example.utils.ErrorLogger

/**
 * Result returned after processing a Bangla or English voice memory command.
 */
sealed class MemoryCommandResult {
  data class Handled(val replyBangla: String) : MemoryCommandResult()

  data object NotAMemoryCommand : MemoryCommandResult()
}

/**
 * Handles explicit user memory commands:
 * - "MAX, এটা মনে রাখো"
 * - "MAX, এটা ভুলে যাও"
 * - "MAX, আমার Memory দেখাও"
 * - "MAX, সব Memory মুছে দাও"
 */
class MemoryController(
  private val memoryManager: MemoryManager,
  private val personalityManager: PersonalityManager,
  private val errorLogger: ErrorLogger,
) {
  private val sensitiveKeywords =
    listOf("password", "pin", "otp", "cvv", "পাসওয়ার্ড", "পিন", "ব্যাংক")

  fun processCommand(rawCommand: String, explicitSensitiveConsent: Boolean = false): MemoryCommandResult {
    val normalized = rawCommand.trim()
    val lower = normalized.lowercase()

    return when {
      lower.contains("সব memory মুছে দাও") || lower.contains("clear all memory") -> {
        memoryManager.clearAll()
        MemoryCommandResult.Handled("${personalityManager.userNameBangla}, আপনার সব সংরক্ষিত মেমোরি নিরাপদে মুছে ফেলা হয়েছে।")
      }

      lower.contains("আমার memory দেখাও") || lower.contains("show my memory") -> {
        val count = memoryManager.memories.value.size
        val reply =
          if (count == 0) {
            "${personalityManager.userNameBangla}, আপনার মেমোরি ভল্ট এই মুহূর্তে খালি আছে।"
          } else {
            val summary = memoryManager.memories.value.take(3).joinToString(" • ") { "${it.key}: ${it.content}" }
            "${personalityManager.userNameBangla}, আপনার $count টি মেমোরি সংরক্ষিত আছে: $summary"
          }
        MemoryCommandResult.Handled(reply)
      }

      lower.contains("ভুলে যাও") || lower.contains("forget") -> {
        val target = extractPayload(normalized, listOf("ভুলে যাও", "forget"))
        val matched = if (target.isNotBlank()) memoryManager.recallMemories(target) else emptyList()
        matched.forEach { memoryManager.deleteMemory(it.id) }
        val reply =
          if (matched.isNotEmpty()) {
            "${personalityManager.userNameBangla}, আমি '${matched.first().key}' তথ্যটি ভুলে গিয়েছি।"
          } else {
            "${personalityManager.userNameBangla}, মুছে ফেলার জন্য নির্দিষ্ট কোন তথ্যটি ভুলে যাবো তা বলুন।"
          }
        MemoryCommandResult.Handled(reply)
      }

      lower.contains("মনে রাখো") || lower.contains("remember") -> {
        val payload = extractPayload(normalized, listOf("এটা মনে রাখো", "মনে রাখো", "remember this", "remember"))
        if (payload.isBlank()) {
          return MemoryCommandResult.Handled("${personalityManager.userNameBangla}, কী মনে রাখতে হবে তা অনুগ্রহ করে বলুন।")
        }
        if (!explicitSensitiveConsent && sensitiveKeywords.any { payload.lowercase().contains(it) }) {
          errorLogger.logWarning("MemoryController", "Blocked sensitive memory storage without explicit consent.")
          return MemoryCommandResult.Handled(
            "নিরাপত্তার কারণে পাসওয়ার্ড বা পিন নম্বর আপনার আলাদা অনুমতি ছাড়া সংরক্ষণ করা হবে না, ${personalityManager.userNameBangla}।",
          )
        }
        memoryManager.saveMemory(
          key = "Sanju Note",
          content = payload,
          category = MemoryCategory.CONTEXT,
        )
        MemoryCommandResult.Handled("ঠিক আছে ${personalityManager.userNameBangla}, আমি এটি নিরাপদে মনে রেখেছি।")
      }

      else -> MemoryCommandResult.NotAMemoryCommand
    }
  }

  private fun extractPayload(command: String, markers: List<String>): String {
    var cleaned = command.replace(Regex("(?i)^max[,:\\s]*"), "").trim()
    for (marker in markers) {
      val idx = cleaned.lowercase().indexOf(marker.lowercase())
      if (idx >= 0) {
        val after = cleaned.substring(idx + marker.length).trim(' ', ':', '-', '—')
        val before = cleaned.substring(0, idx).trim(' ', ':', '-', '—')
        cleaned = if (after.isNotBlank()) after else before
        break
      }
    }
    return cleaned
  }
}

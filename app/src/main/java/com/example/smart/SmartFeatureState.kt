package com.example.smart

import java.util.UUID

/**
 * Structured reminder item managed by [ReminderManager].
 */
data class MaxReminder(
  val id: String = UUID.randomUUID().toString(),
  val title: String,
  val scheduledLabel: String,
  val completed: Boolean = false,
  val createdAtMillis: Long = System.currentTimeMillis(),
)

/**
 * Structured voice-note metadata stub (never captures audio silently).
 */
data class VoiceNoteDraft(
  val id: String = UUID.randomUUID().toString(),
  val title: String,
  val transcriptNote: String,
  val createdAtMillis: Long = System.currentTimeMillis(),
)

/**
 * Operational state of [SmartFeatureManager].
 */
sealed class SmartFeatureState {
  data object Standby : SmartFeatureState()

  data class TranslationOutput(
    val sourceText: String,
    val translatedText: String,
  ) : SmartFeatureState()

  data class ReminderScheduled(val reminder: MaxReminder) : SmartFeatureState()

  data class NoteSaved(val title: String, val content: String) : SmartFeatureState()

  data class SuggestionsReady(val suggestions: List<String>) : SmartFeatureState()
}

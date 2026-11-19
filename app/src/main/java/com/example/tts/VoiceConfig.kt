package com.example.tts

import java.util.Locale

/**
 * Configuration model for MAX's Bangla-first multi-language Text-to-Speech system.
 */
data class VoiceConfig(
  val primaryLocale: Locale = Locale.forLanguageTag("bn-BD"),
  val secondaryBanglaLocale: Locale = Locale.forLanguageTag("bn-IN"),
  val fallbackLocale: Locale = Locale.US,
  val speechSpeed: Float = 0.92f,
  val pitch: Float = 0.98f,
  val style: SpeechStyle = SpeechStyle.CALM,
  val sentencePauseMillis: Long = 320L,
  val preferNaturalVoice: Boolean = true,
)

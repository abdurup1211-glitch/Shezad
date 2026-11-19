package com.example.tts

/**
 * Rich voice synthesis state reported by [TTSManager].
 */
sealed class VoiceState {
  data object Uninitialized : VoiceState()

  data object Initializing : VoiceState()

  data class Ready(
    val activeLocaleTag: String,
    val usingFallbackVoice: Boolean = false,
  ) : VoiceState()

  data class Speaking(
    val currentSentence: String,
    val style: SpeechStyle,
  ) : VoiceState()

  data class Paused(
    val remainingSentences: List<String>,
  ) : VoiceState()

  data object Disabled : VoiceState()

  data class Error(val reason: String) : VoiceState()
}

package com.example.wakeword

/**
 * Operational state of [WakeWordManager].
 */
sealed class WakeWordState {
  data object Idle : WakeWordState()

  data class StandbyArmed(val wakePhrase: String) : WakeWordState()

  data class Detected(
    val keyword: String,
    val confidence: Float,
    val detectedAtMillis: Long = System.currentTimeMillis(),
  ) : WakeWordState()

  data object PrivacyLocked : WakeWordState()

  data object UnavailableFallback : WakeWordState()

  data class Error(val reason: String) : WakeWordState()
}

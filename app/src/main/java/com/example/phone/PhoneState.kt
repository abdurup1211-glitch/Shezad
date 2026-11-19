package com.example.phone

/**
 * Operational state of [PhoneManager].
 */
sealed class PhoneState {
  data object PrivacyLocked : PhoneState()

  data object Standby : PhoneState()

  data class ConfirmationRequired(
    val actionTitle: String,
    val detail: String,
  ) : PhoneState()

  data class CalculationResult(
    val expression: String,
    val result: String,
  ) : PhoneState()

  data class StopwatchStatus(
    val isRunning: Boolean,
    val elapsedMillis: Long,
  ) : PhoneState()

  data class FlashlightStatus(
    val enabled: Boolean,
    val supported: Boolean,
  ) : PhoneState()
}

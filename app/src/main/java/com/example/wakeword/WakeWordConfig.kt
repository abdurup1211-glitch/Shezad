package com.example.wakeword

/**
 * Configuration for the MAX wake-word detection engine.
 */
data class WakeWordConfig(
  val wakePhrase: String = "MAX",
  val sensitivity: Float = 0.65f,
  val audioCapturePermitted: Boolean = false,
  val standbyBatteryOptimized: Boolean = true,
  val fallbackToManualTrigger: Boolean = true,
)

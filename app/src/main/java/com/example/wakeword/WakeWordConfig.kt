package com.example.wakeword

/**
 * Configuration for the MAX wake-word detection engine.
 */
data class WakeWordConfig(
  val wakePhrase: String = "MAX",
  val aliases: Set<String> = setOf("max", "hey max", "hi max", "ok max", "ম্যাক্স", "হ্যালো ম্যাক্স"),
  val sensitivity: Float = 0.65f,
  val audioCapturePermitted: Boolean = false,
  val standbyBatteryOptimized: Boolean = true,
  val fallbackToManualTrigger: Boolean = true,
)

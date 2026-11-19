package com.example.tts

/**
 * Speaking styles supported by MAX's voice synthesis layer.
 */
enum class SpeechStyle(
  val speedMultiplier: Float,
  val pitchMultiplier: Float,
  val sentencePauseMillis: Long,
  val clausePauseMillis: Long = 140L,
) {
  FRIENDLY(speedMultiplier = 0.94f, pitchMultiplier = 1.03f, sentencePauseMillis = 280L, clausePauseMillis = 135L),
  CALM(speedMultiplier = 0.88f, pitchMultiplier = 0.97f, sentencePauseMillis = 360L, clausePauseMillis = 165L),
  HELPFUL(speedMultiplier = 0.95f, pitchMultiplier = 1.00f, sentencePauseMillis = 260L, clausePauseMillis = 125L),
  CARING(speedMultiplier = 0.86f, pitchMultiplier = 0.99f, sentencePauseMillis = 390L, clausePauseMillis = 180L),
  RESPECTFUL(speedMultiplier = 0.90f, pitchMultiplier = 0.98f, sentencePauseMillis = 340L, clausePauseMillis = 155L),
}

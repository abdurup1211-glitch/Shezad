package com.example.tts

/**
 * Speaking styles supported by MAX's voice synthesis layer.
 */
enum class SpeechStyle(
  val speedMultiplier: Float,
  val pitchMultiplier: Float,
  val sentencePauseMillis: Long,
) {
  FRIENDLY(speedMultiplier = 0.94f, pitchMultiplier = 1.03f, sentencePauseMillis = 280L),
  CALM(speedMultiplier = 0.88f, pitchMultiplier = 0.96f, sentencePauseMillis = 360L),
  HELPFUL(speedMultiplier = 0.95f, pitchMultiplier = 1.0f, sentencePauseMillis = 260L),
  CARING(speedMultiplier = 0.86f, pitchMultiplier = 0.98f, sentencePauseMillis = 380L),
  RESPECTFUL(speedMultiplier = 0.90f, pitchMultiplier = 0.97f, sentencePauseMillis = 340L),
}

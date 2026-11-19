package com.example.emotion

import com.example.tts.SpeechStyle

/**
 * Maps an [EmotionState] to voice pacing and empathetic phrasing characteristics
 * for natural, non-robotic responses.
 */
data class ResponseStyle(
  val emotion: EmotionState,
  val speechStyle: SpeechStyle,
  val targetSpeed: Float,
  val targetPitch: Float,
  val warmOpeningPool: List<String>,
) {
  companion object {
    fun forEmotion(emotion: EmotionState): ResponseStyle =
      when (emotion) {
        EmotionState.HAPPY ->
          ResponseStyle(
            emotion = emotion,
            speechStyle = SpeechStyle.FRIENDLY,
            targetSpeed = 0.94f,
            targetPitch = 1.03f,
            warmOpeningPool =
              listOf(
                "শুনে খুব ভালো লাগল, সানজু।",
                "আলহামদুলিল্লাহ, এটা দারুণ খবর সানজু।",
                "খুব সুন্দর, সানজু! এভাবেই এগিয়ে চলুন।",
              ),
          )
        EmotionState.EXCITED ->
          ResponseStyle(
            emotion = emotion,
            speechStyle = SpeechStyle.FRIENDLY,
            targetSpeed = 0.96f,
            targetPitch = 1.04f,
            warmOpeningPool =
              listOf(
                "অসাধারণ সানজু! আমি সত্যিই আনন্দিত।",
                "দারুণ হয়েছে সানজু! চলুন পরের ধাপে যাই।",
              ),
          )
        EmotionState.CARING, EmotionState.EMPATHETIC ->
          ResponseStyle(
            emotion = emotion,
            speechStyle = SpeechStyle.CARING,
            targetSpeed = 0.86f,
            targetPitch = 0.97f,
            warmOpeningPool =
              listOf(
                "চিন্তা করবেন না সানজু, আমি আপনার সাথেই আছি।",
                "একটু শান্ত হয়ে নিন সানজু, ধীরে ধীরে আমরা সব ঠিক করে নেবো।",
              ),
          )
        EmotionState.SUPPORTIVE ->
          ResponseStyle(
            emotion = emotion,
            speechStyle = SpeechStyle.HELPFUL,
            targetSpeed = 0.90f,
            targetPitch = 0.99f,
            warmOpeningPool =
              listOf(
                "অবশ্যই সানজু, আমি সাহায্য করছি।",
                "একসাথে কাজটা করে ফেলি, সানজু।",
              ),
          )
        EmotionState.SORRY ->
          ResponseStyle(
            emotion = emotion,
            speechStyle = SpeechStyle.CALM,
            targetSpeed = 0.85f,
            targetPitch = 0.95f,
            warmOpeningPool =
              listOf(
                "কোনো সমস্যা নেই সানজু, একদম চিন্তা করবেন না।",
                "ঠিক আছে সানজু, আমি বুঝতে পারছি।",
              ),
          )
        EmotionState.RESPECTFUL, EmotionState.CALM, EmotionState.NEUTRAL,
        EmotionState.FOCUSED, EmotionState.CURIOUS, EmotionState.ALERT ->
          ResponseStyle(
            emotion = emotion,
            speechStyle = SpeechStyle.RESPECTFUL,
            targetSpeed = 0.90f,
            targetPitch = 0.98f,
            warmOpeningPool =
              listOf(
                "জি সানজু, বলুন।",
                "আমি শুনছি সানজু।",
              ),
          )
      }
  }
}

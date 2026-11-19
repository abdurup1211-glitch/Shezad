package com.example.emotion

/**
 * Lightweight, privacy-safe mood detector analyzing text signals in Bangla and English.
 * Never accesses microphone, camera, gallery, files, or location.
 */
class EmotionDetector {

  fun detectFromText(text: String): EmotionState {
    val lower = text.lowercase()
    return when {
      lower.contains("দুঃখিত") || lower.contains("sorry") || lower.contains("মাফ") ||
        lower.contains("mistake") || lower.contains("ভুল") -> EmotionState.SORRY

      lower.contains("মন খারাপ") || lower.contains("চিন্তা") || lower.contains("sad") ||
        lower.contains("worried") || lower.contains("stress") || lower.contains("কষ্ট") ||
        lower.contains("tired") || lower.contains("ক্লান্ত") -> EmotionState.CARING

      lower.contains("সাহায্য") || lower.contains("help") || lower.contains("পারছি না") ||
        lower.contains("সমস্যা") || lower.contains("problem") -> EmotionState.SUPPORTIVE

      lower.contains("দারুণ") || lower.contains("অসাধারণ") || lower.contains("awesome") ||
        lower.contains("excited") || lower.contains("great news") || lower.contains("উৎসাহ") ->
        EmotionState.EXCITED

      lower.contains("খুশি") || lower.contains("ভালো আছি") || lower.contains("আলহামদুলিল্লাহ") ||
        lower.contains("happy") || lower.contains("good") || lower.contains("thanks") ||
        lower.contains("ধন্যবাদ") -> EmotionState.HAPPY

      lower.contains("সালাম") || lower.contains("স্যার") || lower.contains("আসসালামু") ->
        EmotionState.RESPECTFUL

      lower.contains("?") || lower.contains("কিভাবে") || lower.contains("কেন") ->
        EmotionState.CURIOUS

      text.isNotBlank() -> EmotionState.CALM
      else -> EmotionState.RESPECTFUL
    }
  }
}

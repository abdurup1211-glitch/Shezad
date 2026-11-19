package com.example.emotion

/**
 * Emotional and conversational states supported by MAX.
 * Preserves Step 1 states alongside Step 4 emotional intelligence states.
 */
enum class EmotionState(val label: String) {
  NEUTRAL("Neutral"),
  FOCUSED("Focused"),
  CURIOUS("Curious"),
  EMPATHETIC("Empathetic"),
  ALERT("Alert"),
  HAPPY("Happy"),
  CALM("Calm"),
  CARING("Caring"),
  SUPPORTIVE("Supportive"),
  EXCITED("Excited"),
  SORRY("Sorry"),
  RESPECTFUL("Respectful"),
}

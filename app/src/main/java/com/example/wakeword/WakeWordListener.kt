package com.example.wakeword

/**
 * Callback interface notified when the "MAX" wake word subsystem transitions state
 * or detects the wake keyword.
 */
interface WakeWordListener {
  /**
   * Invoked when the wake word [keyword] is detected with [confidence] in `[0f, 1f]`.
   */
  fun onWakeWordDetected(keyword: String, confidence: Float)

  /**
   * Invoked whenever [WakeWordManager] changes its [WakeWordState].
   */
  fun onWakeWordStateChanged(state: WakeWordState)
}

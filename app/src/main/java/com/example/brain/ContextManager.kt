package com.example.brain

import com.example.emotion.EmotionState
import com.example.internet.SearchVertical
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single conversation turn retained in active short-term memory while a session is open.
 */
data class ActiveConversationTurn(
  val speaker: String,
  val text: String,
  val intent: UserIntent,
  val emotion: EmotionState,
  val timestampMillis: Long = System.currentTimeMillis(),
)

/**
 * Tracks a missing piece of information MAX asked Sanju to clarify.
 */
data class PendingClarification(
  val intent: UserIntent,
  val questionAsked: String,
  val vertical: SearchVertical = SearchVertical.WEB,
  val requiresInternet: Boolean = false,
)

/**
 * Tracks a pending online task awaiting Sanju's explicit internet permission.
 */
data class PendingInternetRequest(
  val query: String,
  val vertical: SearchVertical,
  val originalIntent: UserIntent,
)

/**
 * Immutable snapshot of MAX's current conversation context.
 */
data class BrainContextSnapshot(
  val recentTurns: List<ActiveConversationTurn> = emptyList(),
  val activeTopic: String? = null,
  val lastIntent: UserIntent? = null,
  val pendingClarification: PendingClarification? = null,
  val pendingInternetRequest: PendingInternetRequest? = null,
)

/**
 * Maintains bounded active conversation context across multiple turns using minimal RAM.
 */
class ContextManager(
  private val maxTurns: Int = 12,
) {

  private val _snapshot = MutableStateFlow(BrainContextSnapshot())
  val snapshot: StateFlow<BrainContextSnapshot> = _snapshot.asStateFlow()

  fun recordTurn(
    speaker: String,
    text: String,
    intent: UserIntent,
    emotion: EmotionState,
    topic: String? = null,
  ) {
    val turn = ActiveConversationTurn(speaker = speaker, text = text, intent = intent, emotion = emotion)
    val current = _snapshot.value
    _snapshot.value =
      current.copy(
        recentTurns = (current.recentTurns + turn).takeLast(maxTurns),
        activeTopic = topic?.takeIf { it.isNotBlank() } ?: current.activeTopic,
        lastIntent = intent,
      )
  }

  fun setPendingClarification(clarification: PendingClarification?) {
    _snapshot.value = _snapshot.value.copy(pendingClarification = clarification)
  }

  fun setPendingInternetRequest(request: PendingInternetRequest?) {
    _snapshot.value = _snapshot.value.copy(pendingInternetRequest = request)
  }

  fun clearSession() {
    _snapshot.value = BrainContextSnapshot()
  }
}

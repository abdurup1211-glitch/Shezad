package com.example.conversation

import com.example.emotion.EmotionAnalyzer
import com.example.emotion.EmotionState
import com.example.memory.MemoryManager
import com.example.utils.ErrorLogger
import com.example.utils.MaxModule
import com.example.utils.ModuleCategory
import com.example.utils.ModuleDescriptor
import com.example.utils.ModuleStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

/**
 * Speaker role in a structured MAX conversation session.
 */
enum class SenderRole {
  USER,
  MAX_CORE,
  SYSTEM,
}

/**
 * Structured message model ready for future NLU and LLM adapters.
 */
data class ConversationMessage(
  val id: String = UUID.randomUUID().toString(),
  val role: SenderRole,
  val text: String,
  val emotion: EmotionState = EmotionState.NEUTRAL,
  val timestampMillis: Long = System.currentTimeMillis(),
)

/**
 * Foundation contract for MAX's dialogue state and intent routing pipeline.
 */
interface ConversationEngine : MaxModule {
  val transcript: StateFlow<List<ConversationMessage>>

  fun recordSystemEvent(message: String)

  fun clearSession()
}

/**
 * Clean foundation implementation connecting [MemoryManager] and [EmotionAnalyzer]
 * without invoking external models prematurely.
 */
class FoundationConversationEngine(
  private val memoryManager: MemoryManager,
  private val emotionAnalyzer: EmotionAnalyzer,
  private val errorLogger: ErrorLogger,
) : ConversationEngine {

  private val _transcript = MutableStateFlow<List<ConversationMessage>>(emptyList())
  override val transcript: StateFlow<List<ConversationMessage>> = _transcript.asStateFlow()

  override val descriptor: ModuleDescriptor
    get() =
      ModuleDescriptor(
        id = "conversation_engine",
        displayName = "ConversationEngine",
        packageName = "com.example.conversation",
        category = ModuleCategory.COGNITION_AND_MEMORY,
        status = ModuleStatus.STANDBY,
        summary = "Dialogue session pipeline bridging memory (${memoryManager.memories.value.size}) & tone.",
      )

  override fun recordSystemEvent(message: String) {
    val tone = emotionAnalyzer.analyzeTextSignal(message)
    val entry =
      ConversationMessage(
        role = SenderRole.SYSTEM,
        text = message,
        emotion = tone.primaryEmotion,
      )
    _transcript.update { it + entry }
    errorLogger.logInfo("ConversationEngine", "Recorded architectural session event.")
  }

  override fun clearSession() {
    _transcript.value = emptyList()
    emotionAnalyzer.resetToNeutral()
  }
}

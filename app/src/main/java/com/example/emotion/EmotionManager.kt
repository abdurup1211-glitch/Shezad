package com.example.emotion

import com.example.tts.TTSManager
import com.example.utils.ErrorLogger
import com.example.utils.MaxModule
import com.example.utils.ModuleCategory
import com.example.utils.ModuleDescriptor
import com.example.utils.ModuleStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Coordinates emotional understanding across MAXCore, ConversationManager, and TTSManager.
 */
interface EmotionManager : MaxModule {
  val currentStyle: StateFlow<ResponseStyle>
  val currentEmotion: StateFlow<EmotionReading>

  fun evaluateUserMood(text: String): ResponseStyle

  fun applyEmotionalVoiceTuning(style: ResponseStyle)

  fun formatEmpatheticReply(userInput: String, coreMessage: String): String

  fun reset()
}

/**
 * Clean, low-memory implementation of [EmotionManager].
 */
class DefaultEmotionManager(
  private val emotionAnalyzer: EmotionAnalyzer,
  private val ttsManager: TTSManager,
  private val errorLogger: ErrorLogger,
  private val detector: EmotionDetector = EmotionDetector(),
) : EmotionManager {

  private var lastOpeningIndex = -1

  private val _currentStyle = MutableStateFlow(ResponseStyle.forEmotion(EmotionState.RESPECTFUL))
  override val currentStyle: StateFlow<ResponseStyle> = _currentStyle.asStateFlow()

  override val currentEmotion: StateFlow<EmotionReading> = emotionAnalyzer.currentEmotion

  override val descriptor: ModuleDescriptor
    get() =
      ModuleDescriptor(
        id = "emotion_manager",
        displayName = "EmotionManager",
        packageName = "com.example.emotion",
        category = ModuleCategory.COGNITION_AND_MEMORY,
        status = ModuleStatus.READY,
        summary = "Emotion & mood adaptive response controller (${_currentStyle.value.emotion.label}).",
      )

  override fun evaluateUserMood(text: String): ResponseStyle {
    val detected = detector.detectFromText(text)
    emotionAnalyzer.analyzeTextSignal(text)
    val style = ResponseStyle.forEmotion(detected)
    _currentStyle.value = style
    applyEmotionalVoiceTuning(style)
    errorLogger.logInfo("EmotionManager", "Mood evaluated -> ${detected.label}")
    return style
  }

  override fun applyEmotionalVoiceTuning(style: ResponseStyle) {
    ttsManager.setSpeechSpeed(style.targetSpeed)
    ttsManager.setPitch(style.targetPitch)
  }

  override fun formatEmpatheticReply(userInput: String, coreMessage: String): String {
    val style = evaluateUserMood(userInput)
    val pool = style.warmOpeningPool
    val nextIndex = if (pool.size > 1) (lastOpeningIndex + 1) % pool.size else 0
    lastOpeningIndex = nextIndex
    val opening = pool.getOrElse(nextIndex) { "" }
    return if (opening.isNotBlank() && !coreMessage.startsWith(opening)) {
      "$opening\n$coreMessage"
    } else {
      coreMessage
    }
  }

  override fun reset() {
    emotionAnalyzer.resetToNeutral()
    _currentStyle.value = ResponseStyle.forEmotion(EmotionState.RESPECTFUL)
  }
}

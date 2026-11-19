package com.example.conversation

import com.example.emotion.EmotionManager
import com.example.emotion.EmotionState
import com.example.tts.SpeechStyle
import com.example.tts.TTSManager
import com.example.utils.ErrorLogger
import com.example.utils.MaxModule
import com.example.utils.ModuleCategory
import com.example.utils.ModuleDescriptor
import com.example.utils.ModuleStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Orchestrates MAX's human-like conversation lifecycle:
 * Wake -> Greeting -> Wait -> (Busy Farewell or Task) -> Follow-up -> Day/Night Farewell -> Wake Mode.
 */
interface ConversationManager : MaxModule {
  val state: StateFlow<ConversationState>
  val transcript: StateFlow<List<ConversationMessage>>
  val personalityManager: PersonalityManager

  fun onWakeWordActivated(speakGreeting: Boolean = true)

  fun handleUserInput(input: String, responseOverride: String? = null)

  fun completeTaskAndAskFollowUp(taskSummary: String, speakPrompt: Boolean = true)

  fun triggerNoResponseTimeout(speakMessage: Boolean = true)

  fun concludeSession(speakFarewell: Boolean = true)

  fun returnToWakeMode()
}

/**
 * Modular implementation of [ConversationManager] connecting [TTSManager], [EmotionManager],
 * [PersonalityManager], [GreetingManager], [FarewellManager], and [ConversationEngine].
 */
class DefaultConversationManager(
  private val conversationEngine: ConversationEngine,
  private val ttsManager: TTSManager,
  private val emotionManager: EmotionManager,
  override val personalityManager: PersonalityManager,
  val greetingManager: GreetingManager,
  val farewellManager: FarewellManager,
  private val errorLogger: ErrorLogger,
  private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
) : ConversationManager {

  private var waitTimeoutJob: Job? = null

  private val _state = MutableStateFlow<ConversationState>(ConversationState.WakeStandby)
  override val state: StateFlow<ConversationState> = _state.asStateFlow()

  private val _transcript = MutableStateFlow<List<ConversationMessage>>(emptyList())
  override val transcript: StateFlow<List<ConversationMessage>> = _transcript.asStateFlow()

  override val descriptor: ModuleDescriptor
    get() =
      ModuleDescriptor(
        id = "conversation_manager",
        displayName = "ConversationManager",
        packageName = "com.example.conversation",
        category = ModuleCategory.COGNITION_AND_MEMORY,
        status = ModuleStatus.READY,
        summary = "Natural Bangla/English dialogue lifecycle & personality orchestrator.",
      )

  override fun onWakeWordActivated(speakGreeting: Boolean) {
    cancelWaitTimer()
    val greeting = greetingManager.nextWakeGreeting()
    _state.value = ConversationState.GreetingUser(greeting)
    appendMessage(SenderRole.MAX_CORE, greeting, EmotionState.RESPECTFUL)
    if (speakGreeting) {
      ttsManager.speak(greeting, SpeechStyle.CARING)
    }
    _state.value = ConversationState.ListeningForCommand
    scheduleIdleTimeout()
    errorLogger.logInfo("ConversationManager", "Greeted Sanju and transitioned to ListeningForCommand.")
  }

  override fun handleUserInput(input: String, responseOverride: String?) {
    cancelWaitTimer()
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return

    val style = emotionManager.evaluateUserMood(trimmed)
    appendMessage(SenderRole.USER, trimmed, style.emotion)
    _state.value = ConversationState.ProcessingTask(trimmed)

    val reply =
      responseOverride
        ?: emotionManager.formatEmpatheticReply(
          userInput = trimmed,
          coreMessage = "আমি আপনার কথাটি বুঝেছি, সানজু।",
        )
    appendMessage(SenderRole.MAX_CORE, reply, style.emotion)
    ttsManager.speak(reply, style.speechStyle)
  }

  override fun completeTaskAndAskFollowUp(taskSummary: String, speakPrompt: Boolean) {
    cancelWaitTimer()
    val followUp = personalityManager.nextFollowUpPrompt()
    val combined = "${personalityManager.taskAcknowledgement(taskSummary)}\n$followUp"
    _state.value = ConversationState.AskingFollowUp(followUp)
    appendMessage(SenderRole.MAX_CORE, combined, EmotionState.SUPPORTIVE)
    if (speakPrompt) {
      ttsManager.speak(combined, SpeechStyle.CALM)
    }
    scheduleFollowUpTimeout()
  }

  override fun triggerNoResponseTimeout(speakMessage: Boolean) {
    cancelWaitTimer()
    val busyMsg = greetingManager.noResponseTimeoutMessage()
    _state.value = ConversationState.Farewell(busyMsg)
    appendMessage(SenderRole.MAX_CORE, busyMsg, EmotionState.CARING)
    if (speakMessage) {
      ttsManager.speak(busyMsg, SpeechStyle.CARING)
    }
    returnToWakeMode()
  }

  override fun concludeSession(speakFarewell: Boolean) {
    cancelWaitTimer()
    val farewell = farewellManager.buildFarewell()
    _state.value = ConversationState.Farewell(farewell)
    appendMessage(SenderRole.MAX_CORE, farewell, EmotionState.RESPECTFUL)
    if (speakFarewell) {
      ttsManager.speak(farewell, SpeechStyle.CALM)
    }
    returnToWakeMode()
  }

  override fun returnToWakeMode() {
    cancelWaitTimer()
    _state.value = ConversationState.WakeStandby
    errorLogger.logInfo("ConversationManager", "Returned cleanly to wake standby mode.")
  }

  private fun scheduleIdleTimeout(waitMillis: Long = 12_000L) {
    waitTimeoutJob =
      scope.launch {
        delay(waitMillis)
        if (_state.value == ConversationState.ListeningForCommand) {
          triggerNoResponseTimeout(speakMessage = false)
        }
      }
  }

  private fun scheduleFollowUpTimeout(waitMillis: Long = 10_000L) {
    waitTimeoutJob =
      scope.launch {
        delay(waitMillis)
        if (_state.value is ConversationState.AskingFollowUp) {
          concludeSession(speakFarewell = false)
        }
      }
  }

  private fun cancelWaitTimer() {
    waitTimeoutJob?.cancel()
    waitTimeoutJob = null
  }

  private fun appendMessage(role: SenderRole, text: String, emotion: EmotionState) {
    val msg = ConversationMessage(role = role, text = text, emotion = emotion)
    _transcript.value = (_transcript.value + msg).takeLast(40)
    conversationEngine.recordSystemEvent("${role.name}: $text")
  }
}

package com.example.repository

import android.content.Intent
import com.example.apps.AppState
import com.example.apps.SupportedExternalApp
import com.example.camera.CameraActionOutcome
import com.example.camera.CameraMode
import com.example.conversation.ConversationMessage
import com.example.conversation.ConversationState
import com.example.core.MAXCore
import com.example.core.MaxCoreState
import com.example.emotion.EmotionReading
import com.example.internet.InternetActionOutcome
import com.example.internet.SearchVertical
import com.example.memory.MemoryCategory
import com.example.memory.MemoryEntry
import com.example.permissions.AssistantPermission
import com.example.permissions.PermissionState
import com.example.phone.PhoneState
import com.example.settings.MaxSettings
import com.example.settings.ThemeMode
import com.example.smart.MaxReminder
import com.example.smart.SmartFeatureState
import com.example.speech.SpeechState
import com.example.tts.SpeechStyle
import com.example.tts.TtsState
import com.example.utils.ErrorLogEntry
import com.example.utils.LogSeverity
import com.example.wakeword.WakeWordState
import kotlinx.coroutines.flow.StateFlow

/**
 * MVVM Repository mediating between [MAXCore] subsystems and presentation ViewModels.
 */
class MaxSystemRepository(
  private val maxCore: MAXCore,
) {
  val coreState: StateFlow<MaxCoreState> = maxCore.coreState
  val settings: StateFlow<MaxSettings> = maxCore.settingsManager.settings
  val permissionStates: StateFlow<Map<AssistantPermission, PermissionState>> =
    maxCore.permissionManager.permissionStates
  val wakeWordState: StateFlow<WakeWordState> = maxCore.wakeWordManager.state
  val speechState: StateFlow<SpeechState> = maxCore.speechManager.state
  val ttsState: StateFlow<TtsState> = maxCore.ttsManager.state
  val memories: StateFlow<List<MemoryEntry>> = maxCore.memoryManager.memories
  val transcript: StateFlow<List<ConversationMessage>> = maxCore.conversationManager.transcript
  val conversationState: StateFlow<ConversationState> = maxCore.conversationManager.state
  val emotionState: StateFlow<EmotionReading> = maxCore.emotionAnalyzer.currentEmotion
  val logs: StateFlow<List<ErrorLogEntry>> = maxCore.errorLogger.logs
  val reminders: StateFlow<List<MaxReminder>> = maxCore.smartFeatureManager.reminderManager.reminders
  val smartState: StateFlow<SmartFeatureState> = maxCore.smartFeatureManager.state

  fun runDiagnostics(): MaxCoreState = maxCore.runDiagnostics()

  fun enforcePrivacyLock() {
    maxCore.enforcePrivacyLock()
  }

  fun triggerWakeWordMax(): Boolean {
    val result = maxCore.triggerWakeWordMax(speakGreeting = settings.value.ttsEnabled)
    maxCore.refreshRegistry()
    return result
  }

  fun submitUserCommand(command: String): String =
    maxCore.processUserCommand(command = command, explicitUserRequest = true)

  fun concludeConversation() {
    maxCore.conversationManager.concludeSession(speakFarewell = settings.value.ttsEnabled)
    maxCore.refreshRegistry()
  }

  fun getSmartSuggestions(): List<String> = maxCore.smartFeatureManager.refreshSuggestions()

  fun prepareAppLaunch(app: SupportedExternalApp, confirmed: Boolean): AppState =
    maxCore.appManager.openApp(app, explicitUserRequest = confirmed)

  fun prepareSearch(query: String, vertical: SearchVertical, confirmed: Boolean): InternetActionOutcome =
    maxCore.internetManager.prepareSearch(query, vertical, explicitUserRequest = confirmed)

  fun prepareCamera(mode: CameraMode, confirmed: Boolean): CameraActionOutcome =
    maxCore.cameraManager.requestCameraAction(mode, explicitUserRequest = confirmed)

  fun releaseCamera() {
    maxCore.cameraManager.releaseResources()
  }

  fun requestGalleryPicker(confirmed: Boolean): Boolean =
    maxCore.galleryManager.requestOpenGallery(explicitUserRequest = confirmed)

  fun buildDialerIntent(number: String, confirmed: Boolean): Intent? =
    maxCore.phoneManager.callManager.buildDialIntent(number, userConfirmed = confirmed)

  fun evaluateCalculation(expression: String): PhoneState.CalculationResult? =
    maxCore.phoneManager.calculate(expression, explicitUserRequest = true)

  fun addReminder(title: String, whenLabel: String): MaxReminder {
    val item = maxCore.smartFeatureManager.reminderManager.addReminder(title, whenLabel)
    maxCore.conversationManager.completeTaskAndAskFollowUp("রিমাইন্ডার সেট ($title)", speakPrompt = false)
    return item
  }

  suspend fun setPrivacySafeMode(enabled: Boolean) {
    maxCore.settingsManager.setPrivacySafeMode(enabled)
    maxCore.permissionManager.setPrivacyLockActive(enabled)
    maxCore.wakeWordManager.prepareEngine()
    maxCore.speechManager.prepare()
    maxCore.refreshRegistry()
  }

  suspend fun setWakeWordStandbyEnabled(enabled: Boolean) {
    maxCore.settingsManager.setWakeWordStandbyEnabled(enabled)
    maxCore.wakeWordManager.prepareEngine()
    maxCore.refreshRegistry()
  }

  suspend fun setTtsEnabled(enabled: Boolean) {
    maxCore.settingsManager.setTtsEnabled(enabled)
    if (enabled) {
      maxCore.ttsManager.initialize()
    } else {
      maxCore.ttsManager.stopSpeaking()
    }
    maxCore.refreshRegistry()
  }

  suspend fun setTtsSpeechRate(rate: Float) {
    maxCore.settingsManager.setTtsSpeechRate(rate)
    maxCore.ttsManager.setSpeechSpeed(rate)
  }

  suspend fun setMemoryEnabled(enabled: Boolean) {
    maxCore.settingsManager.setMemoryEnabled(enabled)
    maxCore.refreshRegistry()
  }

  suspend fun setDiagnosticsLoggingEnabled(enabled: Boolean) {
    maxCore.settingsManager.setDiagnosticsLoggingEnabled(enabled)
  }

  suspend fun setThemeMode(mode: ThemeMode) {
    maxCore.settingsManager.setThemeMode(mode)
  }

  suspend fun resetSettings() {
    maxCore.settingsManager.resetToDefaults()
    maxCore.enforcePrivacyLock()
  }

  fun checkTtsReadiness() {
    maxCore.ttsManager.initialize()
    maxCore.ttsManager.speak("আসসালামু আলাইকুম সানজু। আমি MAX, কথা বলার জন্য প্রস্তুত।", SpeechStyle.CALM)
    maxCore.refreshRegistry()
  }

  fun saveMemory(
    key: String,
    content: String,
    category: MemoryCategory,
  ) {
    if (!settings.value.memoryEnabled) {
      maxCore.errorLogger.logWarning("MemoryManager", "Memory storage skipped: memory module disabled in settings.")
      return
    }
    maxCore.memoryManager.saveMemory(key = key, content = content, category = category)
    maxCore.emotionAnalyzer.analyzeTextSignal(content)
    maxCore.refreshRegistry()
  }

  fun toggleMemoryPin(id: String) {
    maxCore.memoryManager.togglePin(id)
  }

  fun deleteMemory(id: String) {
    maxCore.memoryManager.deleteMemory(id)
    maxCore.refreshRegistry()
  }

  fun clearMemories() {
    maxCore.memoryManager.clearAll()
    maxCore.refreshRegistry()
  }

  fun emitDiagnosticEvent(
    severity: LogSeverity,
    tag: String,
    message: String,
  ) {
    maxCore.errorLogger.log(severity, tag, message)
  }

  fun clearLogs() {
    maxCore.errorLogger.clearLogs()
  }
}

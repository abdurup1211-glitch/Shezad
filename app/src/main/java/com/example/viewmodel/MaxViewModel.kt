package com.example.viewmodel

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.apps.AppState
import com.example.apps.SupportedExternalApp
import com.example.camera.CameraActionOutcome
import com.example.camera.CameraMode
import com.example.conversation.ConversationMessage
import com.example.conversation.ConversationState
import com.example.core.MaxCoreState
import com.example.core.MaxLifecycleState
import com.example.emotion.EmotionReading
import com.example.internet.InternetActionOutcome
import com.example.internet.SearchVertical
import com.example.memory.MemoryCategory
import com.example.memory.MemoryEntry
import com.example.permissions.AssistantPermission
import com.example.permissions.PermissionState
import com.example.repository.MaxSystemRepository
import com.example.settings.MaxSettings
import com.example.settings.ThemeMode
import com.example.smart.MaxReminder
import com.example.speech.SpeechState
import com.example.tts.TtsState
import com.example.utils.ErrorLogEntry
import com.example.utils.LogSeverity
import com.example.utils.ModuleCategory
import com.example.utils.ModuleDescriptor
import com.example.wakeword.WakeWordState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Interactive assistant status state displayed on the MAX Home Screen center core.
 */
enum class AssistantStatus {
  WAITING,
  LISTENING,
  THINKING,
  SPEAKING,
}

/**
 * Pending user-confirmed external intent or rationale preview so MAX never launches
 * external apps, browser, dialer, or camera silently.
 */
data class PendingConfirmationAction(
  val title: String,
  val description: String,
  val intent: Intent? = null,
)

/**
 * Type-safe sealed UI state for the MAX Foundation application.
 */
sealed class MaxScreenState {
  data object Loading : MaxScreenState()

  data class Ready(
    val dashboard: MaxDashboardData,
  ) : MaxScreenState()

  data class Error(
    val message: String,
  ) : MaxScreenState()
}

/**
 * Consolidated immutable presentation state rendered across MAX screens.
 */
data class MaxDashboardData(
  val coreState: MaxCoreState = MaxCoreState(),
  val settings: MaxSettings = MaxSettings(),
  val permissionStates: Map<AssistantPermission, PermissionState> = emptyMap(),
  val wakeWordState: WakeWordState = WakeWordState.PrivacyLocked,
  val speechState: SpeechState = SpeechState.PrivacyLocked,
  val ttsState: TtsState = TtsState.Uninitialized,
  val memories: List<MemoryEntry> = emptyList(),
  val transcript: List<ConversationMessage> = emptyList(),
  val conversationState: ConversationState = ConversationState.WakeStandby,
  val emotionReading: EmotionReading = EmotionReading(),
  val logs: List<ErrorLogEntry> = emptyList(),
  val reminders: List<MaxReminder> = emptyList(),
  val smartSuggestions: List<String> = emptyList(),
  val assistantStatus: AssistantStatus = AssistantStatus.WAITING,
  val lastCompletedTask: String = "MAX Core initialized with Privacy Shield active",
  val lastCompletedTaskAtMillis: Long = System.currentTimeMillis(),
  val pendingConfirmation: PendingConfirmationAction? = null,
  val selectedModuleCategory: ModuleCategory? = null,
  val selectedMemoryCategory: MemoryCategory? = null,
  val memorySearchQuery: String = "",
  val selectedLogSeverity: LogSeverity? = null,
  val feedbackBanner: String? = null,
) {
  val filteredModules: List<ModuleDescriptor>
    get() =
      selectedModuleCategory?.let { category ->
        coreState.modules.filter { it.category == category }
      } ?: coreState.modules

  val filteredMemories: List<MemoryEntry>
    get() =
      memories.filter { entry ->
        val categoryMatch =
          selectedMemoryCategory == null || entry.category == selectedMemoryCategory
        val queryMatch =
          memorySearchQuery.isBlank() ||
            entry.key.contains(memorySearchQuery, ignoreCase = true) ||
            entry.content.contains(memorySearchQuery, ignoreCase = true)
        categoryMatch && queryMatch
      }

  val filteredLogs: List<ErrorLogEntry>
    get() =
      selectedLogSeverity?.let { severity ->
        logs.filter { it.severity == severity }
      } ?: logs
}

private data class FilterPreferences(
  val assistantStatus: AssistantStatus = AssistantStatus.WAITING,
  val lastCompletedTask: String = "MAX Core initialized with Privacy Shield active",
  val lastCompletedTaskAtMillis: Long = System.currentTimeMillis(),
  val pendingConfirmation: PendingConfirmationAction? = null,
  val moduleCategory: ModuleCategory? = null,
  val memoryCategory: MemoryCategory? = null,
  val memoryQuery: String = "",
  val logSeverity: LogSeverity? = null,
  val feedbackBanner: String? = null,
)

/**
 * Primary MVVM [ViewModel] for MAX Core architecture, conversation, smart tools, memory, and diagnostics.
 */
class MaxViewModel(
  private val repository: MaxSystemRepository,
) : ViewModel() {

  private val filterPreferences = MutableStateFlow(FilterPreferences())
  private val cachedSuggestions: List<String> by lazy { repository.getSmartSuggestions() }

  val uiState: StateFlow<MaxScreenState> =
    combine(
      combine(
        repository.coreState,
        repository.settings,
        repository.permissionStates,
        repository.wakeWordState,
        repository.speechState,
      ) { core, settings, permissions, wakeWord, speech ->
        CoreVoiceTuple(core, settings, permissions, wakeWord, speech)
      },
      combine(
        repository.ttsState,
        repository.memories,
        repository.transcript,
        repository.emotionState,
        repository.logs,
      ) { tts, memories, transcript, emotion, logs ->
        DataTuple(tts, memories, transcript, emotion, logs)
      },
      combine(
        repository.conversationState,
        repository.reminders,
        filterPreferences,
      ) { convState, reminders, filters ->
        ExtraTuple(convState, reminders, filters)
      },
    ) { coreVoice, dataTuple, extraTuple ->
      if (coreVoice.core.lifecycle == MaxLifecycleState.ERROR) {
        MaxScreenState.Error(coreVoice.core.statusMessage)
      } else {
        MaxScreenState.Ready(
          MaxDashboardData(
            coreState = coreVoice.core,
            settings = coreVoice.settings,
            permissionStates = coreVoice.permissions,
            wakeWordState = coreVoice.wakeWord,
            speechState = coreVoice.speech,
            ttsState = dataTuple.tts,
            memories = dataTuple.memories,
            transcript = dataTuple.transcript,
            conversationState = extraTuple.convState,
            emotionReading = dataTuple.emotion,
            logs = dataTuple.logs,
            reminders = extraTuple.reminders,
            smartSuggestions = cachedSuggestions,
            assistantStatus = extraTuple.filters.assistantStatus,
            lastCompletedTask = extraTuple.filters.lastCompletedTask,
            lastCompletedTaskAtMillis = extraTuple.filters.lastCompletedTaskAtMillis,
            pendingConfirmation = extraTuple.filters.pendingConfirmation,
            selectedModuleCategory = extraTuple.filters.moduleCategory,
            selectedMemoryCategory = extraTuple.filters.memoryCategory,
            memorySearchQuery = extraTuple.filters.memoryQuery,
            selectedLogSeverity = extraTuple.filters.logSeverity,
            feedbackBanner = extraTuple.filters.feedbackBanner,
          ),
        )
      }
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000),
      initialValue = MaxScreenState.Loading,
    )

  /**
   * Cycles MAX's Home Screen voice core state cleanly through Waiting -> Listening ->
   * Thinking -> Speaking -> Waiting and triggers the natural wake greeting on Listening.
   */
  fun cycleAssistantStatus() {
    val next =
      when (filterPreferences.value.assistantStatus) {
        AssistantStatus.WAITING -> AssistantStatus.LISTENING
        AssistantStatus.LISTENING -> AssistantStatus.THINKING
        AssistantStatus.THINKING -> AssistantStatus.SPEAKING
        AssistantStatus.SPEAKING -> AssistantStatus.WAITING
      }
    setAssistantStatus(next)
  }

  fun setAssistantStatus(status: AssistantStatus) {
    when (status) {
      AssistantStatus.LISTENING -> {
        repository.triggerWakeWordMax()
        filterPreferences.value =
          filterPreferences.value.copy(
            assistantStatus = status,
            lastCompletedTask = "MAX Wake Word greeted Sanju (mic hardware privacy-locked)",
            lastCompletedTaskAtMillis = System.currentTimeMillis(),
            feedbackBanner = "MAX is ready — say or enter your request.",
          )
      }
      AssistantStatus.WAITING -> {
        repository.concludeConversation()
        filterPreferences.value =
          filterPreferences.value.copy(
            assistantStatus = status,
            feedbackBanner = null,
          )
      }
      else -> {
        filterPreferences.value =
          filterPreferences.value.copy(
            assistantStatus = status,
            feedbackBanner = null,
          )
      }
    }
  }

  fun submitCommand(command: String) {
    val clean = command.trim()
    if (clean.isEmpty()) return
    filterPreferences.value = filterPreferences.value.copy(assistantStatus = AssistantStatus.THINKING)
    val reply = repository.submitUserCommand(clean)
    filterPreferences.value =
      filterPreferences.value.copy(
        assistantStatus = AssistantStatus.SPEAKING,
        lastCompletedTask = reply.ifBlank { "Processed '$clean'" },
        lastCompletedTaskAtMillis = System.currentTimeMillis(),
        feedbackBanner = reply.take(90),
      )
  }

  fun requestExternalAppConfirmation(app: SupportedExternalApp) {
    val state = repository.prepareAppLaunch(app, confirmed = true)
    val pending =
      when (state) {
        is AppState.LaunchReady ->
          PendingConfirmationAction(
            title = "Open ${app.displayName}?",
            description =
              if (state.isWebFallback) {
                "${app.displayName} অ্যাপ ইনস্টল নেই; নিরাপদ ব্রাউজারে খুলতে চান?"
              } else {
                "সানজু, আপনি কি এখন ${app.displayName} খুলতে চান?"
              },
            intent = state.intent,
          )
        is AppState.NotInstalled ->
          PendingConfirmationAction(
            title = "${app.displayName} Unavailable",
            description = state.suggestion,
            intent = null,
          )
        is AppState.ConfirmationRequired ->
          PendingConfirmationAction(
            title = "Open ${app.displayName}?",
            description = state.detail,
            intent = null,
          )
        AppState.Standby -> null
      }
    filterPreferences.value = filterPreferences.value.copy(pendingConfirmation = pending)
  }

  fun requestBrowserConfirmation(query: String = "Bangladesh") {
    val outcome = repository.prepareSearch(query, SearchVertical.WEB, confirmed = true)
    val pending =
      when (outcome) {
        is InternetActionOutcome.ReadyBrowser ->
          PendingConfirmationAction(
            title = "Open Browser Search?",
            description = "সানজু, আপনার অনুরোধে '$query' অনুসন্ধানের জন্য ব্রাউজার খুলবেন?",
            intent = outcome.intent,
          )
        is InternetActionOutcome.Blocked ->
          PendingConfirmationAction("Internet Blocked", outcome.message, null)
        is InternetActionOutcome.Summary ->
          PendingConfirmationAction("Summary", outcome.summaryText, null)
      }
    filterPreferences.value = filterPreferences.value.copy(pendingConfirmation = pending)
  }

  fun requestCameraConfirmation() {
    val outcome = repository.prepareCamera(CameraMode.PHOTO, confirmed = true)
    val pending =
      when (outcome) {
        is CameraActionOutcome.ReadyIntent ->
          PendingConfirmationAction(
            title = "Open Camera?",
            description = "সানজু, ছবি তোলার জন্য ক্যামেরা খুলতে চান? কাজ শেষে রিসোর্স বন্ধ করা হবে।",
            intent = outcome.intent,
          )
        is CameraActionOutcome.NeedsPermission ->
          PendingConfirmationAction("Camera Permission Info", outcome.rationale, null)
        is CameraActionOutcome.Blocked ->
          PendingConfirmationAction("Camera Privacy Locked", outcome.message, null)
        is CameraActionOutcome.Unavailable ->
          PendingConfirmationAction("Camera Unavailable", outcome.message, null)
      }
    filterPreferences.value = filterPreferences.value.copy(pendingConfirmation = pending)
  }

  fun requestPhoneDialerConfirmation() {
    val intent = repository.buildDialerIntent("01700000000", confirmed = true)
    filterPreferences.value =
      filterPreferences.value.copy(
        pendingConfirmation =
          PendingConfirmationAction(
            title = "Open Phone Dialer?",
            description = "নিরাপত্তার জন্য MAX নিজে কল করে না। আপনি চাইলে ডায়ালার খুলতে পারেন।",
            intent = intent,
          ),
      )
  }

  fun dismissConfirmation() {
    filterPreferences.value = filterPreferences.value.copy(pendingConfirmation = null)
    repository.releaseCamera()
  }

  fun runDiagnostics() {
    val result = repository.runDiagnostics()
    recordCompletedTask("Verified ${result.modules.size} MAX subsystems via self-check")
    setFeedback("Self-check complete: ${result.modules.size} modules verified.")
  }

  fun enforcePrivacyLock() {
    viewModelScope.launch {
      repository.setPrivacySafeMode(true)
      repository.enforcePrivacyLock()
      filterPreferences.value =
        filterPreferences.value.copy(
          assistantStatus = AssistantStatus.WAITING,
          lastCompletedTask = "Privacy Shield locked across all sensitive modules",
          lastCompletedTaskAtMillis = System.currentTimeMillis(),
          feedbackBanner = "Privacy Shield locked across all sensitive modules.",
        )
    }
  }

  fun checkTtsReadiness() {
    repository.checkTtsReadiness()
    recordCompletedTask("Completed Bangla TextToSpeech readiness check")
    setFeedback("TTS readiness check triggered.")
  }

  fun setModuleCategoryFilter(category: ModuleCategory?) {
    filterPreferences.value = filterPreferences.value.copy(moduleCategory = category)
  }

  fun setMemoryCategoryFilter(category: MemoryCategory?) {
    filterPreferences.value = filterPreferences.value.copy(memoryCategory = category)
  }

  fun setMemorySearchQuery(query: String) {
    filterPreferences.value = filterPreferences.value.copy(memoryQuery = query)
  }

  fun setLogSeverityFilter(severity: LogSeverity?) {
    filterPreferences.value = filterPreferences.value.copy(logSeverity = severity)
  }

  fun dismissFeedback() {
    filterPreferences.value = filterPreferences.value.copy(feedbackBanner = null)
  }

  fun addMemory(
    key: String,
    content: String,
    category: MemoryCategory,
  ) {
    if (content.isBlank()) return
    repository.saveMemory(key = key, content = content, category = category)
    recordCompletedTask("Stored '${key.ifBlank { "Note" }}' in MemoryManager")
    setFeedback("Saved context note to MemoryManager.")
  }

  fun toggleMemoryPin(id: String) {
    repository.toggleMemoryPin(id)
  }

  fun deleteMemory(id: String) {
    repository.deleteMemory(id)
  }

  fun clearMemories() {
    repository.clearMemories()
    recordCompletedTask("Cleared session memory vault")
    setFeedback("Cleared session memory vault.")
  }

  fun setPrivacySafeMode(enabled: Boolean) {
    viewModelScope.launch {
      repository.setPrivacySafeMode(enabled)
      recordCompletedTask(
        if (enabled) "Enabled Privacy Safe Mode" else "Relaxed Privacy Safe Mode (sensors idle)",
      )
      setFeedback(
        if (enabled) "Privacy Safe Mode enabled." else "Privacy Safe Mode relaxed (sensors remain idle).",
      )
    }
  }

  fun setWakeWordStandbyEnabled(enabled: Boolean) {
    viewModelScope.launch {
      repository.setWakeWordStandbyEnabled(enabled)
      recordCompletedTask("Updated wake word 'MAX' standby flag")
    }
  }

  fun setTtsEnabled(enabled: Boolean) {
    viewModelScope.launch {
      repository.setTtsEnabled(enabled)
      recordCompletedTask(if (enabled) "Enabled TTS voice engine" else "Disabled TTS voice engine")
    }
  }

  fun setTtsSpeechRate(rate: Float) {
    viewModelScope.launch {
      repository.setTtsSpeechRate(rate)
    }
  }

  fun setMemoryEnabled(enabled: Boolean) {
    viewModelScope.launch {
      repository.setMemoryEnabled(enabled)
    }
  }

  fun setDiagnosticsLoggingEnabled(enabled: Boolean) {
    viewModelScope.launch {
      repository.setDiagnosticsLoggingEnabled(enabled)
    }
  }

  fun setThemeMode(mode: ThemeMode) {
    viewModelScope.launch {
      repository.setThemeMode(mode)
    }
  }

  fun resetSettings() {
    viewModelScope.launch {
      repository.resetSettings()
      recordCompletedTask("Restored privacy-first default settings")
      setFeedback("Restored MAX privacy-first default settings.")
    }
  }

  fun emitTestDiagnostic() {
    repository.emitDiagnosticEvent(
      severity = LogSeverity.INFO,
      tag = "MaxDiagnostics",
      message = "Manual architecture heartbeat verified by user.",
    )
    recordCompletedTask("Recorded diagnostic heartbeat in ErrorLogger")
    setFeedback("Diagnostic event recorded in ErrorLogger.")
  }

  fun clearLogs() {
    repository.clearLogs()
  }

  private fun recordCompletedTask(summary: String) {
    filterPreferences.value =
      filterPreferences.value.copy(
        lastCompletedTask = summary,
        lastCompletedTaskAtMillis = System.currentTimeMillis(),
      )
  }

  private fun setFeedback(message: String) {
    filterPreferences.value = filterPreferences.value.copy(feedbackBanner = message)
  }

  private data class CoreVoiceTuple(
    val core: MaxCoreState,
    val settings: MaxSettings,
    val permissions: Map<AssistantPermission, PermissionState>,
    val wakeWord: WakeWordState,
    val speech: SpeechState,
  )

  private data class DataTuple(
    val tts: TtsState,
    val memories: List<MemoryEntry>,
    val transcript: List<ConversationMessage>,
    val emotion: EmotionReading,
    val logs: List<ErrorLogEntry>,
  )

  private data class ExtraTuple(
    val convState: ConversationState,
    val reminders: List<MaxReminder>,
    val filters: FilterPreferences,
  )

  class Factory(
    private val repository: MaxSystemRepository,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      if (modelClass.isAssignableFrom(MaxViewModel::class.java)) {
        return MaxViewModel(repository) as T
      }
      throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
  }
}

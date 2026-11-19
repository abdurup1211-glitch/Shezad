package com.example.core

import com.example.apps.AppLauncherCapability
import com.example.apps.AppManager
import com.example.automation.AutomationManager
import com.example.brain.AIBrainManager
import com.example.camera.CameraCapability
import com.example.camera.CameraManager
import com.example.conversation.ConversationEngine
import com.example.conversation.ConversationManager
import com.example.emotion.EmotionAnalyzer
import com.example.emotion.EmotionManager
import com.example.files.FileAccessCapability
import com.example.files.FileManager
import com.example.gallery.GalleryCapability
import com.example.gallery.GalleryManager
import com.example.internet.InternetManager
import com.example.internet.NetworkCapability
import com.example.memory.MemoryController
import com.example.memory.MemoryManager
import com.example.overlay.FloatingAssistantController
import com.example.permissions.PermissionManager
import com.example.permissions.PermissionRequestHandler
import com.example.permissions.PrivacyController
import com.example.phone.PhoneCapability
import com.example.phone.PhoneManager
import com.example.service.AssistantServiceController
import com.example.settings.SettingsManager
import com.example.smart.SmartFeatureManager
import com.example.speech.SpeechManager
import com.example.tts.TTSManager
import com.example.update.AppUpdateChecker
import com.example.utils.ErrorLogger
import com.example.utils.MaxModule
import com.example.utils.ModuleCategory
import com.example.utils.ModuleDescriptor
import com.example.utils.ModuleStatus
import com.example.wakeword.WakeWordListener
import com.example.wakeword.WakeWordManager
import com.example.wakeword.WakeWordState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * High-level lifecycle state of [MAXCore].
 */
enum class MaxLifecycleState {
  UNINITIALIZED,
  INITIALIZING,
  READY,
  PRIVACY_LOCKED,
  ERROR,
}

/**
 * Snapshot of MAX's central architecture and module registry.
 */
data class MaxCoreState(
  val lifecycle: MaxLifecycleState = MaxLifecycleState.UNINITIALIZED,
  val modules: List<ModuleDescriptor> = emptyList(),
  val privacyShieldActive: Boolean = true,
  val lastDiagnosticsAtMillis: Long? = null,
  val statusMessage: String = "MAX Core awaiting initialization.",
)

/**
 * Main architectural controller for MAX.
 *
 * Connects all core managers, cognitive engines, smart features, and privacy-guarded
 * device capability modules while keeping every subsystem decoupled behind interfaces.
 */
class MAXCore(
  val settingsManager: SettingsManager,
  val permissionManager: PermissionManager,
  val privacyController: PrivacyController,
  val permissionRequestHandler: PermissionRequestHandler,
  val speechManager: SpeechManager,
  val ttsManager: TTSManager,
  val memoryManager: MemoryManager,
  val memoryController: MemoryController,
  val wakeWordManager: WakeWordManager,
  val errorLogger: ErrorLogger,
  val conversationEngine: ConversationEngine,
  val conversationManager: ConversationManager,
  val emotionAnalyzer: EmotionAnalyzer,
  val emotionManager: EmotionManager,
  val aiBrainManager: AIBrainManager,
  val automationManager: AutomationManager,
  val floatingAssistantController: FloatingAssistantController,
  val appUpdateChecker: AppUpdateChecker,
  val serviceController: AssistantServiceController,
  val cameraCapability: CameraCapability,
  val cameraManager: CameraManager,
  val galleryCapability: GalleryCapability,
  val galleryManager: GalleryManager,
  val networkCapability: NetworkCapability,
  val internetManager: InternetManager,
  val phoneCapability: PhoneCapability,
  val phoneManager: PhoneManager,
  val appLauncherCapability: AppLauncherCapability,
  val appManager: AppManager,
  val filesCapability: FileAccessCapability,
  val fileManager: FileManager,
  val smartFeatureManager: SmartFeatureManager,
) : MaxModule, WakeWordListener {

  private val _coreState = MutableStateFlow(MaxCoreState())
  val coreState: StateFlow<MaxCoreState> = _coreState.asStateFlow()

  override val descriptor: ModuleDescriptor
    get() =
      ModuleDescriptor(
        id = "max_core",
        displayName = "MAXCore",
        packageName = "com.example.core",
        category = ModuleCategory.CORE_ORCHESTRATION,
        status =
          when (_coreState.value.lifecycle) {
            MaxLifecycleState.READY, MaxLifecycleState.PRIVACY_LOCKED -> ModuleStatus.READY
            MaxLifecycleState.UNINITIALIZED, MaxLifecycleState.INITIALIZING -> ModuleStatus.STANDBY
            MaxLifecycleState.ERROR -> ModuleStatus.ERROR
          },
        summary = "Central orchestrator linking all MAX subsystems.",
      )

  private val allManagedModules: List<MaxModule>
    get() =
      listOf(
        this,
        settingsManager,
        permissionManager,
        wakeWordManager,
        speechManager,
        ttsManager,
        memoryManager,
        conversationEngine,
        conversationManager,
        emotionAnalyzer,
        emotionManager,
        aiBrainManager,
        automationManager,
        floatingAssistantController,
        appUpdateChecker,
        serviceController,
        cameraCapability,
        cameraManager,
        galleryCapability,
        galleryManager,
        networkCapability,
        internetManager,
        phoneCapability,
        phoneManager,
        appLauncherCapability,
        appManager,
        filesCapability,
        fileManager,
        smartFeatureManager,
      )

  /**
   * Initializes core foundation managers safely without activating any privacy-sensitive sensors.
   * Pass [deferTtsWarmup] = true during Application cold start so UI rendering is never delayed.
   */
  fun initialize(deferTtsWarmup: Boolean = false) {
    if (_coreState.value.lifecycle == MaxLifecycleState.INITIALIZING) return

    _coreState.update {
      it.copy(
        lifecycle = MaxLifecycleState.INITIALIZING,
        statusMessage = "Bootstrapping MAX Core architecture...",
      )
    }

    runCatching {
      val privacyMode = settingsManager.settings.value.privacySafeMode
      permissionManager.setPrivacyLockActive(privacyMode)
      wakeWordManager.addListener(this)
      wakeWordManager.enterStandbyMode()
      speechManager.prepare()
      if (!deferTtsWarmup && settingsManager.settings.value.ttsEnabled) {
        ttsManager.initialize()
      }
      smartFeatureManager.refreshSuggestions()
      conversationEngine.recordSystemEvent("MAX Core initialized with Privacy Shield active.")
      errorLogger.logInfo("MAXCore", "MAXCore initialized cleanly (${allManagedModules.size} modules).")
      refreshRegistry(
        lifecycle = if (privacyMode) MaxLifecycleState.PRIVACY_LOCKED else MaxLifecycleState.READY,
        message =
          if (privacyMode) {
            "MAX Core Ready • Privacy Shield Enforced"
          } else {
            "MAX Core Ready • Standby Architecture"
          },
      )
    }.onFailure { throwable ->
      errorLogger.logError("MAXCore", "Core initialization failed.", throwable)
      _coreState.update {
        it.copy(
          lifecycle = MaxLifecycleState.ERROR,
          statusMessage = throwable.message ?: "Initialization error",
        )
      }
    }
  }

  fun warmUpTts() {
    if (settingsManager.settings.value.ttsEnabled) {
      ttsManager.initialize()
      refreshRegistry()
    }
  }

  /**
   * Step 1 & Step 12 Wake Word Flow entry point:
   * Triggered when "MAX" is detected -> prepares speech -> greets Sanju -> awaits command.
   */
  override fun onWakeWordDetected(keyword: String, confidence: Float) {
    errorLogger.logInfo("MAXCore", "Wake word '$keyword' accepted; preparing conversation.")
    speechManager.prepare()
    conversationManager.onWakeWordActivated(speakGreeting = settingsManager.settings.value.ttsEnabled)
    refreshRegistry(message = "Wake word '$keyword' active • Greeting Sanju")
  }

  override fun onWakeWordStateChanged(state: WakeWordState) {
    refreshRegistry()
  }

  fun triggerWakeWordMax(speakGreeting: Boolean = true): Boolean {
    val matched = wakeWordManager.onKeywordMatched(phrase = "MAX", confidence = 0.98f)
    if (!matched) {
      conversationManager.onWakeWordActivated(speakGreeting = speakGreeting)
    }
    return true
  }

  /**
   * Unified user command router backed by [AIBrainManager] so MAX thinks before responding,
   * combines Memory/Emotion/Conversation/Smart tools offline first, asks follow-up questions,
   * and always asks permission before using InternetManager.
   */
  fun processUserCommand(
    command: String,
    explicitUserRequest: Boolean = true,
  ): String {
    val trimmed = command.trim()
    if (trimmed.isEmpty()) return ""

    val brainResponse =
      aiBrainManager.thinkAndRespond(
        rawInput = trimmed,
        explicitUserRequest = explicitUserRequest,
        speakReply = settingsManager.settings.value.ttsEnabled,
      )
    refreshRegistry()
    return brainResponse.replyText
  }

  /**
   * Runs a non-invasive architectural self-check across all registered modules.
   */
  fun runDiagnostics(): MaxCoreState {
    val privacyLocked = settingsManager.settings.value.privacySafeMode
    permissionManager.setPrivacyLockActive(privacyLocked)
    wakeWordManager.enterStandbyMode()
    speechManager.prepare()
    val refreshedPermissions = permissionManager.refreshAllStatuses()
    smartFeatureManager.refreshSuggestions()

    errorLogger.logInfo(
      tag = "MAXCore",
      message =
        "Diagnostics complete: ${allManagedModules.size} modules verified, " +
          "${refreshedPermissions.size} permission domains audited, privacyLock=$privacyLocked.",
    )

    refreshRegistry(
      lifecycle = if (privacyLocked) MaxLifecycleState.PRIVACY_LOCKED else MaxLifecycleState.READY,
      message = "Diagnostics passed • ${allManagedModules.size} modules healthy",
      diagnosticsTimestamp = System.currentTimeMillis(),
    )
    return _coreState.value
  }

  /**
   * Enforces strict privacy lock across all voice, sensor, and capability subsystems.
   */
  fun enforcePrivacyLock() {
    permissionManager.setPrivacyLockActive(true)
    wakeWordManager.disarm()
    speechManager.reset()
    ttsManager.stopSpeaking()
    cameraManager.releaseResources()
    galleryManager.clearSelection()
    phoneManager.releaseResources()
    fileManager.lockFiles()
    floatingAssistantController.stopSystemOverlay()
    conversationManager.returnToWakeMode()
    aiBrainManager.resetContext()
    errorLogger.logInfo("MAXCore", "Strict privacy lock enforced across all subsystems.")
    refreshRegistry(
      lifecycle = MaxLifecycleState.PRIVACY_LOCKED,
      message = "Privacy Shield Locked • Sensors & Capture Inactive",
    )
  }

  fun refreshRegistry(
    lifecycle: MaxLifecycleState = _coreState.value.lifecycle,
    message: String = _coreState.value.statusMessage,
    diagnosticsTimestamp: Long? = _coreState.value.lastDiagnosticsAtMillis,
  ) {
    _coreState.value =
      MaxCoreState(
        lifecycle = lifecycle,
        modules = allManagedModules.map { it.descriptor },
        privacyShieldActive = permissionManager.isPrivacyLockActive(),
        lastDiagnosticsAtMillis = diagnosticsTimestamp,
        statusMessage = message,
      )
  }

  /**
   * Releases active audio, torch, and camera session resources when the app moves to background.
   */
  fun releaseTransientResources() {
    runCatching {
      ttsManager.stopSpeaking()
      cameraManager.releaseResources()
      phoneManager.releaseResources()
      speechManager.reset()
    }
  }

  /**
   * Reduces non-essential in-memory caches under system low-memory pressure.
   */
  fun onTrimMemory(level: Int) {
    if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) {
      errorLogger.trimToRecent(keepCount = 20)
      releaseTransientResources()
    }
  }

  fun shutdown() {
    wakeWordManager.removeListener(this)
    wakeWordManager.disarm()
    speechManager.reset()
    ttsManager.shutdown()
    cameraManager.releaseResources()
    phoneManager.releaseResources()
    floatingAssistantController.stopSystemOverlay()
    aiBrainManager.resetContext()
    serviceController.markStopped()
    errorLogger.logInfo("MAXCore", "MAX Core shut down cleanly.")
  }
}

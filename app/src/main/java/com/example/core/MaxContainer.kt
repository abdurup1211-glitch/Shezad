package com.example.core

import android.content.Context
import com.example.apps.AppLauncher
import com.example.apps.AppManager
import com.example.apps.ChromeManager
import com.example.apps.FacebookManager
import com.example.apps.GmailManager
import com.example.apps.MapsManager
import com.example.apps.MessengerManager
import com.example.apps.PassiveAppsModule
import com.example.apps.WhatsAppManager
import com.example.apps.YouTubeManager
import com.example.automation.AutomationManager
import com.example.brain.AIBrainManager
import com.example.brain.ContextManager
import com.example.brain.DecisionEngine
import com.example.brain.IntentAnalyzer
import com.example.brain.MultiStepPlanner
import com.example.brain.ResponsePlanner
import com.example.camera.CameraController
import com.example.camera.CameraManager
import com.example.camera.PassiveCameraModule
import com.example.conversation.DefaultConversationManager
import com.example.conversation.FarewellManager
import com.example.conversation.FoundationConversationEngine
import com.example.conversation.GreetingManager
import com.example.conversation.PersonalityManager
import com.example.emotion.DefaultEmotionManager
import com.example.emotion.FoundationEmotionAnalyzer
import com.example.files.DocumentReader
import com.example.files.FileBrowser
import com.example.files.FileManager
import com.example.files.ImageReader
import com.example.files.OCRManager
import com.example.files.PDFReader
import com.example.files.PassiveFilesModule
import com.example.gallery.GalleryController
import com.example.gallery.GalleryManager
import com.example.gallery.PassiveGalleryModule
import com.example.internet.BrowserManager
import com.example.internet.InternetManager
import com.example.internet.PassiveInternetModule
import com.example.internet.SearchManager
import com.example.internet.WebRequestManager
import com.example.memory.InMemoryMemoryManager
import com.example.memory.LocalMemoryRepository
import com.example.memory.MemoryController
import com.example.overlay.FloatingAssistantController
import com.example.permissions.AndroidPermissionManager
import com.example.permissions.PermissionRequestHandler
import com.example.permissions.PrivacyController
import com.example.phone.AlarmManager
import com.example.phone.CalculatorManager
import com.example.phone.CallManager
import com.example.phone.ContactsManager
import com.example.phone.FlashlightManager
import com.example.phone.PassivePhoneModule
import com.example.phone.PhoneManager
import com.example.phone.SMSManager
import com.example.phone.StopwatchManager
import com.example.phone.TimerManager
import com.example.repository.MaxSystemRepository
import com.example.service.DefaultAssistantServiceController
import com.example.settings.DataStoreSettingsManager
import com.example.smart.AISuggestionManager
import com.example.smart.CalendarManager
import com.example.smart.NotesManager
import com.example.smart.ReminderManager
import com.example.smart.SmartFeatureManager
import com.example.smart.TranslationManager
import com.example.smart.VoiceNotesManager
import com.example.speech.DefaultSpeechManager
import com.example.tts.AndroidTTSManager
import com.example.update.AppUpdateChecker
import com.example.utils.DefaultErrorLogger
import com.example.utils.ErrorLogger
import com.example.wakeword.DefaultWakeWordManager

/**
 * Lightweight constructor-injection dependency container for MAX.
 * Wires all 21 modular packages cleanly for low memory overhead.
 */
class MaxContainer(context: Context) {

  private val appContext = context.applicationContext

  val errorLogger: ErrorLogger by lazy { DefaultErrorLogger() }

  val settingsManager by lazy { DataStoreSettingsManager(appContext, errorLogger) }

  val permissionManager by lazy { AndroidPermissionManager(appContext, errorLogger) }

  val privacyController by lazy { PrivacyController(permissionManager, errorLogger) }

  val permissionRequestHandler by lazy { PermissionRequestHandler(permissionManager, errorLogger) }

  val memoryRepository by lazy { LocalMemoryRepository(appContext, errorLogger) }

  val memoryManager by lazy { InMemoryMemoryManager(errorLogger, memoryRepository) }

  val personalityManager by lazy { PersonalityManager() }

  val greetingManager by lazy { GreetingManager() }

  val farewellManager by lazy { FarewellManager() }

  val memoryController by lazy { MemoryController(memoryManager, personalityManager, errorLogger) }

  val emotionAnalyzer by lazy { FoundationEmotionAnalyzer() }

  val ttsManager by lazy {
    AndroidTTSManager(
      context = appContext,
      settingsManager = settingsManager,
      errorLogger = errorLogger,
    )
  }

  val emotionManager by lazy {
    DefaultEmotionManager(
      emotionAnalyzer = emotionAnalyzer,
      ttsManager = ttsManager,
      errorLogger = errorLogger,
    )
  }

  val conversationEngine by lazy {
    FoundationConversationEngine(
      memoryManager = memoryManager,
      emotionAnalyzer = emotionAnalyzer,
      errorLogger = errorLogger,
    )
  }

  val conversationManager by lazy {
    DefaultConversationManager(
      conversationEngine = conversationEngine,
      ttsManager = ttsManager,
      emotionManager = emotionManager,
      personalityManager = personalityManager,
      greetingManager = greetingManager,
      farewellManager = farewellManager,
      errorLogger = errorLogger,
    )
  }

  val wakeWordManager by lazy {
    DefaultWakeWordManager(
      settingsManager = settingsManager,
      permissionManager = permissionManager,
      errorLogger = errorLogger,
    )
  }

  val speechManager by lazy {
    DefaultSpeechManager(
      permissionManager = permissionManager,
      errorLogger = errorLogger,
    )
  }

  val serviceController by lazy { DefaultAssistantServiceController() }

  val cameraCapability by lazy { PassiveCameraModule(appContext) }

  val cameraManager by lazy {
    CameraManager(
      context = appContext,
      controller = CameraController(appContext, errorLogger),
      permissionManager = permissionManager,
      privacyController = privacyController,
      permissionRequestHandler = permissionRequestHandler,
      errorLogger = errorLogger,
    )
  }

  val galleryCapability by lazy { PassiveGalleryModule() }

  val galleryManager by lazy {
    GalleryManager(
      controller = GalleryController(),
      privacyController = privacyController,
      errorLogger = errorLogger,
    )
  }

  val networkCapability by lazy { PassiveInternetModule() }

  val internetManager by lazy {
    InternetManager(
      searchManager = SearchManager(),
      browserManager = BrowserManager(appContext, errorLogger),
      webRequestManager = WebRequestManager(errorLogger),
      privacyController = privacyController,
      errorLogger = errorLogger,
    )
  }

  val phoneCapability by lazy { PassivePhoneModule() }

  val phoneManager by lazy {
    PhoneManager(
      callManager = CallManager(),
      smsManager = SMSManager(),
      contactsManager = ContactsManager(),
      alarmManager = AlarmManager(),
      timerManager = TimerManager(),
      stopwatchManager = StopwatchManager(),
      calculatorManager = CalculatorManager(),
      flashlightManager = FlashlightManager(appContext, errorLogger),
      privacyController = privacyController,
      errorLogger = errorLogger,
    )
  }

  val appLauncherCapability by lazy { PassiveAppsModule() }

  private val appLauncher by lazy { AppLauncher(appContext, errorLogger) }

  val appManager by lazy {
    AppManager(
      whatsAppManager = WhatsAppManager(appLauncher),
      messengerManager = MessengerManager(appLauncher),
      facebookManager = FacebookManager(appLauncher),
      chromeManager = ChromeManager(appLauncher),
      youTubeManager = YouTubeManager(appLauncher),
      gmailManager = GmailManager(appLauncher),
      mapsManager = MapsManager(appLauncher),
      privacyController = privacyController,
      errorLogger = errorLogger,
    )
  }

  val filesCapability by lazy { PassiveFilesModule() }

  val fileManager by lazy {
    FileManager(
      context = appContext,
      fileBrowser = FileBrowser(),
      pdfReader = PDFReader(errorLogger),
      documentReader = DocumentReader(errorLogger),
      imageReader = ImageReader(errorLogger),
      ocrManager = OCRManager(),
      privacyController = privacyController,
      errorLogger = errorLogger,
    )
  }

  val smartFeatureManager by lazy {
    SmartFeatureManager(
      translationManager = TranslationManager(),
      reminderManager = ReminderManager(),
      notesManager = NotesManager(memoryManager),
      calendarManager = CalendarManager(appContext),
      voiceNotesManager = VoiceNotesManager(),
      aiSuggestionManager = AISuggestionManager(),
      emotionManager = emotionManager,
      memoryManager = memoryManager,
      permissionManager = permissionManager,
      ttsManager = ttsManager,
      personalityManager = personalityManager,
      errorLogger = errorLogger,
    )
  }

  val automationManager by lazy {
    AutomationManager(
      memoryManager = memoryManager,
      reminderManager = smartFeatureManager.reminderManager,
      permissionManager = permissionManager,
      personalityManager = personalityManager,
      greetingManager = greetingManager,
      farewellManager = farewellManager,
      errorLogger = errorLogger,
    )
  }

  val floatingAssistantController by lazy {
    FloatingAssistantController(
      context = appContext,
      errorLogger = errorLogger,
    )
  }

  val appUpdateChecker by lazy {
    AppUpdateChecker(
      context = appContext,
      errorLogger = errorLogger,
    )
  }

  val intentAnalyzer by lazy { IntentAnalyzer() }

  val contextManager by lazy { ContextManager() }

  val decisionEngine by lazy { DecisionEngine(memoryManager) }

  val multiStepPlanner by lazy { MultiStepPlanner() }

  val responsePlanner by lazy {
    ResponsePlanner(
      emotionManager = emotionManager,
      personalityManager = personalityManager,
      greetingManager = greetingManager,
      farewellManager = farewellManager,
    )
  }

  val aiBrainManager by lazy {
    AIBrainManager(
      intentAnalyzer = intentAnalyzer,
      contextManager = contextManager,
      decisionEngine = decisionEngine,
      responsePlanner = responsePlanner,
      multiStepPlanner = multiStepPlanner,
      conversationManager = conversationManager,
      emotionManager = emotionManager,
      memoryManager = memoryManager,
      memoryController = memoryController,
      automationManager = automationManager,
      floatingAssistantController = floatingAssistantController,
      internetManager = internetManager,
      smartFeatureManager = smartFeatureManager,
      phoneManager = phoneManager,
      appManager = appManager,
      ttsManager = ttsManager,
      settingsManager = settingsManager,
      errorLogger = errorLogger,
    )
  }

  val maxCore: MAXCore by lazy {
    MAXCore(
      settingsManager = settingsManager,
      permissionManager = permissionManager,
      privacyController = privacyController,
      permissionRequestHandler = permissionRequestHandler,
      speechManager = speechManager,
      ttsManager = ttsManager,
      memoryManager = memoryManager,
      memoryController = memoryController,
      wakeWordManager = wakeWordManager,
      errorLogger = errorLogger,
      conversationEngine = conversationEngine,
      conversationManager = conversationManager,
      emotionAnalyzer = emotionAnalyzer,
      emotionManager = emotionManager,
      aiBrainManager = aiBrainManager,
      automationManager = automationManager,
      floatingAssistantController = floatingAssistantController,
      appUpdateChecker = appUpdateChecker,
      serviceController = serviceController,
      cameraCapability = cameraCapability,
      cameraManager = cameraManager,
      galleryCapability = galleryCapability,
      galleryManager = galleryManager,
      networkCapability = networkCapability,
      internetManager = internetManager,
      phoneCapability = phoneCapability,
      phoneManager = phoneManager,
      appLauncherCapability = appLauncherCapability,
      appManager = appManager,
      filesCapability = filesCapability,
      fileManager = fileManager,
      smartFeatureManager = smartFeatureManager,
    )
  }

  val systemRepository: MaxSystemRepository by lazy {
    MaxSystemRepository(maxCore = maxCore)
  }
}

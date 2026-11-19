package com.example.apps

import com.example.permissions.PrivacyCheckResult
import com.example.permissions.PrivacyController
import com.example.permissions.SensitiveDomain
import com.example.utils.ErrorLogger
import com.example.utils.MaxModule
import com.example.utils.ModuleCategory
import com.example.utils.ModuleDescriptor
import com.example.utils.ModuleStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Central App Control orchestrator handling explicit user requests to open
 * WhatsApp, Messenger, Facebook, Chrome, YouTube, Gmail, and Google Maps.
 */
class AppManager(
  val whatsAppManager: WhatsAppManager,
  val messengerManager: MessengerManager,
  val facebookManager: FacebookManager,
  val chromeManager: ChromeManager,
  val youTubeManager: YouTubeManager,
  val gmailManager: GmailManager,
  val mapsManager: MapsManager,
  private val privacyController: PrivacyController,
  private val errorLogger: ErrorLogger,
) : MaxModule {

  private val _state = MutableStateFlow<AppState>(AppState.Standby)
  val state: StateFlow<AppState> = _state.asStateFlow()

  override val descriptor: ModuleDescriptor
    get() =
      ModuleDescriptor(
        id = "app_manager",
        displayName = "AppManager",
        packageName = "com.example.apps",
        category = ModuleCategory.DEVICE_CAPABILITY,
        status = ModuleStatus.STANDBY,
        summary = "User-confirmed launcher for WhatsApp, YouTube, Maps, Chrome & more.",
        privacyGuarded = true,
      )

  fun openApp(
    app: SupportedExternalApp,
    explicitUserRequest: Boolean,
  ): AppState {
    val check = privacyController.validateActionAllowed(SensitiveDomain.APPS, explicitUserRequest)
    if (check is PrivacyCheckResult.Blocked) {
      val blocked = AppState.ConfirmationRequired(app, "অ্যাপ খোলার জন্য আপনার সরাসরি অনুমতি প্রয়োজন, সানজু।")
      _state.value = blocked
      return blocked
    }

    val next =
      when (app) {
        SupportedExternalApp.WHATSAPP -> whatsAppManager.open(explicitUserRequest)
        SupportedExternalApp.MESSENGER -> messengerManager.open(explicitUserRequest)
        SupportedExternalApp.FACEBOOK -> facebookManager.open(explicitUserRequest)
        SupportedExternalApp.CHROME -> chromeManager.open(explicitUserRequest)
        SupportedExternalApp.YOUTUBE -> youTubeManager.open(explicitUserRequest)
        SupportedExternalApp.GMAIL -> gmailManager.open(explicitUserRequest)
        SupportedExternalApp.MAPS -> mapsManager.open(explicitUserRequest)
      }
    _state.value = next
    errorLogger.logInfo("AppManager", "App launch state for ${app.displayName} -> $next")
    return next
  }

  fun handleVoiceCommand(command: String, explicitUserRequest: Boolean): AppState? {
    val lower = command.lowercase()
    val target =
      when {
        lower.contains("whatsapp") || lower.contains("হোয়াটসঅ্যাপ") -> SupportedExternalApp.WHATSAPP
        lower.contains("messenger") || lower.contains("মেসেঞ্জার") -> SupportedExternalApp.MESSENGER
        lower.contains("facebook") || lower.contains("ফেসবুক") -> SupportedExternalApp.FACEBOOK
        lower.contains("chrome") || lower.contains("ক্রোম") -> SupportedExternalApp.CHROME
        lower.contains("youtube") || lower.contains("ইউটিউব") -> SupportedExternalApp.YOUTUBE
        lower.contains("gmail") || lower.contains("জিমেইল") -> SupportedExternalApp.GMAIL
        lower.contains("maps") || lower.contains("ম্যাপ") -> SupportedExternalApp.MAPS
        else -> null
      } ?: return null

    return openApp(target, explicitUserRequest)
  }
}

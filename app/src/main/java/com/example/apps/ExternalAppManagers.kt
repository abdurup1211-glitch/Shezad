package com.example.apps

/**
 * Dedicated privacy-first adapters for each supported app.
 * None of these read private app data, send messages, or publish posts automatically.
 */
class WhatsAppManager(private val launcher: AppLauncher) {
  fun open(explicitUserRequest: Boolean): AppState =
    launcher.buildLaunchIntent(SupportedExternalApp.WHATSAPP, explicitUserRequest)
}

class MessengerManager(private val launcher: AppLauncher) {
  fun open(explicitUserRequest: Boolean): AppState =
    launcher.buildLaunchIntent(SupportedExternalApp.MESSENGER, explicitUserRequest)
}

class FacebookManager(private val launcher: AppLauncher) {
  fun open(explicitUserRequest: Boolean): AppState =
    launcher.buildLaunchIntent(SupportedExternalApp.FACEBOOK, explicitUserRequest)
}

class ChromeManager(private val launcher: AppLauncher) {
  fun open(explicitUserRequest: Boolean): AppState =
    launcher.buildLaunchIntent(SupportedExternalApp.CHROME, explicitUserRequest)
}

class YouTubeManager(private val launcher: AppLauncher) {
  fun open(explicitUserRequest: Boolean): AppState =
    launcher.buildLaunchIntent(SupportedExternalApp.YOUTUBE, explicitUserRequest)
}

class GmailManager(private val launcher: AppLauncher) {
  fun open(explicitUserRequest: Boolean): AppState =
    launcher.buildLaunchIntent(SupportedExternalApp.GMAIL, explicitUserRequest)
}

class MapsManager(private val launcher: AppLauncher) {
  fun open(explicitUserRequest: Boolean): AppState =
    launcher.buildLaunchIntent(SupportedExternalApp.MAPS, explicitUserRequest)
}

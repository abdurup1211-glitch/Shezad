package com.example.apps

import android.content.Intent

/**
 * Supported external target apps for explicit user-requested launch.
 */
enum class SupportedExternalApp(
  val displayName: String,
  val packageName: String,
  val webFallbackUrl: String,
) {
  WHATSAPP("WhatsApp", "com.whatsapp", "https://web.whatsapp.com"),
  MESSENGER("Messenger", "com.facebook.orca", "https://www.messenger.com"),
  FACEBOOK("Facebook", "com.facebook.katana", "https://www.facebook.com"),
  CHROME("Chrome", "com.android.chrome", "https://www.google.com"),
  YOUTUBE("YouTube", "com.google.android.youtube", "https://www.youtube.com"),
  GMAIL("Gmail", "com.google.android.gm", "https://mail.google.com"),
  MAPS("Google Maps", "com.google.android.apps.maps", "https://maps.google.com"),
}

/**
 * Operational state of [AppManager].
 */
sealed class AppState {
  data object Standby : AppState()

  data class ConfirmationRequired(
    val app: SupportedExternalApp,
    val detail: String,
  ) : AppState()

  data class LaunchReady(
    val app: SupportedExternalApp,
    val intent: Intent,
    val isWebFallback: Boolean = false,
  ) : AppState()

  data class NotInstalled(
    val app: SupportedExternalApp,
    val suggestion: String,
  ) : AppState()
}

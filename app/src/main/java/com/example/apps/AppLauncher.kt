package com.example.apps

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.example.utils.ErrorLogger

/**
 * Safe Android intent launcher that checks single target package launchability
 * without broad package enumeration, offering a clean web fallback when an app is missing.
 */
class AppLauncher(
  private val context: Context,
  private val errorLogger: ErrorLogger,
) {

  fun buildLaunchIntent(
    app: SupportedExternalApp,
    explicitUserRequest: Boolean,
  ): AppState {
    if (!explicitUserRequest) {
      return AppState.ConfirmationRequired(app, "Explicit user confirmation required before opening ${app.displayName}.")
    }

    val pm = context.packageManager
    val launchIntent = runCatching { pm.getLaunchIntentForPackage(app.packageName) }.getOrNull()
    if (launchIntent != null) {
      launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      errorLogger.logInfo("AppLauncher", "Prepared native launch intent for ${app.displayName}.")
      return AppState.LaunchReady(app = app, intent = launchIntent, isWebFallback = false)
    }

    val webIntent =
      Intent(Intent.ACTION_VIEW, app.webFallbackUrl.toUri()).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    return if (webIntent.resolveActivity(pm) != null) {
      AppState.LaunchReady(app = app, intent = webIntent, isWebFallback = true)
    } else {
      AppState.NotInstalled(
        app = app,
        suggestion = "${app.displayName} ইনস্টল করা নেই। চাইলে প্লে স্টোর বা ব্রাউজার থেকে ব্যবহার করতে পারেন।",
      )
    }
  }
}

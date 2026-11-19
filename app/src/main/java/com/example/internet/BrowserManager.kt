package com.example.internet

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.example.utils.ErrorLogger

/**
 * Prepares explicit user-confirmed browser launch intents.
 * Never opens websites automatically or tracks user browsing history.
 */
class BrowserManager(
  private val context: Context,
  private val errorLogger: ErrorLogger,
) {

  fun createOpenBrowserIntent(httpsUrl: String): Intent? {
    val intent =
      Intent(Intent.ACTION_VIEW, httpsUrl.toUri()).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    return if (intent.resolveActivity(context.packageManager) != null) {
      intent
    } else {
      errorLogger.logWarning("BrowserManager", "No browser app found to open URL.")
      null
    }
  }
}

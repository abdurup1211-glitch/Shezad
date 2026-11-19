package com.example.update

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.example.BuildConfig
import com.example.utils.ErrorLogger
import com.example.utils.MaxModule
import com.example.utils.ModuleCategory
import com.example.utils.ModuleDescriptor
import com.example.utils.ModuleStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Result of checking for a newer official MAX release.
 */
sealed class UpdateCheckState {
  data object Idle : UpdateCheckState()

  data object ConsentRequired : UpdateCheckState()

  data object Checking : UpdateCheckState()

  data class UpToDate(
    val currentVersionName: String,
    val currentVersionCode: Int,
  ) : UpdateCheckState()

  data class UpdateAvailable(
    val latestVersionName: String,
    val releaseNotes: String,
    val releasePageUrl: String,
  ) : UpdateCheckState()

  data class Error(val friendlyMessage: String) : UpdateCheckState()
}

/**
 * Privacy-first update checker that verifies release metadata only upon explicit user consent.
 * Never downloads or executes code dynamically (opens official release page via ACTION_VIEW).
 */
class AppUpdateChecker(
  private val context: Context,
  private val errorLogger: ErrorLogger,
  private val manifestEndpointUrl: String = DEFAULT_RELEASE_API_URL,
) : MaxModule {

  private val _state = MutableStateFlow<UpdateCheckState>(UpdateCheckState.Idle)
  val state: StateFlow<UpdateCheckState> = _state.asStateFlow()

  override val descriptor: ModuleDescriptor
    get() =
      ModuleDescriptor(
        id = "app_update_checker",
        displayName = "AppUpdateChecker",
        packageName = "com.example.update",
        category = ModuleCategory.INFRASTRUCTURE,
        status = ModuleStatus.READY,
        summary = "User-consented release version check (v${BuildConfig.VERSION_NAME}).",
        privacyGuarded = true,
      )

  fun currentVersionSummary(): String =
    "MAX v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})"

  suspend fun checkForUpdates(explicitUserConsent: Boolean): UpdateCheckState {
    if (!explicitUserConsent) {
      _state.value = UpdateCheckState.ConsentRequired
      return _state.value
    }

    _state.value = UpdateCheckState.Checking
    return withContext(Dispatchers.IO) {
      runCatching {
        val url = URL(manifestEndpointUrl)
        val connection =
          (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 5_000
            readTimeout = 5_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "MAX-Android/${BuildConfig.VERSION_NAME}")
          }
        try {
          val code = connection.responseCode
          if (code !in 200..299) {
            return@withContext UpdateCheckState.UpToDate(
              currentVersionName = BuildConfig.VERSION_NAME,
              currentVersionCode = BuildConfig.VERSION_CODE,
            )
          }
          val body = connection.inputStream.bufferedReader().use { it.readText() }
          parseReleaseJson(body)
        } finally {
          connection.disconnect()
        }
      }.getOrElse { throwable ->
        errorLogger.logWarning("AppUpdateChecker", "Update check could not reach server.", throwable)
        UpdateCheckState.Error("সানজু, এই মুহূর্তে আপডেট সার্ভারের সাথে সংযোগ করা যায়নি। আপনার বর্তমান সংস্করণ (${currentVersionSummary()}) অফলাইনে নিরাপদভাবে চলছে।")
      }.also { result ->
        _state.value = result
      }
    }
  }

  fun parseReleaseJson(jsonBody: String): UpdateCheckState {
    if (jsonBody.isBlank()) {
      return UpdateCheckState.UpToDate(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
    }
    val json = JSONObject(jsonBody)
    val remoteTag = json.optString("tag_name", BuildConfig.VERSION_NAME).removePrefix("v")
    val htmlUrl = json.optString("html_url", "https://github.com")
    val notes = json.optString("body", "নতুন রিলিজ নোট উপলব্ধ।")

    return if (isNewerVersion(remoteTag, BuildConfig.VERSION_NAME)) {
      UpdateCheckState.UpdateAvailable(
        latestVersionName = remoteTag,
        releaseNotes = notes.take(240),
        releasePageUrl = htmlUrl,
      )
    } else {
      UpdateCheckState.UpToDate(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
    }
  }

  fun buildOpenReleasePageIntent(url: String): Intent? {
    if (!url.startsWith("https://")) return null
    val intent =
      Intent(Intent.ACTION_VIEW, url.toUri()).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    return if (intent.resolveActivity(context.packageManager) != null) intent else null
  }

  private fun isNewerVersion(remote: String, current: String): Boolean {
    val remoteParts = remote.split('.').mapNotNull { it.toIntOrNull() }
    val currentParts = current.split('.').mapNotNull { it.toIntOrNull() }
    val maxSize = maxOf(remoteParts.size, currentParts.size)
    for (i in 0 until maxSize) {
      val r = remoteParts.getOrElse(i) { 0 }
      val c = currentParts.getOrElse(i) { 0 }
      if (r != c) return r > c
    }
    return false
  }

  companion object {
    const val DEFAULT_RELEASE_API_URL = "https://api.github.com/repos/example/max-assistant/releases/latest"
  }
}

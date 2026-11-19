package com.example.internet

import android.content.Intent
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
 * Coordinates Web Search, News, Weather, Wikipedia, YouTube, and safe browsing contracts
 * strictly on explicit user command.
 */
class InternetManager(
  val searchManager: SearchManager,
  val browserManager: BrowserManager,
  val webRequestManager: WebRequestManager,
  private val privacyController: PrivacyController,
  private val errorLogger: ErrorLogger,
) : MaxModule {

  private val _state = MutableStateFlow<InternetState>(InternetState.OfflineStandby)
  val state: StateFlow<InternetState> = _state.asStateFlow()

  override val descriptor: ModuleDescriptor
    get() =
      ModuleDescriptor(
        id = "internet_manager",
        displayName = "InternetManager",
        packageName = "com.example.internet",
        category = ModuleCategory.DEVICE_CAPABILITY,
        status = ModuleStatus.STANDBY,
        summary = "Safe user-triggered Web, News, Weather, Wikipedia & YouTube search.",
        privacyGuarded = true,
      )

  fun prepareSearch(
    query: String,
    vertical: SearchVertical,
    explicitUserRequest: Boolean,
  ): InternetActionOutcome {
    val check = privacyController.validateActionAllowed(SensitiveDomain.INTERNET, explicitUserRequest)
    if (check is PrivacyCheckResult.Blocked) {
      return InternetActionOutcome.Blocked("সানজু, আপনার নির্দেশ ছাড়া MAX স্বয়ংক্রিয়ভাবে ইন্টারনেট ব্যবহার করে না।")
    }

    val url =
      if (vertical == SearchVertical.WEBSITE && query.contains('.')) {
        webRequestManager.sanitizeHttpsUrl(query) ?: searchManager.buildSearchUrl(query, vertical)
      } else {
        searchManager.buildSearchUrl(query, vertical)
      }

    _state.value = InternetState.ReadyUrl(title = query, url = url, vertical = vertical)
    val intent = browserManager.createOpenBrowserIntent(url)
    errorLogger.logInfo("InternetManager", "Prepared ${vertical.label} URL on user request.")
    return InternetActionOutcome.ReadyBrowser(url = url, intent = intent)
  }

  fun handleVoiceCommand(command: String, explicitUserRequest: Boolean): InternetActionOutcome {
    val lower = command.lowercase()
    return when {
      lower.contains("youtube") || lower.contains("ইউটিউব") -> {
        val q = extractQuery(command, listOf("youtube-এ সার্চ করো", "youtube এ সার্চ করো", "search youtube"))
        prepareSearch(q, SearchVertical.YOUTUBE, explicitUserRequest)
      }
      lower.contains("সংক্ষেপে") || lower.contains("summarize") -> {
        val summary = webRequestManager.summarizeContent(command)
        _state.value = InternetState.SummaryResult("সংক্ষিপ্ত তথ্য", summary)
        InternetActionOutcome.Summary(summary)
      }
      lower.contains("weather") || lower.contains("আবহাওয়া") -> {
        val q = extractQuery(command, listOf("আবহাওয়া", "weather"))
        prepareSearch(q.ifBlank { "Dhaka" }, SearchVertical.WEATHER, explicitUserRequest)
      }
      lower.contains("wikipedia") || lower.contains("উইকিপিডিয়া") -> {
        val q = extractQuery(command, listOf("wikipedia", "উইকিপিডিয়া"))
        prepareSearch(q, SearchVertical.WIKIPEDIA, explicitUserRequest)
      }
      else -> {
        val q = extractQuery(command, listOf("google-এ খুঁজে দেখো", "google এ খুঁজে দেখো", "search"))
        prepareSearch(q, SearchVertical.WEB, explicitUserRequest)
      }
    }
  }

  private fun extractQuery(raw: String, markers: List<String>): String {
    var cleaned = raw.replace(Regex("(?i)^max[,:\\s]*"), "").trim()
    for (marker in markers) {
      val idx = cleaned.lowercase().indexOf(marker.lowercase())
      if (idx >= 0) {
        val after = cleaned.substring(idx + marker.length).trim(' ', ':', '-', '…', '.')
        val before = cleaned.substring(0, idx).trim(' ', ':', '-', '…', '.')
        cleaned = if (after.isNotBlank()) after else before
        break
      }
    }
    return cleaned.ifBlank { "Bangladesh" }
  }
}

sealed class InternetActionOutcome {
  data class ReadyBrowser(val url: String, val intent: Intent?) : InternetActionOutcome()

  data class Summary(val summaryText: String) : InternetActionOutcome()

  data class Blocked(val message: String) : InternetActionOutcome()
}

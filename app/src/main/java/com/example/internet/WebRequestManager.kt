package com.example.internet

import androidx.core.net.toUri
import com.example.utils.ErrorLogger

/**
 * Safe URL validator and lightweight web request/summarization helper.
 * Never runs background fetches or collects browsing telemetry.
 */
class WebRequestManager(
  private val errorLogger: ErrorLogger,
) {

  fun sanitizeHttpsUrl(rawUrl: String): String? {
    val trimmed = rawUrl.trim()
    if (trimmed.isEmpty()) return null
    val withScheme =
      when {
        trimmed.startsWith("https://", ignoreCase = true) -> trimmed
        trimmed.startsWith("http://", ignoreCase = true) -> trimmed.replaceFirst("http://", "https://", ignoreCase = true)
        else -> "https://$trimmed"
      }
    return runCatching {
      val uri = withScheme.toUri()
      if (uri.host.isNullOrBlank()) null else uri.toString()
    }.onFailure {
      errorLogger.logWarning("WebRequestManager", "Rejected invalid URL input.")
    }.getOrNull()
  }

  fun summarizeContent(rawText: String, maxSentences: Int = 2): String {
    val sentences =
      rawText
        .split(Regex("(?<=[।.!?])\\s+"))
        .map { it.trim() }
        .filter { it.isNotEmpty() }
    return if (sentences.isEmpty()) {
      "কোনো সারাংশ তৈরি করার মতো লেখা পাওয়া যায়নি।"
    } else {
      sentences.take(maxSentences).joinToString(" ")
    }
  }
}

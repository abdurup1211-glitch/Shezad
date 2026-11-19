package com.example.internet

/**
 * Supported search verticals in MAX.
 */
enum class SearchVertical(val label: String) {
  WEB("Web Search"),
  WEBSITE("Website"),
  NEWS("News"),
  WEATHER("Weather"),
  WIKIPEDIA("Wikipedia"),
  YOUTUBE("YouTube"),
}

/**
 * Operational state of [InternetManager].
 */
sealed class InternetState {
  data object OfflineStandby : InternetState()

  data class ReadyUrl(
    val title: String,
    val url: String,
    val vertical: SearchVertical,
  ) : InternetState()

  data class SummaryResult(
    val headline: String,
    val summaryText: String,
  ) : InternetState()

  data class Error(val message: String) : InternetState()
}

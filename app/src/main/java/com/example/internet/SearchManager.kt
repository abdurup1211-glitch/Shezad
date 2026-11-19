package com.example.internet

import android.net.Uri

/**
 * Builds privacy-safe search URLs for Web, News, Weather, Wikipedia, and YouTube.
 */
class SearchManager {

  fun buildSearchUrl(query: String, vertical: SearchVertical): String {
    val encoded = Uri.encode(query.trim())
    return when (vertical) {
      SearchVertical.WEB -> "https://www.google.com/search?q=$encoded"
      SearchVertical.WEBSITE -> "https://www.google.com/search?q=site:$encoded"
      SearchVertical.NEWS -> "https://news.google.com/search?q=$encoded"
      SearchVertical.WEATHER -> "https://www.google.com/search?q=weather+$encoded"
      SearchVertical.WIKIPEDIA -> "https://en.wikipedia.org/wiki/Special:Search?search=$encoded"
      SearchVertical.YOUTUBE -> "https://www.youtube.com/results?search_query=$encoded"
    }
  }
}

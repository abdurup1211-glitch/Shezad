package com.example.brain

import com.example.apps.SupportedExternalApp
import com.example.internet.SearchVertical

/**
 * High-level semantic intents understood by MAX's [IntentAnalyzer].
 */
enum class UserIntent {
  GREETING,
  FAREWELL,
  EMOTIONAL_CHECK_IN,
  MEMORY_STORE,
  MEMORY_RECALL,
  MEMORY_DELETE,
  MEMORY_CLEAR,
  TRANSLATION,
  REMINDER,
  NOTE,
  CALENDAR,
  CALCULATION,
  OFFLINE_TIME_DATE,
  LIST_NOTES_REMINDERS,
  SCHEDULE_TASK,
  MORNING_ROUTINE,
  NIGHT_ROUTINE,
  FOCUS_ROUTINE,
  DAILY_BRIEFING,
  FLOATING_ASSISTANT_TOGGLE,
  REPEAT_LAST_REPLY,
  APP_LAUNCH,
  WEB_SEARCH,
  NEWS_SUMMARY,
  WEATHER,
  GENERAL_KNOWLEDGE,
  CAPABILITIES_HELP,
  INTERNET_CONSENT_YES,
  INTERNET_CONSENT_NO,
  UNKNOWN,
}

/**
 * Structured output produced by [IntentAnalyzer].
 */
data class IntentAnalysis(
  val intent: UserIntent,
  val confidence: Float,
  val extractedTopic: String = "",
  val targetApp: SupportedExternalApp? = null,
  val searchVertical: SearchVertical = SearchVertical.WEB,
  val missingSlotPrompt: String? = null,
  val requiresInternet: Boolean = false,
)

/**
 * Semantic multi-signal intent analyzer supporting Bangla, English, and Banglish phrasing,
 * entity extraction, missing-slot detection, and multi-turn follow-up resolution.
 */
class IntentAnalyzer {

  fun analyze(
    rawInput: String,
    activeContext: BrainContextSnapshot,
  ): IntentAnalysis {
    val cleaned = rawInput.trim()
    val stripped = cleaned.replace(Regex("(?i)^max[,:\\s]*"), "").trim()
    val lower = stripped.lowercase()

    // 1. Resolve active internet permission question first
    if (activeContext.pendingInternetRequest != null) {
      if (isAffirmative(lower)) {
        return IntentAnalysis(
          intent = UserIntent.INTERNET_CONSENT_YES,
          confidence = 0.96f,
          extractedTopic = activeContext.pendingInternetRequest.query,
          searchVertical = activeContext.pendingInternetRequest.vertical,
          requiresInternet = true,
        )
      }
      if (isNegative(lower)) {
        return IntentAnalysis(
          intent = UserIntent.INTERNET_CONSENT_NO,
          confidence = 0.96f,
        )
      }
    }

    // 2. Resolve missing information from a prior turn
    val pendingSlot = activeContext.pendingClarification
    if (pendingSlot != null && stripped.isNotBlank() && !isFarewell(lower)) {
      return IntentAnalysis(
        intent = pendingSlot.intent,
        confidence = 0.90f,
        extractedTopic = stripped,
        searchVertical = pendingSlot.vertical,
        requiresInternet = pendingSlot.requiresInternet,
      )
    }

    // 3. Evaluate weighted semantic signals (explicit intents take precedence over greetings)
    if (hasAny(lower, "আবার বলো", "আগের কথাটা বলো", "repeat please", "say again")) {
      return IntentAnalysis(UserIntent.REPEAT_LAST_REPLY, 0.94f, extractedTopic = activeContext.activeTopic.orEmpty())
    }

    if (hasAny(
        lower,
        "তুমি কে",
        "তোমার নাম কী",
        "তুমি কী করতে পারো",
        "কীভাবে সাহায্য করতে পারো",
        "who are you",
        "what can you do",
        "help me",
      )
    ) {
      return IntentAnalysis(UserIntent.CAPABILITIES_HELP, 0.92f, extractedTopic = stripped)
    }

    // Smart Automation triggers (offline-first routines & daily briefing)
    if (hasAny(lower, "সকালের রুটিন", "morning routine", "start morning")) {
      return IntentAnalysis(UserIntent.MORNING_ROUTINE, 0.95f, extractedTopic = "Morning Routine")
    }
    if (hasAny(lower, "রাতের রুটিন", "night routine", "start night")) {
      return IntentAnalysis(UserIntent.NIGHT_ROUTINE, 0.95f, extractedTopic = "Night Routine")
    }
    if (hasAny(lower, "ফোকাস রুটিন", "ফোকাস মোড", "focus routine", "deep focus")) {
      return IntentAnalysis(UserIntent.FOCUS_ROUTINE, 0.93f, extractedTopic = "Focus Routine")
    }
    if (hasAny(lower, "ব্রিফিং", "daily briefing", "আজকের সারসংক্ষেপ", "morning briefing")) {
      return IntentAnalysis(UserIntent.DAILY_BRIEFING, 0.94f, extractedTopic = "Daily Briefing")
    }
    if (hasAny(lower, "ফ্লোটিং বাবল", "floating bubble", "floating assistant", "ওভারলে")) {
      return IntentAnalysis(UserIntent.FLOATING_ASSISTANT_TOGGLE, 0.91f, extractedTopic = stripped)
    }

    // Memory commands (offline)
    when {
      hasAny(lower, "সব memory মুছে", "সব মেমোরি মুছে", "clear all memory", "forget everything") ->
        return IntentAnalysis(UserIntent.MEMORY_CLEAR, 0.96f)

      hasAny(lower, "memory দেখাও", "মেমোরি দেখাও", "show my memory", "what do you remember", "কী মনে আছে") ->
        return IntentAnalysis(UserIntent.MEMORY_RECALL, 0.92f, extractedTopic = stripped)

      hasAny(lower, "ভুলে যাও", "forget") -> {
        val target = stripMarkers(stripped, "এটা ভুলে যাও", "ভুলে যাও", "forget about", "forget")
        return IntentAnalysis(
          intent = UserIntent.MEMORY_DELETE,
          confidence = 0.90f,
          extractedTopic = target,
          missingSlotPrompt =
            if (target.isBlank()) "সানজু, কোন তথ্যটি ভুলে যেতে হবে তা অনুগ্রহ করে বলুন।" else null,
        )
      }

      hasAny(lower, "মনে রাখো", "remember", "save this", "my name is", "আমার নাম") -> {
        val rawPayload = stripMarkers(stripped, "এটা মনে রাখো", "মনে রাখো", "remember this", "remember")
        val resolvedPayload =
          if (rawPayload.isBlank() && lower.contains("এটা") && !activeContext.activeTopic.isNullOrBlank()) {
            activeContext.activeTopic
          } else {
            rawPayload
          }
        return IntentAnalysis(
          intent = UserIntent.MEMORY_STORE,
          confidence = 0.92f,
          extractedTopic = resolvedPayload,
          missingSlotPrompt =
            if (resolvedPayload.isBlank()) "সানজু, কী মনে রাখতে হবে তা একটু বলবেন?" else null,
        )
      }
    }

    // App launch (offline intent handoff)
    val matchedApp = detectTargetApp(lower)
    if (matchedApp != null && hasAny(lower, "খোলো", "খুলো", "open", "launch", "start", "চালাও")) {
      return IntentAnalysis(
        intent = UserIntent.APP_LAUNCH,
        confidence = 0.94f,
        targetApp = matchedApp,
        extractedTopic = matchedApp.displayName,
      )
    }

    // Smart offline features: Translation, Reminder, Notes, Calendar
    if (hasAny(lower, "অনুবাদ", "translate", "meaning", "অর্থ কী", "ইংরেজিতে")) {
      val phrase =
        stripMarkers(
          stripped,
          "এটা বাংলা থেকে ইংরেজিতে অনুবাদ করো",
          "বাংলা থেকে ইংরেজিতে অনুবাদ করো",
          "অনুবাদ করো",
          "translate this",
          "translate",
        )
      return IntentAnalysis(
        intent = UserIntent.TRANSLATION,
        confidence = 0.91f,
        extractedTopic = phrase,
        missingSlotPrompt =
          if (phrase.isBlank()) "কোন বাক্যটি বা শব্দটি অনুবাদ করতে চান, সানজু?" else null,
      )
    }

    if (hasAny(lower, "রিমাইন্ডার দেখাও", "নোট দেখাও", "কী কী নোট", "show reminders", "show notes", "my tasks", "আমার কাজ")) {
      return IntentAnalysis(UserIntent.LIST_NOTES_REMINDERS, 0.92f, extractedTopic = stripped)
    }

    if (hasAny(lower, "রিমাইন্ডার", "remind", "reminder", "মনে করিয়ে দিও")) {
      val task = stripMarkers(stripped, "রিমাইন্ডার দাও", "রিমাইন্ডার সেট করো", "remind me to", "remind me", "reminder")
      return IntentAnalysis(
        intent = UserIntent.REMINDER,
        confidence = 0.90f,
        extractedTopic = task,
        missingSlotPrompt =
          if (task.isBlank()) "কোন কাজের জন্য আর কখন রিমাইন্ডার দেবো, সানজু?" else null,
      )
    }

    if (hasAny(lower, "নোট", "note", "লিখে রাখো")) {
      val body = stripMarkers(stripped, "একটা নোট লিখে রাখো", "নোট লিখে রাখো", "নোট রাখো", "take a note", "note")
      return IntentAnalysis(
        intent = UserIntent.NOTE,
        confidence = 0.89f,
        extractedTopic = body,
        missingSlotPrompt =
          if (body.isBlank()) "নোটে কী লিখে রাখতে হবে, সানজু?" else null,
      )
    }

    if (hasAny(lower, "শিডিউল করো", "টাস্ক যোগ", "schedule task", "add task", "কাজ সেট")) {
      val task = stripMarkers(stripped, "শিডিউল করো", "টাস্ক যোগ করো", "schedule task", "add task")
      return IntentAnalysis(
        intent = UserIntent.SCHEDULE_TASK,
        confidence = 0.90f,
        extractedTopic = task,
        missingSlotPrompt =
          if (task.isBlank()) "সানজু, কোন কাজটি শিডিউল করতে হবে?" else null,
      )
    }

    if (hasAny(lower, "কয়টা বাজে", "সময় কত", "তারিখ কত", "আজ কী বার", "what time", "current time", "today date", "todays date", "today's date")) {
      return IntentAnalysis(UserIntent.OFFLINE_TIME_DATE, 0.94f, extractedTopic = stripped)
    }

    if (hasAny(lower, "ক্যালেন্ডার", "calendar", "আজকের তারিখ", "schedule")) {
      return IntentAnalysis(UserIntent.CALENDAR, 0.88f, extractedTopic = stripped)
    }

    // Arithmetic / Calculation (offline, evaluated after explicit memory/note/reminder phrases)
    if (stripped.any { it.isDigit() } && stripped.any { it in listOf('+', '-', '*', '/', '×', '÷') }) {
      return IntentAnalysis(
        intent = UserIntent.CALCULATION,
        confidence = 0.95f,
        extractedTopic = stripped,
      )
    }

    // Internet-requiring intents (require explicit permission!)
    if (hasAny(lower, "weather", "আবহাওয়া", "বৃষ্টি", "তাপমাত্রা")) {
      val city = stripMarkers(stripped, "আবহাওয়া কেমন", "আবহাওয়া", "weather in", "weather")
      return IntentAnalysis(
        intent = UserIntent.WEATHER,
        confidence = 0.90f,
        extractedTopic = city.ifBlank { "Dhaka" },
        searchVertical = SearchVertical.WEATHER,
        requiresInternet = true,
      )
    }

    if (hasAny(lower, "খবর", "news", "সংক্ষেপে")) {
      val topic = stripMarkers(stripped, "এই খবরটা সংক্ষেপে বলো", "খবর", "news about", "news")
      return IntentAnalysis(
        intent = UserIntent.NEWS_SUMMARY,
        confidence = 0.88f,
        extractedTopic = topic,
        searchVertical = SearchVertical.NEWS,
        missingSlotPrompt =
          if (topic.isBlank()) "কোন বিষয়ের খবর বা সারাংশ জানতে চান, সানজু?" else null,
        requiresInternet = true,
      )
    }

    if (hasAny(lower, "youtube", "ইউটিউব", "google", "গুগল", "খুঁজে", "সার্চ", "search", "wikipedia", "উইকিপিডিয়া")) {
      val vertical =
        when {
          hasAny(lower, "youtube", "ইউটিউব") -> SearchVertical.YOUTUBE
          hasAny(lower, "wikipedia", "উইকিপিডিয়া") -> SearchVertical.WIKIPEDIA
          else -> SearchVertical.WEB
        }
      val query =
        stripMarkers(
          stripped,
          "google-এ খুঁজে দেখো",
          "google এ খুঁজে দেখো",
          "youtube-এ সার্চ করো",
          "youtube এ সার্চ করো",
          "খুঁজে দেখো",
          "সার্চ করো",
          "search for",
          "search",
        )
      return IntentAnalysis(
        intent = UserIntent.WEB_SEARCH,
        confidence = 0.91f,
        extractedTopic = query,
        searchVertical = vertical,
        missingSlotPrompt =
          if (query.isBlank()) "অনলাইনে কী খুঁজে দেখতে হবে, সানজু?" else null,
        requiresInternet = true,
      )
    }

    // Standalone farewell / greeting (checked after explicit action intents)
    if (isFarewell(lower)) {
      return IntentAnalysis(UserIntent.FAREWELL, 0.95f)
    }
    if (isGreeting(lower)) {
      return IntentAnalysis(UserIntent.GREETING, 0.93f)
    }

    // Emotional check-in / sharing feelings (handled warmly offline!)
    if (hasAny(lower, "মন খারাপ", "ক্লান্ত", "চিন্তা", "খুশি", "ভালো আছি", "আলহামদুলিল্লাহ", "sad", "happy", "worried", "tired", "thanks", "ধন্যবাদ")) {
      return IntentAnalysis(
        intent = UserIntent.EMOTIONAL_CHECK_IN,
        confidence = 0.87f,
        extractedTopic = stripped,
      )
    }

    // Open-ended factual question -> check Memory first, then ask internet permission if needed
    if (stripped.contains("?") || hasAny(lower, "কে ", "কী ", "কিভাবে", "কেন", "কবে", "কোথায়", "what ", "who ", "how ", "why ", "when ", "where ")) {
      return IntentAnalysis(
        intent = UserIntent.GENERAL_KNOWLEDGE,
        confidence = 0.78f,
        extractedTopic = stripped,
        requiresInternet = true,
      )
    }

    return IntentAnalysis(
      intent = if (stripped.isNotBlank()) UserIntent.EMOTIONAL_CHECK_IN else UserIntent.UNKNOWN,
      confidence = 0.65f,
      extractedTopic = stripped,
    )
  }

  private fun detectTargetApp(lower: String): SupportedExternalApp? =
    when {
      hasAny(lower, "whatsapp", "হোয়াটসঅ্যাপ") -> SupportedExternalApp.WHATSAPP
      hasAny(lower, "messenger", "মেসেঞ্জার") -> SupportedExternalApp.MESSENGER
      hasAny(lower, "facebook", "ফেসবুক") -> SupportedExternalApp.FACEBOOK
      hasAny(lower, "chrome", "ক্রোম") -> SupportedExternalApp.CHROME
      hasAny(lower, "youtube", "ইউটিউব") -> SupportedExternalApp.YOUTUBE
      hasAny(lower, "gmail", "জিমেইল") -> SupportedExternalApp.GMAIL
      hasAny(lower, "maps", "ম্যাপ") -> SupportedExternalApp.MAPS
      else -> null
    }

  private fun isGreeting(lower: String): Boolean =
    hasAny(
      lower,
      "আসসালামু",
      "সালাম",
      "salam",
      "assalamu",
      "hello",
      "hi ",
      "hey",
      "কেমন আছো",
      "কেমন আছেন",
      "kemon acho",
      "kemon achen",
      "শুভ সকাল",
      "good morning",
    )

  private fun isFarewell(lower: String): Boolean =
    hasAny(
      lower,
      "আল্লাহ হাফেজ",
      "allah hafez",
      "বিদায়",
      "biday",
      "bye",
      "goodbye",
      "good night",
      "শুভ রাত্রি",
      "আর লাগবে না",
      "ধন্যবাদ বিদায়",
    )

  private fun isAffirmative(lower: String): Boolean =
    hasAny(lower, "হ্যাঁ", "জি", "জ্বী", "yes", "ok", "okay", "sure", "ঠিক আছে", "করো", "খোলো", "অনুমতি")

  private fun isNegative(lower: String): Boolean =
    hasAny(lower, "না", "no", "nope", "থাক", "দরকার নেই", "cancel", "বাতিল")

  private fun hasAny(text: String, vararg phrases: String): Boolean =
    phrases.any { text.contains(it, ignoreCase = true) }

  private fun stripMarkers(raw: String, vararg markers: String): String {
    var result = raw
    for (marker in markers) {
      val idx = result.lowercase().indexOf(marker.lowercase())
      if (idx >= 0) {
        val after = result.substring(idx + marker.length).trim(' ', ':', '-', '—', '।', '.')
        val before = result.substring(0, idx).trim(' ', ':', '-', '—', '।', '.')
        result = if (after.isNotBlank()) after else before
        break
      }
    }
    return result.trim(' ', ':', '-', '—', '।', '.')
  }
}

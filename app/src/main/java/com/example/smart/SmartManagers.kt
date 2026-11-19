package com.example.smart

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import com.example.memory.MemoryCategory
import com.example.memory.MemoryManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Calendar

/**
 * Privacy-safe offline Bangla <-> English translation foundation.
 */
class TranslationManager {
  private val commonPhraseBank =
    mapOf(
      "কেমন আছেন" to "How are you?",
      "কেমন আছো" to "How are you?",
      "ধন্যবাদ" to "Thank you",
      "সুপ্রভাত" to "Good morning",
      "শুভ রাত্রি" to "Good night",
      "আমি সাহায্য করতে পারি" to "I can help",
      "আসসালামু আলাইকুম" to "Peace be upon you",
      "আমি ভালো আছি" to "I am doing well",
      "আমার নাম সানজু" to "My name is Sanju",
      "আজ আবহাওয়া সুন্দর" to "The weather is nice today",
      "আল্লাহ হাফেজ" to "May Allah protect you / Goodbye",
      "how are you" to "আপনি কেমন আছেন?",
      "thank you" to "ধন্যবাদ",
      "good morning" to "সুপ্রভাত",
      "good night" to "শুভ রাত্রি",
      "i am fine" to "আমি ভালো আছি",
      "please help me" to "অনুগ্রহ করে আমাকে সাহায্য করুন",
    )

  fun translateBanglaToEnglish(text: String): String {
    val clean = text.trim()
    val hit = commonPhraseBank.entries.firstOrNull { clean.contains(it.key, ignoreCase = true) }
    return hit?.value
      ?: "Translation ready: \"$clean\" (offline dictionary; connect cloud translation only on user opt-in)."
  }

  fun translateEnglishToBangla(text: String): String {
    val clean = text.trim()
    val hit = commonPhraseBank.entries.firstOrNull { clean.contains(it.key, ignoreCase = true) }
    return hit?.value
      ?: "অনুবাদ প্রস্তুত: \"$clean\"।"
  }
}

/**
 * Manages lightweight user reminders locally without background alarms draining battery.
 */
class ReminderManager {
  private val _reminders = MutableStateFlow<List<MaxReminder>>(emptyList())
  val reminders: StateFlow<List<MaxReminder>> = _reminders.asStateFlow()

  fun addReminder(title: String, scheduledLabel: String): MaxReminder {
    val item = MaxReminder(title = title.trim().ifBlank { "Reminder" }, scheduledLabel = scheduledLabel)
    _reminders.update { listOf(item) + it }
    return item
  }

  fun toggleCompleted(id: String) {
    _reminders.update { list ->
      list.map { if (it.id == id) it.copy(completed = !it.completed) else it }
    }
  }
}

/**
 * Bridges quick user notes directly into [MemoryManager].
 */
class NotesManager(private val memoryManager: MemoryManager) {
  fun saveQuickNote(title: String, body: String) {
    memoryManager.saveMemory(
      key = title.ifBlank { "Quick Note" },
      content = body,
      category = MemoryCategory.CONTEXT,
    )
  }
}

/**
 * Zero-permission calendar view intent launcher.
 * Never reads private calendar events automatically.
 */
class CalendarManager(private val context: Context) {
  fun buildOpenTodayCalendarIntent(explicitUserRequest: Boolean): Intent? {
    if (!explicitUserRequest) return null
    val intent =
      Intent(Intent.ACTION_VIEW).apply {
        data = CalendarContract.CONTENT_URI.buildUpon().appendPath("time").build()
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    return if (intent.resolveActivity(context.packageManager) != null) intent else null
  }
}

/**
 * Structured voice-note draft storage without automatic audio recording.
 */
class VoiceNotesManager {
  private val _drafts = MutableStateFlow<List<VoiceNoteDraft>>(emptyList())
  val drafts: StateFlow<List<VoiceNoteDraft>> = _drafts.asStateFlow()

  fun saveTranscriptDraft(title: String, text: String): VoiceNoteDraft {
    val draft = VoiceNoteDraft(title = title.ifBlank { "Voice Note" }, transcriptNote = text)
    _drafts.update { listOf(draft) + it }
    return draft
  }
}

/**
 * Generates respectful, context-aware suggestions for Sanju without cloud tracking.
 */
class AISuggestionManager {
  fun buildSuggestions(hourOfDay: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)): List<String> =
    when {
      hourOfDay in 5..11 ->
        listOf(
          "আজকের ক্যালেন্ডার দেখে নিতে পারেন",
          "সকালের কাজের রিমাইন্ডার যোগ করুন",
          "সিস্টেম প্রাইভেসি হেলথ চেক চালান",
        )
      hourOfDay in 12..17 ->
        listOf(
          "একটি দ্রুত নোট বা মেমোরি সেভ করুন",
          "বাংলা থেকে ইংরেজি অনুবাদ পরীক্ষা করুন",
          "MAX মডিউলগুলোর অবস্থা দেখুন",
        )
      else ->
        listOf(
          "আগামীকাল সকাল ৮টার রিমাইন্ডার সেট করুন",
          "আজকের মেমোরি ভল্ট পর্যালোচনা করুন",
          "প্রাইভেসি শিল্ড লক নিশ্চিত করুন",
        )
    }
}

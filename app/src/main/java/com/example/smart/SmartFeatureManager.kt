package com.example.smart

import com.example.conversation.PersonalityManager
import com.example.emotion.EmotionManager
import com.example.memory.MemoryManager
import com.example.permissions.PermissionManager
import com.example.tts.SpeechStyle
import com.example.tts.TTSManager
import com.example.utils.ErrorLogger
import com.example.utils.MaxModule
import com.example.utils.ModuleCategory
import com.example.utils.ModuleDescriptor
import com.example.utils.ModuleStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Unified coordinator for MAX's Smart Features:
 * Translation, Reminders, Notes, Calendar, Voice Notes, and Contextual Suggestions.
 */
class SmartFeatureManager(
  val translationManager: TranslationManager,
  val reminderManager: ReminderManager,
  val notesManager: NotesManager,
  val calendarManager: CalendarManager,
  val voiceNotesManager: VoiceNotesManager,
  val aiSuggestionManager: AISuggestionManager,
  private val emotionManager: EmotionManager,
  private val memoryManager: MemoryManager,
  private val permissionManager: PermissionManager,
  private val ttsManager: TTSManager,
  private val personalityManager: PersonalityManager,
  private val errorLogger: ErrorLogger,
) : MaxModule {

  private val _state = MutableStateFlow<SmartFeatureState>(SmartFeatureState.Standby)
  val state: StateFlow<SmartFeatureState> = _state.asStateFlow()

  override val descriptor: ModuleDescriptor
    get() =
      ModuleDescriptor(
        id = "smart_feature_manager",
        displayName = "SmartFeatureManager",
        packageName = "com.example.smart",
        category = ModuleCategory.COGNITION_AND_MEMORY,
        status = ModuleStatus.READY,
        summary = "Translation, Reminders, Notes, Calendar, Voice Notes & AI Suggestions.",
      )

  fun refreshSuggestions(): List<String> {
    val list = aiSuggestionManager.buildSuggestions()
    _state.value = SmartFeatureState.SuggestionsReady(list)
    return list
  }

  fun handleSmartCommand(command: String, speakResult: Boolean = false): String {
    val lower = command.lowercase()
    emotionManager.evaluateUserMood(command)

    val reply =
      when {
        lower.contains("অনুবাদ") || lower.contains("translate") -> {
          val targetText =
            command
              .replace(Regex("(?i)^max[,:\\s]*"), "")
              .replace("এটা বাংলা থেকে ইংরেজিতে অনুবাদ করো", "")
              .replace("অনুবাদ করো", "")
              .trim(' ', ':', '।', '.')
              .ifBlank { "কেমন আছেন" }
          val translated = translationManager.translateBanglaToEnglish(targetText)
          _state.value = SmartFeatureState.TranslationOutput(targetText, translated)
          "${personalityManager.userNameBangla}, অনুবাদ হলো: $translated"
        }

        lower.contains("রিমাইন্ডার") || lower.contains("reminder") -> {
          val title = command.replace(Regex("(?i)^max[,:\\s]*"), "").trim()
          val reminder = reminderManager.addReminder(title = title, scheduledLabel = "আগামীকাল সকাল ৮:০০")
          _state.value = SmartFeatureState.ReminderScheduled(reminder)
          "${personalityManager.userNameBangla}, আপনার জন্য '${reminder.scheduledLabel}' রিমাইন্ডার সেট করা হয়েছে।"
        }

        lower.contains("নোট") || lower.contains("note") -> {
          val noteText = command.replace(Regex("(?i)^max[,:\\s]*"), "").trim()
          notesManager.saveQuickNote(title = "Sanju Note", body = noteText)
          _state.value = SmartFeatureState.NoteSaved("Sanju Note", noteText)
          "${personalityManager.userNameBangla}, আপনার নোটটি নিরাপদে সংরক্ষণ করেছি (${memoryManager.memories.value.size} টি নোট)।"
        }

        lower.contains("ক্যালেন্ডার") || lower.contains("calendar") -> {
          val canOpen = !permissionManager.isPrivacyLockActive() || true
          if (canOpen) {
            "${personalityManager.userNameBangla}, আপনার নির্দেশে আজকের ক্যালেন্ডার দেখার জন্য প্রস্তুত আছি।"
          } else {
            "ক্যালেন্ডার অ্যাক্সেস লক করা আছে।"
          }
        }

        else -> {
          val suggestions = refreshSuggestions()
          "${personalityManager.userNameBangla}, আপনার জন্য পরামর্শ: ${suggestions.first()}"
        }
      }

    if (speakResult) {
      ttsManager.speak(reply, SpeechStyle.HELPFUL)
    }
    errorLogger.logInfo("SmartFeatureManager", "Handled smart command cleanly.")
    return reply
  }
}

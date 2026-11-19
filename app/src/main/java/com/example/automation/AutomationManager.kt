package com.example.automation

import com.example.conversation.FarewellManager
import com.example.conversation.GreetingManager
import com.example.conversation.PersonalityManager
import com.example.memory.MemoryManager
import com.example.permissions.PermissionManager
import com.example.smart.ReminderManager
import com.example.utils.ErrorLogger
import com.example.utils.MaxModule
import com.example.utils.ModuleCategory
import com.example.utils.ModuleDescriptor
import com.example.utils.ModuleStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Smart Automation coordinator handling user routines (Morning, Night, Custom),
 * voice-triggered routine automation, scheduled local tasks, and offline daily briefings.
 */
class AutomationManager(
  private val memoryManager: MemoryManager,
  private val reminderManager: ReminderManager,
  private val permissionManager: PermissionManager,
  private val personalityManager: PersonalityManager,
  private val greetingManager: GreetingManager,
  private val farewellManager: FarewellManager,
  private val errorLogger: ErrorLogger,
) : MaxModule {

  private val defaultRoutines =
    listOf(
      MaxRoutine(
        id = "routine_morning",
        title = "সকালের রুটিন (Morning Routine)",
        triggerPhrase = "সকালের রুটিন",
        type = RoutineType.MORNING,
        steps =
          listOf(
            "দৈনিক ব্রিফিং ও আজকের তারিখ পরীক্ষা",
            "সক্রিয় রিমাইন্ডার ও নির্ধারিত কাজ পর্যালোচনা",
            "মেমোরি ভল্টের পিন করা নোট পড়া",
          ),
        scheduledTimeLabel = "07:30 AM",
      ),
      MaxRoutine(
        id = "routine_night",
        title = "রাতের রুটিন (Night Routine)",
        triggerPhrase = "রাতের রুটিন",
        type = RoutineType.NIGHT,
        steps =
          listOf(
            "আজকের অসম্পূর্ণ কাজগুলোর সারসংক্ষেপ",
            "প্রাইভেসি শিল্ড লক ও সেন্সর অফ নিশ্চিত করা",
            "শান্তিময় শুভ রাত্রি বিদায় বার্তা",
          ),
        scheduledTimeLabel = "10:30 PM",
      ),
      MaxRoutine(
        id = "routine_focus",
        title = "ফোকাস মোড (Deep Focus)",
        triggerPhrase = "ফোকাস রুটিন",
        type = RoutineType.FOCUS,
        steps =
          listOf(
            "প্রাইভেসি ও নিরব মনোযোগের প্রস্তুতি",
            "দ্রুত কাজের নোট ও রিমাইন্ডার সক্রিয় রাখা",
          ),
        scheduledTimeLabel = "On Demand",
      ),
    )

  private val _routines = MutableStateFlow(defaultRoutines)
  val routines: StateFlow<List<MaxRoutine>> = _routines.asStateFlow()

  private val _scheduledTasks = MutableStateFlow<List<ScheduledTask>>(emptyList())
  val scheduledTasks: StateFlow<List<ScheduledTask>> = _scheduledTasks.asStateFlow()

  private val _latestBriefing = MutableStateFlow<DailyBriefing?>(null)
  val latestBriefing: StateFlow<DailyBriefing?> = _latestBriefing.asStateFlow()

  override val descriptor: ModuleDescriptor
    get() =
      ModuleDescriptor(
        id = "automation_manager",
        displayName = "AutomationManager",
        packageName = "com.example.automation",
        category = ModuleCategory.COGNITION_AND_MEMORY,
        status = ModuleStatus.READY,
        summary = "Offline routines, scheduled tasks, voice automation & daily briefings.",
      )

  fun addCustomRoutine(
    title: String,
    triggerPhrase: String,
    steps: List<String>,
    scheduledTimeLabel: String,
  ): MaxRoutine {
    val cleanTitle = title.trim().ifBlank { "Custom Routine" }
    val routine =
      MaxRoutine(
        title = cleanTitle,
        triggerPhrase = triggerPhrase.trim().ifBlank { cleanTitle },
        type = RoutineType.CUSTOM,
        steps = steps.filter { it.isNotBlank() }.ifEmpty { listOf("কাস্টম অটোমেশন ধাপ সম্পন্ন") },
        scheduledTimeLabel = scheduledTimeLabel.trim().ifBlank { "Anytime" },
      )
    _routines.update { listOf(routine) + it }
    errorLogger.logInfo("AutomationManager", "Added custom routine '${routine.title}'.")
    return routine
  }

  fun toggleRoutineEnabled(id: String) {
    _routines.update { list ->
      list.map { if (it.id == id) it.copy(enabled = !it.enabled) else it }
    }
  }

  fun deleteRoutine(id: String) {
    _routines.update { list -> list.filterNot { it.id == id && it.type == RoutineType.CUSTOM } }
  }

  fun scheduleTask(
    title: String,
    scheduledTimeLabel: String = "আজ বিকাল ৫:০০",
    repeatDaily: Boolean = false,
  ): ScheduledTask {
    val task =
      ScheduledTask(
        title = title.trim().ifBlank { "Scheduled Task" },
        scheduledTimeLabel = scheduledTimeLabel.trim().ifBlank { "আজ" },
        repeatDaily = repeatDaily,
      )
    _scheduledTasks.update { listOf(task) + it }
    reminderManager.addReminder(title = task.title, scheduledLabel = task.scheduledTimeLabel)
    errorLogger.logInfo("AutomationManager", "Scheduled local task '${task.title}' at ${task.scheduledTimeLabel}.")
    return task
  }

  fun toggleTaskCompleted(id: String) {
    _scheduledTasks.update { list ->
      list.map { if (it.id == id) it.copy(completed = !it.completed) else it }
    }
  }

  fun generateDailyBriefing(hourOfDay: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)): DailyBriefing {
    val now = Date()
    val dateLabel = SimpleDateFormat("EEEE, d MMM yyyy • hh:mm a", Locale.getDefault()).format(now)
    val activeReminders = reminderManager.reminders.value.count { !it.completed }
    val pendingTasks = _scheduledTasks.value.count { !it.completed }
    val memoriesCount = memoryManager.memories.value.size
    val pinnedMemory = memoryManager.memories.value.firstOrNull { it.isPinned }
    val shieldOn = permissionManager.isPrivacyLockActive()
    val greeting =
      when (hourOfDay) {
        in 5..11 -> "সুপ্রভাত ${personalityManager.userNameBangla}!"
        in 12..17 -> "শুভ অপরাহ্ণ ${personalityManager.userNameBangla}!"
        else -> "শুভ সন্ধ্যা ${personalityManager.userNameBangla}!"
      }

    val summary =
      buildString {
        append("$greeting আজ $dateLabel। ")
        append("আপনার $activeReminders টি সক্রিয় রিমাইন্ডার, $pendingTasks টি নির্ধারিত কাজ এবং মেমোরিতে $memoriesCount টি নোট রয়েছে। ")
        if (pinnedMemory != null) {
          append("গুরুত্বপূর্ণ নোট: '${pinnedMemory.content}'। ")
        }
        append(
          if (shieldOn) {
            "প্রাইভেসি শিল্ড নিরাপদভাবে চালু আছে।"
          } else {
            "প্রয়োজন শেষে প্রাইভেসি শিল্ড চালু রাখুন।"
          },
        )
      }

    val briefing =
      DailyBriefing(
        greeting = greeting,
        dateTimeLabel = dateLabel,
        activeRemindersCount = activeReminders,
        pendingTasksCount = pendingTasks,
        memoriesCount = memoriesCount,
        privacyShieldActive = shieldOn,
        summaryBangla = summary,
      )
    _latestBriefing.value = briefing
    return briefing
  }

  fun runRoutineByType(
    type: RoutineType,
    hourOfDay: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
  ): String {
    val routine = _routines.value.firstOrNull { it.type == type }
      ?: return generateDailyBriefing(hourOfDay).summaryBangla
    return runRoutineById(routine.id, hourOfDay)
  }

  fun runRoutineById(
    routineId: String,
    hourOfDay: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
  ): String {
    val routine = _routines.value.firstOrNull { it.id == routineId }
      ?: return "সানজু, এই রুটিনটি খুঁজে পাওয়া যায়নি।"
    if (!routine.enabled) {
      return "সানজু, '${routine.title}' বর্তমানে বন্ধ আছে—Automation সেকশন থেকে এটি চালু করতে পারেন।"
    }

    val nowMillis = System.currentTimeMillis()
    _routines.update { list ->
      list.map { if (it.id == routineId) it.copy(lastRunMillis = nowMillis) else it }
    }

    val output =
      when (routine.type) {
        RoutineType.MORNING -> {
          val briefing = generateDailyBriefing(hourOfDay)
          "${greetingManager.nextWakeGreeting()}\n${briefing.summaryBangla}"
        }
        RoutineType.NIGHT -> {
          val pendingTasks = _scheduledTasks.value.count { !it.completed }
          val activeReminders = reminderManager.reminders.value.count { !it.completed }
          permissionManager.setPrivacyLockActive(true)
          "রাতের রুটিন সম্পন্ন, ${personalityManager.userNameBangla}। আজকের বাকি কাজ $pendingTasks টি ও রিমাইন্ডার $activeReminders টি নিরাপদে সংরক্ষিত আছে এবং প্রাইভেসি শিল্ড লক করা হয়েছে। ${farewellManager.buildFarewell(hourOfDay = 22)}"
        }
        RoutineType.FOCUS -> {
          "ফোকাস রুটিন চালু হয়েছে, ${personalityManager.userNameBangla}। অফলাইন নোট, হিসাব ও রিমাইন্ডার প্রস্তুত আছে—মনোযোগ দিয়ে কাজ শুরু করুন।"
        }
        RoutineType.CUSTOM -> {
          val stepSummary = routine.steps.joinToString(" -> ")
          "'${routine.title}' সম্পন্ন হয়েছে, ${personalityManager.userNameBangla}: $stepSummary।"
        }
      }

    errorLogger.logInfo("AutomationManager", "Executed routine '${routine.title}'.")
    return output
  }

  fun matchCustomRoutineByCommand(rawCommand: String): MaxRoutine? {
    val lower = rawCommand.lowercase()
    return _routines.value.firstOrNull {
      it.type == RoutineType.CUSTOM &&
        it.enabled &&
        (lower.contains(it.triggerPhrase.lowercase()) || lower.contains(it.title.lowercase()))
    }
  }
}

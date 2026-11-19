package com.example.brain

import com.example.apps.AppManager
import com.example.apps.AppState
import com.example.automation.AutomationManager
import com.example.automation.RoutineType
import com.example.conversation.ConversationManager
import com.example.emotion.EmotionManager
import com.example.emotion.EmotionState
import com.example.internet.InternetActionOutcome
import com.example.internet.InternetManager
import com.example.internet.SearchVertical
import com.example.memory.MemoryCommandResult
import com.example.memory.MemoryController
import com.example.memory.MemoryManager
import com.example.overlay.FloatingAssistantController
import com.example.phone.PhoneManager
import com.example.settings.SettingsManager
import com.example.smart.SmartFeatureManager
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Operational cognitive state of [AIBrainManager].
 */
sealed class AIBrainState {
  data object Idle : AIBrainState()

  data class Thinking(val input: String) : AIBrainState()

  data class AwaitingClarification(val question: String) : AIBrainState()

  data class AwaitingInternetConsent(
    val query: String,
    val vertical: SearchVertical,
    val promptText: String,
  ) : AIBrainState()

  data class Responded(
    val replyText: String,
    val emotion: EmotionState,
    val usedInternet: Boolean,
    val browserIntent: android.content.Intent? = null,
  ) : AIBrainState()

  data class ErrorFallback(val friendlyMessage: String) : AIBrainState()
}

/**
 * Result returned by [AIBrainManager.thinkAndRespond].
 */
data class BrainResponse(
  val replyText: String,
  val usedInternet: Boolean = false,
  val awaitingConsent: Boolean = false,
  val awaitingClarification: Boolean = false,
)

/**
 * Central cognitive orchestrator for MAX.
 *
 * Thinks before responding by combining:
 * [IntentAnalyzer] + [ContextManager] + [DecisionEngine] + [ResponsePlanner]
 * across [ConversationManager], [EmotionManager], [MemoryManager], [InternetManager],
 * and [TTSManager].
 */
class AIBrainManager(
  val intentAnalyzer: IntentAnalyzer,
  val contextManager: ContextManager,
  val decisionEngine: DecisionEngine,
  val responsePlanner: ResponsePlanner,
  val multiStepPlanner: MultiStepPlanner,
  private val conversationManager: ConversationManager,
  private val emotionManager: EmotionManager,
  private val memoryManager: MemoryManager,
  private val memoryController: MemoryController,
  private val automationManager: AutomationManager,
  private val floatingAssistantController: FloatingAssistantController,
  private val internetManager: InternetManager,
  private val smartFeatureManager: SmartFeatureManager,
  private val phoneManager: PhoneManager,
  private val appManager: AppManager,
  private val ttsManager: TTSManager,
  private val settingsManager: SettingsManager,
  private val errorLogger: ErrorLogger,
) : MaxModule {

  private val _brainState = MutableStateFlow<AIBrainState>(AIBrainState.Idle)
  val brainState: StateFlow<AIBrainState> = _brainState.asStateFlow()

  override val descriptor: ModuleDescriptor
    get() =
      ModuleDescriptor(
        id = "ai_brain_manager",
        displayName = "AIBrainManager",
        packageName = "com.example.brain",
        category = ModuleCategory.COGNITION_AND_MEMORY,
        status = ModuleStatus.READY,
        summary = "Intent reasoning, multi-step planning, context retention, offline-first decision & response planner.",
      )

  /**
   * Processes Sanju's input thoughtfully:
   * 1. Decomposes multi-step requests via [MultiStepPlanner] when no pending clarification/consent exists.
   * 2. Analyzes intent & multi-turn context.
   * 3. Checks if offline completion is possible (Memory/Automation/Smart/Calculator/Apps/Emotion).
   * 4. Asks follow-up questions when details are missing.
   * 5. Asks explicit permission before touching [InternetManager].
   */
  fun thinkAndRespond(
    rawInput: String,
    explicitUserRequest: Boolean = true,
    speakReply: Boolean = settingsManager.settings.value.ttsEnabled,
  ): BrainResponse {
    val cleanInput = rawInput.trim()
    if (cleanInput.isEmpty()) {
      return BrainResponse(replyText = "")
    }

    _brainState.value = AIBrainState.Thinking(cleanInput)

    val snapshotBefore = contextManager.snapshot.value
    if (snapshotBefore.pendingInternetRequest == null && snapshotBefore.pendingClarification == null) {
      val compoundPlan = multiStepPlanner.decomposeIfCompound(cleanInput)
      if (compoundPlan != null) {
        return executeMultiStepPlan(compoundPlan, explicitUserRequest, speakReply)
      }
    }

    return thinkSingleStep(cleanInput, explicitUserRequest, speakReply)
  }

  private fun executeMultiStepPlan(
    plan: TaskPlan,
    explicitUserRequest: Boolean,
    speakReply: Boolean,
  ): BrainResponse {
    val stepOutputs = mutableListOf<String>()
    var anyInternet = false
    var awaitingConsent = false
    var awaitingClarification = false

    for (step in plan.steps) {
      val res = thinkSingleStep(step.commandText, explicitUserRequest, speakReply = false)
      if (res.replyText.isNotBlank()) {
        stepOutputs.add("${step.index}. ${res.replyText}")
      }
      anyInternet = anyInternet || res.usedInternet
      if (res.awaitingConsent) {
        awaitingConsent = true
        break
      }
      if (res.awaitingClarification) {
        awaitingClarification = true
        break
      }
    }

    val combinedReply =
      if (stepOutputs.isEmpty()) {
        "সানজু, কাজগুলো সম্পন্ন হয়েছে।"
      } else {
        "ধাপে ধাপে পরিকল্পনা সম্পন্ন (${plan.steps.size} ধাপ):\n" + stepOutputs.joinToString("\n")
      }

    if (!awaitingConsent && !awaitingClarification) {
      _brainState.value =
        AIBrainState.Responded(
          replyText = combinedReply,
          emotion = EmotionState.SUPPORTIVE,
          usedInternet = anyInternet,
        )
    }

    if (speakReply && combinedReply.isNotBlank()) {
      ttsManager.speak(combinedReply, SpeechStyle.HELPFUL)
    }
    errorLogger.logInfo("AIBrainManager", "Executed multi-step plan (${plan.steps.size} steps).")
    return BrainResponse(
      replyText = combinedReply,
      usedInternet = anyInternet,
      awaitingConsent = awaitingConsent,
      awaitingClarification = awaitingClarification,
    )
  }

  private fun thinkSingleStep(
    cleanInput: String,
    explicitUserRequest: Boolean,
    speakReply: Boolean,
  ): BrainResponse {
    return runCatching {
      val snapshot = contextManager.snapshot.value
      val analysis = intentAnalyzer.analyze(cleanInput, snapshot)
      val detectedMood = emotionManager.evaluateUserMood(cleanInput).emotion

      contextManager.recordTurn(
        speaker = "USER",
        text = cleanInput,
        intent = analysis.intent,
        emotion = detectedMood,
        topic = analysis.extractedTopic,
      )

      when (val decision = decisionEngine.decide(analysis, snapshot)) {
        is BrainDecision.AskFollowUp -> {
          contextManager.setPendingClarification(
            PendingClarification(
              intent = decision.intent,
              questionAsked = decision.question,
              vertical = decision.vertical,
              requiresInternet = decision.requiresInternet,
            ),
          )
          val planned = responsePlanner.planFollowUpQuestion(decision.question)
          deliverReply(cleanInput, planned, analysis.intent, speakReply, usedInternet = false)
          _brainState.value = AIBrainState.AwaitingClarification(planned.text)
          BrainResponse(replyText = planned.text, awaitingClarification = true)
        }

        is BrainDecision.AskInternetPermission -> {
          contextManager.setPendingClarification(null)
          contextManager.setPendingInternetRequest(
            PendingInternetRequest(
              query = decision.query,
              vertical = decision.vertical,
              originalIntent = decision.originalIntent,
            ),
          )
          val planned = responsePlanner.planInternetPermissionPrompt(decision.query, decision.vertical.label)
          deliverReply(cleanInput, planned, analysis.intent, speakReply, usedInternet = false)
          _brainState.value =
            AIBrainState.AwaitingInternetConsent(
              query = decision.query,
              vertical = decision.vertical,
              promptText = planned.text,
            )
          BrainResponse(replyText = planned.text, awaitingConsent = true)
        }

        is BrainDecision.ExecuteOnlineWithConsent -> {
          contextManager.setPendingInternetRequest(null)
          contextManager.setPendingClarification(null)
          val outcome = internetManager.prepareSearch(decision.query, decision.vertical, explicitUserRequest)
          val browserIntent = (outcome as? InternetActionOutcome.ReadyBrowser)?.intent
          val replyText =
            when (outcome) {
              is InternetActionOutcome.ReadyBrowser ->
                "অনুমতি দেওয়ার জন্য ধন্যবাদ সানজু। '${decision.query}' খোঁজার লিংক প্রস্তুত করেছি:\n${outcome.url}"
              is InternetActionOutcome.Summary -> outcome.summaryText
              is InternetActionOutcome.Blocked -> outcome.message
            }
          val planned =
            PlannedReply(
              text = replyText,
              speechStyle = emotionManager.currentStyle.value.speechStyle,
              emotion = EmotionState.RESPECTFUL,
            )
          deliverReply(cleanInput, planned, decision.originalIntent, speakReply, usedInternet = true)
          _brainState.value =
            AIBrainState.Responded(
              replyText = planned.text,
              emotion = planned.emotion,
              usedInternet = true,
              browserIntent = browserIntent,
            )
          BrainResponse(replyText = planned.text, usedInternet = true)
        }

        is BrainDecision.InternetDeclinedOfflineFallback -> {
          contextManager.setPendingInternetRequest(null)
          contextManager.setPendingClarification(null)
          val planned = responsePlanner.planErrorExplanation(decision.message)
          deliverReply(cleanInput, planned, analysis.intent, speakReply, usedInternet = false)
          _brainState.value = AIBrainState.Responded(planned.text, planned.emotion, usedInternet = false)
          BrainResponse(replyText = planned.text, usedInternet = false)
        }

        is BrainDecision.LaunchExternalApp -> {
          contextManager.setPendingClarification(null)
          val appOutcome = appManager.openApp(decision.app, explicitUserRequest)
          val message =
            when (appOutcome) {
              is AppState.LaunchReady ->
                "${conversationManager.personalityManager.userNameBangla}, ${appOutcome.app.displayName} খোলার জন্য প্রস্তুত।"
              is AppState.NotInstalled -> appOutcome.suggestion
              is AppState.ConfirmationRequired -> appOutcome.detail
              AppState.Standby -> "${decision.app.displayName} স্ট্যান্ডবাই অবস্থায় আছে।"
            }
          val planned =
            responsePlanner.planOfflineReply(cleanInput, analysis, emptyList(), contextManager.snapshot.value, message)
          deliverReply(cleanInput, planned, analysis.intent, speakReply, usedInternet = false)
          _brainState.value = AIBrainState.Responded(planned.text, planned.emotion, usedInternet = false)
          BrainResponse(replyText = planned.text, usedInternet = false)
        }

        is BrainDecision.ExecuteOffline -> {
          contextManager.setPendingClarification(null)
          val offlineResultText = executeOfflineAction(decision.analysis, cleanInput)
          val planned =
            responsePlanner.planOfflineReply(
              userInput = cleanInput,
              analysis = decision.analysis,
              recalledMemories = decision.recalledMemories,
              context = contextManager.snapshot.value,
              actionOutput = offlineResultText,
            )
          deliverReply(cleanInput, planned, decision.analysis.intent, speakReply, usedInternet = false)
          _brainState.value = AIBrainState.Responded(planned.text, planned.emotion, usedInternet = false)
          BrainResponse(replyText = planned.text, usedInternet = false)
        }

        is BrainDecision.GracefulError -> {
          val planned = responsePlanner.planErrorExplanation(decision.friendlyMessage)
          deliverReply(cleanInput, planned, analysis.intent, speakReply, usedInternet = false)
          _brainState.value = AIBrainState.ErrorFallback(planned.text)
          BrainResponse(replyText = planned.text, usedInternet = false)
        }
      }
    }.getOrElse { throwable ->
      errorLogger.logError("AIBrainManager", "Brain reasoning encountered an unexpected error.", throwable)
      val fallback = "দুঃখিত সানজু, কথাটি প্রক্রিয়া করতে সামান্য সমস্যা হয়েছে। অনুগ্রহ করে আবার চেষ্টা করুন।"
      _brainState.value = AIBrainState.ErrorFallback(fallback)
      BrainResponse(replyText = fallback)
    }
  }

  fun confirmPendingInternetAccess(allowed: Boolean): BrainResponse {
    val replyCommand = if (allowed) "হ্যাঁ" else "না"
    return thinkAndRespond(replyCommand, explicitUserRequest = true)
  }

  fun resetContext() {
    contextManager.clearSession()
    _brainState.value = AIBrainState.Idle
  }

  private fun executeOfflineAction(analysis: IntentAnalysis, rawInput: String): String? =
    when (analysis.intent) {
      UserIntent.MEMORY_STORE -> {
        val normalizedCmd =
          if (rawInput.contains("মনে রাখো") || rawInput.contains("remember", ignoreCase = true)) {
            rawInput
          } else {
            "মনে রাখো ${analysis.extractedTopic}"
          }
        val result = memoryController.processCommand(normalizedCmd)
        if (result is MemoryCommandResult.Handled) {
          result.replyBangla
        } else {
          val saved = memoryManager.saveMemory(key = "Sanju Context", content = analysis.extractedTopic)
          "ঠিক আছে সানজু, '${saved.content}' তথ্যটি নিরাপদে মনে রেখেছি।"
        }
      }

      UserIntent.MEMORY_RECALL -> {
        val result = memoryController.processCommand(rawInput)
        if (result is MemoryCommandResult.Handled) {
          result.replyBangla
        } else {
          val items = memoryManager.recallMemories(analysis.extractedTopic)
          if (items.isEmpty()) {
            "সানজু, এই মুহূর্তে এই বিষয়ের কোনো তথ্য মেমোরি ভল্টে পাওয়া যায়নি।"
          } else {
            items.joinToString(" • ") { "${it.key}: ${it.content}" }
          }
        }
      }

      UserIntent.MEMORY_DELETE -> {
        val normalizedCmd =
          if (rawInput.contains("ভুলে যাও") || rawInput.contains("forget", ignoreCase = true)) {
            rawInput
          } else {
            "ভুলে যাও ${analysis.extractedTopic}"
          }
        val result = memoryController.processCommand(normalizedCmd)
        if (result is MemoryCommandResult.Handled) result.replyBangla else null
      }

      UserIntent.MEMORY_CLEAR -> {
        memoryManager.clearAll()
        "সানজু, আপনার সব সংরক্ষিত মেমোরি নিরাপদে মুছে ফেলা হয়েছে।"
      }

      UserIntent.CALCULATION -> {
        val calc = phoneManager.calculate(analysis.extractedTopic, explicitUserRequest = true)
        calc?.let { "${it.expression} = ${it.result}" }
      }

      UserIntent.TRANSLATION -> {
        val normalizedCmd =
          if (rawInput.contains("অনুবাদ") || rawInput.contains("translate", ignoreCase = true)) {
            rawInput
          } else {
            "${analysis.extractedTopic} অনুবাদ করো"
          }
        smartFeatureManager.handleSmartCommand(command = normalizedCmd, speakResult = false)
      }

      UserIntent.REMINDER -> {
        val normalizedCmd =
          if (rawInput.contains("রিমাইন্ডার") || rawInput.contains("reminder", ignoreCase = true)) {
            rawInput
          } else {
            "রিমাইন্ডার: ${analysis.extractedTopic}"
          }
        smartFeatureManager.handleSmartCommand(command = normalizedCmd, speakResult = false)
      }

      UserIntent.NOTE -> {
        val normalizedCmd =
          if (rawInput.contains("নোট") || rawInput.contains("note", ignoreCase = true)) {
            rawInput
          } else {
            "নোট: ${analysis.extractedTopic}"
          }
        smartFeatureManager.handleSmartCommand(command = normalizedCmd, speakResult = false)
      }

      UserIntent.CALENDAR -> {
        smartFeatureManager.handleSmartCommand(command = rawInput, speakResult = false)
      }

      UserIntent.OFFLINE_TIME_DATE -> {
        val formatted =
          SimpleDateFormat("EEEE, d MMM yyyy • hh:mm a", Locale.getDefault()).format(Date())
        "সানজু, এখন স্থানীয় সময় ও তারিখ: $formatted (সম্পূর্ণ অফলাইনে পঠিত)।"
      }

      UserIntent.LIST_NOTES_REMINDERS -> {
        val activeReminders = smartFeatureManager.reminderManager.reminders.value.filter { !it.completed }
        val storedNotes = memoryManager.memories.value
        buildString {
          append("সানজু, আপনার অফলাইন তালিকা—")
          append("রিমাইন্ডার (${activeReminders.size}): ")
          if (activeReminders.isEmpty()) {
            append("কোনো বাকি রিমাইন্ডার নেই। ")
          } else {
            append(activeReminders.take(3).joinToString(", ") { "${it.title} (${it.scheduledLabel})" })
            append(". ")
          }
          append("সংরক্ষিত নোট/মেমোরি: ${storedNotes.size} টি।")
        }
      }

      UserIntent.SCHEDULE_TASK -> {
        val task = automationManager.scheduleTask(title = analysis.extractedTopic)
        "সানজু, '${task.title}' কাজটি '${task.scheduledTimeLabel}' সময়ের জন্য শিডিউল করা হয়েছে।"
      }

      UserIntent.MORNING_ROUTINE -> automationManager.runRoutineByType(RoutineType.MORNING)

      UserIntent.NIGHT_ROUTINE -> automationManager.runRoutineByType(RoutineType.NIGHT)

      UserIntent.FOCUS_ROUTINE -> automationManager.runRoutineByType(RoutineType.FOCUS)

      UserIntent.DAILY_BRIEFING -> automationManager.generateDailyBriefing().summaryBangla

      UserIntent.FLOATING_ASSISTANT_TOGGLE -> {
        floatingAssistantController.toggleMinimizeRestore()
        "সানজু, ফ্লোটিং অ্যাসিস্ট্যান্ট বাবল অবস্থা পরিবর্তন করা হয়েছে।"
      }

      else -> {
        val matchedCustom = automationManager.matchCustomRoutineByCommand(rawInput)
        matchedCustom?.let { automationManager.runRoutineById(it.id) }
      }
    }

  private fun deliverReply(
    userInput: String,
    planned: PlannedReply,
    intent: UserIntent,
    speakReply: Boolean,
    usedInternet: Boolean,
  ) {
    contextManager.recordTurn(
      speaker = "MAX",
      text = planned.text,
      intent = intent,
      emotion = planned.emotion,
    )
    conversationManager.handleUserInput(
      input = userInput,
      responseOverride = planned.text,
      speakReply = false,
    )
    if (speakReply) {
      ttsManager.speak(planned.text, planned.speechStyle)
    }
    errorLogger.logInfo(
      "AIBrainManager",
      "Planned reply for intent=$intent (usedInternet=$usedInternet, emotion=${planned.emotion.label}).",
    )
  }
}

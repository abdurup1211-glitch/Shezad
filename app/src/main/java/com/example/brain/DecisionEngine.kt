package com.example.brain

import com.example.apps.SupportedExternalApp
import com.example.internet.SearchVertical
import com.example.memory.MemoryEntry
import com.example.memory.MemoryManager

/**
 * Decision produced by [DecisionEngine] after reasoning over intent, context, memory,
 * offline capability, and privacy/internet consent rules.
 */
sealed class BrainDecision {
  data class ExecuteOffline(
    val analysis: IntentAnalysis,
    val recalledMemories: List<MemoryEntry> = emptyList(),
  ) : BrainDecision()

  data class AskFollowUp(
    val intent: UserIntent,
    val question: String,
    val vertical: SearchVertical = SearchVertical.WEB,
    val requiresInternet: Boolean = false,
  ) : BrainDecision()

  data class AskInternetPermission(
    val query: String,
    val vertical: SearchVertical,
    val originalIntent: UserIntent,
  ) : BrainDecision()

  data class ExecuteOnlineWithConsent(
    val query: String,
    val vertical: SearchVertical,
    val originalIntent: UserIntent,
  ) : BrainDecision()

  data class InternetDeclinedOfflineFallback(
    val message: String,
  ) : BrainDecision()

  data class LaunchExternalApp(
    val app: SupportedExternalApp,
  ) : BrainDecision()

  data class GracefulError(
    val friendlyMessage: String,
  ) : BrainDecision()
}

/**
 * Evaluates whether a user request can be answered offline from Memory/Emotion/Smart tools,
 * needs follow-up clarification, or requires explicit internet consent.
 */
class DecisionEngine(
  private val memoryManager: MemoryManager,
) {

  fun decide(
    analysis: IntentAnalysis,
    context: BrainContextSnapshot,
  ): BrainDecision {
    // 1. Handle user's answer to a pending internet consent question
    if (analysis.intent == UserIntent.INTERNET_CONSENT_YES) {
      val pending = context.pendingInternetRequest
      return if (pending != null) {
        BrainDecision.ExecuteOnlineWithConsent(
          query = pending.query,
          vertical = pending.vertical,
          originalIntent = pending.originalIntent,
        )
      } else {
        BrainDecision.GracefulError("সানজু, ইন্টারনেটে কী খুঁজতে হবে তা স্পষ্ট নয়—আবার একটু বলবেন?")
      }
    }

    if (analysis.intent == UserIntent.INTERNET_CONSENT_NO) {
      return BrainDecision.InternetDeclinedOfflineFallback(
        "ঠিক আছে সানজু, আপনার সিদ্ধান্তকে সম্মান করছি—ইন্টারনেট ব্যবহার করলাম না। অফলাইনে অন্য কোনো সাহায্য লাগলে বলুন।",
      )
    }

    // 2. Ask follow-up if essential details are missing
    val missingPrompt = analysis.missingSlotPrompt
    if (!missingPrompt.isNullOrBlank()) {
      return BrainDecision.AskFollowUp(
        intent = analysis.intent,
        question = missingPrompt,
        vertical = analysis.searchVertical,
        requiresInternet = analysis.requiresInternet,
      )
    }

    // 3. Check local Memory first before ever proposing internet usage!
    val recalled =
      if (analysis.extractedTopic.isNotBlank()) {
        memoryManager.recallMemories(analysis.extractedTopic)
      } else {
        emptyList()
      }

    if (analysis.intent == UserIntent.GENERAL_KNOWLEDGE && recalled.isNotEmpty()) {
      return BrainDecision.ExecuteOffline(analysis = analysis, recalledMemories = recalled)
    }

    // 4. App launch handoff
    if (analysis.intent == UserIntent.APP_LAUNCH && analysis.targetApp != null) {
      return BrainDecision.LaunchExternalApp(analysis.targetApp)
    }

    // 5. If task requires internet, ALWAYS ask permission before using InternetManager
    if (analysis.requiresInternet) {
      val query = analysis.extractedTopic.ifBlank { context.activeTopic.orEmpty() }
      if (query.isBlank()) {
        return BrainDecision.AskFollowUp(
          intent = analysis.intent,
          question = "সানজু, কোন বিষয়টি নিয়ে অনলাইনে দেখতে হবে?",
          vertical = analysis.searchVertical,
          requiresInternet = true,
        )
      }
      return BrainDecision.AskInternetPermission(
        query = query,
        vertical = analysis.searchVertical,
        originalIntent = analysis.intent,
      )
    }

    // 6. Complete strictly offline whenever possible
    return BrainDecision.ExecuteOffline(analysis = analysis, recalledMemories = recalled)
  }
}

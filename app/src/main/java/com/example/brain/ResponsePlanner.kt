package com.example.brain

import com.example.conversation.FarewellManager
import com.example.conversation.GreetingManager
import com.example.conversation.PersonalityManager
import com.example.emotion.EmotionManager
import com.example.emotion.EmotionState
import com.example.emotion.ResponseStyle
import com.example.memory.MemoryEntry
import com.example.tts.SpeechStyle

/**
 * Planned response payload combining natural wording and emotional voice style.
 */
data class PlannedReply(
  val text: String,
  val speechStyle: SpeechStyle,
  val emotion: EmotionState,
)

/**
 * Generates warm, non-repetitive, context-aware Bangla replies using Emotion, Memory,
 * and Personality state.
 */
class ResponsePlanner(
  private val emotionManager: EmotionManager,
  private val personalityManager: PersonalityManager,
  private val greetingManager: GreetingManager,
  private val farewellManager: FarewellManager,
) {

  private val calmCheckInReplies =
    listOf(
      "জি সানজু, আমি আপনার কথা মনোযোগ দিয়ে শুনছি। বলুন কীভাবে সাহায্য করতে পারি?",
      "আমি আছি সানজু। আজ কোন কাজে আপনাকে সহায়তা করতে পারি?",
      "অবশ্যই সানজু, আপনার যেকোনো নির্দেশের জন্য আমি প্রস্তুত।",
    )
  private var checkInCursor = -1

  fun planOfflineReply(
    userInput: String,
    analysis: IntentAnalysis,
    recalledMemories: List<MemoryEntry>,
    context: BrainContextSnapshot,
    actionOutput: String? = null,
  ): PlannedReply {
    val style: ResponseStyle = emotionManager.evaluateUserMood(userInput)

    val text =
      when {
        !actionOutput.isNullOrBlank() -> actionOutput

        analysis.intent == UserIntent.GREETING -> greetingManager.nextWakeGreeting()

        analysis.intent == UserIntent.FAREWELL -> farewellManager.buildFarewell()

        analysis.intent == UserIntent.CAPABILITIES_HELP ->
          "আমি MAX, আপনার ব্যক্তিগত এআই সহকারী, ${personalityManager.userNameBangla}। অফলাইনে আমি মেমোরি সংরক্ষণ, হিসাব-নিকাশ, অনুবাদ, নোট, রিমাইন্ডার, সকাল ও রাতের রুটিন এবং অ্যাপ চালনায় সাহায্য করতে পারি—আর অনলাইন কোনো তথ্য লাগলে আগে আপনার অনুমতি নিয়ে তবেই ইন্টারনেট ব্যবহার করব।"

        analysis.intent == UserIntent.REPEAT_LAST_REPLY -> {
          val lastMaxTurn = context.recentTurns.lastOrNull { it.speaker == "MAX" }?.text
          if (!lastMaxTurn.isNullOrBlank()) {
            "অবশ্যই ${personalityManager.userNameBangla}, আবার বলছি: $lastMaxTurn"
          } else {
            nextVariedCheckIn(context)
          }
        }

        recalledMemories.isNotEmpty() -> {
          val mem = recalledMemories.first()
          "${personalityManager.userNameBangla}, আপনার মেমোরি থেকে পেলাম—'${mem.key}': ${mem.content}"
        }

        analysis.intent == UserIntent.EMOTIONAL_CHECK_IN -> {
          val empatheticBase =
            when (style.emotion) {
              EmotionState.CARING, EmotionState.EMPATHETIC ->
                "একটু বিশ্রাম নিন এবং শান্ত থাকুন, সানজু। কোনো কিছু নিয়ে কথা বলতে চাইলে বা সাহায্য লাগলে আমাকে বলুন।"
              EmotionState.HAPPY, EmotionState.EXCITED ->
                "আপনার ভালো লাগা জেনে আমারও খুব ভালো লাগল, সানজু! আজকের কোনো কাজ থাকলে বলুন।"
              EmotionState.SUPPORTIVE ->
                "একসাথে ধাপে ধাপে কাজটা শেষ করা যাবে, সানজু। কী করতে হবে বলুন।"
              EmotionState.SORRY ->
                "একদম ঠিক আছে সানজু, চিন্তার কিছু নেই। আমি সবসময় সাহায্যের জন্য প্রস্তুত।"
              else -> nextVariedCheckIn(context)
            }
          emotionManager.formatEmpatheticReply(userInput, empatheticBase)
        }

        else -> nextVariedCheckIn(context)
      }

    return PlannedReply(text = text, speechStyle = style.speechStyle, emotion = style.emotion)
  }

  fun planInternetPermissionPrompt(
    query: String,
    verticalLabel: String,
  ): PlannedReply =
    PlannedReply(
      text =
        "${personalityManager.userNameBangla}, '$query' সম্পর্কে জানতে ($verticalLabel) ইন্টারনেট ব্যবহার করতে হবে।\n" +
          "আপনি কি এখন ইন্টারনেট ব্যবহারের অনুমতি দিচ্ছেন?",
      speechStyle = SpeechStyle.RESPECTFUL,
      emotion = EmotionState.RESPECTFUL,
    )

  fun planFollowUpQuestion(question: String): PlannedReply =
    PlannedReply(
      text = question,
      speechStyle = SpeechStyle.CALM,
      emotion = EmotionState.CURIOUS,
    )

  fun planErrorExplanation(friendlyMessage: String): PlannedReply =
    PlannedReply(
      text = friendlyMessage,
      speechStyle = SpeechStyle.CARING,
      emotion = EmotionState.SORRY,
    )

  private fun nextVariedCheckIn(context: BrainContextSnapshot): String {
    checkInCursor = (checkInCursor + 1) % calmCheckInReplies.size
    val base = calmCheckInReplies[checkInCursor]
    val topic = context.activeTopic
    return if (!topic.isNullOrBlank() && context.recentTurns.size >= 2) {
      "$base (আগের প্রসঙ্গ: $topic)"
    } else {
      base
    }
  }
}

package com.example.conversation

/**
 * Lifecycle state of [ConversationManager] following MAX's natural wake-word and task flow.
 */
sealed class ConversationState {
  data object WakeStandby : ConversationState()

  data class GreetingUser(val message: String) : ConversationState()

  data object ListeningForCommand : ConversationState()

  data class ProcessingTask(val command: String) : ConversationState()

  data class AskingFollowUp(val prompt: String) : ConversationState()

  data class Farewell(val message: String) : ConversationState()
}

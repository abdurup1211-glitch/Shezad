package com.example.automation

import java.util.UUID

/**
 * Built-in and user-defined smart automation routine categories.
 */
enum class RoutineType(val label: String) {
  MORNING("Morning Routine"),
  NIGHT("Night Routine"),
  FOCUS("Deep Focus"),
  CUSTOM("Custom Routine"),
}

/**
 * Privacy-first local user routine executed strictly offline or via user-approved modules.
 */
data class MaxRoutine(
  val id: String = UUID.randomUUID().toString(),
  val title: String,
  val triggerPhrase: String,
  val type: RoutineType,
  val steps: List<String>,
  val scheduledTimeLabel: String,
  val enabled: Boolean = true,
  val lastRunMillis: Long? = null,
)

/**
 * Locally scheduled task managed safely without waking sensors or draining battery.
 */
data class ScheduledTask(
  val id: String = UUID.randomUUID().toString(),
  val title: String,
  val scheduledTimeLabel: String,
  val repeatDaily: Boolean = false,
  val completed: Boolean = false,
  val createdAtMillis: Long = System.currentTimeMillis(),
)

/**
 * Offline daily briefing summary generated from local time, reminders, memory, and privacy status.
 */
data class DailyBriefing(
  val greeting: String,
  val dateTimeLabel: String,
  val activeRemindersCount: Int,
  val pendingTasksCount: Int,
  val memoriesCount: Int,
  val privacyShieldActive: Boolean,
  val summaryBangla: String,
)

package com.example.phone

import android.content.Intent
import android.provider.AlarmClock

/**
 * Prepares explicit user-requested alarm intents via Android [AlarmClock].
 */
class AlarmManager {
  fun buildSetAlarmIntent(
    hour: Int,
    minute: Int,
    label: String,
    explicitUserRequest: Boolean,
  ): Intent? {
    if (!explicitUserRequest) return null
    return Intent(AlarmClock.ACTION_SET_ALARM).apply {
      putExtra(AlarmClock.EXTRA_HOUR, hour.coerceIn(0, 23))
      putExtra(AlarmClock.EXTRA_MINUTES, minute.coerceIn(0, 59))
      putExtra(AlarmClock.EXTRA_MESSAGE, label.ifBlank { "MAX Alarm" })
      putExtra(AlarmClock.EXTRA_SKIP_UI, false)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
  }
}

/**
 * Prepares explicit user-requested countdown timer intents via Android [AlarmClock].
 */
class TimerManager {
  fun buildSetTimerIntent(
    seconds: Int,
    label: String,
    explicitUserRequest: Boolean,
  ): Intent? {
    if (!explicitUserRequest || seconds <= 0) return null
    return Intent(AlarmClock.ACTION_SET_TIMER).apply {
      putExtra(AlarmClock.EXTRA_LENGTH, seconds)
      putExtra(AlarmClock.EXTRA_MESSAGE, label.ifBlank { "MAX Timer" })
      putExtra(AlarmClock.EXTRA_SKIP_UI, false)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
  }
}

/**
 * Battery-free in-memory stopwatch using monotonic elapsed timestamps.
 */
class StopwatchManager {
  private var startedAtMillis: Long? = null
  private var accumulatedMillis: Long = 0L

  fun start(): PhoneState.StopwatchStatus {
    if (startedAtMillis == null) {
      startedAtMillis = System.currentTimeMillis()
    }
    return status()
  }

  fun pause(): PhoneState.StopwatchStatus {
    startedAtMillis?.let { start ->
      accumulatedMillis += (System.currentTimeMillis() - start).coerceAtLeast(0L)
      startedAtMillis = null
    }
    return status()
  }

  fun reset(): PhoneState.StopwatchStatus {
    startedAtMillis = null
    accumulatedMillis = 0L
    return status()
  }

  fun status(): PhoneState.StopwatchStatus {
    val activeDelta =
      startedAtMillis?.let { (System.currentTimeMillis() - it).coerceAtLeast(0L) } ?: 0L
    return PhoneState.StopwatchStatus(
      isRunning = startedAtMillis != null,
      elapsedMillis = accumulatedMillis + activeDelta,
    )
  }
}

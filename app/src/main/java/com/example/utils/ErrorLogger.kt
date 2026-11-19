package com.example.utils

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

/**
 * Severity level for structured MAX diagnostics and error tracking.
 */
enum class LogSeverity {
  DEBUG,
  INFO,
  WARNING,
  ERROR,
  CRITICAL,
}

/**
 * Immutable diagnostic or error entry captured by [ErrorLogger].
 */
data class ErrorLogEntry(
  val id: String = UUID.randomUUID().toString(),
  val timestampMillis: Long = System.currentTimeMillis(),
  val severity: LogSeverity,
  val tag: String,
  val message: String,
  val throwableSummary: String? = null,
)

/**
 * Central diagnostic and error tracking interface for MAX subsystems.
 */
interface ErrorLogger {
  val logs: StateFlow<List<ErrorLogEntry>>

  fun log(
    severity: LogSeverity,
    tag: String,
    message: String,
    throwable: Throwable? = null,
  )

  fun logInfo(tag: String, message: String) = log(LogSeverity.INFO, tag, message)

  fun logWarning(tag: String, message: String, throwable: Throwable? = null) =
    log(LogSeverity.WARNING, tag, message, throwable)

  fun logError(tag: String, message: String, throwable: Throwable? = null) =
    log(LogSeverity.ERROR, tag, message, throwable)

  fun trimToRecent(keepCount: Int = 25)

  fun clearLogs()
}

/**
 * Thread-safe bounded ring-buffer implementation of [ErrorLogger].
 * Disables verbose platform logcat emission in release builds for security & performance.
 */
class DefaultErrorLogger(
  private val maxEntries: Int = if (BuildConfig.DEBUG) 200 else 80,
) : ErrorLogger {

  private val _logs = MutableStateFlow<List<ErrorLogEntry>>(emptyList())
  override val logs: StateFlow<List<ErrorLogEntry>> = _logs.asStateFlow()

  override fun log(
    severity: LogSeverity,
    tag: String,
    message: String,
    throwable: Throwable?,
  ) {
    if (!BuildConfig.DEBUG && severity == LogSeverity.DEBUG) {
      return
    }

    val entry =
      ErrorLogEntry(
        severity = severity,
        tag = tag,
        message = message,
        throwableSummary = throwable?.let { "${it::class.simpleName}: ${it.message.orEmpty()}" },
      )

    _logs.update { current ->
      (listOf(entry) + current).take(maxEntries)
    }

    if (BuildConfig.DEBUG) {
      emitToPlatformLog(severity, tag, message, throwable)
    }
  }

  override fun trimToRecent(keepCount: Int) {
    val bounded = keepCount.coerceAtLeast(0)
    _logs.update { current -> current.take(bounded) }
  }

  override fun clearLogs() {
    _logs.value = emptyList()
  }

  private fun emitToPlatformLog(
    severity: LogSeverity,
    tag: String,
    message: String,
    throwable: Throwable?,
  ) {
    runCatching {
      val formattedTag = "MAX::$tag"
      when (severity) {
        LogSeverity.DEBUG -> Log.d(formattedTag, message, throwable)
        LogSeverity.INFO -> Log.i(formattedTag, message, throwable)
        LogSeverity.WARNING -> Log.w(formattedTag, message, throwable)
        LogSeverity.ERROR, LogSeverity.CRITICAL -> Log.e(formattedTag, message, throwable)
      }
    }
  }
}

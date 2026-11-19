package com.example

import android.app.Application
import com.example.core.MaxContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Application entry point initializing MAX's modular dependency container,
 * fast non-blocking startup, crash safety net, and memory trim handling.
 */
class MaxApplication : Application() {

  private val startupScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  lateinit var container: MaxContainer
    private set

  override fun onCreate() {
    super.onCreate()
    container = MaxContainer(this)
    installCrashSafetyNet()
    container.maxCore.initialize(deferTtsWarmup = true)
    startupScope.launch {
      container.maxCore.warmUpTts()
    }
  }

  override fun onTrimMemory(level: Int) {
    super.onTrimMemory(level)
    if (::container.isInitialized) {
      container.maxCore.onTrimMemory(level)
    }
  }

  override fun onTerminate() {
    startupScope.cancel()
    if (::container.isInitialized) {
      container.maxCore.shutdown()
    }
    super.onTerminate()
  }

  private fun installCrashSafetyNet() {
    val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
      runCatching {
        if (::container.isInitialized) {
          container.errorLogger.log(
            severity = com.example.utils.LogSeverity.CRITICAL,
            tag = "MaxCrashGuard",
            message = "Uncaught exception on thread '${thread.name}'",
            throwable = throwable,
          )
          container.maxCore.releaseTransientResources()
        }
      }
      previousHandler?.uncaughtException(thread, throwable)
    }
  }
}

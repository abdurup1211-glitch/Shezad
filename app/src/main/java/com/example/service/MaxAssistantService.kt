package com.example.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.example.utils.MaxModule
import com.example.utils.ModuleCategory
import com.example.utils.ModuleDescriptor
import com.example.utils.ModuleStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Lifecycle state of [MaxAssistantService].
 */
enum class AssistantServiceState {
  STOPPED,
  BOUND_IDLE,
  STANDBY_READY,
}

/**
 * Architectural contract for managing the MAX background/bound assistant service.
 */
interface AssistantServiceController : MaxModule {
  val serviceState: StateFlow<AssistantServiceState>

  fun markBoundIdle()

  fun markStopped()
}

/**
 * Lightweight coordinator for [MaxAssistantService] state.
 */
class DefaultAssistantServiceController : AssistantServiceController {
  private val _serviceState = MutableStateFlow(AssistantServiceState.STOPPED)
  override val serviceState: StateFlow<AssistantServiceState> = _serviceState.asStateFlow()

  override val descriptor: ModuleDescriptor
    get() =
      ModuleDescriptor(
        id = "assistant_service",
        displayName = "MaxAssistantService",
        packageName = "com.example.service",
        category = ModuleCategory.CORE_ORCHESTRATION,
        status =
          when (_serviceState.value) {
            AssistantServiceState.STOPPED -> ModuleStatus.STANDBY
            AssistantServiceState.BOUND_IDLE, AssistantServiceState.STANDBY_READY -> ModuleStatus.READY
          },
        summary = "Bound lifecycle service skeleton (no background sensor capture).",
      )

  override fun markBoundIdle() {
    _serviceState.value = AssistantServiceState.BOUND_IDLE
  }

  override fun markStopped() {
    _serviceState.value = AssistantServiceState.STOPPED
  }
}

/**
 * Bound Android [Service] foundation for MAX.
 *
 * Intentionally avoids foreground microphone, camera, or location service types
 * to maintain strict foundation privacy guarantees.
 */
class MaxAssistantService : Service() {

  inner class LocalBinder : Binder() {
    fun getService(): MaxAssistantService = this@MaxAssistantService
  }

  private val binder = LocalBinder()

  override fun onBind(intent: Intent?): IBinder = binder
}

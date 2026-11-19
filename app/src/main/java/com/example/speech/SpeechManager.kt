package com.example.speech

import com.example.permissions.AssistantPermission
import com.example.permissions.PermissionManager
import com.example.permissions.PermissionState
import com.example.utils.ErrorLogger
import com.example.utils.MaxModule
import com.example.utils.ModuleCategory
import com.example.utils.ModuleDescriptor
import com.example.utils.ModuleStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Configuration model for MAX speech recognition pipeline.
 */
data class SpeechConfig(
  val locale: Locale = Locale.getDefault(),
  val partialResultsEnabled: Boolean = true,
  val offlinePreferred: Boolean = true,
)

/**
 * Lifecycle state for [SpeechManager].
 */
sealed class SpeechState {
  data object Idle : SpeechState()

  data object Prepared : SpeechState()

  data object PrivacyLocked : SpeechState()

  data class Error(val message: String) : SpeechState()
}

/**
 * Prepares the speech recognition subsystem for future voice input.
 *
 * Foundation privacy rule: never records audio or calls OS microphone APIs.
 */
interface SpeechManager : MaxModule {
  val state: StateFlow<SpeechState>
  val config: SpeechConfig

  fun prepare(): SpeechState

  fun reset()
}

/**
 * Independent foundation implementation of [SpeechManager].
 */
class DefaultSpeechManager(
  private val permissionManager: PermissionManager,
  private val errorLogger: ErrorLogger,
) : SpeechManager {

  override val config: SpeechConfig = SpeechConfig()

  private val _state = MutableStateFlow<SpeechState>(SpeechState.PrivacyLocked)
  override val state: StateFlow<SpeechState> = _state.asStateFlow()

  override val descriptor: ModuleDescriptor
    get() {
      val status =
        when (_state.value) {
          is SpeechState.Prepared -> ModuleStatus.STANDBY
          is SpeechState.PrivacyLocked -> ModuleStatus.PRIVACY_LOCKED
          is SpeechState.Error -> ModuleStatus.ERROR
          is SpeechState.Idle -> ModuleStatus.STANDBY
        }
      return ModuleDescriptor(
        id = "speech_manager",
        displayName = "SpeechManager",
        packageName = "com.example.speech",
        category = ModuleCategory.VOICE_AND_AUDIO,
        status = status,
        summary = "Speech-to-text pipeline contract (audio capture disabled).",
        privacyGuarded = true,
      )
    }

  override fun prepare(): SpeechState {
    if (permissionManager.isPrivacyLockActive()) {
      _state.value = SpeechState.PrivacyLocked
      return _state.value
    }

    val micStatus = permissionManager.checkPermissionStatus(AssistantPermission.MICROPHONE)
    _state.value =
      if (micStatus == PermissionState.PRIVACY_LOCKED) {
        SpeechState.PrivacyLocked
      } else {
        SpeechState.Prepared
      }
    errorLogger.logInfo("SpeechManager", "Speech pipeline state -> ${_state.value} (no mic access).")
    return _state.value
  }

  override fun reset() {
    _state.value =
      if (permissionManager.isPrivacyLockActive()) {
        SpeechState.PrivacyLocked
      } else {
        SpeechState.Idle
      }
  }
}

package com.example.wakeword

import com.example.permissions.AssistantPermission
import com.example.permissions.PermissionManager
import com.example.permissions.PermissionState
import com.example.settings.SettingsManager
import com.example.utils.ErrorLogger
import com.example.utils.MaxModule
import com.example.utils.ModuleCategory
import com.example.utils.ModuleDescriptor
import com.example.utils.ModuleStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Prepares and manages the wake word subsystem for the "MAX" trigger phrase.
 *
 * Privacy guarantee:
 * - Never records or stores normal conversations in standby mode.
 * - Full speech recognition stays inactive until wake word detection fires.
 * - Does not activate camera, gallery, files, location, or internet.
 */
interface WakeWordManager : MaxModule {
  val state: StateFlow<WakeWordState>
  val config: WakeWordConfig

  fun prepareEngine(): WakeWordState

  fun enterStandbyMode(): WakeWordState

  fun addListener(listener: WakeWordListener)

  fun removeListener(listener: WakeWordListener)

  /**
   * Processes a lightweight wake-word phrase check or explicit trigger for "MAX"
   * and notifies registered [WakeWordListener]s (such as MAXCore) without saving audio.
   */
  fun onKeywordMatched(phrase: String = config.wakePhrase, confidence: Float = 0.95f): Boolean

  fun disarm()
}

/**
 * Independent, battery-friendly implementation of [WakeWordManager].
 */
class DefaultWakeWordManager(
  private val settingsManager: SettingsManager,
  private val permissionManager: PermissionManager,
  private val errorLogger: ErrorLogger,
) : WakeWordManager {

  override val config: WakeWordConfig = WakeWordConfig()

  private val listeners = CopyOnWriteArraySet<WakeWordListener>()

  private val _state = MutableStateFlow<WakeWordState>(WakeWordState.PrivacyLocked)
  override val state: StateFlow<WakeWordState> = _state.asStateFlow()

  override val descriptor: ModuleDescriptor
    get() {
      val status =
        when (_state.value) {
          is WakeWordState.StandbyArmed, is WakeWordState.Detected -> ModuleStatus.READY
          is WakeWordState.PrivacyLocked -> ModuleStatus.PRIVACY_LOCKED
          is WakeWordState.Error -> ModuleStatus.ERROR
          is WakeWordState.Idle, is WakeWordState.UnavailableFallback -> ModuleStatus.STANDBY
        }
      return ModuleDescriptor(
        id = "wake_word_manager",
        displayName = "WakeWordManager",
        packageName = "com.example.wakeword",
        category = ModuleCategory.VOICE_AND_AUDIO,
        status = status,
        summary = "Privacy-first '${config.wakePhrase}' wake standby (no audio recording).",
        privacyGuarded = true,
      )
    }

  override fun prepareEngine(): WakeWordState = enterStandbyMode()

  override fun enterStandbyMode(): WakeWordState {
    val settings = settingsManager.settings.value
    val micPermission = permissionManager.checkPermissionStatus(AssistantPermission.MICROPHONE)

    val nextState =
      when {
        settings.privacySafeMode || permissionManager.isPrivacyLockActive() ->
          WakeWordState.PrivacyLocked
        !settings.wakeWordStandbyEnabled -> WakeWordState.Idle
        micPermission == PermissionState.GRANTED -> WakeWordState.StandbyArmed(config.wakePhrase)
        config.fallbackToManualTrigger -> WakeWordState.StandbyArmed(config.wakePhrase)
        else -> WakeWordState.UnavailableFallback
      }

    updateState(nextState)
    errorLogger.logInfo("WakeWordManager", "Standby evaluation -> $nextState (no audio stored).")
    return nextState
  }

  override fun addListener(listener: WakeWordListener) {
    listeners.add(listener)
  }

  override fun removeListener(listener: WakeWordListener) {
    listeners.remove(listener)
  }

  override fun onKeywordMatched(phrase: String, confidence: Float): Boolean {
    if (!matchesWakePhrase(phrase)) {
      return false
    }
    if (confidence < config.sensitivity) {
      return false
    }

    val detected = WakeWordState.Detected(keyword = config.wakePhrase, confidence = confidence)
    updateState(detected)
    errorLogger.logInfo("WakeWordManager", "Wake word '${config.wakePhrase}' detected (confidence=$confidence).")
    listeners.forEach { listener ->
      runCatching { listener.onWakeWordDetected(config.wakePhrase, confidence) }
    }
    return true
  }

  private fun matchesWakePhrase(rawPhrase: String): Boolean {
    val cleaned =
      rawPhrase
        .trim()
        .replace(Regex("[!?,।.:;\"'()]+"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
        .lowercase()
    if (cleaned.isEmpty()) return false
    if (cleaned == config.wakePhrase.lowercase() || cleaned in config.aliases) {
      return true
    }
    val tokens = cleaned.split(' ')
    return tokens.any { it == "max" || it == "ম্যাক্স" }
  }

  override fun disarm() {
    val next =
      if (permissionManager.isPrivacyLockActive()) {
        WakeWordState.PrivacyLocked
      } else {
        WakeWordState.Idle
      }
    updateState(next)
    errorLogger.logInfo("WakeWordManager", "Wake word engine disarmed into $next.")
  }

  private fun updateState(next: WakeWordState) {
    _state.value = next
    listeners.forEach { listener ->
      runCatching { listener.onWakeWordStateChanged(next) }
    }
  }
}

package com.example.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.utils.ErrorLogger
import com.example.utils.MaxModule
import com.example.utils.ModuleCategory
import com.example.utils.ModuleDescriptor
import com.example.utils.ModuleStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.IOException

private val Context.maxSettingsDataStore: DataStore<Preferences> by
  preferencesDataStore(name = "max_foundation_settings")

/**
 * UI appearance mode stored in [SettingsManager].
 */
enum class ThemeMode {
  SYSTEM,
  DARK,
  LIGHT,
}

/**
 * Persistent configuration model for MAX.
 * Sensitive capture flags default to `false` and `privacySafeMode` defaults to `true`.
 */
data class MaxSettings(
  val assistantName: String = "MAX",
  val wakeWordStandbyEnabled: Boolean = false,
  val speechSystemPrepared: Boolean = false,
  val ttsEnabled: Boolean = true,
  val ttsSpeechRate: Float = 1.0f,
  val ttsPitch: Float = 1.0f,
  val memoryEnabled: Boolean = true,
  val privacySafeMode: Boolean = true,
  val diagnosticsLoggingEnabled: Boolean = true,
  val themeMode: ThemeMode = ThemeMode.SYSTEM,
)

/**
 * Contract for reading and persisting MAX application settings.
 */
interface SettingsManager : MaxModule {
  val settings: StateFlow<MaxSettings>

  suspend fun setWakeWordStandbyEnabled(enabled: Boolean)

  suspend fun setSpeechSystemPrepared(prepared: Boolean)

  suspend fun setTtsEnabled(enabled: Boolean)

  suspend fun setTtsSpeechRate(rate: Float)

  suspend fun setMemoryEnabled(enabled: Boolean)

  suspend fun setPrivacySafeMode(enabled: Boolean)

  suspend fun setDiagnosticsLoggingEnabled(enabled: Boolean)

  suspend fun setThemeMode(mode: ThemeMode)

  suspend fun resetToDefaults()
}

/**
 * Jetpack DataStore-backed implementation of [SettingsManager].
 */
class DataStoreSettingsManager(
  context: Context,
  private val errorLogger: ErrorLogger,
  scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : SettingsManager {

  private val dataStore = context.applicationContext.maxSettingsDataStore

  private val _settings = MutableStateFlow(MaxSettings())
  override val settings: StateFlow<MaxSettings> = _settings.asStateFlow()

  override val descriptor: ModuleDescriptor
    get() =
      ModuleDescriptor(
        id = "settings_manager",
        displayName = "SettingsManager",
        packageName = "com.example.settings",
        category = ModuleCategory.INFRASTRUCTURE,
        status = ModuleStatus.READY,
        summary = "DataStore-backed configuration & privacy preference store.",
      )

  init {
    scope.launch {
      dataStore.data
        .catch { exception ->
          if (exception is IOException) {
            errorLogger.logError("SettingsManager", "Failed reading DataStore preferences.", exception)
            emit(emptyPreferences())
          } else {
            throw exception
          }
        }
        .map { prefs -> prefs.toMaxSettings() }
        .collect { loaded ->
          _settings.value = loaded
        }
    }
  }

  override suspend fun setWakeWordStandbyEnabled(enabled: Boolean) {
    updatePreference { it[Keys.WAKE_WORD_STANDBY] = enabled }
  }

  override suspend fun setSpeechSystemPrepared(prepared: Boolean) {
    updatePreference { it[Keys.SPEECH_PREPARED] = prepared }
  }

  override suspend fun setTtsEnabled(enabled: Boolean) {
    updatePreference { it[Keys.TTS_ENABLED] = enabled }
  }

  override suspend fun setTtsSpeechRate(rate: Float) {
    val clamped = rate.coerceIn(0.6f, 1.6f)
    updatePreference { it[Keys.TTS_SPEECH_RATE] = clamped }
  }

  override suspend fun setMemoryEnabled(enabled: Boolean) {
    updatePreference { it[Keys.MEMORY_ENABLED] = enabled }
  }

  override suspend fun setPrivacySafeMode(enabled: Boolean) {
    updatePreference { prefs ->
      prefs[Keys.PRIVACY_SAFE_MODE] = enabled
      if (enabled) {
        prefs[Keys.WAKE_WORD_STANDBY] = false
        prefs[Keys.SPEECH_PREPARED] = false
      }
    }
  }

  override suspend fun setDiagnosticsLoggingEnabled(enabled: Boolean) {
    updatePreference { it[Keys.DIAGNOSTICS_ENABLED] = enabled }
  }

  override suspend fun setThemeMode(mode: ThemeMode) {
    updatePreference { it[Keys.THEME_MODE] = mode.name }
  }

  override suspend fun resetToDefaults() {
    runCatching {
      dataStore.edit { it.clear() }
      errorLogger.logInfo("SettingsManager", "Settings reset to privacy-first defaults.")
    }.onFailure { error ->
      errorLogger.logError("SettingsManager", "Failed to reset settings.", error)
    }
  }

  private suspend fun updatePreference(
    block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit,
  ) {
    runCatching {
      dataStore.edit(block)
    }.onFailure { error ->
      errorLogger.logError("SettingsManager", "Failed to persist preference update.", error)
    }
  }

  private fun Preferences.toMaxSettings(): MaxSettings {
    val themeRaw = this[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name
    val parsedTheme = runCatching { ThemeMode.valueOf(themeRaw) }.getOrDefault(ThemeMode.SYSTEM)
    return MaxSettings(
      assistantName = "MAX",
      wakeWordStandbyEnabled = this[Keys.WAKE_WORD_STANDBY] ?: false,
      speechSystemPrepared = this[Keys.SPEECH_PREPARED] ?: false,
      ttsEnabled = this[Keys.TTS_ENABLED] ?: true,
      ttsSpeechRate = this[Keys.TTS_SPEECH_RATE] ?: 1.0f,
      ttsPitch = this[Keys.TTS_PITCH] ?: 1.0f,
      memoryEnabled = this[Keys.MEMORY_ENABLED] ?: true,
      privacySafeMode = this[Keys.PRIVACY_SAFE_MODE] ?: true,
      diagnosticsLoggingEnabled = this[Keys.DIAGNOSTICS_ENABLED] ?: true,
      themeMode = parsedTheme,
    )
  }

  private object Keys {
    val WAKE_WORD_STANDBY = booleanPreferencesKey("wake_word_standby")
    val SPEECH_PREPARED = booleanPreferencesKey("speech_prepared")
    val TTS_ENABLED = booleanPreferencesKey("tts_enabled")
    val TTS_SPEECH_RATE = floatPreferencesKey("tts_speech_rate")
    val TTS_PITCH = floatPreferencesKey("tts_pitch")
    val MEMORY_ENABLED = booleanPreferencesKey("memory_enabled")
    val PRIVACY_SAFE_MODE = booleanPreferencesKey("privacy_safe_mode")
    val DIAGNOSTICS_ENABLED = booleanPreferencesKey("diagnostics_enabled")
    val THEME_MODE = stringPreferencesKey("theme_mode")
  }
}

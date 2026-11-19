package com.example.tts

import android.content.Context
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import com.example.settings.SettingsManager
import com.example.utils.ErrorLogger
import com.example.utils.MaxModule
import com.example.utils.ModuleCategory
import com.example.utils.ModuleDescriptor
import com.example.utils.ModuleStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

/**
 * Legacy-compatible operational state preserved for Step 1 callers.
 */
sealed class TtsState {
  data object Uninitialized : TtsState()

  data object Initializing : TtsState()

  data object Ready : TtsState()

  data object Speaking : TtsState()

  data object Disabled : TtsState()

  data class Error(val reason: String) : TtsState()
}

/**
 * Controls MAX's warm, natural Bangla & multi-language Text-to-Speech system.
 *
 * Privacy guarantee: never accesses microphone, camera, gallery, or files.
 */
interface TTSManager : MaxModule {
  val state: StateFlow<TtsState>
  val voiceState: StateFlow<VoiceState>
  val voiceConfig: StateFlow<VoiceConfig>

  fun initialize()

  fun speak(text: String, style: SpeechStyle = voiceConfig.value.style)

  fun speak(text: String) = speak(text, voiceConfig.value.style)

  fun stopSpeaking()

  fun pauseSpeaking()

  fun resumeSpeaking()

  fun setSpeechSpeed(speed: Float)

  fun setPitch(pitch: Float)

  fun setLanguage(locale: Locale)

  fun stop() = stopSpeaking()

  fun shutdown()
}

/**
 * Android [TextToSpeech]-backed implementation selecting the highest-quality natural
 * Bangla voice available on device with graceful fallback and sentence-level pacing.
 */
class AndroidTTSManager(
  private val context: Context,
  private val settingsManager: SettingsManager,
  private val errorLogger: ErrorLogger,
) : TTSManager {

  private var textToSpeech: TextToSpeech? = null
  private val pendingQueue = ArrayDeque<String>()
  private var currentSentence: String = ""
  private var isPaused: Boolean = false

  private val _state = MutableStateFlow<TtsState>(TtsState.Uninitialized)
  override val state: StateFlow<TtsState> = _state.asStateFlow()

  private val _voiceState = MutableStateFlow<VoiceState>(VoiceState.Uninitialized)
  override val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

  private val _voiceConfig = MutableStateFlow(VoiceConfig())
  override val voiceConfig: StateFlow<VoiceConfig> = _voiceConfig.asStateFlow()

  override val descriptor: ModuleDescriptor
    get() {
      val status =
        when (_state.value) {
          is TtsState.Ready, is TtsState.Speaking -> ModuleStatus.READY
          is TtsState.Uninitialized, is TtsState.Initializing -> ModuleStatus.STANDBY
          is TtsState.Disabled -> ModuleStatus.DISABLED
          is TtsState.Error -> ModuleStatus.ERROR
        }
      return ModuleDescriptor(
        id = "tts_manager",
        displayName = "TTSManager",
        packageName = "com.example.tts",
        category = ModuleCategory.VOICE_AND_AUDIO,
        status = status,
        summary = "Natural Bangla & multi-language TTS (${_voiceConfig.value.primaryLocale.toLanguageTag()}).",
      )
    }

  override fun initialize() {
    if (!settingsManager.settings.value.ttsEnabled) {
      _state.value = TtsState.Disabled
      _voiceState.value = VoiceState.Disabled
      return
    }
    if (_state.value is TtsState.Ready || _state.value is TtsState.Initializing) {
      return
    }

    _state.value = TtsState.Initializing
    _voiceState.value = VoiceState.Initializing

    runCatching {
      textToSpeech =
        TextToSpeech(context.applicationContext) { status ->
          if (status == TextToSpeech.SUCCESS) {
            val engine = textToSpeech
            val (selectedLocale, usingFallback) = selectBestBanglaOrFallbackLocale(engine)
            selectBestNaturalVoice(engine, selectedLocale)
            applyVoiceParameters(_voiceConfig.value.style)
            attachProgressListener()

            if (settingsManager.settings.value.ttsEnabled) {
              _state.value = TtsState.Ready
              _voiceState.value =
                VoiceState.Ready(
                  activeLocaleTag = selectedLocale.toLanguageTag(),
                  usingFallbackVoice = usingFallback,
                )
              errorLogger.logInfo(
                "TTSManager",
                "Natural TTS ready (locale=${selectedLocale.toLanguageTag()}, fallback=$usingFallback).",
              )
            } else {
              _state.value = TtsState.Disabled
              _voiceState.value = VoiceState.Disabled
            }
          } else {
            val message = "TTS engine unavailable (status=$status)."
            _state.value = TtsState.Error(message)
            _voiceState.value = VoiceState.Error(message)
            errorLogger.logWarning("TTSManager", message)
          }
        }
    }.onFailure { throwable ->
      val message = throwable.message ?: "Unable to initialize TTS engine."
      _state.value = TtsState.Error(message)
      _voiceState.value = VoiceState.Error(message)
      errorLogger.logError("TTSManager", "Exception initializing TextToSpeech.", throwable)
    }
  }

  override fun speak(text: String, style: SpeechStyle) {
    if (!settingsManager.settings.value.ttsEnabled) {
      _state.value = TtsState.Disabled
      _voiceState.value = VoiceState.Disabled
      return
    }
    if (text.isBlank()) return

    _voiceConfig.value = _voiceConfig.value.copy(style = style)
    val sentences = splitIntoNaturalSentences(text)
    if (sentences.isEmpty()) return

    pendingQueue.clear()
    pendingQueue.addAll(sentences)
    isPaused = false

    val engine = textToSpeech
    if (engine == null || (_state.value !is TtsState.Ready && _state.value !is TtsState.Speaking)) {
      errorLogger.logInfo("TTSManager", "Queued speech while TTS initializes cleanly.")
      initialize()
      return
    }

    applyVoiceParameters(style)
    speakNextSentence(engine, style)
  }

  override fun stopSpeaking() {
    pendingQueue.clear()
    currentSentence = ""
    isPaused = false
    runCatching { textToSpeech?.stop() }
    if (_state.value is TtsState.Speaking) {
      _state.value = TtsState.Ready
      _voiceState.value =
        VoiceState.Ready(
          activeLocaleTag = _voiceConfig.value.primaryLocale.toLanguageTag(),
          usingFallbackVoice = false,
        )
    }
  }

  override fun pauseSpeaking() {
    if (_state.value !is TtsState.Speaking && currentSentence.isBlank()) return
    isPaused = true
    runCatching { textToSpeech?.stop() }
    val remaining =
      buildList {
        if (currentSentence.isNotBlank()) add(currentSentence)
        addAll(pendingQueue)
      }
    _voiceState.value = VoiceState.Paused(remainingSentences = remaining)
    _state.value = TtsState.Ready
  }

  override fun resumeSpeaking() {
    val paused = _voiceState.value as? VoiceState.Paused ?: return
    val engine = textToSpeech ?: return
    isPaused = false
    pendingQueue.clear()
    pendingQueue.addAll(paused.remainingSentences)
    speakNextSentence(engine, _voiceConfig.value.style)
  }

  override fun setSpeechSpeed(speed: Float) {
    val clamped = speed.coerceIn(0.65f, 1.35f)
    _voiceConfig.value = _voiceConfig.value.copy(speechSpeed = clamped)
    applyVoiceParameters(_voiceConfig.value.style)
  }

  override fun setPitch(pitch: Float) {
    val clamped = pitch.coerceIn(0.75f, 1.25f)
    _voiceConfig.value = _voiceConfig.value.copy(pitch = clamped)
    applyVoiceParameters(_voiceConfig.value.style)
  }

  override fun setLanguage(locale: Locale) {
    _voiceConfig.value = _voiceConfig.value.copy(primaryLocale = locale)
    val engine = textToSpeech ?: return
    val result = engine.setLanguage(locale)
    val fallback =
      result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED
    if (!fallback) {
      selectBestNaturalVoice(engine, locale)
    }
    _voiceState.value = VoiceState.Ready(locale.toLanguageTag(), usingFallbackVoice = fallback)
  }

  override fun shutdown() {
    pendingQueue.clear()
    currentSentence = ""
    isPaused = false
    runCatching {
      textToSpeech?.stop()
      textToSpeech?.shutdown()
      textToSpeech = null
      _state.value = TtsState.Uninitialized
      _voiceState.value = VoiceState.Uninitialized
      errorLogger.logInfo("TTSManager", "TextToSpeech engine released.")
    }
  }

  private fun speakNextSentence(engine: TextToSpeech, style: SpeechStyle) {
    if (isPaused) return
    val next = pendingQueue.removeFirstOrNull()
    if (next == null) {
      currentSentence = ""
      _state.value = TtsState.Ready
      _voiceState.value = VoiceState.Ready(_voiceConfig.value.primaryLocale.toLanguageTag(), false)
      return
    }

    currentSentence = next
    _state.value = TtsState.Speaking
    _voiceState.value = VoiceState.Speaking(currentSentence = next, style = style)

    runCatching {
      engine.speak(next, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
      if (pendingQueue.isNotEmpty()) {
        engine.playSilentUtterance(
          style.sentencePauseMillis,
          TextToSpeech.QUEUE_ADD,
          "pause_${UUID.randomUUID()}",
        )
      }
    }.onFailure { throwable ->
      errorLogger.logError("TTSManager", "TTS speak call failed.", throwable)
    }
  }

  private fun selectBestBanglaOrFallbackLocale(engine: TextToSpeech?): Pair<Locale, Boolean> {
    if (engine == null) return _voiceConfig.value.fallbackLocale to true
    val cfg = _voiceConfig.value
    val candidates = listOf(cfg.primaryLocale, cfg.secondaryBanglaLocale, cfg.fallbackLocale)
    for ((index, locale) in candidates.withIndex()) {
      val support = runCatching { engine.setLanguage(locale) }.getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)
      if (support != TextToSpeech.LANG_MISSING_DATA && support != TextToSpeech.LANG_NOT_SUPPORTED) {
        return locale to (index == candidates.lastIndex)
      }
    }
    return cfg.fallbackLocale to true
  }

  private fun selectBestNaturalVoice(engine: TextToSpeech?, locale: Locale) {
    if (engine == null) return
    runCatching {
      val voices = engine.voices.orEmpty()
      val bestVoice: Voice? =
        voices
          .filter { it.locale.language == locale.language }
          .sortedWith(
            compareByDescending<Voice> { it.quality }
              .thenBy { it.latency }
              .thenBy { it.isNetworkConnectionRequired },
          )
          .firstOrNull()
      if (bestVoice != null) {
        engine.voice = bestVoice
      }
    }
  }

  private fun applyVoiceParameters(style: SpeechStyle) {
    val cfg = _voiceConfig.value
    val userRate = settingsManager.settings.value.ttsSpeechRate
    val effectiveSpeed = (cfg.speechSpeed * style.speedMultiplier * userRate).coerceIn(0.60f, 1.45f)
    val effectivePitch = (cfg.pitch * style.pitchMultiplier).coerceIn(0.70f, 1.30f)
    runCatching {
      textToSpeech?.setSpeechRate(effectiveSpeed)
      textToSpeech?.setPitch(effectivePitch)
    }
  }

  private fun attachProgressListener() {
    textToSpeech?.setOnUtteranceProgressListener(
      object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
          if (utteranceId?.startsWith("pause_") == false) {
            _state.value = TtsState.Speaking
          }
        }

        override fun onDone(utteranceId: String?) {
          if (isPaused) return
          if (utteranceId?.startsWith("pause_") == true) {
            textToSpeech?.let { speakNextSentence(it, _voiceConfig.value.style) }
          } else if (pendingQueue.isEmpty()) {
            currentSentence = ""
            _state.value = TtsState.Ready
            _voiceState.value =
              VoiceState.Ready(_voiceConfig.value.primaryLocale.toLanguageTag(), false)
          }
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
          _state.value = TtsState.Error("TTS playback error.")
          _voiceState.value = VoiceState.Error("TTS playback error.")
        }
      },
    )
  }

  private fun splitIntoNaturalSentences(text: String): List<String> =
    text
      .split(Regex("(?<=[।.!?\\n])\\s+"))
      .map { it.trim() }
      .filter { it.isNotEmpty() }
}

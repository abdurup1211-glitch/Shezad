package com.example.overlay

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.view.WindowManager
import androidx.core.net.toUri
import com.example.utils.ErrorLogger
import com.example.utils.MaxModule
import com.example.utils.ModuleCategory
import com.example.utils.ModuleDescriptor
import com.example.utils.ModuleStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Operational mode of MAX's Floating Assistant.
 */
enum class FloatingBubbleMode {
  HIDDEN,
  IN_APP_MINIMIZED,
  IN_APP_EXPANDED,
  SYSTEM_OVERLAY_ACTIVE,
}

/**
 * Immutable UI & service state for [FloatingAssistantController].
 */
data class FloatingAssistantState(
  val mode: FloatingBubbleMode = FloatingBubbleMode.IN_APP_MINIMIZED,
  val hasOverlayPermission: Boolean = false,
  val systemOverlaySupported: Boolean = true,
  val offsetXFraction: Float = 0.82f,
  val offsetYFraction: Float = 0.72f,
  val statusMessage: String = "In-app floating assistant ready (zero-permission).",
)

/**
 * Coordinates both MAX's zero-permission in-app Floating Bubble (minimize/restore/quick-voice)
 * and the optional Android 8.0+ System Overlay Service ([FloatingOverlayService]) gated behind
 * device capability checks, explicit user opt-in, and [Settings.canDrawOverlays].
 */
class FloatingAssistantController(
  private val context: Context,
  private val errorLogger: ErrorLogger,
) : MaxModule {

  private val appContext = context.applicationContext

  private val _state =
    MutableStateFlow(
      FloatingAssistantState(
        hasOverlayPermission = checkSystemOverlayPermission(),
        systemOverlaySupported = isSystemOverlaySupported(),
      ),
    )
  val state: StateFlow<FloatingAssistantState> = _state.asStateFlow()

  override val descriptor: ModuleDescriptor
    get() =
      ModuleDescriptor(
        id = "floating_assistant_controller",
        displayName = "FloatingAssistant",
        packageName = "com.example.overlay",
        category = ModuleCategory.CORE_ORCHESTRATION,
        status =
          when {
            !_state.value.systemOverlaySupported && _state.value.mode == FloatingBubbleMode.HIDDEN ->
              ModuleStatus.DISABLED
            _state.value.mode == FloatingBubbleMode.HIDDEN -> ModuleStatus.STANDBY
            else -> ModuleStatus.READY
          },
        summary = "Floating bubble, minimize/restore quick voice & optional Android 8+ overlay service.",
        privacyGuarded = true,
      )

  /**
   * System-wide `TYPE_APPLICATION_OVERLAY` requires Android 8.0+ (API 26+), WindowManager
   * availability, non-Wear form factor, and non-Android Go (low-RAM) hardware.
   */
  fun isSystemOverlaySupported(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
    val activityManager = appContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    if (activityManager != null && activityManager.isLowRamDevice) return false
    if (appContext.packageManager.hasSystemFeature(PackageManager.FEATURE_WATCH)) return false
    return appContext.getSystemService(Context.WINDOW_SERVICE) is WindowManager
  }

  fun refreshOverlayPermission(): Boolean {
    val supported = isSystemOverlaySupported()
    val granted = if (supported) checkSystemOverlayPermission() else false
    if (!supported && _state.value.mode == FloatingBubbleMode.SYSTEM_OVERLAY_ACTIVE) {
      stopSystemOverlay()
    }
    _state.update {
      it.copy(
        hasOverlayPermission = granted,
        systemOverlaySupported = supported,
      )
    }
    return granted
  }

  fun checkSystemOverlayPermission(): Boolean {
    if (!isSystemOverlaySupported()) return false
    return runCatching { Settings.canDrawOverlays(appContext) }.getOrDefault(false)
  }

  fun showMinimizedBubble() {
    _state.update {
      it.copy(
        mode = FloatingBubbleMode.IN_APP_MINIMIZED,
        statusMessage = "Floating assistant bubble minimized and ready.",
      )
    }
  }

  fun expandBubble() {
    _state.update {
      it.copy(
        mode = FloatingBubbleMode.IN_APP_EXPANDED,
        statusMessage = "Floating quick assistant expanded.",
      )
    }
  }

  fun toggleMinimizeRestore() {
    val next =
      when (_state.value.mode) {
        FloatingBubbleMode.IN_APP_EXPANDED -> FloatingBubbleMode.IN_APP_MINIMIZED
        FloatingBubbleMode.IN_APP_MINIMIZED, FloatingBubbleMode.HIDDEN -> FloatingBubbleMode.IN_APP_EXPANDED
        FloatingBubbleMode.SYSTEM_OVERLAY_ACTIVE -> FloatingBubbleMode.IN_APP_EXPANDED
      }
    _state.update { it.copy(mode = next) }
  }

  fun hideBubble() {
    stopSystemOverlay()
    _state.update {
      it.copy(
        mode = FloatingBubbleMode.HIDDEN,
        statusMessage = "Floating assistant hidden.",
      )
    }
  }

  fun updateBubbleFraction(xFraction: Float, yFraction: Float) {
    _state.update {
      it.copy(
        offsetXFraction = xFraction.coerceIn(0.05f, 0.90f),
        offsetYFraction = yFraction.coerceIn(0.10f, 0.86f),
      )
    }
  }

  /**
   * Starts [FloatingOverlayService] ONLY if device supports Android 8.0+ overlays, user
   * explicitly opted in, and [Settings.canDrawOverlays] is granted; otherwise disables or keeps
   * the zero-permission in-app bubble safely active.
   */
  fun startSystemOverlayIfPermitted(explicitUserRequest: Boolean): Boolean {
    val supported = isSystemOverlaySupported()
    val granted = refreshOverlayPermission()
    if (!supported) {
      stopSystemOverlay()
      _state.update {
        it.copy(
          mode = FloatingBubbleMode.IN_APP_MINIMIZED,
          systemOverlaySupported = false,
          hasOverlayPermission = false,
          statusMessage = "এই ডিভাইস বা অ্যান্ড্রয়েড সংস্করণে সিস্টেম ওভারলে অনুপলব্ধ—ইন-অ্যাপ বাবল সক্রিয় আছে।",
        )
      }
      errorLogger.logInfo("FloatingAssistant", "System overlay unavailable on this device/OS; using in-app bubble.")
      return false
    }

    if (!explicitUserRequest || !granted) {
      _state.update {
        it.copy(
          mode = FloatingBubbleMode.IN_APP_EXPANDED,
          statusMessage =
            if (!granted) {
              "সিস্টেম ওভারলে অনুমতি নেই—নিরাপদ ইন-অ্যাপ ফ্লোটিং বাবল চালু করা হয়েছে।"
            } else {
              "ব্যবহারকারীর অনুমতি ছাড়া সিস্টেম ওভারলে চালু করা হয় না।"
            },
        )
      }
      return false
    }

    return runCatching {
      val intent = Intent(appContext, FloatingOverlayService::class.java).apply {
        action = FloatingOverlayService.ACTION_SHOW
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        appContext.startForegroundService(intent)
      }
      _state.update {
        it.copy(
          mode = FloatingBubbleMode.SYSTEM_OVERLAY_ACTIVE,
          systemOverlaySupported = true,
          hasOverlayPermission = true,
          statusMessage = "System floating assistant overlay active.",
        )
      }
      errorLogger.logInfo("FloatingAssistant", "FloatingOverlayService started cleanly.")
      true
    }.getOrElse { throwable ->
      errorLogger.logWarning("FloatingAssistant", "Overlay service blocked by device policy; using in-app bubble.", throwable)
      _state.update {
        it.copy(
          mode = FloatingBubbleMode.IN_APP_MINIMIZED,
          statusMessage = "ডিভাইস সীমাবদ্ধতার কারণে ইন-অ্যাপ বাবল ব্যবহৃত হচ্ছে।",
        )
      }
      false
    }
  }

  fun stopSystemOverlay() {
    runCatching {
      appContext.stopService(Intent(appContext, FloatingOverlayService::class.java))
    }
    if (_state.value.mode == FloatingBubbleMode.SYSTEM_OVERLAY_ACTIVE) {
      _state.update {
        it.copy(
          mode = FloatingBubbleMode.IN_APP_MINIMIZED,
          statusMessage = "System overlay stopped cleanly.",
        )
      }
    }
  }

  fun buildOverlayPermissionSettingsIntent(): Intent? {
    if (!isSystemOverlaySupported()) return null
    val intent =
      Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        "package:${appContext.packageName}".toUri(),
      ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    return if (intent.resolveActivity(appContext.packageManager) != null) intent else null
  }
}

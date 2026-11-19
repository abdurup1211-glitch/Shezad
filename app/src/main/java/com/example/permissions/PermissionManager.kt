package com.example.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.utils.ErrorLogger
import com.example.utils.MaxModule
import com.example.utils.ModuleCategory
import com.example.utils.ModuleDescriptor
import com.example.utils.ModuleStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Logical permission domains managed by MAX (Microphone, Camera, Gallery/Media,
 * Files, Location, Notifications, Phone, Internet).
 */
enum class AssistantPermission(
  val displayName: String,
  val manifestPermission: String?,
  val description: String,
) {
  MICROPHONE(
    displayName = "Microphone",
    manifestPermission = Manifest.permission.RECORD_AUDIO,
    description = "Activated only when user explicitly enables voice/wake-word.",
  ),
  CAMERA(
    displayName = "Camera",
    manifestPermission = Manifest.permission.CAMERA,
    description = "Opens only on explicit user command; resources released immediately.",
  ),
  MEDIA_LIBRARY(
    displayName = "Gallery / Photo Picker",
    manifestPermission = null,
    description = "Zero-permission Android Photo Picker; no background media scan.",
  ),
  FILES(
    displayName = "Files / Documents",
    manifestPermission = null,
    description = "Zero-permission Storage Access Framework picker on user request.",
  ),
  LOCATION(
    displayName = "Location",
    manifestPermission = Manifest.permission.ACCESS_FINE_LOCATION,
    description = "Strictly inactive unless explicitly requested by user.",
  ),
  PHONE_CALLS(
    displayName = "Phone Dialer",
    manifestPermission = null,
    description = "Uses safe user-confirmed dialer & messaging handoff.",
  ),
  NOTIFICATIONS(
    displayName = "Notifications",
    manifestPermission =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.POST_NOTIFICATIONS
      } else {
        null
      },
    description = "Controls reminder & assistant notifications on Android 13+.",
  ),
  INTERNET(
    displayName = "Internet",
    manifestPermission = Manifest.permission.INTERNET,
    description = "Used only when user explicitly asks for web search or summary.",
  ),
}

/**
 * Safe permission inspection and policy gatekeeper for MAX.
 */
interface PermissionManager : MaxModule {
  val permissionStates: StateFlow<Map<AssistantPermission, PermissionState>>

  fun checkPermissionStatus(permission: AssistantPermission): PermissionState

  fun refreshAllStatuses(): Map<AssistantPermission, PermissionState>

  fun isPrivacyLockActive(): Boolean

  fun setPrivacyLockActive(locked: Boolean)
}

/**
 * Android implementation of [PermissionManager] inspecting system grants passively
 * and enforcing MAX's global privacy lock.
 */
class AndroidPermissionManager(
  private val context: Context,
  private val errorLogger: ErrorLogger,
) : PermissionManager {

  private var privacyLockEnabled: Boolean = true

  private val _permissionStates = MutableStateFlow<Map<AssistantPermission, PermissionState>>(emptyMap())
  override val permissionStates: StateFlow<Map<AssistantPermission, PermissionState>> =
    _permissionStates.asStateFlow()

  override val descriptor: ModuleDescriptor
    get() =
      ModuleDescriptor(
        id = "permission_manager",
        displayName = "PermissionManager",
        packageName = "com.example.permissions",
        category = ModuleCategory.INFRASTRUCTURE,
        status = ModuleStatus.READY,
        summary = "Zero-background-access permission & privacy gatekeeper.",
        privacyGuarded = true,
      )

  init {
    refreshAllStatuses()
  }

  override fun checkPermissionStatus(permission: AssistantPermission): PermissionState {
    if (permission.manifestPermission == null) {
      return PermissionState.ZERO_PERMISSION_DESIGN
    }

    if (privacyLockEnabled &&
      permission in setOf(
        AssistantPermission.MICROPHONE,
        AssistantPermission.CAMERA,
        AssistantPermission.LOCATION,
      )
    ) {
      return PermissionState.PRIVACY_LOCKED
    }

    val granted =
      runCatching {
        ContextCompat.checkSelfPermission(context, permission.manifestPermission) ==
          PackageManager.PERMISSION_GRANTED
      }.getOrDefault(false)

    return if (granted) PermissionState.GRANTED else PermissionState.NOT_GRANTED
  }

  override fun refreshAllStatuses(): Map<AssistantPermission, PermissionState> {
    val snapshot = AssistantPermission.entries.associateWith { checkPermissionStatus(it) }
    _permissionStates.value = snapshot
    return snapshot
  }

  override fun isPrivacyLockActive(): Boolean = privacyLockEnabled

  override fun setPrivacyLockActive(locked: Boolean) {
    privacyLockEnabled = locked
    errorLogger.logInfo(
      tag = "PermissionManager",
      message = if (locked) "Global privacy lock enforced." else "Global privacy lock relaxed (passive audit).",
    )
    refreshAllStatuses()
  }
}

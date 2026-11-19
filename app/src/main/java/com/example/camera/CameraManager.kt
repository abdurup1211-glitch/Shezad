package com.example.camera

import android.content.Context
import android.content.Intent
import com.example.permissions.AssistantPermission
import com.example.permissions.PermissionManager
import com.example.permissions.PermissionRequestHandler
import com.example.permissions.PermissionState
import com.example.permissions.PrivacyCheckResult
import com.example.permissions.PrivacyController
import com.example.permissions.SensitiveDomain
import com.example.utils.ErrorLogger
import com.example.utils.MaxModule
import com.example.utils.ModuleCategory
import com.example.utils.ModuleDescriptor
import com.example.utils.ModuleStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * High-level privacy-guarded Camera subsystem manager connected with MAXCore & PermissionManager.
 */
class CameraManager(
  private val context: Context,
  private val controller: CameraController,
  private val permissionManager: PermissionManager,
  private val privacyController: PrivacyController,
  private val permissionRequestHandler: PermissionRequestHandler,
  private val errorLogger: ErrorLogger,
) : MaxModule {

  private val _state = MutableStateFlow<CameraState>(CameraState.PrivacyLocked)
  val state: StateFlow<CameraState> = _state.asStateFlow()

  override val descriptor: ModuleDescriptor
    get() =
      ModuleDescriptor(
        id = "camera_manager",
        displayName = "CameraManager",
        packageName = "com.example.camera",
        category = ModuleCategory.DEVICE_CAPABILITY,
        status =
          if (_state.value is CameraState.PrivacyLocked) ModuleStatus.PRIVACY_LOCKED else ModuleStatus.STANDBY,
        summary = "Explicit-command Photo, Selfie, Video & QR camera controller.",
        privacyGuarded = true,
      )

  fun requestCameraAction(
    mode: CameraMode,
    explicitUserRequest: Boolean,
  ): CameraActionOutcome {
    val privacyCheck =
      privacyController.validateActionAllowed(
        domain = SensitiveDomain.CAMERA,
        explicitUserRequest = explicitUserRequest,
      )
    if (privacyCheck is PrivacyCheckResult.Blocked) {
      _state.value = CameraState.PrivacyLocked
      return CameraActionOutcome.Blocked(
        "সানজু, নিরাপত্তার কারণে ক্যামেরা স্বয়ংক্রিয়ভাবে চালু করা হয় না। প্রাইভেসি লক পরীক্ষা করুন।",
      )
    }

    if (!controller.isCameraSupported()) {
      _state.value = CameraState.Unavailable("No camera hardware available.")
      return CameraActionOutcome.Unavailable("এই ডিভাইসে ক্যামেরা হার্ডওয়্যার পাওয়া যায়নি।")
    }

    val permissionState = permissionManager.checkPermissionStatus(AssistantPermission.CAMERA)
    if (permissionState != PermissionState.GRANTED && permissionState != PermissionState.ZERO_PERMISSION_DESIGN) {
      val rationale = permissionRequestHandler.buildRationale(AssistantPermission.CAMERA)
      _state.value = CameraState.PermissionRequired(rationale)
      return CameraActionOutcome.NeedsPermission(rationale)
    }

    val intent = controller.buildCaptureIntent(mode)
    return if (intent != null && intent.resolveActivity(context.packageManager) != null) {
      _state.value = CameraState.ActiveMode(mode)
      errorLogger.logInfo("CameraManager", "Prepared explicit camera intent for mode=${mode.label}.")
      CameraActionOutcome.ReadyIntent(intent, mode)
    } else {
      _state.value = CameraState.Unavailable("No compatible camera app available.")
      CameraActionOutcome.Unavailable("কোনো সামঞ্জস্যপূর্ণ ক্যামেরা অ্যাপ পাওয়া যায়নি।")
    }
  }

  fun releaseResources() {
    _state.value =
      if (permissionManager.isPrivacyLockActive()) CameraState.PrivacyLocked else CameraState.Standby
    errorLogger.logInfo("CameraManager", "Camera session released cleanly.")
  }
}

sealed class CameraActionOutcome {
  data class ReadyIntent(val intent: Intent, val mode: CameraMode) : CameraActionOutcome()

  data class NeedsPermission(val rationale: String) : CameraActionOutcome()

  data class Blocked(val message: String) : CameraActionOutcome()

  data class Unavailable(val message: String) : CameraActionOutcome()
}

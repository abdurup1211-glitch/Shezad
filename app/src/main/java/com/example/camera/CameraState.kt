package com.example.camera

/**
 * Supported user-requested camera modes in MAX.
 */
enum class CameraMode(val label: String) {
  PHOTO("Photo"),
  SELFIE("Selfie"),
  VIDEO("Video"),
  QR_SCAN("QR Scan"),
}

/**
 * Operational state of [CameraManager].
 */
sealed class CameraState {
  data object PrivacyLocked : CameraState()

  data object Standby : CameraState()

  data class ActiveMode(val mode: CameraMode) : CameraState()

  data class PermissionRequired(val rationale: String) : CameraState()

  data class Unavailable(val reason: String) : CameraState()
}

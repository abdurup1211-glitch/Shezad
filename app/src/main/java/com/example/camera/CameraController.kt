package com.example.camera

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.MediaStore
import com.example.utils.ErrorLogger

/**
 * Low-level hardware & standard Android camera intent builder.
 * Never opens camera in background and releases references immediately after use.
 */
class CameraController(
  private val context: Context,
  private val errorLogger: ErrorLogger,
) {

  fun isCameraSupported(): Boolean =
    runCatching {
      context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }.getOrDefault(false)

  fun hasFrontCamera(): Boolean =
    runCatching {
      context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)
    }.getOrDefault(false)

  fun buildCaptureIntent(mode: CameraMode): Intent? {
    if (!isCameraSupported()) {
      errorLogger.logWarning("CameraController", "No camera hardware detected on device.")
      return null
    }
    return when (mode) {
      CameraMode.PHOTO, CameraMode.QR_SCAN -> Intent(MediaStore.ACTION_IMAGE_CAPTURE)
      CameraMode.SELFIE ->
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
          putExtra("android.intent.extras.CAMERA_FACING", 1)
          putExtra("android.intent.extra.USE_FRONT_CAMERA", true)
        }
      CameraMode.VIDEO -> Intent(MediaStore.ACTION_VIDEO_CAPTURE)
    }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
  }
}

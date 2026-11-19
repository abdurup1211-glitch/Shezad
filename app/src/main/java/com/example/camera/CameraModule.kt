package com.example.camera

import android.content.Context
import android.content.pm.PackageManager
import com.example.utils.MaxModule
import com.example.utils.ModuleCategory
import com.example.utils.ModuleDescriptor
import com.example.utils.ModuleStatus

/**
 * Capability contract for future visual perception features in MAX.
 *
 * Foundation privacy rule: never activates the camera or opens a capture session.
 */
interface CameraCapability : MaxModule {
  fun hasCameraHardware(): Boolean
}

/**
 * Passive foundation implementation of [CameraCapability].
 */
class PassiveCameraModule(
  private val context: Context,
) : CameraCapability {

  override val descriptor: ModuleDescriptor
    get() =
      ModuleDescriptor(
        id = "camera_module",
        displayName = "CameraCapability",
        packageName = "com.example.camera",
        category = ModuleCategory.DEVICE_CAPABILITY,
        status = ModuleStatus.PRIVACY_LOCKED,
        summary = "Camera hardware inactive. Reserved for explicit opt-in visual tasks.",
        privacyGuarded = true,
      )

  override fun hasCameraHardware(): Boolean =
    runCatching {
      context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }.getOrDefault(false)
}

package com.example.gallery

import com.example.utils.MaxModule
import com.example.utils.ModuleCategory
import com.example.utils.ModuleDescriptor
import com.example.utils.ModuleStatus

/**
 * Capability contract for future media selection via Android Photo Picker.
 *
 * Foundation privacy rule: never queries, scans, or accesses gallery media.
 */
interface GalleryCapability : MaxModule {
  fun usesZeroPermissionPicker(): Boolean
}

/**
 * Passive foundation implementation of [GalleryCapability].
 */
class PassiveGalleryModule : GalleryCapability {

  override val descriptor: ModuleDescriptor
    get() =
      ModuleDescriptor(
        id = "gallery_module",
        displayName = "GalleryCapability",
        packageName = "com.example.gallery",
        category = ModuleCategory.DEVICE_CAPABILITY,
        status = ModuleStatus.PRIVACY_LOCKED,
        summary = "Media library untouched. Designed for zero-permission Photo Picker.",
        privacyGuarded = true,
      )

  override fun usesZeroPermissionPicker(): Boolean = true
}

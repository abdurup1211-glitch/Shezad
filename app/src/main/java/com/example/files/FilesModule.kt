package com.example.files

import com.example.utils.MaxModule
import com.example.utils.ModuleCategory
import com.example.utils.ModuleDescriptor
import com.example.utils.ModuleStatus

/**
 * Capability contract for future user-selected document workflows via Storage Access Framework.
 *
 * Foundation privacy rule: never scans, reads, or indexes device storage.
 */
interface FileAccessCapability : MaxModule {
  fun isFileSystemAccessLocked(): Boolean
}

/**
 * Passive foundation implementation of [FileAccessCapability].
 */
class PassiveFilesModule : FileAccessCapability {

  override val descriptor: ModuleDescriptor
    get() =
      ModuleDescriptor(
        id = "files_module",
        displayName = "FileAccessCapability",
        packageName = "com.example.files",
        category = ModuleCategory.DEVICE_CAPABILITY,
        status = ModuleStatus.PRIVACY_LOCKED,
        summary = "Device file access strictly inactive; SAF picker ready for future use.",
        privacyGuarded = true,
      )

  override fun isFileSystemAccessLocked(): Boolean = true
}

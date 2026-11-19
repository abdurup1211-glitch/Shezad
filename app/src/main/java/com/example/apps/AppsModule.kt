package com.example.apps

import com.example.utils.MaxModule
import com.example.utils.ModuleCategory
import com.example.utils.ModuleDescriptor
import com.example.utils.ModuleStatus

/**
 * Capability contract for future explicit Android intent routing to external apps.
 *
 * Complies with Google Play package-visibility policy (does not enumerate installed apps).
 */
interface AppLauncherCapability : MaxModule {
  fun supportsExplicitIntentRouting(): Boolean
}

/**
 * Passive foundation implementation of [AppLauncherCapability].
 */
class PassiveAppsModule : AppLauncherCapability {

  override val descriptor: ModuleDescriptor
    get() =
      ModuleDescriptor(
        id = "apps_module",
        displayName = "AppLauncherCapability",
        packageName = "com.example.apps",
        category = ModuleCategory.DEVICE_CAPABILITY,
        status = ModuleStatus.STANDBY,
        summary = "Explicit intent-routing contract (no package enumeration).",
      )

  override fun supportsExplicitIntentRouting(): Boolean = true
}

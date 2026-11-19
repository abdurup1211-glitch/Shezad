package com.example.internet

import com.example.utils.MaxModule
import com.example.utils.ModuleCategory
import com.example.utils.ModuleDescriptor
import com.example.utils.ModuleStatus

/**
 * Structured request contract for future web/API connectivity in MAX.
 */
data class WebQueryContract(
  val endpoint: String,
  val requiresUserApproval: Boolean = true,
)

/**
 * Capability contract for future internet & tool connectivity.
 *
 * Foundation stage makes zero outbound network calls.
 */
interface NetworkCapability : MaxModule {
  fun isNetworkExecutionEnabled(): Boolean
}

/**
 * Passive foundation implementation of [NetworkCapability].
 */
class PassiveInternetModule : NetworkCapability {

  override val descriptor: ModuleDescriptor
    get() =
      ModuleDescriptor(
        id = "internet_module",
        displayName = "NetworkCapability",
        packageName = "com.example.internet",
        category = ModuleCategory.DEVICE_CAPABILITY,
        status = ModuleStatus.STANDBY,
        summary = "Outbound web/API transport contract ready (offline foundation).",
      )

  override fun isNetworkExecutionEnabled(): Boolean = false
}

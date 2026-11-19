package com.example.phone

import com.example.utils.MaxModule
import com.example.utils.ModuleCategory
import com.example.utils.ModuleDescriptor
import com.example.utils.ModuleStatus

/**
 * Capability contract for future user-confirmed dialer/communication handoff.
 *
 * Foundation privacy rule: never reads contacts, call logs, or places calls.
 */
interface PhoneCapability : MaxModule {
  fun isTelephonyLocked(): Boolean
}

/**
 * Passive foundation implementation of [PhoneCapability].
 */
class PassivePhoneModule : PhoneCapability {

  override val descriptor: ModuleDescriptor
    get() =
      ModuleDescriptor(
        id = "phone_module",
        displayName = "PhoneCapability",
        packageName = "com.example.phone",
        category = ModuleCategory.DEVICE_CAPABILITY,
        status = ModuleStatus.PRIVACY_LOCKED,
        summary = "Telephony & dialer intents strictly locked in foundation stage.",
        privacyGuarded = true,
      )

  override fun isTelephonyLocked(): Boolean = true
}

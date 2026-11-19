package com.example.utils

/**
 * Architectural categories matching the modular package structure of MAX.
 */
enum class ModuleCategory {
  CORE_ORCHESTRATION,
  VOICE_AND_AUDIO,
  COGNITION_AND_MEMORY,
  DEVICE_CAPABILITY,
  INFRASTRUCTURE,
}

/**
 * Operational readiness status reported by every MAX subsystem.
 */
enum class ModuleStatus {
  READY,
  STANDBY,
  PRIVACY_LOCKED,
  DISABLED,
  ERROR,
}

/**
 * Descriptive architectural metadata for a registered MAX module.
 */
data class ModuleDescriptor(
  val id: String,
  val displayName: String,
  val packageName: String,
  val category: ModuleCategory,
  val status: ModuleStatus,
  val summary: String,
  val privacyGuarded: Boolean = false,
)

/**
 * Common contract implemented by modular MAX subsystems so [com.example.core.MAXCore]
 * can inspect readiness and enforce global privacy locks uniformly.
 */
interface MaxModule {
  val descriptor: ModuleDescriptor
}

package com.example.memory

/**
 * Privacy and user-control rules governing MAX's [MemoryManager].
 */
data class MemorySettings(
  val memoryEnabled: Boolean = true,
  val allowSensitiveWithConsent: Boolean = false,
  val persistOnDevice: Boolean = true,
  val maxEntries: Int = 100,
)

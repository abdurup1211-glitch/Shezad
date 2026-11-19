package com.example.permissions

import com.example.utils.ErrorLogger

/**
 * Sensitive capability domains guarded by [PrivacyController].
 */
enum class SensitiveDomain {
  MICROPHONE,
  CAMERA,
  GALLERY,
  FILES,
  LOCATION,
  INTERNET,
  PHONE,
  APPS,
}

/**
 * Decision returned before executing any sensitive capability in MAX.
 */
sealed class PrivacyCheckResult {
  data object Allowed : PrivacyCheckResult()

  data class Blocked(val reason: String) : PrivacyCheckResult()
}

/**
 * Enforces MAX's strict zero-background-access policy across every sensitive subsystem.
 */
class PrivacyController(
  private val permissionManager: PermissionManager,
  private val errorLogger: ErrorLogger,
) {

  /**
   * Validates that [domain] is ONLY invoked from an explicit user command and respects
   * MAX's global privacy lock.
   */
  fun validateActionAllowed(
    domain: SensitiveDomain,
    explicitUserRequest: Boolean,
  ): PrivacyCheckResult {
    if (!explicitUserRequest) {
      val reason = "Blocked automatic $domain access: explicit user request required."
      errorLogger.logWarning("PrivacyController", reason)
      return PrivacyCheckResult.Blocked(reason)
    }

    if (permissionManager.isPrivacyLockActive() &&
      domain in setOf(SensitiveDomain.CAMERA, SensitiveDomain.LOCATION, SensitiveDomain.MICROPHONE)
    ) {
      val reason = "Privacy Shield is currently locked for $domain."
      errorLogger.logInfo("PrivacyController", reason)
      return PrivacyCheckResult.Blocked(reason)
    }

    return PrivacyCheckResult.Allowed
  }
}

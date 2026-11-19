package com.example.permissions

/**
 * Current authorization state of an [AssistantPermission].
 */
enum class PermissionState {
  GRANTED,
  NOT_GRANTED,
  RATIONALE_REQUIRED,
  ZERO_PERMISSION_DESIGN,
  PRIVACY_LOCKED,
}

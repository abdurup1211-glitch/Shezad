package com.example.permissions

import com.example.utils.ErrorLogger

/**
 * Prepares human-friendly explanations and safe user-initiated permission flows
 * so MAX never triggers OS permission prompts unexpectedly.
 */
class PermissionRequestHandler(
  private val permissionManager: PermissionManager,
  private val errorLogger: ErrorLogger,
) {

  fun buildRationale(permission: AssistantPermission): String =
    when (permission) {
      AssistantPermission.MICROPHONE ->
        "MAX শুধুমাত্র আপনি কথা বলার সময় মাইক্রোফোন ব্যবহার করবে; কোনো অডিও সংরক্ষণ করা হবে না।"
      AssistantPermission.CAMERA ->
        "ক্যামেরা শুধুমাত্র আপনার নির্দেশে ছবি তোলা বা QR স্ক্যানের জন্য খোলা হবে।"
      AssistantPermission.MEDIA_LIBRARY ->
        "নিরাপদ Android Photo Picker ব্যবহার করে কেবল আপনার নির্বাচিত ছবি দেখানো হবে।"
      AssistantPermission.FILES ->
        "নিরাপদ ফাইল পিকারের মাধ্যমে শুধু আপনার বেছে নেওয়া ডকুমেন্ট পড়া হবে।"
      AssistantPermission.LOCATION ->
        "লোকেশন শুধুমাত্র আপনার অনুরোধে ম্যাপ বা আবহাওয়ার জন্য ব্যবহার করা হবে।"
      AssistantPermission.PHONE_CALLS ->
        "আপনার নিশ্চিতকরণের পরেই কেবল ফোন ডায়ালার বা কল হ্যান্ডঅফ করা হবে।"
      AssistantPermission.NOTIFICATIONS ->
        "রিমাইন্ডার ও সহায়ক অ্যালার্ট দেখানোর জন্য নোটিফিকেশন অনুমতি প্রয়োজন।"
      AssistantPermission.INTERNET ->
        "ওয়েব সার্চ ও তথ্যের জন্য শুধুমাত্র আপনার অনুরোধে ইন্টারনেট ব্যবহার হবে।"
    }

  fun canProceedOrNeedsPrompt(permission: AssistantPermission): PermissionState {
    val current = permissionManager.checkPermissionStatus(permission)
    errorLogger.logInfo("PermissionRequestHandler", "Checked ${permission.displayName} -> $current")
    return current
  }
}

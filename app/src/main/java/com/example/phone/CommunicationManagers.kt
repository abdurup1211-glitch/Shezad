package com.example.phone

import android.content.Intent
import androidx.core.net.toUri

/**
 * Prepares explicit user-confirmed dialer handoff intents.
 * Never places phone calls automatically and never reads call logs.
 */
class CallManager {
  fun buildDialIntent(phoneNumber: String, userConfirmed: Boolean): Intent? {
    if (!userConfirmed) return null
    val sanitized = phoneNumber.filter { it.isDigit() || it == '+' || it == '*' || it == '#' }
    if (sanitized.isBlank()) return null
    return Intent(Intent.ACTION_DIAL, "tel:$sanitized".toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
  }
}

/**
 * Prepares explicit user-confirmed SMS composer intents.
 * Never sends SMS messages automatically and never reads inbox SMS.
 */
class SMSManager {
  fun buildSmsComposerIntent(
    phoneNumber: String,
    body: String,
    userConfirmed: Boolean,
  ): Intent? {
    if (!userConfirmed) return null
    val sanitized = phoneNumber.filter { it.isDigit() || it == '+' }
    return Intent(Intent.ACTION_SENDTO, "smsto:$sanitized".toUri()).apply {
      putExtra("sms_body", body)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
  }
}

/**
 * Prepares zero-permission user-initiated contact picker intent.
 * Never reads or syncs contacts secretly.
 */
class ContactsManager {
  fun buildPickContactIntent(explicitUserRequest: Boolean): Intent? {
    if (!explicitUserRequest) return null
    return Intent(Intent.ACTION_PICK).apply {
      type = android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE
    }
  }
}

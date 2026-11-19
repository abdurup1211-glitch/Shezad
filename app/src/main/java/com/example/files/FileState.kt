package com.example.files

import android.net.Uri

/**
 * Operational state of [FileManager].
 */
sealed class FileState {
  data object PrivacyLocked : FileState()

  data object Standby : FileState()

  data class FileLoaded(
    val uri: Uri,
    val displayName: String,
    val mimeType: String,
    val previewText: String,
  ) : FileState()

  data class Error(val message: String) : FileState()
}

package com.example.gallery

import android.net.Uri

/**
 * Operational state of [GalleryManager].
 */
sealed class GalleryState {
  data object PrivacyLocked : GalleryState()

  data object Standby : GalleryState()

  data class SelectedPhotos(val uris: List<Uri>) : GalleryState()

  data class OcrReady(val imageUri: Uri, val extractedPreview: String) : GalleryState()
}

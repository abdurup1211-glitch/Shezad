package com.example.gallery

import android.net.Uri
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts

/**
 * Zero-permission Android Photo Picker helper & future image search / OCR bridge.
 */
class GalleryController {

  fun createPhotoPickerRequest(
    mediaType: ActivityResultContracts.PickVisualMedia.VisualMediaType =
      ActivityResultContracts.PickVisualMedia.ImageOnly,
  ): PickVisualMediaRequest = PickVisualMediaRequest(mediaType)

  fun filterSelectedByQuery(uris: List<Uri>, query: String): List<Uri> {
    if (query.isBlank()) return uris
    return uris.filter { it.toString().contains(query, ignoreCase = true) }
  }
}

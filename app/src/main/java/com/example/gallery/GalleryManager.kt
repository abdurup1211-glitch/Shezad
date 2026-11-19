package com.example.gallery

import android.net.Uri
import com.example.permissions.PrivacyCheckResult
import com.example.permissions.PrivacyController
import com.example.permissions.SensitiveDomain
import com.example.utils.ErrorLogger
import com.example.utils.MaxModule
import com.example.utils.ModuleCategory
import com.example.utils.ModuleDescriptor
import com.example.utils.ModuleStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Privacy-first Gallery subsystem manager.
 *
 * Never scans device media in the background and never deletes files without confirmation.
 */
class GalleryManager(
  private val controller: GalleryController,
  private val privacyController: PrivacyController,
  private val errorLogger: ErrorLogger,
) : MaxModule {

  private val _state = MutableStateFlow<GalleryState>(GalleryState.PrivacyLocked)
  val state: StateFlow<GalleryState> = _state.asStateFlow()

  override val descriptor: ModuleDescriptor
    get() =
      ModuleDescriptor(
        id = "gallery_manager",
        displayName = "GalleryManager",
        packageName = "com.example.gallery",
        category = ModuleCategory.DEVICE_CAPABILITY,
        status = ModuleStatus.PRIVACY_LOCKED,
        summary = "Zero-permission Photo Picker, photo display & OCR hook (no background scan).",
        privacyGuarded = true,
      )

  fun requestOpenGallery(explicitUserRequest: Boolean): Boolean {
    val check = privacyController.validateActionAllowed(SensitiveDomain.GALLERY, explicitUserRequest)
    if (check is PrivacyCheckResult.Blocked) {
      _state.value = GalleryState.PrivacyLocked
      return false
    }
    _state.value = GalleryState.Standby
    errorLogger.logInfo("GalleryManager", "Gallery picker authorized on explicit user request.")
    return true
  }

  fun onPhotosSelected(uris: List<Uri>) {
    _state.value = GalleryState.SelectedPhotos(uris)
    errorLogger.logInfo("GalleryManager", "User selected ${uris.size} photo(s) via Photo Picker.")
  }

  fun findPhotos(query: String): List<Uri> {
    val current = (_state.value as? GalleryState.SelectedPhotos)?.uris.orEmpty()
    return controller.filterSelectedByQuery(current, query)
  }

  fun prepareOcrForSelectedPhoto(uri: Uri): GalleryState.OcrReady {
    val ready =
      GalleryState.OcrReady(
        imageUri = uri,
        extractedPreview = "OCR hook prepared for user-selected image (${uri.lastPathSegment ?: "image"}).",
      )
    _state.value = ready
    return ready
  }

  fun clearSelection() {
    _state.value = GalleryState.PrivacyLocked
  }
}

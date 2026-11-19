package com.example.files

import android.content.Context
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
 * Central privacy-first File subsystem coordinator.
 *
 * Never reads, deletes, modifies, or uploads files automatically.
 */
class FileManager(
  private val context: Context,
  val fileBrowser: FileBrowser,
  val pdfReader: PDFReader,
  val documentReader: DocumentReader,
  val imageReader: ImageReader,
  val ocrManager: OCRManager,
  private val privacyController: PrivacyController,
  private val errorLogger: ErrorLogger,
) : MaxModule {

  private val _state = MutableStateFlow<FileState>(FileState.PrivacyLocked)
  val state: StateFlow<FileState> = _state.asStateFlow()

  override val descriptor: ModuleDescriptor
    get() =
      ModuleDescriptor(
        id = "file_manager",
        displayName = "FileManager",
        packageName = "com.example.files",
        category = ModuleCategory.DEVICE_CAPABILITY,
        status = ModuleStatus.PRIVACY_LOCKED,
        summary = "User-picked SAF browsing, PDF/Text/Image readers & OCR contract.",
        privacyGuarded = true,
      )

  fun inspectUserSelectedFile(uri: Uri, explicitUserRequest: Boolean): FileState {
    val check = privacyController.validateActionAllowed(SensitiveDomain.FILES, explicitUserRequest)
    if (check is PrivacyCheckResult.Blocked) {
      _state.value = FileState.PrivacyLocked
      return _state.value
    }

    val name = fileBrowser.queryDisplayName(context, uri)
    val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
    val preview =
      when {
        mimeType.contains("pdf", ignoreCase = true) -> pdfReader.readPdfSummary(context, uri)
        mimeType.startsWith("image/") -> imageReader.readImageDimensions(context, uri)
        mimeType.startsWith("text/") -> documentReader.readTextPreview(context, uri)
        else -> "Selected file '$name' ($mimeType)."
      }

    val loaded = FileState.FileLoaded(uri = uri, displayName = name, mimeType = mimeType, previewText = preview)
    _state.value = loaded
    errorLogger.logInfo("FileManager", "Inspected user-picked file '$name'.")
    return loaded
  }

  fun lockFiles() {
    _state.value = FileState.PrivacyLocked
  }
}

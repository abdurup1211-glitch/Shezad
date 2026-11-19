package com.example.files

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.OpenableColumns
import com.example.utils.ErrorLogger

/**
 * Zero-permission Storage Access Framework picker helper.
 * Never scans device directories automatically.
 */
class FileBrowser {
  fun buildOpenDocumentIntent(mimeTypes: Array<String> = arrayOf("*/*")): Intent =
    Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
      addCategory(Intent.CATEGORY_OPENABLE)
      type = "*/*"
      putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
    }

  fun queryDisplayName(context: Context, uri: Uri): String =
    runCatching {
      context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
      }
    }.getOrNull() ?: (uri.lastPathSegment ?: "selected_file")
}

/**
 * Safe PDF metadata & page count reader for user-selected PDF documents.
 */
class PDFReader(private val errorLogger: ErrorLogger) {
  fun readPdfSummary(context: Context, uri: Uri): String =
    runCatching {
      context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
        PdfRenderer(pfd).use { renderer ->
          "PDF document ready (${renderer.pageCount} page${if (renderer.pageCount == 1) "" else "s"})."
        }
      } ?: "Unable to open PDF descriptor."
    }.onFailure { error ->
      errorLogger.logWarning("PDFReader", "Could not inspect PDF document.", error)
    }.getOrElse { "PDF preview unavailable on this device." }
}

/**
 * Safe plain-text document reader bounded to prevent excessive memory allocation on low-RAM devices.
 */
class DocumentReader(private val errorLogger: ErrorLogger) {
  fun readTextPreview(context: Context, uri: Uri, maxChars: Int = 2000): String =
    runCatching {
      context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
        val buffer = CharArray(maxChars)
        val count = reader.read(buffer)
        if (count > 0) String(buffer, 0, count) else "Document is empty."
      } ?: "Unable to open document stream."
    }.onFailure { error ->
      errorLogger.logWarning("DocumentReader", "Could not read text document.", error)
    }.getOrElse { "Document preview unavailable." }
}

/**
 * Safe image metadata reader using inJustDecodeBounds to keep memory usage near zero.
 */
class ImageReader(private val errorLogger: ErrorLogger) {
  fun readImageDimensions(context: Context, uri: Uri): String =
    runCatching {
      val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, options)
      }
      if (options.outWidth > 0 && options.outHeight > 0) {
        "Image dimensions: ${options.outWidth} × ${options.outHeight} px (${options.outMimeType ?: "image"})."
      } else {
        "Image metadata unavailable."
      }
    }.onFailure { error ->
      errorLogger.logWarning("ImageReader", "Could not inspect image bounds.", error)
    }.getOrElse { "Image preview unavailable." }
}

/**
 * Foundation OCR pipeline contract ready for user-selected images.
 */
class OCRManager {
  fun extractTextFromSelectedImage(uri: Uri, explicitUserRequest: Boolean): String {
    if (!explicitUserRequest) {
      return "OCR blocked: explicit user selection required."
    }
    return "OCR contract ready for '${uri.lastPathSegment ?: "image"}'. Connect ML Kit Text Recognition when enabled."
  }
}

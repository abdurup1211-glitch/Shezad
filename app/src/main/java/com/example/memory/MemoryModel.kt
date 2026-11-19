package com.example.memory

import java.util.UUID

/**
 * Semantic classification for structured assistant memory entries (Step 1 compatible).
 */
enum class MemoryCategory(val label: String) {
  PREFERENCE("Preference"),
  CONTEXT("Context"),
  FACT("Fact"),
  SESSION("Session"),
}

/**
 * Step 5 memory types for MAX personalization and user control.
 */
enum class MemoryType(val label: String) {
  USER_NAME("User Name"),
  USER_PREFERENCE("Preference"),
  CONVERSATION_CONTEXT("Context"),
  USER_ROUTINE("Routine"),
  USER_SETTING("Setting"),
}

/**
 * Structured memory model for [MemoryRepository] and [MemoryManager].
 */
data class MemoryModel(
  val id: String = UUID.randomUUID().toString(),
  val key: String,
  val content: String,
  val category: MemoryCategory = MemoryCategory.CONTEXT,
  val memoryType: MemoryType = MemoryType.CONVERSATION_CONTEXT,
  val isPinned: Boolean = false,
  val isSensitiveConfirmed: Boolean = false,
  val updatedAtMillis: Long = System.currentTimeMillis(),
)

/**
 * Step 1 typealias compatibility so existing callers of [MemoryEntry] keep working seamlessly.
 */
typealias MemoryEntry = MemoryModel

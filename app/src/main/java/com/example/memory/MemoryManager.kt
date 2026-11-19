package com.example.memory

import com.example.utils.ErrorLogger
import com.example.utils.MaxModule
import com.example.utils.ModuleCategory
import com.example.utils.ModuleDescriptor
import com.example.utils.ModuleStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Foundation contract for MAX's user memory and context recall system.
 */
interface MemoryManager : MaxModule {
  val memories: StateFlow<List<MemoryEntry>>
  val memorySettings: StateFlow<MemorySettings>

  fun saveMemory(
    key: String,
    content: String,
    category: MemoryCategory = MemoryCategory.CONTEXT,
    pinned: Boolean = false,
  ): MemoryEntry

  fun saveTypedMemory(
    key: String,
    content: String,
    type: MemoryType,
    category: MemoryCategory = MemoryCategory.PREFERENCE,
    pinned: Boolean = false,
  ): MemoryModel

  fun recallMemories(query: String): List<MemoryEntry>

  fun getByCategory(category: MemoryCategory): List<MemoryEntry>

  fun togglePin(id: String)

  fun deleteMemory(id: String)

  fun clearAll()
}

/**
 * Clean memory manager supporting both lightweight in-memory operation and optional
 * [MemoryRepository] backing for durable device-local storage.
 */
class InMemoryMemoryManager(
  private val errorLogger: ErrorLogger,
  private val repository: MemoryRepository? = null,
) : MemoryManager {

  private val _memories = MutableStateFlow<List<MemoryEntry>>(emptyList())
  override val memories: StateFlow<List<MemoryEntry>> = repository?.entries ?: _memories.asStateFlow()

  private val _memorySettings = MutableStateFlow(MemorySettings())
  override val memorySettings: StateFlow<MemorySettings> = _memorySettings.asStateFlow()

  override val descriptor: ModuleDescriptor
    get() =
      ModuleDescriptor(
        id = "memory_manager",
        displayName = "MemoryManager",
        packageName = "com.example.memory",
        category = ModuleCategory.COGNITION_AND_MEMORY,
        status = ModuleStatus.READY,
        summary = "Private user memory vault (${memories.value.size} entries, user-controlled).",
      )

  override fun saveMemory(
    key: String,
    content: String,
    category: MemoryCategory,
    pinned: Boolean,
  ): MemoryEntry {
    val type =
      when (category) {
        MemoryCategory.PREFERENCE -> MemoryType.USER_PREFERENCE
        MemoryCategory.FACT -> MemoryType.USER_NAME
        MemoryCategory.CONTEXT -> MemoryType.CONVERSATION_CONTEXT
        MemoryCategory.SESSION -> MemoryType.USER_SETTING
      }
    return saveTypedMemory(key = key, content = content, type = type, category = category, pinned = pinned)
  }

  override fun saveTypedMemory(
    key: String,
    content: String,
    type: MemoryType,
    category: MemoryCategory,
    pinned: Boolean,
  ): MemoryModel {
    val sanitizedKey = key.trim().ifEmpty { "Note" }
    val sanitizedContent = content.trim()
    val entry =
      MemoryModel(
        key = sanitizedKey,
        content = sanitizedContent,
        category = category,
        memoryType = type,
        isPinned = pinned,
      )
    if (repository != null) {
      repository.upsert(entry)
    } else {
      _memories.update { current ->
        listOf(entry) + current.filterNot { it.key.equals(sanitizedKey, ignoreCase = true) }
      }
    }
    errorLogger.logInfo("MemoryManager", "Stored memory entry '${entry.key}' (${type.label}).")
    return entry
  }

  override fun recallMemories(query: String): List<MemoryEntry> {
    if (repository != null) return repository.find(query)
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return _memories.value
    return _memories.value.filter { entry ->
      entry.key.contains(trimmed, ignoreCase = true) ||
        entry.content.contains(trimmed, ignoreCase = true)
    }
  }

  override fun getByCategory(category: MemoryCategory): List<MemoryEntry> =
    memories.value.filter { it.category == category }

  override fun togglePin(id: String) {
    if (repository != null) {
      repository.togglePin(id)
    } else {
      _memories.update { current ->
        current.map { entry ->
          if (entry.id == id) entry.copy(isPinned = !entry.isPinned) else entry
        }
      }
    }
  }

  override fun deleteMemory(id: String) {
    if (repository != null) {
      repository.remove(id)
    } else {
      _memories.update { current -> current.filterNot { it.id == id } }
    }
  }

  override fun clearAll() {
    if (repository != null) {
      repository.clearAll()
    } else {
      _memories.value = emptyList()
    }
    errorLogger.logInfo("MemoryManager", "Cleared user memory entries.")
  }
}

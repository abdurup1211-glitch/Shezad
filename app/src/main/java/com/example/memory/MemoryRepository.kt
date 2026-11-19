package com.example.memory

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.utils.ErrorLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val Context.memoryVaultDataStore: DataStore<Preferences> by
  preferencesDataStore(name = "max_memory_vault")

/**
 * Storage contract for MAX's lightweight, private on-device memory vault.
 */
interface MemoryRepository {
  val entries: StateFlow<List<MemoryModel>>

  fun upsert(entry: MemoryModel)

  fun find(query: String): List<MemoryModel>

  fun togglePin(id: String)

  fun remove(id: String)

  fun clearAll()
}

/**
 * Lightweight, privacy-respecting local [MemoryRepository] backed by Jetpack DataStore.
 * Never shares memory externally and never reads without user interaction.
 */
class LocalMemoryRepository(
  context: Context,
  private val errorLogger: ErrorLogger,
  private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : MemoryRepository {

  private val dataStore = context.applicationContext.memoryVaultDataStore
  private val entriesKey = stringPreferencesKey("serialized_entries_v1")

  private val _entries = MutableStateFlow<List<MemoryModel>>(emptyList())
  override val entries: StateFlow<List<MemoryModel>> = _entries.asStateFlow()

  init {
    scope.launch {
      runCatching {
        val prefs = dataStore.data.first()
        val raw = prefs[entriesKey].orEmpty()
        if (raw.isNotBlank()) {
          _entries.value = decodeEntries(raw)
        }
      }.onFailure { error ->
        errorLogger.logWarning("MemoryRepository", "Could not restore saved memory vault.", error)
      }
    }
  }

  override fun upsert(entry: MemoryModel) {
    _entries.update { current ->
      val updated = listOf(entry) + current.filterNot { it.key.equals(entry.key, ignoreCase = true) }
      persistAsync(updated)
      updated
    }
  }

  override fun find(query: String): List<MemoryModel> {
    val q = query.trim()
    if (q.isEmpty()) return _entries.value
    return _entries.value.filter {
      it.key.contains(q, ignoreCase = true) || it.content.contains(q, ignoreCase = true)
    }
  }

  override fun togglePin(id: String) {
    _entries.update { current ->
      val updated = current.map { if (it.id == id) it.copy(isPinned = !it.isPinned) else it }
      persistAsync(updated)
      updated
    }
  }

  override fun remove(id: String) {
    _entries.update { current ->
      val updated = current.filterNot { it.id == id }
      persistAsync(updated)
      updated
    }
  }

  override fun clearAll() {
    _entries.value = emptyList()
    scope.launch {
      runCatching { dataStore.edit { it.remove(entriesKey) } }
    }
  }

  private fun persistAsync(list: List<MemoryModel>) {
    scope.launch {
      runCatching {
        val encoded = encodeEntries(list)
        dataStore.edit { prefs -> prefs[entriesKey] = encoded }
      }.onFailure { error ->
        errorLogger.logError("MemoryRepository", "Failed persisting memory entry.", error)
      }
    }
  }

  private fun encodeEntries(list: List<MemoryModel>): String =
    list.joinToString(separator = "\n") { item ->
      listOf(
        item.id.sanitizeToken(),
        item.key.sanitizeToken(),
        item.content.sanitizeToken(),
        item.category.name,
        item.memoryType.name,
        item.isPinned.toString(),
        item.updatedAtMillis.toString(),
      ).joinToString(separator = "\t")
    }

  private fun decodeEntries(raw: String): List<MemoryModel> =
    raw.lines()
      .filter { it.isNotBlank() }
      .mapNotNull { line ->
        val parts = line.split("\t")
        if (parts.size < 7) return@mapNotNull null
        MemoryModel(
          id = parts[0],
          key = parts[1],
          content = parts[2],
          category = runCatching { MemoryCategory.valueOf(parts[3]) }.getOrDefault(MemoryCategory.CONTEXT),
          memoryType = runCatching { MemoryType.valueOf(parts[4]) }.getOrDefault(MemoryType.CONVERSATION_CONTEXT),
          isPinned = parts[5].toBooleanStrictOrNull() ?: false,
          updatedAtMillis = parts[6].toLongOrNull() ?: System.currentTimeMillis(),
        )
      }

  private fun String.sanitizeToken(): String = replace("\t", " ").replace("\n", " ")
}

package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.memory.MemoryCategory
import com.example.memory.MemoryEntry
import com.example.ui.theme.MaxMonoLabelStyle
import com.example.viewmodel.MaxDashboardData

@Composable
fun MemoryVaultScreen(
  data: MaxDashboardData,
  onSearchQueryChange: (String) -> Unit,
  onSelectCategory: (MemoryCategory?) -> Unit,
  onAddMemory: (String, String, MemoryCategory) -> Unit,
  onTogglePin: (String) -> Unit,
  onDeleteMemory: (String) -> Unit,
  onClearMemories: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var showAddDialog by remember { mutableStateOf(false) }

  Column(
    modifier = modifier.fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp).padding(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            text = "Memory & Context Vault",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
          )
          Text(
            text = "Tone state: ${data.emotionReading.primaryEmotion.label.uppercase()}",
            style = MaxMonoLabelStyle,
            color = MaterialTheme.colorScheme.secondary,
          )
        }

        Button(
          onClick = { showAddDialog = true },
          modifier = Modifier.minimumInteractiveComponentSize().testTag("add_memory_button"),
        ) {
          Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(stringResource(R.string.action_add_memory))
        }
      }

      OutlinedTextField(
        value = data.memorySearchQuery,
        onValueChange = onSearchQueryChange,
        placeholder = { Text("Recall key or context...") },
        leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = null) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().testTag("memory_search_input"),
      )

      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
          FilterChip(
            selected = data.selectedMemoryCategory == null,
            onClick = { onSelectCategory(null) },
            label = { Text("All (${data.memories.size})") },
          )
        }
        items(MemoryCategory.entries) { category ->
          FilterChip(
            selected = data.selectedMemoryCategory == category,
            onClick = {
              onSelectCategory(if (data.selectedMemoryCategory == category) null else category)
            },
            label = { Text(category.label) },
          )
        }
      }
    }

    if (data.filteredMemories.isEmpty()) {
      Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
      ) {
        Card(
          modifier = Modifier.widthIn(max = 460.dp),
          shape = RoundedCornerShape(24.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.Memory,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(44.dp),
            )
            Text(
              text = stringResource(R.string.empty_memory_title),
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = stringResource(R.string.empty_memory_subtitle),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
            )
          }
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        items(data.filteredMemories, key = { it.id }) { entry ->
          MemoryEntryCard(
            entry = entry,
            onTogglePin = { onTogglePin(entry.id) },
            onDelete = { onDeleteMemory(entry.id) },
            modifier = Modifier.widthIn(max = 720.dp),
          )
        }
        item {
          OutlinedButton(
            onClick = onClearMemories,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("clear_memories_button"),
          ) {
            Text(stringResource(R.string.action_clear_memories))
          }
        }
      }
    }
  }

  if (showAddDialog) {
    AddMemoryDialog(
      onDismiss = { showAddDialog = false },
      onSave = { key, content, category ->
        onAddMemory(key, content, category)
        showAddDialog = false
      },
    )
  }
}

@Composable
private fun MemoryEntryCard(
  entry: MemoryEntry,
  onTogglePin: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
          ) {
            Text(
              text = entry.category.label.uppercase(),
              style = MaxMonoLabelStyle,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            )
          }
          Text(
            text = entry.key,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
        Text(
          text = entry.content,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      IconButton(
        onClick = onTogglePin,
        modifier = Modifier.minimumInteractiveComponentSize(),
      ) {
        Icon(
          imageVector = if (entry.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
          contentDescription = if (entry.isPinned) "Unpin memory" else "Pin memory",
          tint = if (entry.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      IconButton(
        onClick = onDelete,
        modifier = Modifier.minimumInteractiveComponentSize(),
      ) {
        Icon(
          imageVector = Icons.Filled.DeleteOutline,
          contentDescription = "Delete memory",
          tint = MaterialTheme.colorScheme.error,
        )
      }
    }
  }
}

@Composable
private fun AddMemoryDialog(
  onDismiss: () -> Unit,
  onSave: (String, String, MemoryCategory) -> Unit,
) {
  var key by remember { mutableStateOf("") }
  var content by remember { mutableStateOf("") }
  var category by remember { mutableStateOf(MemoryCategory.CONTEXT) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Store Context Note") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
          value = key,
          onValueChange = { key = it },
          label = { Text("Key (e.g. Preferred Tone)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("memory_key_input"),
        )
        OutlinedTextField(
          value = content,
          onValueChange = { content = it },
          label = { Text("Content") },
          minLines = 2,
          modifier = Modifier.fillMaxWidth().testTag("memory_content_input"),
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(MemoryCategory.entries) { item ->
            FilterChip(
              selected = category == item,
              onClick = { category = item },
              label = { Text(item.label) },
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(key, content, category) },
        enabled = content.isNotBlank(),
        modifier = Modifier.testTag("confirm_save_memory_button"),
      ) {
        Text("Save")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    },
  )
}

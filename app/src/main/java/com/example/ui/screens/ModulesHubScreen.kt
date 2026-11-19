package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.components.ModuleStatusChip
import com.example.ui.components.ModuleSummaryCard
import com.example.ui.theme.MaxMonoBodyStyle
import com.example.ui.theme.MaxMonoLabelStyle
import com.example.utils.ModuleCategory
import com.example.utils.ModuleDescriptor
import com.example.viewmodel.MaxDashboardData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModulesHubScreen(
  data: MaxDashboardData,
  selectedModule: ModuleDescriptor?,
  onSelectModule: (ModuleDescriptor?) -> Unit,
  onSelectCategory: (ModuleCategory?) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp).padding(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(
        text = "Modular Architecture",
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onBackground,
      )
      Text(
        text = "All ${data.coreState.modules.size} MAX modular subsystems decoupled behind independent interfaces.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
          FilterChip(
            selected = data.selectedModuleCategory == null,
            onClick = { onSelectCategory(null) },
            label = { Text("All (${data.coreState.modules.size})") },
            modifier = Modifier.testTag("module_filter_all"),
          )
        }
        items(ModuleCategory.entries) { category ->
          FilterChip(
            selected = data.selectedModuleCategory == category,
            onClick = {
              onSelectCategory(if (data.selectedModuleCategory == category) null else category)
            },
            label = { Text(category.displayLabel()) },
            modifier = Modifier.testTag("module_filter_${category.name.lowercase()}"),
          )
        }
      }
    }

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      items(data.filteredModules, key = { it.id }) { module ->
        ModuleSummaryCard(
          module = module,
          onClick = { onSelectModule(module) },
          modifier = Modifier.widthIn(max = 720.dp).testTag("module_hub_card_${module.id}"),
        )
      }
    }
  }

  if (selectedModule != null) {
    ModalBottomSheet(onDismissRequest = { onSelectModule(null) }) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text(
          text = selectedModule.displayName,
          style = MaterialTheme.typography.headlineSmall,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = selectedModule.packageName,
          style = MaxMonoBodyStyle,
          color = MaterialTheme.colorScheme.primary,
        )
        ModuleStatusChip(status = selectedModule.status)
        Text(
          text = selectedModule.summary,
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text =
            if (selectedModule.privacyGuarded) {
              "PRIVACY GUARD: Strict foundation lock active. No hardware or user media accessed."
            } else {
              "CONTRACT STATUS: Interface wired into MAXCore lifecycle."
            },
          style = MaxMonoLabelStyle,
          color =
            if (selectedModule.privacyGuarded) {
              MaterialTheme.colorScheme.tertiary
            } else {
              MaterialTheme.colorScheme.secondary
            },
          modifier = Modifier.padding(bottom = 24.dp),
        )
      }
    }
  }
}

private fun ModuleCategory.displayLabel(): String =
  when (this) {
    ModuleCategory.CORE_ORCHESTRATION -> "Core"
    ModuleCategory.VOICE_AND_AUDIO -> "Voice"
    ModuleCategory.COGNITION_AND_MEMORY -> "Cognition"
    ModuleCategory.DEVICE_CAPABILITY -> "Capabilities"
    ModuleCategory.INFRASTRUCTURE -> "Infra"
  }

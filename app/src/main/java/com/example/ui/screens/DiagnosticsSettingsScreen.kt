package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.permissions.AssistantPermission
import com.example.permissions.PermissionState
import com.example.settings.ThemeMode
import com.example.ui.theme.MaxMonoBodyStyle
import com.example.ui.theme.MaxMonoLabelStyle
import com.example.update.UpdateCheckState
import com.example.utils.ErrorLogEntry
import com.example.utils.LogSeverity
import com.example.viewmodel.MaxDashboardData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DiagnosticsSettingsScreen(
  data: MaxDashboardData,
  onSetPrivacySafeMode: (Boolean) -> Unit,
  onSetWakeWordStandby: (Boolean) -> Unit,
  onSetTtsEnabled: (Boolean) -> Unit,
  onSetTtsRate: (Float) -> Unit,
  onSetMemoryEnabled: (Boolean) -> Unit,
  onSetDiagnosticsEnabled: (Boolean) -> Unit,
  onSetThemeMode: (ThemeMode) -> Unit,
  onSetDynamicColorEnabled: (Boolean) -> Unit,
  onSetFloatingBubbleEnabled: (Boolean) -> Unit,
  onRequestSystemOverlay: () -> Unit,
  onCheckForUpdates: () -> Unit,
  onResetSettings: () -> Unit,
  onSelectLogSeverity: (LogSeverity?) -> Unit,
  onEmitTestLog: () -> Unit,
  onClearLogs: () -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    item {
      Column(
        modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(
          text = "Settings, Permissions & Telemetry",
          style = MaterialTheme.typography.headlineSmall,
          color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
          text = "Managed by SettingsManager, PermissionManager, and ErrorLogger.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    item {
      Card(
        modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text(
            text = "Appearance & Core Controls",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )

          SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            ThemeMode.entries.forEachIndexed { index, mode ->
              SegmentedButton(
                selected = data.settings.themeMode == mode,
                onClick = { onSetThemeMode(mode) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = ThemeMode.entries.size),
              ) {
                Text(mode.name.lowercase().replaceFirstChar { it.uppercase() })
              }
            }
          }

          SettingToggleRow(
            title = "Privacy Safe Mode",
            subtitle = "Forces sensitive capture modules into Privacy Locked state.",
            checked = data.settings.privacySafeMode,
            onCheckedChange = onSetPrivacySafeMode,
            testTag = "switch_privacy_safe_mode",
          )

          SettingToggleRow(
            title = "Wake Word 'MAX' Standby",
            subtitle = "Arms software pipeline contract only (mic never opens).",
            checked = data.settings.wakeWordStandbyEnabled,
            enabled = !data.settings.privacySafeMode,
            onCheckedChange = onSetWakeWordStandby,
            testTag = "switch_wake_word_standby",
          )

          SettingToggleRow(
            title = "TTS Voice Engine",
            subtitle = "Allows Android TextToSpeech initialization.",
            checked = data.settings.ttsEnabled,
            onCheckedChange = onSetTtsEnabled,
            testTag = "switch_tts_enabled",
          )

          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text("TTS Speech Rate", style = MaterialTheme.typography.bodyMedium)
              Text(
                text = String.format(Locale.US, "%.2fx", data.settings.ttsSpeechRate),
                style = MaxMonoLabelStyle,
                color = MaterialTheme.colorScheme.primary,
              )
            }
            Slider(
              value = data.settings.ttsSpeechRate,
              onValueChange = onSetTtsRate,
              valueRange = 0.6f..1.6f,
              enabled = data.settings.ttsEnabled,
            )
          }

          SettingToggleRow(
            title = "Memory Manager Active",
            subtitle = "Permits structured context storage during session.",
            checked = data.settings.memoryEnabled,
            onCheckedChange = onSetMemoryEnabled,
            testTag = "switch_memory_enabled",
          )

          SettingToggleRow(
            title = "Material You Dynamic Color",
            subtitle = "Adapts palette to Android 12+ wallpaper (offline).",
            checked = data.settings.dynamicColorEnabled,
            onCheckedChange = onSetDynamicColorEnabled,
            testTag = "switch_dynamic_color",
          )

          SettingToggleRow(
            title = "Floating Assistant Bubble",
            subtitle = "Shows quick minimize/restore voice bubble inside MAX.",
            checked = data.settings.floatingBubbleEnabled,
            onCheckedChange = onSetFloatingBubbleEnabled,
            testTag = "switch_floating_bubble",
          )

          OutlinedButton(
            onClick = onRequestSystemOverlay,
            modifier =
              Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("enable_system_overlay_button"),
          ) {
            Text(
              if (data.floatingAssistantState.hasOverlayPermission) {
                "Launch System Floating Overlay"
              } else {
                "Enable System Overlay Permission"
              },
            )
          }

          SettingToggleRow(
            title = "ErrorLogger Telemetry",
            subtitle = "Captures structured subsystem diagnostics in memory.",
            checked = data.settings.diagnosticsLoggingEnabled,
            onCheckedChange = onSetDiagnosticsEnabled,
            testTag = "switch_diagnostics_enabled",
          )

          OutlinedButton(
            onClick = onResetSettings,
            modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("reset_settings_button"),
          ) {
            Text("Restore Privacy-First Defaults")
          }
        }
      }
    }

    item {
      Card(
        modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text(
            text = "Production Release & Update Check",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = data.currentVersionLabel,
            style = MaxMonoLabelStyle,
            color = MaterialTheme.colorScheme.primary,
          )
          val updateStatusText =
            when (val status = data.updateCheckState) {
              UpdateCheckState.Idle -> "Offline by default. Tap below to check for updates explicitly."
              UpdateCheckState.ConsentRequired -> "Explicit user consent is required before connecting."
              UpdateCheckState.Checking -> "Checking release metadata securely via HTTPS…"
              is UpdateCheckState.UpToDate -> "You are running the latest build (${status.currentVersionName})."
              is UpdateCheckState.UpdateAvailable -> "New release v${status.latestVersionName} available."
              is UpdateCheckState.Error -> status.friendlyMessage
            }
          Text(
            text = updateStatusText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Button(
            onClick = onCheckForUpdates,
            modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("check_updates_button"),
          ) {
            Text("Check for Updates (Consented)")
          }
        }
      }
    }

    item {
      Card(
        modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text(
            text = "PermissionManager Passive Audit",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
          AssistantPermission.entries.forEach { permission ->
            val state = data.permissionStates[permission] ?: PermissionState.PRIVACY_LOCKED
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = permission.displayName,
                  style = MaterialTheme.typography.titleSmall,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                  text = permission.description,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
              ) {
                Text(
                  text = state.name,
                  style = MaxMonoLabelStyle,
                  color = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
              }
            }
          }
        }
      }
    }

    item {
      Column(
        modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "ErrorLogger Console (${data.filteredLogs.size})",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onEmitTestLog, modifier = Modifier.minimumInteractiveComponentSize()) {
              Text(stringResource(R.string.action_log_diagnostic))
            }
            TextButton(onClick = onClearLogs, modifier = Modifier.minimumInteractiveComponentSize()) {
              Text(stringResource(R.string.action_clear_logs))
            }
          }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          item {
            FilterChip(
              selected = data.selectedLogSeverity == null,
              onClick = { onSelectLogSeverity(null) },
              label = { Text("All") },
              modifier = Modifier.minimumInteractiveComponentSize(),
            )
          }
          items(LogSeverity.entries) { severity ->
            FilterChip(
              selected = data.selectedLogSeverity == severity,
              onClick = {
                onSelectLogSeverity(if (data.selectedLogSeverity == severity) null else severity)
              },
              label = { Text(severity.name) },
              modifier = Modifier.minimumInteractiveComponentSize(),
            )
          }
        }
      }
    }

    if (data.filteredLogs.isEmpty()) {
      item {
        Card(
          modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Text(
              text = stringResource(R.string.empty_logs_title),
              style = MaterialTheme.typography.titleSmall,
            )
            Text(
              text = stringResource(R.string.empty_logs_subtitle),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    } else {
      items(data.filteredLogs, key = { it.id }) { entry ->
        LogEntryCard(entry = entry, modifier = Modifier.widthIn(max = 720.dp))
      }
    }
  }
}

@Composable
private fun SettingToggleRow(
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  enabled: Boolean = true,
  testTag: String,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      enabled = enabled,
      modifier = Modifier.testTag(testTag),
    )
  }
}

@Composable
private fun LogEntryCard(
  entry: ErrorLogEntry,
  modifier: Modifier = Modifier,
) {
  val formattedTime =
    androidx.compose.runtime.remember(entry.timestampMillis) {
      SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(entry.timestampMillis))
    }
  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = "[${entry.severity.name}] ${entry.tag}",
          style = MaxMonoLabelStyle,
          color = MaterialTheme.colorScheme.primary,
        )
        Text(
          text = formattedTime,
          style = MaxMonoLabelStyle,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Text(
        text = entry.message,
        style = MaxMonoBodyStyle,
        color = MaterialTheme.colorScheme.onSurface,
      )
      entry.throwableSummary?.let { summary ->
        Text(
          text = summary,
          style = MaxMonoLabelStyle,
          color = MaterialTheme.colorScheme.error,
        )
      }
    }
  }
}

package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.automation.RoutineType
import com.example.brain.AIBrainState
import com.example.ui.components.AnimatedMicCore
import com.example.ui.components.BottomAssistantSummaryCard
import com.example.ui.components.MaxTopGreetingHeader
import com.example.ui.components.QuickActionGridCard
import com.example.utils.ModuleDescriptor
import com.example.viewmodel.AssistantStatus
import com.example.viewmodel.MaxDashboardData

private data class HomeQuickAction(
  val id: String,
  val titleRes: Int,
  val subtitle: String,
  val icon: ImageVector,
)

private val QUICK_ACTIONS =
  listOf(
    HomeQuickAction("camera", R.string.quick_action_camera, "Privacy guarded", Icons.Filled.CameraAlt),
    HomeQuickAction("gallery", R.string.quick_action_gallery, "Photo Picker", Icons.Filled.PhotoLibrary),
    HomeQuickAction("browser", R.string.quick_action_browser, "Web search", Icons.Filled.Public),
    HomeQuickAction("phone", R.string.quick_action_phone, "Dialer handoff", Icons.Filled.Phone),
    HomeQuickAction("whatsapp", R.string.quick_action_whatsapp, "App launch", Icons.AutoMirrored.Filled.Chat),
    HomeQuickAction("notes", R.string.quick_action_notes, "Memory vault", Icons.Filled.EditNote),
    HomeQuickAction("reminder", R.string.quick_action_reminder, "Smart alerts", Icons.Filled.NotificationsActive),
    HomeQuickAction("settings", R.string.quick_action_settings, "System & logs", Icons.Filled.Settings),
  )

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CoreOverviewScreen(
  data: MaxDashboardData,
  onMicClick: () -> Unit,
  onSelectAssistantStatus: (AssistantStatus) -> Unit,
  onSubmitCommand: (String) -> Unit,
  onConfirmInternetConsent: (Boolean) -> Unit,
  onRunRoutine: (RoutineType) -> Unit,
  onTriggerDailyBriefing: () -> Unit,
  onScheduleTask: (String, String) -> Unit,
  onToggleScheduledTask: (String) -> Unit,
  onCameraAction: () -> Unit,
  onGalleryAction: () -> Unit,
  onBrowserAction: () -> Unit,
  onPhoneAction: () -> Unit,
  onWhatsAppAction: () -> Unit,
  onDismissConfirmation: () -> Unit,
  onRunDiagnostics: () -> Unit,
  onEnforcePrivacyLock: () -> Unit,
  onCheckTts: () -> Unit,
  onEmitTestLog: () -> Unit,
  onInspectModule: (ModuleDescriptor) -> Unit,
  onOpenMemoryVault: () -> Unit,
  onOpenSystemSettings: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  var contentVisible by remember { mutableStateOf(false) }
  var commandInput by remember { mutableStateOf("") }
  var quickTaskTitle by remember { mutableStateOf("") }

  LaunchedEffect(Unit) {
    contentVisible = true
  }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    verticalArrangement = Arrangement.spacedBy(20.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    // 1. Top Area: MAX Logo + Dynamic Bengali/English Greeting + Live Time & Date
    item {
      AnimatedVisibility(
        visible = contentVisible,
        enter = fadeIn(tween(260)) + slideInVertically(tween(260)) { -it / 6 },
      ) {
        MaxTopGreetingHeader(
          coreState = data.coreState,
          onShieldClick = onEnforcePrivacyLock,
          modifier = Modifier.widthIn(max = 760.dp),
        )
      }
    }

    // 2. Center Area: Large Circular Animated Microphone Core + Status Text + Command Input
    item {
      AnimatedVisibility(
        visible = contentVisible,
        enter = fadeIn(tween(300, delayMillis = 60)),
      ) {
        Column(
          modifier = Modifier.widthIn(max = 760.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          AnimatedMicCore(
            status = data.assistantStatus,
            onMicClick = onMicClick,
            onSelectStatus = onSelectAssistantStatus,
            modifier = Modifier.padding(vertical = 4.dp),
          )

          OutlinedTextField(
            value = commandInput,
            onValueChange = { commandInput = it },
            placeholder = { Text("MAX-কে বাংলা বা ইংরেজিতে নির্দেশ দিন…") },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions =
              KeyboardActions(
                onSend = {
                  if (commandInput.isNotBlank()) {
                    onSubmitCommand(commandInput)
                    commandInput = ""
                  }
                },
              ),
            trailingIcon = {
              IconButton(
                onClick = {
                  if (commandInput.isNotBlank()) {
                    onSubmitCommand(commandInput)
                    commandInput = ""
                  }
                },
                modifier = Modifier.minimumInteractiveComponentSize().testTag("send_command_button"),
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.Send,
                  contentDescription = "Send command to MAX",
                  tint = MaterialTheme.colorScheme.primary,
                )
              }
            },
            modifier = Modifier.fillMaxWidth().testTag("home_command_input"),
          )

          when (val brain = data.brainState) {
            is AIBrainState.AwaitingInternetConsent -> {
              ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                colors =
                  CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                  ),
                modifier = Modifier.fillMaxWidth().testTag("brain_internet_consent_card"),
              ) {
                Column(
                  modifier = Modifier.padding(16.dp),
                  verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  Text(
                    text = "Internet Permission Needed",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                  )
                  Text(
                    text = brain.promptText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                  )
                  Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                  ) {
                    Button(
                      onClick = { onConfirmInternetConsent(true) },
                      modifier =
                        Modifier.minimumInteractiveComponentSize().testTag("allow_internet_consent_button"),
                    ) {
                      Text("Allow Internet")
                    }
                    OutlinedButton(
                      onClick = { onConfirmInternetConsent(false) },
                      modifier =
                        Modifier.minimumInteractiveComponentSize().testTag("deny_internet_consent_button"),
                    ) {
                      Text("Stay Offline")
                    }
                  }
                }
              }
            }

            is AIBrainState.AwaitingClarification -> {
              ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                colors =
                  CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                  ),
                modifier = Modifier.fillMaxWidth().testTag("brain_clarification_card"),
              ) {
                Column(
                  modifier = Modifier.padding(16.dp),
                  verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Text(
                    text = "MAX Needs More Info",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                  )
                  Text(
                    text = brain.question,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                  )
                }
              }
            }

            is AIBrainState.Responded -> {
              val browserIntent = brain.browserIntent
              if (browserIntent != null) {
                ElevatedCard(
                  shape = RoundedCornerShape(20.dp),
                  colors =
                    CardDefaults.elevatedCardColors(
                      containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                  modifier = Modifier.fillMaxWidth().testTag("brain_browser_result_card"),
                ) {
                  Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                  ) {
                    Text(
                      text = brain.replyText,
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Button(
                      onClick = { runCatching { context.startActivity(browserIntent) } },
                      modifier =
                        Modifier.minimumInteractiveComponentSize().testTag("open_brain_browser_button"),
                    ) {
                      Text("Open Browser")
                    }
                  }
                }
              }
            }

            else -> Unit
          }
        }
      }
    }

    // 3. Quick Action Cards Grid (Camera, Gallery, Browser, Phone, WhatsApp, Notes, Reminder, Settings)
    item {
      Column(
        modifier = Modifier.fillMaxWidth().widthIn(max = 760.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text(
          text = "Quick Actions",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onBackground,
        )

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
          val columns = if (maxWidth >= 600.dp) 4 else 2
          val rows = QUICK_ACTIONS.chunked(columns)

          Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            rows.forEach { rowItems ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                rowItems.forEach { action ->
                  QuickActionGridCard(
                    title = stringResource(action.titleRes),
                    subtitle = action.subtitle,
                    icon = action.icon,
                    onClick = {
                      when (action.id) {
                        "camera" -> onCameraAction()
                        "gallery" -> onGalleryAction()
                        "browser" -> onBrowserAction()
                        "phone" -> onPhoneAction()
                        "whatsapp" -> onWhatsAppAction()
                        "notes" -> onOpenMemoryVault()
                        "reminder" -> {
                          onScheduleTask("দ্রুত রিমাইন্ডার (${data.reminders.size + 1})", "আজ সন্ধ্যা ৭:০০")
                          onSubmitCommand("রিমাইন্ডার দেখাও")
                        }
                        "settings" -> onOpenSystemSettings()
                      }
                    },
                    modifier = Modifier.weight(1f).testTag("quick_action_${action.id}"),
                  )
                }
                repeat(columns - rowItems.size) {
                  Spacer(modifier = Modifier.weight(1f))
                }
              }
            }
          }
        }
      }
    }

    // 4. Suggested Actions (Smart suggestions + Core actions)
    item {
      Column(
        modifier = Modifier.fillMaxWidth().widthIn(max = 760.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = "Suggested Actions",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onBackground,
        )

        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          AssistChip(
            onClick = onRunDiagnostics,
            label = { Text(stringResource(R.string.action_run_diagnostics)) },
            leadingIcon = {
              Icon(
                imageVector = Icons.Filled.VerifiedUser,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
            },
            modifier = Modifier.minimumInteractiveComponentSize().testTag("run_diagnostics_button"),
          )

          AssistChip(
            onClick = onCheckTts,
            label = { Text(stringResource(R.string.action_test_tts)) },
            leadingIcon = {
              Icon(
                imageVector = Icons.Filled.RecordVoiceOver,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
            },
            modifier = Modifier.minimumInteractiveComponentSize().testTag("check_tts_button"),
          )

          AssistChip(
            onClick = onEnforcePrivacyLock,
            label = { Text(stringResource(R.string.action_lock_privacy)) },
            leadingIcon = {
              Icon(
                imageVector = Icons.Filled.Shield,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
            },
            modifier = Modifier.minimumInteractiveComponentSize().testTag("enforce_privacy_lock_button"),
          )

          AssistChip(
            onClick = onEmitTestLog,
            label = { Text(stringResource(R.string.action_log_diagnostic)) },
            leadingIcon = {
              Icon(
                imageVector = Icons.Filled.BugReport,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
            },
            modifier = Modifier.minimumInteractiveComponentSize().testTag("emit_test_log_button"),
          )

          data.smartSuggestions.take(2).forEach { suggestion ->
            AssistChip(
              onClick = { onSubmitCommand(suggestion) },
              label = { Text(suggestion) },
              modifier = Modifier.minimumInteractiveComponentSize(),
            )
          }
        }
      }
    }

    // 5. Smart Automation, Routines & Daily Briefing (Offline-first)
    item {
      ElevatedCard(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().widthIn(max = 760.dp).testTag("smart_automation_card"),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text(
            text = "Smart Routines & Daily Briefing",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )

          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            FilledTonalButton(
              onClick = { onRunRoutine(RoutineType.MORNING) },
              modifier = Modifier.minimumInteractiveComponentSize().testTag("morning_routine_button"),
            ) {
              Icon(imageVector = Icons.Filled.WbSunny, contentDescription = null, modifier = Modifier.size(18.dp))
              Text(" Morning Routine", modifier = Modifier.padding(start = 6.dp))
            }

            FilledTonalButton(
              onClick = { onRunRoutine(RoutineType.NIGHT) },
              modifier = Modifier.minimumInteractiveComponentSize().testTag("night_routine_button"),
            ) {
              Icon(imageVector = Icons.Filled.NightlightRound, contentDescription = null, modifier = Modifier.size(18.dp))
              Text(" Night Routine", modifier = Modifier.padding(start = 6.dp))
            }

            OutlinedButton(
              onClick = onTriggerDailyBriefing,
              modifier = Modifier.minimumInteractiveComponentSize().testTag("daily_briefing_button"),
            ) {
              Icon(imageVector = Icons.Filled.TaskAlt, contentDescription = null, modifier = Modifier.size(18.dp))
              Text(" Daily Briefing", modifier = Modifier.padding(start = 6.dp))
            }
          }

          data.latestBriefing?.let { briefing ->
            Text(
              text = briefing.summaryBangla,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.testTag("latest_briefing_text"),
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            OutlinedTextField(
              value = quickTaskTitle,
              onValueChange = { quickTaskTitle = it },
              placeholder = { Text("নতুন নির্ধারিত কাজ যোগ করুন…") },
              singleLine = true,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.weight(1f).testTag("schedule_task_input"),
            )
            Button(
              onClick = {
                if (quickTaskTitle.isNotBlank()) {
                  onScheduleTask(quickTaskTitle, "আজ বিকাল ৫:০০")
                  quickTaskTitle = ""
                }
              },
              modifier = Modifier.minimumInteractiveComponentSize().testTag("schedule_task_button"),
            ) {
              Text("Add Task")
            }
          }

          if (data.scheduledTasks.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              data.scheduledTasks.take(3).forEach { task ->
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Checkbox(
                    checked = task.completed,
                    onCheckedChange = { onToggleScheduledTask(task.id) },
                    modifier = Modifier.minimumInteractiveComponentSize(),
                  )
                  Text(
                    text = "${task.title} • ${task.scheduledTimeLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                }
              }
            }
          }
        }
      }
    }

    // 6. Bottom Area: Recent Conversations + Last Completed Task
    item {
      BottomAssistantSummaryCard(
        recentConversations = data.transcript,
        lastCompletedTask = data.lastCompletedTask,
        lastCompletedTaskAtMillis = data.lastCompletedTaskAtMillis,
        modifier = Modifier.widthIn(max = 760.dp).padding(bottom = 8.dp),
      )
    }
  }

  val pending = data.pendingConfirmation
  if (pending != null) {
    AlertDialog(
      onDismissRequest = onDismissConfirmation,
      title = { Text(pending.title) },
      text = { Text(pending.description) },
      confirmButton = {
        Button(
          onClick = {
            val intent = pending.intent
            onDismissConfirmation()
            if (intent != null) {
              runCatching { context.startActivity(intent) }
            }
          },
          modifier = Modifier.testTag("confirm_external_action_button"),
        ) {
          Text(if (pending.intent != null) "Proceed" else "OK")
        }
      },
      dismissButton = {
        if (pending.intent != null) {
          TextButton(onClick = onDismissConfirmation) {
            Text("Cancel")
          }
        }
      },
    )
  }
}

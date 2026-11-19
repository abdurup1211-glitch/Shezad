package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.conversation.ConversationMessage
import com.example.core.MaxCoreState
import com.example.core.MaxLifecycleState
import com.example.ui.theme.LocalMaxAccentColors
import com.example.ui.theme.MaxMonoLabelStyle
import com.example.utils.ModuleDescriptor
import com.example.utils.ModuleStatus
import com.example.viewmodel.AssistantStatus
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MaxTopGreetingHeader(
  coreState: MaxCoreState,
  onShieldClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val accentColors = LocalMaxAccentColors.current
  var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

  // Battery-friendly minute-boundary clock ticker
  LaunchedEffect(Unit) {
    while (true) {
      currentTimeMillis = System.currentTimeMillis()
      val millisToNextMinute = 60_000L - (currentTimeMillis % 60_000L)
      delay(millisToNextMinute.coerceAtLeast(1_000L))
    }
  }

  val formattedTime =
    remember(currentTimeMillis) {
      SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(currentTimeMillis))
    }
  val formattedDate =
    remember(currentTimeMillis) {
      SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(Date(currentTimeMillis))
    }

  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(26.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
  ) {
    Box(modifier = Modifier.fillMaxWidth()) {
      androidx.compose.foundation.Image(
        painter = androidx.compose.ui.res.painterResource(id = R.drawable.img_max_core_backdrop),
        contentDescription = null,
        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
        alpha = 0.08f,
        modifier = Modifier.matchParentSize(),
      )
      Column(
        modifier = Modifier.fillMaxWidth().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Box(
            modifier =
              Modifier
                .size(46.dp)
                .shadow(elevation = 6.dp, shape = RoundedCornerShape(14.dp), clip = false)
                .clip(RoundedCornerShape(14.dp))
                .background(accentColors.primaryGradient),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Filled.AutoAwesome,
              contentDescription = "MAX Premium Logo",
              tint = Color.White,
              modifier = Modifier.size(24.dp),
            )
          }

          Column {
            Text(
              text = stringResource(R.string.app_name),
              style = MaterialTheme.typography.headlineSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = stringResource(R.string.max_core_subtitle),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        Surface(
          onClick = onShieldClick,
          shape = RoundedCornerShape(999.dp),
          color =
            if (coreState.privacyShieldActive) {
              MaterialTheme.colorScheme.tertiaryContainer
            } else {
              MaterialTheme.colorScheme.primaryContainer
            },
          modifier = Modifier.minimumInteractiveComponentSize().testTag("top_privacy_status_pill"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.Lock,
              contentDescription = null,
              tint =
                if (coreState.privacyShieldActive) {
                  MaterialTheme.colorScheme.onTertiaryContainer
                } else {
                  MaterialTheme.colorScheme.onPrimaryContainer
                },
              modifier = Modifier.size(14.dp),
            )
            Text(
              text =
                when (coreState.lifecycle) {
                  MaxLifecycleState.PRIVACY_LOCKED -> "SHIELD ON"
                  MaxLifecycleState.READY -> "CORE READY"
                  MaxLifecycleState.INITIALIZING -> "BOOTING"
                  MaxLifecycleState.ERROR -> "FAULT"
                  MaxLifecycleState.UNINITIALIZED -> "STANDBY"
                },
              style = MaxMonoLabelStyle,
              color =
                if (coreState.privacyShieldActive) {
                  MaterialTheme.colorScheme.onTertiaryContainer
                } else {
                  MaterialTheme.colorScheme.onPrimaryContainer
                },
            )
          }
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = stringResource(R.string.greeting_bengali),
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = stringResource(R.string.greeting_english),
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Medium,
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        ) {
          Text(
            text = formattedDate,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
          )
        }

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
        ) {
          Text(
            text = formattedTime,
            style = MaxMonoLabelStyle,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
          )
        }
      }
      }
    }
  }
}

@Composable
fun AnimatedMicCore(
  status: AssistantStatus,
  onMicClick: () -> Unit,
  onSelectStatus: (AssistantStatus) -> Unit,
  modifier: Modifier = Modifier,
) {
  val accentColors = LocalMaxAccentColors.current
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  val pressScale by
    animateFloatAsState(
      targetValue = if (isPressed) 0.94f else 1f,
      animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
      label = "mic_press_scale",
    )

  val infiniteTransition = rememberInfiniteTransition(label = "mic_core_animation")

  val floatingOffsetDp by
    infiniteTransition.animateFloat(
      initialValue = -4f,
      targetValue = 4f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "floating_offset",
    )

  val waveProgress by
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = 1f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 1800, easing = LinearEasing),
          repeatMode = RepeatMode.Restart,
        ),
      label = "wave_progress",
    )

  val thinkingRotation by
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = 360f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 2200, easing = LinearEasing),
          repeatMode = RepeatMode.Restart,
        ),
      label = "thinking_arc",
    )

  val statusLabel =
    when (status) {
      AssistantStatus.WAITING -> stringResource(R.string.status_waiting)
      AssistantStatus.LISTENING -> stringResource(R.string.status_listening)
      AssistantStatus.THINKING -> stringResource(R.string.status_thinking)
      AssistantStatus.SPEAKING -> stringResource(R.string.status_speaking)
    }

  val statusTint by
    animateColorAsState(
      targetValue =
        when (status) {
          AssistantStatus.WAITING -> MaterialTheme.colorScheme.onSurfaceVariant
          AssistantStatus.LISTENING -> accentColors.gradientStart
          AssistantStatus.THINKING -> accentColors.gradientEnd
          AssistantStatus.SPEAKING -> MaterialTheme.colorScheme.secondary
        },
      animationSpec = tween(260),
      label = "status_tint",
    )

  val micDescription = stringResource(R.string.mic_button_description)

  Column(
    modifier = modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Box(
      modifier =
        Modifier
          .size(184.dp)
          .graphicsLayer {
            translationY = floatingOffsetDp.dp.toPx()
          },
      contentAlignment = Alignment.Center,
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val centerRadius = size.minDimension * 0.28f

        when (status) {
          AssistantStatus.WAITING -> {
            drawCircle(
              color = accentColors.softGlow.copy(alpha = 0.14f),
              radius = centerRadius * 1.32f,
            )
          }

          AssistantStatus.LISTENING -> {
            val glowAlpha = 0.34f * (1f - waveProgress)
            val glowRadius = centerRadius * (1.15f + waveProgress * 0.65f)
            drawCircle(
              brush =
                Brush.radialGradient(
                  colors =
                    listOf(
                      accentColors.gradientStart.copy(alpha = 0.38f),
                      accentColors.gradientEnd.copy(alpha = glowAlpha),
                      Color.Transparent,
                    ),
                ),
              radius = glowRadius,
            )
            drawCircle(
              color = accentColors.gradientStart.copy(alpha = 0.55f * (1f - waveProgress)),
              radius = centerRadius * (1.08f + waveProgress * 0.52f),
              style = Stroke(width = 2.5.dp.toPx()),
            )
          }

          AssistantStatus.THINKING -> {
            drawArc(
              brush = accentColors.primaryGradient,
              startAngle = thinkingRotation,
              sweepAngle = 140f,
              useCenter = false,
              style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
            )
            drawCircle(
              color = accentColors.gradientEnd.copy(alpha = 0.18f),
              radius = centerRadius * 1.36f,
            )
          }

          AssistantStatus.SPEAKING -> {
            val secondWave = (waveProgress + 0.5f) % 1f
            drawCircle(
              color = accentColors.gradientEnd.copy(alpha = 0.45f * (1f - waveProgress)),
              radius = centerRadius * (1.06f + waveProgress * 0.60f),
              style = Stroke(width = 3.dp.toPx()),
            )
            drawCircle(
              color = accentColors.gradientStart.copy(alpha = 0.38f * (1f - secondWave)),
              radius = centerRadius * (1.06f + secondWave * 0.60f),
              style = Stroke(width = 2.5.dp.toPx()),
            )
          }
        }
      }

      Surface(
        modifier =
          Modifier
            .size(104.dp)
            .graphicsLayer {
              scaleX = pressScale
              scaleY = pressScale
            }
            .shadow(elevation = 14.dp, shape = CircleShape, clip = false)
            .clip(CircleShape)
            .clickable(
              interactionSource = interactionSource,
              indication = null,
              role = Role.Button,
              onClick = onMicClick,
            )
            .semantics {
              contentDescription = micDescription
              stateDescription = statusLabel
            }
            .testTag("max_mic_orb_button"),
        shape = CircleShape,
        color = Color.Transparent,
      ) {
        Box(
          modifier = Modifier.fillMaxSize().background(accentColors.primaryGradient),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector =
              when (status) {
                AssistantStatus.SPEAKING -> Icons.Filled.GraphicEq
                AssistantStatus.THINKING -> Icons.Filled.AutoAwesome
                else -> Icons.Filled.Mic
              },
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(42.dp),
          )
        }
      }
    }

    AnimatedContent(
      targetState = statusLabel,
      transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) },
      label = "mic_status_text",
    ) { label ->
      Text(
        text = label,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = statusTint,
        modifier = Modifier.testTag("max_mic_status_text"),
      )
    }

    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      AssistantStatus.entries.forEach { stateOption ->
        val selected = status == stateOption
        Surface(
          onClick = { onSelectStatus(stateOption) },
          shape = RoundedCornerShape(999.dp),
          color =
            if (selected) {
              MaterialTheme.colorScheme.primaryContainer
            } else {
              MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            },
          border =
            if (selected) {
              BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            } else {
              null
            },
          modifier = Modifier.minimumInteractiveComponentSize().testTag("status_chip_${stateOption.name.lowercase()}"),
        ) {
          Text(
            text = stateOption.name.lowercase().replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.labelSmall,
            color =
              if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
              } else {
                MaterialTheme.colorScheme.onSurfaceVariant
              },
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          )
        }
      }
    }
  }
}

@Composable
fun QuickActionGridCard(
  title: String,
  subtitle: String,
  icon: ImageVector,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val accentColors = LocalMaxAccentColors.current
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val scale by
    animateFloatAsState(
      targetValue = if (isPressed) 0.96f else 1f,
      animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
      label = "quick_action_press",
    )

  Surface(
    onClick = onClick,
    interactionSource = interactionSource,
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp,
    shadowElevation = 2.dp,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.32f)),
    modifier =
      modifier
        .fillMaxWidth()
        .minimumInteractiveComponentSize()
        .graphicsLayer {
          scaleX = scale
          scaleY = scale
        },
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Box(
        modifier =
          Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(
              Brush.linearGradient(
                colors =
                  listOf(
                    accentColors.gradientStart.copy(alpha = 0.18f),
                    accentColors.gradientEnd.copy(alpha = 0.18f),
                  ),
              ),
            ),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = title,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(22.dp),
        )
      }

      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
fun BottomAssistantSummaryCard(
  recentConversations: List<ConversationMessage>,
  lastCompletedTask: String,
  lastCompletedTaskAtMillis: Long,
  modifier: Modifier = Modifier,
) {
  val formattedTaskTime =
    remember(lastCompletedTaskAtMillis) {
      SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(lastCompletedTaskAtMillis))
    }

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.Forum,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
          )
          Text(
            text = "Recent Conversations",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }

        if (recentConversations.isEmpty()) {
          Text(
            text = "MAX conversation pipeline is in standby. Stored sessions will appear here.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        } else {
          recentConversations.takeLast(3).reversed().forEach { message ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              verticalAlignment = Alignment.Top,
            ) {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
              ) {
                Text(
                  text = message.role.name,
                  style = MaxMonoLabelStyle,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                )
              }
              Text(
                text = message.text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
              )
            }
          }
        }
      }
    }

    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(20.dp),
      colors =
        CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
        ),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Icon(
          imageVector = Icons.Filled.CheckCircle,
          contentDescription = "Last completed task",
          tint = MaterialTheme.colorScheme.secondary,
          modifier = Modifier.size(22.dp),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
            text = "Last Completed Task",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
          )
          Text(
            text = lastCompletedTask,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
        Text(
          text = formattedTaskTime,
          style = MaxMonoLabelStyle,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
fun ModuleStatusChip(
  status: ModuleStatus,
  modifier: Modifier = Modifier,
) {
  val (containerColor, contentColor, label) =
    when (status) {
      ModuleStatus.READY ->
        Triple(
          MaterialTheme.colorScheme.secondaryContainer,
          MaterialTheme.colorScheme.onSecondaryContainer,
          "READY",
        )
      ModuleStatus.STANDBY ->
        Triple(
          MaterialTheme.colorScheme.primaryContainer,
          MaterialTheme.colorScheme.onPrimaryContainer,
          "STANDBY",
        )
      ModuleStatus.PRIVACY_LOCKED ->
        Triple(
          MaterialTheme.colorScheme.tertiaryContainer,
          MaterialTheme.colorScheme.onTertiaryContainer,
          "LOCKED",
        )
      ModuleStatus.DISABLED ->
        Triple(
          MaterialTheme.colorScheme.surfaceVariant,
          MaterialTheme.colorScheme.onSurfaceVariant,
          "DISABLED",
        )
      ModuleStatus.ERROR ->
        Triple(
          MaterialTheme.colorScheme.errorContainer,
          MaterialTheme.colorScheme.onErrorContainer,
          "ERROR",
        )
    }

  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(999.dp),
    color = containerColor,
  ) {
    Text(
      text = label,
      style = MaxMonoLabelStyle,
      color = contentColor,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
    )
  }
}

@Composable
fun ModuleSummaryCard(
  module: ModuleDescriptor,
  onClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier =
      modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .then(
          if (onClick != null) {
            Modifier.clickable(onClick = onClick)
          } else {
            Modifier
          },
        ),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.size(42.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = iconForModule(module.id),
            contentDescription = module.displayName,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
          )
        }
      }

      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = module.displayName,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = module.packageName,
          style = MaxMonoLabelStyle,
          color = MaterialTheme.colorScheme.primary,
        )
        Text(
          text = module.summary,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }

      ModuleStatusChip(status = module.status)
    }
  }
}

fun iconForModule(moduleId: String): ImageVector =
  when (moduleId) {
    "max_core" -> Icons.Filled.Hub
    "settings_manager" -> Icons.Filled.Settings
    "permission_manager" -> Icons.Filled.Security
    "wake_word_manager" -> Icons.Filled.GraphicEq
    "speech_manager" -> Icons.Filled.Mic
    "tts_manager" -> Icons.Filled.RecordVoiceOver
    "memory_manager" -> Icons.Filled.Memory
    "conversation_engine", "conversation_manager", "ai_brain_manager" -> Icons.Filled.SmartToy
    "emotion_analyzer", "emotion_manager" -> Icons.Filled.SentimentSatisfied
    "automation_manager", "smart_feature_manager", "floating_assistant_controller" -> Icons.Filled.AutoAwesome
    "assistant_service", "app_update_checker" -> Icons.Filled.VerifiedUser
    "camera_module", "camera_manager" -> Icons.Filled.CameraAlt
    "gallery_module", "gallery_manager" -> Icons.Filled.PhotoLibrary
    "internet_module", "internet_manager" -> Icons.Filled.Public
    "phone_module", "phone_manager" -> Icons.Filled.Phone
    "apps_module", "app_manager" -> Icons.Filled.Apps
    "files_module", "file_manager" -> Icons.Filled.Folder
    else -> Icons.Filled.BugReport
  }

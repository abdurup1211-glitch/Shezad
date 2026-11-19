package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.overlay.FloatingAssistantState
import com.example.overlay.FloatingBubbleMode
import com.example.ui.theme.LocalMaxAccentColors
import kotlin.math.roundToInt

/**
 * Interactive, battery-friendly Floating Assistant Bubble rendered safely without
 * demanding intrusive background permissions, plus optional handoff to Android 8+ overlay.
 */
@Composable
fun FloatingBubbleOverlay(
  enabled: Boolean,
  state: FloatingAssistantState,
  onToggleExpandMinimize: () -> Unit,
  onQuickVoice: () -> Unit,
  onDailyBriefing: () -> Unit,
  onRequestSystemOverlay: () -> Unit,
  modifier: Modifier = Modifier,
) {
  if (!enabled || state.mode == FloatingBubbleMode.HIDDEN) return

  val accentColors = LocalMaxAccentColors.current
  var dragOffsetX by remember { mutableFloatStateOf(0f) }
  var dragOffsetY by remember { mutableFloatStateOf(0f) }

  Box(
    modifier =
      modifier
        .offset { IntOffset(dragOffsetX.roundToInt(), dragOffsetY.roundToInt()) }
        .pointerInput(state.mode) {
          detectDragGestures { change, dragAmount ->
            change.consume()
            dragOffsetX = (dragOffsetX + dragAmount.x).coerceIn(-600f, 120f)
            dragOffsetY = (dragOffsetY + dragAmount.y).coerceIn(-900f, 120f)
          }
        },
  ) {
    AnimatedContent(
      targetState = state.mode == FloatingBubbleMode.IN_APP_EXPANDED,
      transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.9f)) togetherWith (fadeOut() + scaleOut(targetScale = 0.9f)) },
      label = "floating_bubble_transition",
    ) { expanded ->
      if (!expanded) {
        Surface(
          onClick = onToggleExpandMinimize,
          shape = CircleShape,
          color = Color.Transparent,
          shadowElevation = 8.dp,
          modifier =
            Modifier
              .size(56.dp)
              .shadow(8.dp, CircleShape, clip = false)
              .testTag("floating_bubble_orb"),
        ) {
          Box(
            modifier = Modifier.background(accentColors.primaryGradient),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Filled.AutoAwesome,
              contentDescription = "Open MAX floating assistant bubble",
              tint = Color.White,
              modifier = Modifier.size(26.dp),
            )
          }
        }
      } else {
        ElevatedCard(
          shape = RoundedCornerShape(24.dp),
          colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.elevatedCardElevation(defaultElevation = 10.dp),
          modifier = Modifier.widthIn(min = 240.dp, max = 300.dp).testTag("floating_bubble_panel"),
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
              ) {
                Surface(
                  shape = CircleShape,
                  color = MaterialTheme.colorScheme.primaryContainer,
                  modifier = Modifier.size(30.dp),
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = Icons.Filled.AutoAwesome,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.onPrimaryContainer,
                      modifier = Modifier.size(16.dp),
                    )
                  }
                }
                Text(
                  text = "MAX Quick Bubble",
                  style = MaterialTheme.typography.titleSmall,
                  color = MaterialTheme.colorScheme.onSurface,
                )
              }

              IconButton(
                onClick = onToggleExpandMinimize,
                modifier = Modifier.minimumInteractiveComponentSize().testTag("floating_bubble_minimize"),
              ) {
                Icon(
                  imageVector = Icons.Filled.CloseFullscreen,
                  contentDescription = "Minimize floating bubble",
                )
              }
            }

            Button(
              onClick = {
                onQuickVoice()
                onToggleExpandMinimize()
              },
              modifier = Modifier.minimumInteractiveComponentSize().testTag("floating_quick_voice_button"),
            ) {
              Icon(imageVector = Icons.Filled.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
              Text(text = " Quick Voice", modifier = Modifier.padding(start = 6.dp))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              FilledTonalButton(
                onClick = {
                  onDailyBriefing()
                  onToggleExpandMinimize()
                },
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("floating_briefing_button"),
              ) {
                Icon(imageVector = Icons.Filled.WbSunny, contentDescription = null, modifier = Modifier.size(16.dp))
                Text(
                  text = " Briefing",
                  style = MaterialTheme.typography.labelMedium,
                  maxLines = 1,
                  softWrap = false,
                )
              }

              OutlinedButton(
                onClick = onRequestSystemOverlay,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("floating_overlay_button"),
              ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                Text(
                  text = " Overlay",
                  style = MaterialTheme.typography.labelMedium,
                  maxLines = 1,
                  softWrap = false,
                )
              }
            }
          }
        }
      }
    }
  }
}

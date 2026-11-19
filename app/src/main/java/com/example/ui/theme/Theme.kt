package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.settings.ThemeMode

@Immutable
data class MaxAccentColors(
  val gradientStart: Color,
  val gradientEnd: Color,
  val softGlow: Color,
) {
  val primaryGradient: Brush
    get() = Brush.linearGradient(colors = listOf(gradientStart, gradientEnd))
}

val LocalMaxAccentColors =
  staticCompositionLocalOf {
    MaxAccentColors(
      gradientStart = GradientBlueDark,
      gradientEnd = GradientPurpleDark,
      softGlow = ElectricBlue.copy(alpha = 0.28f),
    )
  }

private val MaxDarkColorScheme =
  darkColorScheme(
    primary = ElectricBlue,
    onPrimary = OnElectricBlue,
    primaryContainer = BlueContainer,
    onPrimaryContainer = OnBlueContainer,
    secondary = RoyalPurple,
    onSecondary = OnRoyalPurple,
    secondaryContainer = PurpleContainer,
    onSecondaryContainer = OnPurpleContainer,
    tertiary = AmberShield,
    onTertiary = OnAmberShield,
    tertiaryContainer = AmberContainer,
    onTertiaryContainer = OnAmberContainer,
    error = CoralError,
    onError = OnCoralError,
    errorContainer = CoralErrorContainer,
    onErrorContainer = OnCoralErrorContainer,
    background = ObsidianBackground,
    onBackground = InkLight,
    surface = ObsidianSurface,
    onSurface = InkLight,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = InkLightMuted,
    surfaceContainerHigh = ObsidianSurfaceHighest,
    outline = ObsidianOutline,
  )

private val MaxLightColorScheme =
  lightColorScheme(
    primary = AzurePrimary,
    onPrimary = OnAzurePrimary,
    primaryContainer = AzureContainer,
    onPrimaryContainer = OnAzureContainer,
    secondary = VioletSecondary,
    onSecondary = OnVioletSecondary,
    secondaryContainer = VioletContainer,
    onSecondaryContainer = OnVioletContainer,
    tertiary = OchreTertiary,
    onTertiary = OnOchreTertiary,
    tertiaryContainer = OchreContainer,
    onTertiaryContainer = OnOchreContainer,
    background = PorcelainBackground,
    onBackground = InkDark,
    surface = PorcelainSurface,
    onSurface = InkDark,
    surfaceVariant = PorcelainSurfaceVariant,
    onSurfaceVariant = InkDarkMuted,
    surfaceContainerHigh = PorcelainSurfaceHighest,
    outline = PorcelainOutline,
  )

private val MaxShapes =
  Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp),
  )

@Composable
fun MaxTheme(
  themeMode: ThemeMode = ThemeMode.SYSTEM,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val useDarkTheme =
    when (themeMode) {
      ThemeMode.DARK -> true
      ThemeMode.LIGHT -> false
      ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      useDarkTheme -> MaxDarkColorScheme
      else -> MaxLightColorScheme
    }

  val accentColors =
    if (useDarkTheme) {
      MaxAccentColors(
        gradientStart = GradientBlueDark,
        gradientEnd = GradientPurpleDark,
        softGlow = ElectricBlue.copy(alpha = 0.28f),
      )
    } else {
      MaxAccentColors(
        gradientStart = GradientBlueLight,
        gradientEnd = GradientPurpleLight,
        softGlow = AzurePrimary.copy(alpha = 0.22f),
      )
    }

  CompositionLocalProvider(LocalMaxAccentColors provides accentColors) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = MaxTypography,
      shapes = MaxShapes,
      content = content,
    )
  }
}

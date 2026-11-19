package com.example.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.R

const val ROUTE_CORE = "core_overview"
const val ROUTE_MODULES = "modules_hub"
const val ROUTE_MEMORY = "memory_vault"
const val ROUTE_SYSTEM = "diagnostics_settings"

/**
 * Top-level adaptive navigation destinations for MAX.
 */
enum class MaxDestination(
  val route: String,
  @StringRes val labelRes: Int,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String,
) {
  CORE(
    route = ROUTE_CORE,
    labelRes = R.string.nav_core,
    selectedIcon = Icons.Filled.Hub,
    unselectedIcon = Icons.Outlined.Hub,
    testTag = "nav_tab_core",
  ),
  MODULES(
    route = ROUTE_MODULES,
    labelRes = R.string.nav_modules,
    selectedIcon = Icons.Filled.Extension,
    unselectedIcon = Icons.Outlined.Extension,
    testTag = "nav_tab_modules",
  ),
  MEMORY(
    route = ROUTE_MEMORY,
    labelRes = R.string.nav_memory,
    selectedIcon = Icons.Filled.Psychology,
    unselectedIcon = Icons.Outlined.Psychology,
    testTag = "nav_tab_memory",
  ),
  SYSTEM(
    route = ROUTE_SYSTEM,
    labelRes = R.string.nav_settings,
    selectedIcon = Icons.Filled.Tune,
    unselectedIcon = Icons.Outlined.Tune,
    testTag = "nav_tab_system",
  ),
}

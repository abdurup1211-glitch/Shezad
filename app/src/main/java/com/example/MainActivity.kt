package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.overlay.FloatingOverlayService
import com.example.settings.ThemeMode
import com.example.ui.components.FloatingBubbleOverlay
import com.example.ui.navigation.MaxDestination
import com.example.ui.navigation.ROUTE_CORE
import com.example.ui.navigation.ROUTE_MEMORY
import com.example.ui.navigation.ROUTE_MODULES
import com.example.ui.navigation.ROUTE_SYSTEM
import com.example.ui.screens.CoreOverviewScreen
import com.example.ui.screens.DiagnosticsSettingsScreen
import com.example.ui.screens.MemoryVaultScreen
import com.example.ui.screens.ModulesHubScreen
import com.example.ui.theme.MaxTheme
import com.example.utils.ModuleDescriptor
import com.example.viewmodel.AssistantStatus
import com.example.viewmodel.MaxScreenState
import com.example.viewmodel.MaxViewModel

class MainActivity : ComponentActivity() {

  private val viewModel: MaxViewModel by viewModels {
    val app = application as MaxApplication
    MaxViewModel.Factory(app.container.systemRepository)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    handleQuickVoiceIntent(intent)
    setContent {
      val uiState by viewModel.uiState.collectAsStateWithLifecycle()
      val readySettings = (uiState as? MaxScreenState.Ready)?.dashboard?.settings
      val themeMode = readySettings?.themeMode ?: ThemeMode.SYSTEM
      val dynamicColor = readySettings?.dynamicColorEnabled ?: true

      MaxTheme(themeMode = themeMode, dynamicColor = dynamicColor) {
        MaxAppRoot(
          uiState = uiState,
          viewModel = viewModel,
        )
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    handleQuickVoiceIntent(intent)
  }

  private fun handleQuickVoiceIntent(intent: Intent?) {
    if (intent?.getBooleanExtra(FloatingOverlayService.EXTRA_QUICK_VOICE, false) == true) {
      viewModel.setAssistantStatus(AssistantStatus.LISTENING)
    }
  }

  override fun onStop() {
    super.onStop()
    (application as? MaxApplication)?.let { app ->
      app.container.maxCore.releaseTransientResources()
    }
  }
}

@Composable
fun MaxAppRoot(
  uiState: MaxScreenState,
  viewModel: MaxViewModel,
) {
  val navController = rememberNavController()
  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = backStackEntry?.destination?.route ?: ROUTE_CORE
  val snackbarHostState = remember { SnackbarHostState() }
  var inspectedModule by remember { mutableStateOf<ModuleDescriptor?>(null) }

  val readyState = uiState as? MaxScreenState.Ready
  LaunchedEffect(readyState?.dashboard?.feedbackBanner) {
    val message = readyState?.dashboard?.feedbackBanner
    if (!message.isNullOrBlank()) {
      snackbarHostState.showSnackbar(message)
      viewModel.dismissFeedback()
    }
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useNavigationRail = maxWidth >= 840.dp

    Scaffold(
      modifier = Modifier.fillMaxSize(),
      contentWindowInsets = WindowInsets.safeDrawing,
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      bottomBar = {
        if (!useNavigationRail) {
          NavigationBar(windowInsets = WindowInsets.navigationBars) {
            MaxDestination.entries.forEach { destination ->
              val selected = currentRoute == destination.route
              NavigationBarItem(
                selected = selected,
                onClick = {
                  navController.navigate(destination.route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                      saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                  }
                },
                icon = {
                  Icon(
                    imageVector =
                      if (selected) destination.selectedIcon else destination.unselectedIcon,
                    contentDescription = stringResource(destination.labelRes),
                  )
                },
                label = { Text(stringResource(destination.labelRes)) },
                modifier = Modifier.testTag(destination.testTag),
              )
            }
          }
        }
      },
    ) { innerPadding ->
      Row(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
      ) {
        if (useNavigationRail) {
          NavigationRail(modifier = Modifier.fillMaxHeight()) {
            MaxDestination.entries.forEach { destination ->
              val selected = currentRoute == destination.route
              NavigationRailItem(
                selected = selected,
                onClick = {
                  navController.navigate(destination.route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                      saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                  }
                },
                icon = {
                  Icon(
                    imageVector =
                      if (selected) destination.selectedIcon else destination.unselectedIcon,
                    contentDescription = stringResource(destination.labelRes),
                  )
                },
                label = { Text(stringResource(destination.labelRes)) },
                modifier = Modifier.testTag(destination.testTag),
              )
            }
          }
        }

        when (uiState) {
          MaxScreenState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              CircularProgressIndicator()
            }
          }

          is MaxScreenState.Error -> {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
              Text(
                text = uiState.message,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error,
              )
            }
          }

          is MaxScreenState.Ready -> {
            val data = uiState.dashboard
            val photoPickerLauncher =
              androidx.activity.compose.rememberLauncherForActivityResult(
                contract = androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia(),
              ) { uris ->
                if (uris.isNotEmpty()) {
                  viewModel.submitCommand("গ্যালারি থেকে ${uris.size}টি ছবি নির্বাচিত হয়েছে")
                }
              }

            NavHost(
              navController = navController,
              startDestination = ROUTE_CORE,
              enterTransition = { fadeIn(animationSpec = tween(220)) },
              exitTransition = { fadeOut(animationSpec = tween(180)) },
              modifier = Modifier.fillMaxSize(),
            ) {
              composable(ROUTE_CORE) {
                CoreOverviewScreen(
                  data = data,
                  onMicClick = viewModel::cycleAssistantStatus,
                  onSelectAssistantStatus = viewModel::setAssistantStatus,
                  onSubmitCommand = viewModel::submitCommand,
                  onConfirmInternetConsent = viewModel::confirmBrainInternetConsent,
                  onRunRoutine = viewModel::runRoutine,
                  onTriggerDailyBriefing = viewModel::triggerDailyBriefing,
                  onScheduleTask = viewModel::scheduleTask,
                  onToggleScheduledTask = viewModel::toggleScheduledTask,
                  onCameraAction = viewModel::requestCameraConfirmation,
                  onGalleryAction = {
                    photoPickerLauncher.launch(
                      androidx.activity.result.PickVisualMediaRequest(
                        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly,
                      ),
                    )
                  },
                  onBrowserAction = { viewModel.requestBrowserConfirmation("Bangladesh") },
                  onPhoneAction = viewModel::requestPhoneDialerConfirmation,
                  onWhatsAppAction = {
                    viewModel.requestExternalAppConfirmation(com.example.apps.SupportedExternalApp.WHATSAPP)
                  },
                  onDismissConfirmation = viewModel::dismissConfirmation,
                  onRunDiagnostics = viewModel::runDiagnostics,
                  onEnforcePrivacyLock = viewModel::enforcePrivacyLock,
                  onCheckTts = viewModel::checkTtsReadiness,
                  onEmitTestLog = viewModel::emitTestDiagnostic,
                  onInspectModule = { module ->
                    inspectedModule = module
                    navController.navigate(ROUTE_MODULES) {
                      launchSingleTop = true
                    }
                  },
                  onOpenMemoryVault = {
                    navController.navigate(ROUTE_MEMORY) {
                      popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                      }
                      launchSingleTop = true
                      restoreState = true
                    }
                  },
                  onOpenSystemSettings = {
                    navController.navigate(ROUTE_SYSTEM) {
                      popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                      }
                      launchSingleTop = true
                      restoreState = true
                    }
                  },
                )
              }

              composable(ROUTE_MODULES) {
                ModulesHubScreen(
                  data = data,
                  selectedModule = inspectedModule,
                  onSelectModule = { inspectedModule = it },
                  onSelectCategory = viewModel::setModuleCategoryFilter,
                )
              }

              composable(ROUTE_MEMORY) {
                MemoryVaultScreen(
                  data = data,
                  onSearchQueryChange = viewModel::setMemorySearchQuery,
                  onSelectCategory = viewModel::setMemoryCategoryFilter,
                  onAddMemory = viewModel::addMemory,
                  onTogglePin = viewModel::toggleMemoryPin,
                  onDeleteMemory = viewModel::deleteMemory,
                  onClearMemories = viewModel::clearMemories,
                )
              }

              composable(ROUTE_SYSTEM) {
                DiagnosticsSettingsScreen(
                  data = data,
                  onSetPrivacySafeMode = viewModel::setPrivacySafeMode,
                  onSetWakeWordStandby = viewModel::setWakeWordStandbyEnabled,
                  onSetTtsEnabled = viewModel::setTtsEnabled,
                  onSetTtsRate = viewModel::setTtsSpeechRate,
                  onSetMemoryEnabled = viewModel::setMemoryEnabled,
                  onSetDiagnosticsEnabled = viewModel::setDiagnosticsLoggingEnabled,
                  onSetThemeMode = viewModel::setThemeMode,
                  onSetDynamicColorEnabled = viewModel::setDynamicColorEnabled,
                  onSetFloatingBubbleEnabled = viewModel::setFloatingBubbleEnabled,
                  onRequestSystemOverlay = viewModel::requestSystemOverlayEnable,
                  onCheckForUpdates = { viewModel.checkForAppUpdates(explicitConsent = true) },
                  onResetSettings = viewModel::resetSettings,
                  onSelectLogSeverity = viewModel::setLogSeverityFilter,
                  onEmitTestLog = viewModel::emitTestDiagnostic,
                  onClearLogs = viewModel::clearLogs,
                )
              }
            }
          }
        }
      }
    }

    if (readyState != null) {
      FloatingBubbleOverlay(
        enabled = readyState.dashboard.settings.floatingBubbleEnabled,
        state = readyState.dashboard.floatingAssistantState,
        onToggleExpandMinimize = viewModel::toggleFloatingBubbleMode,
        onQuickVoice = { viewModel.setAssistantStatus(AssistantStatus.LISTENING) },
        onDailyBriefing = viewModel::triggerDailyBriefing,
        onRequestSystemOverlay = viewModel::requestSystemOverlayEnable,
        modifier =
          Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 16.dp, bottom = 92.dp),
      )
    }
  }
}

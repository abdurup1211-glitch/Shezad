package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.apps.AppState
import com.example.apps.SupportedExternalApp
import com.example.brain.AIBrainState
import com.example.conversation.ConversationState
import com.example.core.MaxContainer
import com.example.core.MaxLifecycleState
import com.example.emotion.EmotionState
import com.example.internet.InternetActionOutcome
import com.example.internet.SearchVertical
import com.example.memory.MemoryCategory
import com.example.permissions.AssistantPermission
import com.example.permissions.PermissionState
import com.example.permissions.PrivacyCheckResult
import com.example.permissions.SensitiveDomain
import com.example.wakeword.WakeWordState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `app name matches MAX`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("MAX", appName)
  }

  @Test
  fun `MAXCore initializes in strict privacy lock mode with all modules`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val container = MaxContainer(context)
    container.maxCore.initialize()

    val state = container.maxCore.coreState.value
    assertEquals(MaxLifecycleState.PRIVACY_LOCKED, state.lifecycle)
    assertTrue(state.privacyShieldActive)
    assertTrue(state.modules.size >= 24)
    assertEquals(WakeWordState.PrivacyLocked, container.wakeWordManager.state.value)
    assertEquals(
      PermissionState.PRIVACY_LOCKED,
      container.permissionManager.checkPermissionStatus(AssistantPermission.CAMERA),
    )
  }

  @Test
  fun `wake word flow greets Sanju and respects day-night farewell`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val container = MaxContainer(context)
    container.maxCore.initialize()
    container.maxCore.triggerWakeWordMax(speakGreeting = false)

    assertEquals(
      ConversationState.ListeningForCommand,
      container.conversationManager.state.value,
    )
    assertTrue(
      container.conversationManager.transcript.value.any { it.text.contains("সানজু") },
    )

    val dayFarewell = container.farewellManager.buildFarewell(hourOfDay = 10)
    val nightFarewell = container.farewellManager.buildFarewell(hourOfDay = 22)
    assertTrue(dayFarewell.contains("দিনটি সুন্দর কাটুক"))
    assertTrue(nightFarewell.contains("রাতটি শান্তিময়"))
  }

  @Test
  fun `privacy controller blocks automatic sensitive actions`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val container = MaxContainer(context)
    val cameraAuto = container.privacyController.validateActionAllowed(SensitiveDomain.CAMERA, explicitUserRequest = false)
    val internetAuto = container.internetManager.prepareSearch("test", SearchVertical.WEB, explicitUserRequest = false)
    val appAuto = container.appManager.openApp(SupportedExternalApp.WHATSAPP, explicitUserRequest = false)

    assertTrue(cameraAuto is PrivacyCheckResult.Blocked)
    assertTrue(internetAuto is InternetActionOutcome.Blocked)
    assertTrue(appAuto is AppState.ConfirmationRequired)
  }

  @Test
  fun `emotion, memory commands, calculator, and smart translation work together`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val container = MaxContainer(context)
    container.maxCore.initialize()

    val style = container.emotionManager.evaluateUserMood("আজ আমার মন খারাপ")
    assertEquals(EmotionState.CARING, style.emotion)

    val memReply = container.maxCore.processUserCommand("MAX, এটা মনে রাখো আগামীকাল মিটিং আছে")
    assertTrue(memReply.contains("মনে রেখেছি"))

    val calcReply = container.maxCore.processUserCommand("12 * 5")
    assertTrue(calcReply.contains("60"))

    val transReply = container.maxCore.processUserCommand("MAX, কেমন আছেন এটা বাংলা থেকে ইংরেজিতে অনুবাদ করো")
    assertTrue(transReply.contains("How are you?"))

    container.memoryManager.saveMemory("Name", "Sanju", MemoryCategory.FACT)
    assertTrue(container.memoryManager.recallMemories("Sanju").isNotEmpty())
  }

  @Test
  fun `AI Brain handles follow-up questions and offline capability without internet`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val container = MaxContainer(context)
    container.maxCore.initialize()

    // Capability question should stay strictly offline
    val helpResponse = container.aiBrainManager.thinkAndRespond("তুমি কী করতে পারো?", speakReply = false)
    assertTrue(!helpResponse.usedInternet)
    assertTrue(!helpResponse.awaitingConsent)
    assertTrue(helpResponse.replyText.contains("MAX"))

    // Missing-slot follow-up flow: ask for translation text -> clarify -> complete offline
    val askMissing = container.aiBrainManager.thinkAndRespond("অনুবাদ করো", speakReply = false)
    assertTrue(askMissing.awaitingClarification)
    assertTrue(container.aiBrainManager.brainState.value is AIBrainState.AwaitingClarification)

    val completedTranslation = container.aiBrainManager.thinkAndRespond("কেমন আছেন", speakReply = false)
    assertTrue(!completedTranslation.usedInternet)
    assertTrue(completedTranslation.replyText.contains("How are you?"))
  }

  @Test
  fun `AI Brain requires explicit internet consent before searching online`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val container = MaxContainer(context)
    container.maxCore.initialize()

    // Asking about weather triggers internet permission prompt instead of opening internet directly
    val consentPrompt = container.aiBrainManager.thinkAndRespond("ঢাকার আবহাওয়া কেমন?", speakReply = false)
    assertTrue(consentPrompt.awaitingConsent)
    assertTrue(!consentPrompt.usedInternet)
    assertTrue(container.aiBrainManager.brainState.value is AIBrainState.AwaitingInternetConsent)

    // Declining stays strictly offline
    val declined = container.aiBrainManager.thinkAndRespond("না", speakReply = false)
    assertTrue(!declined.usedInternet)
    assertTrue(declined.replyText.contains("ইন্টারনেট ব্যবহার করলাম না"))

    // Asking again and granting consent prepares browser link via InternetManager
    container.aiBrainManager.thinkAndRespond("Google-এ খুঁজে দেখো Bangladesh", speakReply = false)
    assertTrue(container.aiBrainManager.brainState.value is AIBrainState.AwaitingInternetConsent)

    val granted = container.aiBrainManager.confirmPendingInternetAccess(allowed = true)
    assertTrue(granted.usedInternet)
    assertTrue(granted.replyText.contains("https://"))
    assertTrue(container.aiBrainManager.contextManager.snapshot.value.recentTurns.size >= 4)
  }

  @Test
  fun `multi-step planning and offline AI work with zero internet`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val container = MaxContainer(context)
    container.maxCore.initialize()

    // Multi-step offline execution
    val multi = container.aiBrainManager.thinkAndRespond("12 * 5 এবং কেমন আছেন অনুবাদ করো", speakReply = false)
    assertTrue(!multi.usedInternet)
    assertTrue(multi.replyText.contains("60"))
    assertTrue(multi.replyText.contains("How are you?"))
    assertTrue(container.aiBrainManager.brainState.value is AIBrainState.Responded)

    // Offline time & date + reminder creation & listing + context repeat
    val timeReply = container.aiBrainManager.thinkAndRespond("এখন কয়টা বাজে?", speakReply = false)
    assertTrue(!timeReply.usedInternet)
    assertTrue(timeReply.replyText.contains("স্থানীয় সময়"))

    container.aiBrainManager.thinkAndRespond("রিমাইন্ডার দাও পানি খাবো", speakReply = false)
    val listReply = container.aiBrainManager.thinkAndRespond("রিমাইন্ডার দেখাও", speakReply = false)
    assertTrue(listReply.replyText.contains("পানি খাবো"))

    val repeated = container.aiBrainManager.thinkAndRespond("আবার বলো", speakReply = false)
    assertTrue(repeated.replyText.contains("পানি খাবো"))
  }

  @Test
  fun `smart automation routines, daily briefing, and scheduled tasks operate offline`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val container = MaxContainer(context)
    container.maxCore.initialize()

    container.automationManager.scheduleTask("Project Review", "05:00 PM")
    assertEquals(1, container.automationManager.scheduledTasks.value.size)

    val briefing = container.automationManager.generateDailyBriefing(hourOfDay = 9)
    assertTrue(briefing.greeting.contains("সুপ্রভাত"))
    assertTrue(briefing.pendingTasksCount >= 1)

    val custom =
      container.automationManager.addCustomRoutine(
        title = "Study Mode",
        triggerPhrase = "পড়ার রুটিন",
        steps = listOf("ফোকাস চালু", "নোট প্রস্তুত"),
        scheduledTimeLabel = "08:00 PM",
      )
    assertTrue(container.automationManager.matchCustomRoutineByCommand("পড়ার রুটিন চালু করো")?.id == custom.id)

    val nightResult = container.aiBrainManager.thinkAndRespond("রাতের রুটিন", speakReply = false)
    assertTrue(nightResult.replyText.contains("রাতের রুটিন সম্পন্ন"))
    assertTrue(container.permissionManager.isPrivacyLockActive())
  }

  @Test
  fun `floating assistant lifecycle and consented update checker enforce privacy`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val container = MaxContainer(context)
    container.maxCore.initialize()

    // Floating assistant starts safely in minimized zero-permission in-app bubble
    container.floatingAssistantController.toggleMinimizeRestore()
    assertEquals(
      com.example.overlay.FloatingBubbleMode.IN_APP_EXPANDED,
      container.floatingAssistantController.state.value.mode,
    )
    container.floatingAssistantController.hideBubble()
    assertEquals(
      com.example.overlay.FloatingBubbleMode.HIDDEN,
      container.floatingAssistantController.state.value.mode,
    )

    // Without explicit overlay permission, system overlay refuses and stays in safe in-app mode
    val started = container.floatingAssistantController.startSystemOverlayIfPermitted(explicitUserRequest = true)
    assertTrue(!started)

    // Update checker requires consent before network calls & parses releases accurately
    val blockedUpdate = kotlinx.coroutines.runBlocking {
      container.appUpdateChecker.checkForUpdates(explicitUserConsent = false)
    }
    assertTrue(blockedUpdate is com.example.update.UpdateCheckState.ConsentRequired)

    val available =
      container.appUpdateChecker.parseReleaseJson(
        """{"tag_name":"v9.9.0","html_url":"https://github.com/example/max/releases/tag/v9.9.0","body":"Faster offline brain"}""",
      )
    assertTrue(available is com.example.update.UpdateCheckState.UpdateAvailable)
  }

  @Test
  fun `wake word bilingual matching and capability privacy gates hold end-to-end`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val container = MaxContainer(context)
    container.maxCore.initialize()

    assertTrue(!container.wakeWordManager.onKeywordMatched("maximum power", confidence = 0.99f))
    assertTrue(container.wakeWordManager.onKeywordMatched("Hey MAX!", confidence = 0.95f))
    assertTrue(container.wakeWordManager.onKeywordMatched("ম্যাক্স", confidence = 0.92f))

    // Gallery, Files, and Camera require explicit user action and reset cleanly under privacy lock
    assertTrue(!container.galleryManager.requestOpenGallery(explicitUserRequest = false))
    assertTrue(container.galleryManager.requestOpenGallery(explicitUserRequest = true))
    assertTrue(
      container.fileManager.inspectUserSelectedFile(
        android.net.Uri.Builder().scheme("content").authority("example").appendPath("doc.txt").build(),
        explicitUserRequest = false,
      ) is com.example.files.FileState.PrivacyLocked,
    )

    container.maxCore.enforcePrivacyLock()
    assertTrue(container.galleryManager.state.value is com.example.gallery.GalleryState.PrivacyLocked)
    assertTrue(container.fileManager.state.value is com.example.files.FileState.PrivacyLocked)
  }

  @Test
  @Config(sdk = [24])
  fun `system overlay is gracefully disabled below Android 8_0`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val container = MaxContainer(context)
    container.maxCore.initialize()

    val controller = container.floatingAssistantController
    assertTrue(!controller.isSystemOverlaySupported())
    assertTrue(!controller.state.value.systemOverlaySupported)
    assertTrue(!controller.checkSystemOverlayPermission())
    assertTrue(!controller.startSystemOverlayIfPermitted(explicitUserRequest = true))
    assertEquals(null, controller.buildOverlayPermissionSettingsIntent())
    assertEquals(
      com.example.overlay.FloatingBubbleMode.IN_APP_MINIMIZED,
      controller.state.value.mode,
    )
  }

  @Test
  fun `system overlay is gracefully disabled on low-RAM Android Go and Wear devices`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
    org.robolectric.Shadows.shadowOf(activityManager).setIsLowRamDevice(true)

    val lowRamContainer = MaxContainer(context)
    assertTrue(!lowRamContainer.floatingAssistantController.isSystemOverlaySupported())
    assertTrue(!lowRamContainer.floatingAssistantController.refreshOverlayPermission())
    assertTrue(!lowRamContainer.floatingAssistantController.startSystemOverlayIfPermitted(explicitUserRequest = true))

    org.robolectric.Shadows.shadowOf(activityManager).setIsLowRamDevice(false)
    org.robolectric.Shadows.shadowOf(context.packageManager)
      .setSystemFeature(android.content.pm.PackageManager.FEATURE_WATCH, true)

    val watchContainer = MaxContainer(context)
    assertTrue(!watchContainer.floatingAssistantController.isSystemOverlaySupported())
    assertTrue(!watchContainer.floatingAssistantController.startSystemOverlayIfPermitted(explicitUserRequest = true))
  }

  @Test
  fun `system overlay starts cleanly on Android 8_0+ when SYSTEM_ALERT_WINDOW is granted`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    org.robolectric.shadows.ShadowSettings.setCanDrawOverlays(true)
    try {
      val container = MaxContainer(context)
      val controller = container.floatingAssistantController
      assertTrue(controller.isSystemOverlaySupported())
      assertTrue(controller.checkSystemOverlayPermission())
      assertTrue(controller.startSystemOverlayIfPermitted(explicitUserRequest = true))
      assertEquals(
        com.example.overlay.FloatingBubbleMode.SYSTEM_OVERLAY_ACTIVE,
        controller.state.value.mode,
      )

      controller.stopSystemOverlay()
      assertEquals(
        com.example.overlay.FloatingBubbleMode.IN_APP_MINIMIZED,
        controller.state.value.mode,
      )
    } finally {
      org.robolectric.shadows.ShadowSettings.setCanDrawOverlays(false)
    }
  }

  @Test
  fun `FloatingOverlayService handles missing permission and ACTION_STOP without crashing`() {
    org.robolectric.shadows.ShadowSettings.setCanDrawOverlays(false)
    val showIntent =
      android.content.Intent(
        ApplicationProvider.getApplicationContext(),
        com.example.overlay.FloatingOverlayService::class.java,
      ).apply {
        action = com.example.overlay.FloatingOverlayService.ACTION_SHOW
      }
    val serviceController =
      org.robolectric.Robolectric.buildService(com.example.overlay.FloatingOverlayService::class.java, showIntent)
        .create()
        .startCommand(0, 1)

    val stopIntent =
      android.content.Intent(
        ApplicationProvider.getApplicationContext(),
        com.example.overlay.FloatingOverlayService::class.java,
      ).apply {
        action = com.example.overlay.FloatingOverlayService.ACTION_STOP
      }
    serviceController.get().onStartCommand(stopIntent, 0, 2)
    serviceController.destroy()
  }
}

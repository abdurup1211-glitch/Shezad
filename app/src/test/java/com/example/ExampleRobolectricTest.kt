package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.apps.AppState
import com.example.apps.SupportedExternalApp
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
}

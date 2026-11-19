package com.example.phone

import com.example.permissions.PrivacyCheckResult
import com.example.permissions.PrivacyController
import com.example.permissions.SensitiveDomain
import com.example.utils.ErrorLogger
import com.example.utils.MaxModule
import com.example.utils.ModuleCategory
import com.example.utils.ModuleDescriptor
import com.example.utils.ModuleStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Central phone-control coordinator linking Call, SMS, Contacts, Alarm, Timer,
 * Stopwatch, Calculator, and Flashlight managers behind strict user confirmation.
 */
class PhoneManager(
  val callManager: CallManager,
  val smsManager: SMSManager,
  val contactsManager: ContactsManager,
  val alarmManager: AlarmManager,
  val timerManager: TimerManager,
  val stopwatchManager: StopwatchManager,
  val calculatorManager: CalculatorManager,
  val flashlightManager: FlashlightManager,
  private val privacyController: PrivacyController,
  private val errorLogger: ErrorLogger,
) : MaxModule {

  private val _state = MutableStateFlow<PhoneState>(PhoneState.PrivacyLocked)
  val state: StateFlow<PhoneState> = _state.asStateFlow()

  override val descriptor: ModuleDescriptor
    get() =
      ModuleDescriptor(
        id = "phone_manager",
        displayName = "PhoneManager",
        packageName = "com.example.phone",
        category = ModuleCategory.DEVICE_CAPABILITY,
        status = ModuleStatus.PRIVACY_LOCKED,
        summary = "User-confirmed Calls, SMS, Alarms, Timer, Stopwatch, Calculator & Torch.",
        privacyGuarded = true,
      )

  fun calculate(expression: String, explicitUserRequest: Boolean): PhoneState.CalculationResult? {
    val check = privacyController.validateActionAllowed(SensitiveDomain.PHONE, explicitUserRequest)
    if (check is PrivacyCheckResult.Blocked) return null
    val formatted =
      calculatorManager.evaluate(expression).fold(
        onSuccess = { value ->
          if (value % 1.0 == 0.0) {
            value.toLong().toString()
          } else {
            String.format(Locale.US, "%.4f", value).trimEnd('0').trimEnd('.')
          }
        },
        onFailure = { "Invalid expression" },
      )
    val state = PhoneState.CalculationResult(expression, formatted)
    _state.value = state
    errorLogger.logInfo("PhoneManager", "Evaluated calculation '$expression' = '$formatted'.")
    return state
  }

  fun requestTorchToggle(enabled: Boolean, explicitUserRequest: Boolean): PhoneState.FlashlightStatus {
    val check = privacyController.validateActionAllowed(SensitiveDomain.PHONE, explicitUserRequest)
    if (check is PrivacyCheckResult.Blocked) {
      return PhoneState.FlashlightStatus(enabled = false, supported = flashlightManager.isFlashSupported())
    }
    val status = flashlightManager.setTorch(enabled, explicitUserRequest)
    _state.value = status
    return status
  }

  fun releaseResources() {
    flashlightManager.release()
    _state.value = PhoneState.PrivacyLocked
  }
}

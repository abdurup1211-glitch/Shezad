package com.example.phone

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager as AndroidCameraManager
import com.example.utils.ErrorLogger

/**
 * Safe arithmetic evaluator for quick calculations in MAX.
 */
class CalculatorManager {

  fun evaluate(expression: String): Result<Double> =
    runCatching {
      val cleaned = expression.replace("×", "*").replace("÷", "/").replace("\\s+".toRegex(), "")
      require(cleaned.isNotBlank()) { "Empty expression" }
      Parser(cleaned).parseExpression()
    }

  private class Parser(private val input: String) {
    private var pos = -1
    private var ch = 0

    private fun nextChar() {
      ch = if (++pos < input.length) input[pos].code else -1
    }

    private fun eat(charToEat: Int): Boolean {
      while (ch == ' '.code) nextChar()
      if (ch == charToEat) {
        nextChar()
        return true
      }
      return false
    }

    fun parseExpression(): Double {
      nextChar()
      val x = parseAddSub()
      if (pos < input.length) throw IllegalArgumentException("Unexpected character: ${ch.toChar()}")
      return x
    }

    private fun parseAddSub(): Double {
      var x = parseMulDiv()
      while (true) {
        x =
          when {
            eat('+'.code) -> x + parseMulDiv()
            eat('-'.code) -> x - parseMulDiv()
            else -> return x
          }
      }
    }

    private fun parseMulDiv(): Double {
      var x = parseFactor()
      while (true) {
        x =
          when {
            eat('*'.code) -> x * parseFactor()
            eat('/'.code) -> {
              val divisor = parseFactor()
              require(divisor != 0.0) { "Division by zero" }
              x / divisor
            }
            else -> return x
          }
      }
    }

    private fun parseFactor(): Double {
      if (eat('+'.code)) return parseFactor()
      if (eat('-'.code)) return -parseFactor()
      val startPos = pos
      val x: Double
      if (eat('('.code)) {
        x = parseAddSub()
        require(eat(')'.code)) { "Missing closing parenthesis" }
      } else if (ch in '0'.code..'9'.code || ch == '.'.code) {
        while (ch in '0'.code..'9'.code || ch == '.'.code) nextChar()
        x = input.substring(startPos, pos).toDouble()
      } else {
        throw IllegalArgumentException("Invalid token")
      }
      return x
    }
  }
}

/**
 * Hardware-safe flashlight torch controller that only activates on explicit user request
 * and safely handles devices without a flash unit.
 */
class FlashlightManager(
  private val context: Context,
  private val errorLogger: ErrorLogger,
) {
  private var torchEnabled: Boolean = false

  fun isFlashSupported(): Boolean =
    runCatching {
      context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
    }.getOrDefault(false)

  fun setTorch(enabled: Boolean, explicitUserRequest: Boolean): PhoneState.FlashlightStatus {
    if (!explicitUserRequest || !isFlashSupported()) {
      return PhoneState.FlashlightStatus(enabled = false, supported = isFlashSupported())
    }
    runCatching {
      val camManager = context.getSystemService(Context.CAMERA_SERVICE) as? AndroidCameraManager
      val torchCameraId =
        camManager?.cameraIdList?.firstOrNull { id ->
          camManager.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
      if (torchCameraId != null) {
        camManager.setTorchMode(torchCameraId, enabled)
        torchEnabled = enabled
      }
    }.onFailure { error ->
      errorLogger.logWarning("FlashlightManager", "Torch toggle failed on device.", error)
      torchEnabled = false
    }
    return PhoneState.FlashlightStatus(enabled = torchEnabled, supported = isFlashSupported())
  }

  fun release() {
    if (torchEnabled) {
      setTorch(enabled = false, explicitUserRequest = true)
    }
  }
}

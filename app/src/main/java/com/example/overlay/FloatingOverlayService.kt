package com.example.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

/**
 * Lightweight, low-RAM Android 8.0+ (API 26+) Floating Assistant Overlay Service.
 *
 * Strictly privacy-safe:
 * - Never activates microphone, camera, or hidden audio capture from the service.
 * - Attaches overlay only when [Settings.canDrawOverlays] is already granted by the user.
 * - Restores [MainActivity] with quick voice intent on tap, supports minimize/restore & dismiss.
 */
class FloatingOverlayService : Service() {

  private var windowManager: WindowManager? = null
  private var overlayContainer: LinearLayout? = null
  private var isMinimizedBubble: Boolean = true

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onCreate() {
    super.onCreate()
    ensureNotificationChannel()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_STOP -> {
        removeOverlayView()
        stopSelf()
        return START_NOT_STICKY
      }
      ACTION_MINIMIZE -> {
        isMinimizedBubble = true
        refreshBubbleUi()
      }
      ACTION_RESTORE -> {
        isMinimizedBubble = false
        refreshBubbleUi()
      }
      else -> {
        if (!canDrawOverlay()) {
          stopSelf()
          return START_NOT_STICKY
        }
        startForeground(NOTIFICATION_ID, buildPersistentNotification())
        attachOverlayBubbleIfNeeded()
      }
    }
    return START_NOT_STICKY
  }

  override fun onDestroy() {
    removeOverlayView()
    super.onDestroy()
  }

  private fun canDrawOverlay(): Boolean =
    runCatching { Settings.canDrawOverlays(this) }.getOrDefault(false)

  private fun ensureNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager = getSystemService(NotificationManager::class.java) ?: return
      val existing = manager.getNotificationChannel(CHANNEL_ID)
      if (existing == null) {
        val channel =
          NotificationChannel(
            CHANNEL_ID,
            "MAX Floating Assistant",
            NotificationManager.IMPORTANCE_LOW,
          ).apply {
            description = "Quick access floating assistant control without background sensors."
            setShowBadge(false)
          }
        manager.createNotificationChannel(channel)
      }
    }
  }

  private fun buildPersistentNotification(): Notification {
    val openAppIntent =
      Intent(this, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        putExtra(EXTRA_QUICK_VOICE, true)
      }
    val openPendingIntent =
      PendingIntent.getActivity(
        this,
        101,
        openAppIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val stopIntent = Intent(this, FloatingOverlayService::class.java).apply { action = ACTION_STOP }
    val stopPendingIntent =
      PendingIntent.getService(
        this,
        102,
        stopIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle("MAX Floating Assistant")
      .setContentText("Tap to open quick voice (sensors remain locked)")
      .setOngoing(true)
      .setContentIntent(openPendingIntent)
      .addAction(0, "Close", stopPendingIntent)
      .build()
  }

  private fun attachOverlayBubbleIfNeeded() {
    if (overlayContainer != null) return
    val wm = getSystemService(WINDOW_SERVICE) as? WindowManager ?: return
    windowManager = wm

    val layoutType =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
      } else {
        @Suppress("DEPRECATION")
        WindowManager.LayoutParams.TYPE_PHONE
      }

    val params =
      WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        layoutType,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT,
      ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = 32
        y = 260
      }

    val container = buildBubbleContainer(wm, params)
    runCatching {
      wm.addView(container, params)
      overlayContainer = container
    }.onFailure {
      overlayContainer = null
      stopSelf()
    }
  }

  private inner class DraggableBubbleLayout : LinearLayout(this@FloatingOverlayService) {
    override fun performClick(): Boolean {
      super.performClick()
      return true
    }
  }

  private fun buildBubbleContainer(
    wm: WindowManager,
    params: WindowManager.LayoutParams,
  ): LinearLayout {
    val container =
      DraggableBubbleLayout().apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val pad = (12 * resources.displayMetrics.density).toInt()
        setPadding(pad, pad / 2, pad, pad / 2)
        background =
          GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(0xFF2563EB.toInt(), 0xFF7C3AED.toInt()),
          ).apply {
            cornerRadius = 48f * resources.displayMetrics.density
          }
        elevation = 8f * resources.displayMetrics.density
      }

    val bubbleTitle =
      TextView(this).apply {
        text = getString(R.string.overlay_title_max)
        setTextColor(Color.WHITE)
        textSize = 14f
        setPadding(8, 4, 8, 4)
      }

    val voiceButton =
      TextView(this).apply {
        text = getString(R.string.overlay_action_voice)
        setTextColor(Color.WHITE)
        textSize = 13f
        setPadding(12, 4, 12, 4)
        visibility = if (isMinimizedBubble) View.GONE else View.VISIBLE
        setOnClickListener {
          val launch =
            Intent(this@FloatingOverlayService, MainActivity::class.java).apply {
              flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
              putExtra(EXTRA_QUICK_VOICE, true)
            }
          runCatching { startActivity(launch) }
        }
      }

    val closeButton =
      TextView(this).apply {
        text = getString(R.string.overlay_action_close)
        setTextColor(Color.WHITE)
        textSize = 16f
        setPadding(10, 2, 6, 2)
        visibility = if (isMinimizedBubble) View.GONE else View.VISIBLE
        setOnClickListener {
          removeOverlayView()
          stopSelf()
        }
      }

    container.addView(bubbleTitle)
    container.addView(voiceButton)
    container.addView(closeButton)

    var initialX = 0
    var initialY = 0
    var initialTouchX = 0f
    var initialTouchY = 0f

    container.setOnClickListener {
      isMinimizedBubble = !isMinimizedBubble
      voiceButton.visibility = if (isMinimizedBubble) View.GONE else View.VISIBLE
      closeButton.visibility = if (isMinimizedBubble) View.GONE else View.VISIBLE
    }

    container.setOnTouchListener { view, event ->
      when (event.action) {
        MotionEvent.ACTION_DOWN -> {
          initialX = params.x
          initialY = params.y
          initialTouchX = event.rawX
          initialTouchY = event.rawY
          true
        }
        MotionEvent.ACTION_MOVE -> {
          params.x = initialX + (event.rawX - initialTouchX).toInt()
          params.y = initialY + (event.rawY - initialTouchY).toInt()
          runCatching { wm.updateViewLayout(container, params) }
          true
        }
        MotionEvent.ACTION_UP -> {
          val moved =
            kotlin.math.abs(event.rawX - initialTouchX) > 12f ||
              kotlin.math.abs(event.rawY - initialTouchY) > 12f
          if (!moved) {
            view.performClick()
          }
          true
        }
        else -> false
      }
    }
    return container
  }

  private fun refreshBubbleUi() {
    val container = overlayContainer ?: return
    if (container.childCount >= 3) {
      val vis = if (isMinimizedBubble) View.GONE else View.VISIBLE
      container.getChildAt(1).visibility = vis
      container.getChildAt(2).visibility = vis
    }
  }

  private fun removeOverlayView() {
    overlayContainer?.let { view ->
      runCatching { windowManager?.removeView(view) }
    }
    overlayContainer = null
  }

  companion object {
    const val CHANNEL_ID = "max_floating_overlay_channel"
    const val NOTIFICATION_ID = 4102
    const val ACTION_SHOW = "com.example.overlay.ACTION_SHOW"
    const val ACTION_STOP = "com.example.overlay.ACTION_STOP"
    const val ACTION_MINIMIZE = "com.example.overlay.ACTION_MINIMIZE"
    const val ACTION_RESTORE = "com.example.overlay.ACTION_RESTORE"
    const val EXTRA_QUICK_VOICE = "extra_quick_voice"
  }
}

package me.lucky.volta

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.*
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.timerTask

class AccessibilityService : AccessibilityService() {
    companion object {
        private const val LONG_PRESS_DURATION = 500L
        private const val VIBE_DURATION = 200L
        private const val DOUBLE_PRESS_DURATION = 300L
    }

    private lateinit var prefs: Preferences
    private lateinit var torchCallback: TorchCallback
    private var audioManager: AudioManager? = null
    private var cameraManager: CameraManager? = null
    private var vibrator: Vibrator? = null
    @RequiresApi(Build.VERSION_CODES.O)
    private var vibrationEffect: VibrationEffect? = null
    private var longDownTask: Timer? = null
    private var longUpTask: Timer? = null
    private var doubleUpTask: Timer? = null
    private val longDownFlag = AtomicBoolean()
    private val longUpFlag = AtomicBoolean()
    private val doubleUpFlag = AtomicBoolean()
    private val upTime = AtomicLong()

    override fun onCreate() {
        super.onCreate()
        init()
    }

    override fun onDestroy() {
        super.onDestroy()
        deinit()
    }

    private fun deinit() {
        longDownTask?.cancel()
        longUpTask?.cancel()
        doubleUpTask?.cancel()
        cameraManager?.unregisterTorchCallback(torchCallback)
        torchCallback.disableFlashlightTask?.cancel()
    }

    private fun init() {
        prefs = Preferences(this)
        torchCallback = TorchCallback(WeakReference(this))
        audioManager = getSystemService(AudioManager::class.java)
        cameraManager = getSystemService(CameraManager::class.java)
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            getSystemService(Vibrator::class.java)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrationEffect = VibrationEffect.createOneShot(
                VIBE_DURATION,
                VibrationEffect.DEFAULT_AMPLITUDE,
            )
        }
        try {
            cameraManager?.registerTorchCallback(torchCallback, null)
        } catch (exc: IllegalArgumentException) {
            cameraManager = null
        }
    }

    @RequiresPermission(Manifest.permission.VIBRATE)
    private fun vibrate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(vibrationEffect)
        } else {
            @Suppress("deprecation")
            vibrator?.vibrate(VIBE_DURATION)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event == null ||
            !prefs.isServiceEnabled ||
            audioManager?.mode != AudioManager.MODE_NORMAL) return false
        val isMusicActive = audioManager?.isMusicActive == true
        if (prefs.isTrackChecked && isMusicActive)
            when (event.keyCode) {
                KeyEvent.KEYCODE_VOLUME_DOWN -> return previousTrack(event)
                KeyEvent.KEYCODE_VOLUME_UP -> return nextTrack(event)
            }
        else if (
            prefs.isFlashlightChecked &&
            !isMusicActive &&
            event.keyCode == KeyEvent.KEYCODE_VOLUME_UP) return flashlight(event)
        return super.onKeyEvent(event)
    }

    private fun previousTrack(event: KeyEvent): Boolean {
        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                longDownTask?.cancel()
                longDownTask = Timer()
                longDownTask?.schedule(timerTask {
                    longDownFlag.set(true)
                    dispatchMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
                    vibrate()
                }, LONG_PRESS_DURATION)
                return true
            }
            KeyEvent.ACTION_UP -> {
                longDownTask?.cancel()
                if (!longDownFlag.getAndSet(false)) volumeDown()
                return true
            }
        }
        return false
    }

    private fun volumeDown() {
        audioManager?.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_LOWER,
            AudioManager.FLAG_SHOW_UI,
        )
    }

    private fun volumeUp() {
        audioManager?.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_RAISE,
            AudioManager.FLAG_SHOW_UI,
        )
    }

    private fun dispatchMediaKeyEvent(code: Int) {
        audioManager?.dispatchMediaKeyEvent(KeyEvent(
            KeyEvent.ACTION_DOWN,
            code,
        ))
        audioManager?.dispatchMediaKeyEvent(KeyEvent(
            KeyEvent.ACTION_UP,
            code,
        ))
    }

    private fun nextTrack(event: KeyEvent): Boolean {
        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                longUpTask?.cancel()
                longUpTask = Timer()
                longUpTask?.schedule(timerTask {
                    longUpFlag.set(true)
                    dispatchMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_NEXT)
                    vibrate()
                }, LONG_PRESS_DURATION)
                return true
            }
            KeyEvent.ACTION_UP -> {
                longUpTask?.cancel()
                if (!longUpFlag.getAndSet(false)) volumeUp()
                return true
            }
        }
        return false
    }

    private fun flashlight(event: KeyEvent): Boolean {
        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (event.eventTime - upTime.getAndSet(event.eventTime) < DOUBLE_PRESS_DURATION) {
                    doubleUpTask?.cancel()
                    doubleUpFlag.set(true)
                    toggleFlashlight()
                    vibrate()
                }
                return true
            }
            KeyEvent.ACTION_UP -> {
                if (!doubleUpFlag.getAndSet(false)) {
                    doubleUpTask?.cancel()
                    doubleUpTask = Timer()
                    doubleUpTask?.schedule(timerTask { volumeUp() }, DOUBLE_PRESS_DURATION)
                }
                return true
            }
        }
        return false
    }

    private fun toggleFlashlight() {
        val state = !torchCallback.state.get()
        torchCallback.internalState.set(state)
        if (!setTorchMode(state)) torchCallback.internalState.set(false)
    }

    @RequiresPermission("android.permission.FLASHLIGHT")
    private fun setTorchMode(value: Boolean): Boolean {
        try {
            cameraManager?.setTorchMode(
                cameraManager
                    ?.cameraIdList
                    ?.firstOrNull {
                        cameraManager
                            ?.getCameraCharacteristics(it)
                            ?.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                    } ?: return false,
                value,
            )
        } catch (exc: Exception) { return false }
        return true
    }

    private class TorchCallback(
        private val service: WeakReference<me.lucky.volta.AccessibilityService>,
    ) : CameraManager.TorchCallback() {
        companion object {
            private const val DISABLE_FLASHLIGHT_DELAY = 15 * 60 * 1000L
        }

        val state = AtomicBoolean()
        val internalState = AtomicBoolean()
        var disableFlashlightTask: Timer? = null

        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            super.onTorchModeChanged(cameraId, enabled)
            disableFlashlightTask?.cancel()
            state.set(enabled)
            if (internalState.getAndSet(false) && enabled) {
                disableFlashlightTask = Timer()
                disableFlashlightTask?.schedule(timerTask {
                    service.get()?.setTorchMode(false)
                }, DISABLE_FLASHLIGHT_DELAY)
            }
        }
    }
}

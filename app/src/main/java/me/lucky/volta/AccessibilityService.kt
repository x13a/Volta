package me.lucky.volta

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
    private lateinit var proximityListener: ProximityListener
    private var audioManager: AudioManager? = null
    private var cameraManager: CameraManager? = null
    private var sensorManager: SensorManager? = null
    private var proximitySensor: Sensor? = null
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
    private val torchCallback = TorchCallback()

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
        sensorManager?.unregisterListener(proximityListener)
    }

    private fun init() {
        prefs = Preferences(this)
        proximityListener = ProximityListener(WeakReference(this))
        audioManager = getSystemService(AudioManager::class.java)
        cameraManager = getSystemService(CameraManager::class.java)
        sensorManager = getSystemService(SensorManager::class.java)
        proximitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)
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
            sensorManager = null
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
        if (event == null || !prefs.isServiceEnabled) return false
        var result = super.onKeyEvent(event)
        val isMusicActive = audioManager?.isMusicActive == true
        val isAudioModeNormal = audioManager?.mode == AudioManager.MODE_NORMAL
        var isFlashlight = prefs.isFlashlightChecked
        if (isFlashlight) {
            val flag = prefs.flashlightFlag
            if (flag.and(FlashlightFlag.MUSIC.value) == 0) {
                isFlashlight = isFlashlight && !isMusicActive
            }
            if (flag.and(FlashlightFlag.CALL.value) == 0) {
                isFlashlight = isFlashlight && isAudioModeNormal
            }
        }
        if (isMusicActive && isAudioModeNormal)
            when (event.keyCode) {
                KeyEvent.KEYCODE_VOLUME_DOWN -> result = previousTrack(event)
                KeyEvent.KEYCODE_VOLUME_UP -> result = nextTrack(event, isFlashlight)
            }
        if (isFlashlight && event.keyCode == KeyEvent.KEYCODE_VOLUME_UP)
            result = flashlight(event)
        return result
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

    private fun nextTrack(event: KeyEvent, isFlashlight: Boolean): Boolean {
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
                if (!isFlashlight && !longUpFlag.getAndSet(false)) volumeUp()
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
                    if (sensorManager != null && proximitySensor != null) {
                        sensorManager?.registerListener(
                            proximityListener,
                            proximitySensor,
                            SensorManager.SENSOR_DELAY_FASTEST,
                        )
                    } else {
                        toggleFlashlight()
                        vibrate()
                    }
                }
                return true
            }
            KeyEvent.ACTION_UP -> {
                if (!longUpFlag.getAndSet(false) &&
                    !doubleUpFlag.getAndSet(false))
                {
                    doubleUpTask?.cancel()
                    doubleUpTask = Timer()
                    doubleUpTask?.schedule(timerTask { volumeUp() }, DOUBLE_PRESS_DURATION)
                }
                return true
            }
        }
        return false
    }

    @RequiresPermission("android.permission.FLASHLIGHT")
    private fun toggleFlashlight() {
        val state = !torchCallback.state.get()
        try {
            cameraManager?.setTorchMode(
                cameraManager
                    ?.cameraIdList
                    ?.firstOrNull {
                        cameraManager
                            ?.getCameraCharacteristics(it)
                            ?.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                    } ?: return,
                state,
            )

        } catch (exc: Exception) { return }
        torchCallback.state.set(state)
    }

    private class TorchCallback: CameraManager.TorchCallback() {
        val state = AtomicBoolean()

        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            super.onTorchModeChanged(cameraId, enabled)
            state.set(enabled)
        }
    }

    private class ProximityListener(
        private val service: WeakReference<me.lucky.volta.AccessibilityService>,
    ) : SensorEventListener {
        companion object {
            private const val SENSITIVITY = 4
        }

        override fun onSensorChanged(event: SensorEvent?) {
            if (event == null || event.sensor.type != Sensor.TYPE_PROXIMITY) return
            val service = service.get() ?: return
            val value = event.values.getOrNull(0)
            if (value == null || value < -SENSITIVITY || value > SENSITIVITY) {
                service.toggleFlashlight()
                service.vibrate()
            } else {
                service.volumeUp()
            }
            service.sensorManager?.unregisterListener(service.proximityListener)
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }
}

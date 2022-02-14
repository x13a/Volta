package me.lucky.volta

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.media.AudioManager
import android.os.*
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import java.util.*
import kotlin.concurrent.timerTask

class AccessibilityService : AccessibilityService() {
    companion object {
        private const val LONG_PRESS_DURATION = 500L
        private const val VIBE_DURATION = 200L
        private const val DOUBLE_PRESS_DURATION = 300L
    }

    private lateinit var prefs: Preferences
    private lateinit var torchManager: TorchManager
    private var audioManager: AudioManager? = null
    private var vibrator: Vibrator? = null
    @RequiresApi(Build.VERSION_CODES.O)
    private var vibrationEffect: VibrationEffect? = null
    private var longDownTask: Timer? = null
    private var longUpTask: Timer? = null
    private var doubleUpTask: Timer? = null
    private var longDownFlag = false
    private var longUpFlag = false
    private var doubleUpFlag = false
    private var upTime = 0L

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
        torchManager.deinit()
    }

    private fun init() {
        prefs = Preferences(this)
        torchManager = TorchManager(this)
        audioManager = getSystemService(AudioManager::class.java)
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            getSystemService(VibratorManager::class.java)?.defaultVibrator
        else
            getSystemService(Vibrator::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            vibrationEffect = VibrationEffect.createOneShot(
                VIBE_DURATION,
                VibrationEffect.DEFAULT_AMPLITUDE,
            )
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
            !prefs.isEnabled ||
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
                    longDownFlag = true
                    dispatchMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
                    if (prefs.trackOptions.and(TrackOption.VIBRATE.value) != 0) vibrate()
                }, LONG_PRESS_DURATION)
                return true
            }
            KeyEvent.ACTION_UP -> {
                longDownTask?.cancel()
                if (!longDownFlag) volumeDown() else longDownFlag = false
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
                    longUpFlag = true
                    dispatchMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_NEXT)
                    if (prefs.trackOptions.and(TrackOption.VIBRATE.value) != 0) vibrate()
                }, LONG_PRESS_DURATION)
                return true
            }
            KeyEvent.ACTION_UP -> {
                longUpTask?.cancel()
                if (!longUpFlag) volumeUp() else longUpFlag = false
                return true
            }
        }
        return false
    }

    private fun flashlight(event: KeyEvent): Boolean {
        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (event.eventTime - upTime < DOUBLE_PRESS_DURATION) {
                    doubleUpTask?.cancel()
                    doubleUpFlag = true
                    torchManager.toggle()
                    if (prefs.flashlightOptions.and(FlashlightOption.VIBRATE.value) != 0) vibrate()
                } else { upTime = event.eventTime }
                return true
            }
            KeyEvent.ACTION_UP -> {
                if (!doubleUpFlag) {
                    doubleUpTask?.cancel()
                    doubleUpTask = Timer()
                    doubleUpTask?.schedule(timerTask { volumeUp() }, DOUBLE_PRESS_DURATION)
                } else { doubleUpFlag = false }
                return true
            }
        }
        return false
    }
}

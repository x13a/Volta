package me.lucky.volta

import android.accessibilityservice.AccessibilityService
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.timerTask

class AccessibilityService : AccessibilityService() {
    companion object {
        private const val LONG_CLICK_DURATION = 500L
        private const val VIBE_DURATION = 200L
    }

    private lateinit var prefs: Preferences
    private var audioManager: AudioManager? = null
    private var vibrator: Vibrator? = null
    @RequiresApi(Build.VERSION_CODES.O)
    private var vibrationEffect: VibrationEffect? = null
    private var longDownTask: Timer? = null
    private var longUpTask: Timer? = null
    private val longDownFlag = AtomicBoolean()
    private val longUpFlag = AtomicBoolean()

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
    }

    private fun init() {
        prefs = Preferences(this)
        audioManager = getSystemService(AudioManager::class.java)
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
    }

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
        if (event == null
            || !prefs.isServiceEnabled
            || audioManager?.isMusicActive != true) return false
        when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> return previousTrack(event)
            KeyEvent.KEYCODE_VOLUME_UP -> return nextTrack(event)
        }
        return super.onKeyEvent(event)
    }

    private fun dispatchPreviousTrack() {
        audioManager?.dispatchMediaKeyEvent(KeyEvent(
            KeyEvent.ACTION_DOWN,
            KeyEvent.KEYCODE_MEDIA_PREVIOUS,
        ))
        audioManager?.dispatchMediaKeyEvent(KeyEvent(
            KeyEvent.ACTION_UP,
            KeyEvent.KEYCODE_MEDIA_PREVIOUS,
        ))
    }

    private fun previousTrack(event: KeyEvent): Boolean {
        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                longDownTask?.cancel()
                longDownTask = Timer()
                longDownTask?.schedule(timerTask {
                    longDownFlag.set(true)
                    dispatchPreviousTrack()
                    vibrate()
                }, LONG_CLICK_DURATION)
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
        var minVolume = 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            minVolume = audioManager?.getStreamMinVolume(AudioManager.STREAM_MUSIC) ?: 0
        }
        var volume = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
        if (volume > minVolume) volume -= 1
        audioManager?.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            volume,
            AudioManager.FLAG_SHOW_UI,
        )
    }

    private fun volumeUp() {
        val maxVolume = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 0
        var volume = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
        if (volume < maxVolume) volume += 1
        audioManager?.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            volume,
            AudioManager.FLAG_SHOW_UI,
        )
    }

    private fun dispatchNextTrack() {
        audioManager?.dispatchMediaKeyEvent(KeyEvent(
            KeyEvent.ACTION_DOWN,
            KeyEvent.KEYCODE_MEDIA_NEXT,
        ))
        audioManager?.dispatchMediaKeyEvent(KeyEvent(
            KeyEvent.ACTION_UP,
            KeyEvent.KEYCODE_MEDIA_NEXT,
        ))
    }

    private fun nextTrack(event: KeyEvent): Boolean {
        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                longUpTask?.cancel()
                longUpTask = Timer()
                longUpTask?.schedule(timerTask {
                    longUpFlag.set(true)
                    dispatchNextTrack()
                    vibrate()
                }, LONG_CLICK_DURATION)
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
}

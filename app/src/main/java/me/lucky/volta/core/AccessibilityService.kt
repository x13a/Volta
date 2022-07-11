package me.lucky.volta.core

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.media.AudioManager
import android.os.*
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import java.util.*
import kotlin.concurrent.timerTask

import me.lucky.volta.Option
import me.lucky.volta.Mode
import me.lucky.volta.Preferences

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
    private var doubleDownTask: Timer? = null
    private var longDownFlag = false
    private var longUpFlag = false
    private var doubleUpFlag = false
    private var doubleDownFlag = false
    private var doubleUpTime = 0L
    private var doubleDownTime = 0L

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
        doubleDownTask?.cancel()
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
        if (audioManager?.isMusicActive == true) {
            if (prefs.isTrackChecked)
                when (event.keyCode) {
                    KeyEvent.KEYCODE_VOLUME_DOWN -> return previousTrack(event)
                    KeyEvent.KEYCODE_VOLUME_UP -> return nextTrack(event)
                }
        } else if (prefs.isDoubleDownChecked && event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            return doubleDown(event)
        } else if (prefs.isDoubleUpChecked && event.keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            return doubleUp(event)
        }
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
                    if (prefs.trackOptions.and(Option.VIBRATE.value) != 0) vibrate()
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
                    if (prefs.trackOptions.and(Option.VIBRATE.value) != 0) vibrate()
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

    private fun doubleUp(event: KeyEvent): Boolean {
        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (event.eventTime - doubleUpTime < DOUBLE_PRESS_DURATION) {
                    doubleUpTask?.cancel()
                    doubleUpFlag = true
                    doubleSelect(prefs.doubleUpMode, 0)
                    if (prefs.doubleUpOptions.and(Option.VIBRATE.value) != 0) vibrate()
                } else { doubleUpTime = event.eventTime }
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

    private fun doubleDown(event: KeyEvent): Boolean {
        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (event.eventTime - doubleDownTime < DOUBLE_PRESS_DURATION) {
                    doubleDownTask?.cancel()
                    doubleDownFlag = true
                    doubleSelect(prefs.doubleDownMode, 1)
                    if (prefs.doubleDownOptions.and(Option.VIBRATE.value) != 0) vibrate()
                } else { doubleDownTime = event.eventTime }
                return true
            }
            KeyEvent.ACTION_UP -> {
                if (!doubleDownFlag) {
                    doubleDownTask?.cancel()
                    doubleDownTask = Timer()
                    doubleDownTask?.schedule(timerTask { volumeDown() }, DOUBLE_PRESS_DURATION)
                } else { doubleDownFlag = false }
                return true
            }
        }
        return false
    }

    private fun doubleSelect(mode: Int, index: Int) = when (mode) {
        Mode.FLASHLIGHT.value -> flashlight()
        Mode.BROADCAST.value -> broadcast(index)
        else -> {}
    }

    private fun flashlight() { torchManager.toggle() }

    private fun broadcast(index: Int) {
        val data = getBroadcastData(index)
        if (data.action.isEmpty()) return
        sendBroadcast(Intent(data.action).apply {
            val cls = data.receiver.split('/')
            val packageName = cls.firstOrNull() ?: ""
            if (packageName.isNotEmpty()) {
                setPackage(packageName)
                if (cls.size == 2)
                    setClassName(
                        packageName,
                        "$packageName.${cls[1].trimStart('.')}",
                    )
            }
            addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
            if (data.key.isNotEmpty()) putExtra(data.key, data.value)
        })
    }

    private fun getBroadcastData(index: Int) =
        when (index) {
            0 -> BroadcastData(
                prefs.broadcast0Action,
                prefs.broadcast0Receiver,
                prefs.broadcast0Key,
                prefs.broadcast0Value,
            )
            else -> BroadcastData(
                prefs.broadcast1Action,
                prefs.broadcast1Receiver,
                prefs.broadcast1Key,
                prefs.broadcast1Value,
            )
        }

    private data class BroadcastData(
        val action: String,
        val receiver: String,
        val key: String,
        val value: String,
    )
 }
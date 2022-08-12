package me.lucky.volta.core

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.media.AudioManager
import android.os.*
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresPermission
import java.util.*
import kotlin.concurrent.timerTask

import me.lucky.volta.Option
import me.lucky.volta.Mode
import me.lucky.volta.Preferences

class AccessibilityService : AccessibilityService() {
    private lateinit var prefs: Preferences
    private lateinit var torchManager: TorchManager
    private var audioManager: AudioManager? = null
    private var vibrator: Vibrator? = null
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
    }

    @RequiresPermission(Manifest.permission.VIBRATE)
    private fun vibrate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(
                prefs.vibeDuration,
                VibrationEffect.DEFAULT_AMPLITUDE,
            ))
        } else {
            @Suppress("deprecation")
            vibrator?.vibrate(prefs.vibeDuration)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event == null ||
            !prefs.isEnabled ||
            audioManager?.mode != AudioManager.MODE_NORMAL) return false
        if (audioManager?.isMusicActive == true) {
            if (prefs.isTrackEnabled)
                when (event.keyCode) {
                    KeyEvent.KEYCODE_VOLUME_DOWN -> return previousTrack(event)
                    KeyEvent.KEYCODE_VOLUME_UP -> return nextTrack(event)
                }
        } else if (prefs.isDoubleDownEnabled && event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            return doubleDown(event)
        } else if (prefs.isDoubleUpEnabled && event.keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
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
                }, prefs.longPressDuration)
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
                }, prefs.longPressDuration)
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
                if (event.eventTime - doubleUpTime < prefs.doublePressDuration) {
                    doubleUpTask?.cancel()
                    doubleUpFlag = true
                    doubleSelect(prefs.doubleUpMode, Broadcast.UP)
                    if (prefs.doubleUpOptions.and(Option.VIBRATE.value) != 0) vibrate()
                } else { doubleUpTime = event.eventTime }
                return true
            }
            KeyEvent.ACTION_UP -> {
                if (!doubleUpFlag) {
                    doubleUpTask?.cancel()
                    doubleUpTask = Timer()
                    doubleUpTask?.schedule(timerTask { volumeUp() }, prefs.doublePressDuration)
                } else { doubleUpFlag = false }
                return true
            }
        }
        return false
    }

    private fun doubleDown(event: KeyEvent): Boolean {
        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (event.eventTime - doubleDownTime < prefs.doublePressDuration) {
                    doubleDownTask?.cancel()
                    doubleDownFlag = true
                    doubleSelect(prefs.doubleDownMode, Broadcast.DOWN)
                    if (prefs.doubleDownOptions.and(Option.VIBRATE.value) != 0) vibrate()
                } else { doubleDownTime = event.eventTime }
                return true
            }
            KeyEvent.ACTION_UP -> {
                if (!doubleDownFlag) {
                    doubleDownTask?.cancel()
                    doubleDownTask = Timer()
                    doubleDownTask?.schedule(timerTask { volumeDown() }, prefs.doublePressDuration)
                } else { doubleDownFlag = false }
                return true
            }
        }
        return false
    }

    private fun doubleSelect(mode: Int, b: Broadcast) = when (mode) {
        Mode.FLASHLIGHT.value -> flashlight()
        Mode.BROADCAST.value -> broadcast(b)
        else -> {}
    }

    private fun flashlight() { torchManager.toggle() }

    private fun broadcast(b: Broadcast) {
        val data = getBroadcastData(b)
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
            if (data.extraKey.isNotEmpty()) putExtra(data.extraKey, data.extraValue)
        })
    }

    private fun getBroadcastData(b: Broadcast) = when (b) {
        Broadcast.UP -> BroadcastData(
            prefs.broadcastUpAction,
            prefs.broadcastUpReceiver,
            prefs.broadcastUpExtraKey,
            prefs.broadcastUpExtraValue,
        )
        Broadcast.DOWN -> BroadcastData(
            prefs.broadcastDownAction,
            prefs.broadcastDownReceiver,
            prefs.broadcastDownExtraKey,
            prefs.broadcastDownExtraValue,
        )
    }

    private data class BroadcastData(
        val action: String,
        val receiver: String,
        val extraKey: String,
        val extraValue: String,
    )

    private enum class Broadcast {
        UP,
        DOWN,
    }
 }
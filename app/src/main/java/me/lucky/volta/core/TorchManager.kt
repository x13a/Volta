package me.lucky.volta.core

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.annotation.RequiresPermission
import java.lang.ref.WeakReference
import java.util.*
import kotlin.concurrent.timerTask

import me.lucky.volta.Preferences

class TorchManager(ctx: Context) {
    private val torchCallback = TorchCallback(WeakReference(this), Preferences(ctx))
    private var cameraManager: CameraManager? = ctx.getSystemService(CameraManager::class.java)

    init {
        try { cameraManager?.registerTorchCallback(torchCallback, null) }
        catch (exc: IllegalArgumentException) { cameraManager = null }
    }

    fun deinit() {
        cameraManager?.unregisterTorchCallback(torchCallback)
        torchCallback.disableTask?.cancel()
    }

    fun toggle() {
        val state = !torchCallback.state
        torchCallback.internal = state
        if (!setTorchMode(state)) torchCallback.internal = false
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
        private val manager: WeakReference<TorchManager>,
        private val prefs: Preferences,
    ) : CameraManager.TorchCallback() {
        var state = false
        var internal = false
        var disableTask: Timer? = null

        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            super.onTorchModeChanged(cameraId, enabled)
            disableTask?.cancel()
            state = enabled
            if (!internal) return else internal = false
            if (!enabled) return
            disableTask = Timer()
            disableTask?.schedule(timerTask {
                manager.get()?.setTorchMode(false)
            }, prefs.torchDisableDelay * 60 * 1000L)
        }
    }
}
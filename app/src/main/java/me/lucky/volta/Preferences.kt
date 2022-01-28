package me.lucky.volta

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager

class Preferences(ctx: Context) {
    companion object {
        private const val SERVICE_ENABLED = "service_enabled"
        private const val FLASHLIGHT_CHECKED = "flashlight_checked"
    }

    private val prefs = PreferenceManager.getDefaultSharedPreferences(ctx)

    var isServiceEnabled: Boolean
        get() = prefs.getBoolean(SERVICE_ENABLED, false)
        set(value) = prefs.edit { putBoolean(SERVICE_ENABLED, value) }

    var isFlashlightChecked: Boolean
        get() = prefs.getBoolean(FLASHLIGHT_CHECKED, false)
        set(value) = prefs.edit { putBoolean(FLASHLIGHT_CHECKED, value) }
}

package me.lucky.volta

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager

class Preferences(ctx: Context) {
    companion object {
        private const val SERVICE_ENABLED = "service_enabled"
        private const val TRACK_CHECKED = "track_checked"
        private const val FLASHLIGHT_CHECKED = "flashlight_checked"
        private const val SHOW_PROMINENT_DISCLOSURE = "show_prominent_disclosure"
    }

    private val prefs = PreferenceManager.getDefaultSharedPreferences(ctx)

    var isServiceEnabled: Boolean
        get() = prefs.getBoolean(SERVICE_ENABLED, false)
        set(value) = prefs.edit { putBoolean(SERVICE_ENABLED, value) }

    var isTrackChecked: Boolean
        get() = prefs.getBoolean(TRACK_CHECKED, false)
        set(value) = prefs.edit { putBoolean(TRACK_CHECKED, value) }

    var isFlashlightChecked: Boolean
        get() = prefs.getBoolean(FLASHLIGHT_CHECKED, false)
        set(value) = prefs.edit { putBoolean(FLASHLIGHT_CHECKED, value) }

    var isShowProminentDisclosure: Boolean
        get() = prefs.getBoolean(SHOW_PROMINENT_DISCLOSURE, true)
        set(value) = prefs.edit { putBoolean(SHOW_PROMINENT_DISCLOSURE, value) }
}

package me.lucky.volta

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager

class Preferences(ctx: Context) {
    companion object {
        private const val ENABLED = "enabled"
        private const val TRACK_CHECKED = "track_checked"
        private const val TRACK_OPTIONS = "track_options"
        private const val FLASHLIGHT_CHECKED = "flashlight_checked"
        private const val FLASHLIGHT_OPTIONS = "flashlight_options"
        private const val SHOW_PROMINENT_DISCLOSURE = "show_prominent_disclosure"

        // migration
        private const val SERVICE_ENABLED = "service_enabled"
    }

    private val prefs = PreferenceManager.getDefaultSharedPreferences(ctx)

    var isEnabled: Boolean
        get() = prefs.getBoolean(ENABLED, prefs.getBoolean(SERVICE_ENABLED, false))
        set(value) = prefs.edit { putBoolean(ENABLED, value) }

    var isTrackChecked: Boolean
        get() = prefs.getBoolean(TRACK_CHECKED, false)
        set(value) = prefs.edit { putBoolean(TRACK_CHECKED, value) }

    var trackOptions: Int
        get() = prefs.getInt(TRACK_OPTIONS, TrackOption.VIBRATE.value)
        set(value) = prefs.edit { putInt(TRACK_OPTIONS, value) }

    var isFlashlightChecked: Boolean
        get() = prefs.getBoolean(FLASHLIGHT_CHECKED, false)
        set(value) = prefs.edit { putBoolean(FLASHLIGHT_CHECKED, value) }

    var flashlightOptions: Int
        get() = prefs.getInt(FLASHLIGHT_OPTIONS, FlashlightOption.VIBRATE.value)
        set(value) = prefs.edit { putInt(FLASHLIGHT_OPTIONS, value) }

    var isShowProminentDisclosure: Boolean
        get() = prefs.getBoolean(SHOW_PROMINENT_DISCLOSURE, true)
        set(value) = prefs.edit { putBoolean(SHOW_PROMINENT_DISCLOSURE, value) }
}

enum class TrackOption(val value: Int) {
    VIBRATE(1),
}

enum class FlashlightOption(val value: Int) {
    VIBRATE(1),
}

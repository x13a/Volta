package me.lucky.volta

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager

class Preferences(ctx: Context) {
    companion object {
        private const val SERVICE_ENABLED = "service_enabled"
        private const val FLASHLIGHT_CHECKED = "flashlight_checked"
        private const val FLASHLIGHT_FLAG = "flashlight_flag"
        private const val SHOW_PROMINENT_DISCLOSURE = "show_prominent_disclosure"

        // migration
        private const val PROMINENT_DISCLOSURE = "prominent_disclosure"
    }

    private val prefs = PreferenceManager.getDefaultSharedPreferences(ctx)

    var isServiceEnabled: Boolean
        get() = prefs.getBoolean(SERVICE_ENABLED, false)
        set(value) = prefs.edit { putBoolean(SERVICE_ENABLED, value) }

    var isFlashlightChecked: Boolean
        get() = prefs.getBoolean(FLASHLIGHT_CHECKED, false)
        set(value) = prefs.edit { putBoolean(FLASHLIGHT_CHECKED, value) }

    var flashlightFlag: Int
        get() = prefs.getInt(FLASHLIGHT_FLAG, 0)
        set(value) = prefs.edit { putInt(FLASHLIGHT_FLAG, value) }

    var isShowProminentDisclosure: Boolean
        get() = prefs.getBoolean(
            SHOW_PROMINENT_DISCLOSURE,
            prefs.getBoolean(PROMINENT_DISCLOSURE, true),
        )
        set(value) = prefs.edit { putBoolean(SHOW_PROMINENT_DISCLOSURE, value) }
}

enum class FlashlightFlag(val value: Int) {
    MUSIC(1),
    CALL(1 shl 1),
}

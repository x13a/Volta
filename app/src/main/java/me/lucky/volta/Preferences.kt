package me.lucky.volta

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager

class Preferences(ctx: Context) {
    companion object {
        private const val ENABLED = "enabled"

        private const val TRACK_CHECKED = "track_checked"
        private const val TRACK_OPTIONS = "track_options"

        private const val DOUBLE_UP_CHECKED = "double_up_checked"
        private const val DOUBLE_UP_OPTIONS = "double_up_options"
        private const val DOUBLE_UP_MODE = "double_up_mode"

        private const val DOUBLE_DOWN_CHECKED = "double_down_checked"
        private const val DOUBLE_DOWN_OPTIONS = "double_down_options"
        private const val DOUBLE_DOWN_MODE = "double_down_mode"

        private const val BROADCAST_0_ACTION = "broadcast_0_action"
        private const val BROADCAST_0_RECEIVER = "broadcast_0_receiver"
        private const val BROADCAST_0_KEY = "broadcast_0_key"
        private const val BROADCAST_0_VALUE = "broadcast_0_value"

        private const val BROADCAST_1_ACTION = "broadcast_1_action"
        private const val BROADCAST_1_RECEIVER = "broadcast_1_receiver"
        private const val BROADCAST_1_KEY = "broadcast_1_key"
        private const val BROADCAST_1_VALUE = "broadcast_1_value"

        private const val SHOW_PROMINENT_DISCLOSURE = "show_prominent_disclosure"

        // migration
        private const val SERVICE_ENABLED = "service_enabled"
        private const val FLASHLIGHT_CHECKED = "flashlight_checked"
    }

    private val prefs = PreferenceManager.getDefaultSharedPreferences(ctx)

    var isEnabled: Boolean
        get() = prefs.getBoolean(ENABLED, prefs.getBoolean(SERVICE_ENABLED, false))
        set(value) = prefs.edit { putBoolean(ENABLED, value) }

    var isTrackChecked: Boolean
        get() = prefs.getBoolean(TRACK_CHECKED, false)
        set(value) = prefs.edit { putBoolean(TRACK_CHECKED, value) }

    var trackOptions: Int
        get() = prefs.getInt(TRACK_OPTIONS, Option.VIBRATE.value)
        set(value) = prefs.edit { putInt(TRACK_OPTIONS, value) }

    var isDoubleUpChecked: Boolean
        get() = prefs.getBoolean(
            DOUBLE_UP_CHECKED,
            prefs.getBoolean(FLASHLIGHT_CHECKED, false),
        )
        set(value) = prefs.edit { putBoolean(DOUBLE_UP_CHECKED, value) }

    var doubleUpOptions: Int
        get() = prefs.getInt(DOUBLE_UP_OPTIONS, Option.VIBRATE.value)
        set(value) = prefs.edit { putInt(DOUBLE_UP_OPTIONS, value) }

    var doubleUpMode: Int
        get() = prefs.getInt(DOUBLE_UP_MODE, Mode.FLASHLIGHT.value)
        set(value) = prefs.edit { putInt(DOUBLE_UP_MODE, value) }

    var isDoubleDownChecked: Boolean
        get() = prefs.getBoolean(DOUBLE_DOWN_CHECKED, false)
        set(value) = prefs.edit { putBoolean(DOUBLE_DOWN_CHECKED, value) }

    var doubleDownOptions: Int
        get() = prefs.getInt(DOUBLE_DOWN_OPTIONS, Option.VIBRATE.value)
        set(value) = prefs.edit { putInt(DOUBLE_DOWN_OPTIONS, value) }

    var doubleDownMode: Int
        get() = prefs.getInt(DOUBLE_DOWN_MODE, Mode.FLASHLIGHT.value)
        set(value) = prefs.edit { putInt(DOUBLE_DOWN_MODE, value) }

    var isShowProminentDisclosure: Boolean
        get() = prefs.getBoolean(SHOW_PROMINENT_DISCLOSURE, true)
        set(value) = prefs.edit { putBoolean(SHOW_PROMINENT_DISCLOSURE, value) }

    var broadcast0Action: String
        get() = prefs.getString(BROADCAST_0_ACTION, "") ?: ""
        set(value) = prefs.edit { putString(BROADCAST_0_ACTION, value) }

    var broadcast0Receiver: String
        get() = prefs.getString(BROADCAST_0_RECEIVER, "") ?: ""
        set(value) = prefs.edit { putString(BROADCAST_0_RECEIVER, value) }

    var broadcast0Key: String
        get() = prefs.getString(BROADCAST_0_KEY, "") ?: ""
        set(value) = prefs.edit { putString(BROADCAST_0_KEY, value) }

    var broadcast0Value: String
        get() = prefs.getString(BROADCAST_0_VALUE, "") ?: ""
        set(value) = prefs.edit { putString(BROADCAST_0_VALUE, value) }

    var broadcast1Action: String
        get() = prefs.getString(BROADCAST_1_ACTION, "") ?: ""
        set(value) = prefs.edit { putString(BROADCAST_1_ACTION, value) }

    var broadcast1Receiver: String
        get() = prefs.getString(BROADCAST_1_RECEIVER, "") ?: ""
        set(value) = prefs.edit { putString(BROADCAST_1_RECEIVER, value) }

    var broadcast1Key: String
        get() = prefs.getString(BROADCAST_1_KEY, "") ?: ""
        set(value) = prefs.edit { putString(BROADCAST_1_KEY, value) }

    var broadcast1Value: String
        get() = prefs.getString(BROADCAST_1_VALUE, "") ?: ""
        set(value) = prefs.edit { putString(BROADCAST_1_VALUE, value) }
}

enum class Option(val value: Int) {
    VIBRATE(1),
}

enum class Mode(val value: Int) {
    FLASHLIGHT(0),
    BROADCAST(1),
}
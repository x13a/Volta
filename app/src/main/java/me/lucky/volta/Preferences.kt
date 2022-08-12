package me.lucky.volta

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager

class Preferences(ctx: Context) {
    companion object {
        private const val ENABLED = "enabled"

        private const val TRACK_ENABLED = "track_enabled"
        private const val TRACK_OPTIONS = "track_options"

        private const val DOUBLE_UP_ENABLED = "double_up_enabled"
        private const val DOUBLE_UP_OPTIONS = "double_up_options"
        private const val DOUBLE_UP_MODE = "double_up_mode"

        private const val DOUBLE_DOWN_ENABLED = "double_down_enabled"
        private const val DOUBLE_DOWN_OPTIONS = "double_down_options"
        private const val DOUBLE_DOWN_MODE = "double_down_mode"

        private const val BROADCAST_UP_ACTION = "broadcast_up_action"
        private const val BROADCAST_UP_RECEIVER = "broadcast_up_receiver"
        private const val BROADCAST_UP_EXTRA_KEY = "broadcast_up_extra_key"
        private const val BROADCAST_UP_EXTRA_VALUE = "broadcast_up_extra_value"

        private const val BROADCAST_DOWN_ACTION = "broadcast_down_action"
        private const val BROADCAST_DOWN_RECEIVER = "broadcast_down_receiver"
        private const val BROADCAST_DOWN_EXTRA_KEY = "broadcast_down_extra_key"
        private const val BROADCAST_DOWN_EXTRA_VALUE = "broadcast_down_extra_value"

        private const val SHOW_PROMINENT_DISCLOSURE = "show_prominent_disclosure"

        private const val LONG_PRESS_DURATION = "long_press_duration"
        private const val DOUBLE_PRESS_DURATION = "double_press_duration"
        private const val VIBE_DURATION = "vibe_duration"
        private const val TORCH_DISABLE_DELAY = "torch_disable_delay"

        private const val DEFAULT_LONG_PRESS_DURATION = 500L
        private const val DEFAULT_DOUBLE_PRESS_DURATION = 300L
        private const val DEFAULT_VIBE_DURATION = 200L
        private const val DEFAULT_TORCH_DISABLE_DELAY = 15

        // migration
        private const val SERVICE_ENABLED = "service_enabled"
        private const val FLASHLIGHT_CHECKED = "flashlight_checked"
        private const val TRACK_CHECKED = "track_checked"
        private const val DOUBLE_UP_CHECKED = "double_up_checked"
        private const val DOUBLE_DOWN_CHECKED = "double_down_checked"
        private const val BROADCAST_0_ACTION = "broadcast_0_action"
        private const val BROADCAST_0_RECEIVER = "broadcast_0_receiver"
        private const val BROADCAST_0_KEY = "broadcast_0_key"
        private const val BROADCAST_0_VALUE = "broadcast_0_value"
        private const val BROADCAST_1_ACTION = "broadcast_1_action"
        private const val BROADCAST_1_RECEIVER = "broadcast_1_receiver"
        private const val BROADCAST_1_KEY = "broadcast_1_key"
        private const val BROADCAST_1_VALUE = "broadcast_1_value"
    }

    private val prefs = PreferenceManager.getDefaultSharedPreferences(ctx)

    var isEnabled: Boolean
        get() = prefs.getBoolean(ENABLED, prefs.getBoolean(SERVICE_ENABLED, false))
        set(value) = prefs.edit { putBoolean(ENABLED, value) }

    var isTrackEnabled: Boolean
        get() = prefs.getBoolean(TRACK_ENABLED, prefs.getBoolean(TRACK_CHECKED, false))
        set(value) = prefs.edit { putBoolean(TRACK_ENABLED, value) }

    var trackOptions: Int
        get() = prefs.getInt(TRACK_OPTIONS, Option.VIBRATE.value)
        set(value) = prefs.edit { putInt(TRACK_OPTIONS, value) }

    var isDoubleUpEnabled: Boolean
        get() = prefs.getBoolean(
            DOUBLE_UP_ENABLED,
            prefs.getBoolean(
                DOUBLE_UP_CHECKED,
                prefs.getBoolean(FLASHLIGHT_CHECKED, false),
            )
        )
        set(value) = prefs.edit { putBoolean(DOUBLE_UP_ENABLED, value) }

    var doubleUpOptions: Int
        get() = prefs.getInt(DOUBLE_UP_OPTIONS, Option.VIBRATE.value)
        set(value) = prefs.edit { putInt(DOUBLE_UP_OPTIONS, value) }

    var doubleUpMode: Int
        get() = prefs.getInt(DOUBLE_UP_MODE, Mode.FLASHLIGHT.value)
        set(value) = prefs.edit { putInt(DOUBLE_UP_MODE, value) }

    var isDoubleDownEnabled: Boolean
        get() = prefs.getBoolean(
            DOUBLE_DOWN_ENABLED,
            prefs.getBoolean(DOUBLE_DOWN_CHECKED, false),
        )
        set(value) = prefs.edit { putBoolean(DOUBLE_DOWN_ENABLED, value) }

    var doubleDownOptions: Int
        get() = prefs.getInt(DOUBLE_DOWN_OPTIONS, Option.VIBRATE.value)
        set(value) = prefs.edit { putInt(DOUBLE_DOWN_OPTIONS, value) }

    var doubleDownMode: Int
        get() = prefs.getInt(DOUBLE_DOWN_MODE, Mode.FLASHLIGHT.value)
        set(value) = prefs.edit { putInt(DOUBLE_DOWN_MODE, value) }

    var isShowProminentDisclosure: Boolean
        get() = prefs.getBoolean(SHOW_PROMINENT_DISCLOSURE, true)
        set(value) = prefs.edit { putBoolean(SHOW_PROMINENT_DISCLOSURE, value) }

    var broadcastUpAction: String
        get() = prefs.getString(
            BROADCAST_UP_ACTION,
            prefs.getString(BROADCAST_0_ACTION, ""),
        ) ?: ""
        set(value) = prefs.edit { putString(BROADCAST_UP_ACTION, value) }

    var broadcastUpReceiver: String
        get() = prefs.getString(
            BROADCAST_UP_RECEIVER,
            prefs.getString(BROADCAST_0_RECEIVER, ""),
        ) ?: ""
        set(value) = prefs.edit { putString(BROADCAST_UP_RECEIVER, value) }

    var broadcastUpExtraKey: String
        get() = prefs.getString(
            BROADCAST_UP_EXTRA_KEY,
            prefs.getString(BROADCAST_0_KEY, ""),
        ) ?: ""
        set(value) = prefs.edit { putString(BROADCAST_UP_EXTRA_KEY, value) }

    var broadcastUpExtraValue: String
        get() = prefs.getString(
            BROADCAST_UP_EXTRA_VALUE,
            prefs.getString(BROADCAST_0_VALUE, ""),
        ) ?: ""
        set(value) = prefs.edit { putString(BROADCAST_UP_EXTRA_VALUE, value) }

    var broadcastDownAction: String
        get() = prefs.getString(
            BROADCAST_DOWN_ACTION,
            prefs.getString(BROADCAST_1_ACTION, ""),
        ) ?: ""
        set(value) = prefs.edit { putString(BROADCAST_DOWN_ACTION, value) }

    var broadcastDownReceiver: String
        get() = prefs.getString(
            BROADCAST_DOWN_RECEIVER,
            prefs.getString(BROADCAST_1_RECEIVER, ""),
        ) ?: ""
        set(value) = prefs.edit { putString(BROADCAST_DOWN_RECEIVER, value) }

    var broadcastDownExtraKey: String
        get() = prefs.getString(
            BROADCAST_DOWN_EXTRA_KEY,
            prefs.getString(BROADCAST_1_KEY, ""),
        ) ?: ""
        set(value) = prefs.edit { putString(BROADCAST_DOWN_EXTRA_KEY, value) }

    var broadcastDownExtraValue: String
        get() = prefs.getString(
            BROADCAST_DOWN_EXTRA_VALUE,
            prefs.getString(BROADCAST_1_VALUE, ""),
        ) ?: ""
        set(value) = prefs.edit { putString(BROADCAST_DOWN_EXTRA_VALUE, value) }

    var longPressDuration: Long
        get() = prefs.getLong(LONG_PRESS_DURATION, DEFAULT_LONG_PRESS_DURATION)
        set(value) = prefs.edit { putLong(LONG_PRESS_DURATION, value) }

    var doublePressDuration: Long
        get() = prefs.getLong(DOUBLE_PRESS_DURATION, DEFAULT_DOUBLE_PRESS_DURATION)
        set(value) = prefs.edit { putLong(DOUBLE_PRESS_DURATION, value) }

    var vibeDuration: Long
        get() = prefs.getLong(VIBE_DURATION, DEFAULT_VIBE_DURATION)
        set(value) = prefs.edit { putLong(VIBE_DURATION, value) }

    var torchDisableDelay: Int
        get() = prefs.getInt(TORCH_DISABLE_DELAY, DEFAULT_TORCH_DISABLE_DELAY)
        set(value) = prefs.edit { putInt(TORCH_DISABLE_DELAY, value) }
}

enum class Option(val value: Int) {
    VIBRATE(1),
}

enum class Mode(val value: Int) {
    FLASHLIGHT(0),
    BROADCAST(1),
}
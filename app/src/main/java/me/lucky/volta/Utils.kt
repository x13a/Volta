package me.lucky.volta

class Utils {
    companion object {
        fun setFlag(key: Int, value: Int, enabled: Boolean) =
            when(enabled) {
                true -> key.or(value)
                false -> key.and(value.inv())
            }
    }
}
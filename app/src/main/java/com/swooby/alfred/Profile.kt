package com.swooby.alfred

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import com.smartfoo.android.core.annotations.NonNullNonEmpty

class Profile
    (
    private val forcedOrder: Int,
    @field:NonNullNonEmpty @get:NonNullNonEmpty @param:NonNullNonEmpty val name: String,
    @field:NonNullNonEmpty @get:NonNullNonEmpty @param:NonNullNonEmpty val token: String
) {
    companion object {
        var COMPARATOR: Comparator<Profile> = Comparator { lhs: Profile, rhs: Profile ->
            if (lhs.forcedOrder != Int.MAX_VALUE) {
                val compare = lhs.forcedOrder.compareTo(rhs.forcedOrder)
                if (compare != 0) {
                    return@Comparator compare
                }
            }
            lhs.name.compareTo(rhs.name)
        }
    }

    object Tokens {
        const val DISABLED: String = "profile.disabled"
        const val HEADPHONES_WIRED: String = "profile.headphones_wired"
        const val HEADPHONES_BLUETOOTH_ANY: String = "profile.headphones_bluetooth_any"
        const val HEADPHONES_ANY: String = "profile.headphones_any"
        const val ALWAYS_ON: String = "profile.always_on"

        @JvmStatic
        fun isDisabled(value: String?): Boolean {
            return value == null || value == DISABLED
        }

        @JvmStatic
        fun isNotDisabled(value: String?): Boolean {
            return value != null && value != DISABLED
        }
    }

    @SuppressLint("MissingPermission")
    constructor(bluetoothDevice: BluetoothDevice) : this(
        Int.MAX_VALUE,
        bluetoothDevice.name,
        bluetoothDevice.address
    )

    override fun toString(): String {
        return name
    }

    override fun equals(other: Any?): Boolean {
        return other is Profile && token == other.token
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}

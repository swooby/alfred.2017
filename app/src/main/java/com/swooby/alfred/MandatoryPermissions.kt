package com.swooby.alfred

import android.content.Context
import com.smartfoo.android.core.logging.FooLog
import com.smartfoo.android.core.permissions.FooPermissionsChecker
import java.util.LinkedHashMap
import java.util.LinkedHashSet

/**
 * Tracks permission-gated initialisation routines that must succeed before Alfred can operate.
 *
 * Each mandatory permission is registered with a lambda that returns `true` when the
 * permission-dependent initialisation has completed. When a permission is missing the registered
 * listener is notified once. When the permission is granted (and the initialisation succeeds) a
 * matching "granted" callback is emitted so UI components can react accordingly.
 */
class MandatoryPermissions(
    private val context: Context,
    private val listener: Listener
) {

    interface Listener {
        fun onMandatoryPermissionRequired(permission: String)
        fun onMandatoryPermissionGranted(permission: String)
    }

    private data class Entry(
        val permission: String,
        val initializer: () -> Boolean,
        var isInitialized: Boolean = false,
        var hasNotifiedRequired: Boolean = false
    )

    private val entries = LinkedHashMap<String, Entry>()

    fun register(permission: String, initializer: () -> Boolean) {
        val existing = entries[permission]
        if (existing != null) {
            FooLog.w(TAG, "register: permission=" + permission + " already registered")
            return
        }
        entries[permission] = Entry(permission, initializer)
    }

    fun evaluate() {
        for (entry in entries.values) {
            updateEntry(entry)
        }
    }

    fun onPermissionGranted(permission: String) {
        val entry = entries[permission]
        if (entry != null) {
            updateEntry(entry)
        }
    }

    fun isPermissionMissing(permission: String): Boolean {
        val entry = entries[permission] ?: return false
        return !entry.isInitialized
    }

    fun missingPermissions(): Set<String> {
        val missing = LinkedHashSet<String>()
        for (entry in entries.values) {
            if (!entry.isInitialized) {
                missing += entry.permission
            }
        }
        return missing
    }

    private fun updateEntry(entry: Entry) {
        val isGranted = FooPermissionsChecker.isPermissionGranted(context, entry.permission)
        if (!isGranted) {
            if (!entry.hasNotifiedRequired) {
                listener.onMandatoryPermissionRequired(entry.permission)
                entry.hasNotifiedRequired = true
            }
            entry.isInitialized = false
            return
        }

        val wasInitialized = entry.isInitialized
        val initialised = try {
            entry.initializer()
        } catch (e: SecurityException) {
            FooLog.w(TAG, "initializer threw SecurityException for permission=" + entry.permission, e)
            false
        }

        entry.isInitialized = initialised
        if (initialised) {
            if (entry.hasNotifiedRequired) {
                listener.onMandatoryPermissionGranted(entry.permission)
            }
            entry.hasNotifiedRequired = false
        } else if (!entry.hasNotifiedRequired || wasInitialized) {
            listener.onMandatoryPermissionRequired(entry.permission)
            entry.hasNotifiedRequired = true
        }
    }

    companion object {
        private val TAG = FooLog.TAG(MandatoryPermissions::class.java)
    }
}

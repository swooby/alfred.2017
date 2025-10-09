package com.swooby.alfred

import android.content.Context
import com.smartfoo.android.core.app.FooDebugConfiguration
import com.smartfoo.android.core.content.FooPreferences
import com.smartfoo.android.core.logging.FooLog
import com.smartfoo.android.core.logging.FooLogFilePrinter
import com.smartfoo.android.core.logging.FooLogPrinter

class DebugPreferences
    (applicationContext: Context?) : FooPreferences(applicationContext),
    FooDebugConfiguration {
    override fun getDebugLogLimitKb(defaultValue: Int): Int {
        return getInt(FILE_NAME_APP, KEY_APP_DEBUG_LOG_LIMIT_KB, defaultValue)
    }

    override fun setDebugLogLimitKb(value: Int) {
        setInt(FILE_NAME_APP, KEY_APP_DEBUG_LOG_LIMIT_KB, value)
    }

    override fun getDebugLogEmailLimitKb(defaultValue: Int): Int {
        return getInt(FILE_NAME_APP, KEY_APP_DEBUG_LOG_EMAIL_LIMIT_KB, defaultValue)
    }

    override fun setDebugLogEmailLimitKb(value: Int) {
        setInt(FILE_NAME_APP, KEY_APP_DEBUG_LOG_EMAIL_LIMIT_KB, value)
    }

    override fun isDebugEnabled(): Boolean {
        return getBoolean(FILE_NAME_USER, KEY_USER_IS_DEBUG_ENABLED, false)
    }

    override fun setDebugEnabled(value: Boolean): Boolean {
        if (isDebugEnabled == value) {
            return false
        }

        setBoolean(FILE_NAME_USER, KEY_USER_IS_DEBUG_ENABLED, value)
        FooLog.isEnabled = value
        if (value) {
            debugToFileEnabled = debugToFileEnabled
        } else {
            setDebugToFileEnabled(value = false, save = false)
        }

        return true
    }

    override fun getDebugToFileEnabled(): Boolean {
        var isEnabled = getBoolean(FILE_NAME_USER, KEY_USER_IS_DEBUG_TO_FILE_ENABLED, true)
        if (isEnabled) {
            val logPrinter = FooLogFilePrinter.getInstance(mApplicationContext)
            isEnabled = logPrinter.isEnabled
        }
        return isEnabled
    }

    override fun setDebugToFileEnabled(value: Boolean) {
        setDebugToFileEnabled(value, true)
    }

    private fun setDebugToFileEnabled(value: Boolean, save: Boolean) {
        if (save) {
            setBoolean(FILE_NAME_USER, KEY_USER_IS_DEBUG_TO_FILE_ENABLED, value)
        }

        val logPrinter: FooLogPrinter = FooLogFilePrinter.getInstance(mApplicationContext)

        logPrinter.isEnabled = value

        if (value) {
            FooLog.addPrinter(logPrinter)
        }
    }

    companion object {
        private const val KEY_APP_DEBUG_LOG_LIMIT_KB = "pref_app_debug_log_limit_kb"
        private const val KEY_APP_DEBUG_LOG_EMAIL_LIMIT_KB = "pref_app_debug_log_email_limit_kb"

        //
        private const val KEY_USER_IS_DEBUG_ENABLED = "pref_user_debug_enabled"
        private const val KEY_USER_IS_DEBUG_TO_FILE_ENABLED = "pref_user_debug_to_file_enabled"
    }
}

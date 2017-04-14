package com.swooby.alfred;

import android.content.Context;

import com.smartfoo.android.core.content.FooPreferences;
import com.smartfoo.android.core.app.FooDebugConfiguration;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.logging.FooLogFilePrinter;
import com.smartfoo.android.core.logging.FooLogPrinter;

public class DebugPreferences
        extends FooPreferences
        implements FooDebugConfiguration
{
    private static final String KEY_APP_DEBUG_LOG_LIMIT_KB        = "pref_app_debug_log_limit_kb";
    private static final String KEY_APP_DEBUG_LOG_EMAIL_LIMIT_KB  = "pref_app_debug_log_email_limit_kb";
    //
    private static final String KEY_USER_IS_DEBUG_ENABLED         = "pref_user_debug_enabled";
    private static final String KEY_USER_IS_DEBUG_TO_FILE_ENABLED = "pref_user_debug_to_file_enabled";

    public DebugPreferences(Context applicationContext)
    {
        super(applicationContext);
    }

    @Override
    public int getDebugLogLimitKb(int defaultValue)
    {
        return getInt(FILE_NAME_APP, KEY_APP_DEBUG_LOG_LIMIT_KB, defaultValue);
    }

    @Override
    public void setDebugLogLimitKb(int value)
    {
        setInt(FILE_NAME_APP, KEY_APP_DEBUG_LOG_LIMIT_KB, value);
    }

    @Override
    public int getDebugLogEmailLimitKb(int defaultValue)
    {
        return getInt(FILE_NAME_APP, KEY_APP_DEBUG_LOG_EMAIL_LIMIT_KB, defaultValue);
    }

    @Override
    public void setDebugLogEmailLimitKb(int value)
    {
        setInt(FILE_NAME_APP, KEY_APP_DEBUG_LOG_EMAIL_LIMIT_KB, value);
    }

    @Override
    public boolean isDebugEnabled()
    {
        return getBoolean(FILE_NAME_USER, KEY_USER_IS_DEBUG_ENABLED, false);
    }

    @Override
    public boolean setDebugEnabled(boolean value)
    {
        if (isDebugEnabled() == value)
        {
            return false;
        }

        setBoolean(FILE_NAME_USER, KEY_USER_IS_DEBUG_ENABLED, value);
        FooLog.setEnabled(value);
        if (value)
        {
            setDebugToFileEnabled(getDebugToFileEnabled());
        }
        else
        {
            setDebugToFileEnabled(false, false);
        }

        return true;
    }

    @Override
    public boolean getDebugToFileEnabled()
    {
        boolean isEnabled = getBoolean(FILE_NAME_USER, KEY_USER_IS_DEBUG_TO_FILE_ENABLED, true);
        if (isEnabled)
        {
            FooLogPrinter logPrinter = FooLogFilePrinter.getInstance(mApplicationContext);

            //noinspection ConstantConditions
            isEnabled &= logPrinter.isEnabled();
        }

        return isEnabled;
    }

    @Override
    public void setDebugToFileEnabled(boolean value)
    {
        setDebugToFileEnabled(value, true);
    }

    private void setDebugToFileEnabled(boolean value, boolean save)
    {
        if (save)
        {
            setBoolean(FILE_NAME_USER, KEY_USER_IS_DEBUG_TO_FILE_ENABLED, value);
        }

        FooLogPrinter logPrinter = FooLogFilePrinter.getInstance(mApplicationContext);

        logPrinter.setEnabled(value);

        if (value)
        {
            FooLog.addPrinter(logPrinter);
        }
    }
}

package com.swooby.alfred.notification.parsers;

import android.service.notification.StatusBarNotification;

import androidx.annotation.NonNull;

import com.smartfoo.android.core.logging.FooLog;

public class DefaultNotificationParser
        extends AbstractNotificationParser
{
    private static final String TAG = FooLog.TAG(DefaultNotificationParser.class);

    public DefaultNotificationParser(@NonNull NotificationParserCallbacks callbacks)
    {
        super("#DEFAULT", callbacks);
    }

    @Override
    public String getPackageName()
    {
        return "*";
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn)
    {
        //FooLog.v(TAG, "onNotificationRemoved: " + sbn);
    }
}

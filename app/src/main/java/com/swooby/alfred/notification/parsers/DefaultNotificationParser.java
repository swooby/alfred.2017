package com.swooby.alfred.notification.parsers;

import android.content.Context;
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
    public NotificationParseResult onNotificationPosted(StatusBarNotification sbn)
    {
        Context context = mCallbacks.getContext();
        //noinspection UnnecessaryLocalVariable
        NotificationParseResult result = AbstractNotificationParser.defaultOnNotificationPosted(context, sbn, getTextToSpeech());
        //...
        return result;
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn)
    {
        //FooLog.v(TAG, "onNotificationRemoved: " + sbn);
    }
}

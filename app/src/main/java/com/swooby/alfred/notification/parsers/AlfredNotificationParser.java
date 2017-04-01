package com.swooby.alfred.notification.parsers;

import android.service.notification.StatusBarNotification;
import android.support.annotation.NonNull;

import com.smartfoo.android.core.logging.FooLog;

public class AlfredNotificationParser
        extends AbstractNotificationParser
{
    private static final String TAG = FooLog.TAG(AlfredNotificationParser.class);

    public AlfredNotificationParser(@NonNull NotificationParserCallbacks callbacks)
    {
        super("#ALFRED", callbacks);
    }

    @Override
    public String getPackageName()
    {
        return "com.swooby.alfred";
    }

    @Override
    protected boolean getSpeakDefaultNotification()
    {
        return false;
    }

    @Override
    public NotificationParseResult onNotificationPosted(StatusBarNotification sbn)
    {
        return NotificationParseResult.ParsableIgnored;
    }
}

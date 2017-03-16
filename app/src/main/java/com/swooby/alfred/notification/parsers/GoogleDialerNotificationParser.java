package com.swooby.alfred.notification.parsers;

import android.service.notification.StatusBarNotification;
import android.support.annotation.NonNull;

public class GoogleDialerNotificationParser
        extends AbstractNotificationParser
{
    public GoogleDialerNotificationParser(@NonNull NotificationParserCallbacks callbacks)
    {
        super(callbacks);//, "Google Dialer");
    }

    @Override
    public String getPackageName()
    {
        return "com.google.android.dialer";
    }

    @Override
    protected boolean getSpeakDefaultNotification()
    {
        // TODO:(pv) return false once we implement custom handling below
        return true;
    }

    @Override
    public NotificationParseResult onNotificationPosted(StatusBarNotification sbn)
    {
        // TODO:(pv) This needs to better pronounce incoming phone numbers
        return super.onNotificationPosted(sbn);
    }
}

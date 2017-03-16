package com.swooby.alfred.notification.parsers;

import android.service.notification.StatusBarNotification;
import android.support.annotation.NonNull;

public class GoogleMessengerNotificationParser
        extends AbstractNotificationParser
{
    public GoogleMessengerNotificationParser(@NonNull NotificationParserCallbacks callbacks)
    {
        super(callbacks);//, "Google Messenger");
    }

    @Override
    public String getPackageName()
    {
        return "com.google.android.apps.messaging";
    }

    @Override
    public NotificationParseResult onNotificationPosted(StatusBarNotification sbn)
    {
        return super.onNotificationPosted(sbn);
    }
}

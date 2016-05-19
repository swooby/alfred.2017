package com.swooby.alfred.notification.parsers;

import android.service.notification.StatusBarNotification;

import com.swooby.alfred.MainApplication;

public class GoogleMessengerNotificationParser
        extends AbstractNotificationParser
{
    public GoogleMessengerNotificationParser(MainApplication application)
    {
        super(application, "com.google.android.apps.messaging");//, "Google Messenger");
    }

    @Override
    public NotificationParseResult onNotificationPosted(StatusBarNotification sbn)
    {
        return super.onNotificationPosted(sbn);
    }
}

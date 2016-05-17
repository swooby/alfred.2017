package com.swooby.alfred.notification;

import android.service.notification.StatusBarNotification;

import com.swooby.alfred.MainApplication;

public class GoogleMessengerNotificationParser
        extends AbstractNotificationParser
{
    protected GoogleMessengerNotificationParser(MainApplication application)
    {
        super(application, "com.google.android.apps.messaging", "Google Messenger");
    }

    @Override
    public boolean onNotificationPosted(StatusBarNotification sbn)
    {
        return super.onNotificationPosted(sbn);
    }
}

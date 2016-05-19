package com.swooby.alfred.notification.parsers;

import android.service.notification.StatusBarNotification;

import com.swooby.alfred.MainApplication;

public class GoogleDialerNotificationParser
        extends AbstractNotificationParser
{
    public GoogleDialerNotificationParser(MainApplication application)
    {
        super(application, "com.google.android.dialer");//, "Google Dialer");
    }

    @Override
    public NotificationParseResult onNotificationPosted(StatusBarNotification sbn)
    {
        return super.onNotificationPosted(sbn);
    }
}

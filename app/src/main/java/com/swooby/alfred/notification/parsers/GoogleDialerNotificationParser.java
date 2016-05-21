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

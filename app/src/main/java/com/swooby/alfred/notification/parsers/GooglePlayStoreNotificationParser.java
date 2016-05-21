package com.swooby.alfred.notification.parsers;

import android.service.notification.StatusBarNotification;

import com.swooby.alfred.MainApplication;

public class GooglePlayStoreNotificationParser
        extends AbstractNotificationParser
{
    public GooglePlayStoreNotificationParser(MainApplication application)
    {
        super(application, "com.android.vending");//, "Google Play Store");
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
        // TODO:(pv) This needs to ignore app update incremental progress
        return super.onNotificationPosted(sbn);
    }
}

package com.swooby.alfred.notification.parsers;

import android.service.notification.StatusBarNotification;
import android.support.annotation.NonNull;

public class GooglePlayStoreNotificationParser
        extends AbstractNotificationParser
{
    public GooglePlayStoreNotificationParser(@NonNull NotificationParserCallbacks callbacks)
    {
        super("#GOOGLEPLAYSTORE", callbacks);//, "Google Play Store");
    }

    @Override
    public String getPackageName()
    {
        return "com.android.vending";
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

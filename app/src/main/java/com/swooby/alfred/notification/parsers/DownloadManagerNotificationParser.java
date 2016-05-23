package com.swooby.alfred.notification.parsers;

import android.service.notification.StatusBarNotification;

import com.swooby.alfred.MainApplication;

public class DownloadManagerNotificationParser
        extends AbstractNotificationParser
{
    public DownloadManagerNotificationParser(MainApplication application)
    {
        super(application, "com.android.providers.downloads");//, "Google Play Store");
    }

    @Override
    protected boolean getSpeakDefaultNotification()
    {
        // TODO:(pv) return false once we implement custom handling below
        return false;
    }

    @Override
    public NotificationParseResult onNotificationPosted(StatusBarNotification sbn)
    {
        // TODO:(pv) This needs to ignore app update incremental progress
        return super.onNotificationPosted(sbn);
    }
}

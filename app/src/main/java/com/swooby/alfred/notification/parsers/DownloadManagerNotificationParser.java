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
    public NotificationParseResult onNotificationPosted(StatusBarNotification sbn)
    {
        //
        // Ignore non-useful app update incremental progress notification
        //
        super.onNotificationPosted(sbn);

        return NotificationParseResult.ParsableIgnored;
    }
}

package com.swooby.alfred.notification.parsers;

import android.service.notification.StatusBarNotification;
import android.support.annotation.NonNull;

public class DownloadManagerNotificationParser
        extends AbstractNotificationParser
{
    public DownloadManagerNotificationParser(@NonNull NotificationParserCallbacks callbacks)
    {
        super(callbacks);//, "Google Play Store");
    }

    @Override
    public String getPackageName()
    {
        return "com.android.providers.downloads";
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

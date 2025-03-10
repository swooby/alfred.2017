package com.swooby.alfred.notification.parsers;

import androidx.annotation.NonNull;

public class ChromeNotificationParser
        extends AbstractNotificationParser
{
    public ChromeNotificationParser(@NonNull NotificationParserCallbacks callbacks)
    {
        super("#CHROME", callbacks);//, "Chrome");
    }

    @Override
    public String getPackageName()
    {
        return "com.android.chrome";
    }

    @Override
    protected boolean getSpeakDefaultNotification()
    {
        return false;
    }
}

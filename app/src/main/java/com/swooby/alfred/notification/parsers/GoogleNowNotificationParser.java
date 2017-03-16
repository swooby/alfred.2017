package com.swooby.alfred.notification.parsers;

import android.support.annotation.NonNull;

public class GoogleNowNotificationParser
        extends AbstractNotificationParser
{
    public GoogleNowNotificationParser(@NonNull NotificationParserCallbacks callbacks)
    {
        super(callbacks);
    }

    @Override
    public String getPackageName()
    {
        return "com.google.android.googlequicksearchbox";
    }

    @Override
    public String getPackageAppSpokenName()
    {
        return "Google Now";//getString(...);
    }

    @Override
    protected boolean getSpeakDefaultNotification()
    {
        return true;
    }
}

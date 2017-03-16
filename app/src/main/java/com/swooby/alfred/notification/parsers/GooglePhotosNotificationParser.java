package com.swooby.alfred.notification.parsers;

import android.support.annotation.NonNull;

public class GooglePhotosNotificationParser
        extends AbstractNotificationParser
{
    public GooglePhotosNotificationParser(@NonNull NotificationParserCallbacks callbacks)
    {
        super(callbacks);
    }

    @Override
    public String getPackageName()
    {
        return "com.google.android.apps.photos";
    }

    @Override
    public String getPackageAppSpokenName()
    {
        return "Google Photos";//getString(...);
    }
}

package com.swooby.alfred.notification.parsers;

import com.swooby.alfred.MainApplication;

public class GooglePhotosNotificationParser
        extends AbstractNotificationParser
{
    public GooglePhotosNotificationParser(MainApplication application)
    {
        super(application, "com.google.android.apps.photos", "Google Photos");
    }
}

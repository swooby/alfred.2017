package com.swooby.alfred.notification.parsers;

import com.swooby.alfred.MainApplication;

public class GoogleNowNotificationParser
        extends AbstractNotificationParser
{
    public GoogleNowNotificationParser(MainApplication application)
    {
        super(application, "com.google.android.googlequicksearchbox", "Google Now");
    }

    @Override
    protected boolean getSpeakDefaultNotification()
    {
        return true;
    }
}

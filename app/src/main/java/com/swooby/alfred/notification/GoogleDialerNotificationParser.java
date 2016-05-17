package com.swooby.alfred.notification;

import android.service.notification.StatusBarNotification;

import com.smartfoo.android.core.texttospeech.FooTextToSpeech;
import com.swooby.alfred.MainApplication;

public class GoogleDialerNotificationParser
        extends AbstractNotificationParser
{
    protected GoogleDialerNotificationParser(MainApplication application, FooTextToSpeech textToSpeech)
    {
        super(application, "com.google.android.dialer", "Google Dialer");
    }

    @Override
    public boolean onNotificationPosted(StatusBarNotification sbn)
    {
        return super.onNotificationPosted(sbn);
    }
}

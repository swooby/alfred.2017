package com.swooby.alfred.notification;

import android.content.Context;
import android.service.notification.StatusBarNotification;

import com.smartfoo.android.core.texttospeech.FooTextToSpeech;

public class GoogleDialerNotificationParser
        extends AbstractNotificationParser
{
    protected GoogleDialerNotificationParser(Context context, FooTextToSpeech textToSpeech)
    {
        super(context, textToSpeech, "com.google.android.dialer", "Google Dialer");
    }

    @Override
    public boolean onNotificationPosted(StatusBarNotification sbn)
    {
        return super.onNotificationPosted(sbn);
    }
}

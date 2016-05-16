package com.swooby.alfred.notification;

import android.content.Context;
import android.service.notification.StatusBarNotification;

import com.smartfoo.android.core.texttospeech.FooTextToSpeech;

public class GoogleMessengerNotificationParser
        extends AbstractNotificationParser
{
    protected GoogleMessengerNotificationParser(Context context, FooTextToSpeech textToSpeech)
    {
        super(context, textToSpeech, "com.google.android.apps.messaging", "Google Messenger");
    }

    @Override
    public boolean onNotificationPosted(StatusBarNotification sbn)
    {
        return super.onNotificationPosted(sbn);
    }
}

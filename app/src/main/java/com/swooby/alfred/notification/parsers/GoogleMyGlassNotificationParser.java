package com.swooby.alfred.notification.parsers;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.support.annotation.NonNull;

import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.texttospeech.FooTextToSpeechBuilder;
import com.swooby.alfred.BuildConfig;

public class GoogleMyGlassNotificationParser
        extends AbstractNotificationParser
{
    private static final String TAG = FooLog.TAG(GoogleMyGlassNotificationParser.class);

    public GoogleMyGlassNotificationParser(@NonNull NotificationParserCallbacks callbacks)
    {
        super("#GOOGLEGLASS", callbacks);
    }

    @Override
    public String getPackageName()
    {
        return "com.google.glass.companion";
    }

    @Override
    public NotificationParseResult onNotificationPosted(StatusBarNotification sbn)
    {
        FooLog.i(TAG, "---- " + mLogPrefix + " ----");
        if (BuildConfig.DEBUG)
        {
            super.onNotificationPosted(sbn);
        }

        Notification notification = sbn.getNotification();
        if (notification == null)
        {
            FooLog.w(TAG, "onNotificationPosted: " + mLogPrefix + " notification == null; Unparsable");
            return NotificationParseResult.Unparsable;
        }

        Bundle extras = notification.extras;
        if (extras == null)
        {
            FooLog.w(TAG, "onNotificationPosted: " + mLogPrefix + " extras == null; Unparsable");
            return NotificationParseResult.Unparsable;
        }

        CharSequence androidTitle = extras.getCharSequence(Notification.EXTRA_TITLE);
        FooLog.v(TAG, "onNotificationPosted: " + mLogPrefix + " androidTitle == " + FooString.quote(androidTitle));

        String packageAppSpokenName = getPackageAppSpokenName();

        FooTextToSpeechBuilder builder = new FooTextToSpeechBuilder(packageAppSpokenName)
                .appendSilenceSentenceBreak()
                .appendSpeech(androidTitle);

        getTextToSpeech().speak(builder);

        return NotificationParseResult.ParsableHandled;
    }
}

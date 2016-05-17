package com.swooby.alfred.notification;

import android.service.notification.StatusBarNotification;
import android.support.annotation.NonNull;

import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.notification.FooNotificationListener.FooNotificationListenerCallbacks;
import com.swooby.alfred.MainApplication;

import java.util.HashMap;
import java.util.Map;

// TODO:(pv) Make a UI that shows all StatusBarNotification fields, especially:
//  Notification.tickerText
//  All ImageView Resource Ids and TextView Texts in BigContentView
//  All ImageView Resource Ids and TextView Texts in ContentView
//  The user could then select what to say for what images, and prefixing for suffixing texts

public class AppNotificationListener
        implements FooNotificationListenerCallbacks
{
    private static final String TAG = FooLog.TAG(AppNotificationListener.class);

    private final MainApplication                         mApplication;
    private final Map<String, AbstractNotificationParser> mNotificationParsers;

    public AppNotificationListener(
            @NonNull
            MainApplication application)
    {
        mApplication = application;
        mNotificationParsers = new HashMap<>();

        addNotificationParser(new PandoraNotificationParser(mApplication));//, mTextToSpeech));
        addNotificationParser(new SpotifyNotificationParser(mApplication));//, mTextToSpeech));
        addNotificationParser(new GoogleHangoutsNotificationParser(mApplication));//, mTextToSpeech));
        addNotificationParser(new GmailNotificationParser(mApplication));//, mTextToSpeech));
        addNotificationParser(new GoogleMessengerNotificationParser(mApplication));//, mTextToSpeech));
    }

    private void addNotificationParser(AbstractNotificationParser notificationParser)
    {
        mNotificationParsers.put(notificationParser.getPackageName(), notificationParser);
    }

    @Override
    public void onCreate()
    {
        mApplication.speak("Listening for Notifications");
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn)
    {
        String packageName = AbstractNotificationParser.getPackageName(sbn);
        FooLog.d(TAG, "onNotificationPosted: packageName=" + FooString.quote(packageName));

        boolean handled;

        AbstractNotificationParser notificationParser = mNotificationParsers.get(packageName);
        if (notificationParser != null)
        {
            handled = notificationParser.onNotificationPosted(sbn);
        }
        else
        {
            handled = AbstractNotificationParser.defaultOnNotificationPosted(mApplication, sbn, null);
        }

        if (handled)
        {
            mApplication.silence(500);
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn)
    {
        String packageName = sbn.getPackageName();
        FooLog.d(TAG, "onNotificationRemoved: packageName=" + FooString.quote(packageName));

        AbstractNotificationParser notificationParser = mNotificationParsers.get(packageName);
        if (notificationParser != null)
        {
            // TODO:(pv) Reset any cache in the parser
            notificationParser.onNotificationRemoved(sbn);
        }
    }
}

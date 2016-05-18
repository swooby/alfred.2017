package com.swooby.alfred.notification;

import android.service.notification.StatusBarNotification;
import android.support.annotation.NonNull;

import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.notification.FooNotificationListener.FooNotificationListenerCallbacks;
import com.swooby.alfred.MainApplication;
import com.swooby.alfred.R;

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
    public void onNotificationListenerBound()
    {
        mApplication.speak("Listening for Notifications");
    }

    @Override
    public void onNotificationListenerUnbound()
    {
        String appName = mApplication.getString(R.string.app_name);
        String text = mApplication.getString(R.string.notification_access_is_disabled_please_enable, appName);
        mApplication.speak(text);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn)
    {
        String packageName = AbstractNotificationParser.getPackageName(sbn);
        FooLog.d(TAG, "onNotificationPosted: packageName=" + FooString.quote(packageName));

        AbstractNotificationParser notificationParser = mNotificationParsers.get(packageName);
        if (notificationParser != null)
        {
            notificationParser.onNotificationPosted(sbn);
        }
        else
        {
            AbstractNotificationParser.defaultOnNotificationPosted(mApplication, sbn, null);
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

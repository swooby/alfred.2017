package com.swooby.alfred.notification;

import android.service.notification.StatusBarNotification;
import android.support.annotation.NonNull;

import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.notification.FooNotificationListener.FooNotificationListenerCallbacks;
import com.swooby.alfred.MainApplication;
import com.swooby.alfred.R;
import com.swooby.alfred.notification.parsers.AbstractNotificationParser;
import com.swooby.alfred.notification.parsers.AbstractNotificationParser.NotificationParseResult;
import com.swooby.alfred.notification.parsers.GooglePhotosNotificationParser;
import com.swooby.alfred.notification.parsers.PandoraNotificationParser;
import com.swooby.alfred.notification.parsers.SpotifyNotificationParser;

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

        addNotificationParser(new GooglePhotosNotificationParser(mApplication));
        addNotificationParser(new PandoraNotificationParser(mApplication));
        addNotificationParser(new SpotifyNotificationParser(mApplication));
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
        FooLog.v(TAG, "onNotificationPosted: packageName=" + FooString.quote(packageName));

        NotificationParseResult result;

        AbstractNotificationParser notificationParser = mNotificationParsers.get(packageName);
        if (notificationParser != null)
        {
            result = notificationParser.onNotificationPosted(sbn);
        }
        else
        {
            result = AbstractNotificationParser.defaultOnNotificationPosted(mApplication, sbn, null);
        }

        switch (result)
        {
            case DefaultWithTickerText:
            case DefaultWithoutTickerText:
                break;
            case Unparsable:
                FooLog.w(TAG, "onNotificationPosted: Unparsable StatusBarNotification");
                break;
            case ParsableHandled:
            case ParsableIgnored:
                break;
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

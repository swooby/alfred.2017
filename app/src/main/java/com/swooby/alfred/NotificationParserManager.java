package com.swooby.alfred;

import android.content.Context;
import android.service.notification.StatusBarNotification;
import android.support.annotation.NonNull;

import com.smartfoo.android.core.FooListenerManager;
import com.smartfoo.android.core.FooRun;
import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.notification.FooNotificationListenerManager;
import com.smartfoo.android.core.notification.FooNotificationListenerManager.FooNotificationListenerManagerCallbacks;
import com.swooby.alfred.notification.parsers.AbstractNotificationParser;
import com.swooby.alfred.notification.parsers.AbstractNotificationParser.NotificationParseResult;
import com.swooby.alfred.notification.parsers.AbstractNotificationParser.NotificationParserCallbacks;
import com.swooby.alfred.notification.parsers.DownloadManagerNotificationParser;
import com.swooby.alfred.notification.parsers.GoogleCameraNotificationParser;
import com.swooby.alfred.notification.parsers.GoogleDialerNotificationParser;
import com.swooby.alfred.notification.parsers.GoogleHangoutsNotificationParser;
import com.swooby.alfred.notification.parsers.GoogleNowNotificationParser;
import com.swooby.alfred.notification.parsers.GooglePhotosNotificationParser;
import com.swooby.alfred.notification.parsers.GooglePlayStoreNotificationParser;
import com.swooby.alfred.notification.parsers.PandoraNotificationParser;
import com.swooby.alfred.notification.parsers.SpotifyNotificationParser;

import java.util.HashMap;
import java.util.Map;

public class NotificationParserManager
{
    private static final String TAG = FooLog.TAG(NotificationParserManager.class);

    public interface NotificationParserManagerConfiguration
    {
        @NonNull
        Context getContext();

        boolean isEnabled();

        @NonNull
        TextToSpeechManager getTextToSpeech();
    }

    public interface NotificationParserManagerCallbacks
    {
        void onNotificationListenerBound();

        void onNotificationListenerAccessDisabled();
    }

    private final NotificationParserManagerConfiguration                 mConfiguration;
    private final FooListenerManager<NotificationParserManagerCallbacks> mListenerManager;
    private final FooNotificationListenerManager                         mFooNotificationListenerManager;
    private final FooNotificationListenerManagerCallbacks                mFooNotificationListenerManagerCallbacks;
    private final NotificationParserCallbacks                            mNotificationParserCallbacks;
    private final Map<String, AbstractNotificationParser>                mNotificationParsers;

    private boolean mIsInitialized;

    public NotificationParserManager(@NonNull NotificationParserManagerConfiguration configuration)
    {
        FooLog.v(TAG, "+NotificationParserManager(...)");

        FooRun.throwIllegalArgumentExceptionIfNull(configuration, "configuration");

        mConfiguration = configuration;

        mListenerManager = new FooListenerManager<>();

        mFooNotificationListenerManager = FooNotificationListenerManager.getInstance();

        mFooNotificationListenerManagerCallbacks = new FooNotificationListenerManagerCallbacks()
        {
            @Override
            public void onNotificationListenerBound()
            {
                NotificationParserManager.this.onNotificationListenerBound();
            }

            @Override
            public void onNotificationListenerUnbound()
            {
                NotificationParserManager.this.onNotificationListenerUnbound();
            }

            @Override
            public void onNotificationPosted(StatusBarNotification sbn)
            {
                NotificationParserManager.this.onNotificationPosted(sbn);
            }

            @Override
            public void onNotificationRemoved(StatusBarNotification sbn)
            {
                NotificationParserManager.this.onNotificationRemoved(sbn);
            }
        };

        mNotificationParserCallbacks = new NotificationParserCallbacks()
        {
            @Override
            public Context getContext()
            {
                return NotificationParserManager.this.getContext();
            }

            @Override
            public TextToSpeechManager getTextToSpeech()
            {
                return NotificationParserManager.this.getTextToSpeech();
            }
        };

        mNotificationParsers = new HashMap<>();

        FooLog.v(TAG, "-NotificationParserManager(...)");
    }

    @NonNull
    private Context getContext()
    {
        return mConfiguration.getContext();
    }

    @NonNull
    private TextToSpeechManager getTextToSpeech()
    {
        return mConfiguration.getTextToSpeech();
    }

    private boolean isEnabled()
    {
        return mConfiguration.isEnabled();
    }

    public boolean isInitialized()
    {
        return mIsInitialized;
    }

    /**
     * NOTE:(pv) USE CAUTIOUSLY! The Notification Listener can still fail to bind [due to
     * http://stackoverflow.com/a/37081128/252308] even if the setting is AND SHOWS as enabled.
     *
     * @return true if the OS Notification Access Setting is enable for this app's context
     */
    public boolean isNotificationAccessSettingUnverifiedEnabled()
    {
        return FooNotificationListenerManager.isNotificationAccessSettingUnverifiedEnabled(getContext());
    }

    /**
     * @return true if the Notification Listener successfully bound
     */
    public boolean isNotificationListenerBound()
    {
        return mFooNotificationListenerManager.isNotificationListenerBound();
    }

    /**
     * NOTE: Used to determine if the Notification Access Setting is enabled but mismatches its actual state [due to
     * issue http://stackoverflow.com/a/37081128/252308]
     *
     * @return true if the Notification Access Setting is enabled for the context *AND* this Notification Listener
     * successfully bound
     */
    public boolean isNotificationAccessSettingEnabledAndNotBound()
    {
        return isNotificationAccessSettingUnverifiedEnabled() && !isNotificationListenerBound();
    }

    public void startActivityNotificationListenerSettings(@NonNull Context context)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(context, "context");
        mFooNotificationListenerManager.startActivityNotificationListenerSettings(context);
    }

    public void refresh()
    {
        mFooNotificationListenerManager.refresh();
    }

    public void attach(NotificationParserManagerCallbacks callbacks)
    {
        mListenerManager.attach(callbacks);

        if (mListenerManager.size() == 1 && mNotificationParsers.size() == 0)
        {
            start();
        }
    }

    public void detach(NotificationParserManagerCallbacks callbacks)
    {
        mListenerManager.detach(callbacks);
    }

    private void addNotificationParser(@NonNull AbstractNotificationParser notificationParser)
    {
        mNotificationParsers.put(notificationParser.getPackageName(), notificationParser);
    }

    private void start()
    {
        //
        // We can't just blindly initialize all AbstractNotificationParsers.
        //  If we do, then we won't be able to detect the application name for any apps that are not installed...
        //  ...and I would rather not hardcode the application name.
        //  If someone then installs the app, then we will have initialized an empty/null application name, which isn't good.
        // The best general solution is:
        //  1) Have a general method that initializes a parser if its app is installed
        //  2) At startup, detect any apps that are installed and initialize its parser
        //  3) Listen for any installed/uninstalled apps and respectively add/removed any appropriate parser
        //

        // TODO:(pv) Listen for installation/removal of any installed apps
        // TODO:(pv) Enumerate all AbstractNotificationParsers

        // TODO:(pv) Use package reflection to enumerate and load all non-Abstract parsers in parsers package
        // TODO:(pv) Not all of these are required, and could rely on a decent default implementation that walks and talks all visible text elements
        //addNotificationParser(new AndroidSystemNotificationParser(mNotificationParserCallbacks));
        //addNotificationParser(new FacebookNotificationParser(mNotificationParserCallbacks));
        //addNotificationParser(new GmailNotificationParser(mNotificationParserCallbacks));
        addNotificationParser(new DownloadManagerNotificationParser(mNotificationParserCallbacks));
        addNotificationParser(new GoogleCameraNotificationParser(mNotificationParserCallbacks));
        addNotificationParser(new GoogleDialerNotificationParser(mNotificationParserCallbacks));
        addNotificationParser(new GoogleHangoutsNotificationParser(mNotificationParserCallbacks));
        //addNotificationParser(new GoogleMapsNotificationParser(mNotificationParserCallbacks));
        //addNotificationParser(new GoogleMessengerNotificationParser(mNotificationParserCallbacks));
        addNotificationParser(new GoogleNowNotificationParser(mNotificationParserCallbacks));
        addNotificationParser(new GooglePhotosNotificationParser(mNotificationParserCallbacks));
        addNotificationParser(new GooglePlayStoreNotificationParser(mNotificationParserCallbacks));
        addNotificationParser(new PandoraNotificationParser(mNotificationParserCallbacks));
        //addNotificationParser(new RedboxNotificationParser(mNotificationParserCallbacks));
        addNotificationParser(new SpotifyNotificationParser(mNotificationParserCallbacks));

        mFooNotificationListenerManager.attach(mFooNotificationListenerManagerCallbacks);
    }

    private void onNotificationListenerBound()
    {
        FooLog.i(TAG, "onNotificationListenerBound()");
        mIsInitialized = true;
        for (NotificationParserManagerCallbacks callbacks : mListenerManager.beginTraversing())
        {
            callbacks.onNotificationListenerBound();
        }
        mListenerManager.endTraversing();
    }

    private void onNotificationListenerUnbound()
    {
        FooLog.i(TAG, "onNotificationListenerUnbound()");
        mIsInitialized = true;
        for (NotificationParserManagerCallbacks callbacks : mListenerManager.beginTraversing())
        {
            callbacks.onNotificationListenerAccessDisabled();
        }
        mListenerManager.endTraversing();
    }

    private void onNotificationPosted(StatusBarNotification sbn)
    {
        if (!isEnabled())
        {
            FooLog.w(TAG, "onNotificationPosted: isEnabled() == false; ignoring");
            return;
        }

        String packageName = AbstractNotificationParser.getPackageName(sbn);
        FooLog.v(TAG, "onNotificationPosted: packageName=" + FooString.quote(packageName));

        NotificationParseResult result;

        AbstractNotificationParser notificationParser = mNotificationParsers.get(packageName);
        if (notificationParser == null)
        {
            result = AbstractNotificationParser.defaultOnNotificationPosted(getContext(), sbn, getTextToSpeech());
        }
        else
        {
            result = notificationParser.onNotificationPosted(sbn);
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

    private void onNotificationRemoved(StatusBarNotification sbn)
    {
        if (!isEnabled())
        {
            FooLog.w(TAG, "onNotificationRemoved: isEnabled() == false; ignoring");
            return;
        }

        String packageName = AbstractNotificationParser.getPackageName(sbn);
        FooLog.d(TAG, "onNotificationRemoved: packageName=" + FooString.quote(packageName));

        AbstractNotificationParser notificationParser = mNotificationParsers.get(packageName);
        if (notificationParser == null)
        {
            return;
        }

        // TODO:(pv) Reset any cache in the parser...
        notificationParser.onNotificationRemoved(sbn);
    }
}

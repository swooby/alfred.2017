package com.swooby.alfred;

import android.app.Notification;
import android.content.Context;
import android.os.UserHandle;
import android.service.notification.StatusBarNotification;
import android.support.annotation.NonNull;

import com.smartfoo.android.core.FooListenerAutoStartManager;
import com.smartfoo.android.core.FooListenerAutoStartManager.FooListenerAutoStartManagerCallbacks;
import com.smartfoo.android.core.FooRun;
import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.notification.FooNotificationListenerManager;
import com.smartfoo.android.core.notification.FooNotificationListenerManager.FooNotificationListenerManagerCallbacks;
import com.smartfoo.android.core.notification.FooNotificationListenerManager.NotConnectedReason;
import com.swooby.alfred.notification.parsers.AbstractNotificationParser;
import com.swooby.alfred.notification.parsers.AbstractNotificationParser.NotificationParseResult;
import com.swooby.alfred.notification.parsers.AbstractNotificationParser.NotificationParserCallbacks;
import com.swooby.alfred.notification.parsers.AlfredNotificationParser;
import com.swooby.alfred.notification.parsers.DownloadManagerNotificationParser;
import com.swooby.alfred.notification.parsers.GoogleCameraNotificationParser;
import com.swooby.alfred.notification.parsers.GoogleDialerNotificationParser;
import com.swooby.alfred.notification.parsers.GoogleHangoutsNotificationParser;
import com.swooby.alfred.notification.parsers.GoogleMyGlassNotificationParser;
import com.swooby.alfred.notification.parsers.GoogleNowNotificationParser;
import com.swooby.alfred.notification.parsers.GooglePhotosNotificationParser;
import com.swooby.alfred.notification.parsers.GooglePlayStoreNotificationParser;
import com.swooby.alfred.notification.parsers.NotificationParserUtils;
import com.swooby.alfred.notification.parsers.PandoraNotificationParser;
import com.swooby.alfred.notification.parsers.SpotifyNotificationParser;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class NotificationParserManager
{
    private static final String TAG = FooLog.TAG(NotificationParserManager.class);

    public interface NotificationParserManagerConfiguration
    {
        boolean isNotificationParserEnabled();

        @NonNull
        TextToSpeechManager getTextToSpeech();
    }

    public interface NotificationParserManagerCallbacks
    {
        boolean onNotificationListenerConnected(StatusBarNotification[] activeNotifications);

        void onNotificationListenerNotConnected(NotConnectedReason reason, long elapsedMillis);

        void onNotificationParsed(@NonNull AbstractNotificationParser parser);
    }

    private final Context                                                         mContext;
    private final NotificationParserManagerConfiguration                          mConfiguration;
    private final FooListenerAutoStartManager<NotificationParserManagerCallbacks> mListenerManager;
    private final FooNotificationListenerManager                                  mFooNotificationListenerManager;
    private final FooNotificationListenerManagerCallbacks                         mFooNotificationListenerManagerCallbacks;
    private final NotificationParserCallbacks                                     mNotificationParserCallbacks;
    private final Map<String, AbstractNotificationParser>                         mNotificationParsers;

    private boolean mIsInitialized;

    public NotificationParserManager(@NonNull Context context, @NonNull NotificationParserManagerConfiguration configuration)
    {
        FooLog.v(TAG, "+NotificationParserManager(...)");

        mContext = FooRun.toNonNull(context, "context");
        mConfiguration = FooRun.toNonNull(configuration, "configuration");

        mListenerManager = new FooListenerAutoStartManager<>(this);
        mListenerManager.attach(new FooListenerAutoStartManagerCallbacks()
        {
            @Override
            public void onFirstAttach()
            {
                if (mNotificationParsers.size() == 0)
                {
                    start();
                }
            }

            @Override
            public boolean onLastDetach()
            {
                return false;
            }
        });

        mFooNotificationListenerManager = FooNotificationListenerManager.getInstance();

        mFooNotificationListenerManagerCallbacks = new FooNotificationListenerManagerCallbacks()
        {
            @Override
            public boolean onNotificationListenerConnected(@NonNull StatusBarNotification[] activeNotifications)
            {
                return NotificationParserManager.this.onNotificationListenerConnected(activeNotifications);
            }

            @Override
            public void onNotificationListenerNotConnected(@NonNull NotConnectedReason reason, long elapsedMillis)
            {
                NotificationParserManager.this.onNotificationListenerNotConnected(reason, elapsedMillis);
            }

            @Override
            public void onNotificationPosted(@NonNull StatusBarNotification sbn)
            {
                NotificationParserManager.this.onNotificationPosted(sbn);
            }

            @Override
            public void onNotificationRemoved(@NonNull StatusBarNotification sbn)
            {
                NotificationParserManager.this.onNotificationRemoved(sbn);
            }
        };

        mNotificationParserCallbacks = new NotificationParserCallbacks()
        {
            @NonNull
            @Override
            public Context getContext()
            {
                return mContext;
            }

            @NonNull
            @Override
            public TextToSpeechManager getTextToSpeech()
            {
                return NotificationParserManager.this.getTextToSpeech();
            }

            @Override
            public void onNotificationParsed(@NonNull AbstractNotificationParser parser)
            {
                NotificationParserManager.this.onNotificationParsed(parser);
            }
        };

        mNotificationParsers = new HashMap<>();

        FooLog.v(TAG, "-NotificationParserManager(...)");
    }

    @NonNull
    private TextToSpeechManager getTextToSpeech()
    {
        return mConfiguration.getTextToSpeech();
    }

    private boolean isEnabled()
    {
        return mConfiguration.isNotificationParserEnabled();
    }

    public boolean isInitialized()
    {
        return mIsInitialized;
    }

    public boolean isNotificationAccessSettingConfirmedNotEnabled()
    {
        return FooNotificationListenerManager.isNotificationAccessSettingConfirmedNotEnabled(mContext);
    }

    public boolean isNotificationListenerConnected()
    {
        return mFooNotificationListenerManager.isNotificationListenerConnected();
    }

    public void startActivityNotificationListenerSettings()
    {
        FooNotificationListenerManager.startActivityNotificationListenerSettings(mContext);
    }

    public void initializeActiveNotifications()
    {
        StatusBarNotification[] activeNotifications = mFooNotificationListenerManager.getActiveNotifications();
        List<StatusBarNotification> prioritizedActiveNotifications = prioritizeNotifications(activeNotifications);
        for (StatusBarNotification activeNotification : prioritizedActiveNotifications)
        {
            onNotificationPosted(activeNotification);
        }
    }

    @NonNull
    private List<StatusBarNotification> prioritizeNotifications(StatusBarNotification[] statusBarNotifications)
    {
        List<StatusBarNotification> prioritized = new LinkedList<>();
        if (statusBarNotifications != null)
        {
            String packageNameSelf = mContext.getPackageName();
            for (StatusBarNotification statusBarNotification : statusBarNotifications)
            {
                String packageName = statusBarNotification.getPackageName();
                if (packageName.equals(packageNameSelf))
                {
                    Notification notification = statusBarNotification.getNotification();
                    if ((notification.flags & Notification.FLAG_ONGOING_EVENT) == Notification.FLAG_ONGOING_EVENT)
                    {
                        prioritized.add(0, statusBarNotification);
                        continue;
                    }
                }
                prioritized.add(statusBarNotification);
            }
        }
        return prioritized;
    }

    public void attach(NotificationParserManagerCallbacks callbacks)
    {
        mListenerManager.attach(callbacks);
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
        // TODO:(pv) In DEBUG, show any parsers that do not have app installed w/ link to install app from Google Play
        // TODO:(pv) Listen for installation/removal of apps (especially ones w/ parsers)
        // TODO:(pv) Future ecosystem to allow installing 3rd-party developed parsers
        // TODO:(pv) Use package reflection to enumerate and load all non-Abstract parsers in parsers package
        // TODO:(pv) Not all of these parsers may be required, and could rely on a decent default implementation that walks and talks all visible text elements
        addNotificationParser(new AlfredNotificationParser(mNotificationParserCallbacks));
        //addNotificationParser(new AndroidSystemNotificationParser(mNotificationParserCallbacks));
        //addNotificationParser(new FacebookNotificationParser(mNotificationParserCallbacks));
        //addNotificationParser(new GmailNotificationParser(mNotificationParserCallbacks));
        addNotificationParser(new DownloadManagerNotificationParser(mNotificationParserCallbacks));
        addNotificationParser(new GoogleCameraNotificationParser(mNotificationParserCallbacks));
        addNotificationParser(new GoogleDialerNotificationParser(mNotificationParserCallbacks));
        addNotificationParser(new GoogleHangoutsNotificationParser(mNotificationParserCallbacks));
        //addNotificationParser(new GoogleMapsNotificationParser(mNotificationParserCallbacks));
        //addNotificationParser(new GoogleMessengerNotificationParser(mNotificationParserCallbacks));
        addNotificationParser(new GoogleMyGlassNotificationParser(mNotificationParserCallbacks));
        addNotificationParser(new GoogleNowNotificationParser(mNotificationParserCallbacks));
        addNotificationParser(new GooglePhotosNotificationParser(mNotificationParserCallbacks));
        addNotificationParser(new GooglePlayStoreNotificationParser(mNotificationParserCallbacks));
        addNotificationParser(new PandoraNotificationParser(mNotificationParserCallbacks));
        //addNotificationParser(new RedboxNotificationParser(mNotificationParserCallbacks));
        addNotificationParser(new SpotifyNotificationParser(mNotificationParserCallbacks));

        mFooNotificationListenerManager.attach(mContext, mFooNotificationListenerManagerCallbacks);
    }

    private boolean onNotificationListenerConnected(StatusBarNotification[] activeNotifications)
    {
        FooLog.i(TAG, "onNotificationListenerConnected(...)");
        mIsInitialized = true;
        boolean handled = false;
        for (NotificationParserManagerCallbacks callbacks : mListenerManager.beginTraversing())
        {
            handled |= callbacks.onNotificationListenerConnected(activeNotifications);
        }
        mListenerManager.endTraversing();

        if (!handled)
        {
            initializeActiveNotifications();
        }

        return true;
    }

    private void onNotificationListenerNotConnected(NotConnectedReason reason, long elapsedMillis)
    {
        FooLog.i(TAG, "onNotificationListenerNotConnected(reason=" + reason + ", elapsedMillis=" + elapsedMillis + ')');
        mIsInitialized = true;
        for (NotificationParserManagerCallbacks callbacks : mListenerManager.beginTraversing())
        {
            callbacks.onNotificationListenerNotConnected(reason, elapsedMillis);
        }
        mListenerManager.endTraversing();
    }

    private void onNotificationPosted(StatusBarNotification sbn)
    {
        if (!isEnabled())
        {
            FooLog.v(TAG, "onNotificationPosted: isEnabled() == false; ignoring");
            return;
        }

        String packageName = NotificationParserUtils.getPackageName(sbn);
        FooLog.v(TAG, "onNotificationPosted: packageName=" + FooString.quote(packageName));

        NotificationParseResult result;

        AbstractNotificationParser notificationParser = mNotificationParsers.get(packageName);
        if (notificationParser == null)
        {
            result = AbstractNotificationParser.defaultOnNotificationPosted(mContext, sbn, getTextToSpeech());
        }
        else
        {
            result = notificationParser.onNotificationPosted(sbn);
        }

        switch (result)
        {
            case UnparsedIgnored:
            case DefaultWithTickerText:
            case DefaultWithoutTickerText:
                break;
            case Unparsable:
                FooLog.w(TAG, "onNotificationPosted: Unparsable StatusBarNotification");
                break;
            case ParsedHandled:
            case ParsedIgnored:
                break;
        }
    }

    private void onNotificationRemoved(StatusBarNotification sbn)
    {
        if (!isEnabled())
        {
            FooLog.v(TAG, "onNotificationRemoved: isEnabled() == false; ignoring");
            return;
        }

        String packageName = NotificationParserUtils.getPackageName(sbn);
        FooLog.d(TAG, "onNotificationRemoved: packageName=" + FooString.quote(packageName));

        AbstractNotificationParser notificationParser = mNotificationParsers.get(packageName);
        if (notificationParser == null)
        {
            return;
        }

        // TODO:(pv) Reset any cache in the parser…
        notificationParser.onNotificationRemoved(sbn);
    }

    private void onNotificationParsed(AbstractNotificationParser parser)
    {
        for (NotificationParserManagerCallbacks callbacks : mListenerManager.beginTraversing())
        {
            callbacks.onNotificationParsed(parser);
        }
        mListenerManager.endTraversing();
    }
}

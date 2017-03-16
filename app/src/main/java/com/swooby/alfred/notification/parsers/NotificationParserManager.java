package com.swooby.alfred.notification.parsers;

import android.app.PendingIntent;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.service.notification.StatusBarNotification;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;

import com.smartfoo.android.core.FooRun;
import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.notification.FooNotification;
import com.smartfoo.android.core.notification.FooNotificationBuilder;
import com.smartfoo.android.core.notification.FooNotificationListener;
import com.smartfoo.android.core.notification.FooNotificationListener.FooNotificationListenerCallbacks;
import com.smartfoo.android.core.notification.FooNotificationReceiver;
import com.swooby.alfred.MainActivity;
import com.swooby.alfred.R;
import com.swooby.alfred.TextToSpeechManager;
import com.swooby.alfred.notification.parsers.AbstractNotificationParser.NotificationParseResult;
import com.swooby.alfred.notification.parsers.AbstractNotificationParser.NotificationParserCallbacks;

import java.util.HashMap;
import java.util.Map;

public class NotificationParserManager
{
    private static final String TAG = FooLog.TAG(NotificationParserManager.class);

    public interface NotificationParserManagerCallbacks
    {
        @NonNull
        Context getContext();

        boolean isEnabled();

        @NonNull
        TextToSpeechManager getTextToSpeech();
    }

    private final FooNotificationListenerCallbacks        mFooNotificationListenerCallbacks;
    private final NotificationParserCallbacks             mNotificationParserCallbacks;
    private final Map<String, AbstractNotificationParser> mNotificationParsers;

    private NotificationParserManagerCallbacks mCallbacks;

    public NotificationParserManager(@NonNull NotificationParserManagerCallbacks callbacks)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(callbacks, "callbacks");

        mCallbacks = callbacks;

        mFooNotificationListenerCallbacks = new FooNotificationListenerCallbacks()
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
    }

    @NonNull
    protected Context getContext()
    {
        return mCallbacks.getContext();
    }

    @NonNull
    protected TextToSpeechManager getTextToSpeech()
    {
        return mCallbacks.getTextToSpeech();
    }

    public boolean isEnabled()
    {
        return mCallbacks.isEnabled();
    }

    private void addNotificationParser(@NonNull AbstractNotificationParser notificationParser)
    {
        mNotificationParsers.put(notificationParser.getPackageName(), notificationParser);
    }

    public void initialize()
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
        addNotificationParser(new GoogleMapsNotificationParser(mNotificationParserCallbacks));
        //addNotificationParser(new GoogleMessengerNotificationParser(mNotificationParserCallbacks));
        addNotificationParser(new GoogleNowNotificationParser(mNotificationParserCallbacks));
        addNotificationParser(new GooglePhotosNotificationParser(mNotificationParserCallbacks));
        addNotificationParser(new GooglePlayStoreNotificationParser(mNotificationParserCallbacks));
        addNotificationParser(new PandoraNotificationParser(mNotificationParserCallbacks));
        //addNotificationParser(new RedboxNotificationParser(mNotificationParserCallbacks));
        addNotificationParser(new SpotifyNotificationParser(mNotificationParserCallbacks));

        FooNotificationListener.addListener(mFooNotificationListenerCallbacks);

        //
        // MainApplication always starts first, before FooNotificationListener has any chance to bind.
        // After MainApplication, if FooNotificationListener binds then it will call onNotificationListenerBound().
        // On first run it will not bind because the user has not enabled the settings.
        // Normally we would just directly call FooNotificationListener.isNotificationAccessSettingEnabled(Context context).
        // Unfortunately, sometimes NotificationAccess is configured to be enabled, but FooNotificationListener never binds.
        // This almost always happens when re-installing the app between development builds.
        // NOTE:(pv) It is unknown if this is also an issue when the app does production updates through Google Play.
        // Since we cannot reliably test for isNotificationAccessSettingEnabled, the next best thing is to timeout if
        // FooNotificationListener does not bind within a small amount of time (we are using 250ms).
        // If FooNotificationListener does not bind and call onNotificationListenerBound() within 250ms then we need to
        // prompt the user to enable Notification Access.
        //

        // HACK required to detect non-binding when re-installing app even if notification access is enabled:
        //  http://stackoverflow.com/a/37081128/252308
        //
        new Handler().postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                if (!FooNotificationListener.isNotificationListenerBound())
                {
                    onNotificationListenerUnbound();
                }
            }
        }, 250);
    }

    private void onNotificationListenerBound()
    {
        // TODO:(pv) This is where the real app logic actually starts!
        FooLog.i(TAG, "onNotificationListenerBound()");
        ///updateEnabledState();
    }

    private void onNotificationListenerUnbound()
    {
        FooLog.i(TAG, "onNotificationListenerUnbound()");

        //updateEnabledState();

        boolean handled = false;

        /*
        Set<MainApplicationListener> listeners = mListenerManager.beginTraversing();
        for (MainApplicationListener listener : listeners)
        {
            handled = listener.onNotificationListenerAccessDisabled();
            if (handled)
            {
                break;
            }
        }
        mListenerManager.endTraversing();
        */

        if (!handled)
        {
            // TODO:(pv) show notification

            Context context = getContext();

            int notificationRequestCode = 100;
            String contentTitle = context.getString(R.string.app_name);
            String notificationContentText = "Testing 1 2 3...";

            Bundle extras = new Bundle();
            //extras.putString(FooNotificationReceiver.EXTRA_DEVICE_MAC_ADDRESS, macAddress);
            PendingIntent deleteIntent = FooNotification.createPendingIntent(context, notificationRequestCode, FooNotificationReceiver.class, 0, 0, null);

            FooNotificationBuilder builder = new FooNotificationBuilder(context)
                    .setContentIntent(FooNotification.createPendingIntent(context, notificationRequestCode, MainActivity.class))
                    .setSmallIcon(R.drawable.ic_warning_white_18dp)
                    .setAutoCancel(true)
                    .setContentTitle(contentTitle)
                    .setContentText(notificationContentText)
                    .setDeleteIntent(deleteIntent);

            // TODO:(pv) Experimental Android Wear notification...
            //noinspection ConstantConditions
            if (false)
            {
                NotificationCompat.WearableExtender extender = new NotificationCompat.WearableExtender();
                NotificationCompat.Action actionDelete = new NotificationCompat.Action.Builder(R.drawable.ic_warning_white_18dp, "Snooze", deleteIntent)
                        .build();
                extender.addAction(actionDelete);
                builder.extend(extender);
            }

            FooNotification notification = new FooNotification(notificationRequestCode, builder);

            FooLog.v(TAG, "onNotificationListenerUnbound: notification=" + notification + ')');
            notification.show(context);
        }
    }

    private void onNotificationPosted(StatusBarNotification sbn)
    {
        if (!isEnabled())
        {
            FooLog.w(TAG, "onNotificationPosted: mIsEnabled == false; ignoring");
            return;
        }

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
            result = AbstractNotificationParser.defaultOnNotificationPosted(getContext(), sbn, mCallbacks.getTextToSpeech());
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
            FooLog.w(TAG, "onNotificationRemoved: mIsEnabled == false; ignoring");
            return;
        }

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

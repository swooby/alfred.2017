package com.swooby.alfred;

import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.smartfoo.android.core.FooListenerManager;
import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.notification.FooNotification;
import com.smartfoo.android.core.notification.FooNotificationBuilder;
import com.swooby.alfred.HeadsetManager.HeadsetManagerCallbacks;
import com.swooby.alfred.HeadsetManager.HeadsetManagerConfiguration;
import com.swooby.alfred.NotificationParserManager.NotificationParserManagerCallbacks;
import com.swooby.alfred.NotificationParserManager.NotificationParserManagerConfiguration;
import com.swooby.alfred.Profile.Tokens;
import com.swooby.alfred.TextToSpeechManager.TextToSpeechManagerCallbacks;
import com.swooby.alfred.TextToSpeechManager.TextToSpeechManagerConfiguration;

public class MainApplication
        extends Application
{
    private static final String TAG = FooLog.TAG(MainApplication.class);

    public interface MainApplicationCallbacks
    {
        boolean onNotificationListenerAccessDisabled();
    }

    private final FooListenerManager<MainApplicationCallbacks> mListenerManager;
    private final TextToSpeechManager                          mTextToSpeechManager;
    private final TextToSpeechManagerCallbacks                 mTextToSpeechManagerCallbacks;
    private final NotificationParserManager                    mNotificationParserManager;
    private final NotificationParserManagerCallbacks           mNotificationParserManagerCallbacks;
    private final HeadsetManager                               mHeadsetManager;
    private final HeadsetManagerCallbacks                      mHeadsetManagerCallbacks;

    private AppPreferences mAppPreferences;

    private boolean mIsEnabled;


    public MainApplication()
    {
        mListenerManager = new FooListenerManager<>();

        mTextToSpeechManager = new TextToSpeechManager(new TextToSpeechManagerConfiguration()
        {
            @NonNull
            @Override
            public Context getContext()
            {
                return MainApplication.this;
            }

            @Override
            public String getVoiceName()
            {
                return mAppPreferences.getTextToSpeechVoiceName();
            }

            @Override
            public int getAudioStreamType()
            {
                return mAppPreferences.getTextToSpeechAudioStreamType();
            }

            @Override
            public boolean isEnabled()
            {
                return MainApplication.this.isEnabled();
            }
        });
        mTextToSpeechManagerCallbacks = new TextToSpeechManagerCallbacks()
        {
            @Override
            public void onTextToSpeechVoiceNameSet(String voiceName)
            {
                mAppPreferences.setTextToSpeechVoiceName(voiceName);
            }
        };

        mNotificationParserManager = new NotificationParserManager(new NotificationParserManagerConfiguration()
        {
            @NonNull
            @Override
            public Context getContext()
            {
                return MainApplication.this;
            }

            @Override
            public boolean isEnabled()
            {
                return MainApplication.this.isEnabled();
            }

            @NonNull
            @Override
            public TextToSpeechManager getTextToSpeech()
            {
                return MainApplication.this.getTextToSpeechManager();
            }
        });
        mNotificationParserManagerCallbacks = new NotificationParserManagerCallbacks()
        {
            @Override
            public void onNotificationListenerBound()
            {
                MainApplication.this.onNotificationListenerBound();
            }

            @Override
            public void onNotificationListenerAccessDisabled()
            {
                MainApplication.this.onNotificationListenerAccessDisabled();
            }
        };

        mHeadsetManager = new HeadsetManager(new HeadsetManagerConfiguration()
        {
            @NonNull
            @Override
            public Context getContext()
            {
                return MainApplication.this;
            }

            @NonNull
            @Override
            public TextToSpeechManager getTextToSpeech()
            {
                return MainApplication.this.getTextToSpeechManager();
            }
        });
        mHeadsetManagerCallbacks = new HeadsetManagerCallbacks()
        {
            @Override
            public void onHeadsetConnectionChanged(boolean isConnected)
            {
                MainApplication.this.onHeadsetConnectionChanged(isConnected);
            }
        };
    }

    @NonNull
    public TextToSpeechManager getTextToSpeechManager()
    {
        return mTextToSpeechManager;
    }

    @NonNull
    public NotificationParserManager getNotificationParserManager()
    {
        return mNotificationParserManager;
    }

    @NonNull
    public HeadsetManager getHeadsetManager()
    {
        return mHeadsetManager;
    }

    public AppPreferences getAppPreferences()
    {
        return mAppPreferences;
    }

    /*
    public FooBluetoothManager getBluetoothManager()
    {
        return mBluetoothManager;
    }
    */

    private boolean isEnabled()
    {
        return mIsEnabled;
    }

    public void attach(MainApplicationCallbacks callbacks)
    {
        mListenerManager.attach(callbacks);
    }

    public void detach(MainApplicationCallbacks callbacks)
    {
        mListenerManager.detach(callbacks);
    }

    @Override
    public void onCreate()
    {
        FooLog.i(TAG, "+onCreate()");
        super.onCreate();

        //
        // Initialize Context dependent dependencies first
        //
        mAppPreferences = new AppPreferences(this);

        //
        // Initialize dependants of the above
        //
        mTextToSpeechManager.attach(mTextToSpeechManagerCallbacks);
        mHeadsetManager.attach(mHeadsetManagerCallbacks);
        mNotificationParserManager.attach(mNotificationParserManagerCallbacks);

        /*
        if (!updateHeadsetState())
        {
            updateEnabledState();
        }
        */

        /*
        if (!isRecognitionAvailable())
        {
            // TODO:(pv) Better place for initialization and indication of failure...
            //speak(true, true, "Speech recognition is not available for this device.");
            //speak(true, true, "Goodbye.");
            return;
        }
        */

        //mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        FooLog.i(TAG, "-onCreate()");
    }

    private void onNotificationListenerBound()
    {
        //updateEnabledState();

    }

    private void onNotificationListenerAccessDisabled()
    {
        //updateEnabledState();

        boolean handled = false;

        for (MainApplicationCallbacks callbacks : mListenerManager.beginTraversing())
        {
            handled = callbacks.onNotificationListenerAccessDisabled();
            if (handled)
            {
                break;
            }
        }
        mListenerManager.endTraversing();

        if (handled)
        {
            return;
        }

        //
        // Show notification...
        //

        int notificationRequestCode = 100;
        String contentTitle = getString(R.string.app_name);
        String notificationContentText = "Notification Access Disabled: Touch to enable...";

        PendingIntent contentIntent = FooNotification.createPendingIntent(this, notificationRequestCode, MainActivity.class);

        FooNotificationBuilder builder = new FooNotificationBuilder(this)
                .setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.ic_warning_white_18dp)
                .setAutoCancel(true)
                .setContentTitle(contentTitle)
                .setContentText(notificationContentText);

        /*
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
        */

        FooNotification notification = new FooNotification(notificationRequestCode, builder);

        FooLog.v(TAG, "onNotificationListenerUnbound: notification=" + notification + ')');
        notification.show(this);
    }

    private void onHeadsetConnectionChanged(boolean isConnected)
    {
        updateEnabledState();
    }

    private boolean updateEnabledState()
    {
        FooLog.v(TAG, "updateEnabledState()");

        boolean isEnabled = mNotificationParserManager.isNotificationListenerBound();
        if (isEnabled)
        {
            String profileToken = mAppPreferences.getProfileToken();
            switch (profileToken)
            {
                case Tokens.DISABLED:
                    isEnabled = false;
                    break;
                case Tokens.ALWAYS_ON:
                    isEnabled = true;
                    break;
                case Tokens.HEADPHONES_ONLY:
                    isEnabled = mHeadsetManager.isHeadsetConnected();
                    break;
                default:
                    throw new IllegalArgumentException("unhandled profileToken=" + FooString.quote(profileToken));
            }
        }

        if (mIsEnabled != isEnabled)
        {
            FooLog.w(TAG, "updateEnabledState: mIsEnabled != isEnabled; " +
                          (isEnabled ? "ENABLING" : "DISABLING"));

            mIsEnabled = isEnabled;
        }

        return mIsEnabled;
    }

    private void onEnabled()
    {
        mTextToSpeechManager.speak("Alfred enabled");
    }

    private void onDisabled()
    {
    }

    /*
    private boolean isProfileTokenHeadphones()
    {
        return Tokens.HEADPHONES_ONLY.equals(mAppPreferences.getProfileToken());
    }
    */

    /*
    public boolean isRecognitionAvailable()
    {
        return SpeechRecognizer.isRecognitionAvailable(this);
    }
    */
}

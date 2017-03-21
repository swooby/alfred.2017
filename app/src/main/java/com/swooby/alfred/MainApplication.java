package com.swooby.alfred;

import android.app.Activity;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import com.smartfoo.android.core.FooListenerManager;
import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.notification.FooNotification;
import com.smartfoo.android.core.notification.FooNotificationBuilder;
import com.smartfoo.android.core.notification.FooNotificationListenerManager;
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
        Activity getActivity();

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

    private Boolean mIsEnabled;

    public MainApplication()
    {
        FooLog.v(TAG, "+MainApplication()");

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

        FooLog.v(TAG, "-MainApplication()");
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
        return mIsEnabled != null && mIsEnabled;
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
        FooLog.v(TAG, "+onCreate()");
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

        FooLog.v(TAG, "-onCreate()");
    }

    private void onNotificationListenerBound()
    {
        FooLog.i(TAG, "onNotificationListenerBound()");
        updateEnabledState("onNotificationListenerBound");
    }

    private void onNotificationListenerAccessDisabled()
    {
        FooLog.w(TAG, "onNotificationListenerAccessDisabled()");
        updateEnabledState("onNotificationListenerAccessDisabled");
    }

    private void onHeadsetConnectionChanged(boolean isConnected)
    {
        FooLog.i(TAG, "onHeadsetConnectionChanged(isConnected=" + isConnected + ')');
        updateEnabledState("onHeadsetConnectionChanged");
    }

    private boolean updateEnabledState(String debugInfo)
    {
        FooLog.v(TAG, "updateEnabledState(debugInfo=" + FooString.quote(debugInfo) + ')');

        if (!mNotificationParserManager.isInitialized())
        {
            return false;
        }

        boolean isNotificationListenerBound = mNotificationParserManager.isNotificationListenerBound();
        FooLog.v(TAG, "updateEnabledState: isNotificationListenerBound == " + isNotificationListenerBound);

        String profileToken = mAppPreferences.getProfileToken();
        FooLog.v(TAG, "updateEnabledState: profileToken == " + FooString.quote(profileToken));

        boolean isProfileEnabled;
        switch (profileToken)
        {
            case Tokens.ALWAYS_ON:
                isProfileEnabled = true;
                break;
            case Tokens.HEADPHONES_ONLY:
                isProfileEnabled = mHeadsetManager.isHeadsetConnected();
                break;
            case Tokens.DISABLED:
            default:
                isProfileEnabled = false;
                break;
        }
        FooLog.v(TAG, "updateEnabledState: isProfileEnabled == " + isProfileEnabled);

        boolean isEnabled = isNotificationListenerBound && isProfileEnabled;

        if (mIsEnabled != null && mIsEnabled == isEnabled)
        {
            return false;
        }

        FooLog.i(TAG, "updateEnabledState: mIsEnabled != isEnabled; " + (isEnabled ? "ENABLING" : "DISABLING"));

        mIsEnabled = isEnabled;

        if (mIsEnabled)
        {
            onEnabled();
        }
        else
        {
            onDisabled();
        }

        return true;
    }

    private void onEnabled()
    {
        String text = getString(R.string.alfred_enabled);
        mTextToSpeechManager.speak(text);

        //...
    }

    private void onDisabled()
    {
        //
        // Allow the UI to optionally report on its own...
        //

        boolean headless = true;
        boolean handled = false;

        for (MainApplicationCallbacks callbacks : mListenerManager.beginTraversing())
        {
            headless &= callbacks.getActivity() == null;
            handled |= callbacks.onNotificationListenerAccessDisabled();
        }
        mListenerManager.endTraversing();

        boolean isNotificationListenerBound = mNotificationParserManager.isNotificationListenerBound();
        FooLog.v(TAG, "onDisabled: isNotificationListenerBound == " + isNotificationListenerBound);

        //
        // Always speak some things [if enabled]...
        //

        if (!isNotificationListenerBound)
        {
            String title = getNotificationAccessTitle();
            String message = getNotificationAccessMessage();
            String separator = getString(R.string.sentence_separator);
            String text = FooString.join(separator, title, message);
            mTextToSpeechManager.speak(true, headless, text);
        }

        if (handled)
        {
            return;
        }

        //
        // Report if not aborted by the UI...
        //

        if (!isNotificationListenerBound)
        {
            //
            // Show notification...
            //

            int notificationRequestCode = 100;
            String contentTitle = getString(R.string.app_name);
            String contentText = getNotificationAccessTitle();
            String subText = getString(R.string.tap_to_configure);

            Intent intentNotificationListenerSettings = FooNotificationListenerManager.getIntentNotificationListenerSettings();
            PendingIntent contentIntent = PendingIntent.getActivity(this, 1, intentNotificationListenerSettings, 0);

            FooNotificationBuilder builder = new FooNotificationBuilder(this)
                    .setContentIntent(contentIntent)
                    .setSmallIcon(R.drawable.ic_warning_white_18dp)
                    .setAutoCancel(true)
                    .setContentTitle(contentTitle)
                    .setContentText(contentText)
                    .setSubText(subText);

            FooNotification notification = new FooNotification(notificationRequestCode, builder);

            FooLog.v(TAG, "onNotificationListenerUnbound: notification=" + notification + ')');
            notification.show(this);

            return;
        }

        //...
    }

    public String getNotificationAccessTitle()
    {
        int resId;
        if (mNotificationParserManager.isNotificationAccessSettingEnabledAndNotBound())
        {
            resId = R.string.notification_access_mismatch;
        }
        else
        {
            resId = R.string.notification_access_disabled;
        }
        return getString(resId);
    }

    public String getNotificationAccessMessage()
    {
        int resId;
        if (mNotificationParserManager.isNotificationAccessSettingEnabledAndNotBound())
        {
            resId = R.string.please_reenable_notification_access_for_the_X_application;
        }
        else
        {
            resId = R.string.please_enable_notification_access_for_the_X_application;
        }

        String appName = getString(R.string.app_name);

        return getString(resId, appName);
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

package com.swooby.alfred;

import android.app.Activity;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import com.smartfoo.android.core.FooBoolean;
import com.smartfoo.android.core.FooListenerManager;
import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.notification.FooNotification;
import com.smartfoo.android.core.notification.FooNotificationBuilder;
import com.smartfoo.android.core.notification.FooNotificationListenerManager;
import com.smartfoo.android.core.platform.FooScreenListener;
import com.smartfoo.android.core.platform.FooScreenListener.FooScreenListenerCallbacks;
import com.swooby.alfred.NotificationParserManager.NotificationParserManagerCallbacks;
import com.swooby.alfred.NotificationParserManager.NotificationParserManagerConfiguration;
import com.swooby.alfred.ProfileManager.HeadsetType;
import com.swooby.alfred.ProfileManager.ProfileManagerCallbacks;
import com.swooby.alfred.ProfileManager.ProfileManagerConfiguration;
import com.swooby.alfred.TextToSpeechManager.TextToSpeechManagerCallbacks;
import com.swooby.alfred.TextToSpeechManager.TextToSpeechManagerConfiguration;

import java.util.concurrent.TimeUnit;

public class MainApplication
        extends Application
{
    private static final String TAG = FooLog.TAG(MainApplication.class);

    public interface MainApplicationCallbacks
    {
        Activity getActivity();

        boolean onNotificationListenerAccessDisabled();
    }

    private AppPreferences                               mAppPreferences;
    private FooListenerManager<MainApplicationCallbacks> mListenerManager;
    private TextToSpeechManager                          mTextToSpeechManager;
    private ProfileManager                               mProfileManager;
    private NotificationParserManager                    mNotificationParserManager;
    private FooScreenListener                            mScreenListener;

    private Boolean mIsEnabled;

    @NonNull
    public TextToSpeechManager getTextToSpeechManager()
    {
        return mTextToSpeechManager;
    }

    @NonNull
    public ProfileManager getProfileManager()
    {
        return mProfileManager;
    }

    @NonNull
    public NotificationParserManager getNotificationParserManager()
    {
        return mNotificationParserManager;
    }

    private boolean isEnabled()
    {
        return FooBoolean.toBoolean(mIsEnabled);
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

        mAppPreferences = new AppPreferences(this);

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
            public void setVoiceName(String voiceName)
            {
                mAppPreferences.setTextToSpeechVoiceName(voiceName);
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
        mTextToSpeechManager.attach(new TextToSpeechManagerCallbacks());

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
        mNotificationParserManager.attach(new NotificationParserManagerCallbacks()
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
        });

        mScreenListener = new FooScreenListener(this);
        mScreenListener.attach(new FooScreenListenerCallbacks()
        {
            @Override
            public void onScreenOff()
            {
                MainApplication.this.onScreenOff();
            }

            @Override
            public void onScreenOn()
            {
                MainApplication.this.onScreenOn();
            }
        });

        mProfileManager = new ProfileManager(this, new ProfileManagerConfiguration()
        {
            @Override
            public String getProfileToken()
            {
                return mAppPreferences.getProfileToken();
            }

            @Override
            public void setProfileToken(String profileToken)
            {
                mAppPreferences.setProfileToken(profileToken);
            }
        });
        mProfileManager.attach(new ProfileManagerCallbacks()
        {
            @Override
            public void onHeadsetConnectionChanged(HeadsetType headsetType, String headsetName, boolean isConnected)
            {
                MainApplication.this.onHeadsetConnectionChanged(headsetType, headsetName, isConnected);
            }

            @Override
            public void onProfileStateChanged(String profileName, boolean enabled)
            {
                MainApplication.this.onProfileStateChanged(profileName, enabled);
            }
        });

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

    private void onHeadsetConnectionChanged(HeadsetType headsetType, String headsetName, boolean isConnected)
    {
        FooLog.i(TAG, "onHeadsetConnectionChanged(headsetType=" + headsetType +
                      ", headsetName=" + FooString.quote(headsetName) +
                      ", isConnected=" + isConnected + ')');
        if (headsetName == null)
        {
            headsetName = "";
        }

        int resId;
        switch (headsetType)
        {
            case Bluetooth:
                resId = isConnected ? R.string.bluetooth_headset_X_connected : R.string.bluetooth_headset_X_disconnected;
                break;
            case Wired:
                resId = isConnected ? R.string.wired_headset_X_connected : R.string.wired_headset_X_disconnected;
                break;
            default:
                throw new IllegalArgumentException("Unhandled headsetType == " + headsetType);
        }

        String speech = getString(resId, headsetName);
        mTextToSpeechManager.speak(speech);
    }

    private void onProfileStateChanged(String profileName, boolean enabled)
    {
        FooLog.i(TAG, "onProfileStateChanged(profileName=" + FooString.quote(profileName) +
                      ", enabled=" + enabled + ')');
        updateEnabledState("onProfileStateChanged");
    }

    private boolean updateEnabledState(String debugInfo)
    {
        FooLog.v(TAG, "updateEnabledState(debugInfo=" + FooString.quote(debugInfo) + ')');

        /*
        if (!mNotificationParserManager.isInitialized())
        {
            return false;
        }
        */

        boolean isProfileEnabled = mProfileManager.isEnabled();
        FooLog.v(TAG, "updateEnabledState: isProfileEnabled == " + isProfileEnabled);

        //noinspection UnnecessaryLocalVariable
        boolean isEnabled = isProfileEnabled;

        /*
        boolean isNotificationListenerBound = mNotificationParserManager.isNotificationListenerBound();
        FooLog.v(TAG, "updateEnabledState: isNotificationListenerBound == " + isNotificationListenerBound);

        isEnabled &= isNotificationListenerBound;
        */

        FooLog.v(TAG, "updateEnabledState: isEnabled == " + isEnabled);

        FooLog.v(TAG, "updateEnabledState: mIsEnabled == " + mIsEnabled);
        if (mIsEnabled != null && mIsEnabled == isEnabled)
        {
            FooLog.v(TAG, "updateEnabledState: mIsEnabled == isEnabled; ignoring");
            return false;
        }

        FooLog.i(TAG, "updateEnabledState: mIsEnabled != isEnabled; updating");

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

        // !!!!!!THIS IS WHERE THE REAL APP LOGIC ACTUALLY STARTS!!!!!!

        updateScreenInfo();

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

            //
            // Always speak this, even if profile is disabled
            //
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
    public boolean isRecognitionAvailable()
    {
        return SpeechRecognizer.isRecognitionAvailable(this);
    }
    */

    //
    //
    //

    private void onScreenOff()
    {
        FooLog.i(TAG, "onScreenOff()");
        updateScreenInfo();
    }

    private void onScreenOn()
    {
        FooLog.i(TAG, "onScreenOn()");
        updateScreenInfo();
    }

    private long mTimeScreenOffMs = -1;

    private void updateScreenInfo()
    {
        String speech;
        if (mScreenListener.isScreenOn())
        {
            if (mTimeScreenOffMs != -1)
            {
                long durationMs = System.currentTimeMillis() - mTimeScreenOffMs;

                mTimeScreenOffMs = -1;

                speech = getString(R.string.screen_on_screen_was_off_for_X, FooString.getTimeDurationString(this, durationMs, TimeUnit.SECONDS));
            }
            else
            {
                speech = getString(R.string.screen_on);
            }
        }
        else
        {
            mTimeScreenOffMs = System.currentTimeMillis();

            speech = getString(R.string.screen_off);
        }
        mTextToSpeechManager.speak(speech);
    }
}

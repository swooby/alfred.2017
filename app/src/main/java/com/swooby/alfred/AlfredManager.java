package com.swooby.alfred;

import android.app.Activity;
import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;

import com.smartfoo.android.core.FooListenerManager;
import com.smartfoo.android.core.FooRun;
import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.notification.FooNotificationListenerManager.DisabledCause;
import com.smartfoo.android.core.platform.FooChargingListener;
import com.smartfoo.android.core.platform.FooChargingListener.FooChargingListenerCallbacks;
import com.smartfoo.android.core.platform.FooHandler;
import com.smartfoo.android.core.platform.FooPlatformUtils;
import com.smartfoo.android.core.platform.FooScreenListener;
import com.smartfoo.android.core.platform.FooScreenListener.FooScreenListenerCallbacks;
import com.smartfoo.android.core.texttospeech.FooTextToSpeech;
import com.smartfoo.android.core.texttospeech.FooTextToSpeechBuilder;
import com.swooby.alfred.NotificationManager.NotificationStatus;
import com.swooby.alfred.NotificationManager.NotificationStatusNotificationAccessNotEnabled;
import com.swooby.alfred.NotificationManager.NotificationStatusProfileNotEnabled;
import com.swooby.alfred.NotificationParserManager.NotificationParserManagerCallbacks;
import com.swooby.alfred.NotificationParserManager.NotificationParserManagerConfiguration;
import com.swooby.alfred.ProfileManager.HeadsetType;
import com.swooby.alfred.ProfileManager.ProfileManagerCallbacks;
import com.swooby.alfred.ProfileManager.ProfileManagerConfiguration;
import com.swooby.alfred.TextToSpeechManager.TextToSpeechManagerCallbacks;
import com.swooby.alfred.TextToSpeechManager.TextToSpeechManagerConfiguration;

import java.util.concurrent.TimeUnit;

public class AlfredManager
{
    private static final String TAG = FooLog.TAG(AlfredManager.class);

    public interface AlfredManagerCallbacks
    {
        Activity getActivity();

        void onNotificationAccessSettingConfirmedEnabled();

        boolean onNotificationAccessSettingDisabled(DisabledCause disabledCause);

        void onProfileEnabled(String profileToken);

        void onProfileDisabled(String profileToken);
    }

    private final Context                                          mApplicationContext;
    private final FooHandler                                       mHandler;
    private final AppPreferences                                   mAppPreferences;
    private final NotificationManager                              mNotificationManager;
    private final FooListenerManager<AlfredManagerCallbacks>       mListenerManager;
    private final TextToSpeechManager                              mTextToSpeechManager;
    private final DelayedRunnableNotificationAccessSettingDisabled mDelayedRunnableNotificationAccessSettingDisabled;
    private final NotificationParserManager                        mNotificationParserManager;
    private final FooScreenListener                                mScreenListener;
    private final FooChargingListener                              mChargingListener;
    private final ProfileManager                                   mProfileManager;

    public AlfredManager(@NonNull Context applicationContext)
    {
        FooLog.v(TAG, "+AlfredManager(applicationContext=" + applicationContext + ')');

        FooRun.throwIllegalArgumentExceptionIfNull(applicationContext, "applicationContext");

        mApplicationContext = applicationContext;

        mHandler = new FooHandler();

        mAppPreferences = new AppPreferences(mApplicationContext);

        //
        // Create Managers/etc
        //
        mNotificationManager = new NotificationManager(mApplicationContext);
        mListenerManager = new FooListenerManager<>();
        mTextToSpeechManager = new TextToSpeechManager(mApplicationContext, new TextToSpeechManagerConfiguration()
        {
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
                return AlfredManager.this.isProfileEnabled();
            }
        });
        mDelayedRunnableNotificationAccessSettingDisabled = new DelayedRunnableNotificationAccessSettingDisabled();
        mNotificationParserManager = new NotificationParserManager(mApplicationContext, new NotificationParserManagerConfiguration()
        {
            @Override
            public boolean isEnabled()
            {
                return AlfredManager.this.isProfileEnabled();
            }

            @NonNull
            @Override
            public TextToSpeechManager getTextToSpeech()
            {
                return AlfredManager.this.getTextToSpeechManager();
            }
        });
        mScreenListener = new FooScreenListener(mApplicationContext);
        mChargingListener = new FooChargingListener(mApplicationContext);
        mProfileManager = new ProfileManager(mApplicationContext, new ProfileManagerConfiguration()
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

        initialize();

        FooLog.v(TAG, "-AlfredManager(applicationContext=" + applicationContext + ')');
    }

    public String getString(int resId, Object... formatArgs)
    {
        return mApplicationContext.getString(resId, formatArgs);
    }

    @NonNull
    public Context getApplicationContext()
    {
        return mApplicationContext;
    }

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

    private void initialize()
    {
        mNotificationManager.initializing("Text To Speech", "TBD text", "TBD subtext");
        final long timeStartMillis = System.currentTimeMillis();
        mTextToSpeechManager.attach(new TextToSpeechManagerCallbacks()
        {
            @Override
            public void onTextToSpeechInitialized(int status)
            {
                long timeElapsedMillis = System.currentTimeMillis() - timeStartMillis;
                super.onTextToSpeechInitialized(status);
                AlfredManager.this.onTextToSpeechInitialized(status, timeElapsedMillis);
            }
        });
        mNotificationParserManager.attach(new NotificationParserManagerCallbacks()
        {
            @Override
            public boolean onNotificationAccessSettingConfirmedEnabled()
            {
                return AlfredManager.this.onNotificationAccessSettingConfirmedEnabled();
            }

            @Override
            public void onNotificationAccessSettingDisabled(DisabledCause disabledCause)
            {
                AlfredManager.this.onNotificationAccessSettingDisabled(disabledCause, false);
            }
        });
        mScreenListener.attach(new FooScreenListenerCallbacks()
        {
            @Override
            public void onScreenOff()
            {
                AlfredManager.this.onScreenOff();
            }

            @Override
            public void onScreenOn()
            {
                AlfredManager.this.onScreenOn();
            }
        });
        mChargingListener.attach(new FooChargingListenerCallbacks()
        {
            @Override
            public void onChargingConnected()
            {
                AlfredManager.this.onChargingConnected();
            }

            @Override
            public void onChargingDisconnected()
            {
                AlfredManager.this.onChargingDisconnected();
            }
        });
        // TODO:(pv) Phone doze listener
        // TODO:(pv) Phone wifi listener
        // TODO:(pv) Phone cellular listener
        // TODO:(pv) etc...
        mProfileManager.attach(new ProfileManagerCallbacks()
        {
            @Override
            public void onHeadsetConnectionChanged(HeadsetType headsetType, String headsetName, boolean isConnected)
            {
                AlfredManager.this.onHeadsetConnectionChanged(headsetType, headsetName, isConnected);
            }

            @Override
            void onProfileEnabled(String profileToken)
            {
                AlfredManager.this.onProfileEnabled(profileToken);
            }

            @Override
            void onProfileDisabled(String profileToken)
            {
                AlfredManager.this.onProfileDisabled(profileToken);
            }
            /*
            @Override
            public void onProfileStateChanged(String profileName, boolean enabled)
            {
            }
            */
        });

        //updateEnabledState("onCreate");

        /*
        if (!isRecognitionAvailable())
        {
            // TODO:(pv) Better place for initialization and indication of failure...
            //speak(true, true, "Speech recognition is not available for this device.");
            //speak(true, true, "Goodbye.");
            return;
        }

        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        */
    }

    /*
    private SpeechRecognizer mSpeechRecognizer;

    public boolean isSpeechRecognitionAvailable()
    {
        return SpeechRecognizer.isRecognitionAvailable(this);
    }
    */

    public void attach(AlfredManagerCallbacks callbacks)
    {
        mListenerManager.attach(callbacks);
        if (callbacks.getActivity() != null)
        {
            // TODO:(pv) Cancel any pending Toasts...
        }
    }

    public void detach(AlfredManagerCallbacks callbacks)
    {
        mListenerManager.detach(callbacks);
    }

    private boolean isProfileEnabled()
    {
        return mProfileManager.isEnabled();
    }

    public boolean isHeadless()
    {
        for (AlfredManagerCallbacks callbacks : mListenerManager.beginTraversing())
        {
            if (callbacks.getActivity() != null)
            {
                return false;
            }
        }
        mListenerManager.endTraversing();
        return true;
    }

    private void onTextToSpeechInitialized(int status, long timeElapsedMillis)
    {
        FooLog.i(TAG, "onTextToSpeechInitialized: timeElapsedMillis == " + timeElapsedMillis +
                      ", status == " + FooTextToSpeech.statusToString(status));
        if (status != TextToSpeech.SUCCESS)
        {
            return;
        }
    }

    private boolean onNotificationAccessSettingConfirmedEnabled()
    {
        FooLog.i(TAG, "onNotificationAccessSettingConfirmedEnabled()");

        String speech = getString(R.string.alfred_notification_access_enabled);
        mTextToSpeechManager.speak(speech);

        for (AlfredManagerCallbacks callbacks : mListenerManager.beginTraversing())
        {
            callbacks.onNotificationAccessSettingConfirmedEnabled();
        }
        mListenerManager.endTraversing();

        speech = "Initializing Notifications";
        mTextToSpeechManager.speak(speech);
        mNotificationParserManager.refresh();

        return true;
    }

    private class DelayedRunnableNotificationAccessSettingDisabled
            implements Runnable
    {
        private final String TAG = FooLog.TAG(DelayedRunnableNotificationAccessSettingDisabled.class);

        public DisabledCause mDisabledCause;

        @Override
        public void run()
        {
            FooLog.v(TAG, "+run()");
            onNotificationAccessSettingDisabled(mDisabledCause, true);
            FooLog.v(TAG, "-run()");
        }
    }

    private void onNotificationAccessSettingDisabled(DisabledCause disabledCause, boolean delayed)
    {
        FooLog.w(TAG, "onNotificationAccessSettingDisabled(disabledCause=" + disabledCause +
                      ", delayed=" + delayed + ')');

        boolean headless = true;
        boolean handled = false;
        for (AlfredManagerCallbacks callbacks : mListenerManager.beginTraversing())
        {
            headless &= callbacks.getActivity() == null;
            handled |= callbacks.onNotificationAccessSettingDisabled(disabledCause);
        }
        mListenerManager.endTraversing();

        if (headless && !delayed)
        {
            mDelayedRunnableNotificationAccessSettingDisabled.mDisabledCause = disabledCause;
            mHandler.postDelayed(mDelayedRunnableNotificationAccessSettingDisabled, 200);
            return;
        }

        String title = getNotificationAccessNotEnabledTitle(disabledCause);
        String message = getNotificationAccessNotEnabledMessage(disabledCause);

        if (headless)
        {
            String separator = getString(R.string.alfred_line_separator);
            String text = FooString.join(separator, title, message);
            FooPlatformUtils.toastLong(mApplicationContext, text);
        }

        //
        // Always speak this, even if profile is disabled
        //
        mTextToSpeechManager.speak(true, false, new FooTextToSpeechBuilder()
                .appendSpeech(title)
                .appendSilenceWordBreak()
                .appendSpeech(message));

        /*
        if (handled)
        {
            return;
        }
        */

        boolean isProfileEnabled = isProfileEnabled();
        NotificationStatus notificationStatus;
        if (isProfileEnabled)
        {
            notificationStatus = new NotificationStatusNotificationAccessNotEnabled(mApplicationContext,
                    getString(R.string.alfred_running),
                    getString(R.string.alfred_waiting_for_notification_access));
        }
        else
        {
            String profileToken = mProfileManager.getProfileToken();
            notificationStatus = new NotificationStatusProfileNotEnabled(mApplicationContext, profileToken);
        }
        notification(isProfileEnabled, notificationStatus, "TBD text", "onNotificationAccessSettingDisabled");

        //
        // Only show the notification if no UI handled the event
        //
        //showNotificationAccessNotEnabledNotification(title);
    }

    private void notification(boolean isProfileEnabled,
                              NotificationStatus notificationStatus,
                              String text,
                              String subtext)
    {
        if (isProfileEnabled)
        {
            mNotificationManager.running(notificationStatus, text, subtext);
        }
        else
        {
            mNotificationManager.paused(notificationStatus, text, subtext);
        }
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
                resId = isConnected ? R.string.alfred_bluetooth_headphone_X_connected : R.string.alfred_bluetooth_headphone_X_disconnected;
                break;
            case Wired:
                resId = isConnected ? R.string.alfred_wired_headphone_X_connected : R.string.alfred_wired_headphone_X_disconnected;
                break;
            default:
                throw new IllegalArgumentException("Unhandled headsetType == " + headsetType);
        }

        String speech = getString(resId, headsetName);
        mTextToSpeechManager.speak(speech);
    }

    private void onProfileEnabled(String profileToken)
    {
        FooLog.i(TAG, "onProfileEnabled(profileToken=" + FooString.quote(profileToken) + ')');

        boolean isNotificationAccessSettingConfirmedEnabled = mNotificationParserManager.isNotificationAccessSettingConfirmedEnabled();

        NotificationStatus notificationStatus = null;
        if (!isNotificationAccessSettingConfirmedEnabled)
        {
            notificationStatus = new NotificationStatusNotificationAccessNotEnabled(mApplicationContext,
                    getString(R.string.alfred_running),
                    getString(R.string.alfred_waiting_for_notification_access));
        }
        notification(true, notificationStatus, "TBD text", "onProfileEnabled");

        //
        // !!!!!!THIS IS WHERE THE REAL APP LOGIC ACTUALLY STARTS!!!!!!
        //

        //...

        updateScreenInfo();
        updateChargingInfo();

        //...

        for (AlfredManagerCallbacks callbacks : mListenerManager.beginTraversing())
        {
            callbacks.onProfileEnabled(profileToken);
        }
        mListenerManager.endTraversing();

        if (isNotificationAccessSettingConfirmedEnabled)
        {
            onNotificationAccessSettingConfirmedEnabled();
        }
    }

    private void onProfileDisabled(String profileToken)
    {
        FooLog.i(TAG, "onProfileDisabled(profileToken=" + FooString.quote(profileToken) + ')');

        NotificationStatus notificationStatus = new NotificationStatusProfileNotEnabled(mApplicationContext, profileToken);
        notification(false, notificationStatus, "TBD text", "onProfileDisabled");

        //...

        for (AlfredManagerCallbacks callbacks : mListenerManager.beginTraversing())
        {
            callbacks.onProfileDisabled(profileToken);
        }
        mListenerManager.endTraversing();
    }

    public String getNotificationAccessNotEnabledTitle(@NonNull DisabledCause disabledCause)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(disabledCause, "disabledCause");
        int resId;
        switch (disabledCause)
        {
            case ConfirmedNotEnabled:
                resId = R.string.alfred_notification_access_not_enabled;
                break;
            case BindTimeout:
                resId = R.string.alfred_notification_access_bind_timeout;
                break;
            default:
                throw new IllegalArgumentException("Unhandled disabledCause == " + disabledCause);
        }
        return getString(resId);
    }

    public String getNotificationAccessNotEnabledMessage(@NonNull DisabledCause disabledCause)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(disabledCause, "disabledCause");
        int resId;
        switch (disabledCause)
        {
            case ConfirmedNotEnabled:
                resId = R.string.alfred_please_enable_notification_access_for_the_X_application;
                break;
            case BindTimeout:
                resId = R.string.alfred_please_reenable_notification_access_for_the_X_application;
                break;
            default:
                throw new IllegalArgumentException("Unhandled disabledCause == " + disabledCause);
        }

        String appName = getString(R.string.alfred_app_name);

        return getString(resId, appName);
    }

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

    private long mTimeScreenOnMs  = -1;
    private long mTimeScreenOffMs = -1;

    private void updateScreenInfo()
    {
        String speech;
        if (mScreenListener.isScreenOn())
        {
            mTimeScreenOnMs = System.currentTimeMillis();

            if (mTimeScreenOffMs != -1)
            {
                long durationMs = mTimeScreenOnMs - mTimeScreenOffMs;

                mTimeScreenOffMs = -1;

                speech = getString(R.string.alfred_screen_on_screen_was_off_for_X, FooString.getTimeDurationString(mApplicationContext, durationMs, TimeUnit.SECONDS));
            }
            else
            {
                speech = getString(R.string.alfred_screen_on);
            }
        }
        else
        {
            mTimeScreenOffMs = System.currentTimeMillis();

            if (mTimeScreenOnMs != -1)
            {
                long durationMs = mTimeScreenOffMs - mTimeScreenOnMs;

                mTimeScreenOnMs = -1;

                speech = getString(R.string.alfred_screen_off_screen_was_on_for_X, FooString.getTimeDurationString(mApplicationContext, durationMs, TimeUnit.SECONDS));
            }
            else
            {
                speech = getString(R.string.alfred_screen_off);
            }
        }
        mTextToSpeechManager.speak(speech);
    }

    private void onChargingConnected()
    {
        FooLog.i(TAG, "onChargingConnected()");
        updateChargingInfo();
    }

    private void onChargingDisconnected()
    {
        FooLog.i(TAG, "onChargingDisconnected()");
        updateChargingInfo();
    }

    private long mTimeChargingConnectedMs    = -1;
    private long mTimeChargingDisconnectedMs = -1;

    private void updateChargingInfo()
    {
        String speech;
        if (mChargingListener.isCharging())
        {
            mTimeChargingConnectedMs = System.currentTimeMillis();

            if (mTimeChargingDisconnectedMs != -1)
            {
                long durationMs = mTimeChargingConnectedMs - mTimeChargingDisconnectedMs;

                mTimeChargingDisconnectedMs = -1;

                speech = getString(R.string.alfred_charger_connected_charger_was_disconnected_for_X, FooString.getTimeDurationString(mApplicationContext, durationMs, TimeUnit.SECONDS));
            }
            else
            {
                speech = getString(R.string.alfred_charger_connected);
            }
        }
        else
        {
            mTimeChargingDisconnectedMs = System.currentTimeMillis();

            if (mTimeChargingConnectedMs != -1)
            {
                long durationMs = mTimeChargingDisconnectedMs - mTimeChargingConnectedMs;

                mTimeChargingConnectedMs = -1;

                speech = getString(R.string.alfred_charger_disconnected_charger_was_connected_for_X, FooString.getTimeDurationString(mApplicationContext, durationMs, TimeUnit.SECONDS));
            }
            else
            {
                speech = getString(R.string.alfred_charger_disconnected);
            }
        }
        mTextToSpeechManager.speak(speech);
    }
}

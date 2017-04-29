package com.swooby.alfred;

import android.app.Activity;
import android.content.Context;
import android.os.Handler.Callback;
import android.os.Message;
import android.service.notification.StatusBarNotification;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.smartfoo.android.core.FooListenerManager;
import com.smartfoo.android.core.FooRun;
import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.collections.FooLongSparseArray;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.media.FooAudioStreamVolumeObserver;
import com.smartfoo.android.core.media.FooAudioStreamVolumeObserver.OnAudioStreamVolumeChangedCallbacks;
import com.smartfoo.android.core.media.FooAudioUtils;
import com.smartfoo.android.core.network.FooCellularStateListener;
import com.smartfoo.android.core.network.FooCellularStateListener.FooCellularHookStateCallbacks;
import com.smartfoo.android.core.network.FooDataConnectionListener;
import com.smartfoo.android.core.network.FooDataConnectionListener.FooDataConnectionInfo;
import com.smartfoo.android.core.network.FooDataConnectionListener.FooDataConnectionListenerCallbacks;
import com.smartfoo.android.core.notification.FooNotificationListenerManager.NotConnectedReason;
import com.smartfoo.android.core.platform.FooBootListener;
import com.smartfoo.android.core.platform.FooBootListener.FooBootListenerCallbacks;
import com.smartfoo.android.core.platform.FooChargePortListener;
import com.smartfoo.android.core.platform.FooChargePortListener.ChargePort;
import com.smartfoo.android.core.platform.FooChargePortListener.FooChargePortListenerCallbacks;
import com.smartfoo.android.core.platform.FooHandler;
import com.smartfoo.android.core.platform.FooPlatformUtils;
import com.smartfoo.android.core.platform.FooScreenListener;
import com.smartfoo.android.core.platform.FooScreenListener.FooScreenListenerCallbacks;
import com.smartfoo.android.core.texttospeech.FooTextToSpeech;
import com.smartfoo.android.core.texttospeech.FooTextToSpeechBuilder;
import com.swooby.alfred.NotificationManager.NotificationStatus;
import com.swooby.alfred.NotificationManager.NotificationStatusNotificationAccessNotEnabled;
import com.swooby.alfred.NotificationManager.NotificationStatusProfileNotEnabled;
import com.swooby.alfred.NotificationManager.NotificationStatusRunning;
import com.swooby.alfred.NotificationParserManager.NotificationParserManagerCallbacks;
import com.swooby.alfred.NotificationParserManager.NotificationParserManagerConfiguration;
import com.swooby.alfred.ProfileManager.HeadsetType;
import com.swooby.alfred.ProfileManager.ProfileManagerCallbacks;
import com.swooby.alfred.ProfileManager.ProfileManagerConfiguration;
import com.swooby.alfred.TextToSpeechManager.TextToSpeechManagerCallbacks;
import com.swooby.alfred.TextToSpeechManager.TextToSpeechManagerConfiguration;
import com.swooby.alfred.notification.parsers.AbstractNotificationParser;
import com.swooby.alfred.notification.parsers.AlfredNotificationParser;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class AlfredManager
{
    private static final String TAG = FooLog.TAG(AlfredManager.class);

    public interface AlfredManagerCallbacks
    {
        Activity getActivity();

        void onNotificationListenerConnected();

        boolean onNotificationListenerNotConnected(NotConnectedReason reason);

        void onProfileEnabled(Profile profile);

        void onProfileDisabled(Profile profile);

        void onTextToSpeechAudioStreamVolumeChanged(int audioStreamType, int volume);
    }

    private final Context                                    mApplicationContext;
    private final FooHandler                                 mHandler;
    private final AppPreferences                             mAppPreferences;
    private final FooListenerManager<AlfredManagerCallbacks> mListenerManager;
    private final NotificationManager                        mNotificationManager;
    private final SayingsManager                             mSayingsManager;
    private final TextToSpeechManager                        mTextToSpeechManager;
    private final NotificationParserManager                  mNotificationParserManager;
    private final FooScreenListener                          mScreenListener;
    private final FooBootListener                            mBootListener;
    private final FooChargePortListener                      mChargePortListener;
    private final FooCellularStateListener                   mCellularStateListener;
    private final FooCellularHookStateCallbacks              mCellularHookStateCallbacks;
    private final FooDataConnectionListener                  mDataConnectionListener;
    private final FooDataConnectionListenerCallbacks         mDataConnectionListenerCallbacks;
    private final FooAudioStreamVolumeObserver               mAudioStreamVolumeObserver;
    private final ProfileManager                             mProfileManager;

    private boolean mIsStarted;
    private boolean mIsUserUnlocked;

    public AlfredManager(@NonNull Context applicationContext)
    {
        FooLog.v(TAG, "+AlfredManager(applicationContext=" + applicationContext + ')');

        FooRun.throwIllegalArgumentExceptionIfNull(applicationContext, "applicationContext");

        mApplicationContext = applicationContext;

        mHandler = new FooHandler(new Callback()
        {
            @Override
            public boolean handleMessage(Message msg)
            {
                return AlfredManager.this.handleMessage(msg);
            }
        });

        mAppPreferences = new AppPreferences(mApplicationContext);

        //
        // Create Managers/etc
        //
        mListenerManager = new FooListenerManager<>(this);
        mNotificationManager = new NotificationManager(mApplicationContext);
        mSayingsManager = new SayingsManager(mApplicationContext);
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
            public boolean isTextToSpeechEnabled()
            {
                return AlfredManager.this.isTextToSpeechEnabled();
            }
        });
        mNotificationParserManager = new NotificationParserManager(mApplicationContext, new NotificationParserManagerConfiguration()
        {
            @Override
            public boolean isNotificationParserEnabled()
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
        mBootListener = new FooBootListener(mApplicationContext);
        mChargePortListener = new FooChargePortListener(mApplicationContext);

        mCellularStateListener = new FooCellularStateListener(mApplicationContext);
        mCellularHookStateCallbacks = new FooCellularHookStateCallbacks()
        {
            @Override
            public void onCellularOffHook()
            {
                AlfredManager.this.onCellularOffHook();
            }

            @Override
            public void onCellularOnHook()
            {
                AlfredManager.this.onCellularOnHook();
            }
        };
        mDataConnectionListener = new FooDataConnectionListener(mApplicationContext);
        mDataConnectionListenerCallbacks = new FooDataConnectionListenerCallbacks()
        {
            @Override
            public void onDataConnected(FooDataConnectionInfo dataConnectionInfo)
            {
                AlfredManager.this.onDataConnected(dataConnectionInfo);
            }

            @Override
            public void onDataDisconnected(FooDataConnectionInfo dataConnectionInfo)
            {
                AlfredManager.this.onDataDisconnected(dataConnectionInfo);
            }
        };
        mAudioStreamVolumeObserver = new FooAudioStreamVolumeObserver(mApplicationContext);

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

        FooLog.v(TAG, "-AlfredManager(applicationContext=" + applicationContext + ')');
    }

    @NonNull
    public String getString(@StringRes int resId, Object... formatArgs)
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

    public boolean isStarted()
    {
        return mIsStarted;
    }

    public void start()
    {
        try
        {
            FooLog.i(TAG, "+start()");

            if (isStarted())
            {
                return;
            }

            mIsStarted = true;

            mNotificationManager.notifyInitializing("Text To Speech", "TBD text", "TBD subtext");
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
                public boolean onNotificationListenerConnected(StatusBarNotification[] activeNotifications)
                {
                    return AlfredManager.this.onNotificationListenerConnected();
                }

                @Override
                public void onNotificationListenerNotConnected(NotConnectedReason reason, long elapsedMillis)
                {
                    AlfredManager.this.onNotificationListenerNotConnected(reason, elapsedMillis, 200);
                }

                @Override
                public void onNotificationParsed(@NonNull AbstractNotificationParser parser)
                {
                    AlfredManager.this.onNotificationParsed(parser);
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

                @Override
                public void onUserUnlocked()
                {
                    FooLog.e(TAG, "onUserUnlocked()");
                    mTextToSpeechManager.speak("user unlocked");
                }
            });
            mBootListener.attach(new FooBootListenerCallbacks()
            {

                @Override
                public void onBootCompleted()
                {
                }

                @Override
                public void onReboot()
                {
                    mTextToSpeechManager.speak("rebooting");
                }

                @Override
                public void onShutdown()
                {
                    mTextToSpeechManager.speak("shutting down");
                }
            });
            mChargePortListener.attach(new FooChargePortListenerCallbacks()
            {
                @Override
                public void onChargePortConnected(ChargePort chargePort)
                {
                    AlfredManager.this.onChargePortConnected(chargePort);
                }

                @Override
                public void onChargePortDisconnected(ChargePort chargePort)
                {
                    AlfredManager.this.onChargePortDisconnected(chargePort);
                }
            });
            mCellularStateListener.start(mCellularHookStateCallbacks, null);
            mDataConnectionListener.start(mDataConnectionListenerCallbacks);
            for (int audioStreamType : FooAudioUtils.getAudioStreamTypes())
            {
                volumeObserverStart(audioStreamType);
            }
            // TODO:(pv) Phone doze listener
            // TODO:(pv) etc…
            mProfileManager.attach(new ProfileManagerCallbacks()
            {
                @Override
                public void onHeadsetConnectionChanged(HeadsetType headsetType, String headsetName, boolean isConnected)
                {
                    AlfredManager.this.onHeadsetConnectionChanged(headsetType, headsetName, isConnected);
                }

                @Override
                void onProfileEnabled(Profile profile)
                {
                    AlfredManager.this.onProfileEnabled(profile);
                }

                @Override
                void onProfileDisabled(Profile profile)
                {
                    AlfredManager.this.onProfileDisabled(profile);
                }
            });

            /*
            if (!isRecognitionAvailable())
            {
                // TODO:(pv) Better place for initialization and indication of failure…
                //speak(true, true, "Speech recognition is not available for this device.");
                //speak(true, true, "Goodbye.");
                return;
            }

            mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            */
        }
        finally
        {
            FooLog.i(TAG, "-start()");
        }
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
        FooLog.i(TAG, "attach(callbacks=" + callbacks + ')');
        mListenerManager.attach(callbacks);
        if (callbacks.getActivity() != null)
        {
            // TODO:(pv) Cancel any pending Toasts…
        }
    }

    public void detach(AlfredManagerCallbacks callbacks)
    {
        FooLog.i(TAG, "detach(callbacks=" + callbacks + ')');
        mListenerManager.detach(callbacks);
    }

    private boolean isProfileEnabled()
    {
        return mProfileManager.isEnabled();
    }

    private boolean isTextToSpeechEnabled()
    {
        return isProfileEnabled() && mCellularStateListener.isOnHook();
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

    private void notification(@NonNull NotificationStatus notificationStatus,
                              String text,
                              String subtext)
    {
        if (notificationStatus instanceof NotificationStatusProfileNotEnabled)
        {
            mNotificationManager.notifyPaused(notificationStatus, text, subtext);
        }
        else
        {
            mNotificationManager.notifyRunning(notificationStatus, text, subtext);
        }
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

    private void onProfileEnabled(Profile profile)
    {
        FooLog.i(TAG, "onProfileEnabled(profile=" + profile + ')');

        mTextToSpeechManager.clear();

        //
        // !!!!!!THIS IS WHERE THE REAL APP LOGIC ACTUALLY STARTS!!!!!!
        //

        FooTextToSpeechBuilder builder = mSayingsManager.goodPartOfDayUserNoun();
        mTextToSpeechManager.speak(builder);

        mIsUserUnlocked |= mScreenListener.isUserUnlocked();
        if (!mIsUserUnlocked)
        {
            FooLog.i(TAG, "onProfileEnabled: mIsUserUnlocked == false");

            mTextToSpeechManager.speak("Your device has just been rebooted and the screen needs to be unlocked before I can read your notifications.");
        }

        NotificationStatus notificationStatus;
        boolean isNotificationListenerConnected = mNotificationParserManager.isNotificationListenerConnected();
        if (isNotificationListenerConnected)
        {
            notificationStatus = new NotificationStatusRunning(mApplicationContext);
        }
        else
        {
            notificationStatus = new NotificationStatusNotificationAccessNotEnabled(mApplicationContext,
                    getString(R.string.alfred_running),
                    getString(R.string.alfred_waiting_for_notification_access),
                    null); // <-- TODO:(pv) Put above/below speech in here
        }
        notification(notificationStatus, "TBD text", "onProfileEnabled");

        updateScreenInfo();
        updateChargePortInfo();
        updateDataConnectionInfo();

        for (AlfredManagerCallbacks callbacks : mListenerManager.beginTraversing())
        {
            callbacks.onProfileEnabled(profile);
        }
        mListenerManager.endTraversing();

        if (isNotificationListenerConnected)
        {
            onNotificationListenerConnected();
        }
    }

    private void onProfileDisabled(Profile profile)
    {
        FooLog.i(TAG, "onProfileDisabled(profile=" + profile + ')');

        NotificationStatus notificationStatus = new NotificationStatusProfileNotEnabled(mApplicationContext, profile);
        notification(notificationStatus, "TBD text", "onProfileDisabled");

        mTextToSpeechManager.clear();

        for (AlfredManagerCallbacks callbacks : mListenerManager.beginTraversing())
        {
            callbacks.onProfileDisabled(profile);
        }
        mListenerManager.endTraversing();
    }

    //
    //
    //

    private class DelayedRunnableNotificationListenerNotConnected
            implements Runnable
    {
        private final String TAG = FooLog.TAG(DelayedRunnableNotificationListenerNotConnected.class);

        private final NotConnectedReason mReason;
        private final long               mElapsedMillis;

        public DelayedRunnableNotificationListenerNotConnected(NotConnectedReason reason, long elapsedMillis)
        {
            mReason = reason;
            mElapsedMillis = elapsedMillis;
        }

        @Override
        public void run()
        {
            FooLog.v(TAG, "+run()");
            onNotificationListenerNotConnected(mReason, mElapsedMillis, 0);
            FooLog.v(TAG, "-run()");
        }
    }

    private DelayedRunnableNotificationListenerNotConnected mDelayedRunnableNotificationAccessSettingDisabled;

    private boolean onNotificationListenerConnected()
    {
        FooLog.i(TAG, "onNotificationListenerConnected()");

        mHandler.removeCallbacks(mDelayedRunnableNotificationAccessSettingDisabled);
        mDelayedRunnableNotificationAccessSettingDisabled = null;

        String speech = getString(R.string.alfred_notification_listener_connected);
        mTextToSpeechManager.speak(speech);

        for (AlfredManagerCallbacks callbacks : mListenerManager.beginTraversing())
        {
            callbacks.onNotificationListenerConnected();
        }
        mListenerManager.endTraversing();

        mNotificationParserManager.initializeActiveNotifications();

        NotificationStatus notificationStatus;
        boolean isProfileEnabled = isProfileEnabled();
        if (isProfileEnabled)
        {
            notificationStatus = new NotificationStatusRunning(mApplicationContext);
        }
        else
        {
            Profile profile = mProfileManager.getProfile();
            notificationStatus = new NotificationStatusProfileNotEnabled(mApplicationContext, profile);
        }
        notification(notificationStatus, "TBD text", "onNotificationAccessSettingConfirmedEnabled");

        return true;
    }

    /**
     * @param reason                reason
     * @param elapsedMillis         elapsedMillis
     * @param ifHeadlessDelayMillis &gt; 0 to delay the given milliseconds if no UI is attached
     */
    private void onNotificationListenerNotConnected(NotConnectedReason reason, long elapsedMillis, int ifHeadlessDelayMillis)
    {
        FooLog.w(TAG, "onNotificationListenerNotConnected(reason=" + reason +
                      ", elapsedMillis=" + elapsedMillis +
                      ", ifHeadlessDelayMillis=" + ifHeadlessDelayMillis + ')');

        if (!mIsUserUnlocked)
        {
            FooLog.i(TAG, "onNotificationListenerNotConnected: mIsUserUnlocked == false; ignoring");
            return;
        }

        boolean headless = true;
        boolean handled = false;
        for (AlfredManagerCallbacks callbacks : mListenerManager.beginTraversing())
        {
            headless &= callbacks.getActivity() == null;
            handled |= callbacks.onNotificationListenerNotConnected(reason);
        }
        mListenerManager.endTraversing();

        if (headless && ifHeadlessDelayMillis > 0)
        {
            mDelayedRunnableNotificationAccessSettingDisabled = new DelayedRunnableNotificationListenerNotConnected(reason, elapsedMillis);
            mHandler.postDelayed(mDelayedRunnableNotificationAccessSettingDisabled, ifHeadlessDelayMillis);
            return;
        }

        String title = getNotificationListenerNotConnectedTitle(reason);
        String message = getNotificationListenerNotConnectedMessage(reason);

        if (headless)
        {
            String separator = getString(R.string.alfred_line_separator);
            String text = FooString.join(separator, title, message);
            FooPlatformUtils.toastLong(mApplicationContext, text);
        }

        mTextToSpeechManager.speak(new FooTextToSpeechBuilder()
                .appendSpeech(title)
                .appendSilenceWordBreak()
                .appendSpeech(message));

        NotificationStatus notificationStatus;
        boolean isProfileEnabled = isProfileEnabled();
        if (isProfileEnabled)
        {
            notificationStatus = new NotificationStatusNotificationAccessNotEnabled(mApplicationContext,
                    getString(R.string.alfred_running),
                    getString(R.string.alfred_waiting_for_notification_access)
                    , null);
        }
        else
        {
            Profile profile = mProfileManager.getProfile();
            notificationStatus = new NotificationStatusProfileNotEnabled(mApplicationContext, profile);
        }
        notification(notificationStatus, "TBD text", "onNotificationAccessSettingDisabled");
    }

    public String getNotificationListenerNotConnectedTitle(@NonNull NotConnectedReason reason)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(reason, "reason");
        int resId;
        switch (reason)
        {
            case ConfirmedNotEnabled:
                resId = R.string.alfred_notification_access_not_enabled;
                break;
            case ConnectedTimeout:
                resId = R.string.alfred_notification_listener_bind_timeout;
                break;
            default:
                throw new IllegalArgumentException("Unhandled reason == " + reason);
        }
        return getString(resId);
    }

    public String getNotificationListenerNotConnectedMessage(@NonNull NotConnectedReason reason)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(reason, "reason");
        int resId;
        switch (reason)
        {
            case ConfirmedNotEnabled:
                resId = R.string.alfred_please_enable_notification_access_for_the_X_application;
                break;
            case ConnectedTimeout:
                resId = R.string.alfred_please_reenable_notification_access_for_the_X_application;
                break;
            default:
                throw new IllegalArgumentException("Unhandled reason == " + reason);
        }

        String appName = getString(R.string.alfred_app_name);

        return getString(resId, appName);
    }

    private void onNotificationParsed(@NonNull AbstractNotificationParser parser)
    {
        if (parser instanceof AlfredNotificationParser)
        {
            onAlfredNotificationParsed((AlfredNotificationParser) parser);
        }
    }

    private void onAlfredNotificationParsed(AlfredNotificationParser parser)
    {
    }

    //
    //
    //

    private void onHeadsetConnectionChanged(HeadsetType headsetType, String headsetName, boolean isConnected)
    {
        FooLog.i(TAG, "onHeadsetConnectionChanged(headsetType=" + headsetType +
                      ", headsetName=" + FooString.quote(headsetName) +
                      ", isConnected=" + isConnected + ')');
        if (headsetName == null)
        {
            headsetName = "";
        }

        int resIdConnection = isConnected ? R.string.alfred_X_connected : R.string.alfred_X_disconnected;

        int resIdHeadphone;
        switch (headsetType)
        {
            case Bluetooth:
                resIdHeadphone = R.string.alfred_headphone_bluetooth_X;
                break;
            case Wired:
                resIdHeadphone = R.string.alfred_headphone_wired;
                break;
            default:
                throw new IllegalArgumentException("Unhandled headsetType == " + headsetType);
        }

        String textHeadphone = getString(resIdHeadphone, headsetName);
        String speech = getString(resIdConnection, textHeadphone);
        mTextToSpeechManager.speak(speech);
    }

    //
    // Screen…
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

                speech = getString(R.string.alfred_screen_on_after_being_off_for_X, FooString.getTimeDurationString(mApplicationContext, durationMs, TimeUnit.SECONDS));
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

                speech = getString(R.string.alfred_screen_off_after_being_on_for_X, FooString.getTimeDurationString(mApplicationContext, durationMs, TimeUnit.SECONDS));
            }
            else
            {
                speech = getString(R.string.alfred_screen_off);
            }
        }
        mTextToSpeechManager.speak(speech);
    }

    //
    // Charging/Ports…
    //

    private void updateChargePortInfo()
    {
        for (ChargePort chargingPort : mChargePortListener.getChargingPorts())
        {
            onChargePortConnected(chargingPort);
        }
    }

    private final Map<ChargePort, Long> mTimeChargingConnected    = new HashMap<>();
    private final Map<ChargePort, Long> mTimeChargingDisconnected = new HashMap<>();

    private void onChargePortConnected(ChargePort chargePort)
    {
        FooLog.i(TAG, "onChargePortConnected(chargePort=" + chargePort + ')');

        long now = System.currentTimeMillis();

        mTimeChargingConnected.put(chargePort, now);

        String speech;
        String chargePortName = getString(chargePort.getStringRes());
        //FooLog.i(TAG, "onChargePortConnected: chargePortName == " + FooString.quote(chargePortName));
        Long timeChargingDisconnectedMs = mTimeChargingDisconnected.remove(chargePort);
        if (timeChargingDisconnectedMs != null)
        {
            long elapsedMs = now - timeChargingDisconnectedMs;
            speech = getString(R.string.alfred_X_connected_after_being_disconnected_for_Y,
                    chargePortName,
                    FooString.getTimeDurationString(mApplicationContext, elapsedMs, TimeUnit.SECONDS));
        }
        else
        {
            speech = getString(R.string.alfred_X_connected, chargePortName);
        }
        mTextToSpeechManager.speak(speech);
    }

    private void onChargePortDisconnected(ChargePort chargePort)
    {
        FooLog.i(TAG, "onChargePortDisconnected(chargePort=" + chargePort + ')');

        long now = System.currentTimeMillis();

        mTimeChargingDisconnected.put(chargePort, now);

        String speech;
        String chargePortName = getString(chargePort.getStringRes());
        //FooLog.i(TAG, "onChargePortDisconnected: chargePortName == " + FooString.quote(chargePortName));
        Long timeChargingConnectedMs = mTimeChargingConnected.remove(chargePort);
        if (timeChargingConnectedMs != null)
        {
            long elapsedMs = now - timeChargingConnectedMs;
            speech = getString(R.string.alfred_X_disconnected_after_being_connected_for_Y,
                    chargePortName,
                    FooString.getTimeDurationString(mApplicationContext, elapsedMs, TimeUnit.SECONDS));
        }
        else
        {
            speech = getString(R.string.alfred_X_disconnected, chargePortName);
        }
        mTextToSpeechManager.speak(speech);
    }

    //
    // Data Connection…
    //

    private void updateDataConnectionInfo()
    {
        FooDataConnectionInfo dataConnectionInfo = mDataConnectionListener.getDataConnectionInfo();
        if (dataConnectionInfo.isConnected())
        {
            onDataConnected(dataConnectionInfo);
        }
        else
        {
            onDataDisconnected(dataConnectionInfo);
        }
    }

    private void onCellularOffHook()
    {
        mTextToSpeechManager.speak("Phone Call Started");
    }

    private void onCellularOnHook()
    {
        mTextToSpeechManager.speak("Phone Call Ended");
    }

    private final FooLongSparseArray<Long> mTimeDataConnected    = new FooLongSparseArray<>();
    private final FooLongSparseArray<Long> mTimeDataDisconnected = new FooLongSparseArray<>();

    private void onDataConnected(FooDataConnectionInfo dataConnectionInfo)
    {
        FooLog.i(TAG, "onDataConnected(dataConnectionInfo=" + dataConnectionInfo + ')');

        long now = System.currentTimeMillis();

        int dataConnectionType = dataConnectionInfo.getType();

        mTimeDataConnected.put(dataConnectionType, now);

        String speech;
        String dataConnectionTypeName = getString(dataConnectionInfo.getNetworkTypeResourceId(BuildConfig.DEBUG));
        //FooLog.i(TAG, "onDataConnected: dataConnectionTypeName == " + FooString.quote(dataConnectionTypeName));
        Long timeDataDisconnectedMs = mTimeDataDisconnected.remove(dataConnectionType);
        if (timeDataDisconnectedMs != null)
        {
            long elapsedMs = now - timeDataDisconnectedMs;
            speech = dataConnectionTypeName + " connected after being disconnected for " +
                     FooString.getTimeDurationString(mApplicationContext, elapsedMs, TimeUnit.SECONDS);
        }
        else
        {
            speech = dataConnectionTypeName + " connected";
        }
        mTextToSpeechManager.speak(speech);
    }

    private void onDataDisconnected(FooDataConnectionInfo dataConnectionInfo)
    {
        FooLog.i(TAG, "onDataDisconnected(dataConnectionInfo=" + dataConnectionInfo + ')');

        long now = System.currentTimeMillis();

        int dataConnectionType = dataConnectionInfo.getType();

        mTimeDataDisconnected.put(dataConnectionType, now);

        String speech;
        String dataConnectionTypeName = getString(dataConnectionInfo.getNetworkTypeResourceId(BuildConfig.DEBUG));
        //FooLog.i(TAG, "onDataDisconnected: dataConnectionTypeName == " + FooString.quote(dataConnectionTypeName));
        Long timeDataConnectedMs = mTimeDataConnected.remove(dataConnectionType);
        if (timeDataConnectedMs != null)
        {
            long elapsedMs = now - timeDataConnectedMs;
            speech = dataConnectionTypeName + " disconnected after being connected for " +
                     FooString.getTimeDurationString(mApplicationContext, elapsedMs, TimeUnit.SECONDS);
        }
        else
        {
            speech = dataConnectionTypeName + " disconnected";
        }
        mTextToSpeechManager.speak(speech);
    }

    //
    // Volume (candidate for a dedicated class)
    //

    private void volumeObserverStart(int audioStreamType)
    {
        mAudioStreamVolumeObserver.attach(audioStreamType, new OnAudioStreamVolumeChangedCallbacks()
        {
            @Override
            public void onAudioStreamVolumeChanged(int audioStreamType, int volume, int volumeMax, int volumePercent)
            {
                AlfredManager.this.onAudioStreamVolumeChanged(audioStreamType, volume, volumeMax, volumePercent);
            }
        });
    }

    private void onAudioStreamVolumeChanged(int audioStreamType, int volume, int volumeMax, int volumePercent)
    {
        //FooLog.d(TAG, "onAudioStreamVolumeChanged: MESSAGE_VOLUME_CHANGED audioStreamType volume");
        mHandler.removeMessages(Messages.VOLUME_CHANGED);
        mHandler.obtainAndSendMessageDelayed(Messages.VOLUME_CHANGED, audioStreamType, volumePercent, 800);

        if (audioStreamType == mTextToSpeechManager.getAudioStreamType())
        {
            for (AlfredManagerCallbacks callbacks : mListenerManager.beginTraversing())
            {
                callbacks.onTextToSpeechAudioStreamVolumeChanged(audioStreamType, volume);
            }
            mListenerManager.endTraversing();
        }
    }

    private interface Messages
    {
        /**
         * <ul>
         * <li>msg.arg1: audioStreamType</li>
         * <li>msg.arg2: volumePercent</li>
         * <li>msg.obj: ?</li>
         * </ul>
         */
        int VOLUME_CHANGED = 100;
    }

    private boolean handleMessage(Message msg)
    {
        switch (msg.what)
        {
            case Messages.VOLUME_CHANGED:
                onAudioStreamVolumeChanged(msg);
                break;
        }
        return false;
    }

    private void onAudioStreamVolumeChanged(Message msg)
    {
        int audioStreamType = msg.arg1;
        //FooLog.v(TAG, "onAudioStreamVolumeChanged: audioStreamType == " + audioStreamType);
        int volumePercent = msg.arg2;
        //FooLog.v(TAG, "onAudioStreamVolumeChanged: volume == " + volume);

        String audioStreamTypeName = FooAudioUtils.audioStreamTypeToString(mApplicationContext, audioStreamType);
        //String text = getString(R.string.alfred_X_volume_Y_of_Z, audioStreamTypeName, volume, volumeMax);
        String text = getString(R.string.alfred_X_volume_Y_percent, audioStreamTypeName, volumePercent);
        mTextToSpeechManager.speak(text);
    }
}

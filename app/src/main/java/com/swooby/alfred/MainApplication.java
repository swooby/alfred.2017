package com.swooby.alfred;

import android.app.Application;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.annotation.NonNull;

import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.bluetooth.FooBluetoothHeadsetConnectionListener;
import com.smartfoo.android.core.bluetooth.FooBluetoothHeadsetConnectionListener.OnBluetoothHeadsetConnectionCallbacks;
import com.smartfoo.android.core.bluetooth.FooBluetoothManager;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.media.FooWiredHeadsetConnectionListener;
import com.smartfoo.android.core.media.FooWiredHeadsetConnectionListener.OnWiredHeadsetConnectionCallbacks;
import com.swooby.alfred.Profile.Tokens;
import com.swooby.alfred.TextToSpeechManager.TextToSpeechCallbacks;
import com.swooby.alfred.notification.parsers.NotificationParserManager;
import com.swooby.alfred.notification.parsers.NotificationParserManager.NotificationParserManagerCallbacks;

public class MainApplication
        extends Application
{
    private static final String TAG = FooLog.TAG(MainApplication.class);

    /*
    public interface MainApplicationListener
    {
        boolean onNotificationListenerAccessDisabled();
    }

    private final FooListenerManager<MainApplicationListener> mListenerManager;
    */

    private final TextToSpeechManager       mTextToSpeechManager;
    private final NotificationParserManager mNotificationParserManager;

    private final OnBluetoothHeadsetConnectionCallbacks mBluetoothHeadsetConnectionCallbacks;
    private final OnWiredHeadsetConnectionCallbacks     mWiredHeadsetConnectionCallbacks;

    private AppPreferences      mAppPreferences;
    private FooBluetoothManager mBluetoothManager;

    private FooBluetoothHeadsetConnectionListener mBluetoothHeadsetConnectionListener;
    private FooWiredHeadsetConnectionListener     mWiredHeadsetConnectionListener;

    private boolean mIsHeadsetConnected;
    private boolean mIsEnabled;


    public MainApplication()
    {
        //mListenerManager = new FooListenerManager<>();

        mTextToSpeechManager = new TextToSpeechManager(new TextToSpeechCallbacks()
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
                return mAppPreferences.getVoiceName();
            }

            @Override
            public void onSetVoiceName(String voiceName)
            {
                mAppPreferences.setVoiceName(voiceName);
            }

            @Override
            public int getVoiceAudioStreamType()
            {
                return mAppPreferences.getVoiceAudioStreamType();
            }

            @Override
            public boolean isEnabled()
            {
                return MainApplication.this.isEnabled();
            }
        });

        mNotificationParserManager = new NotificationParserManager(new NotificationParserManagerCallbacks()
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
                return mTextToSpeechManager;
            }
        });

        mBluetoothHeadsetConnectionCallbacks = new OnBluetoothHeadsetConnectionCallbacks()
        {
            @Override
            public void onBluetoothHeadsetConnected(BluetoothDevice bluetoothDevice)
            {
                MainApplication.this.onBluetoothHeadsetConnected(bluetoothDevice);
            }

            @Override
            public void onBluetoothHeadsetDisconnected(BluetoothDevice bluetoothDevice)
            {
                MainApplication.this.onBluetoothHeadsetDisconnected(bluetoothDevice);
            }
        };

        mWiredHeadsetConnectionCallbacks = new OnWiredHeadsetConnectionCallbacks()
        {
            @Override
            public void onWiredHeadsetConnected(String name, boolean hasMicrophone)
            {
                MainApplication.this.onWiredHeadsetConnected(name, hasMicrophone);
            }

            @Override
            public void onWiredHeadsetDisconnected(String name, boolean hasMicrophone)
            {
                MainApplication.this.onWiredHeadsetDisconnected(name, hasMicrophone);
            }
        };
    }

    public AppPreferences getAppPreferences()
    {
        return mAppPreferences;
    }

    public FooBluetoothManager getBluetoothManager()
    {
        return mBluetoothManager;
    }

    public TextToSpeechManager getTextToSpeechManager()
    {
        return mTextToSpeechManager;
    }

    private boolean isEnabled()
    {
        return mIsEnabled;
    }

    private boolean updateEnabledState()
    {
        FooLog.v(TAG, "updateEnabledState()");

        boolean isEnabled;
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
                isEnabled = mIsHeadsetConnected;
                break;
            default:
                throw new IllegalArgumentException("unhandled profileToken=" + FooString.quote(profileToken));
        }

        if (mIsEnabled != isEnabled)
        {
            mIsEnabled = isEnabled;

            //FooNotification notification = new FooNotification();

            /*
            if (mIsNotificationListenerBound2 == null)
            {
                boolean isNotificationListenerBound = FooNotificationListener.isNotificationListenerBound();
                if ()
                {
                    mIsEnabled = isEnabled;

                    if (mIsEnabled)
                    {
                        onEnabled();
                    }
                    else
                    {
                        onDisabled();
                    }
                }
                else
                {
                    String appName = getString(R.string.app_name);
                    String text = getString(R.string.notification_access_is_disabled_please_enable, appName);
                    speak(true, true, text);
                }
            }
            */
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

    @Override
    public void onCreate()
    {
        FooLog.i(TAG, "+onCreate()");
        super.onCreate();

        mAppPreferences = new AppPreferences(this);

        mTextToSpeechManager.initialize();

        mNotificationParserManager.initialize();

        mBluetoothManager = new FooBluetoothManager(this);

        initializeHeadsetConnectionListeners();

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

    private void initializeHeadsetConnectionListeners()
    {
        mBluetoothHeadsetConnectionListener = mBluetoothManager.getBluetoothHeadsetConnectionListener();
        mBluetoothHeadsetConnectionListener.attach(mBluetoothHeadsetConnectionCallbacks);

        mWiredHeadsetConnectionListener = new FooWiredHeadsetConnectionListener(this);
        mWiredHeadsetConnectionListener.attach(mWiredHeadsetConnectionCallbacks);
    }

    /*
    private boolean isProfileTokenHeadphones()
    {
        return Tokens.HEADPHONES_ONLY.equals(mAppPreferences.getProfileToken());
    }
    */

    /**
     * @return true if the state changed and updateEnabledState() was called, otherwise false
     */
    private boolean updateHeadsetState()
    {
        FooLog.v(TAG, "updateHeadsetState()");

        boolean isHeadsetConnected = mWiredHeadsetConnectionListener.isWiredHeadsetConnected() ||
                                     mBluetoothHeadsetConnectionListener.isBluetoothHeadsetConnected();
        FooLog.i(TAG, "updateHeadsetState: isHeadsetConnected=" + isHeadsetConnected);
        FooLog.i(TAG, "updateHeadsetState: mIsHeadsetConnected=" + mIsHeadsetConnected);

        if (mIsHeadsetConnected != isHeadsetConnected)
        {
            mIsHeadsetConnected = isHeadsetConnected;

            updateEnabledState();

            return true;
        }

        return false;
    }

    private void onBluetoothHeadsetConnected(BluetoothDevice bluetoothDevice)
    {
        FooLog.i(TAG, "onBluetoothHeadsetConnected(bluetoothDevice=" + bluetoothDevice + ')');
        updateHeadsetState();
        String name = bluetoothDevice.getName();
        speakHeadsetConnectionState(R.string.bluetooth_headset_X_connected, name);
    }

    private void onBluetoothHeadsetDisconnected(BluetoothDevice bluetoothDevice)
    {
        FooLog.i(TAG, "onBluetoothHeadsetDisconnected(bluetoothDevice=" + bluetoothDevice + ')');
        updateHeadsetState();
    }

    private void onWiredHeadsetConnected(String name, boolean hasMicrophone)
    {
        FooLog.i(TAG, "onWiredHeadsetConnected(name=" + FooString.quote(name) +
                      ", hasMicrophone=" + hasMicrophone + ')');
        updateHeadsetState();
        speakHeadsetConnectionState(R.string.wired_headset_X_connected, name);
    }

    private void onWiredHeadsetDisconnected(String name, boolean hasMicrophone)
    {
        FooLog.i(TAG, "onWiredHeadsetDisconnected(name=" + FooString.quote(name) +
                      ", hasMicrophone=" + hasMicrophone + ')');
        updateHeadsetState();
    }

    private void speakHeadsetConnectionState(int resId, String headsetName)
    {
        if (headsetName == null)
        {
            headsetName = "";
        }
        String speech = getString(resId, headsetName);
        mTextToSpeechManager.speak(speech);
    }

    /*
    public boolean isRecognitionAvailable()
    {
        return SpeechRecognizer.isRecognitionAvailable(this);
    }
    */
}

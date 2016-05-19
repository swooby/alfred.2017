package com.swooby.alfred;

import android.app.Application;
import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.speech.SpeechRecognizer;
import android.speech.tts.Voice;
import android.support.annotation.NonNull;

import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.bluetooth.FooBluetoothHeadsetConnectionListener;
import com.smartfoo.android.core.bluetooth.FooBluetoothHeadsetConnectionListener.OnBluetoothHeadsetConnectedCallbacks;
import com.smartfoo.android.core.bluetooth.FooBluetoothManager;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.media.FooAudioUtils;
import com.smartfoo.android.core.notification.FooNotificationListener;
import com.smartfoo.android.core.platform.FooPlatformUtils;
import com.smartfoo.android.core.texttospeech.FooTextToSpeech;
import com.smartfoo.android.core.texttospeech.FooTextToSpeechBuilder;
import com.swooby.alfred.Profile.Tokens;
import com.swooby.alfred.notification.AppNotificationListener;

import java.util.LinkedHashMap;
import java.util.Map;

public class MainApplication
        extends Application
        implements OnBluetoothHeadsetConnectedCallbacks
{
    private static final String TAG = FooLog.TAG(MainApplication.class);


    private final Map<String, BluetoothDevice> mConnectedBluetoothHeadsets;

    private AppPreferences      mAppPreferences;
    private FooBluetoothManager mBluetoothManager;
    private FooTextToSpeech     mTextToSpeech;

    private AppNotificationListener mAppNotificationListener;

    private SpeechRecognizer mSpeechRecognizer;

    public MainApplication()
    {
        mConnectedBluetoothHeadsets = new LinkedHashMap<>();
    }

    public AppPreferences getAppPreferences()
    {
        return mAppPreferences;
    }

    public FooBluetoothManager getBluetoothManager()
    {
        return mBluetoothManager;
    }

    public FooTextToSpeech getTextToSpeech()
    {
        return mTextToSpeech;
    }

    public void setVoice(
            @NonNull
            Voice voice)
    {
        mAppPreferences.setVoice(voice);
        mTextToSpeech.setVoice(voice);
    }

    public int getVoiceAudioStreamType()
    {
        return mTextToSpeech.getAudioStreamType();
    }

    public void setVoiceAudioStreamType(int audioStreamType)
    {
        mTextToSpeech.setAudioStreamType(audioStreamType);
    }

    public void speak(String text)
    {
        if (isSpeechEnabled())
        {
            mTextToSpeech.speak(text);
        }
    }

    public void speak(String text, boolean clear)
    {
        if (isSpeechEnabled())
        {
            mTextToSpeech.speak(text, clear, null);
        }
    }

    public void speak(String text, Runnable runAfter)
    {
        if (isSpeechEnabled())
        {
            mTextToSpeech.speak(text, runAfter);
        }
    }

    public void speak(
            @NonNull
            FooTextToSpeechBuilder builder)
    {
        if (isSpeechEnabled())
        {
            mTextToSpeech.speak(builder);
        }
    }

    public void speak(
            @NonNull
            FooTextToSpeechBuilder builder, boolean clear)
    {
        if (isSpeechEnabled())
        {
            mTextToSpeech.speak(builder, clear, null);
        }
    }

    /*
    public void speak(String text)
    {
        speak(false, false, text, null);
    }

    public void speak(String text, Runnable runAfter)
    {
        speak(false, false, text, runAfter);
    }
    */

    public void speak(boolean force, boolean toast, String text)
    {
        speak(force, toast, text, null);
    }

    public void speak(boolean force, boolean toast, String text, Runnable runAfter)
    {
        if (force || isSpeechEnabled())
        {
            if (toast)
            {
                FooPlatformUtils.toastLong(this, text);
            }

            mTextToSpeech.speak(text, runAfter);
        }
    }

    public void silence(int durationInMs)
    {
        silence(false, durationInMs);
    }

    public void silence(boolean force, int durationInMs)
    {
        if (force || isSpeechEnabled())
        {
            mTextToSpeech.silence(durationInMs);
        }
    }

    private boolean isSpeechEnabled()
    {
        String profileToken = mAppPreferences.getProfileToken();
        switch (profileToken)
        {
            case Tokens.DISABLED:
                return false;
            case Tokens.ALWAYS_ON:
                return true;
            case Tokens.HEADPHONES:
                synchronized (mConnectedBluetoothHeadsets)
                {
                    return mConnectedBluetoothHeadsets.size() > 0;
                }
            default:
                throw new IllegalArgumentException("unhandled profileToken=" + FooString.quote(profileToken));
        }
    }

    private boolean isProfileTokenHeadphones()
    {
        return Tokens.HEADPHONES.equals(mAppPreferences.getProfileToken());
    }

    @Override
    public void onBluetoothDeviceConnected(BluetoothDevice bluetoothDevice)
    {
        FooLog.i(TAG, "onBluetoothDeviceConnected(bluetoothDevice=" + bluetoothDevice + ')');
        synchronized (mConnectedBluetoothHeadsets)
        {
            mConnectedBluetoothHeadsets.put(bluetoothDevice.getAddress(), bluetoothDevice);
            if (mConnectedBluetoothHeadsets.size() == 1 && isProfileTokenHeadphones())
            {
                speak("Bluetooth Headset Connected");
                if (FooNotificationListener.isNotificationListenerBound())
                {
                    mAppNotificationListener.onNotificationListenerBound();
                }
            }
        }
    }

    @Override
    public void onBluetoothDeviceDisconnected(BluetoothDevice bluetoothDevice)
    {
        FooLog.i(TAG, "onBluetoothDeviceDisconnected(bluetoothDevice=" + bluetoothDevice + ')');
        synchronized (mConnectedBluetoothHeadsets)
        {
            mConnectedBluetoothHeadsets.remove(bluetoothDevice.getAddress());
        }
    }

    public boolean isRecognitionAvailable()
    {
        return SpeechRecognizer.isRecognitionAvailable(this);
    }

    @Override
    public void onCreate()
    {
        FooLog.i(TAG, "+onCreate()");
        super.onCreate();

        mAppPreferences = new AppPreferences(this);

        mBluetoothManager = new FooBluetoothManager(this);

        FooBluetoothHeadsetConnectionListener bluetoothHeadsetConnectionListener = mBluetoothManager.getBluetoothHeadsetConnectionListener();
        bluetoothHeadsetConnectionListener.attach(this);

        String voiceName = mAppPreferences.getVoiceName();
        FooLog.i(TAG, "onCreate: voiceName=" + FooString.quote(voiceName));

        int voiceAudioStreamType = mAppPreferences.getVoiceAudioStreamType();
        FooLog.i(TAG, "onCreate: voiceAudioStreamType=" + FooAudioUtils.audioStreamTypeToString(voiceAudioStreamType));

        mTextToSpeech = FooTextToSpeech.getInstance();
        mTextToSpeech.setVoiceName(voiceName);
        mTextToSpeech.setAudioStreamType(voiceAudioStreamType);
        mTextToSpeech.start(this);

        if (!isRecognitionAvailable())
        {
            // TODO:(pv) Better place for initialization and indication of failure...
            return;
        }

        mAppNotificationListener = new AppNotificationListener(this);

        FooNotificationListener.addListener(mAppNotificationListener);

        new Handler().postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                //
                // HACK required to detect non-binding when re-installing app
                // even if notification access is enabled:
                //  http://stackoverflow.com/a/37081128/252308
                //
                if (!FooNotificationListener.isNotificationListenerBound())
                {
                    mAppNotificationListener.onNotificationListenerUnbound();
                }
            }
        }, 250);

        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        FooLog.i(TAG, "-onCreate()");
    }
    }
}

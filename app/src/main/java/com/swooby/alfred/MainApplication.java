package com.swooby.alfred;

import android.app.Application;
import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.service.notification.StatusBarNotification;
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
import com.smartfoo.android.core.notification.FooNotificationListener.FooNotificationListenerCallbacks;
import com.smartfoo.android.core.platform.FooPlatformUtils;
import com.smartfoo.android.core.texttospeech.FooTextToSpeech;
import com.smartfoo.android.core.texttospeech.FooTextToSpeechBuilder;
import com.swooby.alfred.Profile.Tokens;
import com.swooby.alfred.notification.parsers.AbstractNotificationParser;
import com.swooby.alfred.notification.parsers.AbstractNotificationParser.NotificationParseResult;
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
import java.util.LinkedHashMap;
import java.util.Map;

public class MainApplication
        extends Application
{
    private static final String TAG = FooLog.TAG(MainApplication.class);


    private final Map<String, BluetoothDevice> mConnectedBluetoothHeadsets;

    private AppPreferences      mAppPreferences;
    private FooBluetoothManager mBluetoothManager;
    private FooTextToSpeech     mTextToSpeech;

    private final FooNotificationListenerCallbacks        mFooNotificationListenerCallbacks;
    private final Map<String, AbstractNotificationParser> mNotificationParsers;

    private final OnBluetoothHeadsetConnectedCallbacks mBluetoothHeadsetConnectedCallbacks;


    public MainApplication()
    {
        mConnectedBluetoothHeadsets = new LinkedHashMap<>();

        mFooNotificationListenerCallbacks = new FooNotificationListenerCallbacks()
        {
            @Override
            public void onNotificationListenerBound()
            {
                MainApplication.this.onNotificationListenerBound();
            }

            @Override
            public void onNotificationListenerUnbound()
            {
                MainApplication.this.onNotificationListenerUnbound();
            }

            @Override
            public void onNotificationPosted(StatusBarNotification sbn)
            {
                MainApplication.this.onNotificationPosted(sbn);
            }

            @Override
            public void onNotificationRemoved(StatusBarNotification sbn)
            {
                MainApplication.this.onNotificationRemoved(sbn);
            }
        };

        mNotificationParsers = new HashMap<>();

        mBluetoothHeadsetConnectedCallbacks = new OnBluetoothHeadsetConnectedCallbacks()
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

    @Override
    public void onCreate()
    {
        FooLog.i(TAG, "+onCreate()");
        super.onCreate();

        mAppPreferences = new AppPreferences(this);

        initializeTextToSpeech();
        initializeNotificationListener();

        mBluetoothManager = new FooBluetoothManager(this);

        initializeBluetoothHeadsetConnectionListener();

        FooLog.i(TAG, "-onCreate()");
    }

    private void initializeTextToSpeech()
    {
        String voiceName = mAppPreferences.getVoiceName();
        FooLog.i(TAG, "initializeTextToSpeech: voiceName=" + FooString.quote(voiceName));

        int voiceAudioStreamType = mAppPreferences.getVoiceAudioStreamType();
        FooLog.i(TAG, "initializeTextToSpeech: voiceAudioStreamType=" +
                      FooAudioUtils.audioStreamTypeToString(voiceAudioStreamType));

        mTextToSpeech = FooTextToSpeech.getInstance();
        mTextToSpeech.setVoiceName(voiceName);
        mTextToSpeech.setAudioStreamType(voiceAudioStreamType);
        mTextToSpeech.start(this);
    }

    private void initializeNotificationListener()
    {
        addNotificationParser(new DownloadManagerNotificationParser(this));
        addNotificationParser(new GoogleCameraNotificationParser(this));
        addNotificationParser(new GoogleDialerNotificationParser(this));
        addNotificationParser(new GoogleHangoutsNotificationParser(this));
        addNotificationParser(new GoogleNowNotificationParser(this));
        addNotificationParser(new GooglePhotosNotificationParser(this));
        addNotificationParser(new GooglePlayStoreNotificationParser(this));
        addNotificationParser(new PandoraNotificationParser(this));
        addNotificationParser(new SpotifyNotificationParser(this));

        FooNotificationListener.addListener(mFooNotificationListenerCallbacks);
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

    private void addNotificationParser(AbstractNotificationParser notificationParser)
    {
        mNotificationParsers.put(notificationParser.getPackageName(), notificationParser);
    }

    private void initializeBluetoothHeadsetConnectionListener()
    {
        FooBluetoothHeadsetConnectionListener bluetoothHeadsetConnectionListener = mBluetoothManager.getBluetoothHeadsetConnectionListener();
        bluetoothHeadsetConnectionListener.attach(mBluetoothHeadsetConnectedCallbacks);
    }

    private void onNotificationListenerBound()
    {
    }

    private void onNotificationListenerUnbound()
    {
        String appName = getString(R.string.app_name);
        String text = getString(R.string.notification_access_is_disabled_please_enable, appName);
        speak(text);
    }

    private void onNotificationPosted(StatusBarNotification sbn)
    {
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
            result = AbstractNotificationParser.defaultOnNotificationPosted(true, this, sbn);
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
        String packageName = sbn.getPackageName();
        FooLog.d(TAG, "onNotificationRemoved: packageName=" + FooString.quote(packageName));

        AbstractNotificationParser notificationParser = mNotificationParsers.get(packageName);
        if (notificationParser != null)
        {
            notificationParser.onNotificationRemoved(sbn);
        }
    }

    private boolean isProfileTokenHeadphones()
    {
        return Tokens.HEADPHONES.equals(mAppPreferences.getProfileToken());
    }

    private void onBluetoothHeadsetConnected(BluetoothDevice bluetoothDevice)
    {
        FooLog.i(TAG, "onBluetoothHeadsetConnected(bluetoothDevice=" + bluetoothDevice + ')');
        synchronized (mConnectedBluetoothHeadsets)
        {
            mConnectedBluetoothHeadsets.put(bluetoothDevice.getAddress(), bluetoothDevice);
            if (mConnectedBluetoothHeadsets.size() == 1 && isProfileTokenHeadphones())
            {
                speak("Bluetooth Headset Connected");
                if (FooNotificationListener.isNotificationListenerBound())
                {
                    onNotificationListenerBound();
                }
            }
        }
    }

    private void onBluetoothHeadsetDisconnected(BluetoothDevice bluetoothDevice)
    {
        FooLog.i(TAG, "onBluetoothHeadsetDisconnected(bluetoothDevice=" + bluetoothDevice + ')');
        synchronized (mConnectedBluetoothHeadsets)
        {
            mConnectedBluetoothHeadsets.remove(bluetoothDevice.getAddress());
        }
    }
}

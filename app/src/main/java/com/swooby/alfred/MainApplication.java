package com.swooby.alfred;

import android.app.Application;
import android.speech.SpeechRecognizer;

import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.notification.FooNotificationListener;
import com.smartfoo.android.core.texttospeech.FooTextToSpeech;
import com.swooby.alfred.notification.AppNotificationListener;

public class MainApplication
        extends Application
{
    private static final String TAG = FooLog.TAG(MainApplication.class);

    public static final String KEYPHRASE = "alfred";

    private AppPreferences mAppPreferences;

    private FooTextToSpeech mTextToSpeech;

    private SpeechRecognizer mSpeechRecognizer;

    public AppPreferences getAppPreferences()
    {
        return mAppPreferences;
    }

    public FooTextToSpeech getTextToSpeech()
    {
        return mTextToSpeech;
    }

    @Override
    public void onCreate()
    {
        FooLog.i(TAG, "+onCreate()");
        super.onCreate();

        mAppPreferences = new AppPreferences(this);

        String voiceName = mAppPreferences.getVoiceName();

        mTextToSpeech = FooTextToSpeech.getInstance();
        mTextToSpeech.setVoiceName(voiceName);
        mTextToSpeech.start(this);

        if (!isRecognitionAvailable())
        {
            // TODO:(pv) Better place for initialization and indication of failure...
            return;
        }

        AppNotificationListener appNotificationListener = new AppNotificationListener(this, mTextToSpeech);

        FooNotificationListener.addListener(appNotificationListener);

        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        FooLog.i(TAG, "-onCreate()");
    }
    }
}

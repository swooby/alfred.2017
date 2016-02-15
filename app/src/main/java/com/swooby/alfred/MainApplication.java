package com.swooby.alfred;

import android.app.Application;
import android.speech.SpeechRecognizer;

import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.texttospeech.FooTextToSpeech;

public class MainApplication
        extends Application
{
    private static final String TAG = FooLog.TAG("MainApplication");

    private static final String KEYPHRASE = "alfred";

    private FooTextToSpeech mTextToSpeech;

    private SpeechRecognizer mSpeechRecognizer;

    @Override
    public void onCreate()
    {
        FooLog.i(TAG, "+onCreate()");
        super.onCreate();

        mTextToSpeech = FooTextToSpeech.getInstance();
        mTextToSpeech.start(this);

        if (!SpeechRecognizer.isRecognitionAvailable(this))
        {
            //...
        }

        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        FooLog.i(TAG, "+onCreate()");
    }
}

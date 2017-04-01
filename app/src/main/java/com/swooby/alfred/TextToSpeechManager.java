package com.swooby.alfred;

import android.app.Activity;
import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.support.annotation.NonNull;

import com.smartfoo.android.core.FooListenerManager;
import com.smartfoo.android.core.FooRun;
import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.annotations.NonNullNonEmpty;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.media.FooAudioUtils;
import com.smartfoo.android.core.texttospeech.FooTextToSpeech;
import com.smartfoo.android.core.texttospeech.FooTextToSpeech.FooTextToSpeechCallbacks;
import com.smartfoo.android.core.texttospeech.FooTextToSpeechBuilder;
import com.smartfoo.android.core.texttospeech.FooTextToSpeechHelper;

import java.util.Set;

public class TextToSpeechManager
{
    private static final String TAG = FooLog.TAG(TextToSpeechManager.class);

    public interface TextToSpeechManagerConfiguration
    {
        @NonNullNonEmpty
        String getVoiceName();

        void setVoiceName(String voiceName);

        int getAudioStreamType();

        boolean isEnabled();
    }

    public static class TextToSpeechManagerCallbacks
            implements FooTextToSpeechCallbacks
    {
        @Override
        public void onTextToSpeechInitialized(int status)
        {
            // ignore
        }

        public void onTextToSpeechVoiceNameSet(String voiceName)
        {
            // ignore
        }
    }

    private final Context                                          mContext;
    private final TextToSpeechManagerConfiguration                 mConfiguration;
    private final FooListenerManager<TextToSpeechManagerCallbacks> mListenerManager;
    private final FooTextToSpeech                                  mTextToSpeech;

    public TextToSpeechManager(@NonNull Context context, @NonNull TextToSpeechManagerConfiguration configuration)
    {
        FooLog.v(TAG, "+TextToSpeechManager(...)");

        FooRun.throwIllegalArgumentExceptionIfNull(context, "context");
        FooRun.throwIllegalArgumentExceptionIfNull(configuration, "configuration");

        mContext = context;
        mConfiguration = configuration;

        mListenerManager = new FooListenerManager<>();

        mTextToSpeech = FooTextToSpeech.getInstance();
        mTextToSpeech.attach(new FooTextToSpeechCallbacks()
        {
            @Override
            public void onTextToSpeechInitialized(int status)
            {
                TextToSpeechManager.this.onTextToSpeechInitialized(status);
            }
        });

        FooLog.v(TAG, "-TextToSpeechManager(...)");
    }

    public int getAudioStreamType()
    {
        return mTextToSpeech.getAudioStreamType();
    }

    public void setAudioStreamType(int audioStreamType)
    {
        mTextToSpeech.setAudioStreamType(audioStreamType);
    }

    public boolean isEnabled()
    {
        return mConfiguration.isEnabled();
    }

    public Set<Voice> getVoices()
    {
        return mTextToSpeech.getVoices();
    }

    public String getVoiceName()
    {
        return mTextToSpeech.getVoiceName();
    }

    public void setVoice(Voice voice)
    {
        setVoiceName(voice != null ? voice.getName() : null);
    }

    public void setVoiceName(String voiceName)
    {
        if (!mTextToSpeech.setVoiceName(voiceName))
        {
            return;
        }

        mConfiguration.setVoiceName(voiceName);

        for (TextToSpeechManagerCallbacks callbacks : mListenerManager.beginTraversing())
        {
            callbacks.onTextToSpeechVoiceNameSet(voiceName);
        }
        mListenerManager.endTraversing();
    }

    /*
    public boolean isInitialized()
    {
        return mTextToSpeech.isInitialized();
    }
    */

    private void onTextToSpeechInitialized(int status)
    {
        for (TextToSpeechManagerCallbacks callbacks : mListenerManager.beginTraversing())
        {
            callbacks.onTextToSpeechInitialized(status);
        }
        mListenerManager.endTraversing();
    }

    public void attach(@NonNull TextToSpeechManagerCallbacks listener)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(listener, "listener");

        mListenerManager.attach(listener);

        if (mTextToSpeech.isStarted())
        {
            if (mTextToSpeech.isInitialized())
            {
                listener.onTextToSpeechInitialized(TextToSpeech.SUCCESS);
            }
        }
        else
        {
            if (mListenerManager.size() == 1)
            {
                String voiceName = mConfiguration.getVoiceName();
                FooLog.v(TAG, "attach: voiceName=" + FooString.quote(voiceName));

                int audioStreamType = mConfiguration.getAudioStreamType();
                FooLog.v(TAG, "attach: audioStreamType=" + FooAudioUtils.audioStreamTypeToString(audioStreamType));

                mTextToSpeech.setVoiceName(voiceName);
                mTextToSpeech.setAudioStreamType(audioStreamType);
                mTextToSpeech.start(mContext);
            }
        }
    }

    public void detach(TextToSpeechManagerCallbacks listener)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(listener, "listener");
        mListenerManager.detach(listener);
    }

    public boolean requestTextToSpeechData(Activity activity, int requestCode)
    {
        if (!mTextToSpeech.isStarted())
        {
            return false;
        }

        if (!mTextToSpeech.isInitialized())
        {
            return false;
        }

        FooTextToSpeechHelper.requestTextToSpeechData(activity, requestCode);

        return true;
    }

    //
    //
    //

    public void speak(String text)
    {
        speak(text, null);
    }

    public void speak(String text, Runnable runAfter)
    {
        speak(false, text, runAfter);
    }

    public void speak(boolean clear, String text)
    {
        speak(clear, text, null);
    }

    public void speak(boolean clear, String text, Runnable runAfter)
    {
        if (!isEnabled())
        {
            return;
        }

        mTextToSpeech.speak(clear, text, runAfter);
    }

    public void speak(@NonNull FooTextToSpeechBuilder builder)
    {
        speak(builder, null);
    }

    public void speak(@NonNull FooTextToSpeechBuilder builder, Runnable runAfter)
    {
        speak(false, builder, runAfter);
    }

    public void speak(boolean clear, @NonNull FooTextToSpeechBuilder builder)
    {
        speak(clear, builder, null);
    }

    public void speak(boolean clear, @NonNull FooTextToSpeechBuilder builder, Runnable runAfter)
    {
        if (!isEnabled())
        {
            return;
        }

        mTextToSpeech.speak(clear, builder, runAfter);
    }

    public void silence(int durationInMs)
    {
        if (!isEnabled())
        {
            return;
        }

        mTextToSpeech.silence(durationInMs);
    }
}

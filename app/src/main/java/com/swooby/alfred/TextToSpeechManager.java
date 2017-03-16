package com.swooby.alfred;

import android.content.Context;
import android.speech.tts.Voice;
import android.support.annotation.NonNull;

import com.smartfoo.android.core.FooRun;
import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.annotations.NonNullNonEmpty;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.media.FooAudioUtils;
import com.smartfoo.android.core.platform.FooPlatformUtils;
import com.smartfoo.android.core.texttospeech.FooTextToSpeech;
import com.smartfoo.android.core.texttospeech.FooTextToSpeech.FooTextToSpeechCallbacks;
import com.smartfoo.android.core.texttospeech.FooTextToSpeechBuilder;

import java.util.Set;

public class TextToSpeechManager
{
    private static final String TAG = FooLog.TAG(TextToSpeechManager.class);

    public interface TextToSpeechCallbacks
    {
        @NonNull
        Context getContext();

        @NonNullNonEmpty
        String getVoiceName();

        void onSetVoiceName(String voiceName);

        int getVoiceAudioStreamType();

        boolean isEnabled();
    }

    private final TextToSpeechCallbacks mCallbacks;

    private FooTextToSpeech mTextToSpeech;

    public TextToSpeechManager(@NonNull TextToSpeechCallbacks callbacks)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(callbacks, "callbacks");

        mCallbacks = callbacks;
    }

    public int getVoiceAudioStreamType()
    {
        return mTextToSpeech.getAudioStreamType();
    }

    public void setVoiceAudioStreamType(int audioStreamType)
    {
        mTextToSpeech.setAudioStreamType(audioStreamType);
    }

    @NonNull
    protected Context getContext()
    {
        return mCallbacks.getContext();
    }

    public boolean isEnabled()
    {
        return mCallbacks.isEnabled();
    }

    public boolean isInitialized()
    {
        return mTextToSpeech.isInitialized();
    }

    public void attach(FooTextToSpeechCallbacks listener)
    {
        mTextToSpeech.attach(listener);
    }

    public void detach(FooTextToSpeechCallbacks listener)
    {
        mTextToSpeech.detach(listener);
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
        if (mTextToSpeech.setVoiceName(voiceName))
        {
            mCallbacks.onSetVoiceName(voiceName);
        }
    }

    public void initialize()
    {
        String voiceName = mCallbacks.getVoiceName();
        FooLog.i(TAG, "initialize: voiceName=" + FooString.quote(voiceName));

        int voiceAudioStreamType = mCallbacks.getVoiceAudioStreamType();
        FooLog.i(TAG, "initialize: voiceAudioStreamType=" +
                      FooAudioUtils.audioStreamTypeToString(voiceAudioStreamType));

        mTextToSpeech = FooTextToSpeech.getInstance();
        mTextToSpeech.setVoiceName(voiceName);
        mTextToSpeech.setAudioStreamType(voiceAudioStreamType);
        mTextToSpeech.start(getContext());
    }

    public void speak(String text)
    {
        if (isEnabled())
        {
            mTextToSpeech.speak(text);
        }
    }

    public void speak(String text, boolean clear)
    {
        if (isEnabled())
        {
            mTextToSpeech.speak(text, clear, null);
        }
    }

    public void speak(String text, Runnable runAfter)
    {
        if (isEnabled())
        {
            mTextToSpeech.speak(text, runAfter);
        }
    }

    public void speak(
            @NonNull
                    FooTextToSpeechBuilder builder)
    {
        if (isEnabled())
        {
            mTextToSpeech.speak(builder);
        }
    }

    public void speak(
            @NonNull
                    FooTextToSpeechBuilder builder, boolean clear)
    {
        if (isEnabled())
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
        if (force || isEnabled())
        {
            if (toast)
            {
                FooPlatformUtils.toastLong(getContext(), text);
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
        if (force || isEnabled())
        {
            mTextToSpeech.silence(durationInMs);
        }
    }
}

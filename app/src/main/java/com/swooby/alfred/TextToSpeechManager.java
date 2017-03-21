package com.swooby.alfred;

import android.app.Activity;
import android.content.Context;
import android.speech.tts.Voice;
import android.support.annotation.NonNull;

import com.smartfoo.android.core.FooListenerManager;
import com.smartfoo.android.core.FooRun;
import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.annotations.NonNullNonEmpty;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.media.FooAudioUtils;
import com.smartfoo.android.core.platform.FooPlatformUtils;
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
        @NonNull
        Context getContext();

        @NonNullNonEmpty
        String getVoiceName();

        int getAudioStreamType();

        boolean isEnabled();
    }

    public static class TextToSpeechManagerCallbacks
            implements FooTextToSpeechCallbacks
    {
        @Override
        public void onTextToSpeechInitialized()
        {
            // ignore
        }

        public void onTextToSpeechVoiceNameSet(String voiceName)
        {
            // ignore
        }
    }

    private final TextToSpeechManagerConfiguration                 mConfiguration;
    private final FooListenerManager<TextToSpeechManagerCallbacks> mListenerManager;
    private final FooTextToSpeech                                  mTextToSpeech;

    public TextToSpeechManager(@NonNull TextToSpeechManagerConfiguration configuration)
    {
        FooLog.v(TAG, "+TextToSpeechManager(...)");

        FooRun.throwIllegalArgumentExceptionIfNull(configuration, "configuration");

        mConfiguration = configuration;

        mListenerManager = new FooListenerManager<>();
        mTextToSpeech = FooTextToSpeech.getInstance();
        mTextToSpeech.attach(new FooTextToSpeechCallbacks()
        {
            @Override
            public void onTextToSpeechInitialized()
            {
                TextToSpeechManager.this.onTextToSpeechInitialized();
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

    @NonNull
    protected Context getContext()
    {
        return mConfiguration.getContext();
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

    private void onTextToSpeechInitialized()
    {
        for (TextToSpeechManagerCallbacks callbacks : mListenerManager.beginTraversing())
        {
            callbacks.onTextToSpeechInitialized();
        }
        mListenerManager.endTraversing();
    }

    public void attach(TextToSpeechManagerCallbacks listener)
    {
        mListenerManager.attach(listener);

        if (mTextToSpeech.isStarted())
        {
            if (mTextToSpeech.isInitialized())
            {
                onTextToSpeechInitialized();
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
                mTextToSpeech.start(getContext());
            }
        }
    }

    public void detach(TextToSpeechManagerCallbacks listener)
    {
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
        if (!isEnabled())
        {
            return;
        }

        mTextToSpeech.speak(text);
    }

    public void speak(String text, boolean clear)
    {
        if (!isEnabled())
        {
            return;
        }

        mTextToSpeech.speak(text, clear, null);
    }

    public void speak(String text, Runnable runAfter)
    {
        if (!isEnabled())
        {
            return;
        }

        mTextToSpeech.speak(text, runAfter);
    }

    public void speak(@NonNull FooTextToSpeechBuilder builder)
    {
        if (!isEnabled())
        {
            return;
        }

        mTextToSpeech.speak(builder);
    }

    public void speak(@NonNull FooTextToSpeechBuilder builder, boolean clear)
    {
        if (!isEnabled())
        {
            return;
        }

        mTextToSpeech.speak(builder, clear, null);
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
        if (!force && !isEnabled())
        {
            return;
        }

        if (toast)
        {
            FooPlatformUtils.toastLong(getContext(), text);
        }

        mTextToSpeech.speak(text, runAfter);
    }

    public void silence(int durationInMs)
    {
        silence(false, durationInMs);
    }

    public void silence(boolean force, int durationInMs)
    {
        if (!force && !isEnabled())
        {
            return;
        }

        mTextToSpeech.silence(durationInMs);
    }
}

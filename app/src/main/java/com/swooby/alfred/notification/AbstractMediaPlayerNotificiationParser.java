package com.swooby.alfred.notification;

import android.content.Context;
import android.media.AudioManager;
import android.support.annotation.NonNull;

import com.smartfoo.android.core.texttospeech.FooTextToSpeech;

public abstract class AbstractMediaPlayerNotificiationParser
        extends AbstractNotificationParser
{
    /**
     * If we set volume to zero then many media players automatically pause
     */
    public static final int MUTE_VOLUME = 1;

    protected final AudioManager mAudioManager;

    protected int mLastVolume = -1;

    protected AbstractMediaPlayerNotificiationParser(
            @NonNull
            Context applicationContext,
            @NonNull
            FooTextToSpeech textToSpeech,
            @NonNull
            String packageName,
            @NonNull
            String packageAppSpokenName)
    {
        super(applicationContext, textToSpeech, packageName, packageAppSpokenName);

        mAudioManager = (AudioManager) applicationContext.getSystemService(Context.AUDIO_SERVICE);
    }

    // TODO:(pv) User option to always force un-muting, even if mLastVolume == -1, when the next track resumes?
    protected void mute(boolean mute, String speech)
    {
        if (mute)
        {
            if (mLastVolume != -1)
            {
                return;
            }

            final int audioStreamType = mTextToSpeech.getAudioStreamType();
            mLastVolume = mAudioManager.getStreamVolume(audioStreamType);

            if (speech == null)
            {
                speech = "attenuating";
            }

            mTextToSpeech.speak(mPackageAppSpokenName + ' ' + speech, new Runnable()
            {
                @Override
                public void run()
                {
                    mAudioManager.setStreamVolume(audioStreamType, MUTE_VOLUME, 0);
                }
            });
        }
        else
        {
            if (mLastVolume == -1)
            {
                return;
            }

            int audioStreamType = mTextToSpeech.getAudioStreamType();
            int audioStreamVolume = mAudioManager.getStreamVolume(audioStreamType);
            if (audioStreamVolume == MUTE_VOLUME)
            {
                mAudioManager.setStreamVolume(audioStreamType, mLastVolume, 0);

                /*
                if (speech == null)
                {
                    speech = "restored";
                }

                mTextToSpeech.speak(mPackageAppSpokenName + ' ' + speech);
                */
            }

            mLastVolume = -1;
        }
    }
}

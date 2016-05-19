package com.swooby.alfred.notification.parsers;

import android.content.Context;
import android.media.AudioManager;
import android.support.annotation.NonNull;

import com.swooby.alfred.MainApplication;

public abstract class AbstractMediaPlayerNotificiationParser
        extends AbstractNotificationParser
{
    /**
     * If we set volume to zero then many media players automatically pause
     */
    public static final int MUTE_VOLUME = 1;

    protected final AudioManager mAudioManager;

    protected int mLastStreamMusicVolume = -1;

    protected AbstractMediaPlayerNotificiationParser(
            @NonNull
            MainApplication application,
            @NonNull
            String packageName)
    {
        super(application, packageName);

        mAudioManager = (AudioManager) application.getSystemService(Context.AUDIO_SERVICE);
    }

    /*
    protected AbstractMediaPlayerNotificiationParser(
            @NonNull
            MainApplication application,
            @NonNull
            String packageName,
            @NonNull
            String packageAppSpokenName)
    {
        super(application, packageName, packageAppSpokenName);

        mAudioManager = (AudioManager) application.getSystemService(Context.AUDIO_SERVICE);
    }
    */

    // TODO:(pv) User option to always force un-muting, even if mLastVolume == -1, when the next track resumes?
    protected void mute(boolean mute, String speech)
    {
        final int musicAudioStreamType = AudioManager.STREAM_MUSIC;

        if (mute)
        {
            if (mLastStreamMusicVolume != -1)
            {
                return;
            }

            int voiceAudioStreamType = mApplication.getVoiceAudioStreamType();

            mLastStreamMusicVolume = mAudioManager.getStreamVolume(musicAudioStreamType);

            Runnable runAfter = new Runnable()
            {
                @Override
                public void run()
                {
                    mAudioManager.setStreamVolume(musicAudioStreamType, MUTE_VOLUME, 0);
                }
            };

            if (speech == null)
            {
                speech = "attenuating";
            }

            mApplication.speak(mPackageAppSpokenName + ' ' + speech, runAfter);

            if (voiceAudioStreamType != musicAudioStreamType)
            {
                runAfter.run();
            }
        }
        else
        {
            if (mLastStreamMusicVolume == -1)
            {
                return;
            }

            int audioStreamVolume = mAudioManager.getStreamVolume(musicAudioStreamType);
            if (audioStreamVolume == MUTE_VOLUME)
            {
                mAudioManager.setStreamVolume(musicAudioStreamType, mLastStreamMusicVolume, 0);

                /*
                if (speech == null)
                {
                    speech = "restored";
                }

                mTextToSpeech.speak(mPackageAppSpokenName + ' ' + speech);
                */
            }

            mLastStreamMusicVolume = -1;
        }
    }
}

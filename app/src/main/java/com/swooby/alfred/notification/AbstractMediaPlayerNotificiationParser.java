package com.swooby.alfred.notification;

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

    protected int mLastVolume = -1;

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

    // TODO:(pv) User option to always force un-muting, even if mLastVolume == -1, when the next track resumes?
    protected void mute(boolean mute, String speech)
    {
        if (mute)
        {
            if (mLastVolume != -1)
            {
                return;
            }

            final int audioStreamType = mApplication.getAudioStreamType();
            mLastVolume = mAudioManager.getStreamVolume(audioStreamType);

            if (speech == null)
            {
                speech = "attenuating";
            }

            mApplication.speak(mPackageAppSpokenName + ' ' + speech, new Runnable()
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

            int audioStreamType = mApplication.getAudioStreamType();
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

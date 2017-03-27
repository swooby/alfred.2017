package com.swooby.alfred.notification.parsers;

import android.content.Context;
import android.media.AudioManager;
import android.media.session.PlaybackState;
import android.support.annotation.NonNull;

import com.swooby.alfred.TextToSpeechManager;

public abstract class AbstractMediaPlayerNotificiationParser
        extends AbstractNotificationParser
{
    public static String playbackStateToString(int playbackState)
    {
        String s;
        switch (playbackState)
        {
            case PlaybackState.STATE_BUFFERING:
                s = "STATE_BUFFERING";
                break;
            case PlaybackState.STATE_CONNECTING:
                s = "STATE_CONNECTING";
                break;
            case PlaybackState.STATE_ERROR:
                s = "STATE_ERROR";
                break;
            case PlaybackState.STATE_FAST_FORWARDING:
                s = "STATE_FAST_FORWARDING";
                break;
            case PlaybackState.STATE_NONE:
                s = "STATE_NONE";
                break;
            case PlaybackState.STATE_PAUSED:
                s = "STATE_PAUSED";
                break;
            case PlaybackState.STATE_PLAYING:
                s = "STATE_PLAYING";
                break;
            case PlaybackState.STATE_REWINDING:
                s = "STATE_REWINDING";
                break;
            case PlaybackState.STATE_SKIPPING_TO_NEXT:
                s = "STATE_SKIPPING_TO_NEXT";
                break;
            case PlaybackState.STATE_SKIPPING_TO_PREVIOUS:
                s = "STATE_SKIPPING_TO_PREVIOUS";
                break;
            case PlaybackState.STATE_SKIPPING_TO_QUEUE_ITEM:
                s = "STATE_SKIPPING_TO_QUEUE_ITEM";
                break;
            case PlaybackState.STATE_STOPPED:
                s = "STATE_STOPPED";
                break;
            default:
                s = "UNKNOWN";
                break;
        }
        return s + '(' + playbackState + ')';
    }

    /**
     * If we set volume to zero then many media players automatically pause
     */
    public static final int MUTE_VOLUME = 1;

    protected final AudioManager mAudioManager;

    protected int mLastStreamMusicVolume = -1;

    protected AbstractMediaPlayerNotificiationParser(@NonNull NotificationParserCallbacks callbacks)
    {
        super(callbacks);

        mAudioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
    }

    /*
    protected AbstractMediaPlayerNotificiationParser(
            @NonNull MainApplication application,
            @NonNull String packageName,
            @NonNull String packageAppSpokenName)
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

            TextToSpeechManager textToSpeechManager = getTextToSpeech();

            int textToSpeechAudioStreamType = textToSpeechManager.getAudioStreamType();

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
                speech = getPackageAppSpokenName() + " attenuating";
            }

            textToSpeechManager.speak(speech, runAfter);

            if (textToSpeechAudioStreamType != musicAudioStreamType)
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

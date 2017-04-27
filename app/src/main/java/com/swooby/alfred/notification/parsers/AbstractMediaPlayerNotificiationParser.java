package com.swooby.alfred.notification.parsers;

import android.app.Notification;
import android.content.Context;
import android.media.AudioManager;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.smartfoo.android.core.FooRun;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.media.FooAudioFocusListener;
import com.smartfoo.android.core.media.FooAudioFocusListener.FooAudioFocusListenerCallbacks;
import com.swooby.alfred.R;

public abstract class AbstractMediaPlayerNotificiationParser
        extends AbstractNotificationParser
{
    private static final String TAG = FooLog.TAG(AbstractMediaPlayerNotificiationParser.class);

    public MediaController getMediaController(@NonNull Context context, Bundle extras)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(context, "context");
        MediaController mediaController = null;
        if (extras != null)
        {
            MediaSession.Token mediaSession = extras.getParcelable(Notification.EXTRA_MEDIA_SESSION);
            FooLog.v(TAG, "getMediaController: mediaSession=" + mediaSession);

            if (mediaSession != null)
            {
                try
                {
                    mediaController = new MediaController(context, mediaSession);
                }
                catch (Exception e)
                {
                    FooLog.e(TAG, "getMediaController: EXCEPTION", e);
                }
            }
        }
        return mediaController;
    }

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

    private final FooAudioFocusListener mAudioFocusListener;

    protected AbstractMediaPlayerNotificiationParser(@NonNull String hashtag, @NonNull NotificationParserCallbacks callbacks)
    {
        super(hashtag, callbacks);

        mAudioFocusListener = FooAudioFocusListener.getInstance();
    }

    private final FooAudioFocusListenerCallbacks mAudioFocusListenerCallbacks = new FooAudioFocusListenerCallbacks()
    {
        @Override
        public boolean onAudioFocusLost(FooAudioFocusListener audioFocusListener, int audioFocusStreamType, int audioFocusDurationHint, int focusChange)
        {
            audioFocusStart();
            return true;
        }
    };

    private boolean mIsCommercialSpoken;

    NotificationParseResult onCommercial(String prefix)
    {
        audioFocusStart();

        if (!mIsCommercialSpoken)
        {
            mIsCommercialSpoken = true;

            String speech = getString(R.string.alfred_attenuating_X_commercial, getPackageAppSpokenName());

            speak(speech);
        }

        FooLog.w(TAG, prefix + " onCommercial(...); return ParsedIgnored");
        return NotificationParseResult.ParsedIgnored;
    }

    void onNonCommercial()
    {
        mIsCommercialSpoken = false;
        mAudioFocusListener.audioFocusStop(mAudioFocusListenerCallbacks);
    }

    private boolean audioFocusStart()
    {
        return mAudioFocusListener.audioFocusStart(getContext(),
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK,
                mAudioFocusListenerCallbacks);
    }
}

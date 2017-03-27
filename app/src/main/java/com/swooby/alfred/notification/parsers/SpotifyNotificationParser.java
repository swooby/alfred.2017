package com.swooby.alfred.notification.parsers;

import android.app.Notification;
import android.content.Context;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.support.annotation.NonNull;

import com.smartfoo.android.core.BuildConfig;
import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.platform.FooPlatformUtils;
import com.smartfoo.android.core.texttospeech.FooTextToSpeechBuilder;

import java.util.Arrays;

public class SpotifyNotificationParser
        extends AbstractMediaPlayerNotificiationParser
{
    private static final String TAG = FooLog.TAG(SpotifyNotificationParser.class);

    private boolean      mLastIsPlaying;
    private CharSequence mLastArtist;
    private CharSequence mLastTitle;

    public SpotifyNotificationParser(@NonNull NotificationParserCallbacks callbacks)
    {
        super(callbacks);//, application.getString(R.string.spotify_package_app_spoken_name));
    }

    @Override
    public String getPackageName()
    {
        return "com.spotify.music";
    }

    @Override
    public NotificationParseResult onNotificationPosted(StatusBarNotification sbn)
    {
        FooLog.i(TAG, "---- #SPOTIFY ----");
        if (BuildConfig.DEBUG)
        {
            super.onNotificationPosted(sbn);
        }

        Bundle extras = getExtras(sbn);
        FooLog.v(TAG, "onNotificationPosted: extras=" + FooPlatformUtils.toString(extras));
        if (extras == null)
        {
            FooLog.w(TAG, "onNotificationPosted: extras == null; Unparsable");
            return NotificationParseResult.Unparsable;
        }

        CharSequence androidTitle = extras.getCharSequence(Notification.EXTRA_TITLE);
        FooLog.v(TAG, "onNotificationPosted: androidTitle=" + FooString.quote(androidTitle));
        CharSequence textArtist = extras.getCharSequence(Notification.EXTRA_TEXT);
        FooLog.v(TAG, "onNotificationPosted: textArtist=" + FooString.quote(textArtist));
        CharSequence textTitle = extras.getCharSequence(Notification.EXTRA_TITLE);
        FooLog.v(TAG, "onNotificationPosted: textTitle=" + FooString.quote(textTitle));
        MediaSession.Token mediaSession = extras.getParcelable(Notification.EXTRA_MEDIA_SESSION);
        FooLog.v(TAG, "onNotificationPosted: mediaSession=" + mediaSession);
        int[] compactActions = extras.getIntArray(Notification.EXTRA_COMPACT_ACTIONS);
        FooLog.v(TAG, "onNotificationPosted: compactActions=" + Arrays.toString(compactActions));

        if (mediaSession == null)
        {
            FooLog.w(TAG, "onNotificationPosted: mediaSession == null; Unparsable");
            return NotificationParseResult.Unparsable;
        }

        Context context = getContext();

        MediaController mediaController;
        try
        {
            mediaController = new MediaController(context, mediaSession);
        }
        catch (Exception e)
        {
            FooLog.e(TAG, "onNotificationPosted: EXCEPTION; Unparsable", e);
            return NotificationParseResult.Unparsable;
        }

        PlaybackState playbackState = mediaController.getPlaybackState();
        if (playbackState == null)
        {
            FooLog.w(TAG, "onNotificationPosted: mediaSession == null; Unparsable");
            return NotificationParseResult.Unparsable;
        }

        int playbackStateState = playbackState.getState();
        FooLog.v(TAG, "onNotificationPosted: playbackStateState == " + playbackStateToString(playbackStateState));
        if (playbackStateState != PlaybackState.STATE_PAUSED &&
            playbackStateState != PlaybackState.STATE_PLAYING)
        {
            FooLog.w(TAG, "onNotificationPosted: playbackStateState != (PAUSED || PLAYING); Ignored");
            return NotificationParseResult.ParsableIgnored;
        }
        boolean isPlaying = playbackStateState == PlaybackState.STATE_PLAYING;

        if (FooString.isNullOrEmpty(textArtist)) // && only pause control is enabled
        {
            //
            // It's a commercial!
            //
            // TODO:(pv) Make this a user option...
            if (true)
            {
                mute(true, "attenuating " + getPackageAppSpokenName() + " commercial");
            }

            return NotificationParseResult.ParsableIgnored;
        }

        textArtist = unknownIfNullOrEmpty(context, textArtist);
        textTitle = unknownIfNullOrEmpty(context, textTitle);

        if (isPlaying == mLastIsPlaying &&
            textArtist.equals(mLastArtist) &&
            textTitle.equals(mLastTitle))
        {
            return NotificationParseResult.ParsableIgnored;
        }

        mute(false, null);//"un-muting commercial");

        mLastIsPlaying = isPlaying;
        mLastArtist = textArtist;
        mLastTitle = textTitle;

        FooTextToSpeechBuilder builder = new FooTextToSpeechBuilder(getPackageAppSpokenName());

        if (isPlaying)
        {
            builder.appendSpeech("playing");
            builder.appendSilenceWordBreak();
            builder.appendSpeech("artist " + textArtist);
            builder.appendSilenceWordBreak();
            builder.appendSpeech("title " + textTitle);
        }
        else
        {
            builder.appendSpeech("paused");
        }

        getTextToSpeech().speak(builder);

        return NotificationParseResult.ParsableHandled;
    }
}

package com.swooby.alfred.notification.parsers;

import android.app.Notification.Action;
import android.content.Context;
import android.media.session.MediaController;
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

    private boolean      mLastIsCommercial;
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

        CharSequence textTitle = getAndroidTitle(extras);
        FooLog.v(TAG, "onNotificationPosted: textTitle=" + FooString.quote(textTitle));
        CharSequence textArtist = getAndroidText(extras);
        FooLog.v(TAG, "onNotificationPosted: textArtist=" + FooString.quote(textArtist));

        Context context = getContext();

        MediaController mediaController = getMediaController(context, extras);
        FooLog.v(TAG, "onNotificationPosted: mediaController=" + mediaController);
        if (mediaController == null)
        {
            FooLog.w(TAG, "onNotificationPosted: mediaController == null; Unparsable");
            return NotificationParseResult.Unparsable;
        }

        Action[] actions = getActions(sbn);
        FooLog.v(TAG, "onNotificationPosted: actions=" + Arrays.toString(actions));

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

        // @formatter:off
        // title == non-null commercial/advertisement/company name, artist == null/""
        boolean isCommercial = !FooString.isNullOrEmpty(textTitle) && FooString.isNullOrEmpty(textArtist);
        // Test if *ONLY* the Playing/Paused (middle) Action is enabled (actionIntent != null)
        isCommercial &= actions.length < 1 || actions[0].actionIntent == null; // Thumbs Down (action always present)
        isCommercial &= actions.length < 2 || actions[1].actionIntent == null; // Previous Track (action always present)
        isCommercial &= actions.length < 3 || actions[2].actionIntent != null; // PlayingPause or PausedPlay (action always present)
        isCommercial &= actions.length < 4 || actions[3].actionIntent == null; // Next Track (action always present)
        isCommercial &= actions.length < 5 || actions[4].actionIntent == null; // Thumbs Up (action may be absent/null)
        // @formatter:on
        if (isCommercial)
        {
            if (!mLastIsCommercial)
            {
                mLastIsCommercial = true;

                // TODO:(pv) Make this a user option...
                if (true)
                {
                    //mediaController.setVolumeTo(...);
                    mute(true, "attenuating " + getPackageAppSpokenName() + " commercial");
                }
            }

            return NotificationParseResult.ParsableIgnored;
        }

        mLastIsCommercial = false;

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

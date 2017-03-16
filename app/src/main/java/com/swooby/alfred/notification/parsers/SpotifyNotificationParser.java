package com.swooby.alfred.notification.parsers;

import android.content.Context;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.platform.FooPlatformUtils;
import com.smartfoo.android.core.texttospeech.FooTextToSpeechBuilder;
import com.smartfoo.android.core.view.FooViewUtils;

public class SpotifyNotificationParser
        extends AbstractMediaPlayerNotificiationParser
{
    private static final String TAG = FooLog.TAG(SpotifyNotificationParser.class);

    private boolean mLastIsPlaying;
    private String  mLastArtist;
    private String  mLastTitle;
    private String  mLastAlbum;

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
        super.onNotificationPosted(sbn);

        Bundle extras = getExtras(sbn);
        FooLog.v(TAG, "onNotificationPosted: extras=" + FooPlatformUtils.toString(extras));
        /*
        if (extras == null)
        {
            FooLog.w(TAG, "onNotificationPosted: extras == null; Unparsable");
            return NotificationParseResult.Unparsable;
        }

        String androidTitle = extras.getString("android.title");
        FooLog.v(TAG, "onNotificationPosted: androidTitle=" + FooString.quote(androidTitle));
        */

        RemoteViews bigContentView = getBigContentView(sbn);
        if (bigContentView == null)
        {
            FooLog.w(TAG, "onNotificationPosted: bigContentView == null; Unparsable");
            return NotificationParseResult.Unparsable;
        }

        Context context = getContext();

        View remoteView = inflateRemoteView(context, bigContentView);
        if (remoteView == null)
        {
            FooLog.w(TAG, "onNotificationPosted: remoteView == null; Unparsable");
            return NotificationParseResult.Unparsable;
        }

        int idFirstLine = getIdOfChildWithName(remoteView, "firstLine");
        if (idFirstLine == 0)
        {
            FooLog.w(TAG, "onNotificationPosted: idFirstLine == 0; Unparsable");
            return NotificationParseResult.Unparsable;
        }

        int idSecondLine = getIdOfChildWithName(remoteView, "secondLine");
        if (idSecondLine == 0)
        {
            FooLog.w(TAG, "onNotificationPosted: idSecondLine == 0; Unparsable");
            return NotificationParseResult.Unparsable;
        }

        int idThirdLine = getIdOfChildWithName(remoteView, "thirdLine");
        if (idThirdLine == 0)
        {
            FooLog.w(TAG, "onNotificationPosted: idThirdLine == 0; Unparsable");
            return NotificationParseResult.Unparsable;
        }

        int idPause = getIdOfChildWithName(remoteView, "pause");
        if (idPause == 0)
        {
            FooLog.w(TAG, "onNotificationPosted: idPause == 0; Unparsable");
            return NotificationParseResult.Unparsable;
        }

        ImageView imageViewPause = (ImageView) remoteView.findViewById(idPause);
        if (imageViewPause == null)
        {
            FooLog.w(TAG, "onNotificationPosted: imageViewPause == null; Unparsable");
            return NotificationParseResult.Unparsable;
        }
        int pauseVisibility = imageViewPause.getVisibility();
        FooLog.v(TAG, "onNotificationPosted: pauseVisibility=" + FooViewUtils.viewVisibilityToString(pauseVisibility));

        boolean isPlaying = pauseVisibility == View.VISIBLE;
        FooLog.v(TAG, "onNotificationPosted: isPlaying=" + isPlaying);

        TextView textViewFirstLine = (TextView) remoteView.findViewById(idFirstLine);
        if (textViewFirstLine == null)
        {
            FooLog.w(TAG, "onNotificationPosted: textViewFirstLine == null; Unparsable");
            return NotificationParseResult.Unparsable;
        }
        String textTitle = textViewFirstLine.getText().toString();
        FooLog.v(TAG, "onNotificationPosted: textTitle=" + FooString.quote(textTitle));

        TextView textViewSecondLine = (TextView) remoteView.findViewById(idSecondLine);
        if (textViewSecondLine == null)
        {
            FooLog.w(TAG, "onNotificationPosted: textViewSecondLine == null; Unparsable");
            return NotificationParseResult.Unparsable;
        }
        String textAlbum = textViewSecondLine.getText().toString();
        FooLog.v(TAG, "onNotificationPosted: textAlbum=" + FooString.quote(textAlbum));

        TextView textViewThirdLine = (TextView) remoteView.findViewById(idThirdLine);
        if (textViewThirdLine == null)
        {
            FooLog.w(TAG, "onNotificationPosted: textViewThirdLine == null; Unparsable");
            return NotificationParseResult.Unparsable;
        }
        String textArtist = textViewThirdLine.getText().toString();
        FooLog.v(TAG, "onNotificationPosted: textArtist=" + FooString.quote(textArtist));

        if (FooString.isNullOrEmpty(textAlbum) && FooString.isNullOrEmpty(textArtist))
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
        textAlbum = unknownIfNullOrEmpty(context, textAlbum);

        if (isPlaying == mLastIsPlaying &&
            textArtist.equals(mLastArtist) &&
            textTitle.equals(mLastTitle) &&
            textAlbum.equals(mLastAlbum))
        {
            return NotificationParseResult.ParsableIgnored;
        }

        mute(false, null);//"un-muting commercial");

        mLastIsPlaying = isPlaying;
        mLastArtist = textArtist;
        mLastTitle = textTitle;
        mLastAlbum = textAlbum;

        FooTextToSpeechBuilder builder = new FooTextToSpeechBuilder();

        if (isPlaying)
        {
            builder.appendSpeech(getPackageAppSpokenName() + " playing");
            builder.appendSilence(500);
            builder.appendSpeech("artist " + textArtist);
            builder.appendSilence(500);
            builder.appendSpeech("title " + textTitle);
            builder.appendSilence(500);
            builder.appendSpeech("album " + textAlbum);
        }
        else
        {
            builder.appendSpeech(getPackageAppSpokenName() + " paused");
        }

        getTextToSpeech().speak(builder);

        return NotificationParseResult.ParsableHandled;
    }
}

package com.swooby.alfred.notification.parsers;

import android.content.Context;
import android.service.notification.StatusBarNotification;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.RemoteViews;

import com.smartfoo.android.core.logging.FooLog;
import com.swooby.alfred.MainApplication;

import java.util.LinkedHashSet;
import java.util.Set;

public class SpotifyNotificationParser
        extends AbstractMediaPlayerNotificiationParser
{
    private static final String TAG = FooLog.TAG(SpotifyNotificationParser.class);

    private boolean mLastIsPlaying;
    private String  mLastArtist;
    private String  mLastTitle;
    private String  mLastAlbum;

    public SpotifyNotificationParser(MainApplication application)
    {
        super(application, "com.spotify.music");//, application.getString(R.string.spotify_package_app_spoken_name));
    }

    @Override
    public NotificationParseResult onNotificationPosted(StatusBarNotification sbn)
    {
        //super.onNotificationPosted(sbn);

        RemoteViews contentView = getContentView(sbn);
        if (contentView == null)
        {
            FooLog.w(TAG, "onNotificationPosted: contentView == null; Unparsable");
            return NotificationParseResult.Unparsable;
        }

        Set<Integer> contentViewIds = new LinkedHashSet<>();

        Context remoteContext = createPackageContext(mApplication, contentView);
        RelativeLayout viewGroup = new RelativeLayout(remoteContext);
        View remoteView = contentView.apply(remoteContext, viewGroup);
        if (remoteView == null)
        {
            FooLog.w(TAG, "onNotificationPosted: remoteView == null; Unparsable");
            return NotificationParseResult.Unparsable;
        }

        if (true)
        {
            FooLog.e(TAG, "After bigContentView.apply");
            walkView(remoteView, contentViewIds);
        }

        /*
        View mockRemoteView = mockRemoteView(mApplication, contentView);
        if (mockRemoteView == null)
        {
            return NotificationParseResult.Unparsable;
        }
        */

        int idTitle = getIdOfChildWithName(remoteView, "title");
        if (idTitle == 0)
        {
            FooLog.w(TAG, "onNotificationPosted: idTitle == 0; Unparsable");
            return NotificationParseResult.Unparsable;
        }

        int idSubtitle = getIdOfChildWithName(remoteView, "subtitle");
        if (idSubtitle == 0)
        {
            FooLog.w(TAG, "onNotificationPosted: idSubtitle == 0; Unparsable");
            return NotificationParseResult.Unparsable;
        }

        int idPause = getIdOfChildWithName(remoteView, "pause");
        if (idPause == 0)
        {
            FooLog.w(TAG, "onNotificationPosted: idPause == 0; Unparsable");
            return NotificationParseResult.Unparsable;
        }

        return NotificationParseResult.Unparsable;
        /*
        Integer pauseVisibility = (Integer) getRemoteViewValueById(contentView, idPause, ValueTypes.VISIBILITY);
        FooLog.v(TAG, "onNotificationPosted: pauseVisibility=" + pauseVisibility);
        if (pauseVisibility == null)
        {
            return NotificationParseResult.Unparsable;
        }

        boolean isPlaying = pauseVisibility == View.VISIBLE;
        FooLog.v(TAG, "onNotificationPosted: isPlaying=" + isPlaying);

        String textTitle = (String) getRemoteViewValueById(contentView, idTitle, ValueTypes.TEXT);
        textTitle = unknownIfNullOrEmpty(textTitle);
        FooLog.v(TAG, "onNotificationPosted: textTitle=" + FooString.quote(textTitle));

        String textSubtitle = (String) getRemoteViewValueById(contentView, idSubtitle, ValueTypes.TEXT);
        FooLog.v(TAG, "onNotificationPosted: textSubtitle=" + FooString.quote(textSubtitle));
        String[] textArtistAndAlbum = (textSubtitle != null) ? textSubtitle.split("—") : null;
        String textArtist = null;
        String textAlbum = null;
        if (textArtistAndAlbum != null)
        {
            int offset = 0;
            if (offset < textArtistAndAlbum.length)
            {
                textArtist = textArtistAndAlbum[offset++];
            }
            if (offset < textArtistAndAlbum.length)
            {
                //noinspection UnusedAssignment
                textAlbum = textArtistAndAlbum[offset++];
            }
        }

        FooLog.v(TAG, "onNotificationPosted: textArtist=" + FooString.quote(textArtist));
        FooLog.v(TAG, "onNotificationPosted: textAlbum=" + FooString.quote(textAlbum));

        if (textArtist == null && textAlbum == null)
        {
            //
            // It's a commercial!
            //
            // TODO:(pv) Make this a user option...
            if (true)
            {
                mute(true, "attenuating commercial");
            }

            return NotificationParseResult.ParsableIgnored;
        }

        textArtist = unknownIfNullOrEmpty(textArtist);
        textAlbum = unknownIfNullOrEmpty(textAlbum);

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
            builder.appendSpeech(mPackageAppSpokenName + " playing");
            builder.appendSilence(500);
            builder.appendSpeech("artist " + textArtist);
            builder.appendSilence(500);
            builder.appendSpeech("title " + textTitle);
            builder.appendSilence(500);
            builder.appendSpeech("album " + textAlbum);
        }
        else
        {
            builder.appendSpeech(mPackageAppSpokenName + " paused");
        }

        mApplication.speak(builder);

        return NotificationParseResult.ParsableHandled;
        */
    }
}

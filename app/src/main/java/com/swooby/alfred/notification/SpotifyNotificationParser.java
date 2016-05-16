package com.swooby.alfred.notification;

import android.content.Context;
import android.service.notification.StatusBarNotification;
import android.view.View;
import android.widget.RemoteViews;

import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.R;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.texttospeech.FooTextToSpeech;

public class SpotifyNotificationParser
        extends AbstractMediaPlayerNotificiationParser
{
    private static final String TAG = FooLog.TAG(SpotifyNotificationParser.class);

    private boolean mLastIsPlaying;
    private String  mLastArtist;
    private String  mLastTitle;
    private String  mLastAlbum;

    public SpotifyNotificationParser(Context applicationContext, FooTextToSpeech textToSpeech)
    {
        super(applicationContext, textToSpeech, "com.spotify.music", applicationContext.getString(R.string.spotify_package_app_spoken_name));
    }

    @Override
    public boolean onNotificationPosted(StatusBarNotification sbn)
    {
        //super.onNotificationPosted(sbn);

        RemoteViews contentView = getContentView(sbn);

        View mockRemoteView = mockRemoteView(mApplicationContext, contentView);
        if (mockRemoteView == null)
        {
            return false;
        }

        int idTitle = getIdOfChildWithName(mockRemoteView, "title");
        if (idTitle == 0)
        {
            return false;
        }

        int idSubtitle = getIdOfChildWithName(mockRemoteView, "subtitle");
        if (idSubtitle == 0)
        {
            return false;
        }

        int idPause = getIdOfChildWithName(mockRemoteView, "pause");
        if (idPause == 0)
        {
            return false;
        }

        Integer pauseVisibility = (Integer) getRemoteViewValueById(contentView, idPause, ValueTypes.VISIBILITY);
        FooLog.v(TAG, "onNotificationPosted: pauseVisibility=" + pauseVisibility);
        if (pauseVisibility == null)
        {
            return false;
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

            return false;
        }

        textArtist = unknownIfNullOrEmpty(textArtist);
        textAlbum = unknownIfNullOrEmpty(textAlbum);

        if (isPlaying != mLastIsPlaying ||
            !textArtist.equals(mLastArtist) ||
            !textTitle.equals(mLastTitle) ||
            !textAlbum.equals(mLastAlbum))
        {
            mute(false, null);//"un-muting commercial");

            mLastIsPlaying = isPlaying;
            mLastArtist = textArtist;
            mLastTitle = textTitle;
            mLastAlbum = textAlbum;

            if (isPlaying)
            {
                mTextToSpeech.speak(mPackageAppSpokenName + " playing");
                mTextToSpeech.silence(500);
                mTextToSpeech.speak("artist " + textArtist);
                mTextToSpeech.silence(500);
                mTextToSpeech.speak("title " + textTitle);
                mTextToSpeech.silence(500);
                mTextToSpeech.speak("album " + textAlbum);
            }
            else
            {
                mTextToSpeech.speak(mPackageAppSpokenName + " paused");
            }

            return true;
        }

        return false;
    }
}

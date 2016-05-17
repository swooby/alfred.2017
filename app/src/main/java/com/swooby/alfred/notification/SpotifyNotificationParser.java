package com.swooby.alfred.notification;

import android.service.notification.StatusBarNotification;
import android.view.View;
import android.widget.RemoteViews;

import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.logging.FooLog;
import com.swooby.alfred.MainApplication;
import com.swooby.alfred.R;

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
        super(application, "com.spotify.music", application.getString(R.string.spotify_package_app_spoken_name));
    }

    @Override
    public boolean onNotificationPosted(StatusBarNotification sbn)
    {
        //super.onNotificationPosted(sbn);

        RemoteViews contentView = getContentView(sbn);

        View mockRemoteView = mockRemoteView(mApplication, contentView);
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
                mApplication.speak(mPackageAppSpokenName + " playing");
                mApplication.silence(500);
                mApplication.speak("artist " + textArtist);
                mApplication.silence(500);
                mApplication.speak("title " + textTitle);
                mApplication.silence(500);
                mApplication.speak("album " + textAlbum);
            }
            else
            {
                mApplication.speak(mPackageAppSpokenName + " paused");
            }

            return true;
        }

        return false;
    }
}

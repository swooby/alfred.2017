package com.swooby.alfred.notification;

import android.content.Context;
import android.content.res.Resources;
import android.service.notification.StatusBarNotification;
import android.view.View;
import android.widget.RemoteViews;

import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.logging.FooLog;
import com.swooby.alfred.MainApplication;
import com.swooby.alfred.R;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PandoraNotificationParser
        extends AbstractMediaPlayerNotificiationParser
{
    private static final String TAG = FooLog.TAG(PandoraNotificationParser.class);

    private final String mAdvertisementTitle;
    private final String mAdvertisementArtist;

    // TODO:(pv) Make this nullable Boolean to better handle proper state at startup
    protected Boolean mLastIsPlaying;
    protected String  mLastArtist;
    protected String  mLastTitle;
    protected String  mLastStation;

    public PandoraNotificationParser(MainApplication application)
    {
        super(application, "com.pandora.android", application.getString(R.string.pandora_package_app_name));

        mAdvertisementTitle = application.getString(R.string.pandora_advertisement_title);
        mAdvertisementArtist = application.getString(R.string.pandora_advertisement_artist);
    }

    @Override
    public boolean onNotificationPosted(StatusBarNotification sbn)
    {
        //super.onNotificationPosted(sbn);

        //
        // Source: com.pandora.android/res/layout-v21/persistant_notification_expanded.xml
        //
        RemoteViews bigContentView = getBigContentView(sbn);

        //
        // NOTE: We intentionally recompute this every time;
        // The app can update in the background which can cause the resource ids to change.
        //
        View mockBigContentView = mockRemoteView(mApplication, bigContentView);
        if (mockBigContentView == null)
        {
            FooLog.w(TAG, "onNotificationPosted: mockBigContentView == null; ignoring");
            return false;
        }

        /*
        0	2131820659 (0x7F110073)	title		setText             ...
        1	2131820848 (0x7F110130)	artist		setText             ...
        2	2131821312 (0x7F110300)	station		setText             ...
        3	2131820718 (0x7F1100AE)	icon		setImageBitmap      0
        4	2131821182 (0x7F11027E)	skip		setImageResource    ...
        5	2131821182 (0x7F11027E)	skip		setEnabled          true
        6   2131821182 (0x7F11027E) skip        PendingIntent
        7   2131821181 (0x7F11027D) play        setImageResource    ...
        8   2131821181 (0x7F11027D) play        PendingIntent
        9   2131821181 (0x7F11027D) play        setEnabled          true
        10  2131820655 (0x7F11006F) close       PendingIntent
        11  2131821315 (0x7F110303) ?           PendingIntent
        12  2131821314 (0x7F110302) ?           PendingIntent
        13  2131821314 (0x7F110302) ?           setImageResource    ...
        14  2131821315 (0x7F110303) ?           setImageResource    ...
        15  2131821314 (0x7F110302) ?           setEnabled          true
        16  2131821315 (0x7F110303) ?           setEnabled          true
        17  2131821308 (0x7F1102FC) ?           setVisibility       8 (GONE)
         */

        Set<Integer> mockBigContentViewIds = new LinkedHashSet<>();
        if (true)
        {
            // NOTE:(pv) This is a local inflate, not the actual live view,
            // and is only useful to get resource ids and default values from
            walkView(mockBigContentView, mockBigContentViewIds);
        }

        Map<Integer, String> mapIdsToNames = new LinkedHashMap<>();

        int idIcon = getIdOfChildWithName(mockBigContentView, "icon");
        FooLog.v(TAG,
                "onNotificationPosted: idIcon=" + toVerboseString(idIcon));
        if (idIcon == 0)
        {
            FooLog.w(TAG, "onNotificationPosted: idIcon == 0; ignoring");
            return false;
        }
        mapIdsToNames.put(idIcon, "icon");

        int idTitle = getIdOfChildWithName(mockBigContentView, "title");
        //FooLog.v(TAG, "onNotificationPosted: idTitle=" + toVerboseString(idTitle));
        if (idTitle == 0)
        {
            FooLog.w(TAG, "onNotificationPosted: idTitle == 0; ignoring");
            return false;
        }
        mapIdsToNames.put(idTitle, "title");

        int idArtist = getIdOfChildWithName(mockBigContentView, "artist");
        //FooLog.v(TAG, "onNotificationPosted: idArtist=" + toVerboseString(idArtist));
        if (idArtist == 0)
        {
            FooLog.w(TAG, "onNotificationPosted: idArtist == 0; ignoring");
            return false;
        }
        mapIdsToNames.put(idArtist, "artist");

        int idStation = getIdOfChildWithName(mockBigContentView, "station");
        //FooLog.v(TAG, "onNotificationPosted: idStation=" + toVerboseString(idStation));
        if (idStation == 0)
        {
            FooLog.w(TAG, "onNotificationPosted: idStation == 0; ignoring");
            return false;
        }
        mapIdsToNames.put(idStation, "station");

        /*
        int idThumbDown = getIdOfChildWithName(mockBigContentView, "thumb_down");
        FooLog.v(TAG, "onNotificationPosted: idThumbDown=" + toVerboseString(idThumbDown));
        if (idThumbDown == 0)
        {
            FooLog.w(TAG, "onNotificationPosted: idThumbDown == 0; ignoring");
            return false;
        }
        mapIdsToNames.put(idThumbDown, "thumb_down");

        int idThumbUp = getIdOfChildWithName(mockBigContentView, "thumb_up");
        FooLog.v(TAG, "onNotificationPosted: idThumbUp=" + toVerboseString(idThumbUp));
        if (idThumbUp == 0)
        {
            FooLog.w(TAG, "onNotificationPosted: idThumbUp == 0; ignoring");
            return false;
        }
        mapIdsToNames.put(idThumbUp, "thumb_up");
        */

        int idPlay = getIdOfChildWithName(mockBigContentView, "play");
        FooLog.v(TAG,
                "onNotificationPosted: idPlay=" + toVerboseString(idPlay));
        if (idPlay == 0)
        {
            FooLog.w(TAG, "onNotificationPosted: idPlay == 0; ignoring");
            return false;
        }
        mapIdsToNames.put(idPlay, "play");

        int idSkip = getIdOfChildWithName(mockBigContentView, "skip");
        FooLog.v(TAG,
                "onNotificationPosted: idSkip=" + toVerboseString(idSkip));
        if (idSkip == 0)
        {
            FooLog.w(TAG, "onNotificationPosted: idSkip == 0; ignoring");
            return false;
        }
        mapIdsToNames.put(idSkip, "skip");

        int idLoading = getIdOfChildWithName(mockBigContentView, "loading");
        FooLog.v(TAG,
                "onNotificationPosted: idLoading=" + toVerboseString(idLoading));
        if (idLoading == 0)
        {
            FooLog.w(TAG, "onNotificationPosted: idLoading == 0; ignoring");
            return false;
        }
        mapIdsToNames.put(idLoading, "loading");

        Context otherAppContext = createPackageContext(mApplication, bigContentView);
        if (otherAppContext == null)
        {
            FooLog.w(TAG, "onNotificationPosted: otherAppContext == null; ignoring");
            return false;
        }

        Resources resources = otherAppContext.getResources();
        String packageName = otherAppContext.getPackageName();

        int idDrawablePause = resources.getIdentifier("notification_pause_selector", "drawable", packageName);
        // Pandora v7.2: 2130838193 (0x7F0202B1)
        FooLog.e(TAG, "onNotificationPosted: idDrawablePause=" + toVerboseString(idDrawablePause));

        int idDrawablePlay = resources.getIdentifier("notification_play_selector", "drawable", packageName);
        // Pandora v7.2: 2130838194 (0x7F0202B2)
        FooLog.e(TAG, "onNotificationPosted: idDrawablePlay=" + toVerboseString(idDrawablePlay));

        Integer idPlayImageResourceId = (Integer) getRemoteViewValueById(bigContentView, idPlay, ValueTypes.IMAGE_RESOURCE_ID);
        FooLog.e(TAG, "onNotificationPosted: idPlayImageResourceId=" + toVerboseString(idPlayImageResourceId));
        if (idPlayImageResourceId == null)
        {
            FooLog.w(TAG, "onNotificationPosted: idPlayImageResourceId == null; ignoring");
            return false;
        }

        Boolean playEnabled = (Boolean) getRemoteViewValueById(bigContentView, idPlay, ValueTypes.ENABLED);
        FooLog.e(TAG, "onNotificationPosted: playEnabled=" + playEnabled);
        if (playEnabled == null)
        {
            FooLog.w(TAG, "onNotificationPosted: playEnabled == null; ignoring");
            return false;
        }

        Integer idIconBitmapResourceId = (Integer) getRemoteViewValueById(bigContentView, idIcon, ValueTypes.BITMAP_RESOURCE_ID);
        FooLog.e(TAG, "onNotificationPosted: idIconBitmapResourceId=" + toVerboseString(idIconBitmapResourceId));
        if (idIconBitmapResourceId == null)
        {
            //return false;
        }

        Integer iconVisibility = (Integer) getRemoteViewValueById(bigContentView, idIcon, ValueTypes.VISIBILITY);
        FooLog.e(TAG, "onNotificationPosted: iconVisibility=" + iconVisibility);
        if (iconVisibility == null)
        {
            //return false;
        }

        Integer loadingVisibility = (Integer) getRemoteViewValueById(bigContentView, idLoading, ValueTypes.VISIBILITY);
        FooLog.e(TAG, "onNotificationPosted: loadingVisibility=" + loadingVisibility);
        if (loadingVisibility == null)
        {
            //return false;
        }

        if (true)
        {
            List<KeyValue> bigContentViewKeyValues = new LinkedList<>();
            walkActions(bigContentView, bigContentViewKeyValues);
            Collections.sort(bigContentViewKeyValues);
            for (KeyValue keyValue : bigContentViewKeyValues)
            {
                int key = keyValue.mKey;

                String idName = mapIdsToNames.get(key);

                if (idName != null)
                {
                    FooLog.e(TAG, idName + " keyValue=" + keyValue);
                }
            }
        }

        boolean isLoading = //(idIconBitmapResourceId != null && idIconBitmapResourceId == 0) ||
                (loadingVisibility != null && loadingVisibility == View.VISIBLE);
        FooLog.e(TAG, "onNotificationPosted: isLoading=" + isLoading);
        if (isLoading)
        {
            //return false;
        }

        boolean isPlaying = isLoading || idPlayImageResourceId == idDrawablePause;
        FooLog.e(TAG, "onNotificationPosted: isPlaying=" + isPlaying);

        String textTitle = (String) getRemoteViewValueById(bigContentView, idTitle, ValueTypes.TEXT);
        textTitle = unknownIfNullOrEmpty(textTitle);
        FooLog.v(TAG, "onNotificationPosted: textTitle=" + FooString.quote(textTitle));

        String textArtist = (String) getRemoteViewValueById(bigContentView, idArtist, ValueTypes.TEXT);
        textArtist = unknownIfNullOrEmpty(textArtist);
        FooLog.v(TAG, "onNotificationPosted: textArtist=" + FooString.quote(textArtist));

        String textStation = (String) getRemoteViewValueById(bigContentView, idStation, ValueTypes.TEXT);
        textStation = unknownIfNullOrEmpty(textStation);
        FooLog.v(TAG, "onNotificationPosted: textStation=" + FooString.quote(textStation));

        boolean isCommercial = mAdvertisementTitle.equalsIgnoreCase(textTitle) &&
                               mAdvertisementArtist.equalsIgnoreCase(textArtist);
        if (isCommercial)
        {
            //
            // It's a commercial!
            //
            // TODO:(pv) Make this a user option...
            if (true)
            {
                mute(true, "attenuating commercial");
            }

            FooLog.w(TAG, "onNotificationPosted: isCommercial == true; ignoring");

            return false;
        }

        FooLog.v(TAG, "onNotificationPosted: mLastIsPlaying=" + mLastIsPlaying);

        if ((mLastIsPlaying == null || isPlaying != mLastIsPlaying) ||
            !textTitle.equals(mLastTitle) ||
            !textArtist.equals(mLastArtist) ||
            !textStation.equals(mLastStation))
        {
            mute(false, null);//, "un-muting commercial");

            mLastIsPlaying = isPlaying;
            mLastTitle = textTitle;
            mLastArtist = textArtist;
            mLastStation = textStation;

            if (isPlaying)
            {
                FooLog.w(TAG, "onNotificationPosted: playing");

                mApplication.speak(mPackageAppSpokenName + " playing");
                mApplication.silence(500);
                mApplication.speak("artist " + textArtist);
                mApplication.silence(500);
                mApplication.speak("title " + textTitle);
                //mTextToSpeech.silence(500);
                //mTextToSpeech.speak("station " + textStation);
            }
            else
            {
                FooLog.w(TAG, "onNotificationPosted: paused");

                mApplication.speak(mPackageAppSpokenName + " paused");
            }

            return true;
        }
        else
        {
            FooLog.w(TAG, "onNotificationPosted: data unchanged; ignoring");
        }

        return false;
    }
}

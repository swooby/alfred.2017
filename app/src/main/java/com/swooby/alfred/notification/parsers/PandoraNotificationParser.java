package com.swooby.alfred.notification.parsers;

import android.app.PendingIntent;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.platform.FooPlatformUtils;
import com.smartfoo.android.core.texttospeech.FooTextToSpeechBuilder;
import com.smartfoo.android.core.view.FooViewUtils;
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
    //protected boolean mLastIsLoading;
    //protected Boolean mLastIsPlaying;
    protected Boolean mLastIsPausedByUser;
    protected String  mLastArtist;
    protected String  mLastTitle;
    protected String  mLastStation;

    public PandoraNotificationParser(MainApplication application)
    {
        super(application, "com.pandora.android");//, application.getString(R.string.pandora_package_app_name));

        mAdvertisementTitle = application.getString(R.string.pandora_advertisement_title);
        mAdvertisementArtist = application.getString(R.string.pandora_advertisement_artist);
    }

    @Override
    public NotificationParseResult onNotificationPosted(StatusBarNotification sbn)
    {
        FooLog.i(TAG, "---- #PANDORA ----");
        super.onNotificationPosted(sbn);

        Bundle extras = getExtras(sbn);
        FooLog.v(TAG, "onNotificationPosted: extras=" + FooPlatformUtils.toString(extras));

        RemoteViews bigContentView = getBigContentView(sbn);
        if (bigContentView == null)
        {
            FooLog.w(TAG, "onNotificationPosted: bigContentView == null; Unparsable");
            return NotificationParseResult.Unparsable;
        }

        /*
        bigContentView Actions:
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

        View remoteView = inflateRemoteView(mApplication, bigContentView);
        if (remoteView == null)
        {
            FooLog.w(TAG, "onNotificationPosted: remoteView == null; Unparsable");
            return NotificationParseResult.Unparsable;
        }

        if (true)
        {
            Set<Integer> bigContentViewIds = new LinkedHashSet<>();
            walkView(remoteView, bigContentViewIds);
            //FooLog.e(TAG, "mockBigContentViewIds=" + mockBigContentViewIds);
        }

        //
        // NOTE: We intentionally recompute this every time;
        // The remote app version can update in the background which can/will cause the resource ids to change.
        // TODO:(pv) Key this off of the package version?
        //
        /*
        View mockBigContentView = mockRemoteView(mApplication, bigContentView);
        if (mockBigContentView == null)
        {
            FooLog.w(TAG, "onNotificationPosted: mockBigContentView == null; ignoring");
            return NotificationParseResult.Unparsable;
        }

        if (true)
        {
            // NOTE:(pv) This is a local inflate, not the actual live view,
            // and is only useful to get resource ids and default values from
            //
            FooLog.e(TAG, "Before reapply");
            //
            walkView(mockBigContentView, mockBigContentViewIds);
        }

        //mockBigContentView = bigContentView.apply(mApplicationContext, mockBigContentView);
        bigContentView.reapply(mApplication, mockBigContentView);

        mockBigContentViewIds.clear();
        if (true)
        {
            // NOTE:(pv) This is a local inflate, not the actual live view,
            // and is only useful to get resource ids and default values from
            //
            FooLog.e(TAG, "After reapply");
            //
            walkView(mockBigContentView, mockBigContentViewIds);
        }
        */

        Map<Integer, String> mapIdsToNames = new LinkedHashMap<>();

        int idIcon = getIdOfChildWithName(remoteView, "icon");
        FooLog.v(TAG,
                "onNotificationPosted: idIcon=" + toVerboseString(idIcon));
        if (idIcon == 0)
        {
            FooLog.w(TAG, "onNotificationPosted: idIcon == 0; Unparsable");
            return NotificationParseResult.Unparsable;
        }
        mapIdsToNames.put(idIcon, "icon");

        int idTitle = getIdOfChildWithName(remoteView, "title");
        //FooLog.v(TAG, "onNotificationPosted: idTitle=" + toVerboseString(idTitle));
        if (idTitle == 0)
        {
            FooLog.w(TAG, "onNotificationPosted: idTitle == 0; Unparsable");
            return NotificationParseResult.Unparsable;
        }
        mapIdsToNames.put(idTitle, "title");

        int idArtist = getIdOfChildWithName(remoteView, "artist");
        //FooLog.v(TAG, "onNotificationPosted: idArtist=" + toVerboseString(idArtist));
        if (idArtist == 0)
        {
            FooLog.w(TAG, "onNotificationPosted: idArtist == 0; Unparsable");
            return NotificationParseResult.Unparsable;
        }
        mapIdsToNames.put(idArtist, "artist");

        int idStation = getIdOfChildWithName(remoteView, "station");
        //FooLog.v(TAG, "onNotificationPosted: idStation=" + toVerboseString(idStation));
        if (idStation == 0)
        {
            FooLog.w(TAG, "onNotificationPosted: idStation == 0; Unparsable");
            return NotificationParseResult.Unparsable;
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

        int idPlay = getIdOfChildWithName(remoteView, "play");
        FooLog.v(TAG,
                "onNotificationPosted: idPlay=" + toVerboseString(idPlay));
        if (idPlay == 0)
        {
            FooLog.w(TAG, "onNotificationPosted: idPlay == 0; Unparsable");
            return NotificationParseResult.Unparsable;
        }
        mapIdsToNames.put(idPlay, "play");

        int idSkip = getIdOfChildWithName(remoteView, "skip");
        FooLog.v(TAG,
                "onNotificationPosted: idSkip=" + toVerboseString(idSkip));
        if (idSkip == 0)
        {
            FooLog.w(TAG, "onNotificationPosted: idSkip == 0; Unparsable");
            return NotificationParseResult.Unparsable;
        }
        mapIdsToNames.put(idSkip, "skip");

        int idLoading = getIdOfChildWithName(remoteView, "loading");
        FooLog.v(TAG,
                "onNotificationPosted: idLoading=" + toVerboseString(idLoading));
        if (idLoading == 0)
        {
            FooLog.w(TAG, "onNotificationPosted: idLoading == 0; Unparsable");
            return NotificationParseResult.Unparsable;
        }
        mapIdsToNames.put(idLoading, "loading");

        //
        // Source: com.pandora.android v7.2
        //
        Context remoteContext = remoteView.getContext();
        int idDrawablePause = getIdentifier(remoteContext, ResourceType.drawable, "notification_pause_selector"); // 2130838193(0x7F0202B1)
        FooLog.v(TAG, "onNotificationPosted: idDrawablePause=" + toVerboseString(idDrawablePause));
        int idDrawablePlay = getIdentifier(remoteContext, ResourceType.drawable, "notification_play_selector"); // 2130838194(0x7F0202B2)
        FooLog.v(TAG, "onNotificationPosted: idDrawablePlay=" + toVerboseString(idDrawablePlay));

        ImageView imageViewPlay = (ImageView) remoteView.findViewById(idPlay);
        if (imageViewPlay == null)
        {
            FooLog.w(TAG, "onNotificationPosted: imageViewPlay == null; Unparsable");
            return NotificationParseResult.Unparsable;
        }

        int idPlayImageResourceId = getImageResource(imageViewPlay);
        FooLog.e(TAG, "\nonNotificationPosted: idPlayImageResourceId=" + toVerboseString(idPlayImageResourceId));
        if (idPlayImageResourceId == 0)
        {
            FooLog.w(TAG, "onNotificationPosted: idPlayImageResourceId == 0; Unparsable");
            return NotificationParseResult.Unparsable;
        }

        boolean playEnabled = imageViewPlay.isEnabled();
        FooLog.e(TAG, "\nonNotificationPosted: playEnabled=" + playEnabled);

        ImageView imageViewIcon = (ImageView) remoteView.findViewById(idIcon);
        if (imageViewIcon == null)
        {
            FooLog.w(TAG, "onNotificationPosted: imageViewIcon == null; Unparsable");
            return NotificationParseResult.Unparsable;
        }

        BitmapDrawable bitmapIcon = getImageBitmap(imageViewIcon);
        FooLog.e(TAG, "\nonNotificationPosted: bitmapIcon=" + bitmapIcon);
        /*
        Integer idIconBitmapResourceId = (Integer) getRemoteViewValueById(bigContentView, idIcon, ValueTypes.BITMAP_RESOURCE_ID);
        FooLog.e(TAG, "onNotificationPosted: idIconBitmapResourceId=" + toVerboseString(idIconBitmapResourceId));
        if (idIconBitmapResourceId == null)
        {
            //return false;
        }
        */

        int iconVisibility = imageViewIcon.getVisibility();
        FooLog.e(TAG, "\nonNotificationPosted: iconVisibility=" + FooViewUtils.viewVisibilityToString(iconVisibility));

        ProgressBar progressBarLoading = (ProgressBar) remoteView.findViewById(idLoading);
        if (progressBarLoading == null)
        {
            FooLog.w(TAG, "onNotificationPosted: progressBarLoading == null; Unparsable");
            return NotificationParseResult.Unparsable;
        }
        int loadingVisibility = progressBarLoading.getVisibility();
        FooLog.e(TAG,
                "\nonNotificationPosted: loadingVisibility=" + FooViewUtils.viewVisibilityToString(loadingVisibility));

        PendingIntent pendingIntentPlay = (PendingIntent) getRemoteViewValueById(bigContentView, idPlay, ValueTypes.PENDING_INTENT);
        FooLog.e(TAG, "onNotificationPosted: pendingIntentPlay=" + pendingIntentPlay);
        //pendingIntentPlay.getIntentSender().

        if (true)
        {
            List<KeyValue> bigContentViewKeyValues = new LinkedList<>();
            walkActions(bigContentView, bigContentViewKeyValues);
            Collections.sort(bigContentViewKeyValues);
            for (KeyValue keyValue : bigContentViewKeyValues)
            {
                int viewId = keyValue.mViewId;

                String viewIdName = mapIdsToNames.get(viewId);

                if (viewIdName != null)
                {
                    FooLog.e(TAG, viewIdName + " keyValue=" + keyValue);
                }
            }
        }

        boolean isPausedByUser = loadingVisibility == View.GONE &&
                                 bitmapIcon != null &&
                                 idPlayImageResourceId == idDrawablePlay;
        FooLog.v(TAG, "onNotificationPosted: isPausedByUser=" + isPausedByUser);

        //boolean isLoading = bitmapIcon == null || loadingVisibility == View.VISIBLE;
        //FooLog.v(TAG, "onNotificationPosted: isLoading=" + isLoading);
        /*
        if (isLoading)
        {
            //mLastIsLoading = true;
            return NotificationParseResult.ParsableIgnored;
        }
        */

        //boolean isPlaying = isLoading || idPlayImageResourceId == idDrawablePause;
        //FooLog.e(TAG, "onNotificationPosted: isPlaying=" + isPlaying);

        TextView textViewTitle = (TextView) remoteView.findViewById(idTitle);
        if (textViewTitle == null)
        {
            FooLog.w(TAG, "onNotificationPosted: textViewTitle == null; Unparsable");
            return NotificationParseResult.Unparsable;
        }
        String textTitle = unknownIfNullOrEmpty(mApplication, textViewTitle.getText());
        FooLog.v(TAG, "onNotificationPosted: textTitle=" + FooString.quote(textTitle));

        TextView textViewArtist = (TextView) remoteView.findViewById(idArtist);
        if (textViewArtist == null)
        {
            FooLog.w(TAG, "onNotificationPosted: textViewArtist == null; Unparsable");
            return NotificationParseResult.Unparsable;
        }
        String textArtist = unknownIfNullOrEmpty(mApplication, textViewArtist.getText());
        FooLog.v(TAG, "onNotificationPosted: textArtist=" + FooString.quote(textArtist));

        TextView textViewStation = (TextView) remoteView.findViewById(idStation);
        if (textViewStation == null)
        {
            FooLog.w(TAG, "onNotificationPosted: textViewStation == null; Unparsable");
            return NotificationParseResult.Unparsable;
        }
        String textStation = unknownIfNullOrEmpty(mApplication, textViewStation.getText());
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
                mute(true, "attenuating " + mPackageAppSpokenName + " commercial");
            }

            FooLog.w(TAG, "onNotificationPosted: isCommercial == true; ParsableIgnored");

            return NotificationParseResult.ParsableIgnored;
        }

        //FooLog.v(TAG, "onNotificationPosted: mLastIsPlaying=" + mLastIsPlaying);
        FooLog.v(TAG, "onNotificationPosted: mLastIsPausedByUser=" + mLastIsPausedByUser);

        //if ((mLastIsPlaying != null && isPlaying == mLastIsPlaying) &&
        if ((mLastIsPausedByUser != null && isPausedByUser == mLastIsPausedByUser) &&
            textTitle.equals(mLastTitle) &&
            textArtist.equals(mLastArtist) &&
            textStation.equals(mLastStation))
        {
            FooLog.w(TAG, "onNotificationPosted: data unchanged; ParsableIgnored");
            return NotificationParseResult.ParsableIgnored;
        }

        mute(false, null);//, "un-muting commercial");

        //mLastIsPlaying = isPlaying;
        mLastIsPausedByUser = isPausedByUser;
        mLastTitle = textTitle;
        mLastArtist = textArtist;
        mLastStation = textStation;

        FooTextToSpeechBuilder builder = new FooTextToSpeechBuilder();

        if (!isPausedByUser)
        {
            FooLog.w(TAG, "onNotificationPosted: playing");

            builder.appendSpeech(mPackageAppSpokenName + " playing")
                    .appendSilence(500)
                    .appendSpeech("artist " + textArtist)
                    .appendSilence(500)
                    .appendSpeech("title " + textTitle);
            //.appendSilence(500)
            //.appendSpeech("station " + textStation);
        }
        else
        {
            FooLog.w(TAG, "onNotificationPosted: paused");

            builder.appendSpeech(mPackageAppSpokenName + " paused");
        }

        mApplication.speak(builder);

        return NotificationParseResult.ParsableHandled;
    }
}

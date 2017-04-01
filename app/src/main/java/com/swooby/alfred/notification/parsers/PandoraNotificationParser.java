package com.swooby.alfred.notification.parsers;

import android.app.Notification.Action;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.smartfoo.android.core.BuildConfig;
import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.platform.FooPlatformUtils;
import com.smartfoo.android.core.texttospeech.FooTextToSpeechBuilder;
import com.smartfoo.android.core.view.FooViewUtils;
import com.swooby.alfred.notification.parsers.NotificationParserUtils.ActionInfo;
import com.swooby.alfred.notification.parsers.NotificationParserUtils.ActionInfos;
import com.swooby.alfred.notification.parsers.NotificationParserUtils.ActionValueType;
import com.swooby.alfred.notification.parsers.NotificationParserUtils.ResourceType;
import com.swooby.alfred.notification.parsers.NotificationParserUtils.ViewWrapper;
import com.swooby.alfred.notification.parsers.NotificationParserUtils.ViewWrappers;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

import static com.swooby.alfred.notification.parsers.NotificationParserUtils.getActions;
import static com.swooby.alfred.notification.parsers.NotificationParserUtils.getBigContentRemoteViews;
import static com.swooby.alfred.notification.parsers.NotificationParserUtils.getExtras;
import static com.swooby.alfred.notification.parsers.NotificationParserUtils.getIdentifier;
import static com.swooby.alfred.notification.parsers.NotificationParserUtils.getImageBitmap;
import static com.swooby.alfred.notification.parsers.NotificationParserUtils.inflateRemoteView;
import static com.swooby.alfred.notification.parsers.NotificationParserUtils.logResourceInfo;
import static com.swooby.alfred.notification.parsers.NotificationParserUtils.toVerboseString;
import static com.swooby.alfred.notification.parsers.NotificationParserUtils.unknownIfNullOrEmpty;
import static com.swooby.alfred.notification.parsers.NotificationParserUtils.walkActions;
import static com.swooby.alfred.notification.parsers.NotificationParserUtils.walkView;

public class PandoraNotificationParser
        extends AbstractMediaPlayerNotificiationParser
{
    private static final String TAG = FooLog.TAG(PandoraNotificationParser.class);

    // TODO:(pv) Make this nullable Boolean to better handle proper state at startup
    //protected boolean mLastIsLoading;
    //protected Boolean mLastIsPlaying;
    private boolean      mLastIsCommercial;
    private Boolean      mLastIsPausedByUser;
    private CharSequence mLastArtist;
    private CharSequence mLastTitle;
    private CharSequence mLastStationOrAlbum;

    public PandoraNotificationParser(@NonNull NotificationParserCallbacks callbacks)
    {
        super("#PANDORA", callbacks);//, application.getString(R.string.pandora_package_app_name));
    }

    @Override
    public String getPackageName()
    {
        return "com.pandora.android";
    }

    @Override
    public NotificationParseResult onNotificationPosted(StatusBarNotification sbn)
    {
        FooLog.i(TAG, "---- " + hashtag() + " ----");
        if (false && BuildConfig.DEBUG)
        {
            super.onNotificationPosted(sbn);
        }

        final String prefix = hashtag("onNotificationPosted");

        Bundle extras = getExtras(sbn);
        FooLog.v(TAG, prefix + " extras == " + FooPlatformUtils.toString(extras));

        Action[] actions = getActions(sbn);
        FooLog.v(TAG, prefix + " actions == " + Arrays.toString(actions));

        RemoteViews bigContentRemoteViews = getBigContentRemoteViews(sbn);
        if (bigContentRemoteViews == null)
        {
            FooLog.w(TAG, prefix + " bigContentRemoteViews == null; Unparsable");
            return NotificationParseResult.Unparsable;
        }

        Context context = getContext();

        View remoteView = inflateRemoteView(context, bigContentRemoteViews);
        if (remoteView == null)
        {
            FooLog.w(TAG, prefix + " remoteView == null; Unparsable");
            return NotificationParseResult.Unparsable;
        }

        //
        // Extract some needed constants from the resources
        //
        Context remoteContext = remoteView.getContext();

        if (false)
        {
            //
            // Log any ids listed in the below walkView/walkActions that you need reverse looked up
            //
            logResourceInfo(remoteContext, 0x7f02022f); // 2130838063 0x7f02022f == ic_pause
            logResourceInfo(remoteContext, 0x7f020240); // 2130838080 0x7f020240 == ic_play
            logResourceInfo(remoteContext, 0x7f0a00a0); // 2131361952 0x7f0a00a0 == advertisement
            logResourceInfo(remoteContext, 0x7f0a053f); // 2131363135 0x7f0a053f == why_ads_label
        }

        final int ID_DRAWABLE_PLAYING_PAUSE = getIdentifier(remoteContext, ResourceType.drawable, "ic_pause");
        FooLog.v(TAG, prefix + " ID_DRAWABLE_PLAYING_PAUSE == " + toVerboseString(ID_DRAWABLE_PLAYING_PAUSE));
        final int ID_DRAWABLE_PAUSED_PLAY = getIdentifier(remoteContext, ResourceType.drawable, "ic_play");
        FooLog.v(TAG, prefix + " ID_DRAWABLE_PAUSED_PLAY == " + toVerboseString(ID_DRAWABLE_PAUSED_PLAY));
        final int ID_STRING_ADVERTISEMENT = getIdentifier(remoteContext, ResourceType.string, "advertisement");
        FooLog.v(TAG, prefix + " ID_STRING_ADVERTISEMENT == " + toVerboseString(ID_STRING_ADVERTISEMENT));
        final int ID_STRING_WHY_ADS_LABEL = getIdentifier(remoteContext, ResourceType.string, "why_ads_label");
        FooLog.v(TAG, prefix + " ID_STRING_WHY_ADS_LABEL == " + toVerboseString(ID_STRING_WHY_ADS_LABEL));

        Resources remoteResources = remoteContext.getResources();
        final String ADVERTISEMENT_TITLE = remoteResources.getString(ID_STRING_ADVERTISEMENT);
        FooLog.v(TAG, prefix + " ADVERTISEMENT_TITLE == " + FooString.quote(ADVERTISEMENT_TITLE));
        final String ADVERTISEMENT_ARTIST = remoteResources.getString(ID_STRING_WHY_ADS_LABEL);
        FooLog.v(TAG, prefix + " ADVERTISEMENT_ARTIST == " + FooString.quote(ADVERTISEMENT_ARTIST));

        //
        // Walk the views/actions...
        //

        ViewWrappers bigContentViewWrappers = new ViewWrappers();
        walkView(remoteView, bigContentViewWrappers, false);

        ActionInfos bigContentIdToActionInfo = new ActionInfos();
        walkActions(bigContentRemoteViews, bigContentIdToActionInfo);

        ViewWrapper viewWrapperIcon = bigContentViewWrappers.get("icon");
        FooLog.v(TAG, prefix + " viewWrapperIcon == " + viewWrapperIcon);
        Map<ActionValueType, ActionInfo> actionInfosIcon = bigContentIdToActionInfo.get(viewWrapperIcon.getViewId());
        FooLog.v(TAG, prefix + " actionInfosIcon == " + actionInfosIcon);

        ViewWrapper viewWrapperTitle = bigContentViewWrappers.get("title");
        FooLog.v(TAG, prefix + " viewWrapperTitle == " + viewWrapperTitle);
        Map<ActionValueType, ActionInfo> actionInfosTitle = bigContentIdToActionInfo.get(viewWrapperTitle.getViewId());
        FooLog.v(TAG, prefix + " actionInfosTitle == " + actionInfosTitle);

        ViewWrapper viewWrapperArtist = bigContentViewWrappers.get("artist");
        FooLog.v(TAG, prefix + " viewWrapperArtist == " + viewWrapperArtist);
        Map<ActionValueType, ActionInfo> actionInfosArtist = bigContentIdToActionInfo.get(viewWrapperArtist.getViewId());
        FooLog.v(TAG, prefix + " actionInfosArtist == " + actionInfosArtist);

        ViewWrapper viewWrapperStationOrAlbum = bigContentViewWrappers.get("stationOrAlbum");
        FooLog.v(TAG, prefix + " viewWrapperStationOrAlbum == " + viewWrapperStationOrAlbum);
        Map<ActionValueType, ActionInfo> actionInfosStationOrAlbum = bigContentIdToActionInfo.get(viewWrapperStationOrAlbum
                .getViewId());
        FooLog.v(TAG, prefix + " actionInfosStationOrAlbum == " + actionInfosStationOrAlbum);

        ViewWrapper viewWrapperSkipBackwardReplay = bigContentViewWrappers.get("skip_backward_replay");
        FooLog.v(TAG, prefix + " viewWrapperSkipBackwardReplay == " + viewWrapperSkipBackwardReplay);
        Map<ActionValueType, ActionInfo> actionInfosSkipBackwardReplay = bigContentIdToActionInfo.get(viewWrapperSkipBackwardReplay
                .getViewId());
        FooLog.v(TAG, prefix + " actionInfosSkipBackwardReplay == " + actionInfosSkipBackwardReplay);

        ViewWrapper viewWrapperPlayPause = bigContentViewWrappers.get("play_pause");
        FooLog.v(TAG, prefix + " viewWrapperPlayPause == " + viewWrapperPlayPause);
        Map<ActionValueType, ActionInfo> actionInfosPlayPause = bigContentIdToActionInfo.get(viewWrapperPlayPause.getViewId());
        FooLog.v(TAG, prefix + " actionInfosPlayPause == " + actionInfosPlayPause);

        ViewWrapper viewWrapperSkip = bigContentViewWrappers.get("skip");
        FooLog.v(TAG, prefix + " viewWrapperSkip == " + viewWrapperSkip);
        Map<ActionValueType, ActionInfo> actionInfosSkip = bigContentIdToActionInfo.get(viewWrapperSkip.getViewId());
        FooLog.v(TAG, prefix + " actionInfosSkip == " + actionInfosSkip);

        ViewWrapper viewWrapperLoading = bigContentViewWrappers.get("loading");
        FooLog.v(TAG, prefix + " viewWrapperLoading == " + viewWrapperLoading);
        Map<ActionValueType, ActionInfo> actionInfosLoading = bigContentIdToActionInfo.get(viewWrapperLoading.getViewId());
        FooLog.v(TAG, prefix + " actionInfosLoading == " + actionInfosLoading);

        //
        //
        //

        ActionInfo actionInfoPlayPauseEnabled = actionInfosPlayPause.get(ActionValueType.ENABLED);
        boolean playPauseEnabled = (boolean) actionInfoPlayPauseEnabled.mValue;
        FooLog.v(TAG, prefix + " playPauseEnabled == " + playPauseEnabled);

        ActionInfo actionInfoPlayPauseImageResourceId = actionInfosPlayPause.get(ActionValueType.IMAGE_RESOURCE_ID);
        int idViewPlayPauseImageResourceId = (int) actionInfoPlayPauseImageResourceId.mValue;
        FooLog.v(TAG, prefix + " idViewPlayPauseImageResourceId == " + toVerboseString(idViewPlayPauseImageResourceId));

        boolean isPlayPauseImagePausedPlay = idViewPlayPauseImageResourceId == ID_DRAWABLE_PAUSED_PLAY;
        FooLog.v(TAG, prefix + " isPlayPauseImagePausedPlay == " + isPlayPauseImagePausedPlay);

        ImageView imageViewIcon = (ImageView) viewWrapperIcon.mView;
        if (imageViewIcon == null)
        {
            FooLog.w(TAG, prefix + " imageViewIcon == null; Unparsable");
            return NotificationParseResult.Unparsable;
        }

        BitmapDrawable bitmapIcon = getImageBitmap(imageViewIcon);
        FooLog.v(TAG, prefix + " bitmapIcon == " + bitmapIcon);

        int iconVisibility = imageViewIcon.getVisibility();
        FooLog.v(TAG, prefix + " iconVisibility == " + FooViewUtils.viewVisibilityToString(iconVisibility));

        ProgressBar progressBarLoading = (ProgressBar) viewWrapperLoading.mView;
        if (progressBarLoading == null)
        {
            FooLog.w(TAG, prefix + " progressBarLoading == null; Unparsable");
            return NotificationParseResult.Unparsable;
        }
        int loadingVisibility = progressBarLoading.getVisibility();
        FooLog.v(TAG, prefix + " loadingVisibility == " + FooViewUtils.viewVisibilityToString(loadingVisibility));

        boolean isPausedByUser = isPlayPauseImagePausedPlay &&
                                 loadingVisibility == View.GONE &&
                                 bitmapIcon != null && iconVisibility == View.VISIBLE;
        FooLog.v(TAG, prefix + " isPausedByUser == " + isPausedByUser);

        //boolean isLoading = bitmapIcon == null || loadingVisibility == View.VISIBLE;
        //FooLog.v(TAG, prefix + " isLoading == " + isLoading);
        /*
        if (isLoading)
        {
            //mLastIsLoading = true;
            return NotificationParseResult.ParsableIgnored;
        }
        */

        //boolean isPlaying = isLoading || idPlayImageResourceId == idDrawablePause;
        //FooLog.e(TAG, prefix + " isPlaying == " + isPlaying);

        TextView textViewTitle = (TextView) viewWrapperTitle.mView;
        if (textViewTitle == null)
        {
            FooLog.w(TAG, prefix + " textViewTitle == null; Unparsable");
            return NotificationParseResult.Unparsable;
        }
        CharSequence textTitle = textViewTitle.getText();
        FooLog.v(TAG, prefix + " textTitle == " + FooString.quote(textTitle));

        TextView textViewArtist = (TextView) viewWrapperArtist.mView;
        if (textViewArtist == null)
        {
            FooLog.w(TAG, prefix + " textViewArtist == null; Unparsable");
            return NotificationParseResult.Unparsable;
        }
        CharSequence textArtist = textViewArtist.getText();
        FooLog.v(TAG, prefix + " textArtist == " + FooString.quote(textArtist));

        TextView textViewStationOrAlbum = (TextView) viewWrapperStationOrAlbum.mView;
        if (textViewStationOrAlbum == null)
        {
            FooLog.w(TAG, prefix + " textViewStationOrAlbum == null; Unparsable");
            return NotificationParseResult.Unparsable;
        }
        CharSequence textStationOrAlbum = textViewStationOrAlbum.getText();
        FooLog.v(TAG, prefix + " textStationOrAlbum == " + FooString.quote(textStationOrAlbum));

        textTitle = unknownIfNullOrEmpty(context, textTitle);
        textArtist = unknownIfNullOrEmpty(context, textArtist);
        textStationOrAlbum = unknownIfNullOrEmpty(context, textStationOrAlbum);

        //
        //
        //

        boolean isCommercial = ADVERTISEMENT_TITLE.equals(textTitle) &&
                               ADVERTISEMENT_ARTIST.equals(textArtist);
        if (isCommercial)
        {
            if (!mLastIsCommercial)
            {
                mLastIsCommercial = true;

                // TODO:(pv) Make this a user option...
                if (true)
                {
                    mute(true, "attenuating " + getPackageAppSpokenName() + " commercial");
                }
            }

            FooLog.w(TAG, prefix + " isCommercial == true; ParsableIgnored");
            return NotificationParseResult.ParsableIgnored;
        }

        //
        //
        //

        mLastIsCommercial = false;

        mute(false, null);//, "un-muting commercial");

        //FooLog.v(TAG, prefix + " mLastIsPlaying == " + mLastIsPlaying);
        FooLog.v(TAG, prefix + " mLastIsPausedByUser == " + mLastIsPausedByUser);

        //if ((mLastIsPlaying != null && isPlaying == mLastIsPlaying) &&
        if ((mLastIsPausedByUser != null && isPausedByUser == mLastIsPausedByUser) &&
            Objects.equals(textTitle, mLastTitle) &&
            Objects.equals(textArtist, mLastArtist) &&
            Objects.equals(textStationOrAlbum, mLastStationOrAlbum))
        {
            FooLog.w(TAG, prefix + " data unchanged; ParsableIgnored");
            return NotificationParseResult.ParsableIgnored;
        }

        //mLastIsPlaying = isPlaying;
        mLastIsPausedByUser = isPausedByUser;
        mLastTitle = textTitle;
        mLastArtist = textArtist;
        mLastStationOrAlbum = textStationOrAlbum;

        FooTextToSpeechBuilder builder = new FooTextToSpeechBuilder(getPackageAppSpokenName());

        if (isPausedByUser)
        {
            FooLog.w(TAG, prefix + " paused");

            builder.appendSpeech("paused");
        }
        else
        {
            FooLog.w(TAG, prefix + " playing");

            builder.appendSpeech("playing")
                    .appendSilenceWordBreak()
                    .appendSpeech("artist " + textArtist)
                    .appendSilenceWordBreak()
                    .appendSpeech("title " + textTitle);
            //.appendSilenceWordBreak(500)
            //.appendSpeech("station " + textStationOrAlbum);
        }

        getTextToSpeech().speak(builder);

        return NotificationParseResult.ParsableHandled;
    }
}

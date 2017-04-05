package com.swooby.alfred.notification.parsers;

import android.app.Notification;
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

import java.util.Map;
import java.util.Objects;

import static com.swooby.alfred.notification.parsers.NotificationParserUtils.getBigContentRemoteViews;
import static com.swooby.alfred.notification.parsers.NotificationParserUtils.getExtras;
import static com.swooby.alfred.notification.parsers.NotificationParserUtils.getIdentifier;
import static com.swooby.alfred.notification.parsers.NotificationParserUtils.getImageBitmap;
import static com.swooby.alfred.notification.parsers.NotificationParserUtils.getResourcesForApplication;
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

    private boolean      mIsStillListeningPromptShowing;
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
    protected boolean getSpeakDefaultNotification()
    {
        return true;
    }

    private boolean isStillListeningPrompt(@NonNull Context context, @NonNull StatusBarNotification sbn)
    {
        final String prefix = hashtag("isStillListeningPrompt");

        Bundle extras = getExtras(sbn);
        FooLog.v(TAG, prefix + "       extras == " + FooPlatformUtils.toString(extras));
        if (extras == null)
        {
            return false;
        }

        CharSequence androidTitle = extras.getCharSequence(Notification.EXTRA_TITLE);
        FooLog.v(TAG, prefix + " androidTitle == " + FooString.quote(androidTitle));

        CharSequence androidText = extras.getCharSequence(Notification.EXTRA_TEXT);
        FooLog.v(TAG, prefix + "  androidText == " + FooString.quote(androidText));

        String packageName = sbn.getPackageName();
        Resources resources = getResourcesForApplication(context, packageName);
        if (resources == null)
        {
            return false;
        }

        final int ID_LISTENING_TIMEOUT_NOTIFICATION_TEXT_LINE1 = getIdentifier(resources, packageName, ResourceType.string, "listening_timeout_notification_text_line1");
        FooLog.v(TAG, prefix + " ID_LISTENING_TIMEOUT_NOTIFICATION_TEXT_LINE1 == " +
                      toVerboseString(ID_LISTENING_TIMEOUT_NOTIFICATION_TEXT_LINE1));
        final int ID_LISTENING_TIMEOUT_NOTIFICATION_TEXT_LINE2 = getIdentifier(resources, packageName, ResourceType.string, "listening_timeout_notification_text_line2");
        FooLog.v(TAG, prefix + " ID_LISTENING_TIMEOUT_NOTIFICATION_TEXT_LINE2 == " +
                      toVerboseString(ID_LISTENING_TIMEOUT_NOTIFICATION_TEXT_LINE2));

        String LISTENING_TIMEOUT_NOTIFICATION_TEXT_LINE1 = resources.getString(ID_LISTENING_TIMEOUT_NOTIFICATION_TEXT_LINE1);
        FooLog.v(TAG, prefix + " LISTENING_TIMEOUT_NOTIFICATION_TEXT_LINE1 == " +
                      FooString.quote(LISTENING_TIMEOUT_NOTIFICATION_TEXT_LINE1));
        String LISTENING_TIMEOUT_NOTIFICATION_TEXT_LINE2 = resources.getString(ID_LISTENING_TIMEOUT_NOTIFICATION_TEXT_LINE2);
        FooLog.v(TAG, prefix + " LISTENING_TIMEOUT_NOTIFICATION_TEXT_LINE2 == " +
                      FooString.quote(LISTENING_TIMEOUT_NOTIFICATION_TEXT_LINE2));

        boolean isStillListeningPrompt = LISTENING_TIMEOUT_NOTIFICATION_TEXT_LINE1.equals(androidTitle) &&
                                         LISTENING_TIMEOUT_NOTIFICATION_TEXT_LINE2.equals(androidText);

        if (isStillListeningPrompt)
        {
            FooLog.i(TAG, prefix + " isStillListeningPrompt == true");
        }
        else
        {
            FooLog.v(TAG, prefix + " isStillListeningPrompt == false");
        }

        return isStillListeningPrompt;
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
        if (extras == null)
        {
            FooLog.v(TAG, prefix + " extras == null; Unparsable");
            return NotificationParseResult.Unparsable;
        }

        Context context = getContext();

        RemoteViews bigContentRemoteViews = getBigContentRemoteViews(sbn);
        if (bigContentRemoteViews == null)
        {
            FooLog.v(TAG, prefix + " bigContentRemoteViews == null");

            if (isStillListeningPrompt(context, sbn))
            {
                mIsStillListeningPromptShowing = true;

                // Intentional to cause "Pandora paused" to be spoken
                mLastIsPausedByUser = null;
            }

            FooLog.w(TAG, prefix + " mIsStillListeningPromptShowing == " + mIsStillListeningPromptShowing);

            return super.onNotificationPosted(sbn);
        }

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
        // Walk the views/actions…
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
        BitmapDrawable bitmapIcon = getImageBitmap(imageViewIcon);
        FooLog.v(TAG, prefix + " bitmapIcon == " + bitmapIcon);
        int iconVisibility = imageViewIcon.getVisibility();
        FooLog.v(TAG, prefix + " iconVisibility == " + FooViewUtils.viewVisibilityToString(iconVisibility));

        ProgressBar progressBarLoading = (ProgressBar) viewWrapperLoading.mView;
        int loadingVisibility = progressBarLoading.getVisibility();
        FooLog.v(TAG, prefix + " loadingVisibility == " + FooViewUtils.viewVisibilityToString(loadingVisibility));

        FooLog.v(TAG, prefix + " mIsStillListeningPromptShowing == " + mIsStillListeningPromptShowing);

        boolean isPausedByUser = (isPlayPauseImagePausedPlay &&
                                  loadingVisibility == View.GONE &&
                                  bitmapIcon != null && iconVisibility == View.VISIBLE)
                                 || mIsStillListeningPromptShowing;
        FooLog.v(TAG, prefix + " isPausedByUser == " + isPausedByUser);

        //boolean isLoading = bitmapIcon == null || loadingVisibility == View.VISIBLE;
        //FooLog.v(TAG, prefix + " isLoading == " + isLoading);
        /*
        if (isLoading)
        {
            //mLastIsLoading = true;
            return NotificationParseResult.ParsedIgnored;
        }
        */

        //boolean isPlaying = isLoading || idPlayImageResourceId == idDrawablePause;
        //FooLog.e(TAG, prefix + " isPlaying == " + isPlaying);

        TextView textViewTitle = (TextView) viewWrapperTitle.mView;
        CharSequence textTitle = textViewTitle.getText();
        FooLog.v(TAG, prefix + " textTitle == " + FooString.quote(textTitle));

        TextView textViewArtist = (TextView) viewWrapperArtist.mView;
        CharSequence textArtist = textViewArtist.getText();
        FooLog.v(TAG, prefix + " textArtist == " + FooString.quote(textArtist));

        TextView textViewStationOrAlbum = (TextView) viewWrapperStationOrAlbum.mView;
        CharSequence textStationOrAlbum = textViewStationOrAlbum.getText();
        FooLog.v(TAG, prefix + " textStationOrAlbum == " + FooString.quote(textStationOrAlbum));

        //
        //
        //

        boolean isCommercial = ADVERTISEMENT_TITLE.equals(textTitle) &&
                               ADVERTISEMENT_ARTIST.equals(textArtist);
        FooLog.v(TAG, prefix + " isCommercial == " + isCommercial);
        if (isCommercial)
        {
            return onCommercial(prefix);
        }

        onNonCommercial();

        //
        //
        //

        textTitle = unknownIfNullOrEmpty(context, textTitle);
        textArtist = unknownIfNullOrEmpty(context, textArtist);
        textStationOrAlbum = unknownIfNullOrEmpty(context, textStationOrAlbum);

        //FooLog.v(TAG, prefix + " mLastIsPlaying == " + mLastIsPlaying);
        FooLog.v(TAG, prefix + " mLastIsPausedByUser == " + mLastIsPausedByUser);

        //if ((mLastIsPlaying != null && isPlaying == mLastIsPlaying) &&
        if ((mLastIsPausedByUser != null && isPausedByUser == mLastIsPausedByUser) &&
            Objects.equals(textTitle, mLastTitle) &&
            Objects.equals(textArtist, mLastArtist) &&
            Objects.equals(textStationOrAlbum, mLastStationOrAlbum))
        {
            FooLog.w(TAG, prefix + " data unchanged; ParsedIgnored");
            return NotificationParseResult.ParsedIgnored;
        }

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

        return NotificationParseResult.ParsedHandled;
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn)
    {
        super.onNotificationRemoved(sbn);

        final String prefix = hashtag("onNotificationRemoved");

        Context context = getContext();

        if (isStillListeningPrompt(context, sbn))
        {
            mIsStillListeningPromptShowing = false;
        }

        FooLog.w(TAG, prefix + " mIsStillListeningPromptShowing == " + mIsStillListeningPromptShowing);
    }
}

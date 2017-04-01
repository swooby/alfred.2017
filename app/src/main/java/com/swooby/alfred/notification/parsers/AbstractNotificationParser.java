package com.swooby.alfred.notification.parsers;

import android.app.Notification;
import android.content.Context;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.smartfoo.android.core.FooRun;
import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.annotations.NonNullNonEmpty;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.platform.FooPlatformUtils;
import com.smartfoo.android.core.texttospeech.FooTextToSpeechBuilder;
import com.swooby.alfred.TextToSpeechManager;
import com.swooby.alfred.notification.parsers.NotificationParserUtils.WalkViewCallbacks;

import static com.swooby.alfred.notification.parsers.NotificationParserUtils.inflateRemoteView;
import static com.swooby.alfred.notification.parsers.NotificationParserUtils.walkView;

public abstract class AbstractNotificationParser
{
    private static final String TAG = FooLog.TAG(AbstractNotificationParser.class);

    /**
     * @param context
     * @param sbn
     * @param textToSpeechManager set to null suppress textToSpeech (helps when debugging parsing)
     * @return NotificationParseResult
     */
    public static NotificationParseResult defaultOnNotificationPosted(
            Context context,
            StatusBarNotification sbn,
            TextToSpeechManager textToSpeechManager)
    {
        return defaultOnNotificationPosted(context, sbn, textToSpeechManager, null);
    }

    /**
     * @param context
     * @param sbn
     * @param textToSpeechManager  set to null suppress textToSpeech (helps when debugging parsing)
     * @param packageAppSpokenName
     * @return NotificationParseResult
     */
    public static NotificationParseResult defaultOnNotificationPosted(
            Context context,
            StatusBarNotification sbn,
            TextToSpeechManager textToSpeechManager,
            String packageAppSpokenName)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(context, "context");
        FooRun.throwIllegalArgumentExceptionIfNull(sbn, "sbn");

        String packageName = NotificationParserUtils.getPackageName(sbn);
        FooLog.v(TAG, "defaultOnNotificationPosted: packageName=" + FooString.quote(packageName));
        FooRun.throwIllegalArgumentExceptionIfNullOrEmpty(packageName, "packageName");

        if (FooString.isNullOrEmpty(packageAppSpokenName))
        {
            packageAppSpokenName = FooPlatformUtils.getApplicationName(context, packageName);
        }
        FooLog.v(TAG, "defaultOnNotificationPosted: packageAppSpokenName=" + FooString.quote(packageAppSpokenName));
        FooRun.throwIllegalArgumentExceptionIfNullOrEmpty(packageAppSpokenName, "packageAppSpokenName");

        //String groupKey = sbn.getGroupKey();
        //String key = sbn.getKey();
        //UserHandle user = sbn.getUser();
        //long postTime = sbn.getPostTime();
        //int id = sbn.getId();
        //String tag = sbn.getTag();

        Notification notification = NotificationParserUtils.getNotification(sbn);
        FooLog.v(TAG, "defaultOnNotificationPosted: notification=" + notification);
        if (notification == null)
        {
            FooLog.v(TAG, "defaultOnNotificationPosted: notification == null; Unparsable");
            return NotificationParseResult.Unparsable;
        }

        Bundle extras = notification.extras;
        FooLog.v(TAG, "defaultOnNotificationPosted: extras=" + FooPlatformUtils.toString(extras));

        CharSequence tickerText = notification.tickerText;
        FooLog.v(TAG, "defaultOnNotificationPosted: tickerText=" + FooString.quote(tickerText));

        // TODO:(pv) Seriously, introspect and walk all StatusBarNotification fields, especially:
        //  Notification.tickerText
        //  All ImageView Resource Ids and TextView Texts in BigContentView
        //  All ImageView Resource Ids and TextView Texts in ContentView

        final FooTextToSpeechBuilder builder = new FooTextToSpeechBuilder(packageAppSpokenName);

        WalkViewCallbacks walkViewCallbacks = new WalkViewCallbacks()
        {
            @Override
            public void onTextView(TextView textView)
            {
                if (textView.getVisibility() != View.VISIBLE)
                {
                    return;
                }

                String text = textView.getText().toString();
                if (FooString.isNullOrEmpty(text))
                {
                    return;
                }

                builder.appendSpeech(text);
            }
        };

        FooLog.v(TAG, "defaultOnNotificationPosted: ---- bigContentView ----");
        // NOTE: "As of N, this field may be null." :(
        RemoteViews bigContentView = notification.bigContentView;
        View inflatedBigContentView = inflateRemoteView(context, bigContentView);
        walkView(inflatedBigContentView, null, true, walkViewCallbacks);
        //View mockBigContentView = mockRemoteView(mainApplication, bigContentView);
        //Set<Integer> bigContentViewIds = new LinkedHashSet<>();
        //walkView(mockBigContentView, bigContentViewIds);
        /*
        List<KeyValue> bigContentViewKeyValues = new LinkedList<>();
        walkActions(bigContentView, bigContentViewKeyValues);
        for (int i = 0; i < bigContentViewKeyValues.size(); i++)
        {
            KeyValue keyValue = bigContentViewKeyValues.get(i);
            FooLog.e(TAG, "bigContentView.mAction[" + i + "]=" + keyValue);
        }
        */

        FooLog.v(TAG, "defaultOnNotificationPosted: ---- contentView ----");
        // NOTE: "As of N, this field may be null." :(
        RemoteViews contentView = notification.contentView;
        View inflatedContentView = inflateRemoteView(context, contentView);
        walkView(inflatedContentView, null, true, bigContentView != null ? null : walkViewCallbacks);
        //View mockContentView = mockRemoteView(mainApplication, contentView);
        //Set<Integer> contentViewIds = new LinkedHashSet<>();
        //walkView(mockContentView, contentViewIds);
        /*
        List<KeyValue> contentViewKeyValues = new LinkedList<>();
        walkActions(contentView, contentViewKeyValues);
        for (int i = 0; i < contentViewKeyValues.size(); i++)
        {
            KeyValue keyValue = contentViewKeyValues.get(i);
            FooLog.e(TAG, "contentView.mAction[" + i + "]=" + keyValue);
        }
        */

        //RemoteViews headUpContentView = notification.headsUpContentView;

        //Notification.Action[] actions = notification.actions;

        //String category = notification.category;

        if (textToSpeechManager == null)
        {
            return NotificationParseResult.ParsableIgnored;
        }

        if (builder.getNumberOfParts() == 1)
        {
            if (tickerText != null)
            {
                builder.appendSilenceSentenceBreak()
                        .appendSpeech(tickerText.toString());
            }
            else
            {
                boolean appended = false;

                CharSequence androidTitle = extras.getCharSequence(Notification.EXTRA_TITLE);
                FooLog.v(TAG, "defaultOnNotificationPosted: androidTitle == " + FooString.quote(androidTitle));
                if (androidTitle != null)
                {
                    builder.appendSilenceSentenceBreak()
                            .appendSpeech(androidTitle.toString());
                    appended = true;
                }

                CharSequence androidText = extras.getCharSequence(Notification.EXTRA_TEXT);
                FooLog.v(TAG, "defaultOnNotificationPosted: androidText == " + FooString.quote(androidText));
                if (androidText != null)
                {
                    if (!appended)
                    {
                        builder.appendSilenceSentenceBreak();
                    }
                    builder.appendSpeech(androidText.toString());
                }
            }
        }

        textToSpeechManager.speak(builder);

        return NotificationParseResult.DefaultWithTickerText;
    }

    public enum NotificationParseResult
    {
        DefaultWithTickerText,
        DefaultWithoutTickerText,
        Unparsable,
        ParsableIgnored,
        ParsableHandled,
    }

    public interface NotificationParserCallbacks
    {
        @NonNull
        Context getContext();

        @NonNull
        TextToSpeechManager getTextToSpeech();

        void onNotificationParsed(@NonNull AbstractNotificationParser parser);
    }

    private final   String                      mHashtag;
    protected final NotificationParserCallbacks mCallbacks;

    protected AbstractNotificationParser(@NonNull String hashtag, @NonNull NotificationParserCallbacks callbacks)
    {
        mHashtag = FooRun.toNonNullNonEmpty(hashtag, "hashtag").startsWith("#") ? hashtag : ("#" + hashtag);
        mCallbacks = FooRun.toNonNull(callbacks, "callbacks");
    }

    protected String hashtag()
    {
        return hashtag(null);
    }

    protected String hashtag(String methodName)
    {
        return FooString.isNullOrEmpty(methodName) ? mHashtag : (methodName + ": " + mHashtag);
    }

    @NonNullNonEmpty
    public abstract String getPackageName();

    public String getPackageAppSpokenName()
    {
        return FooPlatformUtils.getApplicationName(getContext(), getPackageName());
    }

    protected Context getContext()
    {
        return mCallbacks.getContext();
    }

    protected TextToSpeechManager getTextToSpeech()
    {
        return mCallbacks.getTextToSpeech();
    }

    protected boolean getSpeakDefaultNotification()
    {
        return false;
    }

    public NotificationParseResult onNotificationPosted(StatusBarNotification sbn)
    {
        return defaultOnNotificationPosted(getContext(),
                sbn,
                getSpeakDefaultNotification() ? getTextToSpeech() : null,
                getPackageAppSpokenName());
    }

    public void onNotificationRemoved(StatusBarNotification sbn)
    {
    }
}

package com.swooby.alfred;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;

import com.smartfoo.android.core.FooRun;
import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.collections.FooBundleBuilder;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.notification.FooNotification;
import com.smartfoo.android.core.notification.FooNotificationBuilder;
import com.smartfoo.android.core.notification.FooNotificationListenerManager;
import com.smartfoo.android.core.platform.FooRes;
import com.swooby.alfred.Profile.Tokens;

public class NotificationManager
{
    private static final String TAG = FooLog.TAG(NotificationManager.class);

    private static final String PACKAGE_NAME        = NotificationManager.class.getPackage().getName();
    public static final  String EXTRA_ALFRED_EXTRAS = PACKAGE_NAME + ".EXTRAS";
    /**
     * Must be put inside a Bundle of key {@link #EXTRA_ALFRED_EXTRAS}
     */
    public static        String EXTRA_ALFRED_SPEECH = PACKAGE_NAME + ".SPEECH";

    @NonNull
    public static PendingIntent createPendingIntentMainActivity(@NonNull Context context)
    {
        return createPendingIntentMainActivity(context, NotificationIds.ONGOING);
    }

    @NonNull
    public static PendingIntent createPendingIntentMainActivity(@NonNull Context context, int requestCode)
    {
        return FooNotification.createPendingIntentForActivity(context, requestCode, MainActivity.class);
    }

    @NonNull
    public static PendingIntent createPendingIntentNotificationListenerSettingsActivity(@NonNull Context context)
    {
        return createPendingIntentNotificationListenerSettingsActivity(context, NotificationIds.ONGOING);
    }

    @NonNull
    public static PendingIntent createPendingIntentNotificationListenerSettingsActivity(@NonNull Context context, int requestCode)
    {
        Intent intent = FooNotificationListenerManager.getIntentNotificationListenerSettings();
        return FooNotification.createPendingIntentForActivity(context, requestCode, intent);
    }

    public static abstract class NotificationStatus
    {
        @NonNull
        protected final Context mContext;
        private final   int     mSmallIcon;
        @NonNull
        private final   String  mText;
        protected final Bundle  mExtras;
        protected final int     mRequestCode;

        protected NotificationStatus(@NonNull Context context, @DrawableRes int smallIcon, @NonNull String text, String subtext, Bundle extras)
        {
            this(context, smallIcon, text, subtext, extras, NotificationIds.ONGOING);
        }

        protected NotificationStatus(@NonNull Context context, int smallIcon, @NonNull String text, String subtext, Bundle extras, int requestCode)
        {
            FooRun.throwIllegalArgumentExceptionIfNull(context, "context");
            if (!FooString.isNullOrEmpty(subtext))
            {
                text = context.getString(R.string.alfred_A_colon_B, text, subtext);
            }

            mContext = context;
            mSmallIcon = smallIcon;
            mText = text;
            mExtras = extras;
            mRequestCode = requestCode;
        }

        @Override
        public String toString()
        {
            return getText();
        }

        @DrawableRes
        public int getSmallIcon()
        {
            return mSmallIcon;
        }

        @NonNull
        public String getText()
        {
            return mText;
        }

        public Bundle getExtras()
        {
            return mExtras;
        }

        @NonNull
        public PendingIntent getPendingIntent()
        {
            return createPendingIntentMainActivity(mContext, mRequestCode);
        }
    }

    private static class NotificationStatusStarting
            extends NotificationStatus
    {
        NotificationStatusStarting(@NonNull Context context, @NonNull String text, String subtext, Bundle extras)
        {
            super(context, R.drawable.ic_warning_white_18dp, text, subtext, extras);
        }
    }

    static class NotificationStatusRunning
            extends NotificationStatus
    {
        @NonNull
        private static String getDefaultText(@NonNull Context context)
        {
            return context.getString(R.string.alfred_running);
        }

        private static Bundle DEFAULT_EXTRAS;

        private static Bundle getDefaultExtras(@NonNull Context context)
        {
            return DEFAULT_EXTRAS;
        }

        NotificationStatusRunning(@NonNull Context context)
        {
            this(context, getDefaultText(context), context.getString(R.string.alfred_reading_notifications), getDefaultExtras(context));
        }

        NotificationStatusRunning(@NonNull Context context, @NonNull String text, String subtext, Bundle extras)
        {
            super(context, R.drawable.ic_alfred_running_white_18dp, text, subtext, extras);
        }
    }

    static class NotificationStatusNotificationAccessNotEnabled
            extends NotificationStatus
    {
        NotificationStatusNotificationAccessNotEnabled(@NonNull Context context, @NonNull String text, String subtext, Bundle extras)
        {
            super(context, R.drawable.ic_warning_white_18dp, text, subtext, extras, 0);
        }

        @NonNull
        @Override
        public PendingIntent getPendingIntent()
        {
            return createPendingIntentNotificationListenerSettingsActivity(mContext, mRequestCode);
        }
    }

    static class NotificationStatusProfileNotEnabled
            extends NotificationStatus
    {
        private static String toString(@NonNull Context context, @NonNull String token)
        {
            String s = null;
            switch (token)
            {
                case Tokens.DISABLED:
                    return context.getString(R.string.alfred_manually_disabled);
                case Tokens.HEADPHONES_WIRED:
                    s = context.getString(R.string.alfred_wired_headphone);
                    break;
            }

            if (s != null)
            {
                s = context.getString(R.string.alfred_waiting_for_X, s);
            }

            return s;
        }

        NotificationStatusProfileNotEnabled(@NonNull Context context, String profileToken)
        {
            super(context, R.drawable.ic_alfred_paused_white_18dp, FooRes.getString(context, R.string.alfred_paused), toString(context, profileToken), null);
        }
    }

    private interface NotificationIds
    {
        int ONGOING = 100;
    }

    private final Context mContext;

    private FooNotification mNotificationOngoing;

    NotificationManager(@NonNull Context context)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(context, "context");
        mContext = context;
    }

    private String getString(int resId, Object... formatArgs)
    {
        return mContext.getString(resId, formatArgs);
    }

    private FooNotification notificationShow(int requestCode, FooNotificationBuilder builder)
    {
        FooNotification notification = new FooNotification(requestCode, builder);
        FooLog.v(TAG, "notificationShow: notification=" + notification);
        notification.show(mContext);
        return notification;
    }

    private FooNotification notificationShow(int requestCode,
                                             boolean ongoing,
                                             @NonNull NotificationStatus status,
                                             @NonNull String contentTitle,
                                             String contentText)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(status, "status");
        FooRun.throwIllegalArgumentExceptionIfNullOrEmpty(contentTitle, "contentTitle");
        //FooRun.throwIllegalArgumentExceptionIfNullOrEmpty(contentText, "contentText");

        FooNotificationBuilder builder = new FooNotificationBuilder(mContext)
                .setOngoing(ongoing)
                .setSmallIcon(status.getSmallIcon())
                .setSubText(status.getText())
                .setContentTitle(contentTitle)
                .setContentIntent(status.getPendingIntent())
                .addExtras(new FooBundleBuilder()
                        .putBundle(EXTRA_ALFRED_EXTRAS, status.getExtras())
                        .build());
        if (!FooString.isNullOrEmpty(contentText))
        {
            builder.setContentText(contentText);
        }

        return notificationShow(requestCode, builder);
    }

    //
    //
    //

    void notifyInitializing(@NonNull String statusSubText, @NonNull String contentTitle, String contentText)
    {
        NotificationStatus notificationStatus = new NotificationStatusStarting(mContext, getString(R.string.alfred_initializing), statusSubText, null);
        notificationOngoingShow(notificationStatus, contentTitle, contentText);
    }

    void notifyRunning(@NonNull NotificationStatus notificationStatus, @NonNull String contentTitle, String contentText)
    {
        notificationOngoingShow(notificationStatus, contentTitle, contentText);
    }

    void notifyPaused(@NonNull NotificationStatus notificationStatus, @NonNull String contentTitle, String contentText)
    {
        notificationOngoingShow(notificationStatus, contentTitle, contentText);
    }

    private void notificationOngoingShow(@NonNull NotificationStatus notificationStatus, @NonNull String contentTitle, String contentText)
    {
        mNotificationOngoing = notificationShow(NotificationIds.ONGOING, true, notificationStatus, contentTitle, contentText);
    }

    private void notificationOngoingCancel()
    {
        if (mNotificationOngoing != null)
        {
            mNotificationOngoing.cancel(mContext);
            mNotificationOngoing = null;
        }
    }
}

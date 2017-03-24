package com.swooby.alfred;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;

import com.smartfoo.android.core.FooRun;
import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.notification.FooNotification;
import com.smartfoo.android.core.notification.FooNotificationBuilder;
import com.smartfoo.android.core.notification.FooNotificationListenerManager;
import com.smartfoo.android.core.platform.FooRes;
import com.swooby.alfred.Profile.Tokens;

public class NotificationManager
{
    private static final String TAG = FooLog.TAG(NotificationManager.class);

    @NonNull
    public static PendingIntent createPendingIntentMainActivity(@NonNull Context context)
    {
        return createPendingIntentMainActivity(context, NotificationIds.ONGOING);
    }

    @NonNull
    public static PendingIntent createPendingIntentMainActivity(@NonNull Context context, int requestCode)
    {
        return FooNotification.createPendingIntent(context, requestCode, MainActivity.class);
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
        return FooNotification.createPendingIntent(context, requestCode, intent);
    }

    public static abstract class NotificationStatus
    {
        @NonNull
        protected final Context mContext;
        private final   int     mSmallIcon;
        @NonNull
        private final   String  mText;
        protected final int     mRequestCode;

        protected NotificationStatus(@NonNull Context context, @DrawableRes int smallIcon, @NonNull String text, String subtext)
        {
            this(context, smallIcon, text, subtext, NotificationIds.ONGOING);
        }

        protected NotificationStatus(@NonNull Context context, int smallIcon, @NonNull String text, String subtext, int requestCode)
        {
            FooRun.throwIllegalArgumentExceptionIfNull(context, "context");
            mContext = context;
            mSmallIcon = smallIcon;
            mText = FooString.isNullOrEmpty(subtext) ? text : context.getString(R.string.alfred_A_colon_B, text, subtext);
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

        @NonNull
        public PendingIntent getPendingIntent()
        {
            return createPendingIntentMainActivity(mContext, mRequestCode);
        }
    }

    public static class NotificationStatusStarting
            extends NotificationStatus
    {
        protected NotificationStatusStarting(@NonNull Context context, @NonNull String text, String subtext)
        {
            super(context, R.drawable.ic_warning_white_18dp, text, subtext);
        }
    }

    public static class NotificationStatusRunning
            extends NotificationStatus
    {
        protected NotificationStatusRunning(@NonNull Context context, @NonNull String text, String subtext)
        {
            super(context, R.drawable.ic_alfred_running_white_18dp, text, subtext);
        }
    }

    public static class NotificationStatusNotificationAccessNotEnabled
            extends NotificationStatus
    {
        public NotificationStatusNotificationAccessNotEnabled(@NonNull Context context, @NonNull String text, String subtext)
        {
            super(context, R.drawable.ic_warning_white_18dp, text, subtext, 0);
        }

        @Override
        public PendingIntent getPendingIntent()
        {
            return createPendingIntentNotificationListenerSettingsActivity(mContext, mRequestCode);
        }
    }

    public static class NotificationStatusProfileNotEnabled
            extends NotificationStatus
    {
        private static String toString(@NonNull Context context, @NonNull String token)
        {
            String s = null;
            switch (token)
            {
                case Tokens.DISABLED:
                    return context.getString(R.string.alfred_manually_disabled);
                case Tokens.WIRED_HEADPHONES_ONLY:
                    s = context.getString(R.string.alfred_wired_headphone);
                    break;
            }

            if (s != null)
            {
                s = context.getString(R.string.alfred_waiting_for_X, s);
            }

            return s;
        }

        protected NotificationStatusProfileNotEnabled(@NonNull Context context, String profileToken)
        {
            super(context, R.drawable.ic_alfred_paused_white_18dp, FooRes.getString(context, R.string.alfred_paused), toString(context, profileToken));
        }
    }

    private interface NotificationIds
    {
        int ONGOING = 100;
    }

    private final Context mContext;

    private FooNotification mNotificationOngoing;

    public NotificationManager(@NonNull Context context)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(context, "context");
        mContext = context;
    }

    public void initializing(String statusSubText, String contentTitle, String contentText)
    {
        NotificationStatus notificationStatus = new NotificationStatusStarting(mContext, getString(R.string.alfred_initializing), statusSubText);
        ongoingShow(notificationStatus, contentTitle, contentText);
    }

    public void running(NotificationStatus notificationStatus, String contentTitle, String contentText)
    {
        if (notificationStatus == null)
        {
            notificationStatus = new NotificationStatusRunning(mContext, getString(R.string.alfred_running), getString(R.string.alfred_reading_notifications));
        }
        ongoingShow(notificationStatus, contentTitle, contentText);
    }

    public void paused(@NonNull NotificationStatus notificationStatus, String contentTitle, String contentText)
    {
        ongoingShow(notificationStatus, contentTitle, contentText);
    }

    private void ongoingShow(@NonNull NotificationStatus notificationStatus, String contentTitle, String contentText)
    {
        mNotificationOngoing = notification(NotificationIds.ONGOING, true, notificationStatus, contentTitle, contentText);
    }

    private void ongoingCancel()
    {
        if (mNotificationOngoing != null)
        {
            mNotificationOngoing.cancel(mContext);
            mNotificationOngoing = null;
        }
    }

    //
    //
    //

    private String getString(int resId, Object... formatArgs)
    {
        return mContext.getString(resId, formatArgs);
    }

    public FooNotification notification(int requestCode, boolean ongoing,
                                        @NonNull NotificationStatus status,
                                        String contentTitle, String contentText)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(status, "status");
        FooRun.throwIllegalArgumentExceptionIfNullOrEmpty(contentTitle, "contentTitle");

        // TODO:(pv) Add speech make this the single method that shows/speaks anything we need
        //      Put speech in the waitingFor, or some other helper class? FooTextToSpeechBuilder?
        //      Privatize as many below methods as possible

        FooNotificationBuilder builder = new FooNotificationBuilder(mContext)
                .setOngoing(ongoing)
                .setSmallIcon(status.getSmallIcon())
                .setSubText(status.getText())
                .setContentTitle(contentTitle)
                .setContentIntent(status.getPendingIntent());
        if (!FooString.isNullOrEmpty(contentText))
        {
            builder.setContentText(contentText);
        }

        return notificationShow(requestCode, builder);
    }

    private FooNotification notificationShow(int requestCode, FooNotificationBuilder builder)
    {
        FooNotification notification = new FooNotification(requestCode, builder);
        FooLog.v(TAG, "notificationShow: notification=" + notification);
        notification.show(mContext);
        return notification;
    }
}

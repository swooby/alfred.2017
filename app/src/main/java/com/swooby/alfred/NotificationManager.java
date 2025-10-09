package com.swooby.alfred;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Bundle;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.core.app.NotificationCompat;

import com.smartfoo.android.core.FooRun;
import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.collections.FooBundleBuilder;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.notification.FooNotification;
import com.smartfoo.android.core.notification.FooNotification.Companion.ChannelInfo;
import com.smartfoo.android.core.notification.FooNotificationBuilder;
import com.smartfoo.android.core.notification.FooNotificationListener;
import com.smartfoo.android.core.notification.FooNotificationListenerManager;
import com.smartfoo.android.core.platform.FooRes;
import com.swooby.alfred.Profile.Tokens;

public class NotificationManager
{
    private static final String TAG = FooLog.TAG(NotificationManager.class);

    public static final ChannelInfo CHANNEL_INFO = new ChannelInfo(
            "FOREGROUND_SERVICE_CHANNEL",
            "Foreground Service Channel",
            android.app.NotificationManager.IMPORTANCE_DEFAULT,
            "Non-dismissible notifications for session status");

    public static final int FOREGROUND_SERVICE_TYPE = ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE;

    /** @noinspection DataFlowIssue*/
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

    @Nullable
    public static PendingIntent createPendingIntentNotificationListenerSettingsActivity(@NonNull Context context)
    {
        return createPendingIntentNotificationListenerSettingsActivity(context, NotificationIds.ONGOING);
    }

    @Nullable
    public static PendingIntent createPendingIntentNotificationListenerSettingsActivity(@NonNull Context context, int requestCode)
    {
        Intent intent = FooNotificationListener.getIntentNotificationListenerSettings();
        return intent != null ? FooNotification.createPendingIntentForActivity(context, requestCode, intent) : null;
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

        @NonNull
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

        @Nullable
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
            super(context, R.drawable.ic_warning, text, subtext, extras);
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
            super(context, R.drawable.ic_alfred_running, text, subtext, extras);
        }
    }

    static class NotificationStatusNotificationAccessNotEnabled
            extends NotificationStatus
    {
        NotificationStatusNotificationAccessNotEnabled(@NonNull Context context, @NonNull String text, String subtext, Bundle extras)
        {
            super(context, R.drawable.ic_warning, text, subtext, extras, 0);
        }

        @Nullable
        @Override
        public PendingIntent getPendingIntent()
        {
            return createPendingIntentNotificationListenerSettingsActivity(mContext, mRequestCode);
        }
    }

    static class NotificationStatusProfileNotEnabled
            extends NotificationStatus
    {
        private static String toString(@NonNull Context context, @NonNull Profile profile)
        {
            String s;

            String profileToken = profile.getToken();
            switch (profileToken)
            {
                case Tokens.DISABLED:
                    return context.getString(R.string.alfred_manually_disabled);
                case Tokens.HEADPHONES_WIRED:
                    s = context.getString(R.string.alfred_headphone_wired);
                    break;
                case Tokens.HEADPHONES_BLUETOOTH_ANY:
                    s = context.getString(R.string.alfred_headphone_bluetooth_any);
                    break;
                case Tokens.HEADPHONES_ANY:
                    s = context.getString(R.string.alfred_headphone_any);
                    break;
                case Tokens.ALWAYS_ON:
                    throw new IllegalStateException("Unexpected ALWAYS_ON");
                default:
                    s = profile.getName().trim();
                    break;
            }

            return context.getString(R.string.alfred_waiting_for_X, s);
        }

        NotificationStatusProfileNotEnabled(@NonNull Context context, Profile profile)
        {
            super(context, R.drawable.ic_alfred_paused, FooRes.getString(context, R.string.alfred_paused), toString(context, profile), null);
        }
    }

    private interface NotificationIds
    {
        int ONGOING = 100;
        int ACTION_QUIT = 101;
        int ACTION_PERSISTENT = 102;
    }

    private final Context mContext;
    private final AppPreferences mAppPreferences;

    private FooNotification mNotificationOngoing;

    NotificationManager(@NonNull Context context)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(context, "context");
        mContext = context;
        mAppPreferences = new AppPreferences(context.getApplicationContext());
        FooNotification.createNotificationChannel(mContext, CHANNEL_INFO);
    }

    private String getString(int resId, Object... formatArgs)
    {
        return mContext.getString(resId, formatArgs);
    }

    /** @noinspection SameParameterValue*/
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private FooNotification notificationShow(int requestCode,
                                             int foregroundServiceType,
                                             @NonNull NotificationStatus status,
                                             @NonNull String contentTitle,
                                             @Nullable String contentText)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(status, "status");
        FooRun.throwIllegalArgumentExceptionIfNullOrEmpty(contentTitle, "contentTitle");
        //FooRun.throwIllegalArgumentExceptionIfNullOrEmpty(contentText, "contentText");

        FooNotificationBuilder builder = new FooNotificationBuilder(mContext, CHANNEL_INFO.getId());

        boolean isOngoingNotification = requestCode == NotificationIds.ONGOING || foregroundServiceType != FooNotification.FOREGROUND_SERVICE_TYPE_NONE;
        if (isOngoingNotification)
        {
            builder.setOngoing(true)
                    .setAutoCancel(false)
                    .setOnlyAlertOnce(true)
                    .setCategory(NotificationCompat.CATEGORY_SERVICE);

            if (!mAppPreferences.isPersistentNotificationActionIgnored())
            {
                Notification existingNotification = FooNotification.findCallingAppNotification(mContext, requestCode);
                if (!FooNotification.getNoDismiss(existingNotification))
                {
                    //
                    // NOTE: Since Android 14 (API34) persistent notifications can be dismissed by the user...
                    // ...unless...
                    // https://www.reddit.com/r/tasker/comments/1fv9ez4/how_to_enable_nondismissible_persistent/
                    //
                    // To enable:
                    // `adb shell appops set --uid com.swooby.alfred2017m2 SYSTEM_EXEMPT_FROM_DISMISSIBLE_NOTIFICATIONS allow`
                    //
                    // This will add a `android.app.Notification.FLAG_NO_DISMISS` to the notification that can be seen with:
                    // `adb shell dumpsys notification --noredact | grep alfred2017m2`
                    //
                    // To disable:
                    // `adb shell appops set --uid com.swooby.alfred2017m2 SYSTEM_EXEMPT_FROM_DISMISSIBLE_NOTIFICATIONS default`
                    //
                    // There are some other goodies in this article that might be of some help in the future.
                    //
                    builder.addActionActivity(
                            R.drawable.ic_warning,
                            R.string.alfred_notification_action_persistent,
                            NotificationIds.ACTION_PERSISTENT,
                            PersistentNotificationDialogActivity.createIntent(mContext),
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                }
            }
            builder.addActionBroadcast(
                    R.drawable.ic_warning,
                    R.string.alfred_notification_action_quit,
                    NotificationIds.ACTION_QUIT,
                    NotificationActionReceiver.createQuitIntent(mContext),
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        }

        PendingIntent pendingIntent = status.getPendingIntent();
        if (pendingIntent != null)
        {
            builder.setContentIntent(pendingIntent)
                .addExtras(new FooBundleBuilder()
                .putBundle(EXTRA_ALFRED_EXTRAS, status.getExtras())
                .build());
        }
        else
        {
            FooLog.w(TAG, "notificationShow: Unexpected pendingIntent == null");
        }

        builder.setSmallIcon(status.getSmallIcon())
                .setSubText(status.getText())
                .setContentTitle(contentTitle);
        if (!FooString.isNullOrEmpty(contentText))
        {
            builder.setContentText(contentText);
        }

        return notificationShow(requestCode, foregroundServiceType, builder);
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private FooNotification notificationShow(int requestCode,
                                             int foregroundServiceType,
                                             @NonNull FooNotificationBuilder builder)
    {
        FooNotification notification = new FooNotification(requestCode, foregroundServiceType, builder);
        FooLog.v(TAG, "notificationShow: notification=" + notification);
        notification.show(mContext);
        return notification;
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private void notificationOngoingShow(@NonNull NotificationStatus notificationStatus, @NonNull String contentTitle, @Nullable String contentText)
    {
        mNotificationOngoing = notificationShow(NotificationIds.ONGOING, FOREGROUND_SERVICE_TYPE, notificationStatus, contentTitle, contentText);
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    void cancelOngoingNotification()
    {
        notificationOngoingCancel();
    }

    private void notificationOngoingCancel()
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

    /** @noinspection SameParameterValue*/
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    void notifyOngoingInitializing(@NonNull String statusSubText, @NonNull String contentTitle, @Nullable String contentText)
    {
        NotificationStatus notificationStatus = new NotificationStatusStarting(mContext, getString(R.string.alfred_initializing), statusSubText, null);
        notificationOngoingShow(notificationStatus, contentTitle, contentText);
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    void notifyOngoingRunning(@NonNull NotificationStatus notificationStatus, @NonNull String contentTitle, @Nullable String contentText)
    {
        notificationOngoingShow(notificationStatus, contentTitle, contentText);
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    void notifyOngoingPaused(@NonNull NotificationStatus notificationStatus, @NonNull String contentTitle, @Nullable String contentText)
    {
        notificationOngoingShow(notificationStatus, contentTitle, contentText);
    }
}

package com.swooby.alfred.notification;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.service.notification.StatusBarNotification;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RemoteViews;

import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.platform.FooPlatformUtils;
import com.swooby.alfred.MainApplication;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public abstract class AbstractNotificationParser
{
    private static final String TAG = FooLog.TAG(AbstractNotificationParser.class);

    public static String toVerboseString(Integer value)
    {
        return value == null ? "null" : Integer.toString(value) + " (0x" + Integer.toHexString(value) + ')';
    }

    @NonNull
    public static String getPackageName(
            @NonNull
            StatusBarNotification sbn)
    {
        return sbn.getPackageName();
    }

    @NonNull
    public static Notification getNotification(
            @NonNull
            StatusBarNotification sbn)
    {
        return sbn.getNotification();
    }

    public static RemoteViews getBigContentView(
            @NonNull
            StatusBarNotification sbn)
    {
        return getNotification(sbn).bigContentView;
    }

    public static RemoteViews getContentView(
            @NonNull
            StatusBarNotification sbn)
    {
        return getNotification(sbn).contentView;
    }

    @Nullable
    public static Context createPackageContext(
            @NonNull
            Context context,
            @NonNull
            RemoteViews remoteView)
    {
        String packageName = remoteView.getPackage();

        try
        {
            return context.createPackageContext(packageName, Context.CONTEXT_RESTRICTED);
        }
        catch (NameNotFoundException e)
        {
            return null;
        }
    }

    @Nullable
    public static View mockRemoteView(
            @NonNull
            Context context,
            @NonNull
            RemoteViews remoteView)
    {
        Context otherAppContext = createPackageContext(context, remoteView);
        if (otherAppContext == null)
        {
            return null;
        }

        LayoutInflater layoutInflater = LayoutInflater.from(otherAppContext);

        return layoutInflater.inflate(remoteView.getLayoutId(), null, true);
    }

    public static int getIdOfChildWithName(
            @NonNull
            View parent,
            @NonNull
            String childName)
    {
        //PbLog.e(TAG,
        //        "getIdOfChildWithName(parent=" + parent + ", childName=" + PbStringUtils.quote(childName) + ')');

        Resources resources = parent.getResources();
        String packageName = parent.getContext().getPackageName();

        return resources.getIdentifier(childName, "id", packageName);
    }

    /*
    @Nullable
    public static View findViewByName(
            @NonNull
            View parent,
            @NonNull
            String childName)
    {
        FooLog.v(TAG, "findViewByName(parent=" + parent + ", childName=" + FooString.quote(childName) + ')');

        int id = getIdOfChildWithName(parent, childName);
        if (id == 0)
        {
            return null;
        }

        return parent.findViewById(id);
    }
    */

    public interface TagTypes
    {
        /**
         * https://github.com/android/platform_frameworks_base/blob/master/core/java/android/widget/RemoteViews.java#L733
         */
        int PendingIntent          = 1;
        /**
         * https://github.com/android/platform_frameworks_base/blob/master/core/java/android/widget/RemoteViews.java#L1057
         */
        int ReflectionAction       = 2;
        /**
         * https://github.com/android/platform_frameworks_base/blob/master/core/java/android/widget/RemoteViews.java#L1050
         */
        int BitmapReflectionAction = 12;
    }

    /**
     * From:
     * https://github.com/android/platform_frameworks_base/blob/master/core/java/android/widget/RemoteViews.java#L1074
     */
    public interface ActionTypes
    {
        int BOOLEAN          = 1;
        int BYTE             = 2;
        int SHORT            = 3;
        int INT              = 4;
        int LONG             = 5;
        int FLOAT            = 6;
        int DOUBLE           = 7;
        int CHAR             = 8;
        int STRING           = 9;
        int CHAR_SEQUENCE    = 10;
        int URI              = 11;
        int BITMAP           = 12;
        int BUNDLE           = 13;
        int INTENT           = 14;
        int COLOR_STATE_LIST = 15;
        int ICON             = 16;
    }

    public static abstract class ValueTypes
    {
        public static final int TEXT               = 1;
        public static final int VISIBILITY         = 2;
        public static final int ENABLED            = 3;
        public static final int IMAGE_RESOURCE_ID  = 4;
        public static final int BITMAP_RESOURCE_ID = 5;
        public static final int PENDING_INTENT     = 6;
        /*
        public static final int ICON              = ?;
        */

        public static String toString(int value)
        {
            switch (value)
            {
                case TEXT:
                    return "TEXT(" + value + ')';
                case VISIBILITY:
                    return "VISIBILITY(" + value + ')';
                case ENABLED:
                    return "ENABLED(" + value + ')';
                case IMAGE_RESOURCE_ID:
                    return "IMAGE_RESOURCE_ID(" + value + ')';
                case BITMAP_RESOURCE_ID:
                    return "BITMAP_RESOURCE_ID(" + value + ')';
                case PENDING_INTENT:
                    return "PENDING_INTENT(" + value + ')';
                default:
                    return "UNKNOWN(" + value + ')';
            }
        }
    }

    @Nullable
    public static Object getRemoteViewValueById(
            @NonNull
            RemoteViews remoteViews, int viewId, int valueType)
    {
        //noinspection TryWithIdenticalCatches
        try
        {
            Field field = remoteViews.getClass().getDeclaredField("mActions");
            field.setAccessible(true);

            @SuppressWarnings("unchecked")
            ArrayList<Parcelable> actions = (ArrayList<Parcelable>) field.get(remoteViews);

            for (int i = 0; i < actions.size(); i++)
            {
                Parcelable parcelable = actions.get(i);

                Parcel parcel = Parcel.obtain();

                try
                {
                    parcelable.writeToParcel(parcel, 0);

                    parcel.setDataPosition(0);

                    int actionTag = parcel.readInt();
                    //FooLog.v(TAG, "getRemoteViewValueById: actionTag=" + toVerboseString(tag));
                    switch (valueType)
                    {
                        /*
                        case ValueTypes.PENDING_INTENT:
                            if (tag != TagTypes.PendingIntent)
                            {
                                continue;
                            }
                            break;
                        */
                        case ValueTypes.TEXT:
                        case ValueTypes.VISIBILITY:
                        case ValueTypes.ENABLED:
                        case ValueTypes.IMAGE_RESOURCE_ID:
                            if (actionTag != TagTypes.ReflectionAction)
                            {
                                continue;
                            }
                            break;
                        case ValueTypes.BITMAP_RESOURCE_ID:
                            if (actionTag != TagTypes.BitmapReflectionAction)
                            {
                                continue;
                            }
                            break;
                        default:
                            continue;
                    }

                    int actionViewId = parcel.readInt();
                    //FooLog.v(TAG, "getRemoteViewValueById: actionViewId=" + toVerboseString(viewId));
                    if (actionViewId != viewId)
                    {
                        continue;
                    }

                    Object value = null;

                    switch (actionTag)
                    {
                        /*
                        case TagTypes.PendingIntent:
                        {
                            if (parcel.readInt() != 0)
                            {
                                value = PendingIntent.readPendingIntentOrNullFromParcel(parcel);
                            }
                            break;
                        }
                        */
                        case TagTypes.ReflectionAction:
                        {
                            String actionMethodName = parcel.readString();
                            //FooLog.v(TAG, "getRemoteViewValueById: actionMethodName=" + FooString.quote(actionMethodName));
                            switch (valueType)
                            {
                                case ValueTypes.TEXT:
                                    if (!"setText".equals(actionMethodName))
                                    {
                                        continue;
                                    }
                                    break;
                                case ValueTypes.VISIBILITY:
                                    if (!"setVisibility".equals(actionMethodName))
                                    {
                                        continue;
                                    }
                                    break;
                                case ValueTypes.IMAGE_RESOURCE_ID:
                                    if (!"setImageResource".equals(actionMethodName))
                                    {
                                        continue;
                                    }
                                    break;
                                case ValueTypes.ENABLED:
                                    if (!"setEnabled".equals(actionMethodName))
                                    {
                                        continue;
                                    }
                                    break;
                                default:
                                    continue;
                            }

                            int actionType = parcel.readInt();
                            // per:
                            // https://github.com/android/platform_frameworks_base/blob/master/core/java/android/widget/RemoteViews.java#L1101
                            switch (actionType)
                            {
                                case ActionTypes.BOOLEAN:
                                    value = parcel.readInt() != 0;
                                    break;
                                case ActionTypes.INT:
                                    value = parcel.readInt();
                                    break;
                                case ActionTypes.CHAR_SEQUENCE:
                                    value = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel)
                                            .toString()
                                            .trim();
                                    break;
                                /*
                                case ActionTypes.INTENT:
                                    if (parcel.readInt() != 0)
                                    {
                                        value = Intent.CREATOR.createFromParcel(parcel);
                                    }
                                    break;
                                case ActionTypes.ICON:
                                    if (parcel.readInt() != 0)
                                    {
                                        value = null;//Icon.CREATOR.createFromParcel(parcel);
                                    }
                                */
                            }
                            break;
                        }
                        case TagTypes.BitmapReflectionAction:
                        {
                            String actionMethodName = parcel.readString();
                            //FooLog.v(TAG, "getRemoteViewValueById: actionMethodName=" + FooString.quote(actionMethodName));
                            switch (valueType)
                            {
                                case ValueTypes.BITMAP_RESOURCE_ID:
                                    if (!"setImageBitmap".equals(actionMethodName))
                                    {
                                        continue;
                                    }
                                    break;
                                default:
                                    continue;
                            }

                            value = parcel.readInt();

                            break;
                        }
                        default:
                            continue;
                    }

                    int parcelDataAvail = parcel.dataAvail();
                    if (parcelDataAvail > 0)
                    {
                        FooLog.w(TAG, "getRemoteViewValueById: parcel.dataAvail()=" + parcelDataAvail);
                    }

                    return value;
                }
                finally
                {
                    parcel.recycle();
                }
            }
        }
        catch (IllegalAccessException e)
        {
            FooLog.e(TAG, "getRemoteViewValueById", e);
        }
        catch (NoSuchFieldException e)
        {
            FooLog.e(TAG, "getRemoteViewValueById", e);
        }

        return null;
    }

    public static class KeyValue
            implements Comparable<KeyValue>
    {
        public final int    mKey;
        public final int    mValueType;
        public final Object mValue;

        public KeyValue(int key, int valueType, Object value)
        {
            mKey = key;
            mValueType = valueType;
            mValue = value;
        }

        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder();

            sb.append(getClass().getSimpleName()).append('@').append(Integer.toHexString(hashCode()))
                    .append("{ mKey=").append(toVerboseString(mKey))
                    .append(", mValueType=").append(ValueTypes.toString(mValueType))
                    .append(", mValue=");

            switch (mValueType)
            {
                case ValueTypes.BITMAP_RESOURCE_ID:
                case ValueTypes.IMAGE_RESOURCE_ID:
                    sb.append(toVerboseString((Integer) mValue));
                    break;
                case ValueTypes.VISIBILITY:
                    if (mValue instanceof Integer)
                    {
                        sb.append(FooPlatformUtils.viewVisibilityToString((Integer) mValue));
                        break;
                    }
                default:
                    sb.append(FooString.quote(mValue));
                    break;
            }

            sb.append(" }");

            return sb.toString();
        }

        @Override
        public int compareTo(
                @NonNull
                KeyValue another)
        {
            return Integer.compare(mKey, another.mKey);
        }
    }

    public static void walkActions(RemoteViews remoteViews, List<KeyValue> listKeyValues)
    {
        //noinspection TryWithIdenticalCatches
        try
        {
            Field field = remoteViews.getClass().getDeclaredField("mActions");
            field.setAccessible(true);

            @SuppressWarnings("unchecked")
            ArrayList<Parcelable> actions = (ArrayList<Parcelable>) field.get(remoteViews);

            for (int i = 0; i < actions.size(); i++)
            {
                Parcelable parcelable = actions.get(i);

                Parcel parcel = Parcel.obtain();

                try
                {
                    parcelable.writeToParcel(parcel, 0);

                    parcel.setDataPosition(0);

                    int actionTag = parcel.readInt();
                    //FooLog.v(TAG, "walkActions: actionTag=" + toVerboseString(actionTag));

                    int actionViewId = parcel.readInt();
                    //FooLog.v(TAG, "walkActions: actionViewId=" + toVerboseString(actionViewId));

                    int valueType;
                    Object value = null;

                    switch (actionTag)
                    {
                        case TagTypes.PendingIntent:
                        {
                            valueType = ValueTypes.PENDING_INTENT;
                            if (parcel.readInt() != 0)
                            {
                                value = PendingIntent.readPendingIntentOrNullFromParcel(parcel);
                            }
                            break;
                        }
                        case TagTypes.ReflectionAction:
                        {
                            String actionMethodName = parcel.readString();
                            //FooLog.v(TAG, "walkActions: actionMethodName=" + FooString.quote(actionMethodName));
                            switch (actionMethodName)
                            {
                                case "setText":
                                    valueType = ValueTypes.TEXT;
                                    break;
                                case "setVisibility":
                                    valueType = ValueTypes.VISIBILITY;
                                    break;
                                case "setImageResource":
                                    valueType = ValueTypes.IMAGE_RESOURCE_ID;
                                    break;
                                case "setEnabled":
                                    valueType = ValueTypes.ENABLED;
                                    break;
                                default:
                                    valueType = -1;
                                    break;
                            }

                            int actionType = parcel.readInt();
                            // per:
                            // https://github.com/android/platform_frameworks_base/blob/master/core/java/android/widget/RemoteViews.java#L1101
                            switch (actionType)
                            {
                                case ActionTypes.BOOLEAN:
                                    value = parcel.readInt() != 0;
                                    break;
                                case ActionTypes.INT:
                                    value = parcel.readInt();
                                    break;
                                case ActionTypes.CHAR_SEQUENCE:
                                    value = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel)
                                            .toString()
                                            .trim();
                                    break;
                                /*
                                case ActionTypes.INTENT:
                                    if (parcel.readInt() != 0)
                                    {
                                        value = Intent.CREATOR.createFromParcel(parcel);
                                    }
                                    break;
                                case ActionTypes.ICON:
                                    if (parcel.readInt() != 0)
                                    {
                                        value = null;//Icon.CREATOR.createFromParcel(parcel);
                                    }
                                */
                            }
                            break;
                        }
                        case TagTypes.BitmapReflectionAction:
                        {
                            String actionMethodName = parcel.readString();
                            //FooLog.v(TAG, "walkActions: actionMethodName=" + FooString.quote(actionMethodName));

                            valueType = ValueTypes.BITMAP_RESOURCE_ID;

                            value = parcel.readInt();

                            break;
                        }
                        default:
                            continue;
                    }

                    int parcelDataAvail = parcel.dataAvail();
                    if (parcelDataAvail > 0)
                    {
                        FooLog.w(TAG, "walkActions: parcel.dataAvail()=" + parcelDataAvail);
                    }

                    //FooLog.w(TAG, "walkActions: actionViewId=" + toVerboseString(actionViewId) +
                    //              ", value=" + value);

                    if (listKeyValues != null)
                    {
                        listKeyValues.add(new KeyValue(actionViewId, valueType, value));
                    }
                }
                finally
                {
                    parcel.recycle();
                }
            }
        }
        catch (IllegalAccessException e)
        {
            FooLog.e(TAG, "walkActions", e);
        }
        catch (NoSuchFieldException e)
        {
            FooLog.e(TAG, "walkActions", e);
        }
    }

    public static void walkView(View view, Set<Integer> viewIds)
    {
        FooLog.v(TAG, "walkView: view=" + view);

        if (view != null)
        {
            if (viewIds != null)
            {
                viewIds.add(view.getId());
            }

            if (view instanceof ViewGroup)
            {
                ViewGroup viewGroup = (ViewGroup) view;
                int childCount = viewGroup.getChildCount();
                for (int i = 0; i < childCount; i++)
                {
                    View childView = viewGroup.getChildAt(i);
                    walkView(childView, viewIds);
                }
            }
        }
    }

    @NonNull
    public static String unknownIfNullOrEmpty(String value)
    {
        if (FooString.isNullOrEmpty(value))
        {
            value = "Unknown";
        }
        return value;
    }

    public static boolean defaultOnNotificationPosted(
            @NonNull
            MainApplication mainApplication,
            @NonNull
            StatusBarNotification sbn,
            String packageAppSpokenName)
    {
        Notification notification = getNotification(sbn);
        CharSequence tickerText = notification.tickerText;
        if (!FooString.isNullOrEmpty(tickerText))
        {
            String title = FooString.isNullOrEmpty(packageAppSpokenName) ? getPackageName(sbn) : packageAppSpokenName;
            mainApplication.speak(title);
            mainApplication.silence(500);
            mainApplication.speak(tickerText.toString());

            return true;
        }

        return false;
    }

    protected final MainApplication mApplication;
    protected final Resources       mResources;
    protected final String          mPackageName;
    protected final String          mPackageAppSpokenName;

    protected AbstractNotificationParser(
            @NonNull
            MainApplication application,
            @NonNull
            String packageName,
            @NonNull
            String packageAppSpokenName)
    {
        mApplication = application;
        mResources = application.getResources();
        mPackageName = packageName;
        mPackageAppSpokenName = packageAppSpokenName;
    }

    public String getPackageName()
    {
        return mPackageName;
    }

    public boolean onNotificationPosted(StatusBarNotification sbn)
    {
        //String groupKey = sbn.getGroupKey();
        //String key = sbn.getKey();
        //UserHandle user = sbn.getUser();
        //String packageName = sbn.getPackageName();
        //long postTime = sbn.getPostTime();

        Notification notification = sbn.getNotification();
        FooLog.v(TAG, "onNotificationPosted: notification=" + notification);

        //int id = sbn.getId();
        //String tag = sbn.getTag();

        //CharSequence tickerText = notification.tickerText;

        // TODO:(pv) Seriously, introspect and walk all StatusBarNotification fields, especially:
        //  Notification.tickerText
        //  All ImageView Resource Ids and TextView Texts in BigContentView
        //  All ImageView Resource Ids and TextView Texts in ContentView

        RemoteViews bigContentView = notification.bigContentView;
        View mockBigContentView = mockRemoteView(mApplication, bigContentView);
        FooLog.v(TAG, "onNotificationPosted: bigContentView");
        Set<Integer> bigContentViewIds = new LinkedHashSet<>();
        walkView(mockBigContentView, bigContentViewIds);
        FooLog.v(TAG, "onNotificationPosted: --------");
        RemoteViews contentView = notification.contentView;
        View mockContentView = mockRemoteView(mApplication, contentView);
        FooLog.v(TAG, "onNotificationPosted: contentView");
        Set<Integer> contentViewIds = new LinkedHashSet<>();
        walkView(mockContentView, contentViewIds);

        //RemoteViews headUpContentView = notification.headsUpContentView;

        //Notification.Action[] actions = notification.actions;

        //String category = notification.category;

        Bundle extras = notification.extras;
        FooLog.v(TAG, "onNotificationPosted: extras=" + FooPlatformUtils.toString(extras));

        return defaultOnNotificationPosted(mApplication, sbn, mPackageAppSpokenName);
    }

    public void onNotificationRemoved(StatusBarNotification sbn)
    {
    }
}

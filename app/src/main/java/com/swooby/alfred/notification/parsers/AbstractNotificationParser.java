package com.swooby.alfred.notification.parsers;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.service.notification.StatusBarNotification;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.platform.FooPlatformUtils;
import com.smartfoo.android.core.texttospeech.FooTextToSpeechBuilder;
import com.smartfoo.android.core.view.FooViewUtils;
import com.swooby.alfred.MainApplication;
import com.swooby.alfred.R;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public abstract class AbstractNotificationParser
{
    private static final String TAG = FooLog.TAG(AbstractNotificationParser.class);

    @NonNull
    public static String toVerboseString(Integer value)
    {
        return value == null ? "null" : Integer.toString(value) + "(0x" + Integer.toHexString(value) + ')';
    }

    @Nullable
    public static String getPackageName(StatusBarNotification sbn)
    {
        return sbn != null ? sbn.getPackageName() : null;
    }

    @Nullable
    public static Notification getNotification(StatusBarNotification sbn)
    {
        return sbn != null ? sbn.getNotification() : null;
    }

    @Nullable
    public static RemoteViews getBigContentView(StatusBarNotification sbn)
    {
        Notification notification = getNotification(sbn);
        return notification != null ? notification.bigContentView : null;
    }

    @Nullable
    public static Bundle getExtras(StatusBarNotification sbn)
    {
        Notification notification = getNotification(sbn);
        return notification != null ? notification.extras : null;
    }

    @Nullable
    public static RemoteViews getContentView(StatusBarNotification sbn)
    {
        Notification notification = getNotification(sbn);
        return notification != null ? notification.contentView : null;
    }

    @Nullable
    public static Context createPackageContext(Context context, RemoteViews remoteView)
    {
        if (context == null)
        {
            throw new IllegalArgumentException("context must not be null");
        }

        if (remoteView == null)
        {
            return null;
        }

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
    public static View inflateRemoteView(Context context, RemoteViews remoteViews)
    {
        if (context == null)
        {
            throw new IllegalArgumentException("context must not be null");
        }

        Context remoteContext = createPackageContext(context, remoteViews);
        if (remoteContext == null)
        {
            return null;
        }

        return remoteViews.apply(remoteContext, new RelativeLayout(remoteContext));
    }

    /*
    @Nullable
    public static View mockRemoteView(Context context, RemoteViews remoteView)
    {
        Context otherAppContext = createPackageContext(context, remoteView);
        if (otherAppContext == null)
        {
            return null;
        }

        LayoutInflater layoutInflater = LayoutInflater.from(otherAppContext);

        return layoutInflater.inflate(remoteView.getLayoutId(), null, true);
    }
    */

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

    public enum ResourceType
    {
        drawable,
    }

    public static int getIdentifier(
            @NonNull
            Context context,
            @NonNull
            ResourceType resourceType,
            @NonNull
            String name)
    {
        Resources resources = context.getResources();
        String packageName = context.getPackageName();
        return resources.getIdentifier(name, resourceType.name(), packageName);
    }

    public static int getImageResource(
            @NonNull
            ImageView imageView)
    {
        //noinspection TryWithIdenticalCatches
        try
        {
            Field field = imageView.getClass().getDeclaredField("mResource");
            field.setAccessible(true);
            return (int) field.get(imageView);
        }
        catch (NoSuchFieldException e)
        {
            FooLog.e(TAG, "getImageResource", e);
        }
        catch (IllegalAccessException e)
        {
            FooLog.e(TAG, "getImageResource", e);
        }

        return 0;
    }

    public static BitmapDrawable getImageBitmap(
            @NonNull
            ImageView imageView)
    {
        //noinspection TryWithIdenticalCatches
        try
        {
            Field field = imageView.getClass().getDeclaredField("mRecycleableBitmapDrawable");
            field.setAccessible(true);
            return (BitmapDrawable) field.get(imageView);
        }
        catch (NoSuchFieldException e)
        {
            FooLog.e(TAG, "getImageBitmap", e);
        }
        catch (IllegalAccessException e)
        {
            FooLog.e(TAG, "getImageBitmap", e);
        }

        return null;
    }

    /*
    public static BitmapDrawable getImageBitmap(
            @NonNull
            RemoteViews remoteViews,
            BitmapDrawable )
    {
        //noinspection TryWithIdenticalCatches
        try
        {
            Field field = imageView.getClass().getDeclaredField("mRecycleableBitmapDrawable");
            field.setAccessible(true);
            return (BitmapDrawable) field.get(imageView);
        }
        catch (NoSuchFieldException e)
        {
            FooLog.e(TAG, "getImageBitmap", e);
        }
        catch (IllegalAccessException e)
        {
            FooLog.e(TAG, "getImageBitmap", e);
        }

        return null;
    }
    */

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
        int PendingIntent               = 1;
        /**
         * https://github.com/android/platform_frameworks_base/blob/master/core/java/android/widget/RemoteViews.java#L1057
         */
        int ReflectionAction            = 2;
        /**
         * https://github.com/android/platform_frameworks_base/blob/master/core/java/android/widget/RemoteViews.java#L437
         */
        int SetOnClickFillInIntent      = 9;
        /**
         * https://github.com/android/platform_frameworks_base/blob/master/core/java/android/widget/RemoteViews.java#L655
         */
        int SetRemoteViewsAdapterIntent = 10;
        /**
         * https://github.com/android/platform_frameworks_base/blob/master/core/java/android/widget/RemoteViews.java#L1050
         */
        int BitmapReflectionAction      = 12;
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
        public static final int INTENT             = 7;
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
            RemoteViews remoteViews, int viewId, int valueType)
    {
        if (remoteViews == null)
        {
            return null;
        }

        //noinspection TryWithIdenticalCatches
        try
        {
            Class cls = remoteViews.getClass();
            while (cls != RemoteViews.class)
            {
                cls = cls.getSuperclass();
            }

            Field field = cls.getDeclaredField("mActions");
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
                    FooLog.v(TAG, "getRemoteViewValueById: actionTag=" + toVerboseString(actionTag));
                    switch (valueType)
                    {
                        case ValueTypes.PENDING_INTENT:
                            switch (actionTag)
                            {
                                case TagTypes.PendingIntent:
                                    break;
                                default:
                                    continue;
                            }
                            break;
                        case ValueTypes.TEXT:
                        case ValueTypes.VISIBILITY:
                        case ValueTypes.ENABLED:
                        case ValueTypes.IMAGE_RESOURCE_ID:
                            switch (actionTag)
                            {
                                case TagTypes.ReflectionAction:
                                    break;
                                default:
                                    continue;
                            }
                            break;
                        case ValueTypes.INTENT:
                            switch (actionTag)
                            {
                                case TagTypes.SetOnClickFillInIntent:
                                case TagTypes.SetRemoteViewsAdapterIntent:
                                    break;
                                default:
                                    continue;
                            }
                            break;
                        case ValueTypes.BITMAP_RESOURCE_ID:
                            switch (actionTag)
                            {
                                case TagTypes.BitmapReflectionAction:
                                    break;
                                default:
                                    continue;
                            }
                            break;
                        default:
                            continue;
                    }

                    int actionViewId = parcel.readInt();
                    FooLog.v(TAG, "getRemoteViewValueById: actionViewId=" + toVerboseString(actionViewId));
                    if (actionViewId != viewId)
                    {
                        continue;
                    }

                    Object value = null;

                    switch (actionTag)
                    {
                        case TagTypes.PendingIntent:
                        {
                            if (parcel.readInt() != 0)
                            {
                                value = PendingIntent.readPendingIntentOrNullFromParcel(parcel);
                            }
                            break;
                        }
                        case TagTypes.ReflectionAction:
                        {
                            String actionMethodName = parcel.readString();
                            FooLog.v(TAG,
                                    "getRemoteViewValueById: actionMethodName=" + FooString.quote(actionMethodName));
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
                                        value = Icon.CREATOR.createFromParcel(parcel);
                                    }
                                    */
                            }
                            break;
                        }
                        case TagTypes.SetOnClickFillInIntent:
                        case TagTypes.SetRemoteViewsAdapterIntent:
                        {
                            if (parcel.readInt() != 0)
                            {
                                value = Intent.CREATOR.createFromParcel(parcel);
                            }
                            break;
                        }
                        case TagTypes.BitmapReflectionAction:
                        {
                            String actionMethodName = parcel.readString();
                            FooLog.v(TAG,
                                    "getRemoteViewValueById: actionMethodName=" + FooString.quote(actionMethodName));
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

    /*
    public static Object getViewValueById(View parent, int viewId, int valueType)
    {
        Object value = null;

        View view = parent.findViewById(viewId);
        if (view != null)
        {
            switch (valueType)
            {
                case ValueTypes.TEXT:
                    if (view instanceof TextView)
                    {
                        value = ((TextView) view).getText();
                    }
                    break;
            }
        }

        return value;
    }
    */

    public static class KeyValue
            implements Comparable<KeyValue>
    {
        public final int    mViewId;
        public final int    mValueType;
        public final Object mValue;

        public KeyValue(int viewId, int valueType, Object value)
        {
            mViewId = viewId;
            mValueType = valueType;
            mValue = value;
        }

        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder();

            sb.append("{ mViewId=").append(toVerboseString(mViewId))
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
                        sb.append(FooViewUtils.viewVisibilityToString((Integer) mValue));
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
            return Integer.compare(mViewId, another.mViewId);
        }
    }

    public static void walkActions(RemoteViews remoteViews, List<KeyValue> listKeyValues)
    {
        if (remoteViews == null)
        {
            return;
        }

        //noinspection TryWithIdenticalCatches
        try
        {
            Class cls = remoteViews.getClass();
            while (cls != RemoteViews.class)
            {
                cls = cls.getSuperclass();
            }

            Field field = cls.getDeclaredField("mActions");
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
                                    value = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel);
                                    break;
                                case ActionTypes.INTENT:
                                    if (parcel.readInt() != 0)
                                    {
                                        value = Intent.CREATOR.createFromParcel(parcel);
                                    }
                                    break;
                                case ActionTypes.ICON:
                                    if (parcel.readInt() != 0)
                                    {
                                        value = Icon.CREATOR.createFromParcel(parcel);
                                    }
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

    public interface WalkViewCallbacks
    {
        void onTextView(TextView textView);
    }

    public static void walkView(View view, Set<Integer> viewIds)
    {
        walkView(view, viewIds, null);
    }

    public static void walkView(View view, Set<Integer> viewIds, WalkViewCallbacks callbacks)
    {
        FooLog.v(TAG, "walkView: view=" + view);

        if (view != null)
        {
            if (viewIds != null)
            {
                viewIds.add(view.getId());
            }

            if (callbacks != null)
            {
                if (view instanceof TextView)
                {
                    callbacks.onTextView((TextView) view);
                }
            }

            if (view instanceof ViewGroup)
            {
                ViewGroup viewGroup = (ViewGroup) view;
                int childCount = viewGroup.getChildCount();
                for (int i = 0; i < childCount; i++)
                {
                    View childView = viewGroup.getChildAt(i);
                    walkView(childView, viewIds, callbacks);
                }
            }
        }
    }

    @NonNull
    public static String unknownIfNullOrEmpty(Context context, CharSequence value)
    {
        return unknownIfNullOrEmpty(context, value != null ? value.toString() : null);
    }

    @NonNull
    public static String unknownIfNullOrEmpty(Context context, String value)
    {
        if (FooString.isNullOrEmpty(value))
        {
            value = context.getString(R.string.unknown);
        }
        return value;
    }

    public static NotificationParseResult defaultOnNotificationPosted(
            boolean speak,
            MainApplication mainApplication,
            StatusBarNotification sbn)
    {
        return defaultOnNotificationPosted(speak, mainApplication, sbn, null);
    }

    public static NotificationParseResult defaultOnNotificationPosted(
            boolean speak,
            MainApplication mainApplication,
            StatusBarNotification sbn,
            String packageAppSpokenName)
    {
        if (mainApplication == null)
        {
            throw new IllegalArgumentException("mainApplication must not be null");
        }

        if (sbn == null)
        {
            throw new IllegalArgumentException("sbn must not be null");
        }

        String packageName = getPackageName(sbn);
        FooLog.v(TAG, "onNotificationPosted: packageName=" + FooString.quote(packageName));
        if (FooString.isNullOrEmpty(packageName))
        {
            throw new IllegalStateException("sbn.getPackageName() returned null");
        }

        if (FooString.isNullOrEmpty(packageAppSpokenName))
        {
            packageAppSpokenName = FooPlatformUtils.getApplicationName(mainApplication, packageName);
        }
        FooLog.v(TAG, "onNotificationPosted: packageAppSpokenName=" + FooString.quote(packageAppSpokenName));
        if (FooString.isNullOrEmpty(packageAppSpokenName))
        {
            throw new IllegalStateException("FooPlatformUtils.getApplicationName(...) returned null");
        }

        //String groupKey = sbn.getGroupKey();
        //String key = sbn.getKey();
        //UserHandle user = sbn.getUser();
        //long postTime = sbn.getPostTime();
        //int id = sbn.getId();
        //String tag = sbn.getTag();

        Notification notification = getNotification(sbn);
        FooLog.v(TAG, "onNotificationPosted: notification=" + notification);
        if (notification == null)
        {
            FooLog.v(TAG, "onNotificationPosted: notification == null; Unparsable");
            return NotificationParseResult.Unparsable;
        }

        Bundle extras = notification.extras;
        FooLog.v(TAG, "onNotificationPosted: extras=" + FooPlatformUtils.toString(extras));

        CharSequence tickerText = notification.tickerText;
        FooLog.v(TAG, "onNotificationPosted: tickerText=" + FooString.quote(tickerText));

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

        FooLog.v(TAG, "onNotificationPosted: ---- bigContentView ----");
        RemoteViews bigContentView = notification.bigContentView;
        View inflatedBigContentView = inflateRemoteView(mainApplication, bigContentView);
        walkView(inflatedBigContentView, null, walkViewCallbacks);
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

        FooLog.v(TAG, "onNotificationPosted: ---- contentView ----");
        RemoteViews contentView = notification.contentView;
        View inflatedContentView = inflateRemoteView(mainApplication, contentView);
        walkView(inflatedContentView, null, bigContentView != null ? null : walkViewCallbacks);
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

        if (!speak)
        {
            return NotificationParseResult.ParsableIgnored;
        }

        if (builder.getNumberOfParts() == 1)
        {
            builder.appendSilence(500)
                    .appendSpeech(tickerText.toString());
        }

        mainApplication.speak(builder);

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

    protected final MainApplication mApplication;
    protected final Resources       mResources;
    protected final String          mPackageName;
    protected final String          mPackageAppSpokenName;

    protected AbstractNotificationParser(
            MainApplication application,
            String packageName)
    {
        this(application, packageName, FooPlatformUtils.getApplicationName(application, packageName));
    }

    protected AbstractNotificationParser(
            MainApplication application,
            String packageName,
            String packageAppSpokenName)
    {
        if (application == null)
        {
            throw new IllegalArgumentException("application must not be null");
        }

        if (FooString.isNullOrEmpty(packageName))
        {
            throw new IllegalArgumentException("packageName must not be null/empty");
        }

        if (FooString.isNullOrEmpty(packageAppSpokenName))
        {
            throw new IllegalArgumentException("packageAppSpokenName must not be null/empty");
        }

        mApplication = application;
        mResources = application.getResources();
        mPackageName = packageName;
        mPackageAppSpokenName = packageAppSpokenName;
    }

    public String getPackageName()
    {
        return mPackageName;
    }

    protected boolean getSpeakDefaultNotification()
    {
        return false;
    }

    public NotificationParseResult onNotificationPosted(StatusBarNotification sbn)
    {
        return defaultOnNotificationPosted(getSpeakDefaultNotification(), mApplication, sbn, mPackageAppSpokenName);
    }

    public void onNotificationRemoved(StatusBarNotification sbn)
    {
    }
}

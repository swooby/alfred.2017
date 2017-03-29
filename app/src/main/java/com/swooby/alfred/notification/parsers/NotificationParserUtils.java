package com.swooby.alfred.notification.parsers;

import android.app.Notification;
import android.app.Notification.Action;
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
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.smartfoo.android.core.FooRun;
import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.view.FooViewUtils;
import com.swooby.alfred.R;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

public class NotificationParserUtils
{
    private static final String TAG = FooLog.TAG(NotificationParserUtils.class);

    private NotificationParserUtils()
    {
    }

    @NonNull
    public static String toVerboseString(Integer value)
    {
        return value == null ? "null" : Integer.toString(value) + "(0x" + Integer.toHexString(value) + ')';
    }

    public static String getPackageName(StatusBarNotification sbn)
    {
        return sbn != null ? sbn.getPackageName() : null;
    }

    public static Bundle getExtras(StatusBarNotification sbn)
    {
        Notification notification = getNotification(sbn);
        return notification != null ? notification.extras : null;
    }

    public static Notification getNotification(StatusBarNotification sbn)
    {
        return sbn != null ? sbn.getNotification() : null;
    }

    public static Action[] getActions(StatusBarNotification sbn)
    {
        return getActions(getNotification(sbn));
    }

    public static CharSequence getAndroidTitle(Bundle extras)
    {
        return extras != null ? extras.getCharSequence(Notification.EXTRA_TITLE) : null;
    }

    public static CharSequence getAndroidText(Bundle extras)
    {
        return extras != null ? extras.getCharSequence(Notification.EXTRA_TEXT) : null;
    }

    public static Action[] getActions(Notification notification)
    {
        return notification != null ? notification.actions : null;
    }

    public static int[] getCompactActions(Bundle extras)
    {
        return extras != null ? extras.getIntArray(Notification.EXTRA_COMPACT_ACTIONS) : null;
    }

    /**
     * @param sbn
     * @return "As of N, this field may be null" :(
     */
    public static RemoteViews getBigContentRemoteViews(StatusBarNotification sbn)
    {
        Notification notification = getNotification(sbn);
        //noinspection deprecation
        return notification != null ? notification.bigContentView : null;
    }

    /**
     * @param sbn
     * @return "As of N, this field may be null" :(
     */
    public static RemoteViews getContentRemoteViews(StatusBarNotification sbn)
    {
        Notification notification = getNotification(sbn);
        //noinspection deprecation
        return notification != null ? notification.contentView : null;
    }

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

    public static void logResourceInfo(@NonNull Context context, int resId)
    {
        Resources resources = context.getResources();
        String resourceName = resources.getResourceName(resId);
        FooLog.e(TAG, "logResourceInfo: resourceName == " + FooString.quote(resourceName));
        String resourcePackageName = resources.getResourcePackageName(resId);
        FooLog.e(TAG, "logResourceInfo: resourcePackageName == " + FooString.quote(resourcePackageName));
        String resourceEntryName = resources.getResourceEntryName(resId);
        FooLog.e(TAG, "logResourceInfo: resourceEntryName == " + FooString.quote(resourceEntryName));
        String resourceTypeName = resources.getResourceTypeName(resId);
        FooLog.e(TAG, "logResourceInfo: resourceTypeName == " + FooString.quote(resourceTypeName));
    }

    public enum ResourceType
    {
        drawable,
        id,
        string,
    }

    public static int getPackageIdOfChildWithName(@NonNull View parent, @NonNull String childName)
    {
        //FooLog.e(TAG, "getPackageIdOfChildWithName(parent=" + parent + ", childName=" + FooString.quote(childName) + ')');
        return getIdentifier(parent.getContext(), ResourceType.id, childName);
    }

    public static int getAndroidIdOfChildWithName(@NonNull View parent, @NonNull String childName)
    {
        //FooLog.e(TAG, "getAndroidIdOfChildWithName(parent=" + parent + ", childName=" + FooString.quote(childName) + ')');
        Resources resources = parent.getResources();
        return getIdentifier(resources, "android", ResourceType.id, childName);
    }

    private static int getIdOfChildWithName(@NonNull Resources resources,
                                            @NonNull String packageName,
                                            @NonNull String childName)
    {
        return getIdentifier(resources, childName, ResourceType.id, packageName);
    }

    public static int getIdentifier(@NonNull Context context, @NonNull ResourceType resourceType, @NonNull String name)
    {
        Resources resources = context.getResources();
        String packageName = context.getPackageName();
        return getIdentifier(resources, packageName, resourceType, name);
    }

    public static int getIdentifier(@NonNull Resources resources, @NonNull String packageName, @NonNull ResourceType resourceType, @NonNull String name)
    {
        return resources.getIdentifier(name, resourceType.name(), packageName);
    }

    public static int getImageResource(@NonNull ImageView imageView)
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

    public static BitmapDrawable getImageBitmap(@NonNull ImageView imageView)
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
    public static BitmapDrawable getImageBitmap(@NonNull RemoteViews remoteViews, BitmapDrawable )
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
    public static View findViewByName(@NonNull View parent, @NonNull String childName)
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

    public enum ActionValueType
    {
        UNKNOWN,
        TEXT,
        VISIBILITY,
        ENABLED,
        IMAGE_RESOURCE_ID,
        BITMAP_RESOURCE_ID,
        PENDING_INTENT,
        INTENT,
        //ICON,
    }

    public static Object getRemoteViewValueById(
            RemoteViews remoteViews,
            int viewId,
            ActionValueType valueType)
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
                        case PENDING_INTENT:
                            switch (actionTag)
                            {
                                case TagTypes.PendingIntent:
                                    break;
                                default:
                                    continue;
                            }
                            break;
                        case TEXT:
                        case VISIBILITY:
                        case ENABLED:
                        case IMAGE_RESOURCE_ID:
                            switch (actionTag)
                            {
                                case TagTypes.ReflectionAction:
                                    break;
                                default:
                                    continue;
                            }
                            break;
                        case INTENT:
                            switch (actionTag)
                            {
                                case TagTypes.SetOnClickFillInIntent:
                                case TagTypes.SetRemoteViewsAdapterIntent:
                                    break;
                                default:
                                    continue;
                            }
                            break;
                        case BITMAP_RESOURCE_ID:
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
                                case TEXT:
                                    if (!"setText".equals(actionMethodName))
                                    {
                                        continue;
                                    }
                                    break;
                                case VISIBILITY:
                                    if (!"setVisibility".equals(actionMethodName))
                                    {
                                        continue;
                                    }
                                    break;
                                case IMAGE_RESOURCE_ID:
                                    if (!"setImageResource".equals(actionMethodName))
                                    {
                                        continue;
                                    }
                                    break;
                                case ENABLED:
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
                                case BITMAP_RESOURCE_ID:
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

    public static class ActionInfo
            implements Comparable<ActionInfo>
    {
        public final int             mViewId;
        public final ActionValueType mValueType;
        public final Object          mValue;

        public ActionInfo(int viewId, ActionValueType valueType, Object value)
        {
            mViewId = viewId;
            mValueType = valueType;
            mValue = value;
        }

        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder();

            sb.append("{ mViewId=0x").append(Integer.toHexString(mViewId))
                    .append(", mValueType=").append(mValueType)
                    .append(", mValue=");

            switch (mValueType)
            {
                case BITMAP_RESOURCE_ID:
                case IMAGE_RESOURCE_ID:
                    sb.append("0x").append(Integer.toHexString((Integer) mValue));
                    break;
                case VISIBILITY:
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
        public int compareTo(@NonNull ActionInfo another)
        {
            return Integer.compare(mViewId, another.mViewId);
        }
    }

    public static class ActionInfos
    {
        private final Map<Integer, Map<ActionValueType, ActionInfo>> mViewIdToActionValueTypeToActionInfo;

        public ActionInfos()
        {
            mViewIdToActionValueTypeToActionInfo = new LinkedHashMap<>();
        }

        public void add(ActionInfo actionInfo)
        {
            int viewId = actionInfo.mViewId;

            Map<ActionValueType, ActionInfo> actionInfos = mViewIdToActionValueTypeToActionInfo.get(viewId);
            if (actionInfos == null)
            {
                actionInfos = new LinkedHashMap<>();
                mViewIdToActionValueTypeToActionInfo.put(viewId, actionInfos);
            }

            actionInfos.put(actionInfo.mValueType, actionInfo);
        }

        public Map<ActionValueType, ActionInfo> get(int viewId)
        {
            return mViewIdToActionValueTypeToActionInfo.get(viewId);
        }
    }

    public static void walkActions(RemoteViews remoteViews, ActionInfos actionInfos)
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

                    ActionValueType actionValueType;
                    Object value = null;

                    switch (actionTag)
                    {
                        case TagTypes.PendingIntent:
                        {
                            actionValueType = ActionValueType.PENDING_INTENT;
                            if (parcel.readInt() != 0)
                            {
                                value = PendingIntent.readPendingIntentOrNullFromParcel(parcel);
                            }
                            break;
                        }
                        case TagTypes.ReflectionAction:
                        {
                            String actionMethodName = parcel.readString();
                            //FooLog.e(TAG, "walkActions: actionMethodName=" + FooString.quote(actionMethodName));
                            switch (actionMethodName)
                            {
                                case "setText":
                                    actionValueType = ActionValueType.TEXT;
                                    break;
                                case "setVisibility":
                                    actionValueType = ActionValueType.VISIBILITY;
                                    break;
                                case "setImageResource":
                                    actionValueType = ActionValueType.IMAGE_RESOURCE_ID;
                                    break;
                                case "setEnabled":
                                    actionValueType = ActionValueType.ENABLED;
                                    break;
                                default:
                                    actionValueType = ActionValueType.UNKNOWN;
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

                            actionValueType = ActionValueType.BITMAP_RESOURCE_ID;

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

                    ActionInfo actionInfo = new ActionInfo(actionViewId, actionValueType, value);
                    //FooLog.i(TAG, "walkActions: actionInfo=" + actionInfo);

                    if (actionInfos != null)
                    {
                        actionInfos.add(actionInfo);
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

    interface WalkViewCallbacks
    {
        void onTextView(TextView textView);
    }

    static class ViewWrapper
    {
        public final View   mView;
        public final String mViewEntryName;

        public ViewWrapper(@NonNull View view)
        {
            mView = view;

            int viewId = mView.getId();

            Resources res = view.getResources();
            /*
            switch (mId & 0xff000000)
            {
                /*
                case 0x7f000000:
                    mPackageName = "app";
                    break;
                case 0x01000000:
                    mPackageName = "android";
                    break;
                    * /
                default:
                    mPackageName = res.getResourcePackageName(mId);
                    break;
            }
            mTypeName = res.getResourceTypeName(mId);
            */
            mViewEntryName = res.getResourceEntryName(viewId);
        }

        public int getViewId()
        {
            return mView.getId();
        }

        @Override
        public String toString()
        {
            return mView.toString();
        }
    }

    static class ViewWrappers
    {
        private final Map<String, ViewWrapper>  mViewEntryNameToViewInfo;
        private final Map<Integer, ViewWrapper> mViewIdToViewInfo;

        public ViewWrappers()
        {
            mViewEntryNameToViewInfo = new LinkedHashMap<>();
            mViewIdToViewInfo = new LinkedHashMap<>();
        }

        public void add(@NonNull View view)
        {
            ViewWrapper viewWrapper = new ViewWrapper(view);
            mViewEntryNameToViewInfo.put(viewWrapper.mViewEntryName, viewWrapper);
            mViewIdToViewInfo.put(viewWrapper.getViewId(), viewWrapper);
        }

        public ViewWrapper get(@NonNull String name)
        {
            return mViewEntryNameToViewInfo.get(name);
        }

        public ViewWrapper get(int id)
        {
            return mViewIdToViewInfo.get(id);
        }

        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            for (Iterator<Entry<String, ViewWrapper>> iterator = mViewEntryNameToViewInfo.entrySet().iterator();
                 iterator.hasNext(); )
            {
                Entry<String, ViewWrapper> entry = iterator.next();
                sb.append(entry.getKey()).append(':').append(entry.getValue());
                if (iterator.hasNext())
                {
                    sb.append(", ");
                }
            }
            sb.append(" }");
            return sb.toString();
        }
    }

    public static void walkView(View view, ViewWrappers viewWrappers, boolean visibleOnly)
    {
        walkView(0, view, viewWrappers, visibleOnly, null);
    }

    public static void walkView(View view, ViewWrappers viewWrappers, boolean visibleOnly, WalkViewCallbacks callbacks)
    {
        walkView(0, view, viewWrappers, visibleOnly, callbacks);
    }

    private static void walkView(int depth, View view, ViewWrappers viewWrappers, boolean visibleOnly, WalkViewCallbacks callbacks)
    {
        if (view == null)
        {
            return;
        }

        if (visibleOnly && view.getVisibility() != View.VISIBLE)
        {
            return;
        }

        StringBuilder indent = new StringBuilder();
        for (int i = 0; i < depth; i++)
        {
            indent.append(' ');
        }
        FooLog.v(TAG, "walkView: " + indent + "view=" + view);

        if (viewWrappers != null)
        {
            viewWrappers.add(view);
        }

        if (callbacks != null)
        {
            if (view instanceof TextView &&
                !(view instanceof Button))
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
                walkView(depth + 1, childView, viewWrappers, visibleOnly, callbacks);
            }
        }
    }

    @NonNull
    public static String unknownIfNullOrEmpty(@NonNull Context context, CharSequence value)
    {
        return unknownIfNullOrEmpty(context, value != null ? value.toString() : null);
    }

    @NonNull
    public static String unknownIfNullOrEmpty(@NonNull Context context, String value)
    {
        if (FooString.isNullOrEmpty(value))
        {
            FooRun.throwIllegalArgumentExceptionIfNull(context, "context");
            value = context.getString(R.string.alfred_unknown);
        }
        return value;
    }
}

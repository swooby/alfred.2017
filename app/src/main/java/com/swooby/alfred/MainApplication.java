package com.swooby.alfred;

import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;

import com.smartfoo.android.core.FooRun;
import com.smartfoo.android.core.app.FooDebugApplication;
import com.smartfoo.android.core.app.FooDebugConfiguration;
import com.smartfoo.android.core.logging.FooLog;

public class MainApplication
        extends Application
        implements FooDebugApplication
{
    private static final String TAG = FooLog.TAG(MainApplication.class);

    public static MainApplication getMainApplication(@NonNull Context context)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(context, "context");
        return (MainApplication) context.getApplicationContext();
    }

    private DebugPreferences mDebugPreferences;
    private AlfredManager    mAlfredManager;

    public AlfredManager getAlfredManager()
    {
        return mAlfredManager;
    }

    @Override
    public FooDebugConfiguration getFooDebugConfiguration()
    {
        return mDebugPreferences;
    }

    @Override
    public void onCreate()
    {
        FooLog.v(TAG, "+onCreate()");
        super.onCreate();
        mDebugPreferences = new DebugPreferences(this);
        mAlfredManager = new AlfredManager(this);
        mAlfredManager.start();
        FooLog.v(TAG, "-onCreate()");
    }
}

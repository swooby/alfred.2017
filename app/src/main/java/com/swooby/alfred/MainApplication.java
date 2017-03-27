package com.swooby.alfred;

import android.app.Application;

import com.smartfoo.android.core.logging.FooLog;

public class MainApplication
        extends Application
{
    private static final String TAG = FooLog.TAG(MainApplication.class);

    private AlfredManager mAlfredManager;

    @Override
    public void onCreate()
    {
        FooLog.v(TAG, "+onCreate()");
        super.onCreate();
        mAlfredManager = new AlfredManager(this);
        mAlfredManager.start();
        FooLog.v(TAG, "-onCreate()");
    }

    public AlfredManager getAlfredManager()
    {
        return mAlfredManager;
    }
}

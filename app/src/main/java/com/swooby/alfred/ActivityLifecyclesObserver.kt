package com.swooby.alfred

import android.app.Activity
import android.app.Application
import android.os.Bundle

import com.smartfoo.android.core.logging.FooLog

class ActivityLifecyclesObserver(private val application: Application)  {
    companion object {
        private const val TAG = "ActivityLifecyclesObserver"
    }

    /**
     * Returns true if any Activities are started and not stopped.
     * Often used to determine if:
     * 1. A Service should be started or not
     * 2. A Notification or Toast should be shown or not
     */
    val hasStartedActivities: Boolean
        get() = startedActivities.isNotEmpty()

    val isForeground: Boolean
        get() = hasStartedActivities

    val isBackground: Boolean
        get() = !isForeground

    fun start() {
        application.registerActivityLifecycleCallbacks(activityLifecycleCallbacks)
    }

    fun stop() {
        application.unregisterActivityLifecycleCallbacks(activityLifecycleCallbacks)
    }

    private val startedActivities = mutableSetOf<Activity>()

    private val activityLifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            FooLog.d(TAG, "activityLifecycleCallbacks: Activity ${activity.localClassName} - Created")
        }

        override fun onActivityStarted(activity: Activity) {
            FooLog.d(TAG, "activityLifecycleCallbacks: Activity ${activity.localClassName} - Started")
            startedActivities.add(activity)
        }

        override fun onActivityResumed(activity: Activity) {
            FooLog.d(TAG, "activityLifecycleCallbacks: Activity ${activity.localClassName} - Resumed")
        }

        override fun onActivityPaused(activity: Activity) {
            FooLog.d(TAG, "activityLifecycleCallbacks: Activity ${activity.localClassName} - Paused")
        }

        override fun onActivityStopped(activity: Activity) {
            FooLog.d(TAG, "activityLifecycleCallbacks: Activity ${activity.localClassName} - Stopped")
            startedActivities.remove(activity)
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
            FooLog.d(TAG, "activityLifecycleCallbacks: Activity ${activity.localClassName} - SaveInstanceState")
        }

        override fun onActivityDestroyed(activity: Activity) {
            FooLog.d(TAG, "activityLifecycleCallbacks: Activity ${activity.localClassName} - Destroyed")
        }
    }
}

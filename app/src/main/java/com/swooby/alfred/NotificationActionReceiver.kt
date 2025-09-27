package com.swooby.alfred

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.smartfoo.android.core.logging.FooLog
import com.smartfoo.android.core.platform.FooPlatformUtils

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        FooLog.i(TAG, "+onReceive(context, intent=" + FooPlatformUtils.toString(intent) + ')')
        if (intent?.action == ACTION_QUIT) {
            val application = MainApplication.getMainApplication(context)
            application.alfredManager.quit()
        } else {
            FooLog.w(TAG, "onReceive: Unexpected action=" + intent?.action)
        }
        FooLog.i(TAG, "-onReceive(context, intent=" + FooPlatformUtils.toString(intent) + ')')
    }

    companion object {
        private val TAG: String = FooLog.TAG(NotificationActionReceiver::class.java)

        @JvmField
        val ACTION_QUIT: String = NotificationActionReceiver::class.java.name + ".ACTION_QUIT"

        @JvmStatic
        fun createQuitIntent(context: Context): Intent {
            return Intent(context, NotificationActionReceiver::class.java).setAction(ACTION_QUIT)
        }
    }
}


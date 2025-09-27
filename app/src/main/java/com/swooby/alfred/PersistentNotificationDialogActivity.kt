package com.swooby.alfred

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.smartfoo.android.core.logging.FooLog

class PersistentNotificationDialogActivity : AppCompatActivity() {

    private var alertDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        FooLog.v(TAG, "+onCreate(savedInstanceState=$savedInstanceState)")
        super.onCreate(savedInstanceState)
        showPersistentDialog()
        FooLog.v(TAG, "-onCreate(savedInstanceState=$savedInstanceState)")
    }

    override fun onDestroy() {
        FooLog.v(TAG, "+onDestroy()")
        alertDialog?.setOnDismissListener(null)
        alertDialog?.dismiss()
        alertDialog = null
        super.onDestroy()
        FooLog.v(TAG, "-onDestroy()")
    }

    private fun showPersistentDialog() {
        val command = getString(R.string.alfred_notification_persistent_command)
        val message = getString(R.string.alfred_notification_persistent_dialog_message, command)

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.alfred_notification_persistent_dialog_title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .create()

        dialog.setOnShowListener {
            dialog.findViewById<TextView>(android.R.id.message)?.setTextIsSelectable(true)
        }
        dialog.setOnDismissListener { finish() }
        dialog.show()

        alertDialog = dialog
    }

    companion object {
        private val TAG: String = FooLog.TAG(PersistentNotificationDialogActivity::class.java)

        @JvmStatic
        fun createIntent(context: Context): Intent =
            Intent(context, PersistentNotificationDialogActivity::class.java)
    }
}


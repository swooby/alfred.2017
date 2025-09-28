package com.swooby.alfred

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.smartfoo.android.core.logging.FooLog
import kotlin.math.roundToInt

class PersistentNotificationDialogActivity : AppCompatActivity() {
    companion object {
        private val TAG: String = FooLog.TAG(PersistentNotificationDialogActivity::class.java)

        @JvmStatic
        fun createIntent(context: Context): Intent =
            Intent(context, PersistentNotificationDialogActivity::class.java)
    }

    private var alertDialog: AlertDialog? = null
    private val appPreferences by lazy { AppPreferences(applicationContext) }

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
        val packageName = applicationContext.packageName
        val command = getString(R.string.alfred_notification_persistent_command, packageName)
        val message = getString(R.string.alfred_notification_persistent_dialog_message)

        val density = resources.displayMetrics.density
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val horizontalPadding = (24 * density).roundToInt()
            val topPadding = (24 * density).roundToInt()
            val bottomPadding = (8 * density).roundToInt()
            setPadding(horizontalPadding, topPadding, horizontalPadding, bottomPadding)
        }

        val messageView = TextView(this).apply {
            text = message
            setTextAppearance(android.R.style.TextAppearance_Material_Body1)
        }
        layout.addView(messageView)

        val commandView = TextView(this).apply {
            text = command
            typeface = Typeface.MONOSPACE
            setTextIsSelectable(true)
            setTextAppearance(android.R.style.TextAppearance_Material_Body1)
            val topPadding = (16 * density).roundToInt()
            setPadding(0, topPadding, 0, 0)
        }
        commandView.isClickable = true
        commandView.isFocusable = true
        commandView.setOnClickListener { copyCommandToClipboard(command) }
        layout.addView(commandView)

        val copyButton = Button(this).apply {
            text = getString(R.string.alfred_notification_persistent_copy_button)
            setOnClickListener { copyCommandToClipboard(command) }
        }
        val buttonLayoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            topMargin = (12 * density).roundToInt()
        }
        layout.addView(copyButton, buttonLayoutParams)

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.alfred_notification_persistent_dialog_title)
            .setView(layout)
            .setPositiveButton(android.R.string.ok) { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .setNegativeButton(R.string.alfred_notification_persistent_ignore_button) { dialogInterface, _ ->
                onIgnoreClicked()
                dialogInterface.dismiss()
            }
            .create()
        dialog.setOnDismissListener { finish() }
        dialog.show()

        alertDialog = dialog
    }

    private fun copyCommandToClipboard(command: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        if (clipboard == null) {
            FooLog.w(TAG, "Clipboard manager not available")
            return
        }
        clipboard.setPrimaryClip(ClipData.newPlainText("Persistent notification command", command))
    }

    private fun onIgnoreClicked() {
        FooLog.i(TAG, "User chose to ignore persistent notification guidance")
        appPreferences.setPersistentNotificationActionIgnored(true)

        val application = MainApplication.getMainApplication(this)
        application.alfredManager.refreshOngoingNotification()

        Toast.makeText(
            this,
            R.string.alfred_notification_persistent_ignore_confirmation,
            Toast.LENGTH_LONG,
        ).show()
    }
}

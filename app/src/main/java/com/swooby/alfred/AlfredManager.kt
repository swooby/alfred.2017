package com.swooby.alfred

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Message
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import androidx.annotation.StringRes
import com.smartfoo.android.core.FooListenerManager
import com.smartfoo.android.core.FooString
import com.smartfoo.android.core.collections.FooLongSparseArray
import com.smartfoo.android.core.logging.FooLog
import com.smartfoo.android.core.media.FooAudioStreamVolumeObserver
import com.smartfoo.android.core.media.FooAudioUtils
import com.smartfoo.android.core.network.FooCellularStateListener
import com.smartfoo.android.core.network.FooCellularStateListener.FooCellularHookStateCallbacks
import com.smartfoo.android.core.network.FooDataConnectionListener
import com.smartfoo.android.core.network.FooDataConnectionListener.FooDataConnectionInfo
import com.smartfoo.android.core.network.FooDataConnectionListener.FooDataConnectionListenerCallbacks
import com.smartfoo.android.core.notification.FooNotificationListenerManager.NotConnectedReason
import com.smartfoo.android.core.permissions.FooPermissionsChecker
import com.smartfoo.android.core.platform.FooBootListener
import com.smartfoo.android.core.platform.FooBootListener.FooBootListenerCallbacks
import com.smartfoo.android.core.platform.FooChargePortListener
import com.smartfoo.android.core.platform.FooChargePortListener.ChargePort
import com.smartfoo.android.core.platform.FooChargePortListener.FooChargePortListenerCallbacks
import com.smartfoo.android.core.platform.FooHandler
import com.smartfoo.android.core.platform.FooPlatformUtils
import com.smartfoo.android.core.platform.FooScreenListener
import com.smartfoo.android.core.platform.FooScreenListener.FooScreenListenerCallbacks
import com.smartfoo.android.core.texttospeech.FooTextToSpeech.Companion.statusToString
import com.smartfoo.android.core.texttospeech.FooTextToSpeechBuilder
import com.swooby.alfred.NotificationManager.NotificationStatus
import com.swooby.alfred.NotificationManager.NotificationStatusNotificationAccessNotEnabled
import com.swooby.alfred.NotificationManager.NotificationStatusProfileNotEnabled
import com.swooby.alfred.NotificationManager.NotificationStatusRunning
import com.swooby.alfred.NotificationParserManager.NotificationParserManagerCallbacks
import com.swooby.alfred.NotificationParserManager.NotificationParserManagerConfiguration
import com.swooby.alfred.ProfileManager.HeadsetType
import com.swooby.alfred.ProfileManager.ProfileManagerCallbacks
import com.swooby.alfred.ProfileManager.ProfileManagerConfiguration
import com.swooby.alfred.TextToSpeechManager.TextToSpeechManagerCallbacks
import com.swooby.alfred.TextToSpeechManager.TextToSpeechManagerConfiguration
import com.swooby.alfred.notification.parsers.AbstractNotificationParser
import com.swooby.alfred.notification.parsers.AlfredNotificationParser
import java.util.EnumMap
import java.util.concurrent.TimeUnit

class AlfredManager(applicationContext: Context) {

    companion object {
        private val TAG: String = FooLog.TAG(AlfredManager::class.java)
    }

    interface AlfredManagerCallbacks {
        val activity: Activity?

        fun onReadPhoneStatePermissionRequired()

        fun onReadPhoneStatePermissionGranted()

        fun onBluetoothConnectPermissionRequired()

        fun onBluetoothConnectPermissionGranted()

        fun onPostNotificationsPermissionRequired()

        fun onPostNotificationsPermissionGranted()

        fun onNotificationListenerConnected()

        fun onNotificationListenerNotConnected(reason: NotConnectedReason): Boolean

        fun onProfileEnabled(profile: Profile)

        fun onProfileDisabled(profile: Profile)

        fun onTextToSpeechAudioStreamVolumeChanged(audioStreamType: Int, volume: Int)
    }

    val applicationContext: Context
    private val mHandler: FooHandler
    private val mAppPreferences: AppPreferences
    private val mListenerManager: FooListenerManager<AlfredManagerCallbacks>
    private val mNotificationManager: NotificationManager
    private val mSayingsManager: SayingsManager
    val textToSpeechManager: TextToSpeechManager
    val notificationParserManager: NotificationParserManager
    private val mScreenListener: FooScreenListener
    private val mBootListener: FooBootListener
    private val mChargePortListener: FooChargePortListener
    private val mCellularStateListener: FooCellularStateListener
    private val mCellularHookStateCallbacks: FooCellularHookStateCallbacks
    private val mDataConnectionListener: FooDataConnectionListener
    private val mDataConnectionListenerCallbacks: FooDataConnectionListenerCallbacks
    private val mAudioStreamVolumeObserver: FooAudioStreamVolumeObserver
    private val mMandatoryPermissions: MandatoryPermissions
    val profileManager: ProfileManager
    private val mProfileManagerCallbacks: ProfileManagerCallbacks

    var isStarted: Boolean = false
        private set
    private var mIsUserUnlocked = false
    private var mHasCompletedPostNotificationSetup = false
    private var mHasStartedProfileManager = false
    private var mHasAttachedProfileManager = false
    private var mHasStartedCellularStateListener = false
    private var mHasStartedDataConnectionListener = false

    private val mTimeDataConnected = FooLongSparseArray<Long>()
    private val mTimeDataDisconnected = FooLongSparseArray<Long>()

    init {
        FooLog.v(
            TAG,
            "+AlfredManager(applicationContext=$applicationContext)"
        )

        this.applicationContext = applicationContext

        mHandler = FooHandler { msg: Message ->
            this@AlfredManager.handleMessage(
                msg
            )
        }

        mAppPreferences = AppPreferences(this.applicationContext)

        //
        // Create Managers/etc
        //
        mListenerManager = FooListenerManager(this)
        mNotificationManager = NotificationManager(this.applicationContext)
        mSayingsManager = SayingsManager(this.applicationContext)
        textToSpeechManager =
            TextToSpeechManager(this.applicationContext, object : TextToSpeechManagerConfiguration {
                override fun getVoiceName(): String {
                    return mAppPreferences.textToSpeechVoiceName()
                }

                override fun setVoiceName(voiceName: String) {
                    mAppPreferences.setTextToSpeechVoiceName(voiceName)
                }

                override fun getAudioStreamType(): Int {
                    return mAppPreferences.textToSpeechAudioStreamType
                }

                override fun isTextToSpeechEnabled(): Boolean {
                    return this@AlfredManager.isTextToSpeechEnabled
                }
            })
        notificationParserManager = NotificationParserManager(
            this.applicationContext,
            object : NotificationParserManagerConfiguration {
                override fun isNotificationParserEnabled(): Boolean {
                    return this@AlfredManager.isProfileEnabled
                }

                override fun getTextToSpeech(): TextToSpeechManager {
                    return this@AlfredManager.textToSpeechManager
                }
            })

        mScreenListener = FooScreenListener(this.applicationContext)
        mBootListener = FooBootListener(this.applicationContext)
        mChargePortListener = FooChargePortListener(this.applicationContext)

        mCellularStateListener = FooCellularStateListener(this.applicationContext)
        mCellularHookStateCallbacks = object : FooCellularHookStateCallbacks {
            override fun onCellularOffHook() {
                this@AlfredManager.onCellularOffHook()
            }

            override fun onCellularOnHook() {
                this@AlfredManager.onCellularOnHook()
            }
        }
        mDataConnectionListener = FooDataConnectionListener(this.applicationContext)
        mDataConnectionListenerCallbacks = object : FooDataConnectionListenerCallbacks {
            override fun onDataConnected(dataConnectionInfo: FooDataConnectionInfo) {
                this@AlfredManager.onDataConnected(dataConnectionInfo)
            }

            override fun onDataDisconnected(dataConnectionInfo: FooDataConnectionInfo) {
                this@AlfredManager.onDataDisconnected(dataConnectionInfo)
            }
        }
        mAudioStreamVolumeObserver = FooAudioStreamVolumeObserver(this.applicationContext)

        profileManager =
            ProfileManager(this.applicationContext, object : ProfileManagerConfiguration {
                override fun getProfileToken(): String {
                    return mAppPreferences.profileToken()
                }

                override fun setProfileToken(profileToken: String?) {
                    mAppPreferences.setProfileToken(profileToken)
                }
            })
        mProfileManagerCallbacks = object : ProfileManagerCallbacks() {
            public override fun onHeadsetConnectionChanged(
                headsetType: HeadsetType,
                headsetName: String,
                isConnected: Boolean
            ) {
                this@AlfredManager.onHeadsetConnectionChanged(
                    headsetType,
                    headsetName,
                    isConnected
                )
            }

            override fun onProfileEnabled(profile: Profile) {
                this@AlfredManager.onProfileEnabled(profile)
            }

            override fun onProfileDisabled(profile: Profile) {
                this@AlfredManager.onProfileDisabled(profile)
            }
        }

        mMandatoryPermissions = MandatoryPermissions(
            this.applicationContext,
            object : MandatoryPermissions.Listener {
                override fun onMandatoryPermissionRequired(permission: String) {
                    this@AlfredManager.onMandatoryPermissionRequired(permission)
                }

                override fun onMandatoryPermissionGranted(permission: String) {
                    this@AlfredManager.onMandatoryPermissionGranted(permission)
                }
            }
        )

        FooLog.v(
            TAG,
            "-AlfredManager(applicationContext=$applicationContext)"
        )
    }

    fun getString(@StringRes resId: Int, vararg formatArgs: Any?): String {
        return applicationContext.getString(resId, *formatArgs)
    }

    //
    //region Speak
    //
    fun speak(text: String) {
        speak(false, text)
    }

    fun speak(clear: Boolean, text: String) {
        textToSpeechManager.speak(clear, text)
    }

    fun speak(builder: FooTextToSpeechBuilder) {
        speak(false, builder)
    }

    fun speak(clear: Boolean, builder: FooTextToSpeechBuilder) {
        textToSpeechManager.speak(clear, builder)
    }

    fun speakGreeting() {
        speak(true, mSayingsManager.goodPartOfDayUserNoun())
    }

    //
    //endregion Speak
    //

    private fun isPermissionGranted(permission: String): Boolean {
        return FooPermissionsChecker.isPermissionGranted(applicationContext, permission)
    }

    private val isPermissionGranted_POST_NOTIFICATIONS: Boolean
        get() = isPermissionGranted(Manifest.permission.POST_NOTIFICATIONS)

    private val isPermissionGranted_READ_PHONE_STATE: Boolean
        get() = isPermissionGranted(Manifest.permission.READ_PHONE_STATE)

    private val isPermissionGranted_BLUETOOTH_CONNECT: Boolean
        get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                isPermissionGranted(Manifest.permission.BLUETOOTH_CONNECT)

    val hasPostNotificationsPermission: Boolean
        get() = isPermissionGranted_POST_NOTIFICATIONS

    val hasReadPhoneStatePermission: Boolean
        get() = isPermissionGranted_READ_PHONE_STATE

    val hasBluetoothConnectPermission: Boolean
        get() = isPermissionGranted_BLUETOOTH_CONNECT

    @SuppressLint("MissingPermission")
    fun start() {
        try {
            FooLog.i(TAG, "+start()")

            if (isStarted) {
                mMandatoryPermissions.evaluate()
                return
            }

            isStarted = true
            val timeStartMillis = System.currentTimeMillis()
            textToSpeechManager.attach(object : TextToSpeechManagerCallbacks() {
                override fun onTextToSpeechInitialized(status: Int) {
                    val timeElapsedMillis = System.currentTimeMillis() - timeStartMillis
                    super.onTextToSpeechInitialized(status)
                    this@AlfredManager.onTextToSpeechInitialized(status, timeElapsedMillis)
                }
            })
            notificationParserManager.attach(object : NotificationParserManagerCallbacks {
                override fun onNotificationListenerConnected(activeNotifications: Array<StatusBarNotification>): Boolean {
                    return this@AlfredManager.onNotificationListenerConnected()
                }

                override fun onNotificationListenerNotConnected(
                    reason: NotConnectedReason,
                    elapsedMillis: Long
                ) {
                    this@AlfredManager.onNotificationListenerNotConnected(
                        reason,
                        elapsedMillis,
                        200
                    )
                }

                override fun onNotificationParsed(parser: AbstractNotificationParser) {
                    this@AlfredManager.onNotificationParsed(parser)
                }
            })
            mScreenListener.attach(object : FooScreenListenerCallbacks {
                override fun onScreenOff() {
                    this@AlfredManager.onScreenOff()
                }

                override fun onScreenOn() {
                    this@AlfredManager.onScreenOn()
                }

                override fun onUserUnlocked() {
                    FooLog.e(TAG, "onUserUnlocked()")
                    speak("user unlocked")
                }
            })
            mBootListener.attach(object : FooBootListenerCallbacks {
                override fun onBootCompleted() {
                }

                override fun onReboot() {
                    speak("rebooting")
                }

                override fun onShutdown() {
                    speak("shutting down")
                }
            })
            mChargePortListener.attach(object : FooChargePortListenerCallbacks {
                override fun onChargePortConnected(chargePort: ChargePort) {
                    this@AlfredManager.onChargePortConnected(chargePort)
                }

                override fun onChargePortDisconnected(chargePort: ChargePort) {
                    this@AlfredManager.onChargePortDisconnected(chargePort)
                }
            })
            for (audioStreamType in FooAudioUtils.getAudioStreamTypes()) {
                volumeObserverStart(audioStreamType)
            }
            // TODO:(pv) Phone doze listener
            // TODO:(pv) etc…
            val requiresBluetoothPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            if (requiresBluetoothPermission) {
                mMandatoryPermissions.register(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    this::startProfileManagerIfPossible
                )
            } else {
                startProfileManagerIfPossible()
            }

            mMandatoryPermissions.register(
                Manifest.permission.POST_NOTIFICATIONS,
                this::completePostNotificationSetupIfPossible
            )
            mMandatoryPermissions.register(
                Manifest.permission.READ_PHONE_STATE,
                this::startTelephonyListenersIfPossible
            )
            mMandatoryPermissions.evaluate()

            /*
            if (!isRecognitionAvailable())
            {
                // TODO:(pv) Better place for initialization and indication of failure…
                //speak(true, true, "Speech recognition is not available for this device.");
                //speak(true, true, "Goodbye.");
                return;
            }

            mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            */
        } catch (e: Exception) {
            FooLog.e(TAG, "start()", e)
            throw e
        } finally {
            FooLog.i(TAG, "-start()")
        }
    }

    /*
    // PocketSphinx stuff...

    private SpeechRecognizer mSpeechRecognizer;

    public boolean isSpeechRecognitionAvailable()
    {
        return SpeechRecognizer.isRecognitionAvailable(this);
    }
    */

    fun attach(callbacks: AlfredManagerCallbacks) {
        FooLog.i(TAG, "attach(callbacks=$callbacks)")
        mListenerManager.attach(callbacks)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            mMandatoryPermissions.isPermissionMissing(Manifest.permission.BLUETOOTH_CONNECT)
        ) {
            callbacks.onBluetoothConnectPermissionRequired()
        }
        if (mMandatoryPermissions.isPermissionMissing(Manifest.permission.READ_PHONE_STATE)) {
            callbacks.onReadPhoneStatePermissionRequired()
        }
        if (mMandatoryPermissions.isPermissionMissing(Manifest.permission.POST_NOTIFICATIONS)) {
            callbacks.onPostNotificationsPermissionRequired()
        }
        if (callbacks.activity != null) {
            // TODO:(pv) Cancel any pending Toasts…
        }
    }

    fun detach(callbacks: AlfredManagerCallbacks) {
        FooLog.i(TAG, "detach(callbacks=$callbacks)")
        mListenerManager.detach(callbacks)
    }

    fun onPostNotificationsPermissionGranted() {
        FooLog.i(TAG, "onPostNotificationsPermissionGranted()")
        mMandatoryPermissions.onPermissionGranted(Manifest.permission.POST_NOTIFICATIONS)
    }

    fun onReadPhoneStatePermissionGranted() {
        FooLog.i(TAG, "onReadPhoneStatePermissionGranted()")
        mMandatoryPermissions.onPermissionGranted(Manifest.permission.READ_PHONE_STATE)
    }

    fun onBluetoothConnectPermissionGranted() {
        FooLog.i(TAG, "onBluetoothConnectPermissionGranted()")
        mMandatoryPermissions.onPermissionGranted(Manifest.permission.BLUETOOTH_CONNECT)
    }

    private val isProfileEnabled: Boolean
        get() = profileManager.isEnabled

    private val isTextToSpeechEnabled: Boolean
        get() = isProfileEnabled && mCellularStateListener.isOnHook

    val isHeadless: Boolean
        get() {
            for (callbacks in mListenerManager.beginTraversing()) {
                if (callbacks.activity != null) {
                    return false
                }
            }
            mListenerManager.endTraversing()
            return true
        }

    @SuppressLint("MissingPermission")
    private fun notification(
        notificationStatus: NotificationStatus,
        text: String,
        subtext: String
    ) {
        if (isPermissionGranted_POST_NOTIFICATIONS) {
            if (notificationStatus is NotificationStatusProfileNotEnabled) {
                mNotificationManager.notifyOngoingPaused(notificationStatus, text, subtext)
            } else {
                mNotificationManager.notifyOngoingRunning(notificationStatus, text, subtext)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun completePostNotificationSetupIfPossible(): Boolean {
        if (mHasCompletedPostNotificationSetup) {
            return true
        }
        if (!isPermissionGranted_POST_NOTIFICATIONS) {
            FooLog.v(TAG, "completePostNotificationSetupIfPossible: permission not granted")
            return false
        }
        mHasCompletedPostNotificationSetup = true

        mNotificationManager.notifyOngoingInitializing(
            "Text To Speech",
            "TBD text",
            "TBD subtext"
        )
        return true
    }

    private fun notifyPostNotificationsPermissionRequired() {
        FooLog.i(TAG, "notifyPostNotificationsPermissionRequired()")
        for (callbacks in mListenerManager.beginTraversing()) {
            callbacks.onPostNotificationsPermissionRequired()
        }
        mListenerManager.endTraversing()
    }

    private fun notifyPostNotificationsPermissionGranted() {
        FooLog.i(TAG, "notifyPostNotificationsPermissionGranted()")
        for (callbacks in mListenerManager.beginTraversing()) {
            callbacks.onPostNotificationsPermissionGranted()
        }
        mListenerManager.endTraversing()
    }

    private fun notifyBluetoothConnectPermissionRequired() {
        FooLog.i(TAG, "notifyBluetoothConnectPermissionRequired()")
        for (callbacks in mListenerManager.beginTraversing()) {
            callbacks.onBluetoothConnectPermissionRequired()
        }
        mListenerManager.endTraversing()
    }

    private fun notifyBluetoothConnectPermissionGranted() {
        FooLog.i(TAG, "notifyBluetoothConnectPermissionGranted()")
        for (callbacks in mListenerManager.beginTraversing()) {
            callbacks.onBluetoothConnectPermissionGranted()
        }
        mListenerManager.endTraversing()
    }

    private fun notifyReadPhoneStatePermissionRequired() {
        FooLog.i(TAG, "notifyReadPhoneStatePermissionRequired()")
        for (callbacks in mListenerManager.beginTraversing()) {
            callbacks.onReadPhoneStatePermissionRequired()
        }
        mListenerManager.endTraversing()
    }

    private fun onMandatoryPermissionRequired(permission: String) {
        when (permission) {
            Manifest.permission.POST_NOTIFICATIONS -> notifyPostNotificationsPermissionRequired()
            Manifest.permission.READ_PHONE_STATE -> notifyReadPhoneStatePermissionRequired()
            Manifest.permission.BLUETOOTH_CONNECT -> notifyBluetoothConnectPermissionRequired()
            else -> FooLog.w(TAG, "Unhandled mandatory permission required notification: $permission")
        }
    }

    private fun onMandatoryPermissionGranted(permission: String) {
        when (permission) {
            Manifest.permission.POST_NOTIFICATIONS -> notifyPostNotificationsPermissionGranted()
            Manifest.permission.READ_PHONE_STATE -> notifyReadPhoneStatePermissionGranted()
            Manifest.permission.BLUETOOTH_CONNECT -> notifyBluetoothConnectPermissionGranted()
            else -> FooLog.w(TAG, "Unhandled mandatory permission granted notification: $permission")
        }
    }

    private fun notifyReadPhoneStatePermissionGranted() {
        FooLog.i(TAG, "notifyReadPhoneStatePermissionGranted()")
        for (callbacks in mListenerManager.beginTraversing()) {
            callbacks.onReadPhoneStatePermissionGranted()
        }
        mListenerManager.endTraversing()
    }

    private fun startProfileManagerIfPossible(): Boolean {
        if (!isPermissionGranted_BLUETOOTH_CONNECT) {
            FooLog.v(TAG, "startProfileManagerIfPossible: permission not granted")
            return false
        }

        if (!mHasStartedProfileManager) {
            try {
                profileManager.start()
                mHasStartedProfileManager = true
            } catch (e: SecurityException) {
                FooLog.w(TAG, "startProfileManagerIfPossible: unable to start profile manager", e)
                return false
            }
        }

        if (!mHasAttachedProfileManager) {
            try {
                profileManager.attach(mProfileManagerCallbacks)
                mHasAttachedProfileManager = true
            } catch (e: SecurityException) {
                FooLog.w(TAG, "startProfileManagerIfPossible: unable to attach profile manager", e)
                return false
            }
        }

        return mHasStartedProfileManager && mHasAttachedProfileManager
    }

    private fun startTelephonyListenersIfPossible(): Boolean {
        if (!isPermissionGranted_READ_PHONE_STATE) {
            FooLog.v(TAG, "startTelephonyListenersIfPossible: permission not granted")
            return false
        }

        var permissionError = false
        if (!mHasStartedCellularStateListener) {
            try {
                mCellularStateListener.start(mCellularHookStateCallbacks, null)
                mHasStartedCellularStateListener = true
            } catch (e: SecurityException) {
                FooLog.w(TAG, "startTelephonyListenersIfPossible: unable to start cellular listener", e)
                permissionError = true
            }
        }

        if (!permissionError && !mHasStartedDataConnectionListener) {
            try {
                mDataConnectionListener.start(mDataConnectionListenerCallbacks)
                mHasStartedDataConnectionListener = true
            } catch (e: SecurityException) {
                FooLog.w(TAG, "startTelephonyListenersIfPossible: unable to start data connection listener", e)
                permissionError = true
            }
        }

        val listenersStarted = mHasStartedCellularStateListener && mHasStartedDataConnectionListener
        if (listenersStarted && !permissionError) {
            updateDataConnectionInfo()
            return true
        }

        if (permissionError) {
            FooLog.v(TAG, "startTelephonyListenersIfPossible: still waiting for permission")
        }
        return false
    }

    private fun onTextToSpeechInitialized(status: Int, timeElapsedMillis: Long) {
        FooLog.i(
            TAG, "onTextToSpeechInitialized: timeElapsedMillis == " + timeElapsedMillis +
                    ", status == " + statusToString(status)
        )
        if (status != TextToSpeech.SUCCESS) {
            FooLog.e(TAG, "onTextToSpeechInitialized: status != TextToSpeech.SUCCESS")
            // TODO: Notify the user that this app, who's whole purpose is to speak, is pretty useless then.
            // Similar to what `start()`'s `isPermissionGranted_POST_NOTIFICATIONS()` check needs to do.
            return
        }
    }

    private fun onProfileEnabled(profile: Profile) {
        FooLog.i(TAG, "onProfileEnabled(profile=$profile)")

        textToSpeechManager.clear()

        //
        // !!!!!!THIS IS WHERE THE REAL APP LOGIC ACTUALLY STARTS!!!!!!
        //
        speakGreeting()

        mIsUserUnlocked = mIsUserUnlocked or mScreenListener.isUserUnlocked
        if (!mIsUserUnlocked) {
            FooLog.i(TAG, "onProfileEnabled: mIsUserUnlocked == false")

            speak("Your device has just been rebooted and needs to be unlocked before I can read notifications to you.")
        }

        val notificationStatus: NotificationStatus
        val isNotificationListenerConnected =
            notificationParserManager.isNotificationListenerConnected
        notificationStatus = if (isNotificationListenerConnected) {
            NotificationStatusRunning(applicationContext)
        } else {
            NotificationStatusNotificationAccessNotEnabled(
                applicationContext,
                getString(R.string.alfred_running),
                getString(R.string.alfred_waiting_for_notification_access),
                null
            ) // <-- TODO:(pv) Put above/below speech in here
        }
        notification(notificationStatus, "TBD text", "onProfileEnabled")

        updateScreenInfo()
        updateChargePortInfo()
        updateDataConnectionInfo()

        for (callbacks in mListenerManager.beginTraversing()) {
            callbacks.onProfileEnabled(profile)
        }
        mListenerManager.endTraversing()

        if (isNotificationListenerConnected) {
            onNotificationListenerConnected()
        }
    }

    private fun onProfileDisabled(profile: Profile) {
        FooLog.i(TAG, "onProfileDisabled(profile=$profile)")

        val notificationStatus: NotificationStatus = NotificationStatusProfileNotEnabled(
            applicationContext, profile
        )
        notification(notificationStatus, "TBD text", "onProfileDisabled")

        textToSpeechManager.clear()

        for (callbacks in mListenerManager.beginTraversing()) {
            callbacks.onProfileDisabled(profile)
        }
        mListenerManager.endTraversing()
    }

    //
    //
    //
    private inner class DelayedRunnableNotificationListenerNotConnected
        (private val mReason: NotConnectedReason, private val mElapsedMillis: Long) : Runnable {
        private val TAG: String =
            FooLog.TAG(DelayedRunnableNotificationListenerNotConnected::class.java)

        override fun run() {
            FooLog.v(TAG, "+run()")
            onNotificationListenerNotConnected(mReason, mElapsedMillis, 0)
            FooLog.v(TAG, "-run()")
        }
    }

    private var mDelayedRunnableNotificationAccessSettingDisabled: DelayedRunnableNotificationListenerNotConnected? =
        null

    private fun onNotificationListenerConnected(): Boolean {
        FooLog.i(TAG, "onNotificationListenerConnected()")

        if (mDelayedRunnableNotificationAccessSettingDisabled != null) {
            mHandler.removeCallbacks(mDelayedRunnableNotificationAccessSettingDisabled!!)
            mDelayedRunnableNotificationAccessSettingDisabled = null
        }

        val speech = getString(R.string.alfred_notification_listener_connected)
        speak(speech)

        for (callbacks in mListenerManager.beginTraversing()) {
            callbacks.onNotificationListenerConnected()
        }
        mListenerManager.endTraversing()

        notificationParserManager.initializeActiveNotifications()

        val notificationStatus: NotificationStatus
        val isProfileEnabled = isProfileEnabled
        if (isProfileEnabled) {
            notificationStatus = NotificationStatusRunning(applicationContext)
        } else {
            val profile = profileManager.profile
            notificationStatus = NotificationStatusProfileNotEnabled(applicationContext, profile)
        }
        notification(notificationStatus, "TBD text", "onNotificationAccessSettingConfirmedEnabled")

        return true
    }

    /**
     * @param reason                reason
     * @param elapsedMillis         elapsedMillis
     * @param ifHeadlessDelayMillis &gt; 0 to delay the given milliseconds if no UI is attached
     */
    private fun onNotificationListenerNotConnected(
        reason: NotConnectedReason,
        elapsedMillis: Long,
        ifHeadlessDelayMillis: Int
    ) {
        FooLog.w(
            TAG, "onNotificationListenerNotConnected(reason=" + reason +
                    ", elapsedMillis=" + elapsedMillis +
                    ", ifHeadlessDelayMillis=" + ifHeadlessDelayMillis + ')'
        )

        if (!mIsUserUnlocked) {
            FooLog.i(TAG, "onNotificationListenerNotConnected: mIsUserUnlocked == false; ignoring")
            return
        }

        var headless = true
        var handled = false
        for (callbacks in mListenerManager.beginTraversing()) {
            headless = headless and (callbacks.activity == null)
            handled = handled or callbacks.onNotificationListenerNotConnected(reason)
        }
        mListenerManager.endTraversing()

        if (headless && ifHeadlessDelayMillis > 0) {
            mDelayedRunnableNotificationAccessSettingDisabled =
                DelayedRunnableNotificationListenerNotConnected(reason, elapsedMillis)
            mHandler.postDelayed(
                mDelayedRunnableNotificationAccessSettingDisabled!!,
                ifHeadlessDelayMillis.toLong()
            )
            return
        }

        val title = getNotificationListenerNotConnectedTitle(reason)
        val message = getNotificationListenerNotConnectedMessage(reason)

        if (headless) {
            val separator = getString(R.string.alfred_line_separator)
            val text = FooString.join(separator, title, message)
            FooPlatformUtils.toastLong(applicationContext, text)
        }

        speak(
            FooTextToSpeechBuilder()
                .appendSpeech(title)
                .appendSilenceWordBreak()
                .appendSpeech(message)
        )

        val notificationStatus: NotificationStatus
        val isProfileEnabled = isProfileEnabled
        if (isProfileEnabled) {
            notificationStatus = NotificationStatusNotificationAccessNotEnabled(
                applicationContext,
                getString(R.string.alfred_running),
                getString(R.string.alfred_waiting_for_notification_access),
                null
            )
        } else {
            val profile = profileManager.profile
            notificationStatus = NotificationStatusProfileNotEnabled(applicationContext, profile)
        }
        notification(notificationStatus, "TBD text", "onNotificationAccessSettingDisabled")
    }

    fun getNotificationListenerNotConnectedTitle(reason: NotConnectedReason): String {
        val resId = when (reason) {
            NotConnectedReason.ConfirmedNotEnabled -> R.string.alfred_notification_access_not_enabled
            NotConnectedReason.ConnectedTimeout -> R.string.alfred_notification_listener_bind_timeout
            else -> throw IllegalArgumentException("Unhandled reason == $reason")
        }
        return getString(resId)
    }

    fun getNotificationListenerNotConnectedMessage(reason: NotConnectedReason): String {
        val resId = when (reason) {
            NotConnectedReason.ConfirmedNotEnabled -> R.string.alfred_please_enable_notification_access_for_the_X_application
            NotConnectedReason.ConnectedTimeout -> R.string.alfred_please_reenable_notification_access_for_the_X_application
            else -> throw IllegalArgumentException("Unhandled reason == $reason")
        }

        val appName = getString(R.string.alfred_app_name)

        return getString(resId, appName)
    }

    private fun onNotificationParsed(parser: AbstractNotificationParser) {
        if (parser is AlfredNotificationParser) {
            onAlfredNotificationParsed(parser)
        }
    }

    private fun onAlfredNotificationParsed(parser: AlfredNotificationParser) {
        //...
    }

    //
    //
    //
    private fun onHeadsetConnectionChanged(
        headsetType: HeadsetType,
        headsetName: String?,
        isConnected: Boolean
    ) {
        @Suppress("NAME_SHADOWING")
        var headsetName = headsetName
        FooLog.i(
            TAG, "onHeadsetConnectionChanged(headsetType=" + headsetType +
                    ", headsetName=" + FooString.quote(headsetName) +
                    ", isConnected=" + isConnected + ')'
        )
        if (headsetName == null) {
            headsetName = ""
        }

        val resIdConnection =
            if (isConnected) R.string.alfred_X_connected else R.string.alfred_X_disconnected

        val resIdHeadphone = when (headsetType) {
            HeadsetType.Bluetooth -> R.string.alfred_headphone_bluetooth_X
            HeadsetType.Wired -> R.string.alfred_headphone_wired
        }

        val textHeadphone = getString(resIdHeadphone, headsetName)
        val speech = getString(resIdConnection, textHeadphone)
        speak(speech)
    }

    //
    // Screen…
    //
    private fun onScreenOff() {
        FooLog.i(TAG, "onScreenOff()")
        updateScreenInfo()
    }

    private fun onScreenOn() {
        FooLog.i(TAG, "onScreenOn()")
        updateScreenInfo()
    }

    private var mTimeScreenOnMs: Long = -1
    private var mTimeScreenOffMs: Long = -1

    private fun updateScreenInfo() {
        val isScreenOn = mScreenListener.isScreenOn
        val speech: String
        if (isScreenOn) {
            mTimeScreenOnMs = System.currentTimeMillis()

            if (mTimeScreenOffMs != -1L) {
                val durationMs = mTimeScreenOnMs - mTimeScreenOffMs

                mTimeScreenOffMs = -1

                speech = getString(
                    R.string.alfred_screen_on_after_being_off_for_X,
                    FooString.getTimeDurationString(
                        applicationContext, durationMs, TimeUnit.SECONDS
                    )
                )
            } else {
                speech = getString(R.string.alfred_screen_on)
            }
        } else {
            mTimeScreenOffMs = System.currentTimeMillis()

            if (mTimeScreenOnMs != -1L) {
                val durationMs = mTimeScreenOffMs - mTimeScreenOnMs

                mTimeScreenOnMs = -1

                speech = getString(
                    R.string.alfred_screen_off_after_being_on_for_X,
                    FooString.getTimeDurationString(
                        applicationContext, durationMs, TimeUnit.SECONDS
                    )
                )
            } else {
                speech = getString(R.string.alfred_screen_off)
            }
        }
        speak(speech)
    }

    //
    // Charging/Ports…
    //
    private fun updateChargePortInfo() {
        for (chargingPort in mChargePortListener.chargingPorts) {
            onChargePortConnected(chargingPort)
        }
    }

    private val mTimeChargingConnected: MutableMap<ChargePort, Long> = EnumMap(ChargePort::class.java)
    private val mTimeChargingDisconnected: MutableMap<ChargePort, Long> = EnumMap(ChargePort::class.java)

    private fun onChargePortConnected(chargePort: ChargePort) {
        FooLog.i(TAG, "onChargePortConnected(chargePort=$chargePort)")

        val now = System.currentTimeMillis()

        mTimeChargingConnected[chargePort] = now

        val speech: String
        val chargePortName = getString(chargePort.stringRes)
        //FooLog.i(TAG, "onChargePortConnected: chargePortName == " + FooString.quote(chargePortName));
        val timeChargingDisconnectedMs = mTimeChargingDisconnected.remove(chargePort)
        if (timeChargingDisconnectedMs != null) {
            val elapsedMs = now - timeChargingDisconnectedMs
            speech = getString(
                R.string.alfred_X_connected_after_being_disconnected_for_Y,
                chargePortName,
                FooString.getTimeDurationString(applicationContext, elapsedMs, TimeUnit.SECONDS)
            )
        } else {
            speech = getString(R.string.alfred_X_connected, chargePortName)
        }
        speak(speech)
    }

    private fun onChargePortDisconnected(chargePort: ChargePort) {
        FooLog.i(TAG, "onChargePortDisconnected(chargePort=$chargePort)")

        val now = System.currentTimeMillis()

        mTimeChargingDisconnected[chargePort] = now

        val speech: String
        val chargePortName = getString(chargePort.stringRes)
        //FooLog.i(TAG, "onChargePortDisconnected: chargePortName == " + FooString.quote(chargePortName));
        val timeChargingConnectedMs = mTimeChargingConnected.remove(chargePort)
        if (timeChargingConnectedMs != null) {
            val elapsedMs = now - timeChargingConnectedMs
            speech = getString(
                R.string.alfred_X_disconnected_after_being_connected_for_Y,
                chargePortName,
                FooString.getTimeDurationString(applicationContext, elapsedMs, TimeUnit.SECONDS)
            )
        } else {
            speech = getString(R.string.alfred_X_disconnected, chargePortName)
        }
        speak(speech)
    }

    //
    // Data Connection…
    //
    private fun updateDataConnectionInfo() {
        if (!mHasStartedDataConnectionListener) {
            FooLog.v(TAG, "updateDataConnectionInfo: data connection listener not started")
            return
        }
        val dataConnectionInfo = mDataConnectionListener.dataConnectionInfo
        if (dataConnectionInfo.isConnected) {
            onDataConnected(dataConnectionInfo)
        } else {
            onDataDisconnected(dataConnectionInfo)
        }
    }

    private fun onCellularOffHook() {
        speak("Phone Call Started")
    }

    private fun onCellularOnHook() {
        speak("Phone Call Ended")
    }

    private fun onDataConnected(dataConnectionInfo: FooDataConnectionInfo) {
        FooLog.i(
            TAG,
            "onDataConnected(dataConnectionInfo=$dataConnectionInfo)"
        )

        val now = System.currentTimeMillis()

        val dataConnectionType = dataConnectionInfo.type

        mTimeDataConnected.put(dataConnectionType.toLong(), now)

        val speech: String
        val dataConnectionTypeName = getString(
            dataConnectionInfo.getNetworkTypeResourceId(
                BuildConfig.DEBUG
            )
        )
        //FooLog.i(TAG, "onDataConnected: dataConnectionTypeName == " + FooString.quote(dataConnectionTypeName));
        val timeDataDisconnectedMs = mTimeDataDisconnected.remove(dataConnectionType.toLong())
        if (timeDataDisconnectedMs != null) {
            val elapsedMs = now - timeDataDisconnectedMs
            speech = dataConnectionTypeName + " connected after being disconnected for " +
                    FooString.getTimeDurationString(applicationContext, elapsedMs, TimeUnit.SECONDS)
        } else {
            speech = "$dataConnectionTypeName connected"
        }
        speak(speech)
    }

    private fun onDataDisconnected(dataConnectionInfo: FooDataConnectionInfo) {
        FooLog.i(
            TAG,
            "onDataDisconnected(dataConnectionInfo=$dataConnectionInfo)"
        )

        val now = System.currentTimeMillis()

        val dataConnectionType = dataConnectionInfo.type

        mTimeDataDisconnected.put(dataConnectionType.toLong(), now)

        val speech: String
        val dataConnectionTypeName = getString(
            dataConnectionInfo.getNetworkTypeResourceId(
                BuildConfig.DEBUG
            )
        )
        //FooLog.i(TAG, "onDataDisconnected: dataConnectionTypeName == " + FooString.quote(dataConnectionTypeName));
        val timeDataConnectedMs = mTimeDataConnected.remove(dataConnectionType.toLong())
        if (timeDataConnectedMs != null) {
            val elapsedMs = now - timeDataConnectedMs
            speech = dataConnectionTypeName + " disconnected after being connected for " +
                    FooString.getTimeDurationString(applicationContext, elapsedMs, TimeUnit.SECONDS)
        } else {
            speech = "$dataConnectionTypeName disconnected"
        }
        speak(speech)
    }

    //
    // Volume (candidate for a dedicated class)
    //
    @Suppress("NAME_SHADOWING")
    private fun volumeObserverStart(audioStreamType: Int) {
        mAudioStreamVolumeObserver.attach(audioStreamType) {
            audioStreamType: Int, volume: Int, volumeMax: Int, volumePercent: Int ->
            this@AlfredManager.onAudioStreamVolumeChanged(
                audioStreamType,
                volume,
                volumeMax,
                volumePercent
            )
        }
    }

    private fun onAudioStreamVolumeChanged(
        audioStreamType: Int,
        volume: Int,
        volumeMax: Int,
        volumePercent: Int
    ) {
        //FooLog.d(TAG, "onAudioStreamVolumeChanged: MESSAGE_VOLUME_CHANGED audioStreamType volume");
        mHandler.removeMessages(Messages.VOLUME_CHANGED)
        mHandler.obtainAndSendMessageDelayed(
            Messages.VOLUME_CHANGED,
            audioStreamType,
            volumePercent,
            800
        )

        if (audioStreamType == textToSpeechManager.audioStreamType) {
            for (callbacks in mListenerManager.beginTraversing()) {
                callbacks.onTextToSpeechAudioStreamVolumeChanged(audioStreamType, volume)
            }
            mListenerManager.endTraversing()
        }
    }

    private interface Messages {
        companion object {
            /**
             *
             *  * msg.arg1: audioStreamType
             *  * msg.arg2: volumePercent
             *  * msg.obj: ?
             *
             */
            const val VOLUME_CHANGED: Int = 100
        }
    }

    private fun handleMessage(msg: Message): Boolean {
        when (msg.what) {
            Messages.VOLUME_CHANGED -> onAudioStreamVolumeChanged(msg)
        }
        return false
    }

    private fun onAudioStreamVolumeChanged(msg: Message) {
        val audioStreamType = msg.arg1
        //FooLog.v(TAG, "onAudioStreamVolumeChanged: audioStreamType == " + audioStreamType);
        val volumePercent = msg.arg2

        //FooLog.v(TAG, "onAudioStreamVolumeChanged: volume == " + volume);
        val audioStreamTypeName =
            FooAudioUtils.audioStreamTypeToString(applicationContext, audioStreamType)
        //String text = getString(R.string.alfred_X_volume_Y_of_Z, audioStreamTypeName, volume, volumeMax);
        val text = getString(R.string.alfred_X_volume_Y_percent, audioStreamTypeName, volumePercent)
        speak(text)
    }
}

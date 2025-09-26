package com.swooby.alfred

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.DialogFragment
import com.google.android.material.navigation.NavigationView
import com.smartfoo.android.core.FooString
import com.smartfoo.android.core.app.FooDebugActivity
import com.smartfoo.android.core.app.FooDebugConfiguration
import com.smartfoo.android.core.app.GenericPromptPositiveNegativeDialogFragment
import com.smartfoo.android.core.app.GenericPromptPositiveNegativeDialogFragment.GenericPromptPositiveNegativeDialogFragmentCallbacks
import com.smartfoo.android.core.logging.FooLog
import com.smartfoo.android.core.media.FooAudioStreamVolumeObserver
import com.smartfoo.android.core.media.FooAudioUtils
import com.smartfoo.android.core.notification.FooNotificationListenerManager
import com.smartfoo.android.core.notification.FooNotificationListenerManager.NotConnectedReason
import com.smartfoo.android.core.platform.FooPlatformUtils
import com.smartfoo.android.core.texttospeech.FooTextToSpeechHelper.IntentTextToSpeechSettings
import com.swooby.alfred.AlfredManager.AlfredManagerCallbacks
import com.swooby.alfred.TextToSpeechManager.TextToSpeechManagerCallbacks
import com.swooby.alfred.databinding.ActivityMainBinding
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

class MainActivity
    : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener,
    GenericPromptPositiveNegativeDialogFragmentCallbacks {

    companion object {
        private val TAG: String = FooLog.TAG(MainActivity::class.java)

        private const val REQUEST_ALFRED_MANAGER_REQUIRED_PERMISSIONS = 100
        private const val REQUEST_ACTION_CHECK_TTS_DATA = 101

        private const val FRAGMENT_DIALOG_NOTIFICATION_ACCESS_DISABLED =
            "FRAGMENT_DIALOG_NOTIFICATION_ACCESS_DISABLED"
    }

    private val mAlfredManagerCallbacks: AlfredManagerCallbacks = object : AlfredManagerCallbacks {
        override val activity: Activity
            get() = this@MainActivity

        override fun onAlfredPermissionsRequired(permissions: Set<String>): Boolean {
            return this@MainActivity.onAlfredPermissionsRequired(permissions)
        }

        override fun onActivityAlfredPermissionGranted(permission: String): Boolean {
            return this@MainActivity.onActivityAlfredPermissionGranted(permission)
        }

        override fun onNotificationListenerConnected() {
            this@MainActivity.onNotificationListenerConnected()
        }

        override fun onNotificationListenerNotConnected(reason: NotConnectedReason): Boolean {
            return this@MainActivity.onNotificationListenerNotConnected(reason, true)
        }

        override fun onProfileEnabled(profile: Profile) {
        }

        override fun onProfileDisabled(profile: Profile) {
        }

        override fun onTextToSpeechAudioStreamVolumeChanged(audioStreamType: Int, volume: Int) {
            this@MainActivity.onTextToSpeechAudioStreamVolumeChanged(
                audioStreamType,
                volume,
                updateSeekbar = true,
                updateStreamVolume = false
            )
        }
    }

    private val mTextToSpeechManagerCallbacks: TextToSpeechManagerCallbacks =
        object : TextToSpeechManagerCallbacks() {
            override fun onTextToSpeechInitialized(status: Int) {
                super.onTextToSpeechInitialized(status)
                this@MainActivity.onTextToSpeechInitialized(status)
            }
        }

    private lateinit var mMainApplication: MainApplication
    private lateinit var mDebugConfiguration: FooDebugConfiguration
    private lateinit var mAlfredManager: AlfredManager
    private lateinit var mTextToSpeechManager: TextToSpeechManager
    private lateinit var mProfileManager: ProfileManager
    private lateinit var mNotificationParserManager: NotificationParserManager

    private lateinit var mAudioManager: AudioManager
    private lateinit var mAudioStreamVolumeObserver: FooAudioStreamVolumeObserver

    private lateinit var mDrawerLayout: DrawerLayout
    private lateinit var mDrawerToggle: ActionBarDrawerToggle
    private lateinit var mNavigationView: NavigationView

    private lateinit var binding: ActivityMainBinding

    private lateinit var mSpinnerTextToSpeechVoices: UserTouchSpinner
    private lateinit var mSeekbarTextToSpeechVoiceSpeed: SeekBar
    private lateinit var mSeekbarTextToSpeechVoicePitch: SeekBar
    private lateinit var mSpinnerTextToSpeechAudioStreamType: UserTouchSpinner
    private lateinit var mSeekbarTextToSpeechAudioStreamVolume: SeekBar
    private lateinit var mSpinnerProfiles: UserTouchSpinner
    private lateinit var mButtonNotificationListenerSettings: Button
    private lateinit var mButtonProcessNotifications: Button

    private var mIsRequestingRequiredPermissions = false
    private var mHasRequestedTextToSpeechData = false

    private val isDebugEnabled: Boolean
        get() = mDebugConfiguration.isDebugEnabled

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mMainApplication = MainApplication.getMainApplication(this)
        mDebugConfiguration = mMainApplication.fooDebugConfiguration
        mAlfredManager = mMainApplication.alfredManager
        mTextToSpeechManager = mAlfredManager.textToSpeechManager
        mProfileManager = mAlfredManager.profileManager
        mNotificationParserManager = mAlfredManager.notificationParserManager

        mAudioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        val intent = intent
        FooLog.v(TAG, "onCreate: intent=" + FooPlatformUtils.toString(intent))

        val intentAction = intent.action
        FooLog.v(TAG, "onCreate: intentAction=" + FooString.quote(intentAction))

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)
        val actionbar = supportActionBar
        if (actionbar != null) {
            actionbar.setHomeButtonEnabled(true)
            actionbar.setDisplayHomeAsUpEnabled(true)
        }

        mDrawerLayout = binding.drawerLayout
        mDrawerToggle = object : ActionBarDrawerToggle(
            this,
            mDrawerLayout,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        ) {
            override fun onDrawerOpened(drawerView: View) {
                super.onDrawerOpened(drawerView)
                invalidateOptionsMenu()
            }

            override fun onDrawerClosed(drawerView: View) {
                super.onDrawerClosed(drawerView)
                invalidateOptionsMenu()
            }
        }
        mDrawerToggle.isDrawerIndicatorEnabled = true
        mDrawerLayout.addDrawerListener(mDrawerToggle)
        mDrawerToggle.syncState()

        mNavigationView = binding.navView
        mNavigationView.setNavigationItemSelectedListener(this)

        /*
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main2025);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
        */

        mSpinnerTextToSpeechVoices =
            binding.appBarMain.activityMainContent.spinnerTextToSpeechVoices
        val buttonTextToSpeechVoicesRefresh =
            binding.appBarMain.activityMainContent.buttonTextToSpeechVoicesRefresh
        buttonTextToSpeechVoicesRefresh.setOnClickListener { _: View? ->
            textToSpeechVoicesUpdate()
        }
        val buttonTextToSpeechVoicesTest: Button =
            binding.appBarMain.activityMainContent.buttonTextToSpeechTest
        buttonTextToSpeechVoicesTest.setOnClickListener { _: View? -> textToSpeechTest() }
        val buttonTextToSpeechStop: Button =
            binding.appBarMain.activityMainContent.buttonTextToSpeechStopClear
        buttonTextToSpeechStop.setOnClickListener { _: View? -> mTextToSpeechManager.clear() }

        mSeekbarTextToSpeechVoiceSpeed =
            binding.appBarMain.activityMainContent.seekbarTextToSpeechVoiceSpeed
        mSeekbarTextToSpeechVoiceSpeed.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val voiceSpeed = getRateFromSeekBarProgress(seekBar)
                    mTextToSpeechManager.voiceSpeed = voiceSpeed
                    textToSpeechTest()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
            }
        })

        mSeekbarTextToSpeechVoicePitch =
            binding.appBarMain.activityMainContent.seekbarTextToSpeechVoicePitch
        mSeekbarTextToSpeechVoicePitch.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val voicePitch = getRateFromSeekBarProgress(seekBar)
                    mTextToSpeechManager.voicePitch = voicePitch
                    textToSpeechTest()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
            }
        })

        mSpinnerTextToSpeechAudioStreamType =
            binding.appBarMain.activityMainContent.spinnerTextToSpeechAudioStreamType
        val textToSpeechAudioStreamTypes = AudioStreamType.getTypes(this)
        val textToSpeechAudioStreamTypeAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            textToSpeechAudioStreamTypes
        )
        mSpinnerTextToSpeechAudioStreamType.adapter = textToSpeechAudioStreamTypeAdapter
        mSpinnerTextToSpeechAudioStreamType.setOnItemSelectedListener(object : UserTouchSpinner.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long,
                fromUser: Boolean
            ) {
                if (fromUser) {
                    val audioStreamType = parent.adapter.getItem(position) as AudioStreamType
                    val textToSpeechAudioStreamType = audioStreamType.audioStreamType
                    onTextToSpeechAudioStreamTypeChanged(textToSpeechAudioStreamType)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>, fromUser: Boolean) {
            }
        })

        mSeekbarTextToSpeechAudioStreamVolume =
            binding.appBarMain.activityMainContent.seekbarTextToSpeechAudioStreamVolume
        mSeekbarTextToSpeechAudioStreamVolume.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                onTextToSpeechAudioStreamVolumeChanged(progress, false, fromUser)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
            }
        })

        mSpinnerProfiles = binding.appBarMain.activityMainContent.spinnerProfiles
        val profiles = mProfileManager.profiles
        val profilesAdapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, profiles)
        mSpinnerProfiles.adapter = profilesAdapter
        mSpinnerProfiles.setOnItemSelectedListener(object : UserTouchSpinner.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long,
                fromUser: Boolean
            ) {
                if (fromUser) {
                    val profile = parent.adapter.getItem(position) as Profile
                    mProfileManager.setProfileToken(profile.token)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>, fromUser: Boolean) {
            }
        })

        mButtonNotificationListenerSettings =
            binding.appBarMain.activityMainContent.buttonNotificationListenerSettings
        mButtonNotificationListenerSettings.visibility = View.GONE
        mButtonNotificationListenerSettings.setOnClickListener { _: View? -> startActivityNotificationListenerSettings() }

        mButtonProcessNotifications =
            binding.appBarMain.activityMainContent.buttonProcessNotifications
        mButtonProcessNotifications.visibility = View.GONE
        mButtonProcessNotifications.setOnClickListener { _: View? -> mNotificationParserManager.initializeActiveNotifications() }

        /*
        binding.appBarMain.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null)
                        .setAnchorView(R.id.fab).show();
                MainActivity.this.onFloatingActionButtonClick();
            }
        });
        */

        if (savedInstanceState == null) {
            verifyRequirements()
        } else {
            loadSavedInstanceState(savedInstanceState)
        }
    }

    private fun loadSavedInstanceState(savedInstanceState: Bundle) {
        mIsRequestingRequiredPermissions = savedInstanceState.getBoolean("mIsRequestingRequiredPermissions")
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        outState.putBoolean("mIsRequestingRequiredPermissions", mIsRequestingRequiredPermissions)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        mDrawerToggle.syncState()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        mDrawerToggle.onConfigurationChanged(newConfig)
    }

    override fun onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    /*
    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main2025);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
    */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_main, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val isDebugEnabled = true // TODO: Preferences...isDebugEnabled
        val isLoggingEnabled = FooLog.isEnabled()

        var menuItem = menu.findItem(R.id.action_development_settings)
        menuItem?.setVisible(isDebugEnabled)

        menuItem = menu.findItem(R.id.action_adb_wireless_settings)
        menuItem?.setVisible(isDebugEnabled)

        menuItem = menu.findItem(R.id.action_notification_access)
        menuItem?.setVisible(FooNotificationListenerManager.supportsNotificationListenerSettings())

        menuItem = menu.findItem(R.id.action_debug_show_debug_log)
        menuItem?.setVisible(isLoggingEnabled)

        menuItem = menu.findItem(R.id.action_debug_clear_debug_log)
        menuItem?.setVisible(isLoggingEnabled)

        //…
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        val isDebugEnabled = isDebugEnabled

        val itemId = item.itemId
        when (itemId) {
            android.R.id.home -> {
                mDrawerLayout.openDrawer(GravityCompat.START)
                return true
            }
            //R.id.action_settings:
            //    // TODO:(pv) …
            //    return true;
            //R.id.menu_refresh:
            //    refreshItemsFromTable();
            //    return true;
            R.id.action_application_info -> {
                FooPlatformUtils.showAppSettings(this)
                return true
            }
            R.id.action_development_settings -> {
                FooPlatformUtils.showDevelopmentSettings(this)
                return true
            }
            R.id.action_adb_wireless_settings -> {
                FooPlatformUtils.showAdbWirelessSettings(this)
                return true
            }
            R.id.action_notification_access -> {
                startActivityNotificationListenerSettings()
                return true
            }
            R.id.action_text_to_speech -> {
                startActivity(IntentTextToSpeechSettings)
                return true
            }
            R.id.action_debug_show_debug_log -> {
                val username: String? = null
                val intent = Intent(
                    this,
                    FooDebugActivity::class.java
                )
                intent.putExtras(FooDebugActivity.makeExtras(null, username))
                startActivity(intent)
                return true
            }
            R.id.action_debug_clear_debug_log -> {
                FooLog.clear()
                return true
            }
        }

        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        val id = item.itemId

        /*
        if (id == R.id.nav_camera)
        {
            // Handle the camera action
        }
        else if (id == R.id.nav_gallery)
        {

        }
        else if (id == R.id.nav_slideshow)
        {

        }
        else if (id == R.id.nav_manage)
        {

        }
        else if (id == R.id.nav_share)
        {

        }
        else if (id == R.id.nav_send)
        {

        }
        */
        mDrawerLayout.closeDrawer(GravityCompat.START)

        return true
    }

    override fun onResume() {
        FooLog.v(TAG, "+onResume()")
        super.onResume()

        mAlfredManager.attach(mAlfredManagerCallbacks)
        mTextToSpeechManager.attach(mTextToSpeechManagerCallbacks)

        if (mNotificationParserManager.isNotificationAccessSettingConfirmedEnabled) {
            if (mNotificationParserManager.isNotificationListenerConnected) {
                onNotificationListenerConnected()
            }
        } else {
            onNotificationListenerNotConnected(NotConnectedReason.ConfirmedNotEnabled, true)
        }

        textToSpeechVoiceUpdate()
        textToSpeechAudioStreamTypeUpdate()

        profilesUpdate()

        FooLog.v(TAG, "-onResume()")
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQUEST_ALFRED_MANAGER_REQUIRED_PERMISSIONS -> {
                for (i in permissions.indices) {
                    val permission = permissions[i]
                    val isGranted = grantResults[i] == PackageManager.PERMISSION_GRANTED
                    if (isGranted) {
                        onActivityAlfredPermissionGranted(permission)
                    } else {
                        onActivityAlfredPermissionDenied(permission)
                    }
                }
                for (permission in permissions) {
                    if (mAlfredManager.onActivityPermissionGranted(permission)) {
                        break;
                    }
                }
            }
        }
    }

    private fun onAlfredPermissionsRequired(permissions: Set<String>): Boolean {
        val forceRequest = !mIsRequestingRequiredPermissions
        return requestRequiredPermissions(permissions, force = forceRequest)
    }

    private fun onActivityAlfredPermissionGranted(permission: String): Boolean {
        FooLog.i(TAG, "onActivityAlfredPermissionGranted(permission=${FooString.quote(permission)}")
        val allRequiredPermissionsGranted = mAlfredManager.onActivityPermissionGranted(permission)
        mIsRequestingRequiredPermissions = !allRequiredPermissionsGranted
        return allRequiredPermissionsGranted
    }

    private fun onActivityAlfredPermissionDenied(permission: String) {
        FooLog.w(TAG, "onActivityAlfredPermissionDenied(permission=${FooString.quote(permission)}")
        mIsRequestingRequiredPermissions = true
    }

    private fun requestRequiredPermissions(permissions: Set<String>, force: Boolean = false): Boolean {
        if (permissions.isEmpty()) {
            return false
        }

        if (!force && mIsRequestingRequiredPermissions) {
            return false
        }

        mIsRequestingRequiredPermissions = true

        FooLog.i(TAG, "requestMandatoryPermissionsIfNeeded: requesting")
        ActivityCompat.requestPermissions(
            this,
            permissions.toTypedArray(),
            REQUEST_ALFRED_MANAGER_REQUIRED_PERMISSIONS
        )

        return true
    }

    private fun textToSpeechTest() {
        mAlfredManager.speak(true, "Testing testing 1 2 3")
        //mAlfredManager.speakGreeting()
    }

    private fun textToSpeechVoiceUpdate() {
        val voiceSpeed = mTextToSpeechManager.voiceSpeed
        setSeekBarProgressFromRate(mSeekbarTextToSpeechVoiceSpeed, voiceSpeed)

        val voicePitch = mTextToSpeechManager.voicePitch
        setSeekBarProgressFromRate(mSeekbarTextToSpeechVoicePitch, voicePitch)
    }

    private fun getRateFromSeekBarProgress(seekBar: SeekBar): Float {
        val seekBarProgress = seekBar.progress

        val rate = seekBarProgress * 0.1f

        return rate
    }

    private fun setSeekBarProgressFromRate(seekBar: SeekBar, rate: Float) {
        var seekBarProgress = Math.round(rate * 10)

        val seekBarMax = seekBar.max.toFloat()

        seekBarProgress = max(1.0, min(seekBarProgress.toDouble(), seekBarMax.toDouble())).toInt()

        seekBar.progress = seekBarProgress
    }

    override fun onPause() {
        FooLog.v(TAG, "+onPause()")
        super.onPause()

        mAlfredManager.detach(mAlfredManagerCallbacks)
        mTextToSpeechManager.detach(mTextToSpeechManagerCallbacks)

        FooLog.v(TAG, "-onPause()")
    }

    private fun onTextToSpeechInitialized(status: Int) {
        FooLog.v(TAG, "onTextToSpeechInitialized(status=$status)")
        if (status != TextToSpeech.SUCCESS) {
            FooLog.e(TAG, "TODO:(pv) Report error and exit the app")
            return
        }
        if (!mHasRequestedTextToSpeechData) {
            mHasRequestedTextToSpeechData = true
            mTextToSpeechManager.requestTextToSpeechData(this, REQUEST_ACTION_CHECK_TTS_DATA)
        }
    }

    private fun startActivityNotificationListenerSettings() {
        mNotificationParserManager.startActivityNotificationListenerSettings(this)
    }

    private fun textToSpeechAudioStreamTypeUpdate(): Int {
        @Suppress("UNCHECKED_CAST")
        val audioStreamTypeAdapter =
            mSpinnerTextToSpeechAudioStreamType.adapter as ArrayAdapter<AudioStreamType>

        var selectedIndex = -1

        val textToSpeechAudioStreamType = mTextToSpeechManager.audioStreamType
        FooLog.v(
            TAG, "textToSpeechAudioStreamTypeUpdate: textToSpeechAudioStreamType=" +
                    FooAudioUtils.audioStreamTypeToString(textToSpeechAudioStreamType)
        )

        for (i in 0..<audioStreamTypeAdapter.count) {
            val audioStreamType = audioStreamTypeAdapter.getItem(i)
            if (audioStreamType != null && audioStreamType.audioStreamType == textToSpeechAudioStreamType) {
                selectedIndex = i
                break
            }
        }

        if (selectedIndex != -1 && selectedIndex != mSpinnerTextToSpeechAudioStreamType.selectedItemPosition) {
            mSpinnerTextToSpeechAudioStreamType.setSelection(selectedIndex)
        }

        onTextToSpeechAudioStreamTypeChanged(textToSpeechAudioStreamType)

        return textToSpeechAudioStreamType
    }

    private fun onTextToSpeechAudioStreamTypeChanged(textToSpeechAudioStreamType: Int) {
        volumeControlStream = textToSpeechAudioStreamType

        mTextToSpeechManager.audioStreamType = textToSpeechAudioStreamType

        val volume = FooAudioUtils.getVolumeAbsolute(mAudioManager, textToSpeechAudioStreamType)
        onTextToSpeechAudioStreamVolumeChanged(textToSpeechAudioStreamType, volume,
            updateSeekbar = true,
            updateStreamVolume = false
        )
    }

    private fun onTextToSpeechAudioStreamVolumeChanged(
        volume: Int,
        updateSeekbar: Boolean,
        updateStreamVolume: Boolean
    ) {
        val audioStreamType = mTextToSpeechManager.audioStreamType
        onTextToSpeechAudioStreamVolumeChanged(
            audioStreamType,
            volume,
            updateSeekbar,
            updateStreamVolume
        )
    }

    private fun onTextToSpeechAudioStreamVolumeChanged(
        audioStreamType: Int,
        volume: Int,
        updateSeekbar: Boolean,
        updateStreamVolume: Boolean
    ) {
        if (updateSeekbar) {
            val volumeMax = mAudioManager.getStreamMaxVolume(audioStreamType)
            mSeekbarTextToSpeechAudioStreamVolume.max = volumeMax
            mSeekbarTextToSpeechAudioStreamVolume.progress = volume
        }

        if (updateStreamVolume) {
            mAudioManager.setStreamVolume(audioStreamType, volume, 0)
        }
    }

    private fun profilesUpdate() {
        @Suppress("UNCHECKED_CAST")
        val profileAdapter = mSpinnerProfiles.adapter as ArrayAdapter<Profile>
        profileAdapter.sort(Profile.COMPARATOR)

        var selectedIndex = -1

        val profile = mProfileManager.profile

        for (i in 0..<profileAdapter.count) {
            if (profile == profileAdapter.getItem(i)) {
                selectedIndex = i
                break
            }
        }

        if (selectedIndex != -1) {
            mSpinnerProfiles.setSelection(selectedIndex)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        FooLog.v(TAG, "onActivityResult(...)")
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_ACTION_CHECK_TTS_DATA -> {
                when (resultCode) {
                    TextToSpeech.Engine.CHECK_VOICE_DATA_PASS -> {
                        textToSpeechVoicesUpdate()
                    }
                }
            }
        }
    }

    private class VoiceWrapper(val voice: Voice) : Comparable<VoiceWrapper> {
        private val mDisplayName = voice.name.lowercase(Locale.getDefault())

        override fun toString(): String {
            return mDisplayName
        }

        fun equals(o: VoiceWrapper): Boolean {
            return compareTo(o) == 0
        }

        fun equals(o: Voice): Boolean {
            return compareTo(o) == 0
        }

        fun equals(o: String): Boolean {
            return compareTo(o) == 0
        }

        override fun equals(other: Any?): Boolean {
            if (other is VoiceWrapper) {
                return equals(other)
            }

            if (other is Voice) {
                return equals(other)
            }

            if (other is String) {
                return equals(other)
            }

            return super.equals(other)
        }

        override fun hashCode(): Int {
            return javaClass.hashCode()
        }

        override fun compareTo(other: VoiceWrapper): Int {
            return compareTo(other.mDisplayName)
        }

        fun compareTo(other: Voice): Int {
            return compareTo(other.name)
        }

        fun compareTo(other: String): Int {
            val result = mDisplayName.compareTo(other.lowercase(Locale.getDefault()))
            //FooLog.e(TAG, FooString.quote(mDisplayName) + ".compareTo(" + FooString.quote(other) + ") == " + result);
            return result
        }
    }

    private fun textToSpeechVoicesUpdate() {
        val availableVoices = ArrayList<VoiceWrapper>()
        val voices = mTextToSpeechManager.voices
        if (voices != null) {
            for (voice in voices) {
                val voiceFeatures = voice.features
                //FooLog.e(TAG, "onActivityResult: voiceFeatures=" + voiceFeatures);
                if (voiceFeatures.contains("notInstalled")) {
                    continue
                }

                val voiceWrapper = VoiceWrapper(voice)

                availableVoices.add(voiceWrapper)
            }
        }
        availableVoices.sort()

        val currentVoiceName = mTextToSpeechManager.voiceName
        var currentVoiceIndex = 0
        for (i in availableVoices.indices) {
            val voiceWrapper = availableVoices[i]
            if (voiceWrapper.equals(currentVoiceName)) {
                currentVoiceIndex = i
                break
            }
        }

        val spinnerVoicesAdapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, availableVoices)
        mSpinnerTextToSpeechVoices.adapter = spinnerVoicesAdapter
        mSpinnerTextToSpeechVoices.setSelection(currentVoiceIndex)
        mSpinnerTextToSpeechVoices.setOnItemSelectedListener(object : UserTouchSpinner.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long,
                fromUser: Boolean
            ) {
                if (fromUser) {
                    val voiceWrapper = parent.adapter.getItem(position) as VoiceWrapper
                    val voice = voiceWrapper.voice
                    mTextToSpeechManager.setVoice(voice)
                    textToSpeechTest()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>, fromUser: Boolean) {
            }
        })
    }

    private fun verifyRequirements() {
        //...
    }

    private fun onNotificationListenerConnected() {
        FooLog.i(TAG, "onNotificationListenerConnected()")

        mButtonNotificationListenerSettings.visibility = View.GONE
        mButtonProcessNotifications.visibility = View.VISIBLE

        val fm = supportFragmentManager
        val dialogFragment =
            fm.findFragmentByTag(FRAGMENT_DIALOG_NOTIFICATION_ACCESS_DISABLED) as DialogFragment?
        dialogFragment?.dismiss()
    }

    private fun onNotificationListenerNotConnected(
        reason: NotConnectedReason,
        showDialog: Boolean
    ): Boolean {
        FooLog.w(
            TAG, "onNotificationListenerNotConnected(reason=" + reason +
                    ", showDialog=" + showDialog + ')'
        )

        mButtonProcessNotifications.visibility = View.GONE
        mButtonNotificationListenerSettings.visibility = View.VISIBLE

        if (!showDialog) {
            return false
        }

        val title = mAlfredManager.getNotificationListenerNotConnectedTitle(reason)
        val message = mAlfredManager.getNotificationListenerNotConnectedMessage(reason)

        val fm = supportFragmentManager
        var dialogFragment = fm
            .findFragmentByTag(FRAGMENT_DIALOG_NOTIFICATION_ACCESS_DISABLED) as GenericPromptPositiveNegativeDialogFragment?
        if (dialogFragment != null) {
            if (title != dialogFragment.title || message != dialogFragment.message) {
                dialogFragment.dismiss()
                dialogFragment = null
            }
        }

        if (dialogFragment == null) {
            // TODO:(pv) 3rd button for option to immediately reboot phone…

            dialogFragment = GenericPromptPositiveNegativeDialogFragment.newInstance(title, message)
            dialogFragment.show(fm, FRAGMENT_DIALOG_NOTIFICATION_ACCESS_DISABLED)
        }

        return true
    }

    override fun onGenericPromptPositiveNegativeDialogFragmentResult(dialogFragment: GenericPromptPositiveNegativeDialogFragment): Boolean {
        when (dialogFragment.result) {
            GenericPromptPositiveNegativeDialogFragment.Result.Positive -> startActivityNotificationListenerSettings()
            else -> {}
        }
        return false
    }

    /*
    private void onFloatingActionButtonClick()
    {
        /*
        Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null)
                .show();
        */
        //mMainApplication.startScanning();
    }
    */
}

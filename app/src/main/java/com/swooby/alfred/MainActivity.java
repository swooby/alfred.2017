package com.swooby.alfred;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.NavigationView.OnNavigationItemSelectedListener;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;

import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.app.FooDebugActivity;
import com.smartfoo.android.core.app.FooDebugConfiguration;
import com.smartfoo.android.core.app.GenericPromptPositiveNegativeDialogFragment;
import com.smartfoo.android.core.app.GenericPromptPositiveNegativeDialogFragment.GenericPromptPositiveNegativeDialogFragmentCallbacks;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.media.FooAudioStreamVolumeObserver;
import com.smartfoo.android.core.media.FooAudioUtils;
import com.smartfoo.android.core.notification.FooNotificationListenerManager;
import com.smartfoo.android.core.notification.FooNotificationListenerManager.NotConnectedReason;
import com.smartfoo.android.core.platform.FooPlatformUtils;
import com.smartfoo.android.core.texttospeech.FooTextToSpeechHelper;
import com.swooby.alfred.AlfredManager.AlfredManagerCallbacks;
import com.swooby.alfred.TextToSpeechManager.TextToSpeechManagerCallbacks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

public class MainActivity
        extends AppCompatActivity
        implements OnNavigationItemSelectedListener,
        GenericPromptPositiveNegativeDialogFragmentCallbacks
{
    private static final String TAG = FooLog.TAG(MainActivity.class);

    private static final int REQUEST_ACTION_CHECK_TTS_DATA = 100;

    private static final String FRAGMENT_DIALOG_NOTIFICATION_ACCESS_DISABLED = "FRAGMENT_DIALOG_NOTIFICATION_ACCESS_DISABLED";

    private final AlfredManagerCallbacks mMainApplicationCallbacks = new AlfredManagerCallbacks()
    {
        @Override
        public Activity getActivity()
        {
            return MainActivity.this;
        }

        @Override
        public void onNotificationListenerConnected()
        {
            MainActivity.this.onNotificationListenerConnected();
        }

        @Override
        public boolean onNotificationListenerNotConnected(NotConnectedReason reason)
        {
            return MainActivity.this.onNotificationListenerNotConnected(reason, true);
        }

        @Override
        public void onProfileEnabled(String profileToken)
        {
        }

        @Override
        public void onProfileDisabled(String profileToken)
        {
        }

        @Override
        public void onTextToSpeechAudioStreamVolumeChanged(int audioStreamType, int volume)
        {
            MainActivity.this.onTextToSpeechAudioStreamVolumeChanged(audioStreamType, volume, true, false);
        }
    };

    private final TextToSpeechManagerCallbacks mTextToSpeechManagerCallbacks = new TextToSpeechManagerCallbacks()
    {
        @Override
        public void onTextToSpeechInitialized(int status)
        {
            super.onTextToSpeechInitialized(status);
            MainActivity.this.onTextToSpeechInitialized(status);
        }
    };

    private MainApplication           mMainApplication;
    private FooDebugConfiguration     mDebugConfiguration;
    private AlfredManager             mAlfredManager;
    private TextToSpeechManager       mTextToSpeechManager;
    private ProfileManager            mProfileManager;
    private NotificationParserManager mNotificationParserManager;

    private AudioManager                 mAudioManager;
    private FooAudioStreamVolumeObserver mAudioStreamVolumeObserver;

    private DrawerLayout          mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private NavigationView        mNavigationView;

    private Spinner mSpinnerTextToSpeechVoices;
    private Spinner mSpinnerTextToSpeechAudioStreamType;
    private SeekBar mSeekbarTextToSpeechAudioStreamVolume;
    private Spinner mSpinnerProfiles;
    private Button  mButtonNotificationListenerSettings;
    private Button  mButtonProcessNotifications;

    private boolean mRequestedTextToSpeechData;

    protected boolean isDebugEnabled()
    {
        return mDebugConfiguration.isDebugEnabled();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mMainApplication = MainApplication.getMainApplication(this);
        mDebugConfiguration = mMainApplication.getFooDebugConfiguration();
        mAlfredManager = mMainApplication.getAlfredManager();
        mTextToSpeechManager = mAlfredManager.getTextToSpeechManager();
        mProfileManager = mAlfredManager.getProfileManager();
        mNotificationParserManager = mAlfredManager.getNotificationParserManager();

        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        Intent intent = getIntent();
        FooLog.v(TAG, "onCreate: intent=" + FooPlatformUtils.toString(intent));

        String intentAction = intent.getAction();
        FooLog.v(TAG, "onCreate: intentAction=" + FooString.quote(intentAction));

        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        if (actionbar != null)
        {
            actionbar.setHomeButtonEnabled(true);
            actionbar.setDisplayHomeAsUpEnabled(true);
        }

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        {
            @Override
            public void onDrawerOpened(View drawerView)
            {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu();
            }

            @Override
            public void onDrawerClosed(View drawerView)
            {
                super.onDrawerClosed(drawerView);
                invalidateOptionsMenu();
            }
        };
        mDrawerToggle.setDrawerIndicatorEnabled(true);
        mDrawerLayout.addDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();

        mNavigationView = (NavigationView) findViewById(R.id.nav_view);
        if (mNavigationView != null)
        {
            mNavigationView.setNavigationItemSelectedListener(this);
        }

        mSpinnerTextToSpeechVoices = (Spinner) findViewById(R.id.spinnerTextToSpeechVoices);

        mSpinnerTextToSpeechAudioStreamType = (Spinner) findViewById(R.id.spinnerTextToSpeechAudioStreamType);
        ArrayList<AudioStreamType> textToSpeechAudioStreamTypes = AudioStreamType.getTypes(this);
        ArrayAdapter textToSpeechAudioStreamTypeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, textToSpeechAudioStreamTypes);
        mSpinnerTextToSpeechAudioStreamType.setAdapter(textToSpeechAudioStreamTypeAdapter);
        mSpinnerTextToSpeechAudioStreamType.setOnItemSelectedListener(new OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                AudioStreamType audioStreamType = (AudioStreamType) parent.getAdapter().getItem(position);
                int textToSpeechAudioStreamType = audioStreamType.getAudioStreamType();

                onTextToSpeechAudioStreamTypeChanged(textToSpeechAudioStreamType);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {
            }
        });

        ImageButton buttonTextToSpeechAudioStreamTypeTest = (ImageButton) findViewById(R.id.buttonTextToSpeechAudioStreamTypeTest);
        if (buttonTextToSpeechAudioStreamTypeTest != null)
        {
            buttonTextToSpeechAudioStreamTypeTest.setOnClickListener(new OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    mTextToSpeechManager.speak("Testing testing 1 2 3");
                }
            });
        }

        mSeekbarTextToSpeechAudioStreamVolume = (SeekBar) findViewById(R.id.seekbarTextToSpeechAudioStreamVolume);
        mSeekbarTextToSpeechAudioStreamVolume.setOnSeekBarChangeListener(new OnSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                onTextToSpeechAudioStreamVolumeChanged(progress, false, fromUser);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar)
            {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {
            }
        });

        mSpinnerProfiles = (Spinner) findViewById(R.id.spinnerProfiles);
        ArrayList<Profile> profiles = mProfileManager.getProfiles();
        ArrayAdapter profilesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, profiles);
        mSpinnerProfiles.setAdapter(profilesAdapter);
        mSpinnerProfiles.setOnItemSelectedListener(new OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                Profile profile = (Profile) parent.getAdapter().getItem(position);
                mProfileManager.setProfileToken(profile.getToken());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {
            }
        });

        mButtonNotificationListenerSettings = (Button) findViewById(R.id.buttonNotificationListenerSettings);
        mButtonNotificationListenerSettings.setVisibility(View.GONE);
        mButtonNotificationListenerSettings.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                startActivityNotificationListenerSettings();
            }
        });

        mButtonProcessNotifications = (Button) findViewById(R.id.buttonProcessNotifications);
        mButtonProcessNotifications.setVisibility(View.GONE);
        mButtonProcessNotifications.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                mNotificationParserManager.initializeActiveNotifications();
            }
        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        if (fab != null)
        {
            fab.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    MainActivity.this.onFloatingActionButtonClick();
                }
            });
        }

        if (savedInstanceState == null)
        {
            verifyRequirements();
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState)
    {
        super.onPostCreate(savedInstanceState);
        if (mDrawerToggle != null)
        {
            mDrawerToggle.syncState();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        if (mDrawerToggle != null)
        {
            mDrawerToggle.onConfigurationChanged(newConfig);
        }
    }

    @Override
    public void onBackPressed()
    {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer != null)
        {
            if (drawer.isDrawerOpen(GravityCompat.START))
            {
                drawer.closeDrawer(GravityCompat.START);
            }
            else
            {
                super.onBackPressed();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        boolean isLoggingEnabled = FooLog.isEnabled();

        MenuItem menuItem;

        menuItem = menu.findItem(R.id.action_notification_access);
        if (menuItem != null)
        {
            menuItem.setVisible(FooNotificationListenerManager.supportsNotificationListenerSettings());
        }

        menuItem = menu.findItem(R.id.action_debug_show_debug_log);
        if (menuItem != null)
        {
            menuItem.setVisible(isLoggingEnabled);
        }

        menuItem = menu.findItem(R.id.action_debug_clear_debug_log);
        if (menuItem != null)
        {
            menuItem.setVisible(isLoggingEnabled);
        }

        //…

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        boolean isDebugEnabled = isDebugEnabled();

        switch (item.getItemId())
        {
            case android.R.id.home:
                if (mDrawerLayout != null)
                {
                    mDrawerLayout.openDrawer(GravityCompat.START);
                    return true;
                }
                break;
            //case R.id.action_settings:
            //    // TODO:(pv) …
            //    return true;
            //case R.id.menu_refresh:
            //    refreshItemsFromTable();
            //    return true;
            case R.id.action_application_info:
                FooPlatformUtils.showAppSettings(this);
                return true;
            case R.id.action_notification_access:
                startActivityNotificationListenerSettings();
                return true;
            case R.id.action_text_to_speech:
                startActivity(FooTextToSpeechHelper.getIntentTextToSpeechSettings());
                return true;
            case R.id.action_debug_show_debug_log:
            {
                String username = null;

                Intent intent = new Intent(this, FooDebugActivity.class);
                intent.putExtras(FooDebugActivity.makeExtras(null, username));

                startActivity(intent);

                return true;
            }
            case R.id.action_debug_clear_debug_log:
                FooLog.clear();
                break;
        }

        if (mDrawerToggle != null)
        {
            if (mDrawerToggle.onOptionsItemSelected(item))
            {
                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item)
    {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

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

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer != null)
        {
            drawer.closeDrawer(GravityCompat.START);
        }
        return true;
    }

    @Override
    protected void onResume()
    {
        FooLog.v(TAG, "+onResume()");
        super.onResume();

        mAlfredManager.attach(mMainApplicationCallbacks);
        mTextToSpeechManager.attach(mTextToSpeechManagerCallbacks);

        if (mNotificationParserManager.isNotificationAccessSettingConfirmedNotEnabled())
        {
            onNotificationListenerNotConnected(NotConnectedReason.ConfirmedNotEnabled, false);
        }
        else if (mNotificationParserManager.isNotificationListenerConnected())
        {
            onNotificationListenerConnected();
        }

        textToSpeechAudioStreamTypeUpdate();

        profilesUpdate();

        FooLog.v(TAG, "-onResume()");
    }

    @Override
    protected void onPause()
    {
        FooLog.v(TAG, "+onPause()");
        super.onPause();

        mAlfredManager.detach(mMainApplicationCallbacks);
        mTextToSpeechManager.detach(mTextToSpeechManagerCallbacks);


        FooLog.v(TAG, "-onPause()");
    }

    private void onTextToSpeechInitialized(int status)
    {
        FooLog.v(TAG, "onTextToSpeechInitialized(status=" + status + ')');
        if (status != TextToSpeech.SUCCESS)
        {
            FooLog.e(TAG, "TODO:(pv) Report error and exit the app");
            return;
        }
        if (mRequestedTextToSpeechData)
        {
            return;
        }
        mRequestedTextToSpeechData = true;
        mTextToSpeechManager.requestTextToSpeechData(this, REQUEST_ACTION_CHECK_TTS_DATA);
    }

    private void startActivityNotificationListenerSettings()
    {
        mNotificationParserManager.startActivityNotificationListenerSettings();
    }

    private int textToSpeechAudioStreamTypeUpdate()
    {
        //noinspection unchecked
        ArrayAdapter<AudioStreamType> audioStreamTypeAdapter =
                (ArrayAdapter<AudioStreamType>) mSpinnerTextToSpeechAudioStreamType.getAdapter();

        int selectedIndex = -1;

        int textToSpeechAudioStreamType = mTextToSpeechManager.getAudioStreamType();
        FooLog.v(TAG, "textToSpeechAudioStreamTypeUpdate: textToSpeechAudioStreamType=" +
                      FooAudioUtils.audioStreamTypeToString(textToSpeechAudioStreamType));

        for (int i = 0; i < audioStreamTypeAdapter.getCount(); i++)
        {
            AudioStreamType audioStreamType = audioStreamTypeAdapter.getItem(i);
            if (audioStreamType != null && audioStreamType.getAudioStreamType() == textToSpeechAudioStreamType)
            {
                selectedIndex = i;
                break;
            }
        }

        if (selectedIndex != -1 && selectedIndex != mSpinnerTextToSpeechAudioStreamType.getSelectedItemPosition())
        {
            mSpinnerTextToSpeechAudioStreamType.setSelection(selectedIndex);
        }

        onTextToSpeechAudioStreamTypeChanged(textToSpeechAudioStreamType);

        return textToSpeechAudioStreamType;
    }

    private void onTextToSpeechAudioStreamTypeChanged(int textToSpeechAudioStreamType)
    {
        setVolumeControlStream(textToSpeechAudioStreamType);

        mTextToSpeechManager.setAudioStreamType(textToSpeechAudioStreamType);

        int volume = FooAudioUtils.getVolumeAbsolute(mAudioManager, textToSpeechAudioStreamType);
        onTextToSpeechAudioStreamVolumeChanged(textToSpeechAudioStreamType, volume, true, false);

    }

    private void onTextToSpeechAudioStreamVolumeChanged(int volume, boolean updateSeekbar, boolean updateStreamVolume)
    {
        int audioStreamType = mTextToSpeechManager.getAudioStreamType();
        onTextToSpeechAudioStreamVolumeChanged(audioStreamType, volume, updateSeekbar, updateStreamVolume);
    }

    private void onTextToSpeechAudioStreamVolumeChanged(int audioStreamType, int volume, boolean updateSeekbar, boolean updateStreamVolume)
    {
        if (updateSeekbar)
        {
            int volumeMax = mAudioManager.getStreamMaxVolume(audioStreamType);
            mSeekbarTextToSpeechAudioStreamVolume.setMax(volumeMax);
            mSeekbarTextToSpeechAudioStreamVolume.setProgress(volume);
        }

        if (updateStreamVolume)
        {
            mAudioManager.setStreamVolume(audioStreamType, volume, 0);
        }
    }

    private void profilesUpdate()
    {
        //noinspection unchecked
        ArrayAdapter<Profile> profileAdapter = (ArrayAdapter<Profile>) mSpinnerProfiles.getAdapter();
        profileAdapter.sort(Profile.COMPARATOR);

        int selectedIndex = -1;

        String profileToken = mProfileManager.getProfileToken();

        for (int i = 0; i < profileAdapter.getCount(); i++)
        {
            Profile profile = profileAdapter.getItem(i);
            //noinspection ConstantConditions
            if (profile.getToken().equals(profileToken))
            {
                selectedIndex = i;
                break;
            }
        }

        if (selectedIndex != -1)
        {
            mSpinnerProfiles.setSelection(selectedIndex);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        FooLog.v(TAG, "onActivityResult(...)");
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode)
        {
            case REQUEST_ACTION_CHECK_TTS_DATA:
            {
                switch (resultCode)
                {
                    case TextToSpeech.Engine.CHECK_VOICE_DATA_PASS:
                    {
                        updateTextToSpeechVoices();
                        break;
                    }
                }
                break;
            }
        }
    }

    private static class VoiceWrapper
            implements Comparable<VoiceWrapper>
    {
        private final Voice  mVoice;
        private final String mDisplayName;

        public VoiceWrapper(@NonNull Voice voice)
        {
            mVoice = voice;
            mDisplayName = voice.getName().toLowerCase();
        }

        public Voice getVoice()
        {
            return mVoice;
        }

        @Override
        public String toString()
        {
            return mDisplayName;
        }

        public boolean equals(VoiceWrapper o)
        {
            return compareTo(o) == 0;
        }

        public boolean equals(Voice o)
        {
            return compareTo(o) == 0;
        }

        public boolean equals(String o)
        {
            return compareTo(o) == 0;
        }

        @Override
        public boolean equals(Object o)
        {
            if (o instanceof VoiceWrapper)
            {
                return equals((VoiceWrapper) o);
            }

            if (o instanceof Voice)
            {
                return equals((Voice) o);
            }

            if (o instanceof String)
            {
                return equals((String) o);
            }

            return super.equals(o);
        }

        @Override
        public int compareTo(@NonNull VoiceWrapper other)
        {
            return compareTo(other.mDisplayName);
        }

        public int compareTo(@NonNull Voice other)
        {
            return compareTo(other.getName());
        }

        public int compareTo(@NonNull String other)
        {
            //noinspection UnnecessaryLocalVariable
            int result = mDisplayName.compareTo(other.toLowerCase());
            //FooLog.e(TAG, FooString.quote(mDisplayName) + ".compareTo(" + FooString.quote(other) + ") == " + result);
            return result;
        }
    }

    private void updateTextToSpeechVoices()
    {
        ArrayList<VoiceWrapper> availableVoices = new ArrayList<>();
        Set<Voice> voices = mTextToSpeechManager.getVoices();
        if (voices != null)
        {
            for (Voice voice : voices)
            {
                Set<String> voiceFeatures = voice.getFeatures();
                //FooLog.e(TAG, "onActivityResult: voiceFeatures=" + voiceFeatures);
                if (voiceFeatures.contains("notInstalled"))
                {
                    continue;
                }

                VoiceWrapper voiceWrapper = new VoiceWrapper(voice);

                availableVoices.add(voiceWrapper);
            }
        }
        Collections.sort(availableVoices);

        String currentVoiceName = mTextToSpeechManager.getVoiceName();
        int currentVoiceIndex = 0;
        for (int i = 0; i < availableVoices.size(); i++)
        {
            VoiceWrapper voiceWrapper = availableVoices.get(i);
            if (voiceWrapper.equals(currentVoiceName))
            {
                currentVoiceIndex = i;
                break;
            }
        }

        ArrayAdapter<VoiceWrapper> spinnerVoicesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, availableVoices);
        mSpinnerTextToSpeechVoices.setAdapter(spinnerVoicesAdapter);
        mSpinnerTextToSpeechVoices.setSelection(currentVoiceIndex);
        mSpinnerTextToSpeechVoices.setOnItemSelectedListener(new OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                VoiceWrapper voiceWrapper = (VoiceWrapper) parent.getAdapter().getItem(position);
                Voice voice = voiceWrapper.getVoice();

                mTextToSpeechManager.setVoice(voice);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {
            }
        });
    }

    private void verifyRequirements()
    {
    }

    private void onNotificationListenerConnected()
    {
        FooLog.i(TAG, "onNotificationListenerConnected()");

        mButtonNotificationListenerSettings.setVisibility(View.GONE);
        mButtonProcessNotifications.setVisibility(View.VISIBLE);

        FragmentManager fm = getSupportFragmentManager();
        DialogFragment dialogFragment = (DialogFragment) fm.findFragmentByTag(FRAGMENT_DIALOG_NOTIFICATION_ACCESS_DISABLED);
        if (dialogFragment != null)
        {
            dialogFragment.dismiss();
        }
    }

    private boolean onNotificationListenerNotConnected(NotConnectedReason reason, boolean showDialog)
    {
        FooLog.w(TAG, "onNotificationListenerNotConnected(reason=" + reason +
                      ", showDialog=" + showDialog + ')');

        mButtonProcessNotifications.setVisibility(View.GONE);
        mButtonNotificationListenerSettings.setVisibility(View.VISIBLE);

        if (!showDialog)
        {
            return false;
        }

        String title = mAlfredManager.getNotificationListenerNotConnectedTitle(reason);
        String message = mAlfredManager.getNotificationListenerNotConnectedMessage(reason);

        FragmentManager fm = getSupportFragmentManager();
        GenericPromptPositiveNegativeDialogFragment dialogFragment = (GenericPromptPositiveNegativeDialogFragment) fm
                .findFragmentByTag(FRAGMENT_DIALOG_NOTIFICATION_ACCESS_DISABLED);
        if (dialogFragment != null)
        {
            if (!title.equals(dialogFragment.getTitle()) || !message.equals(dialogFragment.getMessage()))
            {
                dialogFragment.dismiss();
                dialogFragment = null;
            }
        }

        if (dialogFragment == null)
        {
            // TODO:(pv) 3rd button for option to immediately reboot phone…

            dialogFragment = GenericPromptPositiveNegativeDialogFragment.newInstance(title, message);
            dialogFragment.show(fm, FRAGMENT_DIALOG_NOTIFICATION_ACCESS_DISABLED);
        }

        return true;
    }

    @Override
    public boolean onGenericPromptPositiveNegativeDialogFragmentResult(@NonNull GenericPromptPositiveNegativeDialogFragment dialogFragment)
    {
        switch (dialogFragment.getResult())
        {
            case Positive:
                mNotificationParserManager.startActivityNotificationListenerSettings();
                break;
        }
        return false;
    }

    private void onFloatingActionButtonClick()
    {
        /*
        Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null)
                .show();
        */
        //mMainApplication.startScanning();
    }
}

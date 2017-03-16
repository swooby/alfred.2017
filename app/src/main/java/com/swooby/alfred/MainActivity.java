package com.swooby.alfred;

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
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;

import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.bluetooth.FooBluetoothManager;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.media.FooAudioStreamVolumeObserver;
import com.smartfoo.android.core.media.FooAudioStreamVolumeObserver.OnAudioStreamVolumeChangedListener;
import com.smartfoo.android.core.media.FooAudioUtils;
import com.smartfoo.android.core.notification.FooNotificationListener;
import com.smartfoo.android.core.platform.FooPlatformUtils;
import com.smartfoo.android.core.texttospeech.FooTextToSpeech.FooTextToSpeechCallbacks;
import com.smartfoo.android.core.texttospeech.FooTextToSpeechHelper;
import com.swooby.alfred.Profile.Tokens;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

public class MainActivity
        extends AppCompatActivity
        implements OnNavigationItemSelectedListener
{
    private static final String TAG = FooLog.TAG(MainActivity.class);

    private static final int REQUEST_ACTION_CHECK_TTS_DATA = 100;

    private MainApplication     mMainApplication;
    private TextToSpeechManager mTextToSpeechManager;
    private AppPreferences      mAppPreferences;
    private FooBluetoothManager mBluetoothManager;

    private AudioManager                 mAudioManager;
    private FooAudioStreamVolumeObserver mAudioStreamVolumeObserver;

    private DrawerLayout          mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private NavigationView        mNavigationView;
    private Spinner mSpinnerVoices;
    private Spinner mSpinnerVoiceAudioStreamType;
    private SeekBar mSeekbarVoiceAudioStreamVolume;
    private Spinner mSpinnerProfiles;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mMainApplication = (MainApplication) getApplication();
        mTextToSpeechManager = mMainApplication.getTextToSpeechManager();
        mAppPreferences = mMainApplication.getAppPreferences();
        mBluetoothManager = mMainApplication.getBluetoothManager();

        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        int voiceAudioStreamType = mTextToSpeechManager.getVoiceAudioStreamType();
        setVolumeControlStream(voiceAudioStreamType);

        Intent intent = getIntent();
        FooLog.i(TAG, "onCreate: intent=" + FooPlatformUtils.toString(intent));

        String intentAction = intent.getAction();
        FooLog.i(TAG, "onCreate: intentAction=" + FooString.quote(intentAction));

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

        mSpinnerVoices = (Spinner) findViewById(R.id.spinnerVoices);

        mSpinnerVoiceAudioStreamType = (Spinner) findViewById(R.id.spinnerVoiceAudioStreamType);
        ArrayList<AudioStreamType> voiceAudioStreamTypes = AudioStreamType.getTypes(this);
        ArrayAdapter voiceAudioStreamTypeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, voiceAudioStreamTypes);
        mSpinnerVoiceAudioStreamType.setAdapter(voiceAudioStreamTypeAdapter);
        mSpinnerVoiceAudioStreamType.setOnItemSelectedListener(new OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                AudioStreamType voiceAudioStreamType = (AudioStreamType) parent.getAdapter().getItem(position);
                int audioStreamType = voiceAudioStreamType.getAudioStreamType();

                onVoiceAudioStreamTypeChanged(audioStreamType);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {
            }
        });

        ImageButton buttonVoiceAudioStreamTypeTest = (ImageButton) findViewById(R.id.buttonVoiceAudioStreamTypeTest);
        if (buttonVoiceAudioStreamTypeTest != null)
        {
            buttonVoiceAudioStreamTypeTest.setOnClickListener(new OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    mTextToSpeechManager.speak("Testing testing 1 2 3");
                }
            });
        }

        mSeekbarVoiceAudioStreamVolume = (SeekBar) findViewById(R.id.seekbarVoiceAudioStreamVolume);
        mSeekbarVoiceAudioStreamVolume.setOnSeekBarChangeListener(new OnSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                onVoiceAudioStreamVolumeChanged(progress, false, fromUser);
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
        ArrayList<Profile> profiles = profilesCreate();
        ArrayAdapter profilesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, profiles);
        mSpinnerProfiles.setAdapter(profilesAdapter);
        mSpinnerProfiles.setOnItemSelectedListener(new OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                Profile profile = (Profile) parent.getAdapter().getItem(position);
                mAppPreferences.setProfileToken(profile.getToken());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {
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

            if (mTextToSpeechManager.isInitialized())
            {
                FooTextToSpeechHelper.requestTextToSpeechData(this, REQUEST_ACTION_CHECK_TTS_DATA);
            }
            else
            {
                mTextToSpeechManager.attach(new FooTextToSpeechCallbacks()
                {
                    @Override
                    public void onInitialized()
                    {
                        mTextToSpeechManager.detach(this);
                        FooTextToSpeechHelper.requestTextToSpeechData(MainActivity.this, REQUEST_ACTION_CHECK_TTS_DATA);
                    }
                });
            }
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
        MenuItem menuItem = menu.findItem(R.id.action_notification_access);
        if (menuItem != null)
        {
            menuItem.setVisible(FooNotificationListener.supportsNotificationListenerSettings());
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

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
            //    // TODO:(pv) ...
            //    return true;
            case R.id.action_application_info:
                FooPlatformUtils.showAppSettings(this);
                return true;
            case R.id.action_notification_access:
                startActivity(FooNotificationListener.getIntentNotificationListenerSettings());
                return true;
            case R.id.action_text_to_speech:
                startActivity(FooTextToSpeechHelper.getIntentTextToSpeechSettings());
                return true;
            //case R.id.menu_refresh:
            //    refreshItemsFromTable();
            //    return true;
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
        super.onResume();

        voiceAudioStreamTypeUpdate();

        profilesUpdate();

    }

    @Override
    protected void onPause()
    {
        super.onPause();

        volumeObserverStop();
    }

    private int voiceAudioStreamTypeUpdate()
    {
        //noinspection unchecked
        ArrayAdapter<AudioStreamType> voiceAudioStreamTypeAdapter = (ArrayAdapter<AudioStreamType>) mSpinnerVoiceAudioStreamType
                .getAdapter();

        int selectedIndex = -1;

        int voiceAudioStreamType = mAppPreferences.getVoiceAudioStreamType();
        FooLog.i(TAG, "voiceAudioStreamTypeUpdate: voiceAudioStreamType=" +
                      FooAudioUtils.audioStreamTypeToString(voiceAudioStreamType));

        for (int i = 0; i < voiceAudioStreamTypeAdapter.getCount(); i++)
        {
            AudioStreamType audioStreamType = voiceAudioStreamTypeAdapter.getItem(i);
            if (audioStreamType.getAudioStreamType() == voiceAudioStreamType)
            {
                selectedIndex = i;
                break;
            }
        }

        if (selectedIndex != -1 && selectedIndex != mSpinnerVoiceAudioStreamType.getSelectedItemPosition())
        {
            mSpinnerVoiceAudioStreamType.setSelection(selectedIndex);
        }

        onVoiceAudioStreamTypeChanged(voiceAudioStreamType);

        return voiceAudioStreamType;
    }

    private void onVoiceAudioStreamTypeChanged(int audioStreamType)
    {
        mTextToSpeechManager.setVoiceAudioStreamType(audioStreamType);

        int percent = FooAudioUtils.getVolumePercent(mAudioManager, audioStreamType);
        onVoiceAudioStreamVolumeChanged(percent, true, false);

        volumeObserverStart(audioStreamType);
    }

    private void onVoiceAudioStreamVolumeChanged(int percent, boolean updateSeekbar, boolean updateStreamVolume)
    {
        if (updateSeekbar)
        {
            mSeekbarVoiceAudioStreamVolume.setProgress(percent);
        }

        if (updateStreamVolume)
        {
            int voiceAudioStreamType = mTextToSpeechManager.getVoiceAudioStreamType();
            int volume = FooAudioUtils.getVolumeAbsoluteFromPercent(mAudioManager, voiceAudioStreamType, percent);
            mAudioManager.setStreamVolume(voiceAudioStreamType, volume, 0);
        }
    }

    private void volumeObserverStop()
    {
        if (mAudioStreamVolumeObserver != null)
        {
            mAudioStreamVolumeObserver.stop();
            mAudioStreamVolumeObserver = null;
        }
    }

    private void volumeObserverStart(int audioStreamType)
    {
        volumeObserverStop();

        mAudioStreamVolumeObserver = new FooAudioStreamVolumeObserver(this);
        mAudioStreamVolumeObserver.start(audioStreamType, new OnAudioStreamVolumeChangedListener()
        {
            @Override
            public void onAudioStreamVolumeChanged(int audioStreamType, int volume)
            {
                int percent = FooAudioUtils.getVolumePercentFromAbsolute(mAudioManager, audioStreamType, volume);
                onVoiceAudioStreamVolumeChanged(percent, true, false);
            }
        });
    }

    private Profile profileCreate(int index, int resIdName, String token)
    {
        String name = getString(resIdName);
        return new Profile(index, name, token);
    }

    private ArrayList<Profile> profilesCreate()
    {
        ArrayList<Profile> profiles = new ArrayList<>();

        profiles.add(profileCreate(0, R.string.profile_disabled, Tokens.DISABLED));
        profiles.add(profileCreate(1, R.string.profile_always_on, Tokens.ALWAYS_ON));
        profiles.add(profileCreate(2, R.string.profile_headphones, Tokens.HEADPHONES_ONLY));

        return profiles;
    }

    private void profilesUpdate()
    {
        //noinspection unchecked
        ArrayAdapter<Profile> profileAdapter = (ArrayAdapter<Profile>) mSpinnerProfiles.getAdapter();
        profileAdapter.sort(Profile.COMPARATOR);

        int selectedIndex = -1;

        String profileToken = mAppPreferences.getProfileToken();

        for (int i = 0; i < profileAdapter.getCount(); i++)
        {
            Profile profile = profileAdapter.getItem(i);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode)
        {
            case REQUEST_ACTION_CHECK_TTS_DATA:
            {
                switch (resultCode)
                {
                    case TextToSpeech.Engine.CHECK_VOICE_DATA_PASS:
                    {
                        //
                        // We're initialize; start populating the UI
                        //

                        ArrayList<VoiceWrapper> availableVoices = new ArrayList<>();
                        Set<Voice> voices = mTextToSpeechManager.getVoices();
                        if (voices != null)
                        {
                            for (Voice voice : voices)
                            {
                                Set<String> voiceFeatures = voice.getFeatures();
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
                        mSpinnerVoices.setAdapter(spinnerVoicesAdapter);
                        mSpinnerVoices.setSelection(currentVoiceIndex);
                        mSpinnerVoices.setOnItemSelectedListener(new OnItemSelectedListener()
                        {
                            @Override
                            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
                            {
                                VoiceWrapper voiceWrapper = (VoiceWrapper) parent.getAdapter().getItem(position);
                                Voice voice = voiceWrapper.getVoice();

                                mTextToSpeechManager.setVoice(voice);

                                //mMainApplication.speak("Voice Initialized");
                            }

                            @Override
                            public void onNothingSelected(AdapterView<?> parent)
                            {
                            }
                        });
                        break;
                    }
                }
                break;
            }
        }
    }

    private void verifyRequirements()
    {
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

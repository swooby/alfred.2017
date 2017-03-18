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
import com.smartfoo.android.core.app.GenericPromptPositiveNegativeDialogFragment;
import com.smartfoo.android.core.app.GenericPromptPositiveNegativeDialogFragment.GenericPromptPositiveNegativeDialogFragmentCallbacks;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.media.FooAudioStreamVolumeObserver;
import com.smartfoo.android.core.media.FooAudioStreamVolumeObserver.OnAudioStreamVolumeChangedListener;
import com.smartfoo.android.core.media.FooAudioUtils;
import com.smartfoo.android.core.notification.FooNotificationListenerManager;
import com.smartfoo.android.core.platform.FooPlatformUtils;
import com.smartfoo.android.core.texttospeech.FooTextToSpeechHelper;
import com.swooby.alfred.MainApplication.MainApplicationCallbacks;
import com.swooby.alfred.Profile.Tokens;
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

    private final MainApplicationCallbacks mMainApplicationCallbacks = new MainApplicationCallbacks()
    {
        @Override
        public boolean onNotificationListenerAccessDisabled()
        {
            return MainActivity.this.onNotificationListenerAccessDisabled();
        }
    };

    private MainApplication           mMainApplication;
    private NotificationParserManager mNotificationParserManager;
    private TextToSpeechManager       mTextToSpeechManager;
    private AppPreferences            mAppPreferences;

    private AudioManager                 mAudioManager;
    private FooAudioStreamVolumeObserver mAudioStreamVolumeObserver;

    private DrawerLayout          mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private NavigationView        mNavigationView;
    private Spinner mSpinnerTextToSpeechVoices;
    private Spinner mSpinnerTextToSpeechAudioStreamType;
    private SeekBar mSeekbarTextToSpeechAudioStreamVolume;
    private Spinner mSpinnerProfiles;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mMainApplication = (MainApplication) getApplication();
        mNotificationParserManager = mMainApplication.getNotificationParserManager();
        mTextToSpeechManager = mMainApplication.getTextToSpeechManager();
        mAppPreferences = mMainApplication.getAppPreferences();

        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

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

            requestTextToSpeechData();
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
            menuItem.setVisible(FooNotificationListenerManager.supportsNotificationListenerSettings());
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
                mNotificationParserManager.startActivityNotificationListenerSettings(this);
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

        mMainApplication.attach(mMainApplicationCallbacks);

        textToSpeechAudioStreamTypeUpdate();

        profilesUpdate();

    }

    @Override
    protected void onPause()
    {
        super.onPause();

        mMainApplication.detach(mMainApplicationCallbacks);

        volumeObserverStop();
    }

    private int textToSpeechAudioStreamTypeUpdate()
    {
        //noinspection unchecked
        ArrayAdapter<AudioStreamType> textToSpeechAudioStreamTypeAdapter = (ArrayAdapter<AudioStreamType>) mSpinnerTextToSpeechAudioStreamType
                .getAdapter();

        int selectedIndex = -1;

        int textToSpeechAudioStreamType = mAppPreferences.getTextToSpeechAudioStreamType();
        FooLog.i(TAG, "textToSpeechAudioStreamTypeUpdate: textToSpeechAudioStreamType=" +
                      FooAudioUtils.audioStreamTypeToString(textToSpeechAudioStreamType));

        for (int i = 0; i < textToSpeechAudioStreamTypeAdapter.getCount(); i++)
        {
            AudioStreamType audioStreamType = textToSpeechAudioStreamTypeAdapter.getItem(i);
            if (audioStreamType == null)
            {
                continue;
            }
            if (audioStreamType.getAudioStreamType() == textToSpeechAudioStreamType)
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

        int percent = FooAudioUtils.getVolumePercent(mAudioManager, textToSpeechAudioStreamType);
        onTextToSpeechAudioStreamVolumeChanged(percent, true, false);

        volumeObserverStart(textToSpeechAudioStreamType);
    }

    private void onTextToSpeechAudioStreamVolumeChanged(int percent, boolean updateSeekbar, boolean updateStreamVolume)
    {
        if (updateSeekbar)
        {
            mSeekbarTextToSpeechAudioStreamVolume.setProgress(percent);
        }

        if (updateStreamVolume)
        {
            int textToSpeechAudioStreamType = mTextToSpeechManager.getAudioStreamType();
            int volume = FooAudioUtils.getVolumeAbsoluteFromPercent(mAudioManager, textToSpeechAudioStreamType, percent);
            mAudioManager.setStreamVolume(textToSpeechAudioStreamType, volume, 0);
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
                onTextToSpeechAudioStreamVolumeChanged(percent, true, false);
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
        profiles.add(profileCreate(1, R.string.profile_headphones_only, Tokens.HEADPHONES_ONLY));
        profiles.add(profileCreate(2, R.string.profile_always_on, Tokens.ALWAYS_ON));

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
                        updateTextToSpeechVoices();
                        break;
                    }
                }
                break;
            }
        }
    }

    private void requestTextToSpeechData()
    {
        if (!mTextToSpeechManager.isInitialized())
        {
            mTextToSpeechManager.attach(new TextToSpeechManagerCallbacks()
            {
                @Override
                public void onTextToSpeechInitialized()
                {
                    mTextToSpeechManager.detach(this);
                    requestTextToSpeechData();
                }
            });
            return;
        }

        FooTextToSpeechHelper.requestTextToSpeechData(this, REQUEST_ACTION_CHECK_TTS_DATA);
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

    private boolean onNotificationListenerAccessDisabled()
    {
        return false;
    }

    @Override
    public boolean onGenericPromptPositiveNegativeDialogFragmentResult(@NonNull GenericPromptPositiveNegativeDialogFragment dialogFragment)
    {
        switch (dialogFragment.getResult())
        {
            case Positive:
                mNotificationParserManager.startActivityNotificationListenerSettings(this);
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

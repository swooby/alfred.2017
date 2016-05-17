package com.swooby.alfred;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.res.Configuration;
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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.bluetooth.FooBluetoothHeadsetConnectionListener.OnBluetoothHeadsetConnectedCallbacks;
import com.smartfoo.android.core.bluetooth.FooBluetoothManager;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.notification.FooNotificationListener;
import com.smartfoo.android.core.platform.FooPlatformUtils;
import com.smartfoo.android.core.texttospeech.FooTextToSpeech;
import com.smartfoo.android.core.texttospeech.FooTextToSpeechHelper;
import com.swooby.alfred.Profile.Tokens;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

public class MainActivity
        extends AppCompatActivity//PbPermissionHandlingActivity
        implements OnNavigationItemSelectedListener //, AlfredPermissionsCallbacks
{
    private static final String TAG = FooLog.TAG(MainActivity.class);

    private static final int REQUEST_ACTION_CHECK_TTS_DATA = 100;

    private MainApplication     mMainApplication;
    private AppPreferences      mAppPreferences;
    private FooBluetoothManager mBluetoothManager;
    private FooTextToSpeech     mTextToSpeech;

    private DrawerLayout          mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private NavigationView        mNavigationView;
    private Spinner               mSpinnerVoices;
    private Spinner               mSpinnerProfiles;
    private ArrayAdapter<Profile> mSpinnerProfilesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mMainApplication = (MainApplication) getApplication();
        mAppPreferences = mMainApplication.getAppPreferences();
        mBluetoothManager = mMainApplication.getBluetoothManager();
        mTextToSpeech = mMainApplication.getTextToSpeech();
        Intent intent = getIntent();
        FooLog.i(TAG, "onCreate: intent=" + FooPlatformUtils.toString(intent));

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
        mSpinnerProfiles = (Spinner) findViewById(R.id.spinnerProfiles);
        ArrayList<Profile> profiles = profilesCreate();
        mSpinnerProfilesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, profiles);
        mSpinnerProfiles.setAdapter(mSpinnerProfilesAdapter);
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

            FooTextToSpeechHelper.requestTextToSpeechData(this, REQUEST_ACTION_CHECK_TTS_DATA);
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

        profilesUpdate();

        mBluetoothManager.getBluetoothHeadsetConnectionListener().attach(mOnBluetoothDeviceConnectedCallbacks);
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        mBluetoothManager.getBluetoothHeadsetConnectionListener().detach(mOnBluetoothDeviceConnectedCallbacks);
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
        profiles.add(profileCreate(2, R.string.profile_headphones, Tokens.HEADPHONES));

        return profiles;
    }

    private void profilesUpdate()
    {
        mSpinnerProfilesAdapter.sort(Profile.COMPARATOR);

        int selectedIndex = -1;

        String profileToken = mAppPreferences.getProfileToken();

        for (int i = 0; i < mSpinnerProfilesAdapter.getCount(); i++)
        {
            Profile profile = mSpinnerProfilesAdapter.getItem(i);
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

    private void onBluetoothDeviceConnected(BluetoothDevice bluetoothDevice)
    {
    }

    private void onBluetoothDeviceDisconnected(BluetoothDevice bluetoothDevice)
    {
    }

    private static class VoiceWrapper
            implements Comparable<VoiceWrapper>
    {
        private final Voice  mVoice;
        private final String mDisplayName;

        public VoiceWrapper(
                @NonNull
                Voice voice)
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

        public boolean equals(Voice another)
        {
            return toString().equalsIgnoreCase(another.getName());
        }

        public boolean equals(VoiceWrapper another)
        {
            return compareTo(another) == 0;
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

            return super.equals(o);
        }

        @Override
        public int compareTo(
                @NonNull
                VoiceWrapper another)
        {
            return toString().compareTo(another.toString());
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
                        Set<Voice> voices = mTextToSpeech.getVoices();
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

                        Voice currentVoice = mTextToSpeech.getVoice();
                        int currentVoiceIndex = 0;
                        for (int i = 0; i < availableVoices.size(); i++)
                        {
                            VoiceWrapper voiceWrapper = availableVoices.get(i);
                            if (voiceWrapper.equals(currentVoice))
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

                                mAppPreferences.setVoice(voice);

                                mTextToSpeech.setVoice(voice);

                                mTextToSpeech.speak("Initialized");
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
        Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null)
                .show();
        String deviceId = "qwigybo";

        mAzure.iotDeviceAdd(deviceId, new IoTDeviceAddCallback()
        {
            @Override
            public void onSuccess(Device device)
            {
                FooLog.i(TAG, "iotDeviceAdd onSuccess: device id=" + device.getDeviceId());
                FooLog.i(TAG, "iotDeviceAdd onSuccess: device primaryKey=" + device.getPrimaryKey());

                String message = "w00t @ " + new Date().toString();
                mAzure.iotDeviceSendMessage(device, message, new IoTDeviceSendMessageCallback()
                {
                    @Override
                    public void onSuccess(Device device, DeviceClient deviceClient)
                    {
                        FooLog.i(TAG, "iotDeviceSendMessage onSuccess: device id=" + device.getDeviceId());
                    }

                    @Override
                    public void onException(Device device, Exception exception)
                    {
                        FooLog.e(TAG, "iotDeviceAdd onException: device id=" + deviceId, exception);
                    }
                });
            }

            @Override
            public void onException(String deviceId, Exception exception)
            {
                FooLog.e(TAG, "iotDeviceAdd onException: device id=" + deviceId, exception);
            }
        });
    }
    }
}

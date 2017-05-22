package com.swooby.alfred;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.smartfoo.android.core.FooListenerManager;
import com.smartfoo.android.core.FooRun;
import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.bluetooth.FooBluetoothAudioConnectionListener;
import com.smartfoo.android.core.bluetooth.FooBluetoothAudioConnectionListener.OnBluetoothAudioConnectionCallbacks;
import com.smartfoo.android.core.bluetooth.FooBluetoothManager;
import com.smartfoo.android.core.bluetooth.FooBluetoothUtils;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.media.FooWiredHeadsetConnectionListener;
import com.smartfoo.android.core.media.FooWiredHeadsetConnectionListener.OnWiredHeadsetConnectionCallbacks;
import com.swooby.alfred.Profile.Tokens;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProfileManager
{
    private static final String TAG = FooLog.TAG(ProfileManager.class);

    public static String DEFAULT_PROFILE_TOKEN = Tokens.HEADPHONES_WIRED;

    public enum HeadsetType
    {
        Bluetooth,
        Wired
    }

    public interface ProfileManagerConfiguration
    {
        String getProfileToken();

        void setProfileToken(String profileToken);
    }

    public static class ProfileManagerCallbacks
    {
        void onHeadsetConnectionChanged(HeadsetType headsetType, String headsetName, boolean isConnected)
        {
            // ignore
        }

        void onProfileEnabled(Profile profile)
        {
            // ignore
        }

        void onProfileDisabled(Profile profile)
        {
            // ignore
        }

        void onProfileSet(Profile profile)
        {
            // ignore
        }
    }

    private final Context                                     mContext;
    private final ProfileManagerConfiguration                 mConfiguration;
    private final FooListenerManager<ProfileManagerCallbacks> mListenerManager;
    private final FooWiredHeadsetConnectionListener           mWiredHeadsetConnectionListener;
    private final FooBluetoothManager                         mBluetoothManager;
    private final FooBluetoothAudioConnectionListener         mBluetoothAudioConnectionListener;

    private String mProfileTokenEnabled;

    public ProfileManager(@NonNull Context context,
                          @NonNull ProfileManagerConfiguration configuration)
    {
        FooLog.v(TAG, "+ProfileManager(...)");

        FooRun.throwIllegalArgumentExceptionIfNull(context, "context");
        FooRun.throwIllegalArgumentExceptionIfNull(configuration, "configuration");

        mContext = context;
        mConfiguration = configuration;
        mListenerManager = new FooListenerManager<>(this);
        mWiredHeadsetConnectionListener = new FooWiredHeadsetConnectionListener(context);
        mBluetoothManager = new FooBluetoothManager(context);
        mBluetoothAudioConnectionListener = mBluetoothManager.getBluetoothAudioConnectionListener();

        mWiredHeadsetConnectionListener.attach(new OnWiredHeadsetConnectionCallbacks()
        {
            @Override
            public void onWiredHeadsetConnected(String name, boolean hasMicrophone)
            {
                ProfileManager.this.onWiredHeadsetConnected(name, hasMicrophone);
            }

            @Override
            public void onWiredHeadsetDisconnected(String name, boolean hasMicrophone)
            {
                ProfileManager.this.onWiredHeadsetDisconnected(name, hasMicrophone);
            }
        });

        mBluetoothAudioConnectionListener.attach(new OnBluetoothAudioConnectionCallbacks()
        {
            @Override
            public void onBluetoothAudioConnected(BluetoothDevice bluetoothDevice)
            {
                ProfileManager.this.onBluetoothAudioConnected(bluetoothDevice);
            }

            @Override
            public void onBluetoothAudioDisconnected(BluetoothDevice bluetoothDevice)
            {
                ProfileManager.this.onBluetoothAudioDisconnected(bluetoothDevice);
            }
        });

        updateProfiles();

        FooLog.v(TAG, "-ProfileManager(...)");
    }

    Map<String, Profile> mProfiles = new LinkedHashMap<>();

    private void updateProfiles()
    {
        mProfiles.clear();

        addProfile(R.string.profile_disabled, Tokens.DISABLED);
        addProfile(R.string.profile_headphones_wired, Tokens.HEADPHONES_WIRED);
        addProfile(R.string.profile_headphones_bluetooth_any, Tokens.HEADPHONES_BLUETOOTH_ANY);

        Set<BluetoothDevice> bluetoothDevices = mBluetoothManager.getBondedDevices();
        for (BluetoothDevice bluetoothDevice : bluetoothDevices)
        {
            //FooLog.v(TAG, "getProfiles: bluetoothDevice == " + bluetoothDevice);
            String deviceAddress = bluetoothDevice.getAddress();
            //FooLog.v(TAG, "getProfiles: deviceAddress == " + FooString.quote(deviceAddress));
            String deviceName = bluetoothDevice.getName();
            //FooLog.v(TAG, "getProfiles: deviceName == " + FooString.quote(deviceName));
            boolean isAudioOutput = FooBluetoothUtils.isAudioOutput(bluetoothDevice);
            //FooLog.v(TAG, "getProfiles: isAudioOutput == " + isAudioOutput);
            if (isAudioOutput)
            {
                //deviceName = deviceName + " (" + deviceAddress + ')';
                String name = mContext.getString(R.string.profile_headphones_bluetooth_X, deviceName);
                addProfile(name, deviceAddress);
            }
        }

        addProfile(R.string.profile_headphones_any, Tokens.HEADPHONES_ANY);
        addProfile(R.string.profile_always_on, Tokens.ALWAYS_ON);

        updateProfileTokenEnabled();
    }

    private void addProfile(@StringRes int resIdName, String token)
    {
        addProfile(mContext.getString(resIdName), token);
    }

    private void addProfile(String name, String token)
    {
        mProfiles.put(token, new Profile(mProfiles.size(), name, token));
    }

    private Profile getProfile(String profileToken)
    {
        Profile profile = mProfiles.get(profileToken);
        if (profile == null)
        {
            profile = mProfiles.get(DEFAULT_PROFILE_TOKEN);
        }
        return profile;
    }

    @NonNull
    public List<Profile> getProfiles()
    {
        return new ArrayList<>(mProfiles.values());
    }

    public boolean isEnabled()
    {
        return Tokens.isNotDisabled(mProfileTokenEnabled);
    }

    @NonNull
    public Profile getProfile()
    {
        String profileToken = mConfiguration.getProfileToken();
        return getProfile(profileToken);
    }

    public boolean setProfileToken(String profileToken)
    {
        if (FooString.isNullOrEmpty(profileToken))
        {
            profileToken = DEFAULT_PROFILE_TOKEN;
        }

        FooLog.v(TAG, "setProfileToken: profileToken == " + FooString.quote(profileToken));

        Profile profile = getProfile();
        FooLog.v(TAG, "setProfileToken: profile == " + profile);

        if (profile.getToken().equals(profileToken))
        {
            FooLog.v(TAG, "updateProfileToken: profileToken == value; ignoring");
            return false;
        }

        FooLog.i(TAG, "updateProfileToken: profileToken != value; updating");

        profile = getProfile(profileToken);

        mConfiguration.setProfileToken(profileToken);

        for (ProfileManagerCallbacks callbacks : mListenerManager.beginTraversing())
        {
            callbacks.onProfileSet(profile);
        }
        mListenerManager.endTraversing();

        updateProfileTokenEnabled();

        return true;
    }

    public boolean isWiredHeadsetConnected()
    {
        return mWiredHeadsetConnectionListener.isWiredHeadsetConnected();
    }

    public boolean isBluetoothAudioConnected()
    {
        return mBluetoothAudioConnectionListener.isBluetoothAudioConnected();
    }

    public boolean isBluetoothAudioConnected(String deviceMacAddress)
    {
        return mBluetoothAudioConnectionListener.isBluetoothAudioConnected(deviceMacAddress);
    }

    public boolean isWiredHeadsetOrBluetoothAudioConnected()
    {
        return isWiredHeadsetConnected() || isBluetoothAudioConnected();
    }

    /*
    @NonNull
    public Map<String, BluetoothDevice> getConnectedBluetoothHeadsets()
    {
        return mBluetoothAudioConnectionListener.getConnectedBluetoothHeadsets();
    }
    */

    public void attach(@NonNull ProfileManagerCallbacks callbacks)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(callbacks, "callbacks");
        mListenerManager.attach(callbacks);
        Profile profile = getProfile();
        if (isEnabled())
        {
            callbacks.onProfileEnabled(profile);
        }
        else
        {
            callbacks.onProfileDisabled(profile);
        }
    }

    public void detach(@NonNull ProfileManagerCallbacks callbacks)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(callbacks, "callbacks");
        mListenerManager.detach(callbacks);
    }

    private void onBluetoothAudioConnected(BluetoothDevice bluetoothDevice)
    {
        FooLog.v(TAG, "onBluetoothAudioConnected(bluetoothDevice=" + bluetoothDevice + ')');
        String headsetName = bluetoothDevice.getName();
        onHeadsetConnectionChanged(HeadsetType.Bluetooth, headsetName, true);
    }

    private void onBluetoothAudioDisconnected(BluetoothDevice bluetoothDevice)
    {
        FooLog.v(TAG, "onBluetoothAudioDisconnected(bluetoothDevice=" + bluetoothDevice + ')');
        String headsetName = bluetoothDevice.getName();
        onHeadsetConnectionChanged(HeadsetType.Bluetooth, headsetName, false);
    }

    private void onWiredHeadsetConnected(String headsetName, boolean hasMicrophone)
    {
        FooLog.v(TAG, "onWiredHeadsetConnected(headsetName=" + FooString.quote(headsetName) +
                      ", hasMicrophone=" + hasMicrophone + ')');
        onHeadsetConnectionChanged(HeadsetType.Wired, headsetName, true);
    }

    private void onWiredHeadsetDisconnected(String headsetName, boolean hasMicrophone)
    {
        FooLog.v(TAG, "onWiredHeadsetDisconnected(headsetName=" + FooString.quote(headsetName) +
                      ", hasMicrophone=" + hasMicrophone + ')');
        onHeadsetConnectionChanged(HeadsetType.Wired, headsetName, false);
    }

    private void onHeadsetConnectionChanged(HeadsetType headsetType, String headsetName, boolean isConnected)
    {
        FooLog.v(TAG, "onHeadsetConnectionChanged(headsetType=" + headsetType +
                      ", headsetName=" + FooString.quote(headsetName) +
                      ", isConnected=" + isConnected + ')');

        updateProfiles();

        for (ProfileManagerCallbacks callbacks : mListenerManager.beginTraversing())
        {
            callbacks.onHeadsetConnectionChanged(headsetType, headsetName, isConnected);
        }
        mListenerManager.endTraversing();
    }

    private void updateProfileTokenEnabled()
    {
        Profile profile = getProfile();
        String profileToken = profile.getToken();
        FooLog.v(TAG, "updateProfileTokenEnabled: profileToken == " + FooString.quote(profileToken));

        String newProfileTokenEnabled = null;
        switch (profileToken)
        {
            case Tokens.DISABLED:
                // ignore
                break;
            case Tokens.HEADPHONES_WIRED:
                if (isWiredHeadsetConnected())
                {
                    newProfileTokenEnabled = Tokens.HEADPHONES_WIRED;
                }
                break;
            case Tokens.HEADPHONES_BLUETOOTH_ANY:
                if (isBluetoothAudioConnected())
                {
                    newProfileTokenEnabled = Tokens.HEADPHONES_BLUETOOTH_ANY;
                }
                break;
            default:
                if (isBluetoothAudioConnected(profileToken))
                {
                    newProfileTokenEnabled = profileToken;
                }
                break;
            case Tokens.HEADPHONES_ANY:
                if (isWiredHeadsetOrBluetoothAudioConnected())
                {
                    newProfileTokenEnabled = Tokens.HEADPHONES_ANY;
                }
                break;
            case Tokens.ALWAYS_ON:
                newProfileTokenEnabled = Tokens.ALWAYS_ON;
                break;
        }
        FooLog.v(TAG, "updateProfileTokenEnabled: newProfileTokenEnabled == " +
                      FooString.quote(newProfileTokenEnabled));

        String oldProfileTokenEnabled = mProfileTokenEnabled;
        FooLog.v(TAG, "updateProfileTokenEnabled: oldProfileTokenEnabled == " +
                      FooString.quote(oldProfileTokenEnabled));

        if (FooString.equals(newProfileTokenEnabled, oldProfileTokenEnabled))
        {
            FooLog.v(TAG, "updateProfileTokenEnabled: newProfileTokenEnabled == oldProfileTokenEnabled; ignoring");
            return;
        }

        FooLog.i(TAG, "updateProfileTokenEnabled: newProfileTokenEnabled != oldProfileTokenEnabled; updating");

        mProfileTokenEnabled = newProfileTokenEnabled;

        if (Tokens.isDisabled(newProfileTokenEnabled))
        {
            for (ProfileManagerCallbacks callbacks : mListenerManager.beginTraversing())
            {
                callbacks.onProfileDisabled(profile);
            }
            mListenerManager.endTraversing();
        }
        else
        {
            profile = getProfile(newProfileTokenEnabled);
            for (ProfileManagerCallbacks callbacks : mListenerManager.beginTraversing())
            {
                callbacks.onProfileEnabled(profile);
            }
            mListenerManager.endTraversing();
        }
    }
}

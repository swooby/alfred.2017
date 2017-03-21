package com.swooby.alfred;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.annotation.NonNull;

import com.smartfoo.android.core.FooListenerManager;
import com.smartfoo.android.core.FooRun;
import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.bluetooth.FooBluetoothHeadsetConnectionListener;
import com.smartfoo.android.core.bluetooth.FooBluetoothHeadsetConnectionListener.OnBluetoothHeadsetConnectionCallbacks;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.media.FooWiredHeadsetConnectionListener;
import com.smartfoo.android.core.media.FooWiredHeadsetConnectionListener.OnWiredHeadsetConnectionCallbacks;
import com.swooby.alfred.Profile.Tokens;

import java.util.ArrayList;
import java.util.Map;

public class ProfileManager
{
    private static final String TAG = FooLog.TAG(ProfileManager.class);

    public static String DEFAULT_PROFILE_TOKEN = Tokens.WIRED_HEADPHONES_ONLY;

    public enum HeadsetType
    {
        Bluetooth,
        Wired
    }

    public static class ProfileManagerCallbacks
    {
        void onHeadsetConnectionChanged(HeadsetType headsetType, String headsetName, boolean isConnected)
        {
            // ignore
        }

        void onProfileStateChanged(String profileName, boolean isEnabled)
        {
            // ignore
        }

        void onProfileTokenSet(String profileToken)
        {
            // ignore
        }
    }

    public interface ProfileManagerConfiguration
    {
        String getProfileToken();

        void setProfileToken(String profileToken);
    }

    private final Context                                     mContext;
    private final ProfileManagerConfiguration                 mConfiguration;
    private final FooListenerManager<ProfileManagerCallbacks> mListenerManager;
    private final FooWiredHeadsetConnectionListener           mWiredHeadsetConnectionListener;
    private final FooBluetoothHeadsetConnectionListener       mBluetoothHeadsetConnectionListener;

    private String mProfileTokenEnabled;

    public ProfileManager(@NonNull Context context,
                          @NonNull ProfileManagerConfiguration configuration)
    {
        FooLog.v(TAG, "+ProfileManager(...)");

        FooRun.throwIllegalArgumentExceptionIfNull(context, "context");
        FooRun.throwIllegalArgumentExceptionIfNull(configuration, "configuration");

        mContext = context;
        mConfiguration = configuration;
        mListenerManager = new FooListenerManager<>();
        mWiredHeadsetConnectionListener = new FooWiredHeadsetConnectionListener(context);
        mBluetoothHeadsetConnectionListener = new FooBluetoothHeadsetConnectionListener(context);

        updateProfileTokenEnabled();

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

        mBluetoothHeadsetConnectionListener.attach(new OnBluetoothHeadsetConnectionCallbacks()
        {
            @Override
            public void onBluetoothHeadsetConnected(BluetoothDevice bluetoothDevice)
            {
                ProfileManager.this.onBluetoothHeadsetConnected(bluetoothDevice);
            }

            @Override
            public void onBluetoothHeadsetDisconnected(BluetoothDevice bluetoothDevice)
            {
                ProfileManager.this.onBluetoothHeadsetDisconnected(bluetoothDevice);
            }
        });

        FooLog.v(TAG, "-ProfileManager(...)");
    }

    @NonNull
    public ArrayList<Profile> getProfiles()
    {
        ArrayList<Profile> profiles = new ArrayList<>();

        profiles.add(getProfile(profiles.size(), R.string.profile_disabled, Tokens.DISABLED));
        profiles.add(getProfile(profiles.size(), R.string.profile_wired_headphones_only, Tokens.WIRED_HEADPHONES_ONLY));
        //profiles.add(profileCreate(profiles.size(), R.string.profile_headphones_only, Tokens.HEADPHONES_ONLY));
        profiles.add(getProfile(profiles.size(), R.string.profile_always_on, Tokens.ALWAYS_ON));

        return profiles;
    }

    private Profile getProfile(int index, int resIdName, String token)
    {
        String name = mContext.getString(resIdName);
        return new Profile(index, name, token);
    }

    public String getProfileToken()
    {
        return mConfiguration.getProfileToken();
    }

    public boolean setProfileToken(String value)
    {
        if (FooString.isNullOrEmpty(value))
        {
            value = DEFAULT_PROFILE_TOKEN;
        }

        FooLog.v(TAG, "setProfileToken: value == " + FooString.quote(value));

        String profileToken = getProfileToken();
        FooLog.v(TAG, "setProfileToken: profileToken == " + FooString.quote(profileToken));

        if (profileToken.equals(value))
        {
            FooLog.v(TAG, "updateProfileToken: profileToken == value; ignoring");
            return false;
        }

        FooLog.i(TAG, "updateProfileToken: profileToken != value; updating");

        mConfiguration.setProfileToken(value);

        for (ProfileManagerCallbacks callbacks : mListenerManager.beginTraversing())
        {
            callbacks.onProfileTokenSet(value);
        }
        mListenerManager.endTraversing();

        updateProfileTokenEnabled();

        return true;
    }

    public boolean isEnabled()
    {
        return Tokens.isNotDisabled(mProfileTokenEnabled);
    }

    public boolean isHeadsetConnected()
    {
        return isWiredHeadsetConnected() || isBluetoothHeadsetConnected();
    }

    public boolean isWiredHeadsetConnected()
    {
        return mWiredHeadsetConnectionListener.isWiredHeadsetConnected();
    }

    public boolean isBluetoothHeadsetConnected()
    {
        return mBluetoothHeadsetConnectionListener.isBluetoothHeadsetConnected();
    }

    @NonNull
    public Map<String, BluetoothDevice> getConnectedBluetoothHeadsets()
    {
        return mBluetoothHeadsetConnectionListener.getConnectedBluetoothHeadsets();
    }

    public void attach(ProfileManagerCallbacks callbacks)
    {
        mListenerManager.attach(callbacks);
    }

    public void detach(ProfileManagerCallbacks callbacks)
    {
        mListenerManager.detach(callbacks);
    }

    private void onBluetoothHeadsetConnected(BluetoothDevice bluetoothDevice)
    {
        FooLog.v(TAG, "onBluetoothHeadsetConnected(bluetoothDevice=" + bluetoothDevice + ')');
        String headsetName = bluetoothDevice.getName();
        onHeadsetConnectionChanged(HeadsetType.Bluetooth, headsetName, true);
    }

    private void onBluetoothHeadsetDisconnected(BluetoothDevice bluetoothDevice)
    {
        FooLog.v(TAG, "onBluetoothHeadsetDisconnected(bluetoothDevice=" + bluetoothDevice + ')');
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

        for (ProfileManagerCallbacks callbacks : mListenerManager.beginTraversing())
        {
            callbacks.onHeadsetConnectionChanged(headsetType, headsetName, isConnected);
        }
        mListenerManager.endTraversing();

        updateProfileTokenEnabled();
    }

    private void updateProfileTokenEnabled()
    {
        String profileToken = getProfileToken();
        FooLog.v(TAG, "updateProfileTokenEnabled: profileToken == " + FooString.quote(profileToken));

        String newProfileTokenEnabled = null;
        switch (profileToken)
        {
            case Tokens.WIRED_HEADPHONES_ONLY:
                if (isWiredHeadsetConnected())
                {
                    newProfileTokenEnabled = Tokens.WIRED_HEADPHONES_ONLY;
                }
                break;
            /*
            case Tokens.HEADPHONES_ONLY:
                if (isHeadsetConnected())
                {
                    profileTokenEnabled = Tokens.HEADPHONES_ONLY;
                }
                break;
                */
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

        if (Tokens.isDisabled(newProfileTokenEnabled) && Tokens.isNotDisabled(oldProfileTokenEnabled))
        {
            for (ProfileManagerCallbacks callbacks : mListenerManager.beginTraversing())
            {
                callbacks.onProfileStateChanged(oldProfileTokenEnabled, false);
            }
            mListenerManager.endTraversing();
        }

        if (Tokens.isNotDisabled(newProfileTokenEnabled) && Tokens.isDisabled(oldProfileTokenEnabled))
        {
            for (ProfileManagerCallbacks callbacks : mListenerManager.beginTraversing())
            {
                callbacks.onProfileStateChanged(newProfileTokenEnabled, true);
            }
            mListenerManager.endTraversing();
        }
    }
}

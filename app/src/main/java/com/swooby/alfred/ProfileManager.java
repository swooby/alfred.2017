package com.swooby.alfred;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.annotation.NonNull;

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
import java.util.List;
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

        void onProfileEnabled(String profileToken)
        {
            // ignore
        }

        void onProfileDisabled(String profileToken)
        {
            // ignore
        }

        void onProfileTokenSet(String profileToken)
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

        FooLog.v(TAG, "-ProfileManager(...)");
    }

    @NonNull
    public List<Profile> getProfiles(@NonNull Context context)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(context, "context");

        List<Profile> profiles = new ArrayList<>();

        addProfile(profiles, context, R.string.profile_disabled, Tokens.DISABLED);
        addProfile(profiles, context, R.string.profile_headphones_wired, Tokens.HEADPHONES_WIRED);

        addProfile(profiles, context, R.string.profile_always_on, Tokens.ALWAYS_ON);

        return profiles;
    }

    private void addProfile(List<Profile> profiles, Context context, int resIdName, String token)
    {
        addProfile(profiles, context.getString(resIdName), token);
    }

    private void addProfile(List<Profile> profiles, String name, String token)
    {
        profiles.add(new Profile(profiles.size(), name, token));
    }

    public boolean isEnabled()
    {
        return Tokens.isNotDisabled(mProfileTokenEnabled);
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

        updateProfileTokenEnabled();

        for (ProfileManagerCallbacks callbacks : mListenerManager.beginTraversing())
        {
            callbacks.onProfileTokenSet(value);
        }
        mListenerManager.endTraversing();

        return true;
    }

    /*
    public boolean isHeadsetConnected()
    {
        return isWiredHeadsetConnected() || isBluetoothAudioConnected();
    }
    */

    public boolean isWiredHeadsetConnected()
    {
        return mWiredHeadsetConnectionListener.isWiredHeadsetConnected();
    }

    /*
    public boolean isBluetoothAudioConnected()
    {
        return mBluetoothAudioConnectionListener.isBluetoothAudioConnected();
    }
    */

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
        String profileToken = getProfileToken();
        if (isEnabled())
        {
            callbacks.onProfileEnabled(profileToken);
        }
        else
        {
            callbacks.onProfileDisabled(profileToken);
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

        updateProfileTokenEnabled();

        for (ProfileManagerCallbacks callbacks : mListenerManager.beginTraversing())
        {
            callbacks.onHeadsetConnectionChanged(headsetType, headsetName, isConnected);
        }
        mListenerManager.endTraversing();
    }

    private void updateProfileTokenEnabled()
    {
        String profileToken = getProfileToken();
        FooLog.v(TAG, "updateProfileTokenEnabled: profileToken == " + FooString.quote(profileToken));

        String newProfileTokenEnabled = null;
        switch (profileToken)
        {
            case Tokens.HEADPHONES_WIRED:
                if (isWiredHeadsetConnected())
                {
                    newProfileTokenEnabled = Tokens.HEADPHONES_WIRED;
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

        if (Tokens.isDisabled(newProfileTokenEnabled))
        {
            for (ProfileManagerCallbacks callbacks : mListenerManager.beginTraversing())
            {
                callbacks.onProfileDisabled(profileToken);
            }
            mListenerManager.endTraversing();
        }
        else
        {
            for (ProfileManagerCallbacks callbacks : mListenerManager.beginTraversing())
            {
                callbacks.onProfileEnabled(profileToken);
            }
            mListenerManager.endTraversing();
        }
    }
}

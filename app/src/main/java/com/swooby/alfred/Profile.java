package com.swooby.alfred;

import android.bluetooth.BluetoothDevice;
import android.support.annotation.NonNull;

import java.util.Comparator;

public class Profile
{
    public interface Tokens
    {
        String DISABLED        = "profile.disabled";
        String HEADPHONES_ONLY = "profile.headphones_only";
        String ALWAYS_ON       = "profile.always_on";
    }

    public static Comparator<Profile> COMPARATOR = new Comparator<Profile>()
    {
        @Override
        public int compare(Profile lhs, Profile rhs)
        {
            if (lhs.mForcedOrder != -1)
            {
                int compare = Integer.compare(lhs.mForcedOrder, rhs.mForcedOrder);
                if (compare != 0)
                {
                    return compare;
                }
            }

            return lhs.mName.compareTo(rhs.mName);
        }
    };

    private final int    mForcedOrder;
    private final String mName;
    private final String mToken;

    public Profile(@NonNull BluetoothDevice bluetoothDevice)
    {
        this(-1, bluetoothDevice.getName(), bluetoothDevice.getAddress());
    }

    public Profile(int forcedOrder, @NonNull String name, @NonNull String token)
    {
        mForcedOrder = forcedOrder;
        mName = name;
        mToken = token;
    }

    @Override
    public String toString()
    {
        return mName;
    }

    public String getToken()
    {
        return mToken;
    }
}

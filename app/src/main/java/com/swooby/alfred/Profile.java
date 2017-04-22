package com.swooby.alfred;

import android.bluetooth.BluetoothDevice;
import android.support.annotation.NonNull;

import java.util.Comparator;

public class Profile
{
    public static abstract class Tokens
    {
        public static final String DISABLED                 = "profile.disabled";
        public static final String HEADPHONES_WIRED         = "profile.headphones_wired";
        public static final String ALWAYS_ON                = "profile.always_on";

        public static boolean isDisabled(String value)
        {
            return value == null || value.equals(DISABLED);
        }

        public static boolean isNotDisabled(String value)
        {
            return value != null && !value.equals(DISABLED);
        }
    }

    public static Comparator<Profile> COMPARATOR = new Comparator<Profile>()
    {
        @Override
        public int compare(Profile lhs, Profile rhs)
        {
            if (lhs.mForcedOrder != Integer.MAX_VALUE)
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
        this(Integer.MAX_VALUE, bluetoothDevice.getName(), bluetoothDevice.getAddress());
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

package com.swooby.alfred;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.annotation.NonNull;

import com.smartfoo.android.core.FooListenerManager;
import com.smartfoo.android.core.FooRun;
import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.platform.FooHeadsetConnectionListener;
import com.smartfoo.android.core.platform.FooHeadsetConnectionListener.OnHeadsetConnectionCallbacks;

public class HeadsetManager
{
    private static final String TAG = FooLog.TAG(HeadsetManager.class);

    public interface HeadsetManagerCallbacks
    {
        void onHeadsetConnectionChanged(boolean isConnected);
    }

    public interface HeadsetManagerConfiguration
    {
        @NonNull
        Context getContext();

        @NonNull
        TextToSpeechManager getTextToSpeech();
    }

    private final HeadsetManagerConfiguration                 mConfiguration;
    private final FooListenerManager<HeadsetManagerCallbacks> mListenerManager;
    private final OnHeadsetConnectionCallbacks                mHeadsetConnectionCallbacks;

    private FooHeadsetConnectionListener mHeadsetConnectionListener;
    private boolean                      mIsHeadsetConnected;

    public HeadsetManager(@NonNull HeadsetManagerConfiguration configuration)
    {
        FooLog.v(TAG, "+HeadsetManager(...)");

        FooRun.throwIllegalArgumentExceptionIfNull(configuration, "configuration");

        mConfiguration = configuration;

        mListenerManager = new FooListenerManager<>();

        mHeadsetConnectionCallbacks = new OnHeadsetConnectionCallbacks()
        {
            @Override
            public void onBluetoothHeadsetConnected(BluetoothDevice bluetoothDevice)
            {
                HeadsetManager.this.onBluetoothHeadsetConnected(bluetoothDevice);
            }

            @Override
            public void onBluetoothHeadsetDisconnected(BluetoothDevice bluetoothDevice)
            {
                HeadsetManager.this.onBluetoothHeadsetDisconnected(bluetoothDevice);
            }

            @Override
            public void onWiredHeadsetConnected(String name, boolean hasMicrophone)
            {
                HeadsetManager.this.onWiredHeadsetConnected(name, hasMicrophone);
            }

            @Override
            public void onWiredHeadsetDisconnected(String name, boolean hasMicrophone)
            {
                HeadsetManager.this.onWiredHeadsetDisconnected(name, hasMicrophone);
            }
        };

        FooLog.v(TAG, "-HeadsetManager(...)");
    }

    public boolean isHeadsetConnected()
    {
        return mIsHeadsetConnected;
    }

    @NonNull
    private Context getContext()
    {
        return mConfiguration.getContext();
    }

    @NonNull
    private TextToSpeechManager getTextToSpeech()
    {
        return mConfiguration.getTextToSpeech();
    }

    public void attach(HeadsetManagerCallbacks callbacks)
    {
        mListenerManager.attach(callbacks);

        if (mListenerManager.size() == 1)
        {
            if (mHeadsetConnectionListener == null)
            {
                mHeadsetConnectionListener = new FooHeadsetConnectionListener(getContext());
                mHeadsetConnectionListener.attach(mHeadsetConnectionCallbacks);
            }
        }
    }

    private void onBluetoothHeadsetConnected(BluetoothDevice bluetoothDevice)
    {
        FooLog.i(TAG, "onBluetoothHeadsetConnected(bluetoothDevice=" + bluetoothDevice + ')');
        updateHeadsetState();
        String name = bluetoothDevice.getName();
        speakHeadsetConnectionState(R.string.bluetooth_headset_X_connected, name);
    }

    private void onBluetoothHeadsetDisconnected(BluetoothDevice bluetoothDevice)
    {
        FooLog.i(TAG, "onBluetoothHeadsetDisconnected(bluetoothDevice=" + bluetoothDevice + ')');
        updateHeadsetState();
        //String name = bluetoothDevice.getName();
        //speakHeadsetConnectionState(R.string.bluetooth_headset_X_disconnected, name);
    }

    private void onWiredHeadsetConnected(String name, boolean hasMicrophone)
    {
        FooLog.i(TAG, "onWiredHeadsetConnected(name=" + FooString.quote(name) +
                      ", hasMicrophone=" + hasMicrophone + ')');
        updateHeadsetState();
        speakHeadsetConnectionState(R.string.wired_headset_X_connected, name);
    }

    private void onWiredHeadsetDisconnected(String name, boolean hasMicrophone)
    {
        FooLog.i(TAG, "onWiredHeadsetDisconnected(name=" + FooString.quote(name) +
                      ", hasMicrophone=" + hasMicrophone + ')');
        updateHeadsetState();
        //speakHeadsetConnectionState(R.string.wired_headset_X_disconnected, name);
    }

    private void speakHeadsetConnectionState(int resId, String headsetName)
    {
        if (headsetName == null)
        {
            headsetName = "";
        }
        String speech = getContext().getString(resId, headsetName);
        getTextToSpeech().speak(speech);
    }

    /**
     * @return true if the state changed and updateEnabledState() was called, otherwise false
     */
    private boolean updateHeadsetState()
    {
        FooLog.v(TAG, "updateHeadsetState()");

        boolean isHeadsetConnected = mHeadsetConnectionListener.isHeadsetConnected();
        FooLog.i(TAG, "updateHeadsetState: isHeadsetConnected=" + isHeadsetConnected);
        //FooLog.i(TAG, "updateHeadsetState: mIsHeadsetConnected=" + mIsHeadsetConnected);
        if (mIsHeadsetConnected != isHeadsetConnected)
        {
            mIsHeadsetConnected = isHeadsetConnected;

            for (HeadsetManagerCallbacks callbacks : mListenerManager.beginTraversing())
            {
                callbacks.onHeadsetConnectionChanged(isHeadsetConnected);
            }
            mListenerManager.endTraversing();

            return true;
        }

        return false;
    }
}

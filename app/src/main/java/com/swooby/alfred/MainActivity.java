package com.swooby.alfred;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import com.microsoft.azure.iot.service.sdk.Device;
import com.microsoft.azure.iothub.DeviceClient;
import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.logging.FooLog;
import com.swooby.alfred.azure.Azure;
import com.swooby.alfred.azure.Azure.IoTDeviceAddCallback;
import com.swooby.alfred.azure.Azure.IoTDeviceSendMessageCallback;

import java.util.Date;

public class MainActivity
        extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener
{
    private static final String TAG = FooLog.TAG(MainActivity.class);

    private Azure mAzure;

    private DrawerLayout          mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private NavigationView        mNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
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
        mAzure = new Azure(this);
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
    }

    @Override
    protected void onPause()
    {
        super.onPause();
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

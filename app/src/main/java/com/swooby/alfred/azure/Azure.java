package com.swooby.alfred.azure;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.widget.ProgressBar;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.microsoft.azure.iot.service.exceptions.IotHubException;
import com.microsoft.azure.iot.service.sdk.Device;
import com.microsoft.azure.iot.service.sdk.RegistryManager;
import com.microsoft.azure.iothub.DeviceClient;
import com.microsoft.azure.iothub.IotHubClientProtocol;
import com.microsoft.azure.iothub.IotHubEventCallback;
import com.microsoft.azure.iothub.Message;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.authentication.MobileServiceAuthenticationProvider;
import com.microsoft.windowsazure.mobileservices.authentication.MobileServiceUser;
import com.microsoft.windowsazure.mobileservices.http.NextServiceFilterCallback;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilter;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilterRequest;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilterResponse;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;
import com.microsoft.windowsazure.mobileservices.table.query.QueryOperations;
import com.microsoft.windowsazure.mobileservices.table.sync.MobileServiceSyncContext;
import com.microsoft.windowsazure.mobileservices.table.sync.localstore.ColumnDataType;
import com.microsoft.windowsazure.mobileservices.table.sync.localstore.MobileServiceLocalStoreException;
import com.microsoft.windowsazure.mobileservices.table.sync.localstore.SQLiteLocalStore;
import com.microsoft.windowsazure.mobileservices.table.sync.synchandler.SimpleSyncHandler;
import com.microsoft.windowsazure.notifications.NotificationsManager;
import com.smartfoo.android.core.logging.FooLog;
import com.swooby.alfred.MyNotificationsHandler;
import com.swooby.alfred.ToDoItem;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Azure
{
    private static final String TAG = FooLog.TAG("Azure");

    public static final String GOOGLE_CLOUD_MESSAGING_APP_ID = "234844979664";

    /**
     * Mobile Service Client reference
     */
    public static MobileServiceClient mClient;

    /**
     * Mobile Service Table used to access data
     */
    private MobileServiceTable<ToDoItem> mToDoTable;

    //Offline Sync
    /**
     * Mobile Service Table used to access and Sync data
     */
    //private MobileServiceSyncTable<ToDoItem> mToDoTable;

    /**
     * Progress spinner to use for table operations
     */
    private ProgressBar mProgressBar;

    private final Activity mActivity;
    private final ExecutorService mExecutorService;

    public Azure(Activity activity)
    {
        mActivity = activity;
        mExecutorService = Executors.newSingleThreadExecutor();
    }

    /**
     * Mark an item as completed
     *
     * @param item The item to mark
     */
    /*
    public void checkItem(final ToDoItem item)
    {
        if (mClient == null)
        {
            return;
        }

        // Set the item as completed and update it in the table
        item.setComplete(true);

        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>()
        {
            @Override
            protected Void doInBackground(Void... params)
            {
                try
                {
                    checkItemInTable(item);
                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            if (item.isComplete())
                            {
                                mAdapter.remove(item);
                            }
                        }
                    });
                }
                catch ( Exception e)
                {
                    createAndShowDialogFromTask(e, "Error");
                }

                return null;
            }
        };

        runAsyncTask(task);
    }
    */

    /**
     * Mark an item as completed in the Mobile Service Table
     *
     * @param item The item to mark
     */
    public void checkItemInTable(ToDoItem item)
            throws ExecutionException, InterruptedException
    {
        mToDoTable.update(item).get();
    }

    /**
     * Add a new item
     *
     * @param text text for the item
     */
    public void addItem(String text)
    {
        if (mClient == null)
        {
            return;
        }

        // Create a new item
        final ToDoItem item = new ToDoItem();

        item.setText(text);
        item.setComplete(false);

        // Insert the new item
        Runnable task = new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    final ToDoItem entity = addItemInTable(item);

                    mActivity.runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            if (!entity.isComplete())
                            {
                                //mAdapter.add(entity);
                            }
                        }
                    });
                }
                catch (Exception e)
                {
                    createAndShowDialogFromTask(e, "Error");
                }
            }
        };

        runAsyncTask(task);

        //mTextNewToDo.setText("");
    }

    /**
     * Add an item to the Mobile Service Table
     *
     * @param item The item to Add
     */
    public ToDoItem addItemInTable(ToDoItem item)
            throws ExecutionException, InterruptedException
    {
        return mToDoTable.insert(item).get();
    }

    /**
     * Refresh the list with the items in the Table
     */
    private void refreshItemsFromTable()
    {
        // Get the items that weren't marked as completed and add them in the adapter
        Runnable task = new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    final List<ToDoItem> results = refreshItemsFromMobileServiceTable();

                    //Offline Sync
                    //final List<ToDoItem> results = refreshItemsFromMobileServiceTableSyncTable();

                    mActivity.runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            /*
                            mAdapter.clear();

                            for (ToDoItem item : results)
                            {
                                mAdapter.add(item);
                            }
                            */
                        }
                    });
                }
                catch (Exception e)
                {
                    createAndShowDialogFromTask(e, "Error");
                }
            }
        };

        runAsyncTask(task);
    }

    /**
     * Refresh the list with the items in the Mobile Service Table
     */
    private List<ToDoItem> refreshItemsFromMobileServiceTable()
            throws ExecutionException, InterruptedException
    {
        return mToDoTable.where()
                .field("complete")
                .eq(QueryOperations.val(false))
                .execute()
                .get();
    }

    //Offline Sync
    /**
     * Refresh the list with the items in the Mobile Service Sync Table
     */
    /*
    private List<ToDoItem> refreshItemsFromMobileServiceTableSyncTable()
        throws ExecutionException, InterruptedException
    {
        //sync the data
        sync().get();
        Query query = QueryOperations.field("complete").
                eq(val(false));
        return mToDoTable.read(query).get();
    }
    */

    public static final String SHAREDPREFFILE = "temp";
    public static final String USERIDPREF     = "uid";
    public static final String TOKENPREF      = "tkn";

    private boolean loadUserTokenCache(MobileServiceClient client)
    {
        SharedPreferences prefs = mActivity.getSharedPreferences(SHAREDPREFFILE, Context.MODE_PRIVATE);

        String userId = prefs.getString(USERIDPREF, null);
        if (userId == null)
        {
            return false;
        }

        String token = prefs.getString(TOKENPREF, null);
        if (token == null)
        {
            return false;
        }

        MobileServiceUser user = new MobileServiceUser(userId);
        user.setAuthenticationToken(token);
        client.setCurrentUser(user);

        return true;
    }

    private void cacheUserToken(MobileServiceUser user)
    {
        mActivity.getSharedPreferences(SHAREDPREFFILE, Context.MODE_PRIVATE)
                .edit()
                .putString(USERIDPREF, user.getUserId())
                .putString(TOKENPREF, user.getAuthenticationToken())
                .commit();
    }

    public void authenticate()
    {
        try
        {
            // Create the Mobile Service Client instance using the Mobile Service URL
            mClient = new MobileServiceClient(
                    "https://alfred-mobile.azurewebsites.net",
                    mActivity)
                    .withFilter(new ProgressFilter());

            if (false)//loadUserTokenCache(mClient))
            {
                createTable();
            }
            else
            {
                ListenableFuture<MobileServiceUser> mLogin = mClient.login(MobileServiceAuthenticationProvider.Facebook);
                Futures.addCallback(mLogin, new FutureCallback<MobileServiceUser>()
                {
                    @Override
                    public void onFailure(
                            @NonNull
                            Throwable e)
                    {
                        //createAndShowDialog((Exception) e, "Error");
                        createAndShowDialog("You must log in. Login Required", "Error");
                    }

                    @Override
                    public void onSuccess(MobileServiceUser user)
                    {
                        createAndShowDialog(String.format("You are now logged in - %1$2s", user.getUserId()), "Success");
                        cacheUserToken(mClient.getCurrentUser());
                        createTable();
                    }
                });
            }
        }
        catch (MalformedURLException e)
        {
            createAndShowDialog(new Exception("There was an error creating the Mobile Service. Verify the URL"), "Error");
        }
        catch (Exception e)
        {
            createAndShowDialog(e, "Error");
        }
    }

    //
    // IoT BEGIN
    //

    //
    // https://azure.microsoft.com/en-us/documentation/articles/iot-hub-java-java-getstarted/
    //

    private static final String sIoTServiceHostName        = "alfred-mobile-iot-hub.azure-devices.net";
    private static final String sIoTServiceSharedAccessKey = "NW8vGVEPjQuafw45h0B/Q0drKggJcZB0Wub7CvNpLVM=";

    public interface IoTDeviceAddCallback
    {
        void onSuccess(Device device);

        void onException(String deviceId, Exception exception);
    }

    private static String createIoTServiceConnectionString(String hostName, String sharedAccessKey)
    {
        return DeviceClient.HOSTNAME_ATTRIBUTE + hostName +
               ";SharedAccessKeyName=iothubowner" +
               ';' + DeviceClient.SHARED_ACCESS_KEY_ATTRIBUTE + sharedAccessKey;
    }

    private static String createIoTDeviceConnectionString(String hostName, Device device)
    {
        return DeviceClient.HOSTNAME_ATTRIBUTE + hostName +
               ';' + DeviceClient.DEVICE_ID_ATTRIBUTE + device.getDeviceId() +
               ';' + DeviceClient.SHARED_ACCESS_KEY_ATTRIBUTE + device.getPrimaryKey();
    }

    public void iotDeviceAdd(String deviceId, IoTDeviceAddCallback callback)
    {
        runAsyncTask(() -> {

            Device tempDevice;
            Exception tempException;

            try
            {
                String serviceConnectionString = createIoTServiceConnectionString(sIoTServiceHostName, sIoTServiceSharedAccessKey);

                RegistryManager registryManager = RegistryManager.createFromConnectionString(serviceConnectionString);

                tempDevice = Device.createFromId(deviceId, null, null);

                try
                {
                    tempDevice = registryManager.addDevice(tempDevice);
                    tempException = null;
                }
                catch (IotHubException iote)
                {
                    try
                    {
                        tempDevice = registryManager.getDevice(deviceId);
                        tempException = null;
                    }
                    catch (IotHubException iotf)
                    {
                        tempException = iotf;
                    }
                }
            }
            catch (Exception e)
            {
                tempDevice = null;
                tempException = e;
            }

            final Device device = tempDevice;
            final Exception exception = tempException;

            mActivity.runOnUiThread(() -> {
                if (callback != null)
                {
                    if (device != null)
                    {
                        callback.onSuccess(device);
                    }
                    else
                    {
                        callback.onException(deviceId, exception);
                    }
                }
            });
        });
    }

    public interface IoTDeviceSendMessageCallback
    {
        void onSuccess(Device device, DeviceClient deviceClient);

        void onException(Device device, Exception exception);
    }

    public void iotDeviceSendMessage(Device device, String messageText, IoTDeviceSendMessageCallback callback)
    {
        runAsyncTask(() -> {
            DeviceClient tempDeviceClient;
            Exception tempException;

            try
            {
                String connectionString = createIoTDeviceConnectionString(sIoTServiceHostName, device);

                tempDeviceClient = new DeviceClient(connectionString, IotHubClientProtocol.MQTT);

                tempDeviceClient.open();

                Message message = new Message(messageText);
                IotHubEventCallback iotHubEventCallback = (responseStatus, callbackContext) -> {
                    FooLog.i(TAG,
                            "iotDeviceSendMessage: IoT Hub responded to message with status " +
                            responseStatus.name());
                    if (callbackContext != null)
                    {
                        //noinspection SynchronizationOnLocalVariableOrMethodParameter
                        synchronized (callbackContext)
                        {
                            callbackContext.notify();
                        }
                    }
                };
                Object callbackContext = new Object();

                tempDeviceClient.sendEventAsync(message, iotHubEventCallback, callbackContext);

                //noinspection SynchronizationOnLocalVariableOrMethodParameter
                synchronized (callbackContext)
                {
                    try
                    {
                        callbackContext.wait();
                    }
                    catch (InterruptedException e)
                    {
                        // ignore
                    }
                }

                tempException = null;
            }
            catch (Exception e)
            {
                tempDeviceClient = null;
                tempException = e;
            }

            DeviceClient deviceClient = tempDeviceClient;
            Exception exception = tempException;

            mActivity.runOnUiThread(() -> {
                if (callback != null)
                {
                    if (deviceClient != null)
                    {
                        callback.onSuccess(device, deviceClient);
                    }
                    else
                    {
                        callback.onException(device, exception);
                    }
                }
            });

            if (deviceClient != null)
            {
                try
                {
                    deviceClient.close();
                }
                catch (IOException e)
                {
                    // ignore
                }
            }
        });
    }

    //
    // IoT END
    //

    private void createTable()
    {
        // TODO:(pv) Rewrite NotificationsManager to not use deprecated Google APIs
        //  https://developers.google.com/cloud-messaging/android/start
        //  https://developers.google.com/cloud-messaging/android/client
        //  https://developers.google.com/android/guides/setup
        NotificationsManager.handleNotifications(mActivity, GOOGLE_CLOUD_MESSAGING_APP_ID, MyNotificationsHandler.class);

        try
        {
            // Get the Mobile Service Table instance to use
            mToDoTable = mClient.getTable(ToDoItem.class);

            // Offline Sync
            //mToDoTable = mClient.getSyncTable("ToDoItem", ToDoItem.class);

            //Init local storage
            // NOTE:(pv) Blocking?!?!!?
            initLocalStore().get();

            /*
            mTextNewToDo = (EditText) findViewById(R.id.textNewToDo);

            // Create an adapter to bind the items with the view
            mAdapter = new ToDoItemAdapter(this, R.layout.row_list_to_do);
            ListView listViewToDo = (ListView) findViewById(R.id.listViewToDo);
            listViewToDo.setAdapter(mAdapter);
            */

            // Load the items from the Mobile Service
            refreshItemsFromTable();
        }
        catch (InterruptedException | ExecutionException | MobileServiceLocalStoreException e)
        {
            createAndShowDialog(e, "Error");
        }
    }

    /**
     * Initialize local storage
     *
     * @return
     * @throws MobileServiceLocalStoreException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    private Future<Void> initLocalStore()
            throws MobileServiceLocalStoreException, ExecutionException, InterruptedException
    {
        Callable<Void> task = new Callable<Void>()
        {
            @Override
            public Void call()
                    throws Exception
            {
                try
                {
                    MobileServiceSyncContext syncContext = mClient.getSyncContext();

                    if (syncContext.isInitialized())
                    {
                        return null;
                    }

                    SQLiteLocalStore localStore = new SQLiteLocalStore(mClient.getContext(), "OfflineStore", null, 1);

                    Map<String, ColumnDataType> tableDefinition = new HashMap<String, ColumnDataType>();
                    tableDefinition.put("id", ColumnDataType.String);
                    tableDefinition.put("text", ColumnDataType.String);
                    tableDefinition.put("complete", ColumnDataType.Boolean);

                    localStore.defineTable("ToDoItem", tableDefinition);

                    SimpleSyncHandler handler = new SimpleSyncHandler();

                    syncContext.initialize(localStore, handler).get();
                }
                catch (Exception e)
                {
                    createAndShowDialogFromTask(e, "Error");
                }

                return null;
            }
        };

        return runAsyncTask(task);
    }

    //Offline Sync
    /**
     * Sync the current context and the Mobile Service Sync Table
     * @return
     */
    /*
    private AsyncTask<Void, Void, Void> sync() {
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    MobileServiceSyncContext syncContext = mClient.getSyncContext();
                    syncContext.push().get();
                    mToDoTable.pull(null).get();
                } catch ( Exception e) {
                    createAndShowDialogFromTask(e, "Error");
                }
                return null;
            }
        };
        return runAsyncTask(task);
    }
    */

    /**
     * Creates a dialog and shows it
     *
     * @param exception The exception to show in the dialog
     * @param title     The dialog title
     */
    private void createAndShowDialogFromTask(final Exception exception, String title)
    {
        mActivity.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                createAndShowDialog(exception, "Error");
            }
        });
    }

    /**
     * Creates a dialog and shows it
     *
     * @param exception The exception to show in the dialog
     * @param title     The dialog title
     */
    private void createAndShowDialog(Exception exception, String title)
    {
        Throwable ex = exception;
        if (exception.getCause() != null)
        {
            ex = exception.getCause();
        }
        createAndShowDialog(ex.getMessage(), title);
    }

    /**
     * Creates a dialog and shows it
     *
     * @param message The dialog message
     * @param title   The dialog title
     */
    private void createAndShowDialog(String message, String title)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);

        builder.setMessage(message);
        builder.setTitle(title);
        builder.create().show();
    }

    /**
     * Run an ASync task on the corresponding executor
     *
     * @param task
     * @return
     */
    private <T> Future<T> runAsyncTask(Callable<T> task)
    {
        return mExecutorService.submit(task);
    }

    private Future<?> runAsyncTask(Runnable task)
    {
        return mExecutorService.submit(task);
    }

    private class ProgressFilter
            implements ServiceFilter
    {
        @Override
        public ListenableFuture<ServiceFilterResponse> handleRequest(ServiceFilterRequest request, NextServiceFilterCallback nextServiceFilterCallback)
        {
            final SettableFuture<ServiceFilterResponse> resultFuture = SettableFuture.create();

            mActivity.runOnUiThread(new Runnable()
            {

                @Override
                public void run()
                {
                    if (mProgressBar != null)
                    {
                        mProgressBar.setVisibility(ProgressBar.VISIBLE);
                    }
                }
            });

            ListenableFuture<ServiceFilterResponse> future = nextServiceFilterCallback.onNext(request);

            Futures.addCallback(future, new FutureCallback<ServiceFilterResponse>()
            {
                @Override
                public void onFailure(Throwable e)
                {
                    resultFuture.setException(e);
                }

                @Override
                public void onSuccess(ServiceFilterResponse response)
                {
                    mActivity.runOnUiThread(new Runnable()
                    {

                        @Override
                        public void run()
                        {
                            if (mProgressBar != null)
                            {
                                mProgressBar.setVisibility(ProgressBar.GONE);
                            }
                        }
                    });

                    resultFuture.set(response);
                }
            });

            return resultFuture;
        }
    }
}

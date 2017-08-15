package com.startline.slble.Activity;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.widget.*;
import com.startline.slble.Adapter.BleSerializableDeviceAdapter;
import com.startline.slble.BuildConfig;
import com.startline.slble.PureClass.BleSerializableDevice;
import com.startline.slble.PureClass.Constants;
import com.startline.slble.R;
import com.startline.slble.Service.BluetoothLeIndependentService;
import com.startline.slble.Util.LogUtil;
import com.startline.slble.Util.StorageUtil;
import com.startline.slble.Util.TimeUtil;
import com.startline.slble.view.MyProgressDialog;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.startline.slble.Service.BluetoothLeIndependentService.*;


public class AutoConnectActivity extends Activity
{
    //*****************************************************************//
    //  Constant Variables                                             //
    //*****************************************************************//
    private final static String TAG = "BLE";
    private static final int REQUEST_ENABLE_BT = 1;


    //*****************************************************************//
    //  Global Variables                                               //
    //*****************************************************************//
    private boolean mIsScanning = false;

    //*****************************************************************//
    //  Object                                                         //
    //*****************************************************************//
    private Context context;
    private BluetoothAdapter mBluetoothAdapter;
    private BleSerializableDeviceAdapter mLeDeviceListAdapter;
    private BleSerializableDeviceAdapter mBondDeviceListAdapter;
    private BluetoothLeIndependentService mService = null;
    private HashMap<String,String> mHashAutoConnectDevice = null;

    //*****************************************************************//
    //  View                                                           //
    //*****************************************************************//
    private TextView txtAutoConnectDevice;
    private Button btnScan = null;
    private ListView listViewScan = null;
    private ListView listViewBond = null;
    private ProgressBar progressScanDevice = null;
    private MyProgressDialog progressDialog = null;
    private CheckedTextView ckWriteLog = null;


    private Handler mHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            if(msg.arg1 == BluetoothLeIndependentService.PARAM_SCAN_RESULT)
            {
                if(!mIsScanning)
                    return;
                final ArrayList<BleSerializableDevice> deviceList = (ArrayList<BleSerializableDevice>)msg.obj;
                mLeDeviceListAdapter.setDevice(deviceList);
                mLeDeviceListAdapter.notifyDataSetChanged();
            }
        }
    };

    // Receive message from BluetoothIndependentService
    private final BroadcastReceiver mNotifyMessageReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            final String action = intent.getAction();
            final int param = intent.getIntExtra("param",-1);
            if(action.equals(ACTION_SERVICE_NOTIFY_UI))
            {
                switch (param)
                {
                    case PARAM_CONNECT_STATUS:
                    {
                        int stringId = 0;
                        final int connectionStatus = intent.getIntExtra("connectionStatus",0);
                        switch (connectionStatus)
                        {
                            case CONNECTION_STATE_BONDING:
                                stringId = R.string.bonding;
                                break;
                            case CONNECTION_STATE_BOND_FAILED:
                                stringId = R.string.bond_failed;
                                break;
                            case CONNECTION_STATE_BOND_SUCCESS:
                                stringId = R.string.bond_success;
                                break;
                        }

                        if(stringId > 0)
                        {
                            progressDialog.setMessage(getString(stringId));
                            progressDialog.setCancelable(true);
                        }

                        if(connectionStatus == CONNECTION_STATE_BOND_SUCCESS)
                        {
                            updateBondedListAdapter();
                            progressDialog.setImageSrc(R.drawable.check);
                            progressDialog.showImage(true);
                            progressDialog.showButton(true);
                        }
                        else if(connectionStatus == CONNECTION_STATE_BOND_FAILED)
                        {
                            progressDialog.setImageSrc(R.drawable.cross);
                            progressDialog.showImage(true);
                            progressDialog.showButton(true);
                        }
                    }
                    break;
                }
            }
        }
    };


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.auto_connect_device);


        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
        {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
        }

        context = this;

        setupViews();

        ckWriteLog.setChecked(StorageUtil.getWriteLogConfig(context));
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        loadAutoConnectDeviceInfo();
        displayAutoConnectDevice();

        registerReceiver(mNotifyMessageReceiver, makeIntentFilter());

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if ( getBlueAdapter() != null &&  !mBluetoothAdapter.isEnabled())
        {
            if (!mBluetoothAdapter.isEnabled())
            {
                final Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }


        // Initializes list view adapter.
        if(mLeDeviceListAdapter == null)
            mLeDeviceListAdapter = new BleSerializableDeviceAdapter(context,BleSerializableDeviceAdapter.DEVICE_TYPE_SCAN);
        listViewScan.setAdapter(mLeDeviceListAdapter);


        int delayTime = 0;
        if(isServiceRunning(BluetoothLeIndependentService.class) == false)
        {
            startBluetoothLeService();
            delayTime = 500;
        }

        mHandler.postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                final BluetoothLeIndependentService service = BluetoothLeIndependentService.getInstance();
                if(service != null)
                {
                    mService = service;
                    service.setIpcCallbackhandler(mHandler);
                }
            }
        },delayTime);


        // Update bonded device list
        if(mBondDeviceListAdapter == null)
        {
            mBondDeviceListAdapter = new BleSerializableDeviceAdapter(context,BleSerializableDeviceAdapter.DEVICE_TYPE_AUTO_CONNECT);
            mBondDeviceListAdapter.setAutoConnectDeviceAddress(mHashAutoConnectDevice.get(Constants.CONFIG_ITEM_DEVICE_ADDRESS));
            mBondDeviceListAdapter.setUnBondOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    final String address = (String)v.getTag();
                    final Set<BluetoothDevice> deviceSet = getBondedDevice();
                    BluetoothDevice device = null;

                    // Get selected device
                    for (BluetoothDevice dev : deviceSet)
                    {
                        if(dev.getAddress().equals(address))
                        {
                            device = dev;
                            break;
                        }
                    }

                    if(device != null)
                    {
                        final BluetoothLeIndependentService service = BluetoothLeIndependentService.getInstance();
                        if(service != null)
                        {
                            // If unbond device is connected with App, disconnect and close GATT to release it
                            final BluetoothLeIndependentService.CachedBluetoothDevice cachedBluetoothDevice = service.getCachedBluetoothDevice(address);
                            final BluetoothDevice bluetoothDevice = service.getConnectedBluetoothDevice(address);
                            if(cachedBluetoothDevice != null || bluetoothDevice != null)
                            {
                                LogUtil.d(TAG,"Release all connection before un-bond",Thread.currentThread().getStackTrace());
                                service.cutAllConnection();
                            }
                        }
                    }
                    // device not found
                    else
                    {

                    }

                    unBondDevice(device);

                    mHandler.postDelayed(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            // After unbond, refresh list adapter
                            updateBondedListAdapter();
                        }
                    },800);
                }
            });
            listViewBond.setAdapter(mBondDeviceListAdapter);
        }


        updateBondedListAdapter();
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        if(mIsScanning)
            btnScan.performClick();


        final BluetoothLeIndependentService service = BluetoothLeIndependentService.getInstance();
        if(service != null)
        {
            service.setIpcCallbackhandler(null);
        }
    }

    @Override
    protected void onStop()
    {
        super.onStop();

        unregisterReceiver(mNotifyMessageReceiver);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED)
        {
            finish();
            return;
        }
    }

    private BluetoothAdapter getBlueAdapter()
    {

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null)
        {
            Toast.makeText(this, R.string.error_get_ble, Toast.LENGTH_SHORT).show();
            //finish();
            return null;
        }

        return mBluetoothAdapter;
    }

    private void setupViews()
    {
        try
        {
            progressDialog = new MyProgressDialog(context);

            txtAutoConnectDevice = (TextView)findViewById(R.id.txt_auto_connect_device);
            progressScanDevice = (ProgressBar) findViewById(R.id.progress_scan_device);

            listViewScan = (ListView)findViewById(R.id.list_view_available_device);
            listViewScan.setOnItemClickListener(new AdapterView.OnItemClickListener()
            {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id)
                {
                    if (mIsScanning)
                    {
                        btnScan.performClick();
                    }

                    final BleSerializableDevice device = mLeDeviceListAdapter.getDevice(position);
                    if (device == null)
                    {
                        return;
                    }

                    if(mBondDeviceListAdapter != null && mBondDeviceListAdapter.getCount() > 0)
                    {
                        for(int i=0;i<mBondDeviceListAdapter.getCount();i++)
                        {
                            final BleSerializableDevice serializableDevice = mBondDeviceListAdapter.getDevice(i);
                            if(serializableDevice.address.equals(device.address))
                            {
                                progressDialog.show();
                                progressDialog.showProgressBar(false);
                                progressDialog.showButton(false);
                                progressDialog.setCancelable(true);
                                progressDialog.setMessage(String.format("%s(%s) was already paired",device.name,device.address));
                                return;
                            }
                        }
                    }

                    if(mService != null)
                    {
                        progressDialog.show();
                        progressDialog.showProgressBar(true);
                        progressDialog.showButton(false);
                        progressDialog.setCancelable(false);
                        progressDialog.setMessage("Prepare for pairing...");
                        mService.bondingDevice(device.address);
                    }

                    //startDeviceConnectActivity(device.name,device.address);
                }
            });

            listViewBond = (ListView)findViewById(R.id.list_view_paired_device);
            listViewBond.setOnItemClickListener(new AdapterView.OnItemClickListener()
            {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id)
                {
                    if (mIsScanning)
                    {
                        btnScan.performClick();
                    }


                    final BleSerializableDevice device = mBondDeviceListAdapter.getDevice(position);
                    if (device == null)
                    {
                        return;
                    }

                    final String previousAddress = mHashAutoConnectDevice.get(Constants.CONFIG_ITEM_DEVICE_ADDRESS);
                    if(previousAddress.equals(device.address))
                    {
                        mHashAutoConnectDevice.put(Constants.CONFIG_ITEM_DEVICE_NAME, "");
                        mHashAutoConnectDevice.put(Constants.CONFIG_ITEM_DEVICE_ADDRESS, "");

                        displayAutoConnectDevice();
                        saveAutoConnectDeviceInfo();

                        if(mService != null)
                        {
                            mService.stopConnectThread();
                            mService.initConnectDevice("");
                        }
                    }
                    else
                    {
                        mHashAutoConnectDevice.put(Constants.CONFIG_ITEM_DEVICE_NAME, device.name);
                        mHashAutoConnectDevice.put(Constants.CONFIG_ITEM_DEVICE_ADDRESS, device.address);
                        displayAutoConnectDevice();
                        saveAutoConnectDeviceInfo();

                        if(mService != null)
                        {
                            mService.stopConnectThread();
                            mService.initConnectDevice(device.address);
                        }
                    }

                    mBondDeviceListAdapter.setAutoConnectDeviceAddress(mHashAutoConnectDevice.get(Constants.CONFIG_ITEM_DEVICE_ADDRESS));
                    mBondDeviceListAdapter.notifyDataSetChanged();
                    //startDeviceConnectActivity(device.name,device.address);
                }
            });

            btnScan = (Button)findViewById(R.id.btn_scan);
            btnScan.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    LogUtil.w(TAG, "Discovering : " + mBluetoothAdapter.isDiscovering(), Thread.currentThread().getStackTrace());

                    // Stop Scan
                    if(mIsScanning)
                    {
                        progressScanDevice.setVisibility(View.GONE);
                        sendUiActionBroadcast(notifyStopScanDevice());
                    }
                    // Start Scan
                    else
                    {
                        progressScanDevice.setVisibility(View.VISIBLE);
                        mLeDeviceListAdapter.clear();
                        sendUiActionBroadcast(notifyStartScanDevice());
                    }
                }
            });


            ckWriteLog = (CheckedTextView)findViewById(R.id.ck_write_log);
            ckWriteLog.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    final boolean writeLog = !ckWriteLog.isChecked();
                    ckWriteLog.setChecked(writeLog);
                    StorageUtil.saveWriteLogConfig(context, writeLog);

                    final BluetoothLeIndependentService service = BluetoothLeIndependentService.getInstance();
                    if(service != null)
                    {
                        service.setWriteLog(writeLog);
                    }
                }
            });
        }
        catch (Exception e)
        {
            LogUtil.d(getPackageName(), e.toString(), Thread.currentThread().getStackTrace());
        }
    }

    private void loadAutoConnectDeviceInfo()
    {
        mHashAutoConnectDevice = StorageUtil.getAutoConnectDevice(context);
    }

    private void saveAutoConnectDeviceInfo()
    {
        StorageUtil.saveAutoConnectDevice(context, mHashAutoConnectDevice.get(Constants.CONFIG_ITEM_DEVICE_NAME), mHashAutoConnectDevice.get(Constants.CONFIG_ITEM_DEVICE_ADDRESS));
    }

    private void displayAutoConnectDevice()
    {
        txtAutoConnectDevice.setText(String.format("%s\n%s",mHashAutoConnectDevice.get(Constants.CONFIG_ITEM_DEVICE_NAME), mHashAutoConnectDevice.get(Constants.CONFIG_ITEM_DEVICE_ADDRESS)));
    }

    private void updateBondedListAdapter()
    {
        final Set<BluetoothDevice> deviceSet = getBondedDevice();
        final ArrayList<BleSerializableDevice> deviceList = new ArrayList<>();
        for (BluetoothDevice device : deviceSet)
        {
            final BleSerializableDevice bleSerializableDevice = new BleSerializableDevice();
            bleSerializableDevice.name = device.getName();
            bleSerializableDevice.address = device.getAddress();
            deviceList.add(bleSerializableDevice);
        }
        mBondDeviceListAdapter.setDevice(deviceList);
        mBondDeviceListAdapter.notifyDataSetChanged();
    }

    private void unBondAllDevice(final int delayTime)
    {
        try
        {
            Thread.sleep(delayTime);

            final Set<BluetoothDevice> deviceSet = getBondedDevice();

            // Get selected device
            for (BluetoothDevice dev : deviceSet)
            {
                unBondDevice(dev);
                do
                {
                    Thread.sleep(100);
                }
                while(dev.getBondState() != BluetoothDevice.BOND_NONE);
            }

            Thread.sleep(500);
        }
        catch (Exception e)
        {

        }
    }

    private void unBondDevice(final BluetoothDevice device)
    {
        try
        {
            final Method method = device.getClass().getMethod("removeBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
        }
        catch (Exception e)
        {
            Log.e(TAG, e.getMessage());
        }
    }

    private void sendUiActionBroadcast(final Intent intent)
    {
        Log.i(TAG, "sendUiActionBroadcast : " + intent.getIntExtra("mode",-1));
        intent.setAction(BluetoothLeIndependentService.ACTION_UI_NOTIFY_SERVICE);
        sendBroadcast(intent);
    }

    private Intent notifyStartScanDevice()
    {
        mIsScanning = true;
        btnScan.setText("Stop");

        final Intent intent = new Intent();
        intent.putExtra("mode",BluetoothLeIndependentService.ACTION_START_SCAN);
        return intent;
    }

    private Intent notifyStopScanDevice()
    {
        mIsScanning = false;
        btnScan.setText("Scan");

        final Intent intent = new Intent();
        intent.putExtra("mode",BluetoothLeIndependentService.MODE_STOP_SCAN);
        return intent;
    }

    private void startBluetoothLeService()
    {
        final Intent intent = new Intent();
        intent.setClass(context, BluetoothLeIndependentService.class);
        context.startService(intent);
    }

    private void stopBluetoothLeService()
    {
        final Intent intent = new Intent();
        intent.setClass(context,BluetoothLeIndependentService.class);
        context.stopService(intent);
    }

    private Set<BluetoothDevice> getBondedDevice()
    {
        final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        final Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        return pairedDevices;
    }

    private IntentFilter makeIntentFilter()
    {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_SERVICE_NOTIFY_UI);
        return intentFilter;
    }

    protected boolean isServiceRunning(final Class serviceClass)
    {
        boolean running = false;
        final ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE))
        {
            if (serviceClass.getName().equalsIgnoreCase(service.service.getClassName())
                    && service.service.getPackageName().equalsIgnoreCase(context.getPackageName()))
            {
                running = true;
                break;
            }
        }
        return running;
    }
}
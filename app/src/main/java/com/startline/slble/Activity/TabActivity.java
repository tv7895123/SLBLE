/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.startline.slble.Activity;

import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.*;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTabHost;
import android.util.Log;
import android.view.*;
import android.widget.*;

import com.startline.slble.Fragment.DeviceConnectFragment;
import com.startline.slble.Fragment.DeviceStatusFragment;
import com.startline.slble.R;

import com.startline.slble.Service.BluetoothLeIndependentService;
import com.startline.slble.Util.LogUtil;

import java.util.ArrayList;

import static com.startline.slble.Service.BluetoothLeIndependentService.*;
/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */

//@formatter:off
public class TabActivity extends FragmentActivity
{
    //  Common State
	public static final int OFF = 0;
	public static final int ON = 1;
    //*****************************************************************//
    //  Constant Variables                                             //
    //*****************************************************************//
	private final static String TAG = TabActivity.class.getSimpleName();
	public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
	private final String DEVICE_CONNECT_TAG = "Debug";
	private final String DEVICE_STATUS_TAG = "Status";
	public static final boolean SUPPORT_MULTI_DEVICE = false;

    //*****************************************************************//
    //  Global Variables                                               //
    //*****************************************************************//
    private String mDeviceName;
    private String mDeviceAddress;
	private boolean mConnected = false;
	private boolean mAutoScrollDown = false;
	private boolean mThermalTest = false;
	private boolean mRegisterSuccess = false;
	private int mCstaMode = -1;
	private long mCsta = 0;


    //*****************************************************************//
    //  Object                                                         //
    //*****************************************************************//
	private Context context;
	private Handler mHandler = null;
    private BluetoothLeIndependentService mBluetoothLeService = null;
	private DeviceConnectFragment mDeviceConnectFragment = null;
	private DeviceStatusFragment mDeviceStatusFragment = null;


    //*****************************************************************//
    //  View                                                           //
    //*****************************************************************//
    private FragmentTabHost mTabHost = null;
	private RelativeLayout layoutDebugInfo = null;
	private ProgressDialog mProgressDialog = null;


	// ACL action receiver, including connect and disconnect
	private final BroadcastReceiver mAclActionReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			final String action = intent.getAction();
			final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

			if (BluetoothDevice.ACTION_FOUND.equals(action))
			{
				//Toast.makeText(context, device.getName() + " Device found", Toast.LENGTH_LONG).show();
			}
			else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action))
			{
                //mBluetoothLeService.connectDevice(device.getAddress(),device.getName());
				//Toast.makeText(context, device.getName() + " Device is now connected", Toast.LENGTH_LONG).show();
			}
			else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action))
			{
				//Toast.makeText(context, device.getName() + " Device is about to disconnect", Toast.LENGTH_LONG).show();
			}
			else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action))
			{
				//Toast.makeText(context, device.getName() + " Device has disconnected", Toast.LENGTH_LONG).show();
			}
		}
	};


    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection()
	{
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service)
		{
            mBluetoothLeService = ((BluetoothLeIndependentService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize())
			{
                Log.e(TAG, "Unable to initialize Bluetooth");
				//Toast.makeText(DeviceControlActivity.this,"Unable to initialize Bluetooth",Toast.LENGTH_LONG).show();
                finish();
            }


            mTabHost.setCurrentTab(1);

            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connectDevice(mDeviceAddress,mDeviceName);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName)
		{
            mBluetoothLeService = null;
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
                    case PARAM_GATT_CONNECTED:
                    {
                        setConnected(true);

                        // Update title
//							final String nameUTF8 = mBluetoothLeService.getDevice().nameUTF8;
//							mDeviceName = (nameUTF8 != null)? nameUTF8 : mBluetoothLeService.getDevice().bluetoothDevice.getName();
//							getActionBar().setTitle(mDeviceName);

                        // Update connection status
                        updateConnectionState(R.string.connected);

                        // Update action bar menu
                        updateActionBarMenu();
                    }
                    break;

                    case PARAM_GATT_DISCONNECTED:
                    {
                        handleOnDisconnected();
                    }
                    break;

                    case PARAM_GATT_SERVICES_DISCOVERED:
                    {
                        appendLog("Services discovered");
                    }
                    break;

                    case PARAM_GATT_READ_DEVICE_NAME:
                    {
                        // Update title
                        final String nameUTF8 = mBluetoothLeService.getBleDeviceName();
                        mDeviceName = (nameUTF8 != null)? nameUTF8 : mBluetoothLeService.getDevice().bluetoothDevice.getName();
                        getActionBar().setTitle(mDeviceName);
                    }
                    break;

                    case PARAM_CONNECT_STATUS:
                    {
                        int stringId = 0;
                        switch (intent.getIntExtra("connectionStatus",0))
                        {
                            case CONNECTION_STATE_BONDING:
                                stringId = R.string.bonding;
                                updateConnectionStatusIcon(true,false);
                                break;
                            case CONNECTION_STATE_CONNECTING:
                                stringId = R.string.connecting;
                                updateConnectionStatusIcon(true,false);
                                break;
                            case CONNECTION_STATE_AUTO_CONNECTING:
                                stringId = R.string.auto_connecting;
                                updateConnectionStatusIcon(true,false);
                                break;
                            case CONNECTION_STATE_RE_CONNECTING:
                                stringId = R.string.re_connecting;
                                updateConnectionStatusIcon(true,false);
                                break;
                            case CONNECTION_STATE_CANCEL:
                                stringId = R.string.connect_cancelled;
                                updateConnectionStatusIcon(false,false);
                                break;
                            case CONNECTION_STATE_BOND_FAILED:
                                stringId = R.string.bond_failed;
                                updateConnectionStatusIcon(false,false);
                                break;
                        }

                        if(stringId > 0)
                        {

                            appendLog(getString(stringId));

                            setConnected(false);

                            // Update action bar menu
                            updateActionBarMenu();

                            // Update DeviceConnectFragment
                            updateConnectionState(stringId);


                            // Update DeviceStatusFragment
                            if(mRegisterSuccess)
                            {
                                mRegisterSuccess = false;
                                return;
                            }
                            updateConnectionStatus(stringId);
                        }
                    }
                    break;

                    case PARAM_PROCESS_STEP:
                    {
                        final int step = intent.getIntExtra("step",-1);
                        switch (step)
                        {
                            case INIT_STATE_END:
                            {
                                updateConnectionStatusIcon(true,true);
                                displayDeviceMessage("Connect success");

                                if(mCstaMode > -1)
                                {
                                    displayDeviceStatus(mCstaMode,mCsta);
                                }
                            }
                            break;
                        }
                    }
                    break;

                    case PARAM_RSSI:
                    {
                        final int rssi = intent.getIntExtra("rssi",0);
                        final int avgRssi =intent.getIntExtra("avg_rssi",0);
                        displayBleRssi(rssi,avgRssi);
                    }
                    break;

                    case PARAM_CSTA:
                    {
                        mCsta = intent.getLongExtra("csta",-1);
                        mCstaMode = intent.getIntExtra("mode",-1);

                        final boolean result = displayDeviceStatus(mCstaMode,mCsta);
                        if(result)
                        {
                            mCstaMode = -1;
                            mCsta = 0;
                        }
                    }
                    break;
                }
            }
			else if(action.equals(ACTION_DEBUG_SERVICE_TO_UI))
			{
                switch (param)
                {
                    case PARAM_THERMAL:
                    {
                        final int mode = intent.getIntExtra("mode",0);
                        final ArrayList<String> arrayList = new ArrayList<>();
                        if((mode & 0x1) > 0)
                        {
                            final String firstConnect = intent.getStringExtra("first_connect");
                            arrayList.add(firstConnect);
                        }

                        if((mode & 0x2) > 0)
                        {
                            final String lastConnect = intent.getStringExtra("last_connect");
                            arrayList.add(lastConnect);
                        }

                        if((mode & 0x4) > 0)
                        {
                            final String firstDisconnect = intent.getStringExtra("first_disconnect");
                            arrayList.add(firstDisconnect);
                        }

                        if((mode & 0x8) > 0)
                        {
                            final String lastDisconnect = intent.getStringExtra("last_disconnect");
                            arrayList.add(lastDisconnect);
                        }

                        if((mode & 0x10) > 0)
                        {
                            final String thermalCommandCount = intent.getStringExtra("thermal_command_count");
                            arrayList.add(thermalCommandCount);
                        }

                        if((mode & 0x20) > 0)
                        {
                            final String thermalCstaCount = intent.getStringExtra("thermal_csta_count");
                            arrayList.add(thermalCstaCount);
                        }

                        if((mode & 0x40) > 0)
                        {
                            final String disconnectCount = intent.getStringExtra("disconnect_count");
                            arrayList.add(disconnectCount);
                        }

                        updateThermalInfo(mode,arrayList.toArray(new String[arrayList.size()]));
                    }
                    break;
                    case PARAM_LOG:
                    {
                        final String log = intent.getStringExtra("log");
                        appendLog(log);
                    }
                    break;
                }
			}
		}
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.connect_activity_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// Handle presses on the action bar items
		switch (item.getItemId())
		{
			case android.R.id.home:
				finish();
            	return true;
			case R.id.action_setting:
			{
				final Intent intent = new Intent();
				intent.setClass(context,SettingActivity.class);
				startActivity(intent);
			}
			return true;

			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
        //setContentView(R.layout.device_connect);
		setContentView(R.layout.tab_activity);
		context = this;
		mHandler = new Handler();

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);


		getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);

		setupViews();

		final Intent gattServiceIntent = new Intent(this, BluetoothLeIndependentService.class);
		bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

		registerReceiver(mNotifyMessageReceiver, makeIntentFilter());


        // Register ACL action receiver
        if(SUPPORT_BONDING)
        {
            final IntentFilter filter = new IntentFilter(android.bluetooth.BluetoothDevice.ACTION_ACL_DISCONNECTED);
            filter.addAction(android.bluetooth.BluetoothDevice.ACTION_ACL_CONNECTED);
            filter.addAction(android.bluetooth.BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
            registerReceiver(mAclActionReceiver,filter);
        }
	}

    @Override
    protected void onResume()
	{
        super.onResume();

		getAppConfiguration();

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		updateActionBarMenu();

		showThermalInfo(mThermalTest);
    }

    @Override
    protected void onPause()
	{
        super.onPause();
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
	protected void onDestroy()
	{
        super.onDestroy();
		unregisterReceiver(mNotifyMessageReceiver);

		// If doesn't support multi-device connection, need to disconnect when activity destroy
		if(SUPPORT_MULTI_DEVICE == false)
		{
			mBluetoothLeService.disconnect(false);
			unbindService(mServiceConnection);
			mBluetoothLeService = null;
		}

        if(SUPPORT_BONDING)
        {
            unregisterReceiver(mAclActionReceiver);
        }

		//stopService(new Intent().setClass(this,BluetoothLeIndependentService.class));
    }

    public String getDeviceInfo()
    {
        return mDeviceAddress+","+mDeviceName;
    }

	private DeviceConnectFragment getDeviceConnectFragment()
	{
		if(mTabHost == null) return null;

		if(mDeviceConnectFragment == null)
		{
			mDeviceConnectFragment = (DeviceConnectFragment)getSupportFragmentManager().findFragmentByTag(DEVICE_CONNECT_TAG);
		}

		return mDeviceConnectFragment;
	}

	private DeviceStatusFragment getDeviceStatusFragment()
	{
		if(mTabHost == null) return null;

		if(mDeviceStatusFragment == null)
		{
			mDeviceStatusFragment = (DeviceStatusFragment)getSupportFragmentManager().findFragmentByTag(DEVICE_STATUS_TAG);
		}

		return mDeviceStatusFragment;
	}

	public Handler getHandler()
	{
		return mHandler;
	}

	private void setConnected(final boolean connected)
	{
		mConnected = connected;
		notifyDeviceConnected(connected);
	}

	private void setupViews()
	{
		try
		{
            mTabHost = (FragmentTabHost)findViewById(android.R.id.tabhost);
            mTabHost.setup(this, getSupportFragmentManager(), R.id.realtabcontent);

			mTabHost.addTab(mTabHost.newTabSpec(DEVICE_CONNECT_TAG).setIndicator(DEVICE_CONNECT_TAG), DeviceConnectFragment.class, null);
			mTabHost.addTab(mTabHost.newTabSpec(DEVICE_STATUS_TAG).setIndicator(DEVICE_STATUS_TAG), DeviceStatusFragment.class, null);
			for (int i = 0; i < mTabHost.getTabWidget().getChildCount(); i++)
			{
				mTabHost.getTabWidget().getChildAt(i).getLayoutParams().height = 65;
			}

			// find view by id
			layoutDebugInfo = (RelativeLayout)findViewById(R.id.layout_debug_info);
		}
		catch (Exception e)
		{
			LogUtil.e(TAG, e.toString(), Thread.currentThread().getStackTrace());
		}
	}

	private void getAppConfiguration()
	{
		final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		mAutoScrollDown = sharedPreferences.getBoolean(getString(R.string.pref_key_auto_scroll),false);
		mThermalTest = sharedPreferences.getBoolean(getString(R.string.pref_key_thermal_test),false);
	}

	private void handleOnDisconnected()
	{
		setConnected(false);

		// Update connection status
		updateConnectionState(R.string.disconnected);

        updateConnectionStatusIcon(false,false);

		// Update action bar menu
		updateActionBarMenu();

		if(mRegisterSuccess)  return;
		updateConnectionStatus(R.string.disconnected);
	}

	private void displayDeviceMessage(final String message)
	{
		final LinearLayout layoutMessage = (LinearLayout)findViewById(R.id.layout_message);
		final TextView txtMessage = (TextView)findViewById(R.id.txt_message);

		// Update DeviceStatusFragment
		updateProcess(message);

		if(mProgressDialog == null)
			mProgressDialog = new ProgressDialog(context);

		if(mProgressDialog!= null)
			return;

		if(message== null || message.length() == 0)
		{
			layoutMessage.setVisibility(View.GONE);
			mProgressDialog.dismiss();
		}
		else
		{
			txtMessage.setText(message);
			//layoutMessage.setVisibility(View.VISIBLE);
			mProgressDialog.setMessage(message);
			mProgressDialog.show();

			if(message.contains("Login success") || message.contains("cancelled"))
			{
				mHandler.postDelayed(new Runnable()
				{
					@Override
					public void run()
					{
				   		//layoutMessage.setVisibility(View.GONE);
						mProgressDialog.dismiss();
					}
				},1000);
			}
		}
	}

    private static IntentFilter makeGattUpdateIntentFilter()
	{
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_SERVICE_NOTIFY_UI);
        return intentFilter;
    }

    private static IntentFilter makeIntentFilter()
	{
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_SERVICE_NOTIFY_UI);
		intentFilter.addAction(ACTION_DEBUG_SERVICE_TO_UI);
        return intentFilter;
    }

	public void resetThermalTest(final boolean reset)
	{
		Log.i(TAG, "notifyResetThermal ");
        final Intent intent = new Intent(ACTION_DEBUG_UI_SERVICE);
		intent.putExtra("mode",MODE_THERMAL_RESET);
		intent.putExtra("thermal_reset",reset);
        sendBroadcast(intent);
	}

	protected boolean isServiceRunning(Class serviceClass)
    {
        boolean running = false;
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE))
        {
            final String pckName = service.service.getPackageName();
            if (serviceClass.getName().equalsIgnoreCase(service.service.getClassName())
                && context.getPackageName().equalsIgnoreCase(service.service.getPackageName()))
            {
                running = true;
                break;
            }
        }
        return running;
    }



	//==============================================================================
	//  DeviceConnectFragment function
	//==============================================================================
	private void notifyDeviceConnected(final boolean connected)
	{
		if(getDeviceConnectFragment() != null)
			getDeviceConnectFragment().setConnected(connected);
	}
	private void showTxPower(final boolean visible)
	{
		if(getDeviceConnectFragment() != null)
			getDeviceConnectFragment().showTxPower(visible);
	}
	private void showKeyInfo(final boolean visible)
	{
		if(getDeviceConnectFragment() != null)
			getDeviceConnectFragment().showKeyInfo(visible);
	}
	private void showThermalInfo(final boolean visible)
	{
		if(getDeviceConnectFragment() != null)
			getDeviceConnectFragment().showThermalInfo(visible);
	}

	private void updateTxPowerValue(final int value)
	{
		if(getDeviceConnectFragment() != null)
			getDeviceConnectFragment().updateTxPowerValue(value);
	}

    private void updateConnectionState(final int resourceId)
	{
		if(getDeviceConnectFragment() != null)
			getDeviceConnectFragment().updateConnectionState(resourceId);
    }

	private void updateActionBarMenu()
	{
		if(getDeviceConnectFragment() != null)
			getDeviceConnectFragment().updateActionBarMenu();
		invalidateOptionsMenu();
	}

	private void displayBleRssi(final int rssi, final int avgRssi)
	{
		if(getDeviceConnectFragment() != null)
			getDeviceConnectFragment().displayBleRssi(rssi,avgRssi);
	}

	private void updateThermalInfo(final int mode, final String[] params)
	{
		if(getDeviceConnectFragment() != null)
			getDeviceConnectFragment().updateThermalInfo(mode,params);
	}

	private void appendLog(final String log)
	{
		if(getDeviceConnectFragment() != null)
			getDeviceConnectFragment().appendLog(mAutoScrollDown,log);
	}


	//==============================================================================
	//  DeviceStatusFragment function
	//==============================================================================
	private void updateProcess(final String message)
	{
		if(getDeviceStatusFragment() != null)
		{
			getDeviceStatusFragment().updateProcess(message);
		}
	}
 	private void updateConnectionStatus(final int resourceId)
	{
		if(getDeviceStatusFragment() != null)
			getDeviceStatusFragment().updateConnectionStatus(resourceId);
    }

	private void updateConnectionStatusIcon(final boolean activated,final boolean selected)
	{
		if(getDeviceStatusFragment() != null)
		{
			getDeviceStatusFragment().updateConnectionStatusIcon(activated,selected);
		}
	}


	private boolean displayDeviceStatus(final int mode,final long csta)
	{
		if(getDeviceStatusFragment() != null)
		{
			getDeviceStatusFragment().displayDeviceStatus(mode,csta);
			return true;
		}
		return false;
	}
}
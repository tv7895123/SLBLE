package com.startline.slble.Activity;

import android.app.Activity;
import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.*;
import com.startline.slble.Adapter.BleSerializableDeviceAdapter;
import com.startline.slble.PureClass.Constants;
import com.startline.slble.R;
import com.startline.slble.Service.BluetoothLeIndependentService;

import com.startline.slble.PureClass.BleSerializableDevice;
import com.startline.slble.Util.LogUtil;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;
import static com.startline.slble.Service.BluetoothLeIndependentService.*;


public class DeviceListActivity extends Activity
{
    //*****************************************************************//
    //  Constant Variables                                             //
    //*****************************************************************//
	private final static String TAG = "BLE";
	private static final int REQUEST_ENABLE_BT = 1;
	public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
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

    //*****************************************************************//
    //  View                                                           //
    //*****************************************************************//
	private TextView txtAdvData = null;
	private Button btnScan = null;
	private Button btnStop = null;
	private ListView listViewScan = null;
	private ListView listViewBond = null;
	private LinearLayout layoutProgress = null;
	//private ProgressDialog progressDialog = null;


	private Handler mHandler = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			if(msg.arg1 == BluetoothLeIndependentService.PARAM_SCAN_RESULT)
			{
				final ArrayList<BleSerializableDevice> deviceList = (ArrayList<BleSerializableDevice>)msg.obj;
				mLeDeviceListAdapter.setDevice(deviceList);
				mLeDeviceListAdapter.notifyDataSetChanged();
			}
		}
	};

    private final BroadcastReceiver mDebugDataReceiver = new BroadcastReceiver()
	{
        @Override
        public void onReceive(Context context, Intent intent)
		{
            final String action = intent.getAction();
            if (BluetoothLeIndependentService.ACTION_DEBUG_SERVICE_TO_UI.equals(action))
			{
				final int param = intent.getIntExtra("param",-1);
				if(param == PARAM_ADV_DATA)
				{
					final byte[] advData = intent.getByteArrayExtra("adv_data");
					if(advData == null || advData.length == 0) return;


					String txt = String.format("%02X  ",advData[0]);;
					for(int i=1;i<advData.length;i++)
					{
						if(i%8 == 0)
							txt += "\n";
						txt = txt + String.format("%02X  ",advData[i]);
					}

					if(txtAdvData != null)
					{
						txtAdvData.setText(txt);
					}
				}
            }
		}
	};


	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.device_list);


        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
		{
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
        }

		context = this;

		setupViews();
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeIndependentService.ACTION_DEBUG_SERVICE_TO_UI);
		registerReceiver(mDebugDataReceiver, intentFilter);


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
					service.setIpcCallbackhandler(mHandler);
				}
			}
		},delayTime);


		// Update bonded device list
		if(mBondDeviceListAdapter == null)
		{
			mBondDeviceListAdapter = new BleSerializableDeviceAdapter(context,BleSerializableDeviceAdapter.DEVICE_TYPE_BOND);
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
							if(cachedBluetoothDevice != null)
							{
								if(cachedBluetoothDevice.bluetoothGatt != null)
								{
									cachedBluetoothDevice.bluetoothGatt.disconnect();
									cachedBluetoothDevice.bluetoothGatt.close();
								}
							}

							// Disconnect it from HID service
							final BluetoothDevice bluetoothDevice = service.getConnectedBluetoothDevice(address);
							if(bluetoothDevice != null)
							{
								service.closeBTConnection(bluetoothDevice);
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



//		if(deviceAddress .length() > 0 )
//		{
//			mLeDeviceListAdapter.clear();
//			layoutProgress.setVisibility(View.VISIBLE);
//		}
	}

    @Override
    protected void onPause()
	{
        super.onPause();

		btnStop.performClick();

		unregisterReceiver(mDebugDataReceiver);

		final BluetoothLeIndependentService service = BluetoothLeIndependentService.getInstance();
		if(service != null)
		{
			service.setIpcCallbackhandler(null);
		}

        //mLeDeviceListAdapter.clear();
    }

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		if(isServiceRunning(BluetoothLeIndependentService.class) == true)
		{
			stopBluetoothLeService();
		}
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
			layoutProgress = (LinearLayout)findViewById(R.id.layout_progress);

			listViewScan = (ListView)findViewById(R.id.listview_scan);
			listViewScan.setOnItemClickListener(new AdapterView.OnItemClickListener()
			{
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id)
				{
					if (mIsScanning)
					{
						btnStop.performClick();
					}

					final BleSerializableDevice device = mLeDeviceListAdapter.getDevice(position);
					if (device == null)
					{
						return;
					}

					startDeviceConnectActivity(device.name,device.address);
				}
			});

			listViewBond = (ListView)findViewById(R.id.listview_bonded);
			listViewBond.setOnItemClickListener(new AdapterView.OnItemClickListener()
			{
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id)
				{
					if (mIsScanning)
					{
						btnStop.performClick();
					}

					final BleSerializableDevice device = mBondDeviceListAdapter.getDevice(position);
					if (device == null)
					{
						return;
					}

					startDeviceConnectActivity(device.name,device.address);
				}
			});

			btnScan = (Button)findViewById(R.id.btn_scan);
			btnScan.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					mLeDeviceListAdapter.clear();
                    sendUiActionBroadcast(notifyStartScanDevice());
					layoutProgress.setVisibility(View.VISIBLE);
					//progressDialog.show();
				}
			});

			btnStop = (Button)findViewById(R.id.btn_stop);
			btnStop.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
                    sendUiActionBroadcast(notifyStopScanDevice());
					layoutProgress.setVisibility(View.INVISIBLE);
					//progressDialog.dismiss();
				}
			});

			txtAdvData = (TextView)findViewById(R.id.txt_adv_data);
		}
		catch (Exception e)
		{
			LogUtil.d(getPackageName(), e.toString(), Thread.currentThread().getStackTrace());
		}
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

	private void updateButtonState()
	{
		if (!mIsScanning)
		{
//			btnStop.setTextColor(getResources().getColor(android.R.color.darker_gray));
//			btnScan.setTextColor(getResources().getColor(android.R.color.black));

			btnStop.setEnabled(false);
			btnScan.setEnabled(true);
        }
		else
		{
//			btnStop.setTextColor(getResources().getColor(android.R.color.black));
//			btnScan.setTextColor(getResources().getColor(android.R.color.darker_gray));

			btnStop.setEnabled(true);
			btnScan.setEnabled(false);
        }
	}

	private void startDeviceConnectActivity(final String name,final String address)
	{
		final Intent intent = new Intent(context, TabActivity.class);
		intent.putExtra(EXTRAS_DEVICE_NAME, name);
		intent.putExtra(EXTRAS_DEVICE_ADDRESS, address);
		startActivity(intent);
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
        updateButtonState();

        final Intent intent = new Intent();
        intent.putExtra("mode",BluetoothLeIndependentService.ACTION_START_SCAN);
        return intent;
	}

	private Intent notifyStopScanDevice()
	{
		mIsScanning = false;
		updateButtonState();

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

	protected boolean isServiceRunning(final Class serviceClass)
    {
        boolean running = false;
        final ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE))
        {
            if (serviceClass.getName().equalsIgnoreCase(service.service.getClassName()))
            {
                running = true;
				break;
            }
        }
        return running;
    }
}

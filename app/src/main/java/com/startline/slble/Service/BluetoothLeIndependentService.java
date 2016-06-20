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

package com.startline.slble.Service;

import android.app.*;
import android.bluetooth.*;
import android.content.*;
import android.media.MediaPlayer;
import android.os.*;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;
import com.startline.slble.Adapter.BleDeviceRssiAdapter;
import com.startline.slble.PureClass.*;
import com.startline.slble.R;
import com.startline.slble.Util.*;
import com.startline.slble.module.StaCstaDecode;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

import static com.startline.slble.PureClass.SlbleProtocol.*;
import static com.startline.slble.PureClass.BleGattAttributes.*;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
//@formatter:off
public class BluetoothLeIndependentService extends Service
{
	//=================================================================//
	//																   //
	//  Debug				                                           //
	//																   //
	//=================================================================//
	// Debug mode
	public static final int DEBUG_SHOW_ADV_DATA = 0x1;
	public static final int DEBUG_MODE = DEBUG_SHOW_ADV_DATA;

	// Record ADV Data
	private final String SHOW_ADV_DATA_ADDRESS = "48:36:5F:00:00:FF";

	//=================================================================//
	//																   //
	//  Constant Variables                                             //
	//																   //
	//=================================================================//
	private static final String TAG = "BLE";
	private final String NEW_LINE_CHARACTER = "<BR/>";

	public static final int CSTA_STA = 0;
	public static final int[] TX_POWER_LEVEL = new int[]
	{
		-17,-15,-10,-5,0,2,4,7
	};

	//--------------------------------------------------------------------
	// Ack Flag
	public static final int ACK_FLAG_TX_POWER = 0x1;
	public static final int ACK_FLAG_TX_POWER_KEYLESS = 0x2;
	public static final int ACK_FLAG_READ_PROGRAM_TABLE = 0x4;
	public static final int ACK_FLAG_WRITE_PROGRAM_TABLE = 0x8;

	//--------------------------------------------------------------------
    // Connection State
	public static final int CONNECTION_STATE_BONDING = 0;
	public static final int CONNECTION_STATE_CONNECTING = 1;
	public static final int CONNECTION_STATE_AUTO_CONNECTING = 2;
	public static final int CONNECTION_STATE_RE_CONNECTING = 3;
	public static final int CONNECTION_STATE_CANCEL = 4;
	public static final int CONNECTION_STATE_BOND_FAILED = 5;

    //--------------------------------------------------------------------
	// Connection Parameter
	public static final int CONNECTION_MAX_RETRY = -1;
	public static final int CONNECTION_MAX_CONNECT = -1;
	public static final int SCAN_INTERVAL_MS = 500;
	public static final int READ_RSSI_INTERVAL = 3000;
	public static final long SCAN_PERIOD = 10 * 1000; // Stops scanning after 10 seconds.


	//--------------------------------------------------------------------
	// Task Array
	public final int TASK_CHECK_INTERVAL = 100;
	public static final int TIMER_TASK_PROGRAM_TABLE = 0;
	public static final int TIMER_TASK_COUNT = 1;


    //--------------------------------------------------------------------
	// Process
	public final long TIMEOUT_BOND_TO_DISCONNECT = 8000;
	public static final long TIMEOUT_TASK_RETRY = 3 * 1000;
	public static final String KEYWORD_SLBLE = "SLBLE";
	private static final String FILTER_ADDRESS = "48:36:5F";
	private final String UUID_SERVICE_ROOT = "0000FFF0-0000-1000-8000-00805F9B34FB";
	private final String UUID_BLE_NOTIFY_CHANNEL = "0000FFF1-0000-1000-8000-00805F9B34FB";
	private final String UUID_BLE_WRITE_CHANNEL = "0000FFF2-0000-1000-8000-00805F9B34FB";
	public static final String EXTRA_DATA = "EXTRA_DATA";
	public static final String EXTRA_BINARY_DATA = "EXTRA_BINARY_DATA";
	public static final int INIT_STATE_NONE = 0;
	public static final int INIT_STATE_BONDING = 1;
	public static final int INIT_STATE_BOND_TO_CONNECT = 2;
	public static final int INIT_STATE_BOND_OK = 3;
	public static final int INIT_STATE_DEVICE_NAME = 4;
	public static final int INIT_STATE_INFORMATION = 5;
	public static final int INIT_STATE_CSTA = 6;
	public static final int INIT_STATE_END = 7;

	//-------------------------------------------------------------------
	// Keyless
	public final int KEYLESS_CONNECT_INTERVAL = 10*1000;												// ms
	public final int KEYLESS_CONNECT_TIME_HOUR = 3600*1000/KEYLESS_CONNECT_INTERVAL;		// Keep trying in one hour
	public final int KEYLESS_CONNECT_TIME_DAY = KEYLESS_CONNECT_TIME_HOUR*24;				// Keep trying in one day
	public final int KEYLESS_CONNECT_TIME_MAX = -1;

    //--------------------------------------------------------------------
    // Action from UI thread
    public static final String ACTION_UI_NOTIFY_SERVICE = "ACTION_UI_NOTIFY_SERVICE";
	public static final int ACTION_START_SCAN = 1;
	public static final int MODE_STOP_SCAN = 2;
	public static final int MODE_CONNECT = 3;
	public static final int MODE_DISCONNECT = 4;
	public static final int MODE_CONTROL_COMMAND = 5;

    //--------------------------------------------------------------------
	// Notify UI thread
    public static final String ACTION_SERVICE_NOTIFY_UI = "ACTION_SERVICE_NOTIFY_UI";
	public static final int PARAM_ERROR = 0x11;

    // For list activity
    public static final int PARAM_SCAN_RESULT = 0x0;

	// For connected activity
    public static final int PARAM_CONNECT_STATUS = 0x100;
    public static final int PARAM_PROCESS_STEP = 0x101;
    public static final int PARAM_RSSI = 0x102;
    public static final int PARAM_CSTA = 0x103;

    // GATT
	public static final int PARAM_GATT_CONNECTING = 0x200;
    public static final int PARAM_GATT_CONNECTED = 0x201;
    public static final int PARAM_GATT_DISCONNECTED = 0x202;
    public static final int PARAM_GATT_SERVICES_DISCOVERED = 0x203;
    public static final int PARAM_GATT_READ_DEVICE_NAME = 0x204;
    public static final int PARAM_GATT_ON_DESCRIPTOR_WRITE = 0x205;

	// Setting
	public static final int PARAM_SETTING_INFORMATION = 0x1000;
	public static final int PARAM_TX_POWER_LEVEL = 0x1001;


    // Debug
	public static final String ACTION_DEBUG_SERVICE_TO_UI = "ACTION_DEBUG_SERVICE_TO_UI";
    public static final int PARAM_ADV_DATA = 0;
    public static final int PARAM_LOG = 2;

	//--------------------------------------------------------------------
	// IPC command
	public static final int IPC_COMMAND = 0x1000;
	public static final int IPC_COMMAND_SCAN_RESULT = 0x1001;

	// Program table
	public static final int PROGRAM_TABLE_READ = 0;
	public static final int PROGRAM_TABLE_WRITE = 1;

	public static final int PROGRAM_TABLE_SUCCESS = 0;
	public static final int PROGRAM_TABLE_FAIL = 1;

	//=================================================================//
	//																   //
	//  Global Variables                                               //
	//																   //
	//=================================================================//
	// Basic
    private final int RECEIVE_DATA_BUFFER_LENGTH = 100;
	private int  mAckFlag = 0;

	// Connect
	private boolean mAllowReBind = false;
	private boolean mManualConnect = false;

	// App Setting
	private boolean mAutoConnectOnDisconnect = false;
	private boolean mAutoScroll = false;

	// Ble Setting
	private int mobileNumber = 0;
	private int mTxPowerLevel = 0;
	private int txPowerKeylessLock = 0;
	private int txPowerKeylessUnlock = 0;
	private int slaveTagCounter = 0;

	// Process
	private int mDeviceInitState = INIT_STATE_NONE;
	private boolean mScanning;
	private int mConnectionState = BluetoothProfile.STATE_DISCONNECTED;
	private byte mPacketId = 0x0;
	private byte[] mReceivedData = null;
	private byte[] mReceivedDataBuffer = null;
	private int mReceiveDataCounter = 0;
	private int mTaskRetry = 0;
	private int mReadRssiInterval = READ_RSSI_INTERVAL;

	// Keyless
	private int mConnectTimeLimitScreenOn = KEYLESS_CONNECT_TIME_MAX;		// Limit times for screen on, default MAX
	private int mConnectTimeLimitScreenOff = KEYLESS_CONNECT_TIME_HOUR;		// Limit times for screen off, default ONE HOUR
	private int mConnectTimeLimit = mConnectTimeLimitScreenOn;						// Limit times, default set as ScreenOn

	// ProgramTable
	private int mProgramTableDataIndex = 0;
	private byte mReadCheckSum = 0;
	private byte[] mWriteTableDataBuffer = null;
	private byte[] mReadTableDataBuffer = null;
	//=================================================================//
	//																   //
	//  Object                                                         //
	//																   //
	//=================================================================//
	private static BluetoothLeIndependentService serviceInstance = null;
	private BluetoothManager mBluetoothManager;
	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothDevice mBluetoothDevice;
	private BluetoothGatt mBluetoothGatt;
	private BleDeviceRssiAdapter mBleDeviceRssiAdapter;
	private BluetoothGattService mRootService;
	private BluetoothGattCharacteristic mNotifyCharacteristic;
	private BluetoothGattCharacteristic mReadCharacteristic;
	private BluetoothGattCharacteristic mWriteCharacteristic;

	private Context context;
	private Handler mIpcCallbackHandler = null;

	private BluetoothProfile mInputDeviceProfile = null;
	private static ArrayList<BluetoothDevice> mConnectedDeviceList = null;
	private static ArrayList<CachedBluetoothDevice> mCachedDeviceList = null;
	private boolean mBTConnectionRequest = false;
	private Runnable mRunnableBondToConnect = null;
	private static Thread mThreadConnect = null;
	private static Runnable mRunnableConnectDevice = null;

	private MyTimerTask[] mTimerTaskPool = null;
	private Thread mThreadTimer = null;
	//=================================================================//
	//																   //
	//  Custom  Class		                                           //
	//																   //
	//=================================================================//

	// Bind Local Service as returned object
	public class LocalBinder extends Binder
	{
		public BluetoothLeIndependentService getService()
		{
			return BluetoothLeIndependentService.this;
		}
	}

	public class CachedBluetoothDevice
	{
		public BluetoothDevice bluetoothDevice;
		public BluetoothGatt bluetoothGatt;

		public CachedBluetoothDevice(final BluetoothDevice device,final BluetoothGatt gatt)
		{
			bluetoothDevice = device;
			bluetoothGatt = gatt;
		}
	}

	public class MyTimerTask
	{
		public long expiredTime;
		public Runnable task;
	}
	//=================================================================//
	//																   //
	//  Global defined Object                                          //
	//																   //
	//=================================================================//
	private final IBinder mBinder = new LocalBinder();
	private final Handler mHandler = new Handler();

	// For Keyless
	// OS may enter sleep mode after screen off in minutes
	// So we register a receiver to listen for screen ON/OFF
	// And set a limit times for connecting
	private BroadcastReceiver mScreenStateReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			// Check re-connect setting is enabled
			if(!mAutoConnectOnDisconnect)
				return;

			if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF))
			{
				//LogUtil.d(TAG, "Screen OFF",Thread.currentThread().getStackTrace());

				mConnectTimeLimit = mConnectTimeLimitScreenOff;

				// Terminate original thread
				if(mThreadConnect != null)
				{
					mThreadConnect.interrupt();
				}

				// Delay to start new thread to avoid state error
				mHandler.postDelayed(new Runnable()
				{
					@Override
					public void run()
					{
						if(mThreadConnect == null)
						{
							connectDevice();
						}
					}
				},2000);
			}
			else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON))
			{
				//LogUtil.d(TAG, "Screen ON",Thread.currentThread().getStackTrace());

				mConnectTimeLimit = mConnectTimeLimitScreenOn;

				if(mThreadConnect == null)
				{
					connectDevice();
				}
			}

			LogUtil.d(TAG,"Connection time limit = "+mConnectTimeLimit,Thread.currentThread().getStackTrace());
		}
	};

	// Bonding receiver, handle bonding message
	private BroadcastReceiver mBondingBroadcastReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(final Context context, final Intent intent)
		{
			final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
			final int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
			final int previousBondState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1);

			final String bondingLog = "Bond " + device.getAddress() + " ,\nState: " + previousBondState + " --> " + bondState;
			LogUtil.d(TAG,bondingLog ,Thread.currentThread().getStackTrace());
			appendLog(bondingLog);

			if(mBluetoothDevice == null)
			{
				return;
			}


			// Only handle message with our device
			if(device.getAddress().equals(mBluetoothDevice.getAddress()))
			{
				// After device bonded, connect to it
				if (previousBondState == BluetoothDevice.BOND_BONDING && bondState == BluetoothDevice.BOND_BONDED)
				{
					appendLog("Device bonded");
					if(mBluetoothGatt == null)
					{
						// To indicate that we are at the state between BONDED and CONNECT
						mDeviceInitState = INIT_STATE_BOND_TO_CONNECT;

						if(mRunnableBondToConnect != null)
						{
							mHandler.removeCallbacks(mRunnableBondToConnect);
							mRunnableBondToConnect = null;
						}


						// Action when not receiving ACL_DISCONNECT after bonding success
						mRunnableBondToConnect = new Runnable()
						{
							@Override
							public void run()
							{
								LogUtil.d(TAG,"mRunnableBondToConnect",Thread.currentThread().getStackTrace());

								mDeviceInitState = INIT_STATE_BOND_OK;
								createBTConnection();
							}
						};

						// Some phones may DISCONNECT from BluetoothDevice after bonding, but some may not
						// This is for phones which won't receive DISCONNECT message.
						// So I set a timer to continue connecting
						mHandler.postDelayed(mRunnableBondToConnect,TIMEOUT_BOND_TO_DISCONNECT);
					}
				}
				else if (previousBondState == BluetoothDevice.BOND_BONDING && bondState == BluetoothDevice.BOND_NONE)
				{
					appendLog("Bond failed");
					broadcastNotifyUi(getConnectStatusIntent(CONNECTION_STATE_BOND_FAILED));
				}
			}
		}
	};

	// Action when connection timeout
	private final Runnable mRunnableConnectTimeout = new Runnable()
	{
		@Override
		public void run()
		{
			disconnect(true);   //mRunnableConnectTimeout
		}
	};


	// Scan and pause periodically ,  interval SCAN_INTERVAL_MS
	private final Runnable scanDeviceRunnable = new Runnable()
	{
		@Override
		public void run()
		{
			if (mScanning)
			{
				// Stop scan, remove expired device then notify UI activity to display devices
				stopScan();
				mBleDeviceRssiAdapter.removeUnavailableDevice();
                //broadcastNotifyUi(getScanResultIntent());
				notifyScanResult();
				mHandler.postDelayed(scanDeviceRunnable, SCAN_INTERVAL_MS);
			}
			else
			{
				startScan();
				mHandler.postDelayed(scanDeviceRunnable, SCAN_INTERVAL_MS);
			}
		}
	};

	// Read rssi periodically
	private final Runnable readRssiRunnable = new Runnable()
	{
		@Override
		public void run()
		{
			// Only read rssi when it is connected
			if (getConnectionState() == BluetoothProfile.STATE_CONNECTED)
			{
				try
				{
					// Check bluetoothGatt is avilable
					if (mBluetoothGatt != null)
					{
						getBluetoothGatt().readRemoteRssi();
						mHandler.postDelayed(readRssiRunnable, mReadRssiInterval);
					}
					else
					{
						// If bluetoothGatt is released, reset resources to disconnected
						disconnect(true); // readRssiRunnable
					}
				}
				catch (Exception e)
				{
					// If get errors when read rssi, cancel this function
					cancelRssiTimer();
					LogUtil.e(TAG, e.toString(),Thread.currentThread().getStackTrace());
				}
			}
		}
	};

	// Task retry functions
	// Do action depends on mTaskRetry
	private final Runnable taskRetryRunnable = new Runnable()
	{
		@Override
		public void run()
		{
			// No limit retry times or still under limit, continue retry
			if ( CONNECTION_MAX_RETRY == -1 || mTaskRetry++ < CONNECTION_MAX_RETRY)
			{
				handleTaskTimeout();
			}
			// exceed max try times limit, cancel task and disconnect
			else
			{
				disconnect(false);  //taskRetryRunnable
				cancelTaskTimeoutTimer();  //taskRetryRunnable
				broadcastNotifyUi(getConnectStatusIntent(CONNECTION_STATE_CANCEL));
			}
		}
	};


	// Implements callback methods for GATT events that the app cares about.
	// For example,connection change and services discovered.
	private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback()
	{
		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
		{
			try
			{
				if(mBluetoothDevice == null || !gatt.getDevice().getAddress().equals(mBluetoothDevice.getAddress()))
				{
					LogUtil.d(TAG, String.format("Not match address : %s , Current = %s",gatt.getDevice().getAddress(),(mBluetoothDevice == null || mBluetoothDevice.getAddress() == null)? "null":mBluetoothDevice.getAddress()),Thread.currentThread().getStackTrace());
					return;
				}

				appendLog(String.format("Connection State = %d,  Status = %d ", newState, status));
				LogUtil.d(TAG, "[Process] onConnectionStateChange : " + newState,Thread.currentThread().getStackTrace());

				if (newState == BluetoothProfile.STATE_CONNECTED)
				{
					handleBleConnect();
				}
				else if (newState == BluetoothProfile.STATE_DISCONNECTED)
				{
					appendLog(formatConnectionString("disconnected"));
					LogUtil.d(TAG, "[Process] Disconnected from GATT server.",Thread.currentThread().getStackTrace());

					// Status != 0 , means has error, re-Initialize all resources and retry
					if(status != 0)
					{
						final boolean result = reInitialize();
						appendLog("Re-Initialization : "+ (result? "Success":"Fail"));
					}

					handleBleDisconnect(); // onConnectionStateChange,newState == BluetoothProfile.STATE_DISCONNECTED
				}
			}
			catch (Exception e)
			{
				LogUtil.e(TAG, e.toString(), Thread.currentThread().getStackTrace());
			}
		}

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status)
		{
			if(mBluetoothDevice == null || !gatt.getDevice().getAddress().equals(mBluetoothDevice.getAddress()))
			{
				LogUtil.d(TAG, String.format("Not match address : %s , Current = %s",gatt.getDevice().getAddress(),(mBluetoothDevice == null || mBluetoothDevice.getAddress() == null)? "null":mBluetoothDevice.getAddress()),Thread.currentThread().getStackTrace());
				return;
			}

			LogUtil.d(TAG, "[Process] onServicesDiscovered received: " + status,Thread.currentThread().getStackTrace());
			appendLog("[Process] onServicesDiscovered received: " + status);
			try
			{
				if (status == BluetoothGatt.GATT_SUCCESS)
				{
					broadcastNotifyUi(getGattIntent(PARAM_GATT_SERVICES_DISCOVERED));

					if (getConnectionState() == BluetoothProfile.STATE_CONNECTED)
					{
						if (isNotificationEnabled(getNotifyGattCharacteristic(getRootService())) == false)
						{
							// Enable notify
							startListening();
						}
						// Notify already enabled, read device name
						else
						{
							readDeviceNameFromService();
							appendLog(formatConnectionString("Notification already enabled"));
						}
					}
					else
					{
						appendLog("Discovery service but device disconnected");
						LogUtil.d(TAG, "Discovery service but device disconnected", Thread.currentThread().getStackTrace());
						disconnect(true); //Discovery service but device disconnected
					}
				}
				else
				{
					appendLog("Discovery service failed.");
					LogUtil.d(TAG, "Discovery service failed.", Thread.currentThread().getStackTrace());
					disconnect(true); //Discovery service failed
				}
			}
			catch (Exception e)
			{
				LogUtil.e(TAG, e.toString(), Thread.currentThread().getStackTrace());
			}
		}

		@Override
		public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
		{
			if(mBluetoothDevice == null || !gatt.getDevice().getAddress().equals(mBluetoothDevice.getAddress()))
			{
				LogUtil.d(TAG, String.format("Not match address : %s , Current = %s",gatt.getDevice().getAddress(),(mBluetoothDevice == null || mBluetoothDevice.getAddress() == null)? "null":mBluetoothDevice.getAddress()),Thread.currentThread().getStackTrace());
				return;
			}
			LogUtil.d(TAG, "[Process] onCharacteristicRead received: " + status,Thread.currentThread().getStackTrace());
			try
			{
				if (status == BluetoothGatt.GATT_SUCCESS)
				{
					// Get device name
					if (characteristic.getUuid().toString().equalsIgnoreCase(CHAR_DEVICE_NAME))
					{
						// After get device name, we consider the connection is completed
						removeConnectionTimeout(); // onCharacteristicRead , get device name

						// Get and set device name
						String deviceName = new String(characteristic.getValue(), "UTF8");
						if(deviceName.length() > 15)
						{
							deviceName = deviceName.substring(0,15);
						}

						broadcastNotifyUi(getGattIntent(PARAM_GATT_READ_DEVICE_NAME));

						if(!deviceName.equals(KEYWORD_SLBLE))
						{
							appendLog(formatConnectionString("Unsupported device !!!"));
							disconnect(false);  //Unsupported device
							return;
						}

						appendLog(formatConnectionString("Get device name"));

						mDeviceInitState = INIT_STATE_DEVICE_NAME;

                        initDeviceState(INIT_STATE_INFORMATION);// Get device name
					}
					else if(characteristic.getUuid().toString().equalsIgnoreCase(CHAR_TX_POWER_LEVEL))
					{
						mTxPowerLevel = characteristic.getValue()[0];
						appendLog(formatReceiveString(" " + mTxPowerLevel + " dB"));
						LogUtil.d(TAG, "[Process] ]Read TxPower value : " + mTxPowerLevel,Thread.currentThread().getStackTrace());

						final Message message = new Message();
						message.arg1 = PARAM_TX_POWER_LEVEL;
						message.obj = mTxPowerLevel;
						sendCallbackMessage(message);
					}
				}
			}
			catch (Exception e)
			{
				LogUtil.e(TAG, e.toString(), Thread.currentThread().getStackTrace());
			}
		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
		{
			if(mBluetoothDevice == null || !gatt.getDevice().getAddress().equals(mBluetoothDevice.getAddress()))
			{
				LogUtil.d(TAG, String.format("Not match address : %s , Current = %s",gatt.getDevice().getAddress(),(mBluetoothDevice == null || mBluetoothDevice.getAddress() == null)? "null":mBluetoothDevice.getAddress()),Thread.currentThread().getStackTrace());
				return;
			}

			LogUtil.d(TAG, "[Process] onCharacteristicChanged",Thread.currentThread().getStackTrace());
			try
			{
				mReceivedData = characteristic.getValue();
				handleBleResponse(mReceivedData);
			}
			catch (Exception e)
			{
				LogUtil.e(TAG, e.toString(), Thread.currentThread().getStackTrace());
			}
		}

		@Override
		public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status)
		{
			if(mBluetoothDevice == null || !gatt.getDevice().getAddress().equals(mBluetoothDevice.getAddress()))
			{
				LogUtil.d(TAG, String.format("Not match address : %s , Current = %s",gatt.getDevice().getAddress(),(mBluetoothDevice == null || mBluetoothDevice.getAddress() == null)? "null":mBluetoothDevice.getAddress()),Thread.currentThread().getStackTrace());
				return;
			}

			LogUtil.d(TAG, "[Process] onDescriptorWrite : " + status,Thread.currentThread().getStackTrace());
			appendLog("[Process] onDescriptorWrite : " + status);
			try
			{
				final String uuid = descriptor.getUuid().toString();
				if (uuid.equalsIgnoreCase(BleGattAttributes.DESCRIPTORS_CLIENT_CHARACTERISTIC_CONFIGURATION))
				{
					if (status == BluetoothGatt.GATT_SUCCESS)
					{
						readDeviceNameFromService();
						appendLog(formatConnectionString("Notification enabled"));
					}
					else if (status == BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION)
					{
						// this is where the tricky part comes

						if (gatt.getDevice().getBondState() == BluetoothDevice.BOND_NONE)
						{
							appendLog("GATT_INSUFFICIENT_AUTHENTICATION");
							_bondingDevice();
						}
						else
						{
							appendLog("UNKNOWN ERROR");
							// this situation happens when you try to connect for the second time to already bonded device
							// it should never happen, in my opinion
							//Logger.e(TAG, "The phone is trying to read from paired device without encryption. Android Bug?");
							// I don't know what to do here
							// This error was found on Nexus 7 with KRT16S build of Andorid 4.4. It does not appear on Samsung S4 with Andorid 4.3.
						}
					}
					else
					{
						disconnect(true); //Enable notification failed
						appendLog("Enable notification failed");
						LogUtil.d(TAG, "Enable notification failed.",Thread.currentThread().getStackTrace());
					}
				}
				else
				{
					appendLog("onDescriptorWrite unknown status");
                    disconnect(false);
                }
			}
			catch (Exception e)
			{
				LogUtil.e(TAG, e.toString(), Thread.currentThread().getStackTrace());
			}
		}

		@Override
		public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status)
		{
			if(mBluetoothDevice == null || !gatt.getDevice().getAddress().equals(mBluetoothDevice.getAddress()))
			{
				LogUtil.d(TAG, String.format("Not match address : %s , Current = %s",gatt.getDevice().getAddress(),(mBluetoothDevice == null || mBluetoothDevice.getAddress() == null)? "null":mBluetoothDevice.getAddress()),Thread.currentThread().getStackTrace());
				return;
			}

			if (status == BluetoothGatt.GATT_SUCCESS)
			{
				if (rssi < 0)
				{
					broadcastNotifyUi(getRssiIntent(rssi, 0));
				}
			}
			else
			{
				cancelRssiTimer();
			}
		}
	};

	// Device scan callback.
	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback()
	{
		@Override
		public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord)
		{
			try
			{
				if((DEBUG_MODE & DEBUG_SHOW_ADV_DATA) > 0)
				{
					if(device.getAddress().equals(SHOW_ADV_DATA_ADDRESS))
					{
						final Intent intent = new Intent(ACTION_DEBUG_SERVICE_TO_UI);
						intent.putExtra("param",PARAM_ADV_DATA);
						intent.putExtra("adv_data",scanRecord);
						broadcastIntent(intent);
					}
				}


				// Decode device name
				final BleAdvertisedData advertisedData = BleUtil.parseAdertisedData(scanRecord);
				String nameUTF8 = advertisedData.getName();

				if(nameUTF8 == null)
				{
					nameUTF8 = device.getName();
					if(nameUTF8 == null)
						nameUTF8 = "Unknown Device";
				}

				// We can put only 16 bytes data in packet, so get the first 16 bytes character
				if(nameUTF8.length() >15)
					nameUTF8 = nameUTF8.substring(0,15);
				LogUtil.d(TAG, "[Process] mLeScanCallback addDevice : " + nameUTF8,Thread.currentThread().getStackTrace());



				// Add scanned device into list
				if(device.getAddress().startsWith(FILTER_ADDRESS))
					mBleDeviceRssiAdapter.addDevice(device, nameUTF8, rssi);
			}
			catch (Exception e)
			{
				LogUtil.e(TAG, e.toString(), Thread.currentThread().getStackTrace());
			}
		}
	};


	// Receive message from UI activity
	private final BroadcastReceiver mMessageFromUiReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			try
			{
				final String action = intent.getAction();
				LogUtil.d(TAG, "[Broadcast] "+action,Thread.currentThread().getStackTrace());
				if(action.equals(ACTION_UI_NOTIFY_SERVICE))
				{
                    final int mode = intent.getIntExtra("mode",-1);
                    switch (mode)
                    {
                        case ACTION_START_SCAN:
                        {
                            // Clear original device list
                            if (mBleDeviceRssiAdapter != null)
                            {
                                clearAllDevice();
                                //broadcastNotifyUi(getScanResultIntent());
								notifyScanResult();
                            }

                            // Do initialize and start scan
                            if(reInitialize())
                                settingScanTimer();
                            else
                            {
                                appendLog("Initialize failed when start scan");
                            }
                        }
                        break;

                        case MODE_STOP_SCAN:
                        {
                            cancelScanTimer();
                            stopScan();
                        }
                        break;

                        case MODE_CONNECT:
                        {
							if(!isBluetoothEnabled())
							{
								appendLog("Bluetooth is not enabled");
								LogUtil.d(TAG,"Bluetooth is not enabled",Thread.currentThread().getStackTrace());
								return;
							}

							if(mBluetoothDevice != null)
							{
								removeConnectDeviceRunnable();
								setManualConnect(true);
								connectDevice(mBluetoothDevice.getAddress());  // Manual connect
							}
                        }
                        break;

                        case MODE_DISCONNECT:
                        {
							if(!isBluetoothEnabled())
							{
								appendLog("Bluetooth is not enabled");
								LogUtil.d(TAG,"Bluetooth is not enabled",Thread.currentThread().getStackTrace());
								return;
							}

							disconnect(false);  //MODE_BLE_DISCONNECT
							if(mInputDeviceProfile != null && mInputDeviceProfile.getConnectionState(mBluetoothDevice) == BluetoothProfile.STATE_CONNECTED)
							{
								appendLog("closeBTConnection");
								closeBTConnection(mInputDeviceProfile,mBluetoothDevice);
							}
						}
                        break;

                        case MODE_CONTROL_COMMAND:
                        {
                            final int cmd = intent.getIntExtra("param", 0);
                            sendCommand(cmd);
                        }
                        break;
                    }

				}
			}
			catch (Exception e)
			{
				LogUtil.e(TAG, e.toString(), Thread.currentThread().getStackTrace());
			}
		}
	};

	// Callback when get a profile
	private final BluetoothProfile.ServiceListener mProfileListener = new BluetoothProfile.ServiceListener()
	{
		// Callback when we register listener success
		// Or when Bluetooth service become available
		@Override
		public void onServiceConnected(int profile, BluetoothProfile proxy)
		{
			appendLog("On BluetoothProfile connected : " + profile);
			LogUtil.d(context.getPackageName(),"onServiceConnected - " + profile,Thread.currentThread().getStackTrace());

			// Save the profile proxy for BTConnection functions
			if (profile == getInputDeviceHiddenConstant() && mInputDeviceProfile == null)
			{
				mInputDeviceProfile = proxy;
			}

			// If need to connect after get profile
			if(mBTConnectionRequest)
			{
				mBTConnectionRequest = false;
				mHandler.postDelayed(new Runnable()
				{
					@Override
					public void run()
					{
						createBTConnection();
					}
				},1000);
			}
		}

		@Override
		public void onServiceDisconnected(int profile)
		{
			appendLog("On BluetoothProfile disconnected : " + profile);
			LogUtil.d(context.getPackageName(),"onServiceDisconnected - " + profile,Thread.currentThread().getStackTrace());
			if (profile == getInputDeviceHiddenConstant())
			{
				mInputDeviceProfile = null;
			}
		}
	};


	//*****************************************************************//
	//  Override function                                              //
	//*****************************************************************//
	@Override
	public IBinder onBind(Intent intent)
	{
		return mBinder;
	}

	@Override
	public void onCreate()
	{
		super.onCreate();

//		startForegroundWithNotification("",-1);  // onCreate
	}

	@Override
	public boolean onUnbind(Intent intent)
	{
		// After using a given device, you should make sure that BluetoothGatt.close() is called
		// such that resources are cleaned up properly.  In this particular example, close() is
		// invoked when the UI is disconnected from the Service.
		closeGatt();

		return super.onUnbind(intent);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		// Init variables
		context = this;
		serviceInstance = this;

		mTimerTaskPool = new MyTimerTask[TIMER_TASK_COUNT];

		loadAppSetting(readAppSetting());
		loadBleSetting(readBleSetting());


		// Register receiver
		registerBleOperateReceiver();

		// Register bonding receiver
		registerReceiver(mBondingBroadcastReceiver, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));


		// Register screen on and off receiver
		final IntentFilter screenStateFilter = new IntentFilter();
		screenStateFilter.addAction(Intent.ACTION_SCREEN_ON);
		screenStateFilter.addAction(Intent.ACTION_SCREEN_OFF);
		registerReceiver(mScreenStateReceiver, screenStateFilter);

		// Auto start scan if KeyLess and AutoConnect are enabled
		if( initialize() )
		{
			if(intent == null)
				return super.onStartCommand(intent, flags, startId);

			final String action = intent.getAction();

			if(action == null)
				return super.onStartCommand(intent, flags, startId);
		}

		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy()
	{
        // Must disconnect device and recycle resources when service destroyed
		if(mConnectionState == BluetoothProfile.STATE_CONNECTING
			|| mConnectionState == BluetoothProfile.STATE_CONNECTED)
			disconnect(false);

		try
		{
			unregisterReceiver(mMessageFromUiReceiver);
			unregisterReceiver(mBondingBroadcastReceiver);

			unregisterReceiver(mScreenStateReceiver);

			closeProfileProxy();
		}
		catch (Exception e)
		{
			LogUtil.e(TAG,"receiver not registered",Thread.currentThread().getStackTrace());
		}


		serviceInstance = null;
		super.onDestroy();
	}


	//*****************************************************************//
	//  Member function                                                //
	//*****************************************************************//
	public static BluetoothLeIndependentService getInstance()
	{
		return serviceInstance;
	}

	private BluetoothGatt getBluetoothGatt()
	{
		return mBluetoothGatt;
	}

	private void setBluetoothGatt(final BluetoothGatt bluetoothGatt)
	{
		mBluetoothGatt = bluetoothGatt;
	}

	private int getAckFlag()
	{
		return mAckFlag;
	}

	private void setAckFlag(final int flag)
	{
		mAckFlag = (mAckFlag & (~(0x1<<(flag-1)))) | flag;
	}

	private void clearAckFlag(final int flag)
	{
		mAckFlag = mAckFlag & (~(0x1<<(flag-1)));
	}

	private void clearAckFlag()
	{
		mAckFlag = 0;
	}

	public void setIpcCallbackhandler(final Handler callbackHandler)
	{
		mIpcCallbackHandler = callbackHandler;
	}

	public Handler getIpcCallbackHandler()
	{
		return mIpcCallbackHandler;
	}

	public void setAutoConnectOnDisconnect(final boolean enable)
	{
		mAutoConnectOnDisconnect = enable;
	}

	public boolean getAutoConnectOnDisconnect()
	{
		return mAutoConnectOnDisconnect;
	}

	public void setAutoScroll(final boolean enable)
	{
		mAutoScroll = enable;
	}

	public boolean getAutoScroll()
	{
		return mAutoScroll;
	}

	public int getTxPowerLevel()
	{
		return mTxPowerLevel;
	}

	public int getTxPowerKeylessLock()
	{
		return txPowerKeylessLock;
	}

	public int getTxPowerKeylessUnlock()
	{
		return txPowerKeylessUnlock;
	}

	public int getSlaveTagCounter()
	{
		return slaveTagCounter;
	}

	public void setManualConnect(final boolean manual)
	{
		mManualConnect = manual;
	}

	public void setupBluetoothDeviceFromCache(final String address)
	{
		mRootService = null;
		mNotifyCharacteristic = null;
		mWriteCharacteristic = null;

		// Get device form cache
		final CachedBluetoothDevice cachedBluetoothDevice = getCachedBluetoothDevice(address);
		if(cachedBluetoothDevice != null)
		{
			// Set up bluetooth resources
			setBluetoothGatt(cachedBluetoothDevice.bluetoothGatt);
			setBluetoothDevice(cachedBluetoothDevice.bluetoothDevice);

			// Set as connected
			mConnectionState = BluetoothProfile.STATE_CONNECTED;

			// Notify UI
			broadcastNotifyUi(getGattIntent(PARAM_GATT_CONNECTED)); // notify Connect fragment
			broadcastNotifyUi(getProcessStepIntent(INIT_STATE_END));		// notify Status fragment

			// Request Csta to show right status
			sendRequestCsta();
		}
		else
		{
			mConnectionState = BluetoothProfile.STATE_DISCONNECTED;
			setBluetoothGatt(null);
			setBluetoothDevice(null);
		}
	}


	private void setProgramTableTimerTask(final int action)
	{
		final MyTimerTask myTimerTask = new MyTimerTask();
		myTimerTask.expiredTime = System.currentTimeMillis() + 600;
		myTimerTask.task = new Runnable()
		{
			@Override
			public void run()
			{
				appendLog("Program table data timeout");
				final Message message = new Message();
				message.arg1 = action;
				message.arg2 = PROGRAM_TABLE_FAIL;
				sendCallbackMessage(message);

				//Toast.makeText(context,"Program table data timeout",Toast.LENGTH_SHORT).show();
			}
		};

		setTimerTask(TIMER_TASK_PROGRAM_TABLE,myTimerTask);
	}


	private void setTimerTask(final int taskIndex,final MyTimerTask myTimerTask)
	{
		if(mTimerTaskPool[taskIndex] != null)
		{
			Toast.makeText(context,String.format("Timer task [%d] already running",taskIndex),Toast.LENGTH_SHORT).show();
			return;
		}
		else
		{
			mTimerTaskPool[taskIndex] = myTimerTask;
		}

		if(mThreadTimer == null)
		{
			mThreadTimer = new Thread(
				new Runnable()
				{
					@Override
					public void run()
					{
						boolean gotException = false;

						while(!gotException)																			// No exception
						{
							try
							{
								MyTimerTask task;
								int pendingTask = 0;
								for(int i=0;i<TIMER_TASK_COUNT;i++)
								{
									task = mTimerTaskPool[i];
									if(task != null)
									{
										pendingTask++;
										if(System.currentTimeMillis()>task.expiredTime)
										{
											mTimerTaskPool[i] = null;
											mHandler.post(task.task);
										}
									}
								}
								if(pendingTask == 0)
									break;
								Thread.sleep(TASK_CHECK_INTERVAL);
							}
							catch (InterruptedException e)
							{
								gotException = true;
								e.printStackTrace();
							}
						}
						mThreadTimer = null;
						LogUtil.d(getPackageName(),"Leaving _connect loop.",Thread.currentThread().getStackTrace());
					}
				}
			);

			mThreadTimer.start();
		}
	}

	private void updateTimerTask(final int taskIndex,final MyTimerTask myTimerTask)
	{
		mTimerTaskPool[taskIndex] = myTimerTask;
	}

	private void setupTimerTask(final Runnable task,final long delay,final long period)
	{
		if(task == null || delay < 0) return;

		mHandler.removeCallbacks(task);

		if(period > 0)
		{
			mHandler.postDelayed(task,delay);
		}
		else
		{
			mHandler.postDelayed(task,delay);
		}
	}

	private void loadAppSetting(final String appSetting)
	{
		if(appSetting.isEmpty())
		{
			setAutoConnectOnDisconnect(false);
			setAutoScroll(false);
		}
		else
		{
			try
			{
				JSONObject jsonObject = new JSONObject(appSetting);
				setAutoConnectOnDisconnect(jsonObject.get(getString(R.string.title_auto_connect)) == 1);
				setAutoScroll(jsonObject.get(getString(R.string.title_auto_scroll)) == 1);
			}
			catch (Exception e)
			{

			}
		}
	}

	public void loadBleSetting(final byte[] bleSetting)
	{
		try
		{
			mobileNumber = bleSetting[0] & 0x0F;
			txPowerKeylessLock = bleSetting[1] & 0x0F;
			txPowerKeylessUnlock = (bleSetting[1]>>4) & 0x0F;
			slaveTagCounter = bleSetting[2] & 0x0F;
		}
		catch (Exception e)
		{

		}
	}

	public void clearAllDevice()
	{
		mBleDeviceRssiAdapter.clear();
	}

	public BluetoothDevice getBluetoothDevice()
	{
		return mBluetoothDevice;
	}

	public void setBluetoothDevice(final BluetoothDevice bluetoothDevice)
	{
		mBluetoothDevice = bluetoothDevice;
	}

	private BluetoothGattCharacteristic getNotifyGattCharacteristic(final BluetoothGattService service)
	{
	 	if(mNotifyCharacteristic == null)
		{
			mNotifyCharacteristic = getCharacteristic(service,UUID_BLE_NOTIFY_CHANNEL);
		}

		return mNotifyCharacteristic;
	}

	private BluetoothGattCharacteristic getWriteGattCharacteristic(final BluetoothGattService service)
	{
	 	if(mWriteCharacteristic == null)
		{
			mWriteCharacteristic = getCharacteristic(service,UUID_BLE_WRITE_CHANNEL);
		}

		return mWriteCharacteristic;
	}

	private BluetoothGattCharacteristic getCharacteristic(final BluetoothGattService bluetoothGattService,final String uuid)
	{
		if(bluetoothGattService == null) return null;

		return bluetoothGattService.getCharacteristic(UUID.fromString(uuid));
	}

	private BluetoothGattService getService(final String uuid)
	{
		if(uuid == null || uuid.length() == 0) return null;

		final List<BluetoothGattService> serviceList = getSupportedGattServices();
		if( serviceList != null)
		{
			for(int i=0;i<serviceList.size();i++)
			{
				final BluetoothGattService service = serviceList.get(i);
				final String serviceUuid = service.getUuid().toString();
				if(serviceUuid.equalsIgnoreCase(uuid))
				{
					return service;
				}
			}
		}
		return null;
	}

	private BluetoothGattService getRootService()
	{
		if(mRootService == null)
		{
			mRootService = getService(UUID_SERVICE_ROOT);
		}

		return mRootService;
	}

    public boolean isDeviceInitialized()
    {
        if(mDeviceInitState == INIT_STATE_END)
            return true;
        else
            return false;
    }
	private void initDeviceState(final int nextState)
	{
		try
		{
			int error = 0;
			switch (mDeviceInitState)
			{
				case INIT_STATE_DEVICE_NAME:
				{
					if(nextState == INIT_STATE_INFORMATION)
					{
						sendRequestBleSetting();
						mDeviceInitState = INIT_STATE_INFORMATION;
					}
					else
					{
						error = 1;
					}
				}
				break;

				case INIT_STATE_INFORMATION:
				{
					if(nextState == INIT_STATE_CSTA)
					{
						sendRequestCsta();
						mDeviceInitState = INIT_STATE_CSTA;
					}
					else
					{
						error = 1;
					}
				}
				break;
				case INIT_STATE_CSTA:
				{
					if(nextState == INIT_STATE_END)
					{
						playLoginSuccessSound();
						broadcastNotifyUi(getProcessStepIntent(INIT_STATE_END));
						mDeviceInitState = INIT_STATE_END;
						removeConnectionTimeout();
						mManualConnect = false;

						// After connecting and verified, add device to cache list
						addBluetoothDeviceToCache(mBluetoothDevice,getBluetoothGatt());
					}
					else
					{
						error = 1;
					}
				}
				break;
			}

			if(error > 0)
			{
				appendLog(String.format("InitState error, current state=%d, receive state=%d",mDeviceInitState,nextState));
			}
		}
		catch (Exception e)
		{
			LogUtil.e(TAG, e.toString(),Thread.currentThread().getStackTrace());
		}
	}

	private int[] sortArray(final int[] array,final int length, final int latestIndex)
	{
		final int offsetIndex = latestIndex-1;
		final int[] record = new int[length];


		for(int i=0;i<length;i++)
		{
			record[i] = array[((offsetIndex-i)+length)%length];
		}

		return record;
	}


	protected  double getDistance(int rssi, int txPower)
	{
		/*	protected double calculateDistance(int txPower, double rssi)
	{
		if (rssi == 0)
		{
			return -1.0; // if we cannot determine distance, return -1.
		}

		double ratio = rssi*1.0/txPower;
		if (ratio < 1.0)
		{
			return Math.pow(ratio,10);
		}
		else
		{
			double accuracy =  (0.89976)*Math.pow(ratio,7.7095) + 0.111;
			return accuracy;
		}
	}
		 * RSSI = TxPower - 10 * n * lg(d)
		 * n = 2 (in free space)
		 *
		 * d = 10 ^ ((TxPower - RSSI) / (10 * n))
		 */

		return Math.pow(10d, ((double) txPower - rssi) / (10 * 2));
	}


    //*****************************************************************//
    //  Scan BLE function                                              //
    //*****************************************************************//
	public void startScan()
	{
		scanLeDevice(true);
	}

	public void stopScan()
	{
		scanLeDevice(false);
	}

	private void settingScanTimer()
	{
		cancelScanTimer();
		mHandler.post(scanDeviceRunnable);
	}

	private void cancelScanTimer()
	{
		mHandler.removeCallbacks(scanDeviceRunnable);
	}

//	public boolean reScan()
//	{
//		//if(++mScanTime < MAX_SCAN_TIMES)
//		{
//		scanLeDevice(true);
//		return true;
//		}
//
//		//return false;
//	}

    private void scanLeDevice(final boolean enable)
	{
		try
		{
			LogUtil.d(TAG, "[Process] scanLeDevice  " + mScanning + " --> " + enable,Thread.currentThread().getStackTrace());
			if (enable)
			{
				if(mScanning) return;

				mScanning = true;

				if(mBleDeviceRssiAdapter == null)
					mBleDeviceRssiAdapter = new BleDeviceRssiAdapter(BluetoothLeIndependentService.this);


				mBluetoothAdapter.startLeScan(mLeScanCallback);
			}
			else
			{
				if(mScanning == false) return;

				mScanning = false;

				mBluetoothAdapter.stopLeScan(mLeScanCallback);
			}
		}
		catch (Exception e)
		{
			LogUtil.e(TAG, e.toString(),Thread.currentThread().getStackTrace());
		}
    }
    //*****************************************************************//
    //  Connection  function                                           //
    //*****************************************************************//
	protected boolean isBluetoothEnabled()
	{
		final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		final BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
		if ( bluetoothAdapter != null && bluetoothAdapter.isEnabled())
		{
			return true;
		}
		return false;
	}

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize()
	{
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null)
		{
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null)
			{
                LogUtil.e(TAG, "[Error] Unable to initialize BluetoothManager.",Thread.currentThread().getStackTrace());
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
			LogUtil.e(TAG, "[Error] Unable to obtain a BluetoothAdapter.",Thread.currentThread().getStackTrace());
            return false;
        }

        return true;
    }

	// It address is changed, need to Re-Initialize or it will can not connect to device
    public boolean reInitialize()
	{
		mBluetoothManager = null;
		mBluetoothAdapter = null;

		return initialize();
    }

	public int getConnectionState()
	{
		return mConnectionState;
	}


	// Connect to current device
	public void connectDevice()
	{
		if(mBluetoothDevice != null)
		{
			connectDevice(mBluetoothDevice.getAddress());
		}
	}

	// init connection with address
	public void connectDevice(final String address)
	{
		LogUtil.d(TAG,"[Process] connectDevice address : "+address,Thread.currentThread().getStackTrace());

		// Check bluetooth adapter
		if(mBluetoothAdapter == null)
		{
			LogUtil.e(TAG, "[Error] mBluetoothAdapter is null.  Unable to connect.",Thread.currentThread().getStackTrace());
			return;
		}

		// init mBluetoothDevice , get device from adapter
		setBluetoothDevice(mBluetoothAdapter.getRemoteDevice(address));
		if (mBluetoothDevice == null)
		{
			LogUtil.e(TAG, "[Error] mBluetoothDevice not found.  Unable to connect.",Thread.currentThread().getStackTrace());
			return;
		}

		// If device not bonded, start bonding
		if(mBluetoothDevice.getBondState() != BluetoothDevice.BOND_BONDED)
		{
			appendLog("Not bonded");
			LogUtil.d(TAG,"Not bonded",Thread.currentThread().getStackTrace());
			_bondingDevice();
		}
		// Device bonded, try to connect to it
		else
		{
			// Connected with OS
			if(isBluetoothDeviceConnected(address))
			{
				appendLog("Connected with OS");
				LogUtil.d(TAG,"Connected with OS",Thread.currentThread().getStackTrace());
				// Cached with App
				if(isBluetoothDeviceCached(address))
				{
					LogUtil.d(getPackageName(),"Device cached",Thread.currentThread().getStackTrace());
					appendLog("Device cached");
					setupBluetoothDeviceFromCache(address);
				}
				else
				{
					LogUtil.d(getPackageName(),"Device connected not cached with app, start binding ",Thread.currentThread().getStackTrace());
					appendLog("Device not cached");
					// Connected,not cached, start bind it with app
					_bind();
				}
			}
			// Not connected
			else
			{
				appendLog("Not connected with OS");
				LogUtil.d(TAG,"Not connected with OS",Thread.currentThread().getStackTrace());
				// start connect
				_connect();
			}
		}
	}

	// Bond function
	private void _bondingDevice()
	{
		mDeviceInitState = INIT_STATE_BONDING;
		broadcastNotifyUi(getConnectStatusIntent(CONNECTION_STATE_BONDING));
		mBluetoothDevice.createBond();
	}

	// Connect function
	private void _connect()
	{
		LogUtil.d(TAG, "[Process] connect",Thread.currentThread().getStackTrace());
		try
		{
			broadcastNotifyUi(getConnectStatusIntent(CONNECTION_STATE_CONNECTING));

			int delayTime = 0;
			if(mThreadConnect != null)
			{
				LogUtil.d(TAG, "Connect thread is running, skip",Thread.currentThread().getStackTrace());
				return;
			}

			final String address = mBluetoothDevice.getAddress();
			mThreadConnect = new Thread()
			{
				@Override
				public void run()
				{

					int tryTimes = 0;
					boolean gotException = false;

					while(!gotException																			// No exception
							&& mBluetoothAdapter.isEnabled()													// Bluetooth enabled
							&& getConnectedBluetoothDevice(address) == null										// Device not connected
							&& (mConnectTimeLimit<0 || tryTimes<mConnectTimeLimit) )						// No limit or times under limit
					{
						try
						{
							// Get bond device set from BluetoothAdapter, make sure device is bond
							final Set<BluetoothDevice> bondDeviceSet = mBluetoothAdapter.getBondedDevices();
							boolean bond = false;
							for(BluetoothDevice bluetoothDevice:bondDeviceSet)
							{
								if(bluetoothDevice.getAddress().equals(address))
								{
									bond = true;

									break;
								}
							}

							// if bond, start connecting
							if(bond)
							{
								LogUtil.d(getPackageName(),"Device not connected, start connect "+tryTimes,Thread.currentThread().getStackTrace());
								tryTimes++;
								createBTConnection();
							}
							// Device un-bond
							else
							{
								LogUtil.d(getPackageName(),"Device not bonded!! It was already un-bond.",Thread.currentThread().getStackTrace());
								break;
							}
							Thread.sleep(KEYLESS_CONNECT_INTERVAL);
						}
						catch (InterruptedException e)
						{
							gotException = true;
							e.printStackTrace();
						}
					}
					mThreadConnect = null;
					LogUtil.d(getPackageName(),"Leaving _connect loop.",Thread.currentThread().getStackTrace());
				}
			};

			mThreadConnect.start();
		}
		catch (Exception e)
		{
			mConnectionState = BluetoothProfile.STATE_DISCONNECTED;
			LogUtil.e(TAG, e.toString(), Thread.currentThread().getStackTrace());
		}
	}

	private void _bind()
	{
		LogUtil.d(TAG, "[Process] _bind",Thread.currentThread().getStackTrace());
		try
		{
			if(mBluetoothDevice == null)
			{
				LogUtil.d(TAG, "[Error] mBluetoothDevice is null.",Thread.currentThread().getStackTrace());
				return;
			}

			final int connectionState = getConnectionState();
			if(connectionState == BluetoothProfile.STATE_CONNECTING )
			{
				appendLog("Already binding");
				LogUtil.e(TAG, "[Error] Already binding.",Thread.currentThread().getStackTrace());
				return;
			}
			else if(connectionState == BluetoothProfile.STATE_CONNECTED)
			{
				appendLog("Already bind");
				LogUtil.e(TAG, "[Error] Already bind.",Thread.currentThread().getStackTrace());
				return;
			}
			else if(connectionState == BluetoothProfile.STATE_DISCONNECTING)
			{
				appendLog("Disconnecting, wait until disconnected");
				LogUtil.e(TAG, "[Error] Disconnecting, wait until disconnected.",Thread.currentThread().getStackTrace());
				return;
			}
			broadcastNotifyUi(getConnectStatusIntent(CONNECTION_STATE_CONNECTING));
			mConnectionState = BluetoothProfile.STATE_CONNECTING;

			// We want to directly connect to the device, so we are setting the autoConnect parameter to false.
			setBluetoothGatt(mBluetoothDevice.connectGatt(context, false, mGattCallback));
			setupConnectionTimeout(5*1000);
		}
		catch (Exception e)
		{
			mConnectionState = BluetoothProfile.STATE_DISCONNECTED;
			LogUtil.e(TAG, e.toString(), Thread.currentThread().getStackTrace());
		}
	}


    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect(final boolean reBind)
	{
		mAllowReBind = reBind;

        if (initialize() == false)
		{
			LogUtil.e(TAG, "[Error] BluetoothAdapter not initialized", Thread.currentThread().getStackTrace());
			appendLog("[Error] BluetoothAdapter not initialized");
			return;
        }

		if(getConnectionState() == BluetoothProfile.STATE_CONNECTED)
		{
			if(mBluetoothGatt != null)
			{
				mConnectionState = BluetoothProfile.STATE_DISCONNECTING;
				getBluetoothGatt().disconnect();
				appendLog("Disconnecting");
			}
			else
			{
				appendLog(formatErrorString("mBluetoothGatt is null"));
				handleBleDisconnect();
			}
		}
		else
		{
			appendLog("Not connected , skip ");
			handleBleDisconnect(); // disconnect,getConnectionState() == BluetoothProfile.STATE_DISCONNECTED
		}
    }

	public boolean createBTConnection()
	{
		LogUtil.d(TAG,"createBTConnection",Thread.currentThread().getStackTrace());

		// Must get profile proxy before create BTConnection
		if(mInputDeviceProfile == null)
		{
			// CreateBtConnection after get profile proxy
			mBTConnectionRequest = true;

			getProfileProxy();
		}
		else
		{
			// Get connected device list from OS and assign
			final ArrayList<BluetoothDevice> connectedList = getConnectedDevice(mInputDeviceProfile);
			if(mConnectedDeviceList == null || connectedList.size() > mConnectedDeviceList.size())
			{
				mConnectedDeviceList = connectedList;
			}

			// Make sure we have a preferred device to connect
			if(mBluetoothDevice != null)
			{
				// If input device is not connected,create connection now
				if(mInputDeviceProfile.getConnectionState(mBluetoothDevice) != BluetoothProfile.STATE_CONNECTED)
				{
					createBTConnection(mInputDeviceProfile, mBluetoothDevice);
				}
				// Already connected with OS
				else
				{
					// App not connected, bind it
					if(getConnectionState() == BluetoothProfile.STATE_DISCONNECTED)
					{
						_bind();
					}
				}
			}
			return true;
		}

		return false;
	}


	private void setupConnectionTimeout(final int timeout)
	{
		setupTimerTask(mRunnableConnectTimeout,timeout,-1);
	}

	private void removeConnectionTimeout()
	{
		mHandler.removeCallbacks(mRunnableConnectTimeout);
	}

	private void setupConnectDeviceRunnable(final String address,final long delay)
	{
		removeConnectDeviceRunnable();

		mRunnableConnectDevice = new Runnable()
		{
			@Override
			public void run()
			{
				LogUtil.d(TAG, "Re-Connect in removeConnectedBluetoothDevice callback", Thread.currentThread().getStackTrace());
				if (mConnectionState == BluetoothProfile.STATE_DISCONNECTED)
				{
					if(address.isEmpty())
					{
						connectDevice();
					}
					else
					{
						connectDevice(address);
					}
				}
			}
		};

		mHandler.postDelayed(mRunnableConnectDevice,delay);
	}

	private void removeConnectDeviceRunnable()
	{
		LogUtil.d(TAG,"Remove reconnect runnable",Thread.currentThread().getStackTrace());

		mHandler.removeCallbacks(mRunnableConnectDevice);

		mRunnableConnectDevice = null;
	}

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void closeGatt()
	{
        if (mBluetoothGatt != null)
		{
            getBluetoothGatt().close();
        }

		mRootService = null;
		mBluetoothGatt = null;

		mNotifyCharacteristic = null;
		mWriteCharacteristic = null;
    }

	private boolean refreshDeviceCache(final BluetoothGatt gatt)
	{
		try
		{
			final BluetoothGatt localBluetoothGatt = gatt;
			final Method localMethod = localBluetoothGatt.getClass().getMethod("refresh", new Class[0]);
			if (localMethod != null)
			{
				boolean bool = ((Boolean) localMethod.invoke(localBluetoothGatt, new Object[0])).booleanValue();
				return bool;
			 }
		}
		catch (Exception localException)
		{
			Log.e(TAG, "An exception occurred while refreshing device");
		}
		return false;
	}
    //*****************************************************************//
    //  Operate BLE  function                                          //
    //*****************************************************************//
	public static boolean isCharacteristicWritable(final BluetoothGattCharacteristic pChar)
	{
        return (pChar.getProperties() & (BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) != 0;
    }

    /**
     * @return Returns <b>true</b> if property is Readable
     */
    public static boolean isCharacteristicReadable(BluetoothGattCharacteristic pChar)
	{
        return ((pChar.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) != 0);
    }

    /**
     * @return Returns <b>true</b> if property is supports notification
     */
    public boolean isCharacteristicNotifiable(BluetoothGattCharacteristic pChar)
	{
        return (pChar.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(final BluetoothGattCharacteristic characteristic)
	{
        if (mBluetoothAdapter == null || mBluetoothGatt == null)
		{
            LogUtil.d(TAG, "[Process] BluetoothAdapter not initialized",Thread.currentThread().getStackTrace());
            return;
        }
        getBluetoothGatt().readCharacteristic(characteristic);
    }

	public void writeCharacteristic(BluetoothGattCharacteristic characteristic)
	{
        if (mBluetoothAdapter == null || mBluetoothGatt == null)
		{
            LogUtil.d(TAG, "[Process] BluetoothAdapter not initialized",Thread.currentThread().getStackTrace());
			return;
		}
        getBluetoothGatt().writeCharacteristic(characteristic);
    }


	public boolean isNotificationEnabled(final BluetoothGattCharacteristic bluetoothGattCharacteristic)
	{
		if(bluetoothGattCharacteristic == null ) return false;

	 	//final BluetoothGattDescriptor bluetoothGattDescriptor = bluetoothGattCharacteristic.getDescriptor(UUID.fromString(BleGattAttributes.DESCRIPTORS_CLIENT_CHARACTERISTIC_CONFIGURATION));

		return  (bluetoothGattCharacteristic.getValue() == BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
	}



    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,boolean enabled)
	{
        if (mBluetoothAdapter == null || mBluetoothGatt == null)
		{
            LogUtil.d(TAG, "[Process] BluetoothAdapter not initialized",Thread.currentThread().getStackTrace());
            return;
        }
        getBluetoothGatt().setCharacteristicNotification(characteristic, enabled);

		final BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(BleGattAttributes.DESCRIPTORS_CLIENT_CHARACTERISTIC_CONFIGURATION));
		if(descriptor != null)
		{
			descriptor.setValue(enabled?BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
			getBluetoothGatt().writeDescriptor(descriptor);
		}
		else
		{
			appendLog(formatErrorString("BluetoothGattDescriptor is null !!!!"));
			LogUtil.d(TAG, "[Process] BluetoothGattDescriptor is null !!!!",Thread.currentThread().getStackTrace());
		}
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices()
	{
        if (mBluetoothGatt == null) return null;

        return getBluetoothGatt().getServices();
    }

	private void readDeviceNameFromServiceWithTimer()
	{
		readDeviceNameFromService();

		settingTaskTimeoutTimer(); // getDeviceNameFromService
	}

	private void startListeningWithTimer()
	{
		startListening();

		settingTaskTimeoutTimer(); //startListening
	}

	private void startListening()
	{
		if(getNotifyGattCharacteristic(getRootService()) != null)
		{
			LogUtil.d(TAG, "[Process] startListening",Thread.currentThread().getStackTrace());
			appendLog("Start listening");
			setCharacteristicNotification(mNotifyCharacteristic, true);
		}
	}

	private void readDeviceNameFromService()
	{
		readFromCharacteristic(CHAR_DEVICE_NAME);
	}

	public void readTxPower()
	{
		LogUtil.d(TAG, "[Process] readTxPower",Thread.currentThread().getStackTrace());
		appendLog(formatSendString("readTxPower"));
		readFromCharacteristic(CHAR_TX_POWER_LEVEL);
	}

	private void readFromCharacteristic(final String strCharacteristicUuid)
	{
		try
		{
			final UUID charUUid = UUID.fromString(strCharacteristicUuid);
			final List<BluetoothGattService> serviceList = getSupportedGattServices();
			if(serviceList == null) return;

			for(int i=0; i<serviceList.size();i++)
			{
			 	final BluetoothGattService service = serviceList.get(i);
				final BluetoothGattCharacteristic characteristic = service.getCharacteristic(charUUid);
				if(characteristic != null)
				{
					readCharacteristic(characteristic);
					break;
				}
			}
		}
		catch (Exception e)
		{
			LogUtil.e(TAG, e.toString(),Thread.currentThread().getStackTrace());
		}
	}

	private ArrayList<BluetoothDevice> getConnectedDevice(final BluetoothProfile profile)
	{
		if(profile == null)
			return null;
		final List<BluetoothDevice> list = profile.getConnectedDevices();


		final ArrayList<BluetoothDevice> arrayList = new ArrayList<>(list.size());
		arrayList.addAll(list);
		if(list != null)
		{
			for(final BluetoothDevice bluetoothDevice: list)
			{
				LogUtil.d(TAG,"Connected device: "+ bluetoothDevice.getAddress(),Thread.currentThread().getStackTrace());
			}
		}

		return arrayList;
	}

	private void getProfileProxy()
	{
		final int INPUT_DEVICE = getInputDeviceHiddenConstant();
		mBluetoothAdapter.getProfileProxy(context, mProfileListener, INPUT_DEVICE);
	}

	private void closeProfileProxy()
	{
		LogUtil.d(TAG,"closeProfileProxy",Thread.currentThread().getStackTrace());
		if(mInputDeviceProfile != null)
		{
			mBluetoothAdapter.closeProfileProxy(getInputDeviceHiddenConstant(), mInputDeviceProfile);
		}
	}

    //*****************************************************************//
    //  Process flow function                                          //
    //*****************************************************************//
	private void packetIdNotMatch(final byte initId, final byte followerId)
	{
		final String message = String.format("Initiator ID =  0x02X , Follower ID =  0x02X", initId, followerId);
		LogUtil.d(TAG, message, null);
		appendLog(formatErrorString(message));
		disconnect(true);
	}


	private void settingRssiTimer()
	{
		settingRssiTimer(READ_RSSI_INTERVAL);
	}


	private void settingRssiTimer(final int period)
	{
		cancelRssiTimer();
		mReadRssiInterval = period;
		mHandler.post(readRssiRunnable);
	}

	private void cancelRssiTimer()
	{
		mHandler.removeCallbacks(readRssiRunnable);
	}

	private void settingTaskTimeoutTimer(final long timeout)
	{
		mTaskRetry = 0;
		cancelTaskTimeoutTimer(); //settingTaskTimeoutTimer
		mHandler.postDelayed(taskRetryRunnable, timeout);
	}

	private void settingTaskTimeoutTimer()
	{
		settingTaskTimeoutTimer(TIMEOUT_TASK_RETRY);
	}

	private void cancelTaskTimeoutTimer()
	{
		mHandler.removeCallbacks(taskRetryRunnable);
	}

	private void handleTaskTimeout()
	{
		mHandler.postDelayed(taskRetryRunnable, TIMEOUT_TASK_RETRY);
	}

	private void handleBleConnect()
	{
		try
		{
            appendLog(formatConnectionString("connected"));
			LogUtil.d(TAG, "[Process] Connected to GATT server.",Thread.currentThread().getStackTrace());

			if(mRunnableBondToConnect != null)
			{
				mHandler.removeCallbacks(mRunnableBondToConnect);
				mRunnableBondToConnect = null;
			}


			// Record connection state
			mConnectionState = BluetoothProfile.STATE_CONNECTED;

            broadcastNotifyUi(getGattIntent(PARAM_GATT_CONNECTED));

            // Update read rssi period
			settingRssiTimer();

			// If services characteristics is empty,
			// It is the first connect, update flow step and start discovery services
			if(getRootService() == null)
			{
				// Attempts to discover services after successful connection.
				discoverServices();
			}
			else
			{
				disconnect(true); // BleService is not null
			}
		}
		catch (Exception e)
		{
			LogUtil.e(TAG, e.toString(),Thread.currentThread().getStackTrace());
		}
	}

	private void handleBleDisconnect()
	{
		try
		{
			mConnectionState = BluetoothProfile.STATE_DISCONNECTED;

			// Clear timer when disconnected, avoid re-connect automatically
			removeConnectionTimeout(); //handleBleDisconnect

			if(isDeviceInitialized())
				playDisconnectedSound();

			closeGatt();
			cancelRssiTimer();
			cancelTaskTimeoutTimer();	//handleBleDisconnect

            broadcastNotifyUi(getGattIntent(PARAM_GATT_DISCONNECTED));

			mDeviceInitState = INIT_STATE_NONE;

			// if allowReConnect is false, skip
			if(mAllowReBind == false)
			{
				mHandler.removeCallbacksAndMessages(null);
				mBluetoothGatt = null;
				return;
			}

			mAllowReBind = false;
			if(mRunnableConnectDevice == null)
			{
				setupConnectDeviceRunnable("",3000);
			}

		}
		catch (Exception e)
		{
			LogUtil.e(TAG, e.toString(),Thread.currentThread().getStackTrace());
		}
	}

	private void discoverServicesWithTimer()
	{
		discoverServices();

		settingTaskTimeoutTimer(); // discoverServices
	}

	private void discoverServices()
	{
		mHandler.postDelayed
		(
			new Runnable()
			{
				@Override
				public void run()
				{
					boolean discoverSuccess = false;
					try
					{
						if(getConnectionState() != BluetoothProfile.STATE_CONNECTED) return;

						if (mBluetoothGatt != null)
						{
							mBluetoothAdapter.cancelDiscovery();
							discoverSuccess = getBluetoothGatt().discoverServices();
							LogUtil.d(TAG, "[Connection] Attempting to start service discover:" + discoverSuccess,Thread.currentThread().getStackTrace());
							appendLog(formatConnectionString("Attempting to start service discover:" + discoverSuccess));
						}
						else
						{
							LogUtil.e(TAG, "[Connection] mBluetoothGatt is null !!!",Thread.currentThread().getStackTrace());
							appendLog(formatErrorString("mBluetoothGatt is null !!!"));
							removeConnectionTimeout();
							disconnect(true); //mBluetoothGatt is null
						}
					}
					catch (Exception e)
					{
						LogUtil.e(TAG, e.toString(), Thread.currentThread().getStackTrace());
					}
				}
			}
		,50);
	}

	private boolean handleReceiveCsta(final byte[] receiveData,final boolean sendAck)
	{
		final int cstaLength = (receiveData[PACKET_PARAMETER]>>4) & 0x0F;
		final int cstaSequence = receiveData[PACKET_PARAMETER] & 0x0F;

		final String s = formatByteArrayToLog(receiveData);
		LogUtil.d(TAG,"[Process] Receive Csta : " + s,Thread.currentThread().getStackTrace());
		appendLog(formatReceiveString("Receive Csta"));

		// First packet of Csta data
		// Allocate a new array buffer
		if(cstaSequence == 1 || mReceivedDataBuffer == null)
		{
			mReceivedDataBuffer = new byte[RECEIVE_DATA_BUFFER_LENGTH];
			mReceiveDataCounter = 0;
		}

		// Fill data to buffer
		appendArray(mReceivedDataBuffer,subByteArray(receiveData,3,16),16*(cstaSequence-1));
		mReceiveDataCounter++;

		if(mReceiveDataCounter != cstaSequence)
		{
			LogUtil.d(TAG, "[Process] Lost Csta packet !!!",Thread.currentThread().getStackTrace());
			appendLog(formatReceiveString("Lost first Csta packet !!!"));

//			mReceivedDataBuffer = new byte[RECEIVE_DATA_BUFFER_LENGTH];
//			mReceiveDataCounter = 0;
			return false;
		}


		// Start decoding Csta when receive the last packet
		if(cstaLength == cstaSequence)
		{
			// If miss packet , notify csta in invalid
			if(cstaLength != mReceiveDataCounter)
			{
                broadcastNotifyUi(getCstaIntent(-1,new byte[]{0}));
				return false;
			}

			// Decode Csta
			decodeCstaSta(subByteArray(mReceivedDataBuffer,0,8));

			// Update csta in DeviceStatusFragment
            broadcastNotifyUi(getCstaIntent(CSTA_STA,subByteArray(mReceivedDataBuffer,0,8)));

			if(sendAck)
			{
				sendAck(true);
			}
		}

		return true;
	}

	private void decodeCstaSta(final byte[] cstaFlag)
	{
		final int arm = StaCstaDecode.getArmSTA(cstaFlag);
	}

	private void handleBleResponse(final byte[] receiveData)
	{
		try
		{
			appendLog(formatByteArrayToLog(receiveData));
			// If check sum is incorrect , restart the process flow from ready
			if(isCheckSumOk(receiveData) == false)
			{
				LogUtil.d(TAG, "[Process] Check sum error",Thread.currentThread().getStackTrace());
				appendLog(formatErrorString("Check sum error"));

				sendErrorMessage(false,PARAM_MESSAGE_CHECKSUM_ERROR);
				return;
			}

			// If receive error message ,
			if(receiveData[PACKET_COMMAND] == CMD_ERROR_MESSAGE)
			{
				LogUtil.d(TAG, "[Process] Receive ERROR_MESSAGE",Thread.currentThread().getStackTrace());
				appendLog(formatErrorString("Receive ERROR_MESSAGE : 0x" + String.format("%02X", receiveData[PACKET_PARAMETER])));

				clearAckFlag();

				final Message message = new Message();
				message.arg1 = PARAM_ERROR;
				sendCallbackMessage(message);
				return;
			}

			handleInActiveCommandFunction(receiveData);
		}
		catch (Exception e)
		{
			LogUtil.e(TAG, e.toString(),Thread.currentThread().getStackTrace());
		}
	}

	private void handleInActiveCommandFunction(final byte[] receiveData)
	{
		try
		{
			switch ((int)receiveData[PACKET_COMMAND])
			{
				case CMD_ACK:
				{
					final int ackFlag = getAckFlag();
					if(ackFlag>0)
					{
						if((ackFlag & ACK_FLAG_WRITE_PROGRAM_TABLE) > 0)
						{
							updateTimerTask(TIMER_TASK_PROGRAM_TABLE,null);
							sendProgramTableData();
						}
						else if((ackFlag & ACK_FLAG_TX_POWER) >0)
						{
							clearAckFlag(ACK_FLAG_TX_POWER);
							readTxPower();
						}
						else if((ackFlag & ACK_FLAG_TX_POWER_KEYLESS) >0)
						{
							clearAckFlag(ACK_FLAG_TX_POWER_KEYLESS);
							sendRequestBleSetting();
						}
					}
				}
				break;
				case CMD_CAR_STATUS:
				{
					handleReceiveCsta(receiveData,false);

                    initDeviceState(INIT_STATE_END); // Receive CSTA
				}
				break;

				case CMD_CHECK_CONNECTION:
				{
					final byte[] writeData = new byte[PACKET_LENGTH];
					writeData[PACKET_ID] = receiveData[PACKET_ID];
					writeData[PACKET_COMMAND] = receiveData[PACKET_COMMAND];

					final String s = formatByteArrayToLog(receiveData);
					LogUtil.d(TAG, "Receive Ping " + s,Thread.currentThread().getStackTrace());
					appendLog(formatReceiveString("Receive Ping"));
					sendCheckConnectionAck(true);
				}
				break;

				case CMD_SETTING_INFORMATION:
				{
                    // Receive response from device
					if(receiveData[PACKET_PARAMETER] == PARAM_SETTING_RESPONSE)
                    {
						final byte[] data = subByteArray(receiveData,3,16);
						loadBleSetting(data);
						saveBleSetting(data);


						final Message message = new Message();
						message.arg1 = PARAM_SETTING_INFORMATION;
						message.arg2 = (int)receiveData[PACKET_PARAMETER];
						message.obj = data;
						sendCallbackMessage(message);
                    }
					// Receive response from device, check if data is match we sent before
					else if(receiveData[PACKET_PARAMETER] == PARAM_SETTING_WRITE)
					{
						final byte[] data = subByteArray(receiveData,3,16);

						final Message message = new Message();
						message.arg1 = PARAM_SETTING_INFORMATION;
						message.arg2 = (int)receiveData[PACKET_PARAMETER];
						message.obj = data;
						sendCallbackMessage(message);
					}
				}
				break;
				case CMD_PROGRAM_TABLE_HEADER:
				{
					if(receiveData[PACKET_PARAMETER] == PARAM_READ_PROGRAM_TABLE)
					{
						setAckFlag(ACK_FLAG_READ_PROGRAM_TABLE);
						mReadCheckSum = receiveData[18];
						sendAck(true);
						setProgramTableTimerTask(PROGRAM_TABLE_READ);
					}
				}
				break;
				case CMD_PROGRAM_TABLE_DATA:
				{
					final int ackFlag = getAckFlag();
					if((ackFlag & ACK_FLAG_READ_PROGRAM_TABLE) > 0)
					{
						updateTimerTask(TIMER_TASK_PROGRAM_TABLE,null);
						sendAck(true);
						saveProgramData(receiveData);
					}
				}
				break;
			}
		}
		catch (Exception e)
		{
			LogUtil.e(TAG, e.toString(),Thread.currentThread().getStackTrace());
		}
	}

	public void sendRequestBleSetting()
	{
		LogUtil.d(TAG, "[Process] Send RequestSetting",Thread.currentThread().getStackTrace());
		appendLog(formatSendString("Send RequestSetting"));
		// Init packet content
		final byte[] writeData = new byte[PACKET_LENGTH];
		writeData[PACKET_ID] = getRandom(255);
		writeData[PACKET_COMMAND] = CMD_SETTING_INFORMATION;
		writeData[PACKET_PARAMETER] = PARAM_SETTING_REQUEST;
		writeData[PACKET_CHECK_SUM] = getCheckSum(writeData);

		sendPlainData(writeData);
	}

	private void sendErrorMessage(final boolean keepRandom,final int errorCode)
	{
		LogUtil.d(TAG, "[Error] Send error message:" + errorCode,Thread.currentThread().getStackTrace());
		appendLog(formatSendString("Send error message : " + errorCode));

		final byte[] writeData = new byte[PACKET_LENGTH];
		if(keepRandom)
			writeData[PACKET_ID] = mReceivedData[PACKET_ID];
		else
			writeData[PACKET_ID] = getRandom(255);

		writeData[PACKET_COMMAND] = CMD_ERROR_MESSAGE;
		writeData[PACKET_PARAMETER] = (byte)errorCode;
		writeData[PACKET_CHECK_SUM] = getCheckSum(writeData);
		sendPlainData(writeData);
	}

	private void sendAck(final boolean keepRandom)
	{
		appendLog(formatSendString("Send ACK"));
		LogUtil.d(TAG, "[Process] Send ACK",Thread.currentThread().getStackTrace());

		final byte[] writeData = new byte[PACKET_LENGTH];
		if(keepRandom)
			writeData[PACKET_ID] = mReceivedData[PACKET_ID];
		else
			writeData[PACKET_ID] = getRandom(255);

		writeData[PACKET_COMMAND] = CMD_ACK;
		writeData[PACKET_PARAMETER] = (byte)PARAM_NONE;
		writeData[PACKET_CHECK_SUM] = getCheckSum(writeData);


//		final String s = formatByteArrayToLog(writeData);
//		LogUtil.d(TAG, "Send ACK  : " + s);
//		appendLog(formatSendString("Send ACK  : " + NEW_LINE_CHARACTER + s));

		sendPlainData(writeData);
	}

    private void sendRequestCsta()
    {
        LogUtil.d(TAG, "[Process] Send RequestCsta",Thread.currentThread().getStackTrace());
        appendLog(formatSendString("Send RequestCsta"));

        final byte[] writeData = new byte[PACKET_LENGTH];
        writeData[PACKET_ID] = getRandom(255);
        writeData[PACKET_COMMAND] = CMD_CAR_STATUS;
        writeData[PACKET_PARAMETER] = (byte)PARAM_NONE;
        writeData[PACKET_CHECK_SUM] = getCheckSum(writeData);
        sendPlainData(writeData);
    }

	private void sendCheckConnectionAck(final boolean keepId)
	{
		LogUtil.d(TAG, "[Process] Send CheckConnection ACK",Thread.currentThread().getStackTrace());
		appendLog(formatSendString("Send CheckConnection ACK"));

		final byte[] writeData = new byte[PACKET_LENGTH];
		if(keepId)
			writeData[PACKET_ID] = mReceivedData[PACKET_ID];
		else
			writeData[PACKET_ID] = getRandom(255);
		writeData[PACKET_COMMAND] = CMD_CHECK_CONNECTION;
		writeData[PACKET_PARAMETER] = (byte)PARAM_NONE;
		writeData[PACKET_CHECK_SUM] = getCheckSum(writeData);

		sendPlainData(writeData);
	}

	public void sendCommand(final int cmd)
	{
		try
		{
			if(getConnectionState() != BluetoothProfile.STATE_CONNECTED) return;

			final byte[] writeData = new byte[PACKET_LENGTH];
			writeData[PACKET_ID] = getRandom(255);
			writeData[PACKET_COMMAND] = CMD_PHONE_CONTROL_COMMAND;
			writeData[PACKET_PARAMETER] = (byte)PARAM_NONE;

			final byte[] data = generateRandomArray(16,1);
			data[0] = (byte)cmd;
			fillData(writeData,data);

			// fill check sum
			writeData[PACKET_CHECK_SUM] = getCheckSum(writeData);


			final String s = formatByteArrayToLog(writeData);
			LogUtil.d(TAG, "[Process] Send Cmd  : " + s,Thread.currentThread().getStackTrace());
			appendLog(formatSendString("Send Cmd  : " + String.format("0x%02X", cmd)));

			sendPlainData(writeData);
		}
		catch (Exception e)
		{
			LogUtil.e(TAG, e.toString(),Thread.currentThread().getStackTrace());
		}
	}

	public void sendTxPowerLevel(final int index)
	{
		//final int level = TX_POWER_LEVEL[index];
		sendKeylessLevel(PARAM_SET_NORMAL_LEVEL,index);
		setAckFlag(ACK_FLAG_TX_POWER);
	}

	public void sendKeylessLevel(final int level)
	{
		sendKeylessLevel(PARAM_SET_KEYLESS_LEVEL,level);
		setAckFlag(ACK_FLAG_TX_POWER_KEYLESS);
	}

	private void sendKeylessLevel(final int param,final int level)
	{
		if(getConnectionState() != BluetoothProfile.STATE_CONNECTED) return;

		final byte[] writeData = new byte[PACKET_LENGTH];
		writeData[PACKET_ID] = getRandom(255);
		writeData[PACKET_COMMAND] = CMD_TX_POWER;
		writeData[PACKET_PARAMETER] = (byte)param;

		final byte[] data = new byte[16];
		data[0] = (byte)level;
		fillData(writeData,data);

		// fill check sum
		writeData[PACKET_CHECK_SUM] = getCheckSum(writeData);


		final String s = formatByteArrayToLog(writeData);
		LogUtil.d(TAG, " Send TxPower  : " + s,Thread.currentThread().getStackTrace());
		appendLog(formatSendString("Send TxPower  : " + String.format("0x%02X", level)));

		sendPlainData(writeData);
	}

	public void sendSettingInformation(final byte[] data)
	{
		if(getConnectionState() != BluetoothProfile.STATE_CONNECTED) return;

		final byte[] writeData = new byte[PACKET_LENGTH];
		writeData[PACKET_ID] = getRandom(255);
		writeData[PACKET_COMMAND] = CMD_SETTING_INFORMATION;
		writeData[PACKET_PARAMETER] = (byte)PARAM_SETTING_WRITE;

		fillData(writeData,data);

		// fill check sum
		writeData[PACKET_CHECK_SUM] = getCheckSum(writeData);


		final String s = formatByteArrayToLog(writeData);
		LogUtil.d(TAG, " Send SettingInformation  : " + s,Thread.currentThread().getStackTrace());
		appendLog(formatSendString("Send SettingInformation  : "+ NEW_LINE_CHARACTER + s));

		sendPlainData(writeData);
	}

	public void readProgramTable(final int addHigh,final int addLow,final int length)
	{
		mReadTableDataBuffer = new byte[length];
		mProgramTableDataIndex = 0;

		sendProgramTableHeader(PARAM_READ_PROGRAM_TABLE,addHigh,addLow,length);
	}

	public void writeProgramTable(final int addHigh,final int addLow,final byte[] data)
	{
		mWriteTableDataBuffer = data;
		//mWriteTableDataBuffer = generateRandomArray(32,10);
		final String s = formatByteArrayToLog(mWriteTableDataBuffer);
		LogUtil.d(TAG, "Generate ProgramTable data  : " + s,Thread.currentThread().getStackTrace());
		appendLog(formatSendString("Generate ProgramTable data  : "+ NEW_LINE_CHARACTER + s));

		mProgramTableDataIndex = 0;
		setAckFlag(ACK_FLAG_WRITE_PROGRAM_TABLE);

		sendProgramTableHeader(PARAM_WRITE_PROGRAM_TABLE,addHigh,addLow,mWriteTableDataBuffer.length);
	}

	public void sendProgramTableHeader(final int parameter,final int addHigh,final int addLow,final int length)
	{
		if(getConnectionState() != BluetoothProfile.STATE_CONNECTED) return;

		final byte[] writeData = new byte[PACKET_LENGTH];
		writeData[PACKET_ID] = getRandom(255);
		writeData[PACKET_COMMAND] = (byte)CMD_PROGRAM_TABLE_HEADER;
		writeData[PACKET_PARAMETER] = (byte)parameter;
		writeData[PACKET_TOTAL_DATA_LENGTH] = (byte)length;
		writeData[PACKET_ADDRESS_HIGH_BYTE] = (byte)addHigh;
		writeData[PACKET_ADDRESS_LOW_BYTE] = (byte)addLow;

		// Read
		int action = PROGRAM_TABLE_READ;
		if(parameter == PARAM_READ_PROGRAM_TABLE)
		{

		}
		// Write
		else
		{
			action = PROGRAM_TABLE_WRITE;
			writeData[PACKET_PROGRAM_DATA_CHECK_SUM] = getCheckSum(mWriteTableDataBuffer);
		}


		// fill check sum
		writeData[PACKET_CHECK_SUM] = getCheckSum(writeData);


		final String s = formatByteArrayToLog(writeData);
		LogUtil.d(TAG, " Send ProgramTableHeader"+s,Thread.currentThread().getStackTrace());
		appendLog(formatSendString("Send ProgramTableHeader"));

		sendPlainData(writeData);

		setProgramTableTimerTask(action);
	}

	public void sendProgramTableData()
	{
		if(getConnectionState() != BluetoothProfile.STATE_CONNECTED) return;
		if(mProgramTableDataIndex >= mWriteTableDataBuffer.length)
		{
			LogUtil.d(TAG, "No program data to send",Thread.currentThread().getStackTrace());
			appendLog(formatSendString("No program data to send"));
			clearAckFlag(ACK_FLAG_WRITE_PROGRAM_TABLE);
			return;
		}

		final byte[] writeData = new byte[PACKET_LENGTH];
		writeData[PACKET_ID] = (mWriteTableDataBuffer.length-mProgramTableDataIndex) >= PACKET_PROGRAM_DATA_LENGTH ? (byte)PACKET_PROGRAM_DATA_LENGTH : (byte)(mWriteTableDataBuffer.length-mProgramTableDataIndex);
		writeData[PACKET_COMMAND] = (byte)CMD_PROGRAM_TABLE_DATA;

		fillData(writeData,PACKET_PROGRAM_DATA,subByteArray(mWriteTableDataBuffer,mProgramTableDataIndex,17));
		mProgramTableDataIndex += 17;

		// fill check sum
		writeData[PACKET_CHECK_SUM] = getCheckSum(writeData);


		final String s = formatByteArrayToLog(writeData);
		LogUtil.d(TAG, " Send ProgramTableData : "+s,Thread.currentThread().getStackTrace());
		appendLog(formatSendString("Send ProgramTableData"));

		sendPlainData(writeData);

		setProgramTableTimerTask(PROGRAM_TABLE_WRITE);
	}

	private void sendPlainData(final byte[] data)
	{
		appendLog(formatByteArrayToLog(data));
		if(getWriteGattCharacteristic(getRootService()) != null)
		{
			sendPacket(data);
		}
		else
		{
			LogUtil.e(TAG,"[Error] Can not get write characteristic ",Thread.currentThread().getStackTrace());
		}
	}

	private void saveProgramData(final byte[] receiveData)
	{
		final byte[] programData = subByteArray(receiveData,PACKET_PROGRAM_DATA,(int)receiveData[PACKET_AVAILABLE_DATA_LENGTH]);
		setDataField(mReadTableDataBuffer,mProgramTableDataIndex,programData);
		mProgramTableDataIndex = mProgramTableDataIndex + programData.length;

		if(mProgramTableDataIndex < mReadTableDataBuffer.length-1)
		{
			setProgramTableTimerTask(PROGRAM_TABLE_READ);
		}
		else
		{
			appendLog("Receive Data : " + formatByteArrayToHexString(mReadTableDataBuffer));
		}
	}

	private void fillData(final byte[] outputPacket,final int offsetIndex,byte[] data)
	{
		setDataField(outputPacket,offsetIndex,data);
	}

	private void fillData(final byte[] outputPacket,byte[] data)
	{
	 	setDataField(outputPacket,PACKET_DATA,data);
	}

	private void sendPacket(final byte[] data)
	{
		mWriteCharacteristic.setValue(data);
		writeCharacteristic(mWriteCharacteristic);
	}

	private byte[] generateRandomArray(final int length,final int mode)
	{
		if(length <= 0 ) return null;

		final byte[] randomArray = new byte[length];
		for(int i=0;i<length;i++)
		{
			randomArray[i] = getRandom(mode);
		}

		return randomArray;
	}

	private byte getRandom(final int modeNum)
	{
		final Random r = new Random();
		return (byte)r.nextInt(modeNum);
	}

	private byte getCheckSum(final byte[] dataArray)
	{
		byte sum = 0;
		for(int i=0;i<dataArray.length;i++)
		{
			sum += dataArray[i];
		}

		sum = (byte)(0xFF ^ sum);

		return sum;
	}

	private byte[] xorByteArray(final byte[] array1, final byte[] array2)
	{
		final byte[] xorArray = new byte[array1.length];
		for(int i=0;i<array1.length;i++)
		{
			xorArray[i] = (byte)(array1[i]^ array2[i]);
		}

		return xorArray;
	}

	private boolean isCheckSumOk(final byte[] data)
	{
		if(data == null || data.length == 0) return false;

		byte sum = 0;
		for(int i=0;i<data.length;i++)
		{
			sum+=data[i];
		}

		return sum == (byte)0xFF;
	}

	private byte[] subByteArray(final byte[] byteArray,final int offset,final int length)
	{
		if(length <= 0)
			return null;

		if(length >= byteArray.length)
			return byteArray;

		final byte[] subArray = new byte[length];
		for(int i= 0;i<length && i+offset < byteArray.length;i++)
		{
			subArray[i] = byteArray[i+offset];
		}

		return subArray;
	}

	private byte[] appendArray(final byte[] desArray,final byte[] srcArray,final int offset)
	{
		if(desArray == null || srcArray == null)
			return null;

		for(int i= 0;i<srcArray.length && offset+i < desArray.length;i++)
		{
			desArray[offset+i] = srcArray[i];
		}

		return desArray;
	}

	public byte[] longToBytes(long x)
	{
    	ByteBuffer buffer = ByteBuffer.allocate(Long.SIZE / Byte.SIZE);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
    	buffer.putLong(x);
    	return buffer.array();
	}

    private long byteArrayToLong(final byte[] array)
    {
        ByteBuffer buffer = ByteBuffer.wrap(array);
//		buffer.order(ByteOrder.BIG_ENDIAN);
//		System.out.println(buffer.getLong());
//		buffer = ByteBuffer.wrap(array);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        final long value = buffer.getLong();
        System.out.println(value);
        return value;
    }

    private String byteArrayToString(final byte[] array)
    {
        String str = "";
        for(int i=0;i<array.length;i++)
        {
            str += String.valueOf((int)array[i]);
        }

        return str;
    }

	private String formatByteArrayToHexString(final byte[] byteArray)
	{
		String outputString = "";
		for(int i=0 ;i<byteArray.length;i++)
		{
			outputString = outputString + String.format("%x",byteArray[i]);
		}

		return outputString;
	}

    //*****************************************************************//
    //  Update UI  function                                            //
    //*****************************************************************//
	private void appendLog(final String log)
	{
        final Intent intent = new Intent(ACTION_DEBUG_SERVICE_TO_UI);
        intent.putExtra("param",PARAM_LOG);
        intent.putExtra("log",log);
        broadcastIntent(intent);
	}

	private void sendCallbackMessage(final Message message)
	{
		if(mIpcCallbackHandler != null)
		{
			mIpcCallbackHandler.sendMessage(message);
		}
	}


    //*****************************************************************//
    //  Broadcast  function                                            //
    //*****************************************************************//
    private void broadcastNotifyUi(final Intent intent)
    {
        intent.setAction(ACTION_SERVICE_NOTIFY_UI);
        broadcastIntent(intent);
    }

    private void broadcastDebugNotifyUi(final Intent intent)
    {
        intent.setAction(ACTION_DEBUG_SERVICE_TO_UI);
        broadcastIntent(intent);
    }

	private void notifyScanResult()
	{
		final ArrayList<BleSerializableDevice> deviceList = new ArrayList<BleSerializableDevice>();
		for(int i=0 ;i<mBleDeviceRssiAdapter.getCount(); i++)
		{
			final BleDeviceRssi deviceRssi = mBleDeviceRssiAdapter.getDevice(i);
			final BleSerializableDevice simpleBleDevice = new BleSerializableDevice();
			final String nameUTF8 = deviceRssi.nameUTF8;
			simpleBleDevice.rssi = deviceRssi.rssi;
			simpleBleDevice.name = (nameUTF8 != null) ? nameUTF8 :  deviceRssi.bluetoothDevice.getName();
			simpleBleDevice.address = deviceRssi.bluetoothDevice.getAddress();

			deviceList.add(simpleBleDevice);
		}

		final Message message = new Message();
		message.arg1 = PARAM_SCAN_RESULT;
		message.obj = deviceList;
		sendCallbackMessage(message);
	}

    private Intent getGattIntent(final int param)
    {
        final Intent intent = new Intent();
        intent.putExtra("param",param);
        return intent;
    }

	private void broadcastIntent(final Intent intent)
	{
		LogUtil.d(TAG, String.format("[Broadcast] :  %s" ,intent.getAction()),Thread.currentThread().getStackTrace());
		try
		{
			sendBroadcast(intent);
		}
		catch (Exception e)
		{
			LogUtil.e(TAG,e.toString(),Thread.currentThread().getStackTrace());
		}
	}

	private Intent getRssiIntent(final int rssi,final int averageRssi)
	{
        final Intent intent = new Intent();
        intent.putExtra("param",PARAM_RSSI);
		intent.putExtra("rssi",rssi);
		intent.putExtra("avg_rssi", averageRssi);
        return intent;
	}

	private Intent getConnectStatusIntent(final int connectionStatus)
	{
		final Intent intent = new Intent();
		try
		{
			// Notify to TabActivity, it will pass to next page
            intent.putExtra("param",PARAM_CONNECT_STATUS);
			intent.putExtra("connectionStatus",connectionStatus);
		}
		catch (Exception e)
		{
			LogUtil.e(TAG,e.toString(),Thread.currentThread().getStackTrace());
		}

        return intent;
	}

	private Intent getCstaIntent(final int mode,final byte[] array)
	{
        final Intent intent = new Intent();
		final long csta =  byteArrayToLong(array);
        intent.putExtra("param",PARAM_CSTA);
		intent.putExtra("mode",mode);
		intent.putExtra("csta",csta);

        return intent;
	}

	private Intent getProcessStepIntent(final int step)
	{
        final Intent intent = new Intent();
        intent.putExtra("param",PARAM_PROCESS_STEP);
        intent.putExtra("step",step);
        return intent;
	}

    // Register a receiver to receive message from UI activity
	private void registerBleOperateReceiver()
	{
		try
		{
			final IntentFilter intentFilter = new IntentFilter();
			intentFilter.addAction(ACTION_UI_NOTIFY_SERVICE);
			registerReceiver(mMessageFromUiReceiver, intentFilter);
		}
		catch (Exception e)
		{

		}
	}



	//=================================================================//
	//																   //
	//  Connect / Cached List Function 		                           //
	//																   //
	//=================================================================//
	//---------------------------------------
	// Connect list
	public boolean isBluetoothDeviceConnected(final String address)
	{
		if(mConnectedDeviceList == null || mConnectedDeviceList.size() == 0)
			return false;

		if(getConnectedBluetoothDevice(address) != null)
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	public void clearConnectedBluetoothDevice()
	{
		if(mConnectedDeviceList != null)
		{
			for(int i=0;i<mConnectedDeviceList.size();i++)
			{
				final BluetoothDevice bluetoothDevice = mConnectedDeviceList.get(i);
				closeBTConnection(bluetoothDevice);
			}
			mConnectedDeviceList.clear();
			clearCachedBluetoothDevice();
		}
	}

	public void addConnectedBluetoothDevice(final BluetoothDevice bluetoothDevice)
	{
		if(mConnectedDeviceList == null)
			mConnectedDeviceList = new ArrayList<>();

		LogUtil.d(TAG,"addConnectedBluetoothDevice , new device : " + bluetoothDevice.getAddress(),Thread.currentThread().getStackTrace());
		if(getConnectedBluetoothDevice(bluetoothDevice.getAddress()) == null)
		{
			mConnectedDeviceList.add(bluetoothDevice);

			// No preferred bluetooth
			if(mBluetoothDevice == null)
			{
				LogUtil.d(TAG,"addConnectedBluetoothDevice , current device is null ",Thread.currentThread().getStackTrace());
				return;
			}

			if(bluetoothDevice.getAddress().equals(mBluetoothDevice.getAddress()))
			{
				// During bonding process, do not connect , do nothing until bonding finish
				if(mDeviceInitState == INIT_STATE_BONDING || mDeviceInitState == INIT_STATE_BOND_TO_CONNECT || bluetoothDevice.getBondState() != BluetoothDevice.BOND_BONDED)
				{
					LogUtil.d(TAG,"After bonding , wait for disconnect ",Thread.currentThread().getStackTrace());
					return;
				}

				// Check if it is a bonded device when we receiving ACL_CONNECTED message
				// If it is not bonded, it is the first ACL_CONNECTED during bonding or not what we want
				if(bluetoothDevice.getBondState() == BluetoothDevice.BOND_BONDED)
				{
					LogUtil.d(TAG,String.format("mAutoConnectOnDisconnect = %s , mManualConnect = %s",mAutoConnectOnDisconnect,mManualConnect),Thread.currentThread().getStackTrace());
					if(mDeviceInitState == INIT_STATE_BOND_OK || mManualConnect || mAutoConnectOnDisconnect)
					{
						LogUtil.d(TAG,"addConnectedBluetoothDevice , start connect device after 3 seconds",Thread.currentThread().getStackTrace());
						setupConnectDeviceRunnable(bluetoothDevice.getAddress(),3000);
					}
				}
			}
		}
	}

	public void removeConnectedBluetoothDevice(final BluetoothDevice bluetoothDevice)
	{
		if(bluetoothDevice == null)
			return;

		boolean removeFromCachedList = false;
		if(mConnectedDeviceList != null)
		{
			for(int i=0;i<mConnectedDeviceList.size();i++)
			{
				final BluetoothDevice device = mConnectedDeviceList.get(i);
				if(device.getAddress().equals(bluetoothDevice.getAddress()))
				{
					mConnectedDeviceList.remove(i);

					removeFromCachedList = removeBluetoothDeviceFromCache(device.getAddress());

					break;
				}
			}
		}

		// When receiving ACL_DISCONNECTED message
		// Check if we are in bonding process, if yes , start connect now
		if(mDeviceInitState == INIT_STATE_BOND_TO_CONNECT)
		{
			if(mRunnableBondToConnect != null)
			{
				mHandler.removeCallbacks(mRunnableBondToConnect);
				mRunnableBondToConnect = null;
			}

			mDeviceInitState = INIT_STATE_BOND_OK;

			createBTConnection();
			return;
		}

		// If autoConnect is enabled or is tuning key less, re-connect it
		if(mAutoConnectOnDisconnect)
		{
			setupConnectDeviceRunnable("",3000);
		}
	}

	public BluetoothDevice getConnectedBluetoothDevice(final String address)
	{
		if(mConnectedDeviceList != null)
		{
			for(int i=0;i<mConnectedDeviceList.size();i++)
			{
				final BluetoothDevice device = mConnectedDeviceList.get(i);
				LogUtil.d(TAG,"getConnectedBluetoothDevice , connected device : " + device.getAddress(),Thread.currentThread().getStackTrace());
				if(device.getAddress().equals(address))
				{
					return device;
				}
			}
		}

		return null;
	}


	//---------------------------------------
	// Cached list
	public boolean isBluetoothDeviceCached(final String address)
	{
		if(mCachedDeviceList == null || mCachedDeviceList.size() == 0)
			return false;

		if(getCachedBluetoothDevice(address) != null)
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	public CachedBluetoothDevice getCachedBluetoothDevice(final String address)
	{
		if(mCachedDeviceList != null)
		{
			for(int i=0;i<mCachedDeviceList.size();i++)
			{
				final CachedBluetoothDevice device = mCachedDeviceList.get(i);
				final BluetoothDevice bluetoothDevice = device.bluetoothDevice;
				if(bluetoothDevice != null && bluetoothDevice.getAddress().equals(address))
				{
					return device;
				}
			}
		}

		return null;
	}

	public void addBluetoothDeviceToCache(final BluetoothDevice bluetoothDevice,final BluetoothGatt bluetoothGatt)
	{
		if(mCachedDeviceList == null)
			mCachedDeviceList = new ArrayList<>();

		if(getCachedBluetoothDevice(bluetoothDevice.getAddress()) == null)
		{
			mCachedDeviceList.add(new CachedBluetoothDevice(bluetoothDevice,bluetoothGatt));
		}
	}

	public boolean removeBluetoothDeviceFromCache(final String address)
	{
		if(address == null)
			return false;

		boolean containDevice = false;
		if(mCachedDeviceList != null)
		{
			for(int i=0;i<mCachedDeviceList.size();i++)
			{
				final CachedBluetoothDevice cachedDevice = mCachedDeviceList.get(i);
				final BluetoothDevice bluetoothDevice = cachedDevice.bluetoothDevice;
				if(bluetoothDevice != null && bluetoothDevice.getAddress().equals(address))
				{
					mCachedDeviceList.remove(i);

					containDevice = true;

					cachedDevice.bluetoothGatt.disconnect();
					cachedDevice.bluetoothGatt.close();
				}
			}
		}

		return  containDevice;
	}

	public void clearCachedBluetoothDevice()
	{
		if(mCachedDeviceList != null)
		{
			for(int i=0;i<mCachedDeviceList.size();i++)
			{
				final CachedBluetoothDevice cachedBluetoothDevice = mCachedDeviceList.get(i);
				if(mBluetoothDevice != null && mBluetoothDevice.getAddress().equals(cachedBluetoothDevice.bluetoothDevice.getAddress()))
				{
					disconnect(false);
				}
				else
				{
					cachedBluetoothDevice.bluetoothGatt.disconnect();
					cachedBluetoothDevice.bluetoothGatt.close();
				}
			}
			mCachedDeviceList.clear();
		}
	}



   //*****************************************************************//
    //  Debug Message function                                         //
    //*****************************************************************//
	private String formatByteArrayToLog(final byte[] byteArray)
	{
		String outputString = "";
		for(int i=0 ;i<byteArray.length;i++)
		{
			outputString = outputString + String.format(" %02X",byteArray[i]);
			if((i+1) % 8 == 0)
				outputString = outputString+ NEW_LINE_CHARACTER;
		}

		return outputString;
	}

	private String formatErrorString(final String text)
	{
		return String.format("<font color='red'>%s</font>",text);
	}


	private String formatConnectionString(final String text)
	{
		return String.format("<font color='black'>%s</font>",text);
	}

	private String formatReceiveString(final String text)
	{
		return String.format("<font color='blue'>%s</font>",text);
	}

	private String formatSendString(final String text)
	{
		return String.format("<font color='#008000'>%s</font>",text);
	}

	private String formatAesString(final String text)
	{
		return String.format("<font color='#DAA520'>%s</font>",text);
	}

	private String formatTimesString(final String text)
	{
		return String.format("<font color='#ACACAC'>%s</font>",text);
	}

	//*****************************************************************//
	//  IO  function   	 	                                           //
	//*****************************************************************//
	private void playLoginSuccessSound()
	{
		//playSound(RingtoneManager.TYPE_NOTIFICATION);
		playSound(R.raw.connected);
	}
	private void playDisconnectedSound()
	{
		playSound(R.raw.disconnected);
	}

	private void playSound(int res)
	{
		try
		{
			final MediaPlayer mp = MediaPlayer.create(this, res);
			mp.start();
			//mp.release();


//			Uri notification = RingtoneManager.getDefaultUri(type);
//			Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
//			r.play();
		}
		catch (Exception e)
		{

		}
	}

	public String readAppSetting()
	{
		final SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.CONFIG_FILE_SLBLE_SETTING, Context.MODE_PRIVATE);
		final String dataString =  sharedPreferences.getString(Constants.CONFIG_ITEM_APP_SETTING,"");
		return dataString;
	}

	public void saveAppSetting()
	{
		final JSONObject jsonObject = new JSONObject();
		try
		{
			jsonObject.put(getString(R.string.title_auto_connect),mAutoConnectOnDisconnect?1:0);
			jsonObject.put(getString(R.string.title_auto_scroll),mAutoScroll?1:0);
		}
		catch (Exception e)
		{

		}

		saveAppSetting(jsonObject);
	}

	public void saveAppSetting(final JSONObject jsonObject)
	{
		final SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.CONFIG_FILE_SLBLE_SETTING, Context.MODE_PRIVATE);
		final String dataString =  jsonObject.toString();
		sharedPreferences.edit().putString(Constants.CONFIG_ITEM_APP_SETTING,dataString).apply();
	}

	public byte[] readBleSetting()
	{
		final SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.CONFIG_FILE_SLBLE_SETTING, Context.MODE_PRIVATE);
		final String encodeString =  sharedPreferences.getString(Constants.CONFIG_ITEM_BLE_SETTING,"");
		byte[] data = Base64.decode(encodeString,Base64.DEFAULT);

		if(data == null || data.length == 0)
		{
			data = new byte[16];
		}
		return data;
	}


	public void saveBleSetting(final byte[] data)
	{
		final SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.CONFIG_FILE_SLBLE_SETTING, Context.MODE_PRIVATE);
		final String encodeString =  Base64.encodeToString(data,Base64.DEFAULT);
		sharedPreferences.edit().putString(Constants.CONFIG_ITEM_BLE_SETTING,encodeString).apply();

		initDeviceState(INIT_STATE_CSTA);
	}

	//*****************************************************************//
	//  Special  function                                              //
	//*****************************************************************//
    // Because INPUT_DEVICE class is hidden by android
    // We can only use Class function to get it
	public static int getInputDeviceHiddenConstant()
	{
		final Class<BluetoothProfile> clazz = BluetoothProfile.class;
		for (Field f : clazz.getFields())
		{
			int mod = f.getModifiers();
			if (Modifier.isStatic(mod) && Modifier.isPublic(mod) && Modifier.isFinal(mod))
			{
				try
				{
					if (f.getName().equals("INPUT_DEVICE"))
					{
						return f.getInt(null);
					}
				}
				catch (Exception e)
				{
					Log.e(TAG, e.toString(), e);
				}
			}
		}
		return -1;
	}

	// Connect to BluetoothInputDevice
	public boolean createBTConnection(BluetoothProfile proxy, BluetoothDevice btDevice)
	{
		Boolean returnValue = false;
		Class class1 = null;
		try
		{
			class1 = Class.forName("android.bluetooth.BluetoothInputDevice");
			Method createConnectionMethod = class1.getMethod("connect", new Class[] {BluetoothDevice.class});
			returnValue = (Boolean) createConnectionMethod.invoke(proxy, btDevice);
		}
		catch (Exception e)
		{

		}
		return returnValue.booleanValue();
	}

	public boolean closeBTConnection(final BluetoothDevice bluetoothDevice)
	{
		if(bluetoothDevice != null)
		{
			if(mInputDeviceProfile != null && mInputDeviceProfile.getConnectionState(bluetoothDevice) == BluetoothProfile.STATE_CONNECTED)
			{
				return closeBTConnection(mInputDeviceProfile,bluetoothDevice);
			}
		}

		return false;
	}

	public boolean closeBTConnection(BluetoothProfile proxy, BluetoothDevice btDevice)
	{
		Boolean returnValue = false;
		Class class1 = null;
		try
		{
			class1 = Class.forName("android.bluetooth.BluetoothInputDevice");
			Method createConnectionMethod = class1.getMethod("disconnect", new Class[] {BluetoothDevice.class});
			returnValue = (Boolean) createConnectionMethod.invoke(proxy, btDevice);
		}
		catch (Exception e)
		{

		}
		return returnValue.booleanValue();
	}
}
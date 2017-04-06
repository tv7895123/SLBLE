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
	private final String SHOW_ADV_DATA_ADDRESS_1 = "48:36:5F:00:00:00";
	private final String SHOW_ADV_DATA_ADDRESS_2 = "48:36:5F:22:22:22";

	//=================================================================//
	//																   //
	//  Constant Variables                                             //
	//																   //
	//=================================================================//
	private static final String TAG = "BLE";
	private final String NEW_LINE_CHARACTER = "<BR/>";
	private final int OFF = 0;
	private final int ON = 1;
	private final int TIMEOUT_PROGRAM_TASK = 2000;

	public static final int CSTA_STA = 0;
	public static final int[] TX_POWER_LEVEL = new int[]
			{
					-17,-15,-10,-5,0,2,4,7
			};

	private final long PRBS_FEEDBACK_TABLE[] =
			{
					//0b11110000010000000000000000000001
					0b10000000000000000000001000001111
					, /* [32, 31, 30, 29, 28, 22] */

					0b10000000000000000001000000001111
					, /* [32, 31, 30, 29, 28, 19] */

					0b10010010010000000000000000010100
					, /* [32, 29, 27,  9,  6,  3] */

					0b10000000001010000001000000101000
					, /* [32, 28, 26, 19, 12, 10] */

					0b10000000000000010000001001000000
			  /* [32, 25, 22, 15]         */
			};


	//--------------------------------------------------------------------
	// Ack Flag
	public static final int ACK_FLAG_TX_POWER = 0x1;
	public static final int ACK_FLAG_TX_POWER_KEYLESS = 0x2;

	//-------------------------------------------------------------------
	// Timeout Task Index
	public final int TASK_NONE = 0;
	public final int TASK_BOND = 0x01;
	public final int TASK_BIND = 0x02;

	//--------------------------------------------------------------------
	// Connection State
	public static final int CONNECTION_STATE_BONDING = 0;
	public static final int CONNECTION_STATE_CONNECTING = 1;
	public static final int CONNECTION_STATE_BINDING = 2;
	public static final int CONNECTION_STATE_CANCEL = 3;
	public static final int CONNECTION_STATE_BOND_FAILED = 4;
	public static final int CONNECTION_STATE_BOND_SUCCESS = 5;

	//--------------------------------------------------------------------
	// Connection Parameter
	public static final int CONNECTION_MAX_RETRY = -1;
	public static final int CONNECTION_MAX_CONNECT = -1;
	public static final int SCAN_INTERVAL_MS = 500;
	public static final int READ_RSSI_INTERVAL = 3000;
	public static final long SCAN_PERIOD = 10 * 1000; // Stops scanning after 10 seconds.


	//--------------------------------------------------------------------
	// Process
	private final int TASK_BIND_RETRY_MAX = 10;
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
	public static final int INIT_STATE_SEND_TEST_COMMAND = 99;
	public static final int INIT_STATE_TEST_SUCCESS = 100;

	//-------------------------------------------------------------------
	// Keyless
	public final int KEYLESS_CONNECT_INTERVAL = 5*1000;												// ms
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
	public static final int MODE_CONTROL_COMMAND_STOP = 6;

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
	public static final int IPC_COMMAND_COMMAND_STATE = 0x1002;
	public static final int IPC_COMMAND_PROGRAM_RESULT = 0x1003;
	public static final int IPC_COMMAND_PROGRAM_TASK_FINISH = 0x1004;

	//=================================================================//
	//																   //
	//  Global Variables                                               //
	//																   //
	//=================================================================//
	// Basic
	private final int RECEIVE_DATA_BUFFER_LENGTH = 100;
	private int  mAckFlag = 0;

	// Connect
	private boolean mAllowConnect = false;

	// App Setting
	private boolean mAutoConnectOnDisconnect = false;
	private boolean mAutoScroll = false;
	private boolean mAutoTest = false;

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
	private int mTimeoutTask = TASK_NONE;
	private byte mPacketId = 0x0;
	private byte[] mReceivedData = null;
	private byte[] mReceivedDataBuffer = null;
	private int mReceiveDataCounter = 0;
	private int mTaskRetry = 0;
	private int mReadRssiInterval = READ_RSSI_INTERVAL;
	private int mBindRetry = 0;

	// Keyless
	private int mConnectTimeLimitScreenOn = KEYLESS_CONNECT_TIME_MAX;		// Limit times for screen on, default MAX
	private int mConnectTimeLimitScreenOff = KEYLESS_CONNECT_TIME_HOUR;		// Limit times for screen off, default ONE HOUR
	private int mConnectTimeLimit = mConnectTimeLimitScreenOn;						// Limit times, default set as ScreenOn

	// ProgramTable
	private long mPrbsKey;
	private int mPrbsJump;
	private int mProgramModeState = OFF;
	private int mQueueStartIndex = 0;
	private int mQueueEndIndex = 0;

	private long mDisconnectTimeStamp = 0;
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
	private static Thread mThreadProgram = null;
	private static Thread mThreadSendCommand = null;

	private SlbleCommand mSlbleCommand = null;


	private Thread mThreadTimer = null;
	private TaskObject[] mTaskQueue = null;
	private TaskObject mCurrentTask = null;
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

	public class TaskObject
	{
		public static final int STATE_IDLE = 0;
		public static final int STATE_WAIT = 1;

		public int state;				// IDLE,WAIT
		public byte taskCommand;
		public byte taskParameter;

		public Object taskData;

		public long timeout;
		public long updateTime;
	}

	public class ProgramData
	{
		public int dataId;			// AF,SF,LNT,CH

		public byte addressHigh;	// START-ADDRESS
		public byte addressLow;		// START ADDRESS
		public int dataLength;		// TOTAL DATA LENGTH
		public int dataCount;		//
		public byte[] dataBuffer;

		public byte subAddressHigh;
		public byte subAddressLow;
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
				stopConnectThread();

				// Delay to start new thread to avoid state error
				mHandler.postDelayed(new Runnable()
				{
					@Override
					public void run()
					{
						if(mThreadConnect == null)
						{
							_connect(); // ACTION_SCREEN_OFF
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
					_connect(); //ACTION_SCREEN_ON
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

			if(mBluetoothDevice == null || device == null)
			{
				return;
			}


			// Only handle message with our device
			if(device.getAddress().equals(mBluetoothDevice.getAddress()))
			{
				// After device bonded, connect to it
				if (previousBondState == BluetoothDevice.BOND_BONDING && bondState == BluetoothDevice.BOND_BONDED)
				{
					if(mDeviceInitState != INIT_STATE_BONDING)
					{
						LogUtil.d(TAG,"Not at bonding state, skip" ,Thread.currentThread().getStackTrace());
						return;
					}

					removeTaskTimeout(); // Remove Bonding timeout runnable

					broadcastNotifyUi(getConnectStatusIntent(CONNECTION_STATE_BOND_SUCCESS));
					appendLog("Device bonded");

					if(mBluetoothGatt == null)
					{
						// Direct connect after bonded
						if(true)
						{
							mDeviceInitState = INIT_STATE_BOND_OK;

							createBTConnection();

							mHandler.postDelayed(new Runnable()
							{
								@Override
								public void run()
								{
									_connect();
								}
							},500);
						}
						else
						// Wait or make a disconnection after bonded for some mobile phones
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
									// Timeout and disconnect, wait next connected
									if(mBluetoothDevice != null && mInputDeviceProfile != null)
									{
										closeBTConnection(mInputDeviceProfile,mBluetoothDevice);
									}
								}
							};

							// Some phones may DISCONNECT from BluetoothDevice after bonding, but some may not
							// This is for phones which won't receive DISCONNECT message.
							// So I set a timer to continue connecting
							mHandler.postDelayed(mRunnableBondToConnect,TIMEOUT_BOND_TO_DISCONNECT);
						}
					}
				}
				else if (previousBondState == BluetoothDevice.BOND_BONDING && bondState == BluetoothDevice.BOND_NONE)
				{
					if(mDeviceInitState != INIT_STATE_BONDING)
					{
						LogUtil.d(TAG,"Not at bonding state, skip" ,Thread.currentThread().getStackTrace());
						return;
					}

					mDeviceInitState = INIT_STATE_NONE;

					removeTaskTimeout(); // Remove Bonding timeout runnable
					broadcastNotifyUi(getConnectStatusIntent(CONNECTION_STATE_BOND_FAILED));
				}
				else if(previousBondState == BluetoothDevice.BOND_BONDED && bondState == BluetoothDevice.BOND_NONE)
				{
					initPreferredDevice("");
				}
			}
		}
	};

	// Action when connection timeout
	private final Runnable mRunnableTimeout = new Runnable()
	{
		@Override
		public void run()
		{
			LogUtil.d(TAG,"Task timeout",Thread.currentThread().getStackTrace());
			mHandler.removeCallbacks(mRunnableTimeout);
			switch (mTimeoutTask)
			{
				case TASK_BOND:
				{
					appendLog("Bonding timeout");
					LogUtil.d(TAG,"Bonding timeout",Thread.currentThread().getStackTrace());
					broadcastNotifyUi(getConnectStatusIntent(CONNECTION_STATE_BOND_FAILED));
				}
				break;
				case TASK_BIND:
				{
					appendLog("Bind timeout");
					LogUtil.d(TAG,"Bind timeout",Thread.currentThread().getStackTrace());
					disconnect();
					handleBleDisconnect();
				}
				break;
			}
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
				long start = System.currentTimeMillis();
				mBleDeviceRssiAdapter.sortList();
				LogUtil.d(TAG,String.format("Sort list cost %d ms",System.currentTimeMillis()-start),Thread.currentThread().getStackTrace());
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
						disconnect(); // readRssiRunnable
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
						disconnect(); //Discovery service but device disconnected
					}
				}
				else
				{
					appendLog("Discovery service failed.");
					LogUtil.d(TAG, "Discovery service failed.", Thread.currentThread().getStackTrace());
					disconnect(); //Discovery service failed
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
						// Get and set device name
						String deviceName = new String(characteristic.getValue(), "UTF8");
						if(deviceName.length() > 15)
						{
							deviceName = deviceName.substring(0,15);
						}

						broadcastNotifyUi(getGattIntent(PARAM_GATT_READ_DEVICE_NAME));

						if(!deviceName.startsWith(KEYWORD_SLBLE))
						{
							appendLog(formatConnectionString("Unsupported device !!!"));
							//disconnect(false);  //Unsupported device
							return;
						}

						if(mAutoTest)
						{
							LogUtil.d(TAG,gatt.getDevice().getAddress(),Thread.currentThread().getStackTrace());
							// Notify to update UI
							broadcastNotifyUi(getProcessStepIntent(INIT_STATE_SEND_TEST_COMMAND));
							sendDebugCommand(1);
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
						disconnect(); //Enable notification failed
						appendLog("Enable notification failed");
						LogUtil.d(TAG, "Enable notification failed.",Thread.currentThread().getStackTrace());
					}
				}
				else
				{
					appendLog("onDescriptorWrite unknown status");
					disconnect();
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

			if(!isBluetoothDeviceConnected(mBluetoothDevice.getAddress()))
			{
				LogUtil.d(TAG, String.format("Device %s already disconnected, un-register gatt callback",mBluetoothDevice.getAddress()),Thread.currentThread().getStackTrace());
				getBluetoothGatt().disconnect();
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
	private final BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback()
	{
		@Override
		public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord)
		{
			try
			{
				if((DEBUG_MODE & DEBUG_SHOW_ADV_DATA) > 0)
				{
					if(device.getAddress().equals(SHOW_ADV_DATA_ADDRESS_1) || device.getAddress().equals(SHOW_ADV_DATA_ADDRESS_2))
					{
						final Intent intent = new Intent(ACTION_DEBUG_SERVICE_TO_UI);
						intent.putExtra("param",PARAM_ADV_DATA);
						intent.putExtra("address",device.getAddress());
						intent.putExtra("adv_data",scanRecord);
						broadcastIntent(intent);
					}
				}

				if(!mScanning)
					return;

				if(!device.getAddress().startsWith(FILTER_ADDRESS))
					return;

				final long updateTime = mBleDeviceRssiAdapter.getDeviceUpdateTime(device.getAddress());
				LogUtil.d(TAG,String.format("Address = %s, UpdateTime = %d",device.getAddress(), updateTime),Thread.currentThread().getStackTrace());
				if(updateTime >= 0 && updateTime <= 200)
					return;


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
				LogUtil.d(TAG, String.format("[Process] mLeScanCallback addDevice : %s - %s", nameUTF8,device.getAddress()),Thread.currentThread().getStackTrace());



				// Add scanned device into list
				if(device.getAddress().startsWith(FILTER_ADDRESS))
				{
					mBleDeviceRssiAdapter.addDevice(device, nameUTF8, rssi);
				}
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
								if(mBluetoothDevice.getBondState() == BluetoothDevice.BOND_BONDED)
								{
									connectDevice(mBluetoothDevice.getAddress());  // Manual connect
								}
								else if(mBluetoothDevice.getBondState() == BluetoothDevice.BOND_BONDING)
								{
									Toast.makeText(context,"Device is binding now, please wait binding process finished.",Toast.LENGTH_LONG).show();
								}
								else
								{
									Toast.makeText(context,"Device not bonded, please bond device first.",Toast.LENGTH_LONG).show();
								}
							}
							else
							{
								Toast.makeText(context,"Cannot find device!!",Toast.LENGTH_LONG).show();
							}
						}
						break;

						case MODE_DISCONNECT:
						{
							cutAllConnection();
						}
						break;

						case MODE_CONTROL_COMMAND:
						{
							final int command = intent.getIntExtra("command", 0);
							final int interval = intent.getIntExtra("interval", 200);
							final int times = intent.getIntExtra("times", 0);

							if(times == 0)
							{
								sendCommand(command);
							}
							else
							{
								startCommandThread(command,interval,times);
							}
						}
						break;

						case MODE_CONTROL_COMMAND_STOP:
						{
							stopCommandThread();
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

		mTaskQueue = new TaskObject[8];

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
		{
			disconnect();
		}

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

	public boolean getAutoTest()
	{
		return mAutoTest;
	}

	public void setAutoTest(final boolean enable)
	{
		mAutoTest = enable;
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
			initPreferredDevice(address);

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
			initPreferredDevice("");
		}
	}

	public void cutAllConnection()
	{
		if(!isBluetoothEnabled())
		{
			appendLog("Bluetooth is not enabled");
			LogUtil.d(TAG,"Bluetooth is not enabled",Thread.currentThread().getStackTrace());
			return;
		}

		stopConnectThread();

		setAllowConnect(false);

		disconnect();
		if(mInputDeviceProfile != null && mInputDeviceProfile.getConnectionState(mBluetoothDevice) == BluetoothProfile.STATE_CONNECTED)
		{
			appendLog("closeBTConnection");
			closeBTConnection(mInputDeviceProfile,mBluetoothDevice);
		}
	}

	private void setupTaskTimeout(final int task,final int timeout)
	{
		mTimeoutTask = task;
		setupTimerTask(mRunnableTimeout,timeout,-1);
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

	private void removeTaskTimeout()
	{
		LogUtil.d(TAG,"",Thread.currentThread().getStackTrace());
		mTimeoutTask = TASK_NONE;
		mHandler.removeCallbacks(mRunnableTimeout);
	}

	private void loadAppSetting(final String appSetting)
	{
		if(appSetting.isEmpty())
		{
			setAutoConnectOnDisconnect(false);
			setAutoScroll(false);
			setAutoTest(false);
		}
		else
		{
			try
			{
				JSONObject jsonObject = new JSONObject(appSetting);
				setAutoConnectOnDisconnect((int)jsonObject.get(getString(R.string.title_auto_connect)) == 1);
				setAutoScroll((int)jsonObject.get(getString(R.string.title_auto_scroll)) == 1);
				//setAutoTest(jsonObject.get(getString(R.string.title_auto_send_test)) == 1);
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
		if (mBleDeviceRssiAdapter != null)
		{
			mBleDeviceRssiAdapter.clear();
		}
	}

	public BluetoothDevice getBluetoothDevice()
	{
		return mBluetoothDevice;
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

	private void initPreferredDevice(final String address)
	{
		if(mBluetoothAdapter == null || address.length() == 0)
		{
			mBluetoothDevice = null;
		}
		else
		{
			mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(address);
		}
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

						// After init finished, must remove timeout task
						removeTaskTimeout(); // Remove connect timeout runnable

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

	public boolean isInProgramMode()
	{
		return mProgramModeState == ON;
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
	public void initStopScan()
	{
		cancelScanTimer();
		stopScan();
	}

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

		if(mInputDeviceProfile == null)
		{
			getProfileProxy();
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

	private boolean isBluetoothAvailable()
	{
		// Check bluetooth adapter
		if(mBluetoothAdapter == null)
		{
			LogUtil.e(TAG, "[Error] mBluetoothAdapter is null.  Unable to connect.",Thread.currentThread().getStackTrace());
			return false;
		}

		if(!mBluetoothAdapter.isEnabled())
		{
			LogUtil.e(TAG, "[Error] mBluetoothAdapter is not enabled.  Unable to connect.",Thread.currentThread().getStackTrace());
			return false;
		}

		if (mBluetoothDevice == null)
		{
			LogUtil.e(TAG, "[Error] mBluetoothDevice not found.  Unable to connect.",Thread.currentThread().getStackTrace());
			return false;
		}

		return true;
	}

	public int getConnectionState()
	{
		return mConnectionState;
	}

	// init a connection
	// Maybe a Connect or a Bonding, called by TabActivity
	public void initConnectDevice(final String address)
	{
		// init mBluetoothDevice , get device from adapter
		initPreferredDevice(address);

		// Check bluetooth adapter
		if(!isBluetoothAvailable())
		{
			return;
		}

		if(address == null || address.length() == 0)
		{
			return;
		}

		if(mBluetoothDevice.getBondState() == BluetoothDevice.BOND_BONDED)
		{
			connectDevice(address);
		}
		else
		{
			bondingDevice(address);
		}
	}

	// Connection with address
	public void connectDevice(final String address)
	{
		LogUtil.d(TAG,"[Process] connectDevice address : "+address,Thread.currentThread().getStackTrace());

		setAllowConnect(true); //connectDevice

		// init mBluetoothDevice , get device from adapter
		initPreferredDevice(address);

		// Check bluetooth adapter
		if(!isBluetoothAvailable())
		{
			return;
		}

		if(address == null || address.length() == 0)
		{
			return;
		}

		// If paired with APP, start _connect or _bind
		// Connected with OS
		if(isBluetoothDeviceConnected(address))
		{
			// Cached with App
			if(isBluetoothDeviceCached(address))
			{
				LogUtil.d(getPackageName(),"Device cached",Thread.currentThread().getStackTrace());
				setupBluetoothDeviceFromCache(address);
				return ;
			}
			else
			{
				LogUtil.d(getPackageName(),"Device connected not cached with app, start binding ",Thread.currentThread().getStackTrace());

				// Connected,not cached, start bind it with app
				_bind(); //connectDevice
			}
		}
		// Not connected
		else
		{
			LogUtil.d(getPackageName(),"Not connected with OS",Thread.currentThread().getStackTrace());

			// start connect
			_connect(); //connectDevice
		}
	}

	public void bondingDevice(final String address)
	{
		initPreferredDevice(address);

		setAllowConnect(true); //bondingDevice

		if(!isBluetoothAvailable())
		{
			return;
		}

		if(mBluetoothDevice.getBondState() != BluetoothDevice.BOND_NONE)
		{
			return;
		}

		_bondingDevice();
	}

	// Bond function
	private void _bondingDevice()
	{
		mDeviceInitState = INIT_STATE_BONDING;
		broadcastNotifyUi(getConnectStatusIntent(CONNECTION_STATE_BONDING));
		setupTaskTimeout(TASK_BOND,30*1000);

		try
		{
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
			{
				mBluetoothDevice.createBond();
			}
			else
			{
				final Method method = mBluetoothDevice.getClass().getMethod("createBond", (Class[]) null);
				method.invoke(mBluetoothDevice, (Object[]) null);
			}
		}
		catch (Exception e)
		{
			Toast.makeText(context,"Can not bond device",Toast.LENGTH_SHORT).show();
			removeTaskTimeout();
			Log.e(TAG, e.getMessage());
		}}

	// Keyless
	// Connect function
	private void _connect()
	{
		LogUtil.d(TAG, "[Process] connect",Thread.currentThread().getStackTrace());
		try
		{
			if(!isBluetoothAvailable())
			{
				return;
			}

			if(mDeviceInitState != INIT_STATE_NONE && mDeviceInitState != INIT_STATE_BOND_OK)
			{
				LogUtil.d(TAG, "DeviceInitState not at NONE",Thread.currentThread().getStackTrace());
				return;
			}

			if(mThreadConnect != null)
			{
				LogUtil.d(TAG, "Connect thread is running, skip",Thread.currentThread().getStackTrace());
				return;
			}

			broadcastNotifyUi(getConnectStatusIntent(CONNECTION_STATE_CONNECTING));

			int delayTime = 0;
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
							&& !isBluetoothDeviceConnected(address)      										// Device not connected
							&& (mConnectTimeLimit<0 || tryTimes<mConnectTimeLimit) )						// No limit or times under limit
					{
						int delayTime = KEYLESS_CONNECT_INTERVAL;
						try
						{
							// Set a delay to start connecting after IvBT disconnected
							final long current = System.currentTimeMillis();
							if(current-mDisconnectTimeStamp < 3*1000)
							{
								LogUtil.d(TAG, String.format("mDisconnectTimeStamp = %d, current = %d, difference = %d",mDisconnectTimeStamp,current,current-mDisconnectTimeStamp),Thread.currentThread().getStackTrace());
								Thread.sleep(1000);
								continue;
							}

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

								if(!mScanning)
								{
									clearAllDevice();
									mHandler.post(new Runnable()
									{
										@Override
										public void run()
										{
											startScan();
										}
									});


									Thread.sleep(1000);

									mHandler.post(new Runnable()
									{
										@Override
										public void run()
										{
											stopScan();
										}
									});

									for(int i=0 ;i<mBleDeviceRssiAdapter.getCount(); i++)
									{
										final BleDeviceRssi deviceRssi = mBleDeviceRssiAdapter.getDevice(i);
										if(deviceRssi.bluetoothDevice == null)
											continue;

										if(mBluetoothDevice == null)
											break;

										LogUtil.d(TAG,deviceRssi.bluetoothDevice.getAddress(),Thread.currentThread().getStackTrace());
										if(deviceRssi.bluetoothDevice.getAddress().equals(mBluetoothDevice.getAddress()))
										{
											createBTConnection();
											delayTime = delayTime * 2;
											break;
										}
									}
								}
							}
							// Device un-bond
							else
							{
								LogUtil.d(getPackageName(),"Device not bonded!! It was already un-bond.",Thread.currentThread().getStackTrace());
								break;
							}
							Thread.sleep(delayTime);
						}
						catch (InterruptedException e)
						{
							gotException = true;
							e.printStackTrace();
						}
					}
					mThreadConnect = null;
					LogUtil.d(getPackageName(),"Leaving _connect loop.",Thread.currentThread().getStackTrace());

					if(getConnectedBluetoothDevice(address) != null && getConnectionState() == BluetoothProfile.STATE_DISCONNECTED && (mDeviceInitState == INIT_STATE_NONE || mDeviceInitState == INIT_STATE_BOND_OK))
					{
						_bind(); //_Connect
					}
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

	public void stopConnectThread()
	{
		if(mThreadConnect != null)
		{
			mThreadConnect.interrupt();
		}
	}


	private void _bind()
	{
		LogUtil.d(TAG, "[Process] _bind",Thread.currentThread().getStackTrace());
		try
		{
			if(!isBluetoothAvailable())
			{
				return;
			}

			if(!isBluetoothDeviceConnected(mBluetoothDevice.getAddress()))
			{
				LogUtil.e(TAG,"Device not connected, skip binding",Thread.currentThread().getStackTrace());
				if(mThreadConnect == null)
				{
					_connect(); //_bind
				}
				return;
			}

			if(isBluetoothDeviceCached(mBluetoothDevice.getAddress()))
			{
				LogUtil.e(TAG,"Device is cached, skip binding",Thread.currentThread().getStackTrace());
				return;
			}

			final int connectionState = getConnectionState();
			if(connectionState != BluetoothProfile.STATE_DISCONNECTED)
			{
				LogUtil.e(TAG, "Current init state = "+mDeviceInitState,Thread.currentThread().getStackTrace());

				if(connectionState == BluetoothProfile.STATE_CONNECTING )
				{
					LogUtil.e(TAG,"Device was binding, not connected with GATT",Thread.currentThread().getStackTrace());
				}
				else if(connectionState == BluetoothProfile.STATE_CONNECTED)
				{
					LogUtil.e(TAG,String.format("Device was binding, at step %d, skip binding",mDeviceInitState),Thread.currentThread().getStackTrace());
				}
				else if(connectionState == BluetoothProfile.STATE_DISCONNECTING)
				{
					LogUtil.e(TAG, "Disconnecting, wait until disconnected.",Thread.currentThread().getStackTrace());
				}

				return;
			}
			
			if(!mAllowConnect)
			{
				LogUtil.e(TAG, "Not allow binding.",Thread.currentThread().getStackTrace());
				return;
			}

			broadcastNotifyUi(getConnectStatusIntent(CONNECTION_STATE_BINDING));
			mConnectionState = BluetoothProfile.STATE_CONNECTING;
			setupTaskTimeout(TASK_BIND,10*1000);

			// We want to directly connect to the device, so we are setting the autoConnect parameter to false.
			mHandler.post(new Runnable()
			{
				@Override
				public void run()
				{
					setBluetoothGatt(mBluetoothDevice.connectGatt(context, false, mGattCallback));
				}
			});
		}
		catch (Exception e)
		{
			mConnectionState = BluetoothProfile.STATE_DISCONNECTED;
			LogUtil.e(TAG, e.toString(), Thread.currentThread().getStackTrace());
		}
	}

	public void setAllowConnect(final boolean allowConnect)
	{
		mAllowConnect = allowConnect;
	}


	/**
	 * Disconnects an existing connection or cancel a pending connection. The disconnection result
	 * is reported asynchronously through the
	 * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
	 * callback.
	 */
	public void disconnect()
	{
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
			getProfileProxy();
		}
		else
		{
			// Get connected device list and assign
			getConnectedDeviceFromProfile(mInputDeviceProfile);

			if(mBluetoothDevice != null)
			{
				// If input device is not connected,create connection now
				if(mInputDeviceProfile.getConnectionState(mBluetoothDevice) != BluetoothProfile.STATE_CONNECTED)
				{
					createBTConnection(mInputDeviceProfile, mBluetoothDevice);
				}
			}
			return true;
		}

		return false;
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
				disconnect(); // BleService is not null
			}
		}
		catch (Exception e)
		{
			LogUtil.e(TAG, e.toString(),Thread.currentThread().getStackTrace());
		}
	}

	public void handleBleDisconnect()
	{
		try
		{
			if(isInProgramMode())
			{
				mProgramModeState = OFF;
				addTaskToQueue(CMD_PROGRAM_INTERFACE,(byte)PARAM_ASK_LEAVE_PROGRAM_INTERFACE,TIMEOUT_PROGRAM_TASK,genProgramData(-1,0,0,0,null));
			}

			if(mConnectionState == BluetoothProfile.STATE_DISCONNECTED)
			{
				return;
			}

			mConnectionState = BluetoothProfile.STATE_DISCONNECTED;

			removeBluetoothDeviceFromCache(mBluetoothDevice.getAddress());

			// Clear timer when disconnected, avoid re-connect automatically
			removeTaskTimeout(); //handleBleDisconnect

			if(isDeviceInitialized())
				playDisconnectedSound();

			closeGatt();
			cancelRssiTimer();

			broadcastNotifyUi(getGattIntent(PARAM_GATT_DISCONNECTED));

			mDeviceInitState = INIT_STATE_NONE;

			if( !mAllowConnect)
			{
				mHandler.removeCallbacksAndMessages(null);
				LogUtil.d(TAG,"Not allow re-binding",Thread.currentThread().getStackTrace());
				return;
			}

			// Delay 300ms to check ACL state, because GATT state is normal taster than ACL message
			mHandler.postDelayed(new Runnable()
			{
				@Override
				public void run()
				{
					if(isBluetoothDeviceConnected(mBluetoothDevice.getAddress()))
					{
						// Allow to re-connect under limit
						if(mBluetoothDevice != null && mBindRetry++ < TASK_BIND_RETRY_MAX)
						{
							mHandler.postDelayed(new Runnable()
							{
								@Override
								public void run()
								{
									LogUtil.d(TAG, "Re-binding, retry:" + mBindRetry, Thread.currentThread().getStackTrace());
									_bind(); //handleBleDisconnect
								}
							}, 2000);
						}
					}
					// Do not call bond from bind function
//					else
//					{
//						mHandler.postDelayed(new Runnable()
//						{
//							@Override
//							public void run()
//							{
//								LogUtil.d(TAG,"Re-connect, retry:"+mBindRetry,Thread.currentThread().getStackTrace());
//								_connect(); //handleBleDisconnect
//							}
//						},2000);
//					}
				}
			},300);
		}
		catch (Exception e)
		{
			LogUtil.e(TAG, e.toString(),Thread.currentThread().getStackTrace());
		}
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
										disconnect(); //mBluetoothGatt is null
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
		final int cstaLength = (receiveData[FIELD_PARAMETER]>>4) & 0x0F;
		final int cstaSequence = receiveData[FIELD_PARAMETER] & 0x0F;

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

			if(mSlbleCommand != null && mSlbleCommand.packetId == (receiveData[FIELD_ID] & 0xFF))
			{
				stopCommandThread();
				mSlbleCommand.data = byteArrayToLong(subByteArray(mReceivedDataBuffer,0,8));
				LogUtil.d(TAG,String.format("notifyCommandState - CSTA"),Thread.currentThread().getStackTrace());
				notifyCommandState(mSlbleCommand); // CSTA
				mSlbleCommand = null;
			}
			else
			{
				// Update csta in DeviceStatusFragment
				broadcastNotifyUi(getCstaIntent(CSTA_STA,subByteArray(mReceivedDataBuffer,0,8)));
			}

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

			if(mAutoTest)
			{
				if(mReceivedData[FIELD_COMMAND] == CMD_BLE_DEBUG && mReceivedData[FIELD_PARAMETER] == 0x0)
				{
					// Must remove timeout task to avoid auto disconnect
					removeTaskTimeout(); // Remove connect timeout runnable

					// Notify to update UI
					broadcastNotifyUi(getProcessStepIntent(INIT_STATE_TEST_SUCCESS));

					mDeviceInitState = INIT_STATE_END;

					// After connecting and verified, add device to cache list
					addBluetoothDeviceToCache(mBluetoothDevice,getBluetoothGatt());
					return;
				}
			}


			// If check sum is incorrect , restart the process flow from ready
			if(isCheckSumOk(receiveData) == false)
			{
				LogUtil.d(TAG, "[Process] Check sum error",Thread.currentThread().getStackTrace());
				appendLog(formatErrorString("Check sum error"));

				sendErrorMessage(false,PARAM_MESSAGE_CHECKSUM_ERROR);
				return;
			}

			// If receive error message ,
			if(receiveData[FIELD_COMMAND] == CMD_ERROR_MESSAGE)
			{
				LogUtil.d(TAG, "[Process] Receive ERROR_MESSAGE",Thread.currentThread().getStackTrace());
				appendLog(formatErrorString("Receive ERROR_MESSAGE : 0x" + String.format("%02X", receiveData[FIELD_PARAMETER])));

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
			final int command = receiveData[FIELD_COMMAND] & 0xFF;
			switch (receiveData[FIELD_COMMAND])
			{
				case CMD_ACK:
				{
					final String s = formatByteArrayToLog(receiveData);
					LogUtil.d(TAG,"[Process] Receive Ack : " + s,Thread.currentThread().getStackTrace());
					final int ackFlag = getAckFlag();
					if(ackFlag>0)
					{
						if((ackFlag & ACK_FLAG_TX_POWER) >0)
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

					if(mSlbleCommand != null)
					{
						final int parameter = receiveData[FIELD_PARAMETER] & 0xFF;
						LogUtil.d(TAG,String.format("notifyCommandState - STATE"),Thread.currentThread().getStackTrace());

						switch (parameter)
						{
							case PARAM_ACCEPT_COMMAND:
							case PARAM_COMMAND_PROCESSING:
							case PARAM_REJECT_COMMAND:
							{
								mSlbleCommand.state = parameter;
								stopCommandThread();
								notifyCommandState(mSlbleCommand); // STATE change
								break;
							}
							default:
						}
					}
				}
				break;
				case CMD_CAR_STATUS:
				{
					if(isInProgramMode())
					{
						clearTaskQueue();

						addTaskToQueue(CMD_PROGRAM_INTERFACE,(byte)PARAM_ASK_LEAVE_PROGRAM_INTERFACE,TIMEOUT_PROGRAM_TASK,genProgramData(-1,0,0,0,null));
					}

					handleReceiveCsta(receiveData,false);

					initDeviceState(INIT_STATE_END); // Receive CSTA
				}
				break;

				case CMD_CHECK_CONNECTION:
				{
					final byte[] writeData = new byte[PACKET_LENGTH];
					writeData[FIELD_ID] = receiveData[FIELD_ID];
					writeData[FIELD_COMMAND] = receiveData[FIELD_COMMAND];

					final String s = formatByteArrayToLog(receiveData);
					LogUtil.d(TAG, "Receive Ping " + s,Thread.currentThread().getStackTrace());
					appendLog(formatReceiveString("Receive Ping"));
					sendCheckConnectionAck(true);
				}
				break;

				case CMD_SETTING_INFORMATION:
				{
					// Receive response from device
					if(receiveData[FIELD_PARAMETER] == PARAM_SETTING_RESPONSE)
					{
						final byte[] data = subByteArray(receiveData,3,16);
						loadBleSetting(data);
						saveBleSetting(data);


						final Message message = new Message();
						message.arg1 = PARAM_SETTING_INFORMATION;
						message.arg2 = (int)receiveData[FIELD_PARAMETER];
						message.obj = data;
						sendCallbackMessage(message);
					}
					// Receive response from device, check if data is match we sent before
					else if(receiveData[FIELD_PARAMETER] == PARAM_SETTING_WRITE)
					{
						final byte[] data = subByteArray(receiveData,3,16);

						final Message message = new Message();
						message.arg1 = PARAM_SETTING_INFORMATION;
						message.arg2 = (int)receiveData[FIELD_PARAMETER];
						message.obj = data;
						sendCallbackMessage(message);
					}
				}
				break;

				case CMD_PROGRAM_INTERFACE:
				{
					if(receiveData[FIELD_PARAMETER] == PARAM_BLE_INTO_PROGRAM_INTERFACE)
					{
						mProgramModeState = ON;
						final byte[] longBytes = new byte[8];
						longBytes[0] = receiveData[3];
						longBytes[1] = receiveData[4];
						longBytes[2] = receiveData[5];
						longBytes[3] = receiveData[6];
						mPrbsKey = byteArrayToLong(longBytes,ByteOrder.LITTLE_ENDIAN);
						mPrbsJump = receiveData[7] & 0xFF;

						appendLog("PRBS Key : " + NEW_LINE_CHARACTER + formatByteArrayToLog(longToBytes(mPrbsKey,ByteOrder.BIG_ENDIAN)));
						appendLog("PRBS Jump : " + String.format("%02X",mPrbsJump));
						LogUtil.d(TAG,"PRBS Key : " + formatByteArrayToLog(longToBytes(mPrbsKey,ByteOrder.BIG_ENDIAN)),Thread.currentThread().getStackTrace());
						LogUtil.d(TAG,"PRBS Jump : " + String.format("%02X",mPrbsJump),Thread.currentThread().getStackTrace());
						if(mCurrentTask != null && mCurrentTask.taskCommand == CMD_PROGRAM_INTERFACE && mCurrentTask.taskParameter == PARAM_ASK_INTO_PROGRAM_INTERFACE)
						{
							final ProgramData programData = (ProgramData)(mCurrentTask.taskData);
							if(programData != null)
							{
								programData.dataCount = 1;
							}
							mCurrentTask.state = TaskObject.STATE_IDLE;
						}
					}
					else if(receiveData[FIELD_PARAMETER] == PARAM_BLE_LEAVE_PROGRAM_INTERFACE)
					{
						// APP ask to leave programming mode
						if(mCurrentTask != null && mCurrentTask.taskCommand == CMD_PROGRAM_INTERFACE && mCurrentTask.taskParameter == PARAM_ASK_LEAVE_PROGRAM_INTERFACE)
						{
							final ProgramData programData = (ProgramData)(mCurrentTask.taskData);
							if(programData != null)
							{
								programData.dataCount = 1;
							}
							mCurrentTask.state = TaskObject.STATE_IDLE;
						}
						// CarAlarm leave programming mode active
						else
						{
							if(isInProgramMode())
							{
								clearTaskQueue();

								addTaskToQueue(CMD_PROGRAM_INTERFACE,(byte)PARAM_ASK_LEAVE_PROGRAM_INTERFACE,TIMEOUT_PROGRAM_TASK,genProgramData(-1,0,0,0,null));
							}
						}

						mProgramModeState = OFF;
						mPrbsKey = 0;
						mPrbsJump = 0;
					}
				}
				break;

				case CMD_PROGRAM_DATA:
				{
					if(mCurrentTask == null)
					{
						return;
					}

					if(mCurrentTask.taskCommand != CMD_PROGRAM_DATA)
					{
						return;
					}

					final ProgramData programData = (ProgramData)mCurrentTask.taskData;
					if(programData == null)
					{
						return;
					}

					// Reply READ data
					if(receiveData[FIELD_PARAMETER] == PARAM_TX_REPLY_DEAD_DATA)
					{
						// Check address
						if(programData.subAddressHigh == receiveData[FIELD_PROGRAM_ADDRESS_HIGH_BYTE] && programData.subAddressLow == receiveData[FIELD_PROGRAM_ADDRESS_LOW_BYTE])
						{
							final byte[] data = subByteArray(receiveData,FIELD_PROGRAM_DATA,receiveData[FIELD_PROGRAM_DATA_LENGTH]);
							appendLog("Decode data : " + NEW_LINE_CHARACTER + formatByteArrayToLog(data));
							prbsEncodeDecode(data,data.length);
							appendLog("-->" + formatByteArrayToLog(data));
							fillData(programData.dataBuffer,programData.dataCount,data);
							mCurrentTask.state = TaskObject.STATE_IDLE;
							programData.dataCount = programData.dataCount + data.length;
						}
					}
					// Reply WRITE data
					else if(receiveData[FIELD_PARAMETER] == PARAM_TX_REPLY_WRITE_DATA)
					{
						mCurrentTask.state = TaskObject.STATE_IDLE;
						programData.dataCount = programData.dataCount + PACKET_PROGRAM_DATA_LENGTH;
						if(programData.dataCount > programData.dataLength)
						{
							programData.dataCount = programData.dataLength;
						}
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
		writeData[FIELD_ID] = getRandom(255);
		writeData[FIELD_COMMAND] = CMD_SETTING_INFORMATION;
		writeData[FIELD_PARAMETER] = PARAM_SETTING_REQUEST;
		writeData[FIELD_CHECK_SUM] = getCheckSum(writeData);

		sendPlainData(writeData);
	}

	private void sendErrorMessage(final boolean keepRandom,final int errorCode)
	{
		LogUtil.d(TAG, "[Error] Send error message:" + errorCode,Thread.currentThread().getStackTrace());
		appendLog(formatSendString("Send error message : " + errorCode));

		final byte[] writeData = new byte[PACKET_LENGTH];
		if(keepRandom)
			writeData[FIELD_ID] = mReceivedData[FIELD_ID];
		else
			writeData[FIELD_ID] = getRandom(255);

		writeData[FIELD_COMMAND] = CMD_ERROR_MESSAGE;
		writeData[FIELD_PARAMETER] = (byte)errorCode;
		writeData[FIELD_CHECK_SUM] = getCheckSum(writeData);
		sendPlainData(writeData);
	}

	private void sendAck(final boolean keepRandom)
	{
		appendLog(formatSendString("Send ACK"));
		LogUtil.d(TAG, "[Process] Send ACK",Thread.currentThread().getStackTrace());

		final byte[] writeData = new byte[PACKET_LENGTH];
		if(keepRandom)
			writeData[FIELD_ID] = mReceivedData[FIELD_ID];
		else
			writeData[FIELD_ID] = getRandom(255);

		writeData[FIELD_COMMAND] = CMD_ACK;
		writeData[FIELD_PARAMETER] = (byte)PARAM_NONE;
		writeData[FIELD_CHECK_SUM] = getCheckSum(writeData);


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
		writeData[FIELD_ID] = getRandom(255);
		writeData[FIELD_COMMAND] = CMD_CAR_STATUS;
		writeData[FIELD_PARAMETER] = (byte)PARAM_NONE;
		writeData[FIELD_CHECK_SUM] = getCheckSum(writeData);
		sendPlainData(writeData);
	}

	private void sendCheckConnectionAck(final boolean keepId)
	{
		LogUtil.d(TAG, "[Process] Send CheckConnection ACK",Thread.currentThread().getStackTrace());
		appendLog(formatSendString("Send CheckConnection ACK"));

		final byte[] writeData = new byte[PACKET_LENGTH];
		if(keepId)
			writeData[FIELD_ID] = mReceivedData[FIELD_ID];
		else
			writeData[FIELD_ID] = getRandom(255);
		writeData[FIELD_COMMAND] = CMD_CHECK_CONNECTION;
		writeData[FIELD_PARAMETER] = (byte)PARAM_NONE;
		writeData[FIELD_CHECK_SUM] = getCheckSum(writeData);

		sendPlainData(writeData);
	}

	public void startCommandThread(final int cmd,final int resend_interval,final int times)
	{
		if(mThreadSendCommand != null)
			return;

		mSlbleCommand = new SlbleCommand();
		mSlbleCommand.command = cmd;
		mSlbleCommand.resend_interval = resend_interval;
		mSlbleCommand.maxTimes = times;
		mSlbleCommand.currentTimes = -1;
		mSlbleCommand.state = -1;
		mThreadSendCommand = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				while(mSlbleCommand.currentTimes < times)
				{
					try
					{
						if((System.currentTimeMillis() - mSlbleCommand.updateTime) >= resend_interval)
						{
							// Re-send
							if(mSlbleCommand.currentTimes <= times)
							{
								LogUtil.d(TAG,String.format("notifyCommandState - send command %d",cmd),Thread.currentThread().getStackTrace());
								sendCommand(cmd);
								mSlbleCommand.currentTimes++;
								mSlbleCommand.updateTime = System.currentTimeMillis();
							}
							// Reach MAX re-send
							else
							{
								// if re-send reach max and not receive ACK, notify command fail
								if(mSlbleCommand != null && mSlbleCommand.currentTimes == times && mSlbleCommand.data == null)
								{
									LogUtil.d(TAG,String.format("notifyCommandState - THREAD"),Thread.currentThread().getStackTrace());
									notifyCommandState(mSlbleCommand); // Command timeout
								}
								break;
							}
						}

						Thread.sleep(50);
					}
					catch (Exception e)
					{
						e.printStackTrace();
						break;
					}
				}

				mThreadSendCommand = null;
			}
		});

		mThreadSendCommand.start();
	}

	public void stopCommandThread()
	{
		if(mThreadSendCommand != null)
		{
			mThreadSendCommand.interrupt();
		}
	}

	public void sendCommand(final int cmd)
	{
		try
		{
			if(getConnectionState() != BluetoothProfile.STATE_CONNECTED) return;

			final byte[] writeData = new byte[PACKET_LENGTH];
			writeData[FIELD_ID] = getRandom(255);
			writeData[FIELD_COMMAND] = CMD_PHONE_CONTROL_COMMAND;
			writeData[FIELD_PARAMETER] = (byte)PARAM_NONE;

			final byte[] data = generateRandomArray(16,1);
			data[0] = (byte)cmd;
			fillData(writeData,data);
			if(mSlbleCommand != null)
			{
				mSlbleCommand.packetId = writeData[FIELD_ID] & 0xFF;
			}

			// fill check sum
			writeData[FIELD_CHECK_SUM] = getCheckSum(writeData);


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

	public void sendDebugCommand(final int cmd)
	{
		try
		{
//            if(getConnectionState() != BluetoothProfile.STATE_CONNECTED) return;

			final byte[] writeData = new byte[PACKET_LENGTH];
			writeData[FIELD_ID] = getRandom(255);
			writeData[FIELD_COMMAND] = CMD_BLE_DEBUG;
			writeData[FIELD_PARAMETER] = (byte)PARAM_NONE;

			final byte[] data = generateRandomArray(16,1);
			data[0] = (byte)cmd;
			fillData(writeData,data);

			// fill check sum
			writeData[FIELD_CHECK_SUM] = getCheckSum(writeData);


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
		writeData[FIELD_ID] = getRandom(255);
		writeData[FIELD_COMMAND] = CMD_TX_POWER;
		writeData[FIELD_PARAMETER] = (byte)param;

		final byte[] data = new byte[16];
		data[0] = (byte)level;
		fillData(writeData,data);

		// fill check sum
		writeData[FIELD_CHECK_SUM] = getCheckSum(writeData);


		final String s = formatByteArrayToLog(writeData);
		LogUtil.d(TAG, " Send TxPower  : " + s,Thread.currentThread().getStackTrace());
		appendLog(formatSendString("Send TxPower  : " + String.format("0x%02X", level)));

		sendPlainData(writeData);
	}

	public void sendSettingInformation(final byte[] data)
	{
		if(getConnectionState() != BluetoothProfile.STATE_CONNECTED) return;

		final byte[] writeData = new byte[PACKET_LENGTH];
		writeData[FIELD_ID] = getRandom(255);
		writeData[FIELD_COMMAND] = CMD_SETTING_INFORMATION;
		writeData[FIELD_PARAMETER] = (byte)PARAM_SETTING_WRITE;

		fillData(writeData,data);

		// fill check sum
		writeData[FIELD_CHECK_SUM] = getCheckSum(writeData);


		final String s = formatByteArrayToLog(writeData);
		LogUtil.d(TAG, " Send SettingInformation  : " + s,Thread.currentThread().getStackTrace());
		appendLog(formatSendString("Send SettingInformation  : "+ NEW_LINE_CHARACTER + s));

		sendPlainData(writeData);
	}

	private void clearTaskQueue()
	{
		mQueueStartIndex = 0;
		mQueueEndIndex = 0;
	}

	private boolean isTaskQueueFull()
	{
		if(mQueueStartIndex == (mQueueEndIndex+1)%mTaskQueue.length)
		{
			LogUtil.d(TAG,"Queue is full", Thread.currentThread().getStackTrace());
			return true;
		}

		return false;
	}

	private void startProgramThread()
	{
		if(mThreadProgram != null)
		{
			return;
		}

		mThreadProgram = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				boolean getException = false;

//				// Check and into program mode at thread started
//				if(!isInProgramMode())
//				{
//					mCurrentTask  = new TaskObject();
//					mCurrentTask.taskAction = TaskObject.ACTION_INTO_PROGRAM_MODE;
//					mCurrentTask.state = TaskObject.STATE_WAIT;
//					mCurrentTask.updateTime = System.currentTimeMillis();
//
//					sendIntoProgramMode();
//				}

				while(!getException)
				{
					try
					{
						// Pop task from queue
						if(mCurrentTask == null)
						{
							mCurrentTask = getTaskFromQueue();
						}

						if(mCurrentTask == null)
						{
							throw new Exception("No task to be executed");
						}

//						if(!isDeviceInitialized())
//						{
//							Toast.makeText(context,"Device is disconnected.",Toast.LENGTH_SHORT).show();
//
//							throw new Exception("Disconnected");
//						}

						if(!isInProgramMode())
						{
							if(mCurrentTask.taskCommand == CMD_PROGRAM_DATA)
							{
								notifyProgramResult(mCurrentTask);
								mCurrentTask = null;
								LogUtil.d(TAG,"Not in program mode , skip action",Thread.currentThread().getStackTrace());
								continue;
							}
						}

						// Wait response
						if(mCurrentTask.state == TaskObject.STATE_WAIT)
						{
							// Timeout
							if(System.currentTimeMillis() > mCurrentTask.updateTime + mCurrentTask.timeout)
							{
								LogUtil.d(TAG,String.format("Command  %d timeout",mCurrentTask.taskCommand),Thread.currentThread().getStackTrace());
								notifyProgramResult(mCurrentTask);
								mCurrentTask = null;
							}
						}
						// Idle
						else
						{
							if(mCurrentTask.taskCommand == CMD_PROGRAM_INTERFACE || mCurrentTask.taskCommand == CMD_PROGRAM_DATA)
							{
								final ProgramData programData = (ProgramData)mCurrentTask.taskData;
								if(programData == null)
								{
									notifyProgramResult(mCurrentTask);
									mCurrentTask = null;
									LogUtil.d(TAG,"Task has no data , skip action",Thread.currentThread().getStackTrace());
									continue;
								}

								// Task finish
								if(programData.dataCount == programData.dataLength)
								{
									if(mCurrentTask.taskCommand == CMD_PROGRAM_INTERFACE)
									{
										// Into Program mode
										if(mCurrentTask.taskParameter == PARAM_ASK_INTO_PROGRAM_INTERFACE)
										{
											LogUtil.d(TAG,"Into Program mode.",Thread.currentThread().getStackTrace());
											appendLog("Into Program mode");
										}
										// Exit Program mode
										else if(mCurrentTask.taskParameter == PARAM_ASK_LEAVE_PROGRAM_INTERFACE)
										{
											LogUtil.d(TAG,"Exit Program mode.",Thread.currentThread().getStackTrace());
											appendLog("Exit Program mode");
										}
									}
									else
									{
										final String s = formatByteArrayToLog(programData.dataBuffer);

										// Read
										if(mCurrentTask.taskParameter == PARAM_READ_PROGRAM_DATA)
										{
											LogUtil.d(TAG,"Read finish : " + s,Thread.currentThread().getStackTrace());
											appendLog(String.format("Read 0x%02X 0x%02X",programData.addressHigh & 0xFF,programData.addressLow & 0xFF) + NEW_LINE_CHARACTER + s);
										}
										// Write
										else if(mCurrentTask.taskParameter == PARAM_WRITE_PROGRAM_DATA)
										{
											LogUtil.d(TAG,"Write finish: " + s,Thread.currentThread().getStackTrace());
											appendLog(String.format("Write 0x%02X 0x%02X",programData.addressHigh & 0xFF,programData.addressLow & 0xFF) + NEW_LINE_CHARACTER + s);
										}
									}

									notifyProgramResult(mCurrentTask);
									mCurrentTask = null;
								}
								else
								{
									final byte[] add = calculateOffsetAddress(programData.addressHigh, programData.addressLow, programData.dataCount);
									final int length = (programData.dataLength - programData.dataCount) > PACKET_PROGRAM_DATA_LENGTH ? PACKET_PROGRAM_DATA_LENGTH : programData.dataLength - programData.dataCount;
									programData.subAddressHigh = add[0];
									programData.subAddressLow = add[1];

									if(mCurrentTask.taskCommand == CMD_PROGRAM_INTERFACE)
									{
										// Into Program mode
										if(mCurrentTask.taskParameter == PARAM_ASK_INTO_PROGRAM_INTERFACE)
										{
											sendIntoProgramMode();
										}
										// Exit Program mode
										else if(mCurrentTask.taskParameter == PARAM_ASK_LEAVE_PROGRAM_INTERFACE)
										{
											sendExitProgramMode();
										}
									}
									else
									{
										// Read
										if(mCurrentTask.taskParameter == PARAM_READ_PROGRAM_DATA)
										{
											sendProgramTablePacket(PARAM_READ_PROGRAM_DATA,add[0],add[1],length,null);
										}
										// Write
										else if(mCurrentTask.taskParameter == PARAM_WRITE_PROGRAM_DATA)
										{
											final byte[] data = subByteArray(programData.dataBuffer,programData.dataCount,length);
											prbsEncodeDecode(data,data.length);
											sendProgramTablePacket(PARAM_WRITE_PROGRAM_DATA,add[0],add[1],length,data);
										}
									}

									mCurrentTask.state = TaskObject.STATE_WAIT;
									mCurrentTask.updateTime = System.currentTimeMillis();
								}
							}
						}

						Thread.sleep(200);
					}
					catch (Exception e)
					{
						mCurrentTask = null;
						clearTaskQueue();
						LogUtil.e(TAG,e.toString(),Thread.currentThread().getStackTrace());
						break;
					}
				}

				LogUtil.d(TAG,"Exit Program thread loop.",Thread.currentThread().getStackTrace());
				mThreadProgram = null;
				notifyProgramTaskFinish();
			}
		});

		mThreadProgram.start();
	}

	private void prbsEncodeDecode(final byte[] data, final int dataLength)
	{
		long localKey = mPrbsKey;

		appendLog("Init PRBS KEY = " + formatByteArrayToLog(longToBytes(localKey,ByteOrder.BIG_ENDIAN)) + ",Jump = " + mPrbsJump + "Length = " + dataLength);
		LogUtil.d(TAG,"Init PRBS KEY = " + formatByteArrayToLog(longToBytes(localKey,ByteOrder.BIG_ENDIAN)) + ",Jump = " + mPrbsJump + "Length = " + dataLength, Thread.currentThread().getStackTrace());
		for(int i=0;i<dataLength;i++)
		{
			data[i] = (byte)(data[i] ^ (localKey & 0xFF));
			//appendLog(String.format("Data[%02d]  = %02X",i,data[i]));
			//LogUtil.d(TAG,String.format("Data[%02d]  = %02X",i,data[i]), Thread.currentThread().getStackTrace());

			// change the key according to the specified jump amount
			for(int j=0;j<mPrbsJump;j++)
			{
				// if the last bit will effect the feed back
				if((localKey & 0x00000001) > 0)
				{
					//calculate new key
					localKey = ((localKey>>1)^(PRBS_FEEDBACK_TABLE[0]));
				}
				//otherwise just shift the key
				else
				{
					localKey = localKey>>1;
				}

				localKey = localKey & 0x00000000FFFFFFFFL;
				//appendLog(String.format("[%02d] Local KEY = ",j) + formatByteArrayToLog(longToBytes(localKey,ByteOrder.BIG_ENDIAN)));
				//LogUtil.d(TAG,String.format("Local KEY[%d][%d] = %s",i,j, formatByteArrayToLog(longToBytes(localKey,ByteOrder.BIG_ENDIAN))), Thread.currentThread().getStackTrace());
			}
		}
	}

	private byte[] calculateOffsetAddress(final byte addHigh,final byte addLow,final int length)
	{
		final byte[] add = new byte[2];
		int iAddHigh = (int)addHigh;
		int iAddLow = (int)addLow;

		iAddLow = iAddLow + length;
		if(iAddLow > 255)
		{
			iAddHigh = iAddHigh + 1;
		}

		add[0] = (byte)iAddHigh;
		add[1] = (byte)iAddLow;

		return add;
	}

	private ProgramData genProgramData(final int id,final int addHigh,final int addLow,final int length,final byte[] data)
	{
		final ProgramData programData = new ProgramData();
		programData.dataId = id;
		programData.addressHigh = (byte)addHigh;
		programData.addressLow = (byte)addLow;
		programData.dataLength = length;
		programData.dataCount = 0;
		if(data == null)
		{
			programData.dataBuffer = new byte[length];
		}
		else
		{
			programData.dataBuffer = data;
		}

		return programData;
	}

	private void addTaskToQueue(final byte cmd,final byte parameter,final long timeout,final Object data)
	{
		final TaskObject taskObject = new TaskObject();
		taskObject.taskCommand = cmd;
		taskObject.taskParameter = parameter;
		taskObject.timeout = timeout;
		taskObject.taskData = data;

		mQueueEndIndex = (mQueueEndIndex + 1) % mTaskQueue.length;
		mTaskQueue[mQueueEndIndex] = taskObject;

		startProgramThread();
	}

	private TaskObject getTaskFromQueue()
	{
		if(mQueueStartIndex == mQueueEndIndex)
		{
			LogUtil.d(TAG,"Queue is empty", Thread.currentThread().getStackTrace());
			return null;
		}

		mQueueStartIndex = (mQueueStartIndex + 1) % mTaskQueue.length;
		return mTaskQueue[mQueueStartIndex];
	}

	public void readProgramTable(final int id,final int addHigh,final int addLow,final int length)
	{
		if(isTaskQueueFull())
		{
			return;
		}

		addTaskToQueue(CMD_PROGRAM_DATA,(byte)PARAM_READ_PROGRAM_DATA,TIMEOUT_PROGRAM_TASK,genProgramData(id,addHigh,addLow,length,null));
	}

	public void writeProgramTable(final int id,final int addHigh,final int addLow,final byte[] data)
	{
		if(isTaskQueueFull())
		{
			return;
		}

		final String s = formatByteArrayToLog(data);
		LogUtil.d(TAG, "WriteProgramTable  : " + s,Thread.currentThread().getStackTrace());
		appendLog(formatSendString("WriteProgramTable  : "+ NEW_LINE_CHARACTER + s));


		addTaskToQueue(CMD_PROGRAM_DATA,(byte)PARAM_WRITE_PROGRAM_DATA,TIMEOUT_PROGRAM_TASK,genProgramData(id,addHigh,addLow,data.length,data));
	}

	public void sendProgramTablePacket(final int parameter,final int addHigh,final int addLow,final int length,final byte[] data)
	{
		if(getConnectionState() != BluetoothProfile.STATE_CONNECTED) return;

		final byte[] writeData = new byte[PACKET_LENGTH];
		writeData[FIELD_ID] = getRandom(255);
		writeData[FIELD_COMMAND] = (byte)CMD_PROGRAM_DATA;
		writeData[FIELD_PARAMETER] = (byte)parameter;
		writeData[FIELD_PROGRAM_ADDRESS_HIGH_BYTE] = (byte)addHigh;
		writeData[FIELD_PROGRAM_ADDRESS_LOW_BYTE] = (byte)addLow;
		writeData[FIELD_PROGRAM_DATA_LENGTH] = (byte)length;


		// Write
		if(parameter == PARAM_WRITE_PROGRAM_DATA)
		{
			if(data == null)
			{
				return;
			}

			if(length == data.length )
			{
				fillData(writeData,FIELD_PROGRAM_DATA,data);
			}
			else if(length < data.length)
			{
				fillData(writeData,FIELD_PROGRAM_DATA,subByteArray(data,0,length));
			}
			else
			{
				return;
			}
		}

		// fill check sum
		writeData[FIELD_CHECK_SUM] = getCheckSum(writeData);


		final String s = formatByteArrayToLog(writeData);
		LogUtil.d(TAG, "sendProgramTablePacket : "+s,Thread.currentThread().getStackTrace());
		appendLog(formatSendString("sendProgramTablePacket"));

		sendPlainData(writeData);
	}

	public void askIntoProgramMode()
	{
		if(isTaskQueueFull())
		{
			return;
		}

		addTaskToQueue(CMD_PROGRAM_INTERFACE,(byte)PARAM_ASK_INTO_PROGRAM_INTERFACE,TIMEOUT_PROGRAM_TASK,genProgramData(-1,0,0,1,null));
	}

	public void askExitProgramMode()
	{
		clearTaskQueue();

		addTaskToQueue(CMD_PROGRAM_INTERFACE,(byte)PARAM_ASK_LEAVE_PROGRAM_INTERFACE,TIMEOUT_PROGRAM_TASK,genProgramData(-1,0,0,1,null));
	}

	public void sendIntoProgramMode()
	{
		if(getConnectionState() != BluetoothProfile.STATE_CONNECTED) return;

		final byte[] writeData = new byte[PACKET_LENGTH];
		writeData[FIELD_ID] = getRandom(16);
		writeData[FIELD_COMMAND] = (byte)CMD_PROGRAM_INTERFACE;
		writeData[FIELD_PARAMETER] = (byte)PARAM_ASK_INTO_PROGRAM_INTERFACE;


		// fill check sum
		writeData[FIELD_CHECK_SUM] = getCheckSum(writeData);


		final String s = formatByteArrayToLog(writeData);
		LogUtil.d(TAG, "Send Into Program mode : "+s,Thread.currentThread().getStackTrace());
		appendLog(formatSendString("Send Into Program mode"));

		sendPlainData(writeData);
	}

	public void sendExitProgramMode()
	{
		if(getConnectionState() != BluetoothProfile.STATE_CONNECTED) return;

		final byte[] writeData = new byte[PACKET_LENGTH];
		writeData[FIELD_ID] = getRandom(16);
		writeData[FIELD_COMMAND] = (byte)CMD_PROGRAM_INTERFACE;
		writeData[FIELD_PARAMETER] = (byte)PARAM_ASK_LEAVE_PROGRAM_INTERFACE;

		// fill check sum
		writeData[FIELD_CHECK_SUM] = getCheckSum(writeData);


		final String s = formatByteArrayToLog(writeData);
		LogUtil.d(TAG, "Send Exit Program mode : "+s,Thread.currentThread().getStackTrace());
		appendLog(formatSendString("Send Exit Program mode"));

		sendPlainData(writeData);
	}

	private void notifyCommandState(final SlbleCommand slbleCommand)
	{
		try
		{
			final Message message = new Message();
			message.arg1 = IPC_COMMAND_COMMAND_STATE;
			message.obj = (SlbleCommand)slbleCommand.clone();
			sendCallbackMessage(message);

			LogUtil.d(TAG,String.format("ID = %d, Command = %d, State = %d, current = %d, limit = %d",slbleCommand.packetId,slbleCommand.command,slbleCommand.state,slbleCommand.currentTimes,slbleCommand.maxTimes),Thread.currentThread().getStackTrace());
		}
		catch (Exception e)
		{

		}
	}

	private void notifyProgramResult(final TaskObject taskObject)
	{
		final Message message = new Message();
		message.arg1 = IPC_COMMAND_PROGRAM_RESULT;
		message.obj = taskObject;

		sendCallbackMessage(message);
	}

	private void notifyProgramTaskFinish()
	{
		final Message message = new Message();
		message.arg1 = IPC_COMMAND_PROGRAM_TASK_FINISH;

		sendCallbackMessage(message);
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

	private void fillData(final byte[] outputPacket,final int offsetIndex,byte[] data)
	{
		setDataField(outputPacket,offsetIndex,data);
	}

	private void fillData(final byte[] outputPacket,byte[] data)
	{
		setDataField(outputPacket,FIELD_DATA,data);
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

	public byte[] longToBytes(final long x,final ByteOrder order)
	{
		ByteBuffer buffer = ByteBuffer.allocate(Long.SIZE / Byte.SIZE);
		buffer.order(order);
		buffer.putLong(x);
		return buffer.array();
	}

	public int byteArrayToInt(byte[] b,final ByteOrder order)
	{
		if(order == ByteOrder.LITTLE_ENDIAN)
			return   b[3] & 0xFF |
					(b[2] & 0xFF) << 8 |
					(b[1] & 0xFF) << 16 |
					(b[0] & 0xFF) << 24;
		else
			return   b[0] & 0xFF |
					(b[1] & 0xFF) << 8 |
					(b[2] & 0xFF) << 16 |
					(b[3] & 0xFF) << 24;
	}

	public byte[] intToByteArray(int a)
	{
		return new byte[] {
				(byte) ((a >> 24) & 0xFF),
				(byte) ((a >> 16) & 0xFF),
				(byte) ((a >> 8) & 0xFF),
				(byte) (a & 0xFF)
		};
	}

	private long byteArrayToLong(final byte[] array)
	{
		return byteArrayToLong(array,ByteOrder.LITTLE_ENDIAN);
	}

	private long byteArrayToLong(final byte[] array,final ByteOrder order)
	{
		ByteBuffer buffer = ByteBuffer.wrap(array);
//		buffer.order(ByteOrder.BIG_ENDIAN);
//		System.out.println(buffer.getLong());
//		buffer = ByteBuffer.wrap(array);
		buffer.order(order);

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
		getConnectedDeviceFromProfile(mInputDeviceProfile);

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
		}
	}

	private void getConnectedDeviceFromProfile(final BluetoothProfile profile)
	{
		if(profile != null)
		{
			mConnectedDeviceList = getConnectedDevice(profile);
		}
		else
		{
			mConnectedDeviceList = new ArrayList<>();
		}
	}

	public void addConnectedBluetoothDevice(final BluetoothDevice bluetoothDevice)
	{
		getConnectedDeviceFromProfile(mInputDeviceProfile);

		LogUtil.d(TAG,"addConnectedBluetoothDevice , new device : " + bluetoothDevice.getAddress(),Thread.currentThread().getStackTrace());
		if(getConnectedBluetoothDevice(bluetoothDevice.getAddress()) == null)
		{
			mConnectedDeviceList.add(bluetoothDevice);
		}

		// No preferred bluetooth, current selected device haven't paired with IvBT
		if(!isBluetoothAvailable())
		{
			return;
		}

		// Connected device not preferred
		if(!bluetoothDevice.getAddress().equals(mBluetoothDevice.getAddress()))
		{
			return;
		}

		// During bonding process, do not connect , do nothing until bonding finish
		if((mDeviceInitState == INIT_STATE_BONDING
				|| mDeviceInitState == INIT_STATE_BOND_TO_CONNECT)
				&& bluetoothDevice.getBondState() != BluetoothDevice.BOND_BONDED)
		{
			LogUtil.d(TAG,String.format("After bonding, state=%d , wait for disconnect ",mDeviceInitState),Thread.currentThread().getStackTrace());
			return;
		}

		boolean isBondedDevice = false;
		final Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

		for(BluetoothDevice bondDevice : pairedDevices)
		{
			if(bondDevice.getAddress().equals(bluetoothDevice.getAddress()))
			{
				isBondedDevice = true;
				break;
			}
		}


		if(isBondedDevice)
		{
			LogUtil.d(TAG,"addConnectedBluetoothDevice , start binding after 1500ms",Thread.currentThread().getStackTrace());
			mBindRetry = 0;
			mHandler.postDelayed(new Runnable()
			{
				@Override
				public void run()
				{
					_bind(); // AddConnectedBluetoothDevice
				}
			},1500);

			//notifyDeviceListUpdated("connected",mConnectedDeviceList);
		}
		else
		{
			LogUtil.d(TAG,"Device not bonded, skip",Thread.currentThread().getStackTrace());
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

		if(mDeviceInitState == INIT_STATE_BONDING)
		{
			LogUtil.d(TAG,"Disconnect during bonding, bond failed",Thread.currentThread().getStackTrace());
			appendLog("Bonding failed, disconnected");
			removeTaskTimeout();
			return;
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
			//closeBTConnection(bluetoothDevice.getAddress());


			LogUtil.d(TAG,"Disconnect after bonding success, start connect",Thread.currentThread().getStackTrace());
			mHandler.postDelayed(new Runnable()
			{
				@Override
				public void run()
				{
					_connect(); //removeConnectedBluetoothDevice
				}
			},500);

			return;
		}

		if(!mAllowConnect)
		{
			LogUtil.d(TAG,"Block any connection",Thread.currentThread().getStackTrace());
			return;
		}

		if(!mAutoConnectOnDisconnect)
		{
			LogUtil.d(TAG,"Auto connect not enabled",Thread.currentThread().getStackTrace());
			return;
		}

		// Record time stamp on disconnect, we have a 3 seconds delay then allowed to start connecting
		mDisconnectTimeStamp = System.currentTimeMillis();

		LogUtil.d(TAG,"Re-Connect",Thread.currentThread().getStackTrace());
		if(mConnectionState == BluetoothProfile.STATE_DISCONNECTED)
		{
			final PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
			boolean isScreenOn = true;

			if(powerManager != null)
			{
				isScreenOn = powerManager.isScreenOn();
			}
			// If screen is off, request PARTIAL_WAKE_LOCK to keep thread running, and release after 6s
			if(!isScreenOn && powerManager != null)
			{
				final PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WakeForConnect");
				wakeLock.acquire();
				_connect(); //removeConnectedBluetoothDevice
				mHandler.postDelayed(new Runnable()
				{
					@Override
					public void run()
					{
						wakeLock.release();
					}
				},6000);
			}
			else
			{
				_connect(); //removeConnectedBluetoothDevice
			}
		}
		else
		{
			LogUtil.d(TAG,"Gatt is still connected, disconnect first",Thread.currentThread().getStackTrace());
			disconnect();
		}
	}

	public BluetoothDevice getConnectedBluetoothDevice(final String address)
	{
		if(mConnectedDeviceList != null)
		{
			for(int i=0;i<mConnectedDeviceList.size();i++)
			{
				final BluetoothDevice device = mConnectedDeviceList.get(i);
				//LogUtil.d(TAG,"getConnectedBluetoothDevice , connected device : " + device.getAddress(),Thread.currentThread().getStackTrace());
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
					setAllowConnect(false);
					disconnect();
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
	//  Debug function                                                 //
	//*****************************************************************//
	public int terminateTestModeConnection()
	{
		stopConnectThread();

		setAllowConnect(false);
		disconnect();  //MODE_BLE_DISCONNECT

		if(mInputDeviceProfile != null && mInputDeviceProfile.getConnectionState(mBluetoothDevice) == BluetoothProfile.STATE_CONNECTED)
		{
			appendLog("closeBTConnection");
			closeBTConnection(mInputDeviceProfile,mBluetoothDevice);
			return 500;
		}

		return 0;
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
			jsonObject.put(getString(R.string.title_auto_send_test),mAutoTest?1:0);
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
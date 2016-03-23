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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.*;
import android.preference.PreferenceManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;
import com.startline.slble.Adapter.BleDeviceRssiAdapter;
import com.startline.slble.PureClass.*;
import com.startline.slble.R;
import com.startline.slble.Util.*;
import com.startline.slble.module.BleConfiguration;
import com.startline.slble.module.StaCstaDecode;

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
	public static final int DEBUG_MODE = DEBUG_SHOW_ADV_DATA & 0;

	// Temperature Test
	public int mDisconnectCount = -1;
	private boolean mThermalTestEnabled = false;
	private int mThermalCommandCount = 0;
	private int mThermalCstaCount = 0;
	private boolean mWaitThermalCsta = false;
	private static final int THERMAL_COMMAND_INTERVAL = 30*1000;

	//=================================================================//
	//																   //
	//  Constant Variables                                             //
	//																   //
	//=================================================================//
	private static final String TAG = "BLE";
	private final String NEW_LINE_CHARACTER = "<BR/>";

	public static final int CSTA_STA = 0;

	public final int KEY_LESS_COMMAND_ARM = 0x01;
	public final int KEY_LESS_COMMAND_DISARM = 0x09;

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
	public static final int CONNECTION_RSSI_THRESHOLD = -82;
	public static final int CONNECTION_TIMEOUT = 6*1000;
	public static final int CONNECTION_MAX_RETRY = -1;
	public static final int CONNECTION_MAX_CONNECT = -1;
	public static final int SCAN_INTERVAL_MS = 500;
	public static final int READ_RSSI_INTERVAL = 3000;
	public static final long SCAN_PERIOD = 10 * 1000; // Stops scanning after 10 seconds.

    //--------------------------------------------------------------------
	// KeyLess
	public static final int READ_RSSI_KEY_LESS_INTERVAL = 400;
	public static final int READ_RSSI_KEY_LESS_TUNING_INTERVAL = 200;
	public static final int RSSI_TUNING_TOLERANCE = 2;
	public static final int RSSI_TUNING_STABLE_THRESHOLD = 5;
	public static final int RSSI_STABLE_RECORD_NUMBER = 10;
	public static final int RSSI_AVERAGE_RECORD_NUMBER = 10;
	public static final int RSSI_REAL_TIME_RECORD_NUMBER = 10;
	public static final int RSSI_KEY_LESS_OFFSET = -5;
	public static final int RSSI_KEY_LESS_AUTO_DISARM_RSSI = -58;
	public static final int RSSI_KEY_LESS_AUTO_ARM_RSSI = -85;

    //--------------------------------------------------------------------
	// Process
	public final long TIMEOUT_BOND_TO_DISCONNECT = 8000;
	public static final long TIMEOUT_TASK_RETRY = 3 * 1000;
	public static final String KEYWORD_SLBLE = "SLBLE";
	private static final String FILTER_ADDRESS = "48:36:5F";
	private final String UUID_BLE_SERVICE = "0000FFF0-0000-1000-8000-00805F9B34FB";
	private final String UUID_BLE_NOTIFY_CHANNEL = "0000FFF1-0000-1000-8000-00805F9B34FB";
	private final String UUID_BLE_WRITE_CHANNEL = "0000FFF2-0000-1000-8000-00805F9B34FB";
	public static final String EXTRA_DATA = "EXTRA_DATA";
	public static final String EXTRA_BINARY_DATA = "EXTRA_BINARY_DATA";
	public static final int INIT_STATE_NONE = 0;
	public static final int INIT_STATE_BONDING = 1;
	public static final int INIT_STATE_BOND_TO_CONNECT = 2;
	public static final int INIT_STATE_BOND_OK = 3;
	public static final int INIT_STATE_DEVICE_NAME = 4;
	public static final int INIT_STATE_TX_POWER = 5;
	public static final int INIT_STATE_INFORMATION = 6;
	public static final int INIT_STATE_CSTA = 7;
	public static final int INIT_STATE_END = 8;
    //--------------------------------------------------------------------
    // Action from UI thread
    public static final String ACTION_UI_NOTIFY_SERVICE = "ACTION_UI_NOTIFY_SERVICE";
	public static final int MODE_SET_CONFIGURATION = 0;
	public static final int ACTION_START_SCAN = 1;
	public static final int MODE_STOP_SCAN = 2;
	public static final int MODE_CONNECT = 3;
	public static final int MODE_DISCONNECT = 4;
	public static final int MODE_CONTROL_COMMAND = 5;
	public static final int MODE_KEY_LESS_TUNING = 6;
	public static final int MODE_TX_POWER = 7;

    // Debug
	public static final String ACTION_DEBUG_UI_SERVICE = "ACTION_DEBUG_UI_SERVICE";
    public static final int MODE_THERMAL_RESET = 0;


    //--------------------------------------------------------------------
	// Notify UI thread
    public static final String ACTION_SERVICE_NOTIFY_UI = "ACTION_SERVICE_NOTIFY_UI";

    // For list activity
    public static final int PARAM_SCAN_RESULT = 0x0;

	// For connected activity
    public static final int PARAM_CONNECT_STATUS = 0x100;
    public static final int PARAM_PROCESS_STEP = 0x101;
    public static final int PARAM_RSSI = 0x102;
    public static final int PARAM_CSTA = 0x103;
    public static final int PARAM_TUNING_RESULT = 0x104;
    public static final int PARAM_STABLE_RSSI_RECORD = 0x105;

    // GATT
	public static final int PARAM_GATT_CONNECTING = 0x200;
    public static final int PARAM_GATT_CONNECTED = 0x201;
    public static final int PARAM_GATT_DISCONNECTED = 0x202;
    public static final int PARAM_GATT_SERVICES_DISCOVERED = 0x203;
    public static final int PARAM_GATT_READ_DEVICE_NAME = 0x204;
    public static final int PARAM_GATT_ON_DESCRIPTOR_WRITE = 0x205;

    // Debug
	public static final String ACTION_DEBUG_SERVICE_TO_UI = "ACTION_DEBUG_SERVICE_TO_UI";
    public static final int PARAM_ADV_DATA = 0;
    public static final int PARAM_THERMAL = 1;
    public static final int PARAM_LOG = 2;




    //--------------------------------------------------------------------
	// AES
	private static final String CIPHER_ALGORITHM_AES = "AES";
	private static final String CIPHER_TRANSFORMATION = "AES/CBC/NoPadding";
	private final byte[] DEFAULT_INITIALIZE_VECTOR = new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
	private final byte[] DEFAULT_AES_KEY = new byte[]
	{
		(byte) 0x92, (byte) 0xA5, (byte) 0x23, (byte) 0xCD, (byte) 0xF6, (byte) 0x68, (byte) 0x74, (byte) 0xBA
		, (byte) 0x63, (byte) 0x73, (byte) 0x53, (byte) 0x55, (byte) 0xBF, (byte) 0x91, (byte) 0x02, (byte) 0xFA
	};

    //--------------------------------------------------------------------
    // TX POWER
	private final int[] TX_POWER_VALUE_ARRAY = new int[]
	{
        PARAM_TX_POWER_N_17_DBM,       PARAM_TX_POWER_N_15_DBM,       PARAM_TX_POWER_N_10_DBM,       PARAM_TX_POWER_N_5_DBM
		,PARAM_TX_POWER_0_DBM,          PARAM_TX_POWER_2_DBM,           PARAM_TX_POWER_4_DBM,           PARAM_TX_POWER_7_DBM
	};


	//=================================================================//
	//																   //
	//  Global Variables                                               //
	//																   //
	//=================================================================//
	// Basic
    private final int RECEIVE_DATA_BUFFER_LENGTH = 100;

	// AutoConnect
	private boolean mAllowReConnect = true;
	private boolean mAutoConnectOnDisconnect = false;

	// Process
	private int mDeviceInitState = INIT_STATE_NONE;
	private boolean mScanning;
	private boolean mWriteTxPower = false;
	private int mConnectionState = BluetoothProfile.STATE_DISCONNECTED;
	private byte mPacketId = 0x0;
	private byte[] mReceivedData = null;
	private byte[] mReceivedDataBuffer = null;
	private int mReceiveDataCounter = 0;
	private int mTaskRetry = 0;
	private int mConnectRetry = 0;
	private int mReadRssiInterval = READ_RSSI_INTERVAL;
	private int mBleSettingSendCount = 0;

	// KeyLess
	private boolean mKeyLessEnabled = false;
	private boolean mKeyLessTuning = false;
	private boolean mSlaveTag = false;
	private int mAverageRssi = 0;
	private int mLastKeyLessCommand = 0;
	private int mRealTimeRssiRecordIndex = 0;
	private int mAvgRssiRecordIndex = 0;
	private int mStableRssiRecordIndex = 0;
	private int mKeyLessThreshold = 0;
	private int mRssiTuningStableCounter = 0;
	private boolean mKeyLessArmAvailable = false;
	private boolean mKeyLessDisarmAvailable = false;
	private boolean mIsCarArm = false;
	private boolean mISCarLock = false;
	private long mLastTimeRecordAverageRssi = -1;
	private long mLastTimeRecordStableRssiRecord = -1;
	private long mLastTimeRecordTuningRssi = -1;
	private int[] mRealTimeRssiRecord = null;
	private int[] mAvgRssiRecord = null;
	private int[] mStableRssiRecord = null;

	// For debug
	private long mLastTimeTryConnect = 0;
	private boolean mTestMode = false;
	private int mDebugParameter = -1;
	private boolean mDebugFormatError = false;
	private boolean mDebugPingNoAck = false;


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
	private BluetoothGattService mBleService;
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
	//=================================================================//
	//																   //
	//  Global defined Object                                          //
	//																   //
	//=================================================================//
	private final IBinder mBinder = new LocalBinder();
	private final Handler mHandler = new Handler();



	// Bonding receiver, handle bond message
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


						// Action when receiving ACL_DISCONNECT after bonding success
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
						mBluetoothGatt.readRemoteRssi();
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

	// This is for Thermal Test
	// It will send CONTROL_ALARM_CHECK_CAR_STATUS periodically and wait for response
	private final Runnable thermalRunnable = new Runnable()
	{
		@Override
		public void run()
		{
			// Set a wait csta flag to indicate we have to receive a csta later
			mWaitThermalCsta = true;
			mThermalCommandCount++;
			sendCommand(CONTROL_ALARM_CHECK_CAR_STATUS);
            broadcastDebugNotifyUi(getThermalIntent(16|32,mThermalCommandCount+"",mThermalCstaCount+""));

			mHandler.postDelayed(thermalRunnable,THERMAL_COMMAND_INTERVAL);
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
				//appendLog(String.format("Connection State = %d,  Status = %d ", newState, status));

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
			LogUtil.d(TAG, "[Process] onServicesDiscovered received: " + status,Thread.currentThread().getStackTrace());
			appendLog("[Process] onServicesDiscovered received: " + status);
			try
			{
				if (status == BluetoothGatt.GATT_SUCCESS)
				{
					broadcastNotifyUi(getGattIntent(PARAM_GATT_SERVICES_DISCOVERED));

					if (getConnectionState() == BluetoothProfile.STATE_CONNECTED)
					{
						if (isNotificationEnabled(getNotifyGattCharacteristic()) == false)
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

						mDeviceInitState = INIT_STATE_TX_POWER;

                        initDeviceState();// Get device name
					}
					else if(characteristic.getUuid().toString().equalsIgnoreCase(CHAR_TX_POWER_LEVEL))
					{
                        final int dbValue = characteristic.getValue()[0];
                        LogUtil.d(TAG, "[Process] ]Read TxPower value : " + dbValue,Thread.currentThread().getStackTrace());

                        // dbValue from device
                        saveTxPowerValue(dbValue);

                        initDeviceState(); // Read Tx Power
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
			if (status == BluetoothGatt.GATT_SUCCESS)
			{
				if (rssi < 0)
				{
					if (mKeyLessTuning
							|| mKeyLessEnabled
							)
					{
						recordRssi(rssi);
					}
					broadcastNotifyUi(getRssiIntent(rssi, mAverageRssi));
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
                        case MODE_SET_CONFIGURATION:
                        {
                            final String key = intent.getStringExtra("key");
                            if(key.equals(getString(R.string.pref_key_auto_connect)))
                            {
                                mAutoConnectOnDisconnect = intent.getBooleanExtra("auto_connect", false);
                            }
                            else if(key.equals(getString(R.string.pref_key_slave_tag)))
                            {
                                mSlaveTag = intent.getBooleanExtra("slave_tag", false);
                            }
                            else if(key.equals(getString(R.string.pref_key_key_less)))
                            {
                                mKeyLessEnabled = intent.getBooleanExtra("key_less", false);
                                mKeyLessArmAvailable = intent.getBooleanExtra("key_less_arm", false);
                                mKeyLessDisarmAvailable = intent.getBooleanExtra("key_less_disarm", false);
                                updateKeyLessConfiguration();
                            }
                            else if(key.equals(getString(R.string.pref_key_tx_power)))
                            {
                                // value from SettingActivity,it is index of TX_POWER_VALUE_ARRAY
                                final int value = intent.getIntExtra("tx_power", -1);
                                sendTxPowerSetting(value);
                            }
                            else if(key.equals(getString(R.string.pref_key_test_mode)))
                            {
                                mTestMode = intent.getBooleanExtra("test_mode", false);
                            }
                            else if(key.equals(getString(R.string.pref_key_ping_no_ack)))
                            {
                                mDebugPingNoAck = intent.getBooleanExtra("ping_no_ack",false);
                            }
                            else if(key.equals(getString(R.string.pref_key_format_error)))
                            {
                                mDebugFormatError = intent.getBooleanExtra("format_error", false);
                            }
                            else if(key.equals(getString(R.string.pref_key_thermal_test)))
                            {
                                mThermalTestEnabled = intent.getBooleanExtra("thermal_test", false);

                                // If disabled, remove runnable
                                if(mThermalTestEnabled == false)
                                {
                                    mHandler.removeCallbacks(thermalRunnable);
                                }
                                else
                                {
                                    // If enabled and already at connected state, set up runnable
                                    if(getConnectionState() == BluetoothProfile.STATE_CONNECTED)
                                    {
                                        mHandler.postDelayed(thermalRunnable,THERMAL_COMMAND_INTERVAL);
                                    }
                                }
                            }
                        }
                        break;

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
								connectWithTimer(mBluetoothDevice.getAddress());  // Manual connect
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

							if(mInputDeviceProfile != null && mInputDeviceProfile.getConnectionState(mBluetoothDevice) == BluetoothProfile.STATE_CONNECTED)
							{
								appendLog("closeBTConnection");
								closeBTConnection(mInputDeviceProfile,mBluetoothDevice);
							}

                            disconnect(false);  //MODE_BLE_DISCONNECT
                        }
                        break;

                        case MODE_CONTROL_COMMAND:
                        {
                            final int cmd = intent.getIntExtra("param", 0);
                            sendCommand(cmd);
                        }
                        break;

                        case MODE_KEY_LESS_TUNING:
                        {
                            if (mKeyLessTuning == false)
                            {
                                mKeyLessTuning = true;
                            }
                            else
                            {
                                mKeyLessTuning = false;
                            }
                            settingRssiTimer();

                            mRssiTuningStableCounter = 0;
                            resetRssiRecord();
                            resetAvgRssiRecord();
                        }
                        break;

                        case MODE_TX_POWER:
                        {
                            final int value = intent.getIntExtra("tx_power", -1);
                            if(value >= 0)
                            {
                                sendTxPowerSetting(TX_POWER_VALUE_ARRAY[value]);
                            }
                        }
                        break;
                    }

				}
				else if (action.equals(ACTION_DEBUG_UI_SERVICE))
				{
                    final int mode = intent.getIntExtra("mode",-1);
                    switch (mode)
                    {
                        case MODE_THERMAL_RESET:
                        {
                            final boolean reset = intent.getBooleanExtra("thermal_reset",false);

                            // Reset thermal test configuration
                            if(reset)
                            {
                                mDisconnectCount = -1;
                                mThermalCommandCount = 0;
                                mThermalCstaCount = 0;
                                mWaitThermalCsta = false;
                            }
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

		resetRssiRecord();
		resetAvgRssiRecord();
		resetStableRssiRecord();

		getAppConfiguration();


		// Register receiver
		registerBleOperateReceiver();

		// Register bonding receiver
		registerReceiver(mBondingBroadcastReceiver, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));


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

	public void setIpcCallbackhandler(final Handler callbackHandler)
	{
		mIpcCallbackHandler = callbackHandler;
	}

	public Handler getmIpcCallbackHandler()
	{
		return mIpcCallbackHandler;
	}

	public void setupBluetoothDeviceFromCache(final String address)
	{
		// Get device form cache
		final CachedBluetoothDevice cachedBluetoothDevice = getCachedBluetoothDevice(address);
		if(cachedBluetoothDevice != null)
		{
			// Set up bluetooth resources
			setBluetoothGatt(cachedBluetoothDevice.bluetoothGatt);
			setBluetoothDevice(cachedBluetoothDevice.bluetoothDevice);

			// Set as connected
			mConnectionState = BluetoothProfile.STATE_CONNECTED;

			// Request Csta to show right status
			sendRequestCsta();

			// Notify UI
			broadcastNotifyUi(getGattIntent(PARAM_GATT_CONNECTED)); // notify Connect fragment
			broadcastNotifyUi(getProcessStepIntent(INIT_STATE_END));		// notify Status fragment
		}
		else
		{
			mConnectionState = BluetoothProfile.STATE_DISCONNECTED;
			setBluetoothGatt(null);
			setBluetoothDevice(null);
		}
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

	private void getAppConfiguration()
	{
		final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		mAutoConnectOnDisconnect = sharedPreferences.getBoolean(getString(R.string.pref_key_auto_connect),false);
		mSlaveTag = sharedPreferences.getBoolean(getString(R.string.pref_key_slave_tag),false);
		mKeyLessEnabled = sharedPreferences.getBoolean(getString(R.string.pref_key_key_less),false);
		mKeyLessThreshold = getKeyLessThreshold();
		getKeyLessArmSetting();
		getKeyLessDisarmSetting();

		mTestMode = sharedPreferences.getBoolean(getString(R.string.pref_key_test_mode),false);
		mDebugPingNoAck = sharedPreferences.getBoolean(getString(R.string.pref_key_ping_no_ack),false);
		mDebugFormatError = sharedPreferences.getBoolean(getString(R.string.pref_key_format_error),false);
		mThermalTestEnabled = sharedPreferences.getBoolean(getString(R.string.pref_key_thermal_test),false);
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

	private BluetoothGattCharacteristic getNotifyGattCharacteristic()
	{
	 	if(mNotifyCharacteristic == null)
		{
			mNotifyCharacteristic = getCharacteristic(UUID_BLE_NOTIFY_CHANNEL);
		}

		return mNotifyCharacteristic;
	}

	private BluetoothGattCharacteristic getWriteGattCharacteristic()
	{
	 	if(mWriteCharacteristic == null)
		{
			mWriteCharacteristic = getCharacteristic(UUID_BLE_WRITE_CHANNEL);
		}

		return mWriteCharacteristic;
	}

	private BluetoothGattCharacteristic getCharacteristic(final String uuid)
	{
		if(getBleService() == null) return null;

		return getBleService().getCharacteristic(UUID.fromString(uuid));
	}

	private BluetoothGattService getBleService()
	{
		if(mBleService == null)
		{
			final List<BluetoothGattService> serviceList = getSupportedGattServices();
			if( serviceList != null)
			{
				for(int i=0;i<serviceList.size();i++)
				{
					final BluetoothGattService service = serviceList.get(i);
					final String uuid = service.getUuid().toString();
					if(uuid.equalsIgnoreCase(UUID_BLE_SERVICE))
					{
						mBleService = service;
						break;
					}
				}
			}
		}

		return mBleService;
	}

    private boolean isDeviceInitialized()
    {
        if(mDeviceInitState == INIT_STATE_END)
            return true;
        else
            return false;
    }
	private void initDeviceState()
	{
		try
		{
			switch (mDeviceInitState)
			{
				case INIT_STATE_DEVICE_NAME:
				{
					readTxPower();
					mDeviceInitState = INIT_STATE_TX_POWER;
				}
				break;
				case INIT_STATE_TX_POWER:
				{
					initSendBleSetting();
					mDeviceInitState = INIT_STATE_INFORMATION;
				}
				break;
				case INIT_STATE_INFORMATION:
				{
					sendRequestCsta();
					mDeviceInitState = INIT_STATE_CSTA;
				}
				break;
				case INIT_STATE_CSTA:
				{
					playLoginSuccessSound();
					broadcastNotifyUi(getProcessStepIntent(INIT_STATE_END));
					mDeviceInitState = INIT_STATE_END;

					// After connecting and verified, add device to cache list
					addBluetoothDeviceToCache(mBluetoothDevice,getBluetoothGatt());
				}
				break;
			}
		}
		catch (Exception e)
		{
			LogUtil.e(TAG, e.toString(),Thread.currentThread().getStackTrace());
		}
	}

	private void updateKeyLessConfiguration()
	{
		initSendBleSetting();
		if(mKeyLessEnabled)
		{
			mKeyLessThreshold = getKeyLessThreshold();
			// Key less turn to enabled, reset all record
			if(mKeyLessDisarmAvailable || mKeyLessArmAvailable)
			{
				resetRssiRecord();
				resetAvgRssiRecord();
				resetStableRssiRecord();
			}
		}
		settingRssiTimer();
	}

	private void resetRssiRecord()
	{
		mRealTimeRssiRecordIndex = 0;
		mRealTimeRssiRecord = new int[RSSI_REAL_TIME_RECORD_NUMBER];
	}

	private void resetAvgRssiRecord()
	{
		mAvgRssiRecordIndex = 0;
		mAverageRssi = 0;
		mAvgRssiRecord = new int[RSSI_AVERAGE_RECORD_NUMBER];
	}

	private void resetStableRssiRecord()
	{
		mStableRssiRecordIndex = 0;
		mStableRssiRecord = new int[RSSI_STABLE_RECORD_NUMBER];
	}

	// Average Algorithm
	//*
	private void recordRssi(final int rssi)
	{
		int temp = 0;
		int validNumber = 0;
		int realTimeAvg = 0;


		// Save real time rssi
		mRealTimeRssiRecord[mRealTimeRssiRecordIndex++] = rssi;
		if(mRealTimeRssiRecordIndex == mRealTimeRssiRecord.length)
			mRealTimeRssiRecordIndex = 0;


		// Get the average Rssi from real time Rssi record at this time
		for(int i=0;i<mRealTimeRssiRecord.length;i++)
		{
			if(mRealTimeRssiRecord[i] < 0)
				validNumber++;
			temp += mRealTimeRssiRecord[i];
		}
		if(validNumber > 0)
		//if(validNumber == mRssiRecord.length)
			realTimeAvg = temp / validNumber;
		else
			realTimeAvg = 0;



		// Save real time average Rssi
		//if(System.currentTimeMillis()-mLastTimeRecordAverageRssi > (mKeyLessTuning? 0 : 300))
		{
			mLastTimeRecordAverageRssi = System.currentTimeMillis();
			mAvgRssiRecord[mAvgRssiRecordIndex++] = realTimeAvg;
			if(mAvgRssiRecordIndex == mAvgRssiRecord.length)
				mAvgRssiRecordIndex = 0;


			// Get average Rssi from AvgRssiRecord
			temp = 0;
			validNumber = 0;
			for(int i=0 ; i< mAvgRssiRecord.length ; i++)
			{
				if (mAvgRssiRecord[i] < 0)
					validNumber++;
				temp += mAvgRssiRecord[i];
			}
			if(validNumber > 0)
			//if(validNumber == mAvgRssiRecord.length)
				mAverageRssi = temp / validNumber;
			else
				mAverageRssi = 0;
		}



		// For key less tuning
		if(mKeyLessTuning)
		{
			// Wait until mAvgRssiRecord is full
			if(validNumber == mAvgRssiRecord.length)
			{
				tuningKeyLessRssi();
			}
			broadcastNotifyUi(getTuningResultIntent(0, mRealTimeRssiRecord,mAvgRssiRecord,mAverageRssi));
		}


		// For key less enable
		// Wait until mAvgRssiRecord is full
		else if(mKeyLessEnabled && (mKeyLessArmAvailable || mKeyLessDisarmAvailable) && validNumber == mAvgRssiRecord.length)
		{
			broadcastNotifyUi(getTuningResultIntent(0, mRealTimeRssiRecord,mAvgRssiRecord,mAverageRssi));
			if(getConnectionState() == BluetoothProfile.STATE_CONNECTED)
				handleKeyLess(realTimeAvg);
		}
	}

	private void tuningKeyLessRssi()
	{
		// Judge if Rssi value is stable or not in the last 5 seconds
		// Judge once per second
		if(System.currentTimeMillis()-mLastTimeRecordTuningRssi > 1000)
		{
			mLastTimeRecordTuningRssi = System.currentTimeMillis();
			boolean stable = true;

			// All the record must close the last average Rssi
			// The difference must less than tolerance
			for(int i=0;i<mAvgRssiRecord.length;i++)
			{
				if(Math.abs(mAverageRssi-mAvgRssiRecord[i]) > RSSI_TUNING_TOLERANCE)
				{
					stable = false;
					break;
				}
			}

			// Count how stable it is
			if(stable)
			{
				mRssiTuningStableCounter ++;
			}
			else
			{
				mRssiTuningStableCounter = 0;
			}

			// If it's stable still for 5 seconds, save the tuning result
			if(mRssiTuningStableCounter == RSSI_TUNING_STABLE_THRESHOLD)
			{
				broadcastNotifyUi(getTuningResultIntent(1,mRealTimeRssiRecord,mAvgRssiRecord,mAverageRssi));
				mKeyLessTuning = false;
				mKeyLessThreshold = mAverageRssi;
				saveKeyLessThreshold(mAverageRssi);

				settingRssiTimer();
			}
		}
	}


	// Using Average Algorithm
	private void handleKeyLess(final int realTimeAvg)
	{
		if(System.currentTimeMillis()-mLastTimeRecordStableRssiRecord > 1000)
		{
			mLastTimeRecordStableRssiRecord = System.currentTimeMillis();
			mStableRssiRecord[mStableRssiRecordIndex++] = mAverageRssi;

			if(mStableRssiRecordIndex == mStableRssiRecord.length)
				mStableRssiRecordIndex = 0;
		}

		// Sort stable record from new to old ASC
		final int[] stableRecord = sortArray(mStableRssiRecord,RSSI_STABLE_RECORD_NUMBER,mStableRssiRecordIndex);
        broadcastNotifyUi(getStableRssiRecordIntent(stableRecord));

		// Sort average record from new to old ASC
		final int[] averageRecord = sortArray(mAvgRssiRecord,RSSI_AVERAGE_RECORD_NUMBER,mAvgRssiRecordIndex);

		// Sort real time record from new to old ASC
		final int[] realTimeRecord = sortArray(mRealTimeRssiRecord,RSSI_REAL_TIME_RECORD_NUMBER,mRealTimeRssiRecordIndex);


        // Keyless can only action after init process finished
		if(isDeviceInitialized())
			analyzeKeyLessBehavior(realTimeRecord,averageRecord,stableRecord);
	}

	private void analyzeKeyLessBehavior(final int[] realTimeRecord, final int[] averageRecord, final int[] stableRecord)
	{
		final int disArmThreshold = mKeyLessThreshold + RSSI_KEY_LESS_OFFSET;
		final int armThreshold = mKeyLessThreshold;

		// Disarm
		if( mKeyLessDisarmAvailable )
		{
			if(	//realTimeRecord[0]-mKeyLessThreshold > 0
				//&& (averageRecord[0]-averageRecord[1] > 0 || realTimeRecord[0] - realTimeRecord[1] > 0)
				//&& averageRecord[0] > averageRecord[1] && averageRecord[1] > averageRecord[2] //&& averageRecord[2] > averageRecord[3] //&& averageRecord[3] > averageRecord[4]
				//&& stableRecord[0] > stableRecord[1] &&  stableRecord[1] > stableRecord[2]
				mAverageRssi > Math.max(RSSI_KEY_LESS_AUTO_DISARM_RSSI,disArmThreshold)
			)
			{
				if(mIsCarArm)
				{
					mKeyLessDisarmAvailable = false;
					sendKeyLessCommand(KEY_LESS_COMMAND_DISARM);
					//settingRssiTimer(READ_RSSI_KEY_LESS_INTERVAL);
				}
				return;
			}
		}

		// Arm
		if( mKeyLessArmAvailable )
		{
			if( (armThreshold - averageRecord[0] > 0 && armThreshold - averageRecord[1] > 0 && armThreshold - averageRecord[2] > 0
					&& averageRecord[0] < averageRecord[1] && averageRecord[1] < averageRecord[2] && averageRecord[2] < averageRecord[3]
					&& stableRecord[0] < stableRecord[1] && stableRecord[1] < stableRecord[2])
				|| mAverageRssi <= armThreshold -10
				)
			{
				if(mIsCarArm == false)
				{
					mKeyLessArmAvailable = false;
					sendKeyLessCommand(KEY_LESS_COMMAND_ARM);
				}
				//settingRssiTimer(READ_RSSI_KEY_LESS_INTERVAL);
			}
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

	private void sendKeyLessCommand(final int cmd)
	{
		//if(mLastKeyLessCommand != cmd)
		{
			mLastKeyLessCommand = cmd;
			sendCommand(cmd);
		}
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

	// For non member function calls
	public void connectDevice(final String deviceAddress)
	{
		LogUtil.d(TAG,"[Process] connectDevice address : "+deviceAddress,Thread.currentThread().getStackTrace());
		if(deviceAddress == null || deviceAddress.length() == 0)
		{
			return;
		}

		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		broadcastNotifyUi(getConnectStatusIntent(CONNECTION_STATE_CONNECTING));

		// Connected with OS
		if(isBluetoothDeviceConnected(deviceAddress))
		{
			// Cached with App
			if(isBluetoothDeviceCached(deviceAddress))
			{
				setupBluetoothDeviceFromCache(deviceAddress);
			}
			else
			{
				// Connected,not cached
				_connectDevice(deviceAddress);
			}
		}
		// Not connected
		else
		{
			boolean paired = false;

			// Check if paired
			final Set<BluetoothDevice> bondDeviceSet = mBluetoothAdapter.getBondedDevices();
			for(BluetoothDevice bluetoothDevice:bondDeviceSet)
			{
				if(bluetoothDevice.getAddress().equals(deviceAddress))
				{
					paired = true;
					break;
				}
			}

			// Paired, start BTConnection first
			if(paired)
			{
				try
				{
					new Thread()
					{
						@Override
						public void run()
						{
							while(getConnectionState() != BluetoothProfile.STATE_CONNECTED)
							{
								initConnection(deviceAddress);
								try {
									Thread.sleep(2000);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
						}
					}.start();

				}
				catch (Exception e)
				{
					LogUtil.e(TAG,e.toString(),Thread.currentThread().getStackTrace());
				}
			}
			// Not paired, start bond
			else
			{
				_connectDevice(deviceAddress);
			}
		}
	}

	// init mBluetoothDevice and start BTConnect
	public void initConnection(final String address)
	{
		LogUtil.d(TAG,"[Process] initConnection address : "+address,Thread.currentThread().getStackTrace());
		if(mBluetoothAdapter == null)
		{
			LogUtil.e(TAG, "[Error] mBluetoothAdapter is null.  Unable to connect.",Thread.currentThread().getStackTrace());
			return;
		}

		// Get device from adapter
		setBluetoothDevice(mBluetoothAdapter.getRemoteDevice(address));
		if (mBluetoothDevice == null)
		{
			LogUtil.e(TAG, "[Error] mBluetoothDevice not found.  Unable to connect.",Thread.currentThread().getStackTrace());
			return;
		}

		createBTConnection();
	}

	// Bonding or connect
	private void _connectDevice(final String address)
	{
		LogUtil.d(TAG,"[Process] connectDevice address : "+address,Thread.currentThread().getStackTrace());

		// Get device from adapter
		setBluetoothDevice(mBluetoothAdapter.getRemoteDevice(address));
		if (mBluetoothDevice == null)
		{
			LogUtil.e(TAG, "[Error] mBluetoothDevice not found.  Unable to connect.",Thread.currentThread().getStackTrace());
			return;
		}

		// Check if address valid
		if(address!= null && address.length() > 0)
		{
			// If device not bonded, start bonding
			if(mBluetoothDevice.getBondState() != BluetoothDevice.BOND_BONDED)
			{
				_bondingDevice();
			}
			// Device bonded, try to connect to it
			else
			{
				_connect();
			}
		}
	}

	private void reConnectAfterMilliSeconds(final int time)
	{
		// Delay time to connect
		mHandler.postDelayed(new Runnable()
		{
			@Override
			public void run()
			{
				reConnectDevice();
			}
		}, time);
	}

	private void reConnectDevice()
	{
		if((mKeyLessEnabled && (mKeyLessArmAvailable || mKeyLessDisarmAvailable) || mKeyLessTuning) || CONNECTION_MAX_CONNECT == -1 || mConnectRetry++ < CONNECTION_MAX_CONNECT)
		{
			LogUtil.d(TAG,"[Process] reConnectDevice",Thread.currentThread().getStackTrace());
			//broadcastConnectStatus(ACTION_BLE_NOTIFY_CONNECT_STATUS,CONNECTION_STATE_RE_CONNECTING);
			_connect();
		}
	}

	private void connectWithTimer(final String address)
	{
		mConnectRetry = 0;
		connectDevice(address);
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
			if(mBluetoothDevice == null)
			{
				LogUtil.d(TAG, "[Error] mBluetoothDevice is null.",Thread.currentThread().getStackTrace());
				return;
			}


			final int connectionState = getConnectionState();
			if(connectionState == BluetoothProfile.STATE_CONNECTING )
			{
				appendLog("Already connecting");
				LogUtil.e(TAG, "[Error] Already connecting.",Thread.currentThread().getStackTrace());
				return;
			}
			else if(connectionState == BluetoothProfile.STATE_CONNECTED)
			{
				appendLog("Already connected");
				LogUtil.e(TAG, "[Error] Already connected.",Thread.currentThread().getStackTrace());
				return;
			}
			else if(connectionState == BluetoothProfile.STATE_DISCONNECTING)
			{
				appendLog("Disconnecting, wait until disconnected");
				LogUtil.e(TAG, "[Error] Disconnecting, wait until disconnected.",Thread.currentThread().getStackTrace());
				return;
			}
			mConnectionState = BluetoothProfile.STATE_CONNECTING;

			// We want to directly connect to the device, so we are setting the autoConnect parameter to false.
			setBluetoothGatt(mBluetoothDevice.connectGatt(context, false, mGattCallback));

			mAllowReConnect = true;
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
    public void disconnect(final boolean allowReConnect)
	{
		mAllowReConnect = allowReConnect;

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
				mBluetoothGatt.disconnect();
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
						_connectDevice(mBluetoothDevice.getAddress());
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

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void closeGatt()
	{
        if (mBluetoothGatt != null)
		{
            mBluetoothGatt.close();
        }

		mBleService = null;
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
        mBluetoothGatt.readCharacteristic(characteristic);
    }

	public void writeCharacteristic(BluetoothGattCharacteristic characteristic)
	{
        if (mBluetoothAdapter == null || mBluetoothGatt == null)
		{
            LogUtil.d(TAG, "[Process] BluetoothAdapter not initialized",Thread.currentThread().getStackTrace());
			return;
		}
        mBluetoothGatt.writeCharacteristic(characteristic);
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
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
		//mBluetoothGatt.reg

		final BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(BleGattAttributes.DESCRIPTORS_CLIENT_CHARACTERISTIC_CONFIGURATION));
		if(descriptor != null)
		{
			descriptor.setValue(enabled?BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
			mBluetoothGatt.writeDescriptor(descriptor);
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

        return mBluetoothGatt.getServices();
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
		if(getNotifyGattCharacteristic() != null)
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

	private void readTxPower()
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
		if(mKeyLessTuning)
		{
			settingRssiTimer(READ_RSSI_KEY_LESS_TUNING_INTERVAL);
		}
		else if(mKeyLessEnabled && (mKeyLessArmAvailable || mKeyLessDisarmAvailable))
		{
			// If unlock is enable,we need to read Rssi frequently
			if(mKeyLessDisarmAvailable)
			{
				settingRssiTimer(READ_RSSI_KEY_LESS_TUNING_INTERVAL);
			}
			else
			{
				settingRssiTimer(READ_RSSI_KEY_LESS_INTERVAL);
			}
		}
		else
		{
			settingRssiTimer(READ_RSSI_INTERVAL);
		}
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

			// Record connection state
			mConnectionState = BluetoothProfile.STATE_CONNECTED;

            broadcastNotifyUi(getGattIntent(PARAM_GATT_CONNECTED));

			// Debug information
			mLastTimeTryConnect = 0;

            // Update read rssi period
			settingRssiTimer();

			getKeyLessDisarmSetting();

			getKeyLessArmSetting();

			// If services characteristics is empty,
			// It is the first connect, update flow step and start discovery services
			if(getBleService() == null)
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

			 // Remove runnable when disconnect to avoid unexpected action
			if(mThermalTestEnabled)
			{
				mHandler.removeCallbacks(thermalRunnable);

				// If was login, record disconnect count
				if(isDeviceInitialized())
				{
					// First Disconnect
					if(mDisconnectCount == 0)
					{
						mDisconnectCount++;
                        broadcastDebugNotifyUi(getThermalIntent(4|64,TimeUtil.getTimeNow(TimeUtil.FORMAT_HH_mm),mDisconnectCount+""));
					}
					else
					{
						mDisconnectCount++;
                        broadcastDebugNotifyUi(getThermalIntent(8|64,TimeUtil.getTimeNow(TimeUtil.FORMAT_HH_mm),mDisconnectCount+""));
					}
				}
			}

			if(isDeviceInitialized())
				playDisconnectedSound();

			closeGatt();
			cancelRssiTimer();
			cancelTaskTimeoutTimer();	//handleBleDisconnect

            broadcastNotifyUi(getGattIntent(PARAM_GATT_DISCONNECTED));

			mDeviceInitState = INIT_STATE_NONE;

			// mAllowReConnect is false when disconnect manually
			if(mAllowReConnect == false)
			{
				mHandler.removeCallbacksAndMessages(null);
				mBluetoothGatt = null;
				return;
			}

			// If autoConnect is enabled or is tuning key less, re-connect it
			if(mAutoConnectOnDisconnect || mKeyLessTuning)
			{
				reConnectAfterMilliSeconds(2000); // Re-connect if AutoConnect or KeyLessTuning is enabled
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
							discoverSuccess = mBluetoothGatt.discoverServices();
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

		if(mThermalTestEnabled && mWaitThermalCsta)
		{
			mWaitThermalCsta = false;
			mThermalCstaCount++;
            broadcastDebugNotifyUi(getThermalIntent(16|32,mThermalCommandCount+"",mThermalCstaCount+""));
		}

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

		if(arm == 1)
			mIsCarArm = true;
		else
			mIsCarArm = false;
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
			switch (receiveData[PACKET_COMMAND])
			{
				case CMD_ACK:
				{
					if(mWriteTxPower == true)
					{
						readTxPower();
                        mWriteTxPower = false;
					}
				}
				break;
				case CMD_CAR_STATUS:
				{
					handleReceiveCsta(receiveData,false);

                    initDeviceState(); // Receive CSTA
				}
				break;

				case CMD_CHECK_CONNECTION:
				{
					// Debug
					if(mDebugPingNoAck == true) return;

					final byte[] writeData = new byte[PACKET_LENGTH];
					writeData[PACKET_ID] = receiveData[PACKET_ID];
					writeData[PACKET_COMMAND] = receiveData[PACKET_COMMAND];

					final String s = formatByteArrayToLog(receiveData);
					LogUtil.d(TAG, "Receive Ping " + s,Thread.currentThread().getStackTrace());
					appendLog(formatReceiveString("Receive Ping"));
					sendCheckConnectionAck(true);
				}
				break;

				case CMD_SLAVE_TAG:
				{
					// Skip if SlaveTag is disabled
					if(mSlaveTag == false)
						return;


					// Check parameter
					if(receiveData[PACKET_PARAMETER] != PARAM_SLAVE_TAG_REQUEST)
						return;

					// Init packet content
					final byte[] writeData = new byte[PACKET_LENGTH];
					writeData[PACKET_ID] = receiveData[PACKET_ID];
					writeData[PACKET_COMMAND] = receiveData[PACKET_COMMAND];
					writeData[PACKET_PARAMETER] = PARAM_SLAVE_TAG_RESPONSE;

					// Fill data, the first byte fill DATA_SLAVE_TAG, others random number
					final byte[] data = generateRandomArray(16,255);
					data[0] = DATA_SLAVE_TAG;
					fillData(writeData,data);

					// Fill check sum
					writeData[PACKET_CHECK_SUM] = getCheckSum(writeData);

					final String s = formatByteArrayToLog(receiveData);
					LogUtil.d(TAG, "[Tag] Request tag " + s,Thread.currentThread().getStackTrace());
					appendLog(formatReceiveString("Request tag"));

					final long delay = receiveData[PACKET_DATA];

					// send data
					if(delay == 0)
						sendPlainData(writeData);
					else
					{
						mHandler.postDelayed(new Runnable()
						{
							@Override
							public void run()
							{
								sendPlainData(writeData);
							}
						},delay*1000);
					}
				}
				break;

				case CMD_SETTING_INFORMATION:
				{
					// If receive ble setting from device, save it and response back
					if(receiveData[PACKET_PARAMETER] == PARAM_SETTING_SEND)
                    {
                        responseBleSetting(receiveData);
                    }
                    // Receive response from device, check if data is match we sent before
					else if(receiveData[PACKET_PARAMETER] == PARAM_SETTING_RESPONSE)
                    {
                        checkBleSetting(receiveData);
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

	private void initSendBleSetting()
	{
		mBleSettingSendCount = 0;
		sendBleSetting();
	}

	private void reSendBleSetting()
	{
		if(mBleSettingSendCount < 3)
		{
			sendBleSetting();
			mBleSettingSendCount++;
		}
		else
		{
			disconnect(true);
		}
	}

	// Send Ble Setting to device
	private void sendBleSetting()
	{
        LogUtil.d(TAG, "[Process] sendBleSetting",Thread.currentThread().getStackTrace());
        appendLog(formatSendString("sendBleSetting"));
		// Init packet content
		final byte[] writeData = new byte[PACKET_LENGTH];
		writeData[PACKET_ID] = getRandom(255);
		writeData[PACKET_COMMAND] = CMD_SETTING_INFORMATION;
		writeData[PACKET_PARAMETER] = PARAM_SETTING_SEND;

		sendBleSettingPacket(writeData);
	}

	// Response Ble Setting to device
	private void responseBleSetting(final byte[] receiveData)
	{
		// Init packet content
		final byte[] writeData = new byte[PACKET_LENGTH];
		writeData[PACKET_ID] = receiveData[PACKET_ID];
		writeData[PACKET_COMMAND] = CMD_SETTING_INFORMATION;
		writeData[PACKET_PARAMETER] = PARAM_SETTING_RESPONSE;

		final String s = formatByteArrayToLog(receiveData);
		LogUtil.d(TAG, "Request Ble setting " + s,Thread.currentThread().getStackTrace());
		appendLog(formatReceiveString("Request Ble setting"));


		saveBleConfiguration(subByteArray(receiveData,3,10));

		sendBleSettingPacket(writeData);
	}


	// Check Ble Setting we received, if not equal , re-send Ble Setting
	private void checkBleSetting(final byte[] receiveData)
	{
		LogUtil.d(TAG, "[Process] checkBleSetting",Thread.currentThread().getStackTrace());
		appendLog(formatSendString("checkBleSetting"));
		// Ble setting local
		final byte[] dataLocal = readBleConfiguration();

		// Ble setting from device
		final byte[] dataDevice = subByteArray(receiveData,3,10);


		if(dataLocal == null || dataLocal.length == 0) return;

		if(dataDevice == null || dataDevice.length == 0)
		{
			reSendBleSetting();
			return;
		}

		for(int i=0 ; i<10;i++)
		{
			if(dataLocal[i] != dataDevice[i])
			{
				reSendBleSetting();
				return;
			}
		}

		// Packet parameter is 1
		// We can get mobile number from data
		if(receiveData[PACKET_PARAMETER] == 1)
		{
			final byte[] mobileNum = subByteArray(receiveData,13,1);
			final SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
			editor.putString(Constants.CONFIG_ITEM_MOBILE_NUMBER,String.valueOf(((int)(mobileNum[0])))).apply();
		}

        initDeviceState(); // Check information
	}

	private void sendBleSettingPacket(final byte[] writeData)
	{
		try
		{
			// Fill data, the first byte fill DATA_SLAVE_TAG, others random number
			byte[] data = readBleConfiguration();

			// If mDeviceAuthenticated is true
			fillData(writeData,data);

			// Fill check sum
			writeData[PACKET_CHECK_SUM] = getCheckSum(writeData);

			// send data
			sendPlainData(writeData);
		}
		catch (Exception e)
		{
			LogUtil.e(TAG, e.toString(),Thread.currentThread().getStackTrace());
		}
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


	private void sendTxPowerSetting(final int value)
	{
		try
		{
			if( getConnectionState() != BluetoothProfile.STATE_CONNECTED) return;

			final byte[] writeData = new byte[PACKET_LENGTH];
			writeData[PACKET_ID] = getRandom(255);
			writeData[PACKET_COMMAND] = CMD_TX_POWER;
			writeData[PACKET_PARAMETER] = (byte)value;

			final byte[] data = generateRandomArray(16,255);
			fillData(writeData,data);

			// fill check sum
			writeData[PACKET_CHECK_SUM] = getCheckSum(writeData);


			final String s = formatByteArrayToLog(writeData);
			LogUtil.d(TAG, "[Process] Set Tx Power  : " + s,Thread.currentThread().getStackTrace());
			appendLog(formatSendString("Set Tx Power  : " + String.format("0x%02X", value)));


            mWriteTxPower = true;
			sendPlainData(writeData);
		}
		catch (Exception e)
		{
			LogUtil.e(TAG, e.toString(),Thread.currentThread().getStackTrace());
		}
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

	private void sendPlainData(final byte[] data)
	{
		appendLog(formatByteArrayToLog(data));
		if(getWriteGattCharacteristic() != null)
		{
			sendPacket(data);
		}
		else
		{
			LogUtil.e(TAG,"[Error] Can not get write characteristic ",Thread.currentThread().getStackTrace());
		}
	}

	private void fillData(final byte[] outputPacket,byte[] data)
	{
	 	setDataField(outputPacket,data);
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
		for(int i= 0;i<length;i++)
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
		if(mIpcCallbackHandler != null)
		{
			mIpcCallbackHandler.sendMessage(message);
		}
	}

    private Intent getScanResultIntent()
    {
        final Intent intent = new Intent();
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
        intent.putExtra("param",PARAM_SCAN_RESULT);
        intent.putExtra("device_scanned",deviceList);

		final Message message = new Message();
		message.arg1 = PARAM_SCAN_RESULT;
		message.obj = deviceList;
		if(mIpcCallbackHandler != null)
		{
			mIpcCallbackHandler.sendMessage(message);
		}

        return intent;
    }

    private Intent getGattIntent(final int param)
    {
        final Intent intent = new Intent();
        intent.putExtra("param",param);
        return intent;
    }


	private Intent getThermalIntent(final int mode, final String... param)
	{
		try
		{
			final Intent intent = new Intent();
			intent.setAction(ACTION_DEBUG_SERVICE_TO_UI);
            intent.putExtra("param",PARAM_THERMAL);
			intent.putExtra("mode",mode);
			int paramIndex = 0;
			if((mode & 0x1) > 0)
			{
				intent.putExtra("first_connect",param[paramIndex]);
				paramIndex++;
			}

			if((mode & 0x2) > 0)
			{
				intent.putExtra("last_connect",param[paramIndex]);
				paramIndex++;
			}

			if((mode & 0x4) > 0)
			{
				intent.putExtra("first_disconnect",param[paramIndex]);
				paramIndex++;
			}

			if((mode & 0x8) > 0)
			{
				intent.putExtra("last_disconnect",param[paramIndex]);
				paramIndex++;
			}

			if((mode & 0x10) > 0)
			{
				intent.putExtra("thermal_command_count",param[paramIndex]);
				paramIndex++;
			}

			if((mode & 0x20) > 0)
			{
				intent.putExtra("thermal_csta_count",param[paramIndex]);
				paramIndex++;
			}

			// Disconnect count
			if((mode & 0x40) > 0)
			{
				intent.putExtra("disconnect_count",param[paramIndex]);
				paramIndex++;
			}

            return intent;
		}
		catch (Exception e)
		{
			LogUtil.e(TAG,e.toString(),Thread.currentThread().getStackTrace());
		}

        return null;
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

	private Intent getTuningResultIntent(final int tuningResult,final int[] rssiRecord,final int[] avgRssiRecord, final int avgRssi)
	{
        final Intent intent = new Intent();
        intent.putExtra("param",PARAM_TUNING_RESULT);
		intent.putExtra("tuningResult", tuningResult);
		if(avgRssi < 0)
        {
            intent.putExtra("avgRssi",avgRssi);
        }
		intent.putExtra("rssiRecord", sortArray(rssiRecord,rssiRecord.length,mRealTimeRssiRecordIndex));
		intent.putExtra("avgRssiRecord",sortArray(avgRssiRecord,avgRssiRecord.length,mAvgRssiRecordIndex));

        return intent;
	}

	private Intent getStableRssiRecordIntent(final int[] rssiRecord)
	{
        final Intent intent = new Intent();
        intent.putExtra("param",PARAM_STABLE_RSSI_RECORD);
		intent.putExtra("rssiRecord", rssiRecord);
        return intent;
	}

	private Intent getProcessStepIntent(final int step)
	{
        final Intent intent = new Intent();
        intent.putExtra("param",PARAM_PROCESS_STEP);
        intent.putExtra("step",step);
        return intent;
	}

    private void broadcastCharacteristicData(final String action,byte[] dataArray)
	{
        final Intent intent = new Intent(action);


        // writes the data formatted in HEX.
        if (dataArray != null && dataArray.length > 0)
        {
            intent.putExtra(EXTRA_DATA, new String(dataArray));

            String binaryData = ("******************* Data *******************") + NEW_LINE_CHARACTER + formatByteArrayToLog(dataArray)+ NEW_LINE_CHARACTER + "********************************************";
            intent.putExtra(EXTRA_BINARY_DATA, formatReceiveString(binaryData));
        }

        broadcastIntent(intent);
    }

    // Register a receiver to receive message from UI activity
	private void registerBleOperateReceiver()
	{
		try
		{
			final IntentFilter intentFilter = new IntentFilter();
			intentFilter.addAction(ACTION_UI_NOTIFY_SERVICE);
			intentFilter.addAction(ACTION_DEBUG_UI_SERVICE);

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
				if(mDeviceInitState == INIT_STATE_BONDING || mDeviceInitState == INIT_STATE_BOND_TO_CONNECT)
				{
					LogUtil.d(TAG,"After bonding , wait for disconnect ",Thread.currentThread().getStackTrace());
					return;
				}
				// Check if it is a bonded device when we receiving ACL_CONNECTED message
				// If it is not bonded, it is the first ACL_CONNECTED during bonding or not what we want
				if(bluetoothDevice.getBondState() == BluetoothDevice.BOND_BONDED)
				{
					LogUtil.d(TAG,"addConnectedBluetoothDevice , start connect device",Thread.currentThread().getStackTrace());
					mHandler.postDelayed(new Runnable()
					{
						@Override
						public void run()
						{
							connectDevice(bluetoothDevice.getAddress());
						}
					},3000);
				}
			}
		}
	}

	public void removeConnectedBluetoothDevice(final BluetoothDevice bluetoothDevice)
	{
		if(bluetoothDevice == null)
			return;

		if(mConnectedDeviceList != null)
		{
			for(int i=0;i<mConnectedDeviceList.size();i++)
			{
				final BluetoothDevice device = mConnectedDeviceList.get(i);
				if(device.getAddress().equals(bluetoothDevice.getAddress()))
				{
					mConnectedDeviceList.remove(i);

					removeBluetoothDeviceFromCache(device.getAddress());
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

	public void removeBluetoothDeviceFromCache(final String address)
	{
		if(address == null)
			return;

		if(mCachedDeviceList != null)
		{
			for(int i=0;i<mCachedDeviceList.size();i++)
			{
				final CachedBluetoothDevice device = mCachedDeviceList.get(i);
				final BluetoothDevice bluetoothDevice = device.bluetoothDevice;
				if(bluetoothDevice != null && bluetoothDevice.getAddress().equals(address))
				{
					mCachedDeviceList.remove(i);
				}
			}
		}
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

	private String getLastDeviceName()
	{
		final SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.CONFIG_FILE_BLE_SETTING, Context.MODE_PRIVATE);

		return sharedPreferences.getString(Constants.CONFIG_ITEM_BLE_DEVICE_NAME, "--");
	}

	private int getKeyLessThreshold()
	{
		final SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.CONFIG_FILE_BLE_SETTING, Context.MODE_PRIVATE);
		return sharedPreferences.getInt(Constants.CONFIG_ITEM_KEY_LESS_THRESHOLD, 0);
	}

	private void getKeyLessArmSetting()
	{
		final long setting = readBleSetting();
		mKeyLessArmAvailable = BleConfiguration.getValue(setting,BleConfiguration.BLE_SETTING_KEY_LES_ARM,BleConfiguration.MASK_BIT)==1?true:false;
	}

	private void getKeyLessDisarmSetting()
	{
		final long setting = readBleSetting();
		mKeyLessDisarmAvailable = BleConfiguration.getValue(setting,BleConfiguration.BLE_SETTING_KEY_LES_DISARM,BleConfiguration.MASK_BIT)==1?true:false;
	}

	private void saveKeyLessThreshold(final int value)
	{
		final SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.CONFIG_FILE_BLE_SETTING, Context.MODE_PRIVATE);
		final SharedPreferences.Editor editor = sharedPreferences.edit();

		editor.putInt(Constants.CONFIG_ITEM_KEY_LESS_THRESHOLD, value);
		editor.commit();
	}

	private byte[] readBleConfiguration()
	{
		final long infoValue = readBleInfo();
		final long settingValue = readBleSetting();

		final byte[] byteInfo = longToBytes(infoValue);
		final byte[] byteSetting = longToBytes(settingValue);

		final byte[] configuration = new byte[16];
		for(int i=0; i<5; i++)
		{
			configuration[i] = byteInfo[i];
		}
		for(int i=0; i<5; i++)
		{
			configuration[5+i] = byteSetting[i];
		}

//		for(int i=0; i<6; i++)
//		{
//			configuration[10+i] = getRandom(255);
//		}

		return configuration;
	}

	private boolean saveBleConfiguration(final byte[] data)
	{
		if(data == null || data.length == 0) return false;

		final byte[] tempInfo = subByteArray(data,0,5);
		final byte[] tempSetting = subByteArray(data,5,5);

		final byte[] info = new byte[8];
		for(int i=0;i<tempInfo.length;i++)
			info[i]=tempInfo[i];

		final byte[] setting = new byte[8];
		for(int i=0;i<tempSetting.length;i++)
			setting[i]=tempSetting[i];

		saveBleInfo(byteArrayToLong(info));
		saveBleSetting(byteArrayToLong(setting));

		return true;
	}

	private long readBleSetting()
	{
		final SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.CONFIG_FILE_BLE_SETTING, Context.MODE_PRIVATE);
		return sharedPreferences.getLong(Constants.CONFIG_ITEM_BLE_SETTING,0);
	}

	private long readBleInfo()
	{
		final SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.CONFIG_FILE_BLE_SETTING, Context.MODE_PRIVATE);
		return sharedPreferences.getLong(Constants.CONFIG_ITEM_BLE_INFO,0);
	}

	private void saveBleSetting(final long data)
	{
		final SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.CONFIG_FILE_BLE_SETTING, Context.MODE_PRIVATE);
		sharedPreferences.edit().putLong(Constants.CONFIG_ITEM_BLE_SETTING,data).apply();
	}

	private void saveBleInfo(final long data)
	{
		final SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.CONFIG_FILE_BLE_SETTING, Context.MODE_PRIVATE);
		sharedPreferences.edit().putLong(Constants.CONFIG_ITEM_BLE_INFO,data).apply();
	}

	private void saveTxPowerValue(final int value)
    {
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        final int lastIndex = Integer.parseInt( sharedPreferences.getString(getString(R.string.pref_key_tx_power),"-1"));

        // Get index pf TX_POWER_VALUE_ARRAY by value
        int dbIndex = -1;
        for(int i=0;i<TX_POWER_VALUE_ARRAY.length;i++)
        {
            if(value == TX_POWER_VALUE_ARRAY[i])
            {
                dbIndex = i;
                break;
            }
        }

        // if index changed and new index is valid, save it
        if(dbIndex != lastIndex && dbIndex != -1)
            sharedPreferences.edit().putString(getString(R.string.pref_key_tx_power),dbIndex+"").apply();
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
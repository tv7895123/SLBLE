package com.startline.slble.Receiver;

import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import com.startline.slble.Service.BluetoothLeIndependentService;
import com.startline.slble.Util.LogUtil;

/**
 * Created by terry on 2016/1/25.
 * For stabling bluetooth connection in APP, APP must after OS connect to bluetooth then start bind
 * Ex. SLBLE connected with OS, APP receive ACL_CONNECTED message then APP start binding
 *
 * To improve bonding process in APP, register a receiver to listen bluetooth ACL messages:
 * 1.Auto start BluetoothLeIndentService when bluetooth turned on
 * 2.Clear connected and cached device list when bluetooth turned off
 * 3.Receive ACL_CONNECTED message, add device to lists and start binding
 * 4.Receive ACL_DISCONNECTED message, remove device from lists
 * 5.After APP bind SLBLE success, add device to cache list
 */
public class BluetoothReceiver extends BroadcastReceiver
{
    private final String TAG = "BluetoothReceiver";
    private final String BLUETOOTH_INPUT_DEVICE_STATE_CHANGED = "android.bluetooth.input.profile.action.CONNECTION_STATE_CHANGED";
    private Handler mHandler = new Handler();
    private BluetoothLeIndependentService mService = null;

    private Runnable mThreadRunnable = null;
    private static Thread mThread = null;

    @Override
    public void onReceive(final Context context, final Intent intent)
    {
        LogUtil.d(TAG,intent.getAction(), Thread.currentThread().getStackTrace());

        // If not support BLE, do nothing
        if(!isSupportBLE(context))
        {
            return;
        }

        mService = getIvLinkService(context);
        if(mService == null)
        {
            LogUtil.d(TAG,"Can't get BluetoothService", Thread.currentThread().getStackTrace());
            return;
        }

        final String action = intent.getAction();
        // Bluetooth state change
        if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED))
        {
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            LogUtil.d(TAG,"ACTION_STATE_CHANGED = " + state, Thread.currentThread().getStackTrace());
            switch (state)
            {
                case BluetoothAdapter.STATE_OFF:
                {

                }
                break;
                case BluetoothAdapter.STATE_TURNING_OFF:
                {
                    mService.clearCachedBluetoothDevice();

                    mService.clearConnectedBluetoothDevice();

                    mService.initStopScan();

                    mService.handleBleDisconnect();
                }
                break;
                case BluetoothAdapter.STATE_ON:
                {
                    if(isServiceRunning(context,BluetoothLeIndependentService.class) == false)
                    {
                        LogUtil.d(TAG,"start Service",Thread.currentThread().getStackTrace());
                        startService(context);
                    }

                    handleBluetoothTurnOn();
                }
                break;
                case BluetoothAdapter.STATE_TURNING_ON:
                {
                    mService.setAllowConnect(true);
                }
                break;
            }
        }
        else if(action.equals(BluetoothDevice.ACTION_ACL_CONNECTED)
                || action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED))
        {
            final BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if(bluetoothDevice == null) return;

            final String deviceName = bluetoothDevice.getName();
            if(deviceName == null) return;

            LogUtil.i(TAG, String.format("[ %s ] - %s %s",action.equals(BluetoothDevice.ACTION_ACL_CONNECTED)?"Connect":"Disconnect",deviceName, bluetoothDevice.getAddress()), Thread.currentThread().getStackTrace());
            //if(deviceName.startsWith(BluetoothLeIndependentService.KEYWORD_SLBLE))
            {
                handleBluetoothAclEvent(context,intent.getAction(),bluetoothDevice);
            }
        }
        else if(action.equals(BLUETOOTH_INPUT_DEVICE_STATE_CHANGED))
        {
            final int prevState = intent.getIntExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE,0);
            final int newState = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, 0);
            final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            LogUtil.i(TAG,String.format("PrevState = %d, NewState = %d, Device : %s", prevState, newState, device==null?"NULL" : device.getAddress()), Thread.currentThread().getStackTrace());
        }
    }

    private void handleBluetoothTurnOn()
    {
        try
        {
            // Do something when Bluetooth turned on
            mService.reInitialize();
        }
        catch (Exception e)
        {
            LogUtil.e(TAG,e.toString(), Thread.currentThread().getStackTrace());
        }
    }

    synchronized private void handleBluetoothAclEvent(final Context context, final String action, final BluetoothDevice bluetoothDevice)
    {
        LogUtil.d(TAG,"handleBluetoothAclEvent = " + action, Thread.currentThread().getStackTrace());

        switch (action)
        {
            case BluetoothDevice.ACTION_ACL_CONNECTED:
            {
                mThreadRunnable = new Runnable()
                {
                    @Override
                    public void run()
                    {
                        mService.addConnectedBluetoothDevice(bluetoothDevice);
                    }
                };
                setThread(mThreadRunnable);
                mThread.start();
            }
            break;

            case BluetoothDevice.ACTION_ACL_DISCONNECTED:
            {
                mThreadRunnable = new Runnable()
                {
                    @Override
                    public void run()
                    {
                        mService.removeConnectedBluetoothDevice(bluetoothDevice);
                    }
                };
                setThread(mThreadRunnable);
                mThread.start();
            }
            break;
        }
    }

    private void setThread(final Runnable runnable)
    {
        if(mThread != null)
        {
            mThread.interrupt();
            mThread = null;
        }

        if(runnable != null)
        {
            mThread = new Thread(runnable);
        }
    }

    protected boolean isSupportBLE(final Context context)
    {
        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
        {
            //Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            final DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    //finish();
                }
            };
            //DialogUtil.messageDialog(context,getString(R.string.app_name),getString(R.string.ble_not_supported),onClickListener,null,R.string.ok,0);
            return false;
        }
        else
        {
            // Support BLE function
            return true;
        }
    }

    private BluetoothLeIndependentService getIvLinkService(final Context context)
    {
        if(!isServiceRunning(context,BluetoothLeIndependentService.class))
        {
            startService(context);
        }

        if(mService == null)
        {
            mService = BluetoothLeIndependentService.getInstance();
        }

        return mService;
    }

    private void startService(final Context context)
    {
        final Intent intent = new Intent();
        intent.setClass(context, BluetoothLeIndependentService.class);
        context.startService(intent);
    }

    private void stopService(final Context context)
    {
        final Intent intent = new Intent();
        intent.setClass(context, BluetoothLeIndependentService.class);
        context.stopService(intent);
    }

    private boolean isServiceRunning(final Context context, final Class serviceClass)
    {
        boolean running = false;
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE))
        {
            if (serviceClass.getName().equalsIgnoreCase(service.service.getClassName())
                    && service.service.getPackageName().equalsIgnoreCase(context.getPackageName()))
            {
                running = true;
            }
        }
        return running;
    }
}
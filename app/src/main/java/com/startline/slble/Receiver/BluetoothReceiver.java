package com.startline.slble.Receiver;

import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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
 */
public class BluetoothReceiver extends BroadcastReceiver
{
    private final String TAG = "BluetoothReceiver";
    private Handler mHandler = new Handler();
    private static Runnable mRunnablePostPoneConnect = null;
    private static Runnable mRunnablePostPoneDisconnect = null;

    @Override
    public void onReceive(final Context context, final Intent intent)
    {
        //LogUtil.d("BluetoothReceiver",intent.getAction(), Thread.currentThread().getStackTrace());

        // If not support BLE, do nothing
        if(!isSupportBLE(context))
        {
            return;
        }

        final String action = intent.getAction();
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
                    if(isServiceRunning(context,BluetoothLeIndependentService.class))
                    {
                        final BluetoothLeIndependentService service = BluetoothLeIndependentService.getInstance();
                        if(service != null)
                        {
                            service.clearConnectedBluetoothDevice();
                        }
                    }
                }
                break;
                case BluetoothAdapter.STATE_ON:
                {
                    BluetoothLeIndependentService service = null;

                    if(isServiceRunning(context,BluetoothLeIndependentService.class) == false)
                    {
                        LogUtil.d("BluetoothReceiver","startService", Thread.currentThread().getStackTrace());
                        startService(context);
                    }

                    service = BluetoothLeIndependentService.getInstance();

                    if(service != null)
                    {
                        handleBluetoothTurnOn(service);
                    }
                    else
                    {
                        mHandler.postDelayed(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                final BluetoothLeIndependentService service = BluetoothLeIndependentService.getInstance();
                                if(service != null)
                                {
                                    handleBluetoothTurnOn(service);
                                }
                                else
                                {
                                    LogUtil.d(TAG,"BluetoothLeIndependentService not running", Thread.currentThread().getStackTrace());
                                }
                            }
                        },1000);
                    }
                }
                break;
                case BluetoothAdapter.STATE_TURNING_ON:

                    break;
            }
        }
        else
        {
            final BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if(bluetoothDevice == null) return;

            final String deviceName = bluetoothDevice.getName();
            if(deviceName == null) return;

            if(deviceName.equals(BluetoothLeIndependentService.KEYWORD_SLBLE))
            {
                int delayTimer = 0;
                int tryStartService = 0;
                while(!isServiceRunning(context,BluetoothLeIndependentService.class) && tryStartService<2)
                {
                    startService(context);
                    delayTimer = 600;
                    tryStartService++;
                }


                if(isServiceRunning(context,BluetoothLeIndependentService.class))
                {
                    mHandler.postDelayed(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            handleBluetoothAclEvent(context,intent.getAction(),bluetoothDevice);
                        }
                    },delayTimer);
                }
                else
                {
                    LogUtil.d(TAG,"Can not start BluetoothLeIndependentService!!", Thread.currentThread().getStackTrace());
                }
            }
        }
    }

    private void handleBluetoothTurnOn(final BluetoothLeIndependentService service)
    {
        try
        {
            // Do something when Bluetooth turned on
            service.reInitialize();
        }
        catch (Exception e)
        {
            LogUtil.e(TAG,e.toString(), Thread.currentThread().getStackTrace());
        }
    }

    synchronized private void handleBluetoothAclEvent(final Context context, final String action, final BluetoothDevice bluetoothDevice)
    {
        LogUtil.d(TAG,"handleBluetoothAclEvent = " + action, Thread.currentThread().getStackTrace());
        final BluetoothLeIndependentService service = BluetoothLeIndependentService.getInstance();
        if(service == null)
            return;
        switch (action)
        {
            case BluetoothDevice.ACTION_ACL_CONNECTED:
            {
                removeCallbacks();

                mRunnablePostPoneConnect = new Runnable()
                {
                    @Override
                    public void run()
                    {
                        mRunnablePostPoneConnect = null;
                        service.addConnectedBluetoothDevice(bluetoothDevice);
                    }
                };

                mHandler.postDelayed(mRunnablePostPoneConnect,0);
            }
            break;

            case BluetoothDevice.ACTION_ACL_DISCONNECTED:
            {
                removeCallbacks();

                mRunnablePostPoneDisconnect = new Runnable()
                {
                    @Override
                    public void run()
                    {
                        mRunnablePostPoneDisconnect = null;
                        if(isServiceRunning(context,BluetoothLeIndependentService.class) )
                        {
                            service.removeConnectedBluetoothDevice(bluetoothDevice);
                        }
                    }
                };
                mHandler.postDelayed(mRunnablePostPoneDisconnect,0);
            }
            break;
        }
    }

    private void removeCallbacks()
    {
        if(mRunnablePostPoneConnect != null)
        {
            mHandler.removeCallbacks(mRunnablePostPoneConnect);
            mRunnablePostPoneConnect = null;
        }

        if(mRunnablePostPoneDisconnect != null)
        {
            mHandler.removeCallbacks(mRunnablePostPoneDisconnect);
            mRunnablePostPoneDisconnect = null;
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

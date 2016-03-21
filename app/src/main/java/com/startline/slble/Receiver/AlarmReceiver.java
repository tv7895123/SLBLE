package com.startline.slble.Receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;
import com.startline.slble.Service.BluetoothLeIndependentService;
import com.startline.slble.Util.LogUtil;

/**
 * Created by terry on 2015/7/17.
 */
public class AlarmReceiver extends BroadcastReceiver
{
	@Override
	public void onReceive(Context arg0, Intent arg1)
	{
		LogUtil.d("com.startline.slble","Alarm Receiver start connect device", Thread.currentThread().getStackTrace());

		final BluetoothLeIndependentService service  = BluetoothLeIndependentService.getInstance();
	}
}

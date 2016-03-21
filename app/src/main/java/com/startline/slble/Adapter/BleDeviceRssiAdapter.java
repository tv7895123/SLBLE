package com.startline.slble.Adapter;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.startline.slble.PureClass.BleDeviceRssi;
import com.startline.slble.R;

import java.util.ArrayList;

/**
 * Created by terry on 2015/3/31.
 */
 // Adapter for holding devices found through scanning.
public class BleDeviceRssiAdapter extends BaseAdapter
{
	private final long EXPIRED_TIME = 10000;
	private ArrayList<BleDeviceRssi> mBleDeviceList;
	private Context context;


	class ViewHolder
	{
        TextView deviceName;
        TextView deviceAddress;
		TextView deviceRssi;
    }

	public BleDeviceRssiAdapter(final Context context)
	{
		super();
		mBleDeviceList = new ArrayList<BleDeviceRssi>();
		this.context = context;
	}

	public void addDevice(final BluetoothDevice bluetoothDevice,final String name,final int rssi)
	{
		final BleDeviceRssi bluetoothDeviceRssi = new BleDeviceRssi(bluetoothDevice,name,rssi,System.currentTimeMillis());
		boolean contains = false;

		for(int i=0;i<mBleDeviceList.size();i++)
		{
			final BleDeviceRssi bleDeviceRssi = mBleDeviceList.get(i);
			final BluetoothDevice device = bleDeviceRssi.bluetoothDevice;
			if(device.getAddress().equalsIgnoreCase(bluetoothDevice.getAddress()))
			{
				mBleDeviceList.set(i,bluetoothDeviceRssi);
				contains = true;
				break;
			}
		}

		if(contains == false)
			mBleDeviceList.add(bluetoothDeviceRssi);
	}

	public BleDeviceRssi getDevice(int position)
	{
		return mBleDeviceList.get(position);
	}

	public void clear()
	{
		mBleDeviceList.clear();
	}

	@Override
	public int getCount()
	{
		return mBleDeviceList.size();
	}

	@Override
	public Object getItem(int i)
	{
		return mBleDeviceList.get(i);
	}

	@Override
	public long getItemId(int i)
	{
		return i;
	}


	// Remove device from mBleDeviceList if it is expired
	public void removeUnavailableDevice()
	{
		if(mBleDeviceList == null || mBleDeviceList.size() == 0) return;

		int totalDeviceNum = mBleDeviceList.size();
		for(int i=0; i< totalDeviceNum;)
		{
			final BleDeviceRssi bleDeviceRssi = mBleDeviceList.get(i);
			final long diff = System.currentTimeMillis()-bleDeviceRssi.createdTime;

//			if(bleDeviceRssi.nameUTF8.length() >= 15)
//			{
//				bleDeviceRssi.nameUTF8 = bleDeviceRssi.nameUTF8.substring(0,15) + String.format(" [ %d ]",diff/1000);
//			}

			if(diff > EXPIRED_TIME)
			{
				mBleDeviceList.remove(i);
				totalDeviceNum--;
			}
			else
			{
				i++;
			}
		}
	}

	@Override
	public View getView(int i, View view, ViewGroup viewGroup)
	{
		ViewHolder viewHolder;
		// General ListView optimization code.
		if (view == null)
		{
			view = View.inflate(context, R.layout.device_scanned, null);
			viewHolder = new ViewHolder();
			viewHolder.deviceAddress = (TextView) view.findViewById(R.id.txt_address);
			viewHolder.deviceName = (TextView) view.findViewById(R.id.txt_name);
			viewHolder.deviceRssi = (TextView)view.findViewById(R.id.txt_rssi);
			view.setTag(viewHolder);
		}
		else
		{
			viewHolder = (ViewHolder) view.getTag();
		}

		final BleDeviceRssi deviceRssi = mBleDeviceList.get(i);

		final String deviceName = deviceRssi.bluetoothDevice.getName();
		if (deviceName != null && deviceName.length() > 0)
			viewHolder.deviceName.setText(deviceName);
		else
			viewHolder.deviceName.setText(R.string.unknown_device);

		viewHolder.deviceAddress.setText(deviceRssi.bluetoothDevice.getAddress());

		viewHolder.deviceRssi.setText(deviceRssi.rssi+" mdB");

		return view;
	}
}

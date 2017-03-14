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
		{
			mBleDeviceList.add(bluetoothDeviceRssi);
		}
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

	public boolean isContainDevice(final String address)
	{
		if(mBleDeviceList == null || mBleDeviceList.size() == 0)
			return false;

		for(int i=0;i<mBleDeviceList.size();i++)
		{
			final BleDeviceRssi device = mBleDeviceList.get(i);
			if(device.bluetoothDevice.getAddress().equals(address))
				return true;
		}

		return false;
	}

	public long getDeviceUpdateTime(final String address)
	{
		if(mBleDeviceList == null || mBleDeviceList.size() == 0)
			return -1;

		for(int i=0;i<mBleDeviceList.size();i++)
		{
			final BleDeviceRssi device = mBleDeviceList.get(i);
			if(device.bluetoothDevice.getAddress().equals(address))
			{
				return System.currentTimeMillis()-device.createdTime;
			}
		}

		return -1;
	}

	public void sortList()
	{
		if(mBleDeviceList.size()>1)
			quickSort(mBleDeviceList,0,mBleDeviceList.size()-1);
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

	private void swap (ArrayList<BleDeviceRssi> list, final int i, final int j)
	{
		BleDeviceRssi tmpI = list.get(i);
		BleDeviceRssi tmpJ = list.get(j);
		list.set(i,tmpJ);
		list.set(j,tmpI);
	};

	private void quickSort(ArrayList<BleDeviceRssi> list,final int indexLeft, final int indexRight)
	{
		//--------------------------------------------------------------------------------------------------------------
		// middle pivot
		//--------------------------------------------------------------------------------------------------------------
		//*
		if(indexLeft >= indexRight)
			return;
		int pivotIndex = (indexLeft+indexRight)/2;
		BleDeviceRssi pivot = list.get(pivotIndex);
		swap(list, pivotIndex, indexRight);
		int swapIndex = indexLeft;
		for (int i = indexLeft; i < indexRight; ++i)
		{
			if (list.get(i).rssi >= pivot.rssi)
			{
				swap (list, i, swapIndex);
				++swapIndex;
			}
		}
		swap(list, swapIndex, indexRight);

		quickSort(list, indexLeft, swapIndex - 1);
		quickSort(list, swapIndex + 1, indexRight);
		//*/


		//--------------------------------------------------------------------------------------------------------------
		// Left pivot
		//--------------------------------------------------------------------------------------------------------------
		/*
		if(indexLeft >= indexRight)
			return;

		int i, j;
		BleDeviceRssi pivot = list.get(indexLeft);

		i = indexLeft + 1;
		j = indexRight;

		while (true)
		{
			while (i <= indexRight)
			{
				if (list.get(i).rssi < pivot.rssi)
				{
					break;
				}
				i = i + 1;
			}

			while (j > indexLeft)
			{
				if (list.get(j).rssi > pivot.rssi)
				{
					break;
				}
				j = j - 1;
			}

			if (i > j)
				break;

			swap(list,indexLeft,indexRight);
		}

		swap(list,indexLeft, j);
		quickSort(list, indexLeft, j - 1);
		quickSort(list, j + 1, indexRight);
		//*/
	}
}

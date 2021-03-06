package com.startline.slble.Adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;
import com.startline.slble.PureClass.BleSerializableDevice;
import com.startline.slble.R;

import java.util.ArrayList;

/**
 * Created by terry on 2015/3/31.
 */
 // Adapter for holding devices found through scanning.
public class BleSerializableDeviceAdapter extends BaseAdapter
{
	public static final int DEVICE_TYPE_SCAN = 0;
	public static final int DEVICE_TYPE_BOND = 1;

	private ArrayList<BleSerializableDevice> mLeDevices;
	private Context context;
	private int deviceType = DEVICE_TYPE_SCAN;
	private View.OnClickListener onClickListener = null;

	class ViewHolder
	{
        TextView deviceName;
        TextView deviceAddress;
		TextView deviceRssi;

		Button btnUnbond;
    }

	public BleSerializableDeviceAdapter(final Context context)
	{
		super();
		mLeDevices = new ArrayList<BleSerializableDevice>();
		this.context = context;
	}

	public BleSerializableDeviceAdapter(final Context context,final int deviceType)
	{
		super();
		mLeDevices = new ArrayList<BleSerializableDevice>();
		this.context = context;
		this.deviceType = deviceType;
	}

	public void setDevice(final ArrayList list)
	{
		if(list != null)
		{
			mLeDevices = list;
		}
	}

	public void setUnBondOnClickListener(final View.OnClickListener onClickListener)
	{
		this.onClickListener = onClickListener;
	}



	public BleSerializableDevice getDevice(int position)
	{
		return mLeDevices.get(position);
	}

	public void clear()
	{
		mLeDevices.clear();
	}

	@Override
	public int getCount()
	{
		return mLeDevices.size();
	}

	@Override
	public Object getItem(int i)
	{
		return mLeDevices.get(i);
	}

	@Override
	public long getItemId(int i)
	{
		return i;
	}

	@Override
	public View getView(int i, View view, ViewGroup viewGroup)
	{
		ViewHolder viewHolder;
		// General ListView optimization code.
		if (view == null)
		{
			viewHolder = new ViewHolder();
			if(deviceType == DEVICE_TYPE_SCAN)
			{
				view = View.inflate(context, R.layout.device_scanned, null);
				viewHolder.deviceAddress = (TextView) view.findViewById(R.id.txt_address);
				viewHolder.deviceName = (TextView) view.findViewById(R.id.txt_name);
				viewHolder.deviceRssi = (TextView)view.findViewById(R.id.txt_rssi);
			}
			else
			{
				view = View.inflate(context, R.layout.device_bonded, null);
				viewHolder.deviceAddress = (TextView) view.findViewById(R.id.txt_address);
				viewHolder.deviceName = (TextView) view.findViewById(R.id.txt_name);
				viewHolder.btnUnbond = (Button)view.findViewById(R.id.btn_unbond);
				if(onClickListener != null)
				{
					viewHolder.btnUnbond.setOnClickListener(onClickListener);
				}
			}

			view.setTag(viewHolder);
		}
		else
		{
			viewHolder = (ViewHolder) view.getTag();
		}

		final BleSerializableDevice simpleBleDevice = mLeDevices.get(i);

		final String deviceName = simpleBleDevice.name;
		if (deviceName != null && deviceName.length() > 0)
			viewHolder.deviceName.setText(deviceName);
		else
			viewHolder.deviceName.setText(R.string.unknown_device);

		viewHolder.deviceAddress.setText(simpleBleDevice.address);

		if(deviceType == DEVICE_TYPE_SCAN)
		{
			viewHolder.deviceRssi.setText(simpleBleDevice.rssi + " dBm");
		}
		else
		{
			viewHolder.btnUnbond.setTag(simpleBleDevice.address);
		}

		return view;
	}
}

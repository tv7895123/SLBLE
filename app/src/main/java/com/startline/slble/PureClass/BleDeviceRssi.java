package com.startline.slble.PureClass;

import android.bluetooth.BluetoothDevice;

import java.io.Serializable;

/**
 * Created by terry on 2015/4/7.
 */
public class BleDeviceRssi
{
	public BluetoothDevice bluetoothDevice;
	public int rssi;
	public String nameUTF8;
	public long createdTime;

	public BleDeviceRssi(final BluetoothDevice device, final String name, final int rssi,final long time)
	{
		bluetoothDevice = device;
		this.rssi = rssi;
		nameUTF8 = name;
		createdTime = time;
	}

	public BleDeviceRssi(final BluetoothDevice device, final int rssi)
	{
		bluetoothDevice = device;
		this.rssi = rssi;
		nameUTF8 = "Unknown";
	}
}

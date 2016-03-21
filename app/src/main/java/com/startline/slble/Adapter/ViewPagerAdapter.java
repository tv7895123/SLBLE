package com.startline.slble.Adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import com.startline.slble.Fragment.DeviceConnectFragment;
import com.startline.slble.Fragment.DeviceStatusFragment;

import java.util.ArrayList;

/**
 * Created by terry on 2015/8/5.
 */
public class ViewPagerAdapter extends FragmentPagerAdapter
{
	private static DeviceConnectFragment mDeviceConnectFragment = null;
	private static DeviceStatusFragment mDeviceStatusFragment = null;
	private static ArrayList<Fragment> mFragmentList = null;

    public ViewPagerAdapter(FragmentManager fm)
    {
        super(fm);
    }

	public ViewPagerAdapter(FragmentManager fm, ArrayList<Fragment> fragmentList)
    {
        super(fm);
		mFragmentList = fragmentList;
    }

    @Override
    public android.support.v4.app.Fragment getItem(int index)
    {
    	switch (index)
		{
		   case 0:
			  return mFragmentList.get(0);
		   case 1:
			  return mFragmentList.get(1);
		}

        return null;
    }

	@Override
	public int getItemPosition(Object object)
	{
		return mFragmentList.indexOf(object);
	}

	public DeviceConnectFragment getDeviceConnectFragment()
	{
		if(mDeviceConnectFragment == null)
			mDeviceConnectFragment = new DeviceConnectFragment();

		return mDeviceConnectFragment;
	}

	public DeviceStatusFragment getDeviceStatusFragment()
	{
		if(mDeviceStatusFragment == null)
			mDeviceStatusFragment = new DeviceStatusFragment();

		return mDeviceStatusFragment;
	}

    @Override
    public int getCount()
    {
        return 2;
    }
}


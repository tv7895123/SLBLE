package com.startline.slble.PureClass;

import java.util.List;
import java.util.UUID;

/**
 * Created by terry on 2015/4/7.
 */
public class BleAdvertisedData
{
	private List<UUID> mUuids;
	private String mName;
	public BleAdvertisedData(List<UUID> uuids, String name){
		mUuids = uuids;
		mName = name;
	}

	public List<UUID> getUuids(){
		return mUuids;
	}

	public String getName(){
		return mName;
	}
}

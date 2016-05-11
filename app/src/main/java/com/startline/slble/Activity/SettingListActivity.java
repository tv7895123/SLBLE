package com.startline.slble.Activity;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.widget.*;
import com.startline.slble.Adapter.SettingListAdapter;
import com.startline.slble.PureClass.Constants;
import com.startline.slble.R;
import com.startline.slble.Service.BluetoothLeIndependentService;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.startline.slble.Adapter.SettingListAdapter.*;

/**
 * Created by terry on 2015/8/31.
 */
public class SettingListActivity extends Activity
{
	private String TAG = SettingListActivity.class.getName();

	private byte[] mBleSetting = null;
	private int[] titleArray = null;
	private int[] descriptionArray = null;

	private List<Integer> mTypeList = null;

	private Context context = null;
	private ListView listView = null;
	private SettingListAdapter settingListAdapter = null;

	private BluetoothLeIndependentService service = null;
	private Handler mHandler = new Handler();

	private final CompoundButton.OnCheckedChangeListener onCheckedChangeListener = new CompoundButton.OnCheckedChangeListener()
	{
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
		{
			final String title = (String)buttonView.getTag();
			if(title.equals(getString(R.string.title_auto_connect)))
			{
				if(service != null)
				{
					service.setAutoConnectOnDisconnect(isChecked);
					service.saveAppSetting();
				}
			}
			else if(title.equals(getString(R.string.title_auto_scroll)))
			{
				if(service != null)
				{
					service.setAutoScroll(isChecked);
					service.saveAppSetting();
				}
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.setting_list_activity);

		context = this;

		titleArray = new int[]
		{
			R.string.title_mobile_number
			,R.string.title_auto_connect
			,R.string.title_auto_scroll
		};

		descriptionArray = new int[]
		{
			R.string.description_mobile_number
			,R.string.description_auto_connect
			,R.string.description_auto_scroll
		};

		listView = (ListView)findViewById(R.id.list_view);

		mHandler.postDelayed(new Runnable()
		{
			@Override
			public void run()
			{
				service = BluetoothLeIndependentService.getInstance();

				initTypeList();

				updateListAdapter(true);
			}
		},initService());
	}

	private int initService()
	{
		int delayTime = 0;
		if(!isServiceRunning(BluetoothLeIndependentService.class))
			delayTime = 500;

		return delayTime;
	}

	private void initTypeList()
	{
		mTypeList = new ArrayList<Integer>();
		mTypeList.add(TYPE_TEXT);
		mTypeList.add(TYPE_CHECKBOX);
		mTypeList.add(TYPE_CHECKBOX);
	}

	private List<Map<String, Object>> getDataList()
	{
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();

		final String appSetting = service.readAppSetting();
		final byte[] bleSetting = service.readBleSetting();
		JSONObject jsonObject = null;
		try
		{
			if(appSetting.isEmpty())
			{
				for (int i = 0; i < mTypeList.size(); i++)
				{
					Map<String, Object> map = new HashMap<String, Object>();
					map.put("title", getDisplayString(titleArray[i]));
					map.put("description", getDisplayString(descriptionArray[i]));
					map.put("value",0);
					list.add(map);
				}
			}
			else
			{
				jsonObject = new JSONObject(appSetting);
				for (int i = 0; i < mTypeList.size(); i++)
				{
					Map<String, Object> map = new HashMap<String, Object>();
					map.put("title", getDisplayString(titleArray[i]));
					map.put("description", getDisplayString(descriptionArray[i]));
					if(i == 0)
					{
						map.put("value",(int)bleSetting[0]);
					}
					else
					{
						map.put("value",jsonObject.getInt(getDisplayString(titleArray[i])));
					}
					list.add(map);
				}
			}
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		return list;
	}

	private void updateListAdapter(final boolean dataEnabled)
	{
		if(settingListAdapter == null)
		{
			settingListAdapter = new SettingListAdapter(context,getDataList(),true,onCheckedChangeListener,null);
			settingListAdapter.setTypeList(mTypeList);
			listView.setAdapter(settingListAdapter);
		}
		else
		{
			settingListAdapter.setDataEnabled(dataEnabled);
			settingListAdapter.setDataList(getDataList());
			settingListAdapter.notifyDataSetChanged();
		}
	}

	private String getDisplayString(final int stringId)
	{
		if(stringId == 0)
		{
			return  "";
		}
		else
		{
			return getString(stringId);
		}
	}

	protected boolean isServiceRunning(Class serviceClass)
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

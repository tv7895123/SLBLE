package com.startline.slble.Activity;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import com.startline.slble.Adapter.SettingListAdapter;
import com.startline.slble.PureClass.Constants;
import com.startline.slble.PureClass.SlbleProtocol;
import com.startline.slble.R;
import com.startline.slble.Service.BluetoothLeIndependentService;
import com.startline.slble.Util.DialogUtil;
import com.startline.slble.Util.LogUtil;
import com.startline.slble.module.BleConfiguration;
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
	private int selectPosition = -1;
	private byte[] initSetting = null;
	private BleConfiguration mBleConfiguration = null;

	private List<Integer> mTypeList = null;

	private Context context = null;
	private ListView listView = null;
	private SettingListAdapter settingListAdapter = null;

	private BluetoothLeIndependentService service = null;
	private ProgressDialog mProgressDialog = null;
	private Menu mMenu = null;

	public static final String[] TX_POWER_LEVEL = new String[]
	{
			"-17 dB","-15 dB","-10 dB","-5 dB","0 dB","2 dB","4 dB","7 dB"
	};
	private final String[] KEYLESS_LEVEL = new String[]{"Off","Low","Middle","High"};
	private final String[] SLAVE_TAG = new String[]{"Off","On"};
	private final String[] BTR_MODE = new String[]{"BTR","BTR+Keyless"};

	private final CompoundButton.OnCheckedChangeListener onCheckedChangeListener = new CompoundButton.OnCheckedChangeListener()
	{
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
		{
			final String title = (String)buttonView.getTag();
			if(title.equals(getString(R.string.title_auto_connect)))
			{
				if(service != null
					//&& service.isDeviceInitialized()
					)
				{
					service.setAutoConnectOnDisconnect(isChecked);
					service.saveAppSetting();
				}
			}
			else if(title.equals(getString(R.string.title_auto_scroll)))
			{
				if(service != null
					//&& service.isDeviceInitialized()
				)
				{
					service.setAutoScroll(isChecked);
					service.saveAppSetting();
				}
			}
			else if(title.equals(getString(R.string.title_auto_send_test)))
			{
				if(service != null
					//&& service.isDeviceInitialized()
						)
				{
					service.setAutoTest(isChecked);
					service.saveAppSetting();
				}
			}
		}
	};

	private Handler mHandler = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			switch (msg.arg1)
			{
				case BluetoothLeIndependentService.PARAM_TX_POWER_LEVEL:
				{
					mProgressDialog.dismiss();
					mHandler.removeCallbacks(runnableTimeout);
					updateListAdapter(true);
				}
				break;
				case BluetoothLeIndependentService.PARAM_SETTING_INFORMATION:
				{
					mProgressDialog.dismiss();
					mHandler.removeCallbacks(runnableTimeout);

					if(msg.arg2 == SlbleProtocol.PARAM_SETTING_RESPONSE)
					{
						initSetting = (byte[])msg.obj;
						mBleConfiguration.setSetting(copyArray(initSetting));
						updateListAdapter(true);
						refreshMenu();
					}
					else if(msg.arg2 == SlbleProtocol.PARAM_SETTING_WRITE)
					{
						final byte[] data = (byte[])msg.obj;
						if(isEqual(data,mBleConfiguration.getSetting()))
						{
							initSetting = (byte[])msg.obj;
							mBleConfiguration.setSetting(copyArray(initSetting));
							updateListAdapter(true);
							refreshMenu();
						}
						else
						{
							Toast.makeText(context,"Save data failed.",Toast.LENGTH_SHORT).show();
						}
					}
				}
				break;
				case BluetoothLeIndependentService.PARAM_ERROR:
				{
					mProgressDialog.dismiss();
					mHandler.removeCallbacks(runnableTimeout);
					Toast.makeText(context,"Receive Error",Toast.LENGTH_SHORT).show();
				}
				break;
			}
		}
	};

	private Runnable runnableTimeout = new Runnable()
	{
		@Override
		public void run()
		{
			if(mProgressDialog.isShowing())
			{
				mProgressDialog.dismiss();
				Toast.makeText(context,"Device no response",Toast.LENGTH_LONG).show();
			}
		}
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.setting_activity_menu, menu);
		mMenu = menu;
		menu.findItem(R.id.action_save).setVisible(false);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// Handle presses on the action bar items
		switch (item.getItemId())
		{
			case R.id.action_save:
			{
				saveSetting();
			}
			return true;

			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.setting_list_activity);

		context = this;
		mProgressDialog = new ProgressDialog(context);
		titleArray = new int[]
		{
			R.string.title_mobile_number
			,R.string.title_auto_connect
			,R.string.title_auto_scroll

			,R.string.blank
			,R.string.title_tx_power_keyless_lock
			,R.string.title_tx_power_keyless_unlock
			,R.string.title_slave_tag_counter

			,R.string.blank
			,R.string.title_btr_mode
		};

		descriptionArray = new int[]
		{
			R.string.description_mobile_number
			,R.string.description_auto_connect
			,R.string.description_auto_scroll

			,R.string.blank
			,R.string.description_tx_power_keyless_lock
			,R.string.description_tx_power_keyless_unlock
			,R.string.description_slave_tag_counter

			,R.string.blank
			,R.string.description_btr_mode
		};

		listView = (ListView)findViewById(R.id.list_view);
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				final int titleId = titleArray[position];

				if(titleId == R.string.title_tx_power_normal)
				{
					final int level = service.getTxPowerLevel();
					final String strLevel = level+ " dB";
					int index = 0;
					for(int i=0;i<TX_POWER_LEVEL.length;i++)
					{
						if(strLevel.equals(TX_POWER_LEVEL[i]))
						{
							index = i;
							break;
						}
					}
					customPickDialog(titleId,TX_POWER_LEVEL,index);
				}
				else if(titleId == R.string.title_tx_power_keyless_lock)
				{
					if(service == null || !service.isDeviceInitialized())
					{
						Toast.makeText(context,"Device not connected",Toast.LENGTH_SHORT).show();
						return;
					}

					final int level = mBleConfiguration.getKeylessLock();
					customPickDialog(titleId,KEYLESS_LEVEL,level);
				}
				else if(titleId == R.string.title_tx_power_keyless_unlock)
				{
					if(service == null || !service.isDeviceInitialized())
					{
						Toast.makeText(context,"Device not connected",Toast.LENGTH_SHORT).show();
						return;
					}

					final int level = mBleConfiguration.getKeylessUnlock();
					customPickDialog(titleId,KEYLESS_LEVEL,level);
				}
//				else if(title == R.string.title_tx_power)
//				{
//					service.readTxPower();
//					mProgressDialog.setMessage("Read TX Power level, please wait.");
//					mProgressDialog.show();
//					mProgressDialog.setCancelable(false);
//
//					mHandler.removeCallbacks(runnableTimeout);
//					mHandler.postDelayed(runnableTimeout,5000);
//				}
				else if(titleId == R.string.title_slave_tag_counter)
				{
					if(service == null || !service.isDeviceInitialized())
					{
						Toast.makeText(context,"Device not connected",Toast.LENGTH_SHORT).show();
						return;
					}

					final int tagCounter = mBleConfiguration.getSlaveTagMode() > 0? 1:0;
					customPickDialog(titleId,SLAVE_TAG,tagCounter);
				}
			}
		});

		mHandler.postDelayed(new Runnable()
		{
			@Override
			public void run()
			{
				service = BluetoothLeIndependentService.getInstance();

				initSetting = service.readBleSetting();
				mBleConfiguration = new BleConfiguration(copyArray(initSetting));

				service.setIpcCallbackhandler(mHandler);

				if(service.isDeviceInitialized())
				{
					service.sendRequestBleSetting();
				}

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
		mTypeList.add(TYPE_TEXT);			// Mobile
		mTypeList.add(TYPE_CHECKBOX);		// Auto Connect
		mTypeList.add(TYPE_CHECKBOX);		// Auto Scroll

		mTypeList.add(TYPE_GROUP);			// Group
		mTypeList.add(TYPE_TEXT);			// Keyless Lock
		mTypeList.add(TYPE_TEXT);			// Keyless Unlock
		mTypeList.add(TYPE_TEXT);			// Counter

		mTypeList.add(TYPE_GROUP);			// Group
		mTypeList.add(TYPE_TEXT);			// BTR Mode
	}

	private List<Map<String, Object>> getDataList()
	{
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();

		final String appSetting = service.readAppSetting();
		JSONObject jsonObject = null;
		try
		{
			for (int i = 0; i < mTypeList.size(); i++)
			{
				Map<String, Object> map = new HashMap<String, Object>();
				final int titleId = titleArray[i];
				map.put("title", getDisplayString(titleId));
				map.put("description", getDisplayString(descriptionArray[i]));

				// Mobile Quality
				if(titleId == R.string.title_mobile_number)
				{
					map.put("value", String.valueOf(mBleConfiguration.getMobileNumber()));
				}
				else if( titleId == R.string.title_auto_connect
						|| titleId == R.string.title_auto_scroll)
				{
					int value = 0;
					if(!appSetting.isEmpty())
					{
						jsonObject = new JSONObject(appSetting);
						value = jsonObject.getInt(getDisplayString(titleId));
					}

					map.put("value",String.valueOf(value));
				}
				else if(titleId == R.string.title_tx_power_keyless_lock)
				{
					int level = mBleConfiguration.getKeylessLock();
					level = level< 0? 0 : level>=KEYLESS_LEVEL.length? KEYLESS_LEVEL.length-1:level;
					map.put("value",KEYLESS_LEVEL[level]);
				}
				else if(titleId == R.string.title_tx_power_keyless_unlock)
				{
					int level = mBleConfiguration.getKeylessUnlock();
					level = level< 0? 0 : level>=KEYLESS_LEVEL.length? KEYLESS_LEVEL.length-1:level;
					map.put("value",KEYLESS_LEVEL[level]);
				}
				else if(titleId == R.string.title_slave_tag_counter)
				{
					final int level = mBleConfiguration.getSlaveTagMode() > 0 ?1:0;
					map.put("value",SLAVE_TAG[level]);
				}
				else if(titleId == R.string.title_btr_mode)
				{
					int mode = mBleConfiguration.getBtrMode();
					mode = mode< 0? 0 : mode>=BTR_MODE.length? BTR_MODE.length-1:mode;
					map.put("value",BTR_MODE[mode]);
				}
				else
				{
					map.put("value","");
				}
				list.add(map);
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

	private void customPickDialog(final int titleId, final String[] items, final int defaultIndex)
	{
		final DialogInterface.OnClickListener onOkClickListener = new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				handleAction(titleId,selectPosition);
				updateListAdapter(true);
			}
		};

		final DialogInterface.OnClickListener onCancelClick = new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
			}
		};

		final DialogInterface.OnClickListener onItemClickListener = new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				selectPosition = which;
			}
		};

		selectPosition = defaultIndex;
		DialogUtil.singleChoiceDialog(context, getString(titleId), items, defaultIndex, onOkClickListener, onCancelClick, onItemClickListener, R.string.ok, R.string.cancel);
	}

	// Handle selection of pop dialog
	private void handleAction(final int titleId,int position)
	{
		try
		{
			if(titleId == R.string.title_tx_power_normal)
			{
				final String oriLevel = service.getTxPowerLevel()+" dB";
				if(oriLevel.equals(TX_POWER_LEVEL[position]))
					return;

				service.sendTxPowerLevel(position);
				mProgressDialog.setMessage("Set TX Power level, please wait.");
				mProgressDialog.show();

				mHandler.removeCallbacks(runnableTimeout);
				mHandler.postDelayed(runnableTimeout,5000);
			}
			else if(titleId == R.string.title_tx_power_keyless_lock)
			{
				mBleConfiguration.setKeylessLock(position);
				refreshMenu();

//				final int oriLevel = service.getTxPowerKeylessLock();
//				if(oriLevel == position)
//					return;

//				service.sendKeylessLevel(service.getTxPowerKeylessUnlock()<<4|position);
//				mProgressDialog.setMessage("Set Keyless lock level, please wait.");
//				mProgressDialog.show();
//
//				mHandler.removeCallbacks(runnableTimeout);
//				mHandler.postDelayed(runnableTimeout,5000);
			}
			else if(titleId == R.string.title_tx_power_keyless_unlock)
			{
				mBleConfiguration.setKeylessUnlock(position);
				refreshMenu();

//				final int oriLevel = service.getTxPowerKeylessUnlock();
//				if(oriLevel == position)
//					return;

//				service.sendKeylessLevel(position<<4 | service.getTxPowerKeylessLock());
//				mProgressDialog.setMessage("Set Keyless unlock level, please wait.");
//				mProgressDialog.show();
//
//				mHandler.removeCallbacks(runnableTimeout);
//				mHandler.postDelayed(runnableTimeout,5000);
			}
			else if(titleId == R.string.title_slave_tag_counter)
			{
				mBleConfiguration.setSlaveTagMode(position);
				refreshMenu();
			}
		}
		catch (Exception e)
		{
			LogUtil.e(getPackageName(), e.toString(), Thread.currentThread().getStackTrace());
		}
	}

	private boolean isDataChanged()
	{
		return !isEqual(initSetting,mBleConfiguration.getSetting());
	}


	private boolean isEqual(final byte[] src1,final byte[] src2)
	{
		if(src1 == null || src2 == null)
		{
			Toast.makeText(context,"Data is null!!",Toast.LENGTH_SHORT).show();
			return false;
		}

		for(int i=0;i<src1.length;i++)
		{
			if(src1[i] != src2[i])
				return false;
		}

		return true;
	}

	private void refreshMenu()
	{
		final MenuItem menuItem = mMenu.findItem(R.id.action_save);
		if(isDataChanged())
		{
			menuItem.setVisible(true);
		}
		else
		{
			menuItem.setVisible(false);
		}
	}

	private void saveSetting()
	{
		if(!isDataChanged())
			return;

		if(service == null || !service.isDeviceInitialized())
		{
			Toast.makeText(context,"Device not connected",Toast.LENGTH_SHORT).show();
			return;
		}

		mProgressDialog.setMessage("Save setting... please wait.");
		mProgressDialog.show();
		mProgressDialog.setCancelable(false);
		mHandler.removeCallbacks(runnableTimeout);
		mHandler.postDelayed(runnableTimeout,5000);

		service.sendSettingInformation(mBleConfiguration.getSetting());
	}

	private byte[] copyArray(final byte[] byteArray)
	{
		if(byteArray == null || byteArray.length == 0)
			return null;

		final byte[] data = new byte[byteArray.length];
		for(int i=0;i<byteArray.length;i++)
		{
			data[i] = byteArray[i];
		}
		return data;
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

package com.startline.slble.Activity;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Base64;
import android.view.View;
import android.widget.*;
import com.startline.slble.Adapter.SettingListAdapter;
import com.startline.slble.PureClass.Constants;
import com.startline.slble.R;
import com.startline.slble.Service.BluetoothLeIndependentService;
import com.startline.slble.Util.DialogUtil;
import com.startline.slble.Util.LogUtil;
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

	private List<Integer> mTypeList = null;

	private Context context = null;
	private ListView listView = null;
	private SettingListAdapter settingListAdapter = null;

	private BluetoothLeIndependentService service = null;
	private ProgressDialog mProgressDialog = null;

	public static final String[] TX_POWER_LEVEL = new String[]
	{
			"-17 dB","-15 dB","-10 dB","-5 dB","0 dB","2 dB","4 dB","7 dB"
	};
	private final String[] KEYLESS_LEVEL = new String[]{"Off","Low","Middle","High"};

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
				case BluetoothLeIndependentService.PARAM_KEYLESS_LEVEL:
				{
					mProgressDialog.dismiss();
					mHandler.removeCallbacks(runnableTimeout);
					updateListAdapter(true);
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
			,R.string.title_tx_power
			//,R.string.title_tx_power_normal
			,R.string.title_tx_power_keyless_lock
			,R.string.title_tx_power_keyless_unlock
		};

		descriptionArray = new int[]
		{
			R.string.description_mobile_number
			,R.string.description_auto_connect
			,R.string.description_auto_scroll
			,R.string.description_tx_power
			//,R.string.description_tx_power_normal
			,R.string.description_tx_power_keyless_lock
			,R.string.description_tx_power_keyless_unlock
		};

		listView = (ListView)findViewById(R.id.list_view);
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				final int title = titleArray[position];
				if(title == R.string.title_tx_power_normal)
				{
					if(service != null)
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
						customPickDialog(getString(title),TX_POWER_LEVEL,index);
					}
				}
				else if(title == R.string.title_tx_power_keyless_lock)
				{
					if(service != null)
					{
						final int level = service.getTxPowerKeylessLock();
						customPickDialog(getString(title),KEYLESS_LEVEL,level);
					}
				}
				else if(title == R.string.title_tx_power_keyless_unlock)
				{
					if(service != null)
					{
						final int level = service.getTxPowerKeylessUnlock();
						customPickDialog(getString(title),KEYLESS_LEVEL,level);
					}
				}
				else if(title == R.string.title_tx_power)
				{
					if(service != null)
					{
						service.readTxPower();
						mProgressDialog.setMessage("Read TX Power level, please wait.");
						mProgressDialog.show();
						mProgressDialog.setCancelable(false);

						mHandler.removeCallbacks(runnableTimeout);
						mHandler.postDelayed(runnableTimeout,5000);
					}
				}
			}
		});

		mHandler.postDelayed(new Runnable()
		{
			@Override
			public void run()
			{
				service = BluetoothLeIndependentService.getInstance();
				service.setIpcCallbackhandler(mHandler);
				service.readTxPower();

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
		mTypeList.add(TYPE_GROUP);			// Tx Power
		//mTypeList.add(TYPE_TEXT);			// Normal
		mTypeList.add(TYPE_TEXT);			// Keyless Lock
		mTypeList.add(TYPE_TEXT);			// Keyless Unlock
	}

	private List<Map<String, Object>> getDataList()
	{
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();

		final String appSetting = service.readAppSetting();
		final byte[] bleSetting = service.readBleSetting();
		JSONObject jsonObject = null;
		try
		{
			for (int i = 0; i < mTypeList.size(); i++)
			{
				Map<String, Object> map = new HashMap<String, Object>();
				map.put("title", getDisplayString(titleArray[i]));
				map.put("description", getDisplayString(descriptionArray[i]));
				switch (i)
				{
					case 0: // Mobile Quality
					{
						map.put("value",((int)bleSetting[0] & 0x0F)+"");
					}
					break;
					case 1:	// Auto Connect
					case 2:	// Auto scroll
					{
						int value = 0;
						if(!appSetting.isEmpty())
						{
							jsonObject = new JSONObject(appSetting);
							value = jsonObject.getInt(getDisplayString(titleArray[i]));
						}

						map.put("value",value);
					}
					break;
					case 3: // TX Power Group
					{
						if(service != null)
						{
							map.put("value",service.getTxPowerLevel() + " dB");
						}
						else
						{
							map.put("value","--");
						}
					}
					break;
//					case 4:	// Tx Power Level
//					{
//						int value = 4;
//						if((bleSetting[1] & 0xFF) > 0)
//						{
//							map.put("value","--");
//						}
//						else
//						{
//							if(service != null)
//							{
//								value = service.getTxPowerLevel();
//							}
//
//							String level = value+" dB";
//							map.put("value",level);
//						}
//					}
//					break;
					case 4:	// Keyless Lock
					{
						final int level = (int)bleSetting[1] & 0x0F;
						map.put("value",KEYLESS_LEVEL[level]);
					}
					break;
					case 5:	// Keyless Unlock
					{
						final int level = (int)((bleSetting[1]>>4) & 0x0F);
						map.put("value",KEYLESS_LEVEL[level]);
					}
					break;

					default:
						break;
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

	private void customPickDialog(final String title, final String[] items, final int defaultIndex)
	{
		final DialogInterface.OnClickListener onOkClickListener = new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				handleAction(title,selectPosition);
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
		DialogUtil.singleChoiceDialog(context, title, items, defaultIndex, onOkClickListener, onCancelClick, onItemClickListener, R.string.ok, R.string.cancel);
	}

	// Handle selection of pop dialog
	private void handleAction(final String title,int position)
	{
		try
		{
			if(title.equals(getString(R.string.title_tx_power_normal)))
			{
				if(service != null)
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
			}
			else if(title.equals(getString(R.string.title_tx_power_keyless_lock)))
			{
				if(service != null)
				{
					final int oriLevel = service.getTxPowerKeylessLock();
					if(oriLevel == position)
						return;
					service.sendKeylessLevel(service.getTxPowerKeylessUnlock()<<4|position);
					mProgressDialog.setMessage("Set Keyless lock level, please wait.");
					mProgressDialog.show();

					mHandler.removeCallbacks(runnableTimeout);
					mHandler.postDelayed(runnableTimeout,5000);
				}
			}
			else if(title.equals(getString(R.string.title_tx_power_keyless_unlock)))
			{
				if(service != null)
				{
					final int oriLevel = service.getTxPowerKeylessUnlock();
					if(oriLevel == position)
						return;
					service.sendKeylessLevel(position<<4 | service.getTxPowerKeylessLock());
					mProgressDialog.setMessage("Set Keyless unlock level, please wait.");
					mProgressDialog.show();

					mHandler.removeCallbacks(runnableTimeout);
					mHandler.postDelayed(runnableTimeout,5000);
				}
			}
		}
		catch (Exception e)
		{
			LogUtil.e(getPackageName(), e.toString(), Thread.currentThread().getStackTrace());
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

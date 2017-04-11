package com.startline.slble.Activity;

import android.app.*;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import com.startline.slble.Adapter.AutoStartSettingAdapter;
import com.startline.slble.PureClass.SlbleProtocol;
import com.startline.slble.R;
import com.startline.slble.Service.BluetoothLeIndependentService;
import com.startline.slble.Util.DialogUtil;
import com.startline.slble.Util.LogUtil;
import com.startline.slble.Util.TimeUtil;
import com.startline.slble.module.AutoStartSetting;

import java.lang.reflect.Field;
import java.security.spec.ECField;
import java.util.*;

import static com.startline.slble.Activity.DeviceListActivity.EXTRAS_DEVICE_NAME;

public class AutoStartSettingActivity extends Activity
{
    //*****************************************************************//
    //  Constant variable                                              //
    //*****************************************************************//
	private final String TAG = AutoStartSettingActivity.class.getSimpleName();
    //*****************************************************************//
    //  Global variable                                                //
    //*****************************************************************//
	private int mSelectPosition = -1;
	private int[] mTitleArray = null;
	private byte[] mInitSetting = null;
	private String[] mTurboTimeItems = null;
	private String[] mDailyTimeItems = null;
	private String[] mTemperatureItems = null;
	private String[] mLowVoltageItems = null;
    //*****************************************************************//
    //  Object                                                         //
    //*****************************************************************//
	private Context mContext = null;
	private AutoStartSettingAdapter mAutoStartSettingAdapter = null;
	private AutoStartSetting mAutoStartSetting = null;
	private View.OnClickListener onValueClick = null;

	private BluetoothLeIndependentService mService = null;
    //*****************************************************************//
    //  View                                                           //
    //*****************************************************************//
	private Menu mMenu = null;
	private ProgressDialog mProgressDialog = null;
	private ListView listView = null;



	private Handler mHandler = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			switch (msg.arg1)
			{
				case BluetoothLeIndependentService.PARAM_SETTING_AUTO_START:
				{
					mProgressDialog.dismiss();
					mHandler.removeCallbacks(runnableTimeout);

					if(msg.arg2 == SlbleProtocol.PARAM_SETTING_RESPONSE)
					{
						mInitSetting = (byte[])msg.obj;
						mAutoStartSetting.setSetting(copyArray(mInitSetting));
						updateListAdapter(true);
						refreshMenu();
					}
					else if(msg.arg2 == SlbleProtocol.PARAM_SETTING_WRITE)
					{
						final byte[] data = (byte[])msg.obj;
						if(isEqual(data,mAutoStartSetting.getSetting()))
						{
							mInitSetting = (byte[])msg.obj;
							mAutoStartSetting.setSetting(copyArray(mInitSetting));
							updateListAdapter();
							refreshMenu();
						}
						else
						{
							Toast.makeText(mContext,"Save data failed.",Toast.LENGTH_SHORT).show();
						}
					}
				}
				break;
				case BluetoothLeIndependentService.PARAM_ERROR:
				{
					mProgressDialog.dismiss();
					mHandler.removeCallbacks(runnableTimeout);
					Toast.makeText(mContext,"Receive Error",Toast.LENGTH_SHORT).show();
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
				Toast.makeText(mContext,"Device no response",Toast.LENGTH_LONG).show();
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
			case android.R.id.home:
				finish();
				return true;
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
    protected void onPostCreate(Bundle savedInstanceState)
    {
        super.onPostCreate(savedInstanceState);
		setContentView(R.layout.auto_start_setting);
		mContext = this;

		final Intent intent = getIntent();
		final String deviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
		getActionBar().setTitle(deviceName);
		getActionBar().setDisplayHomeAsUpEnabled(true);

		mTitleArray = new int[]
		{
			R.string.title_turbo_time
			,R.string.title_daily_time
			,R.string.title_temperature
			//,R.string.title_low_voltage
			,R.string.title_clock_time
		};

		mTurboTimeItems = new String[]
		{
			"OFF", "1 min", "2 min", "3 min", "4 min", "5 min", "6 min"
		};

		mDailyTimeItems = new String[]
		{
			"OFF","2 hr","4 hr","6hr","8 hr","10 hr","12 hr"
			,"14 hr","16 hr","18 hr","20 hr","22 hr","24 hr"
		};

		mTemperatureItems = new String[]
		{
			"OFF","-3 \u2103","-6 \u2103","-9 \u2103","-12 \u2103","-15 \u2103","-18 \u2103","-21 \u2103","-24 \u2103","-27 \u2103"
		};

		mLowVoltageItems = new String[]
		{
			"OFF"
		};

		mAutoStartSetting = new AutoStartSetting(new byte[16]);

		mProgressDialog = new ProgressDialog(mContext);

		listView = (ListView)findViewById(R.id.list_view_auto_start);

		updateListAdapter(false);

		mHandler.postDelayed(new Runnable()
		{
			@Override
			public void run()
			{
				mService = BluetoothLeIndependentService.getInstance();

				mInitSetting = mService.readAutoStartSetting();
				mAutoStartSetting = new AutoStartSetting(copyArray(mInitSetting));

				mService.setIpcCallbackhandler(mHandler);

				if(mService.isDeviceInitialized())
				{
					mHandler.removeCallbacks(runnableTimeout);
					mHandler.postDelayed(runnableTimeout,5000);
					mService.sendRequestAutoStartSetting();
				}
			}
		},initService());
    }

    private void handleListItemClick(AdapterView<?> parent, View view, int position, long id)
	{
		final int title = mTitleArray[position];

		if(title == R.string.title_turbo_time)
		{
			customPickDialog(title, mTurboTimeItems, mAutoStartSetting.getTurboTime());
		}
		else if(title == R.string.title_daily_time)
		{
			customPickDialog(title, mDailyTimeItems, mAutoStartSetting.getDailyTime());
		}
		else if(title == R.string.title_temperature)
		{
			customPickDialog(title, mTemperatureItems, mAutoStartSetting.getTemperature());
		}
		else if(title == R.string.title_low_voltage)
		{
			customPickDialog(title, mLowVoltageItems, mAutoStartSetting.getLowVoltage());
		}
		else if(title == R.string.title_clock_time)
		{
			showTimePickerDialog();
		}
	}

	private void customPickDialog(final int title, final String[] items, int defaultIndex)
	{
		final DialogInterface.OnClickListener onOkClickListener = new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				handleAction(title,mSelectPosition);
				updateListAdapter();
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
				mSelectPosition = which;
			}
		};

		if(defaultIndex < 0)
			defaultIndex = 0;
		else if(defaultIndex >= items.length)
			defaultIndex = items.length-1;


		mSelectPosition = defaultIndex;
		DialogUtil.singleChoiceDialog(mContext, getString(title), items, defaultIndex, onOkClickListener, onCancelClick, onItemClickListener, R.string.ok, R.string.cancel);
	}

	// Handle selection of pop dialog
	private void handleAction(final int title,int position)
	{
		try
		{
			if(title == R.string.title_turbo_time)
			{
				mAutoStartSetting.setTurboTime(position);
			}
			else if(title == R.string.title_daily_time)
			{
				mAutoStartSetting.setDailyTime(position);
			}
			else if(title == R.string.title_temperature)
			{
				mAutoStartSetting.setTemperature(position);
			}
			else if(title == R.string.title_low_voltage)
			{
				mAutoStartSetting.setLowVoltage(position);
			}

			refreshMenu();
		}
		catch (Exception e)
		{
			LogUtil.e(getPackageName(), e.toString(), Thread.currentThread().getStackTrace());
		}
	}

	private void showTimePickerDialog()
	{
		final View view = View.inflate(mContext, R.layout.clock_time_setting, null);
		final NumberPicker pickerHour = (NumberPicker)view.findViewById(R.id.picker_hour);
		final NumberPicker pickerMinute = (NumberPicker)view.findViewById(R.id.picker_minute);

		final GregorianCalendar calendar = new GregorianCalendar();
		final int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
		final int currentMinute = calendar.get(Calendar.MINUTE);


		setNumberPickerTextColor(pickerHour, getResources().getColor(R.color.black));
		setNumberPickerTextColor(pickerMinute, getResources().getColor(R.color.black));

		// HOUR
		pickerHour.setMinValue(0);
		pickerHour.setMaxValue(23);
		pickerHour.setDescendantFocusability(NumberPicker.FOCUS_AFTER_DESCENDANTS);

		// MINUTE
		pickerMinute.setMinValue(0);
		pickerMinute.setMaxValue(59);
		pickerMinute.setDescendantFocusability(NumberPicker.FOCUS_AFTER_DESCENDANTS);


		final int totalMinute = convertClockTimeToMinute(mAutoStartSetting.getClockTimeHour(),mAutoStartSetting.getClockTimeMinute());

		// Clock Time not set
		if(totalMinute == 0)
		{
			pickerHour.setValue(currentHour);
			pickerMinute.setValue(currentMinute);
		}
		else
		{
			final String offsetTime = TimeUtil.timeOffset("HH:mm", getTime(currentHour, currentMinute), totalMinute*60*1000);
			final String[] temp = offsetTime.split(":");
			pickerHour.setValue(Integer.parseInt(temp[0]));
			pickerMinute.setValue(Integer.parseInt(temp[1]));
		}

		final DialogInterface.OnClickListener onOkClick = new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				final int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
				final int currentMinute = calendar.get(Calendar.MINUTE);
				final int hour = pickerHour.getValue();
				final int minute = pickerMinute.getValue();

				final long diffMilliSecond = TimeUtil.getTimeDiff("HH:mm", getTime(currentHour,currentMinute), getTime(hour,minute));
				int totalMinute = (int)(diffMilliSecond / 1000 /60) ;
				if(totalMinute < 0)
					totalMinute += 1440;

				final int[] clockTime = convertClockTime(totalMinute);
				mAutoStartSetting.setClockTimeHour(clockTime[1]);
				mAutoStartSetting.setClockTimeMinute(clockTime[0]);
				updateListAdapter();
				refreshMenu();
			}
		};


		DialogUtil.customViewDialog(mContext, getString(R.string.title_clock_time), view, onOkClick, null, R.string.ok, R.string.cancel);
	}

	private boolean isDataChanged()
	{
		return !isEqual(mInitSetting,mAutoStartSetting.getSetting());
	}

	private boolean isEqual(final byte[] src1,final byte[] src2)
	{
		if(src1 == null || src2 == null)
		{
			Toast.makeText(mContext,"Data is null!!",Toast.LENGTH_SHORT).show();
			return false;
		}

		for(int i=0;i<src1.length;i++)
		{
			if(src1[i] != src2[i])
				return false;
		}

		return true;
	}

	private int initService()
	{
		int delayTime = 0;
		if(!isServiceRunning(BluetoothLeIndependentService.class))
			delayTime = 500;

		return delayTime;
	}

	private void updateListAdapter()
	{
		if(mAutoStartSettingAdapter == null)
		{
			updateListAdapter(false);
		}

		mAutoStartSettingAdapter.setDataList(getData());
		mAutoStartSettingAdapter.notifyDataSetChanged();
	}

	private void updateListAdapter(final boolean dataEnabled)
	{
		if(mAutoStartSettingAdapter == null)
		{
			mAutoStartSettingAdapter = new AutoStartSettingAdapter(mContext, getData(), dataEnabled, onValueClick);
			listView.setAdapter(mAutoStartSettingAdapter);
		}
		else
		{
			mAutoStartSettingAdapter.setDataEnabled(dataEnabled);
			mAutoStartSettingAdapter.setDataList(getData());
			mAutoStartSettingAdapter.notifyDataSetChanged();
		}

		if(dataEnabled)
		{
			listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
			{
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id)
				{
					handleListItemClick(parent, view, position, id);
				}
			});
		}
	}


	private List<Map<String, String>> getData()
	{
		List<Map<String, String>> list = new ArrayList<Map<String, String>>();

		for (int i = 0; i<mTitleArray.length; i++)
		{
			Map<String, String> map = new HashMap<String, String>();
			map.put("title", getDisplayString(mTitleArray[i]));
//			map.put("unit", getUnit(mTitleArray[i]));
			map.put("value",getItemValue(mTitleArray[i]));
			list.add(map);
		}
		return list;
	}

	private String getUnit(final int stringId)
	{
		int unitId = 0;
		switch (stringId)
		{
			case R.string.title_turbo_time:
			{
				unitId = R.string.min;
			}
			break;
			case R.string.title_daily_time:
			{
				unitId = R.string.hr;
			}
			break;
			case R.string.title_temperature:
			{
				unitId = R.string.degree_celsius;
			}
			break;
			case R.string.title_low_voltage:
			{
				unitId = R.string.voltage;
			}
			break;
		}

		if(unitId > 0)
		{
			return getDisplayString(unitId);
		}
		else
		{
			return "";
		}
	}

	private String getItemValue(final int stringId)
	{
		String value = "";
		int index;
		switch (stringId)
		{
			case R.string.title_turbo_time:
			{
				index = mAutoStartSetting.getTurboTime();
				value = index< 0?mTurboTimeItems[0]:index>=mTurboTimeItems.length? mTurboTimeItems[mTurboTimeItems.length-1]: mTurboTimeItems[index];
			}
			break;
			case R.string.title_daily_time:
			{
				index = mAutoStartSetting.getDailyTime();
				value = index< 0?mDailyTimeItems[0]:index>=mDailyTimeItems.length? mDailyTimeItems[mDailyTimeItems.length-1]: mDailyTimeItems[index];
			}
			break;
			case R.string.title_temperature:
			{
				index = mAutoStartSetting.getTemperature();
				value = index< 0?mTemperatureItems[0]:index>=mTemperatureItems.length? mTemperatureItems[mTemperatureItems.length-1]: mTemperatureItems[index];
			}
			break;
			case R.string.title_low_voltage:
			{
				index = mAutoStartSetting.getLowVoltage();
				value = index< 0?mLowVoltageItems[0]:index>=mLowVoltageItems.length? mLowVoltageItems[mLowVoltageItems.length-1]: mLowVoltageItems[index];
			}
			break;
			case R.string.title_clock_time:
			{
				final int hour = mAutoStartSetting.getClockTimeHour();
				final int minute = mAutoStartSetting.getClockTimeMinute();
				value = convertClockTime(hour,minute);
			}
			break;
		}

		return value;
	}

	private String getTime(final int hour, final int minute)
	{
		return String.format("%02d:%02d",hour,minute);
	}

	private int convertClockTimeToMinute(final int clockHour, final int clockMinute)
	{
		final int totalMinute = (clockMinute*5)/60 + clockHour*10;

		return totalMinute;
	}

	private String convertClockTime(final int clockHour, final int clockMinute)
	{
		final int totalMinute = convertClockTimeToMinute(clockHour, clockMinute);
		int hour,minute;

		hour = totalMinute / 60;
		minute = totalMinute % 60;

		return getTime(hour,minute);
	}

	private int[] convertClockTime(final int totalMinute)
	{
		int[] clockTime = new int[2];

		clockTime[1] = totalMinute / 10;
		clockTime[0] = (totalMinute % 10) *60 /5;

		return clockTime;
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

		if(mService == null || !mService.isDeviceInitialized())
		{
			Toast.makeText(mService,"Device not connected",Toast.LENGTH_SHORT).show();
			return;
		}

		mProgressDialog.setMessage("Save setting... please wait.");
		mProgressDialog.show();
		mProgressDialog.setCancelable(false);
		mHandler.removeCallbacks(runnableTimeout);
		mHandler.postDelayed(runnableTimeout,5000);

		mService.sendAutoStartSetting(mAutoStartSetting.getSetting());
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
		ActivityManager manager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);

		for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE))
		{
			if (serviceClass.getName().equalsIgnoreCase(service.service.getClassName())
					&& service.service.getPackageName().equalsIgnoreCase(mContext.getPackageName()))
			{
				running = true;
			}
		}
		return running;
	}

	public boolean setNumberPickerTextColor(NumberPicker numberPicker, int color)
	{
		final int count = numberPicker.getChildCount();
		for(int i = 0; i < count; i++){
			View child = numberPicker.getChildAt(i);
			if(child instanceof EditText){
				try{
					Field selectorWheelPaintField = numberPicker.getClass().getDeclaredField("mSelectorWheelPaint");
					selectorWheelPaintField.setAccessible(true);
					((Paint)selectorWheelPaintField.get(numberPicker)).setColor(color);
					((EditText)child).setTextColor(color);
					numberPicker.invalidate();
					return true;
				}
				catch(NoSuchFieldException e){
					Log.w(TAG, e);
				}
				catch(IllegalAccessException e){
					Log.w(TAG, e);
				}
				catch(IllegalArgumentException e){
					Log.w(TAG, e);
				}
			}
		}
		return false;
	}
}

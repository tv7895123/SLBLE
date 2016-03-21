package com.startline.slble.Activity;

import android.content.*;
import android.os.Bundle;
import android.os.Handler;
import android.preference.*;
import android.view.View;
import android.widget.AbsListView;
import android.widget.TextView;
import com.startline.slble.PureClass.Constants;
import com.startline.slble.R;
import com.startline.slble.Service.BluetoothLeIndependentService;
import com.startline.slble.module.BleConfiguration;

/**
 * Created by terry on 2015/8/31.
 */
public class SettingActivity extends PreferenceActivity
{
	private String TAG = SettingActivity.class.getName();
	private final int REQUEST_CODE_TUNING = 1;


	private int mPosition = 0;
	private long mBleSetting = 0;
	private Context context = null;
	private boolean mInitKeyLess = false;
	private boolean mInitKeyLessArm = false;
	private boolean mInitKeyLessDisarm = false;
	private SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener = null;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		context = this;

		// Sync ble setting from CONFIG_FILE_BLE_SETTING
		mBleSetting = readBleSetting();
		final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
//		final SharedPreferences.Editor editor = sharedPreferences.edit();
//		editor.putBoolean(getString(R.string.pref_key_key_less_arm),BleConfiguration.getValue(mBleSetting,BleConfiguration.BLE_SETTING_KEY_LES_ARM,BleConfiguration.MASK_BIT) == 1? true:false);
//		editor.putBoolean(getString(R.string.pref_key_key_less_disarm), BleConfiguration.getValue(mBleSetting, BleConfiguration.BLE_SETTING_KEY_LES_DISARM, BleConfiguration.MASK_BIT) == 1 ? true : false);
//		editor.apply();

		mInitKeyLess = sharedPreferences.getBoolean(getString(R.string.pref_key_key_less),false);
		mInitKeyLessArm = sharedPreferences.getBoolean(getString(R.string.pref_key_key_less_arm),false);
		mInitKeyLessDisarm = sharedPreferences.getBoolean(getString(R.string.pref_key_key_less_disarm),false);


		// Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

		// Add widget for mobile number
		final Preference prefMobile = getPreferenceManager().findPreference(getString(R.string.pref_key_mobile_number));
		prefMobile.setWidgetLayoutResource(R.layout.mobile_number);

		onSharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener()
		{
			@Override
			public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
			{
				final Intent intent = new Intent();
				String intentKey = key;
				mPosition = getListView().getFirstVisiblePosition();
				if(key.equals(getString(R.string.pref_key_auto_connect)))
				{
					intent.putExtra("auto_connect", sharedPreferences.getBoolean(key, false));
				}
				else if(key.equals(getString(R.string.pref_key_auto_wake_up)))
				{
					intent.putExtra("auto_wake_up", sharedPreferences.getBoolean(key, false));
				}
				else if(key.equals(getString(R.string.pref_key_auto_scroll)))
				{
					intent.putExtra("auto_scroll",sharedPreferences.getBoolean(key, false));
				}
				else if(key.equals(getString(R.string.pref_key_slave_tag)))
				{
					intent.putExtra("slave_tag",sharedPreferences.getBoolean(key, false));
				}
				else if(key.equals(getString(R.string.pref_key_user_password)))
				{
					intent.putExtra("user_password",sharedPreferences.getBoolean(key, false));
				}
				else if(key.equals(getString(R.string.pref_key_key_less)))
				{
					final boolean enabled = sharedPreferences.getBoolean(key, false);
					boolean arm = sharedPreferences.getBoolean(getString(R.string.pref_key_key_less_arm), false);
					boolean disarm = sharedPreferences.getBoolean(getString(R.string.pref_key_key_less_disarm), false);

					// If KeyLess is enabled
					if(enabled)
					{
						// If there is no threshold, start tuning activity
						final int threshold = getKeyLessThreshold();
						if(threshold == 0)
						{
						 	startTuningActivity(REQUEST_CODE_TUNING);
						}

						// Force ARM and DISARM to be enabled
						if(arm == false && disarm == false)
						{
							arm = true;
							disarm = true;
//							final SharedPreferences.Editor editor = sharedPreferences.edit();
							final SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
							editor.putBoolean(getString(R.string.pref_key_key_less_arm),arm);
							editor.putBoolean(getString(R.string.pref_key_key_less_disarm),disarm);
							editor.commit();
							refreshPreferenceActivity(mPosition);
						}
					}
					intent.putExtra("key_less",enabled);
					intent.putExtra("key_less_arm",arm);
					intent.putExtra("key_less_disarm",disarm);
					intentKey = ""; // do not broadcast until leave SettingActivity
				}
				else if(key.equals(getString(R.string.pref_key_key_less_arm)))
				{
					final boolean arm = sharedPreferences.getBoolean(key, false);
					final boolean disarm = sharedPreferences.getBoolean(getString(R.string.pref_key_key_less_disarm), false);
					if(arm == false && disarm == false)
					{
						final SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
						editor.putBoolean(getString(R.string.pref_key_key_less),false).apply();
						refreshPreferenceActivity(mPosition);
					}
					intent.putExtra("key_less_arm",arm);
					intentKey = "";  // do not broadcast until leave SettingActivity
				}
				else if(key.equals(getString(R.string.pref_key_key_less_disarm)))
				{
					final boolean arm = sharedPreferences.getBoolean(getString(R.string.pref_key_key_less_arm), false);
					final boolean disarm = sharedPreferences.getBoolean(key, false);
					if(arm == false && disarm == false)
					{
						final SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
						editor.putBoolean(getString(R.string.pref_key_key_less),false).apply();
						refreshPreferenceActivity(mPosition);
					}
				 	intent.putExtra("key_less_disarm",disarm);
					intentKey = ""; // do not broadcast until leave SettingActivity
				}
				else if(key.equals(getString(R.string.pref_key_tx_power)))
				{
					intent.putExtra("tx_power", Integer.parseInt(sharedPreferences.getString(key, "4")));
				}
				else if(key.equals(getString(R.string.pref_key_test_mode)))
				{
					intent.putExtra("test_mode",sharedPreferences.getBoolean(key, false));
				}
				else if(key.equals(getString(R.string.pref_key_ping_no_ack)))
				{
					intent.putExtra("ping_no_ack",sharedPreferences.getBoolean(key, false));
				}
				else if(key.equals(getString(R.string.pref_key_format_error)))
				{
					intent.putExtra("format_error",sharedPreferences.getBoolean(key, false));
				}
				else if(key.equals(getString(R.string.pref_key_thermal_test)))
				{
					intent.putExtra("thermal_test",sharedPreferences.getBoolean(key, false));
				}
				else
				{
					intentKey = "";
				}

				if(intentKey.length() > 0)
				{
					intent.putExtra("key",intentKey);
					broadcastConfiguration(intent);
				}
			}
		};

		getListView().setOnScrollListener(new AbsListView.OnScrollListener()
		{
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState)
			{

			}

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount)
			{
				// Custom widget will be released when scroll out of screen
				// We need to recover the value when it is visible
				if(firstVisibleItem == 0)
				{
					updateMobileNumber();
				}
			}
		});

		new Handler().postDelayed(new Runnable()
		{
			@Override
			public void run()
			{
                updateMobileNumber();
			}
		},200);
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();

		// Read last setting
		final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		boolean keyLess = sharedPreferences.getBoolean(getString(R.string.pref_key_key_less), false);
		boolean arm = sharedPreferences.getBoolean(getString(R.string.pref_key_key_less_arm), false);
		boolean disarm = sharedPreferences.getBoolean(getString(R.string.pref_key_key_less_disarm), false);

		// If setting are not changed, no need to save
		if(mInitKeyLess == keyLess
			&& mInitKeyLessArm == arm
			&& mInitKeyLessDisarm == disarm)
			return;

		// If Keyless is off, disable ARM and DISARM , too
		if(keyLess == false)
		{
			arm = false;
			disarm = false;
		}

		// Save ble setting as a long value
		mBleSetting = BleConfiguration.setValue(mBleSetting,arm==true?1:0,BleConfiguration.BLE_SETTING_KEY_LES_ARM,BleConfiguration.MASK_BIT);
		mBleSetting = BleConfiguration.setValue(mBleSetting,disarm==true?1:0,BleConfiguration.BLE_SETTING_KEY_LES_DISARM,BleConfiguration.MASK_BIT);
		saveBleSetting(mBleSetting);

		// Notify service
		final Intent intent = new Intent();
		intent.putExtra("key",getString(R.string.pref_key_key_less));
		intent.putExtra("key_less",keyLess);
		intent.putExtra("key_less_arm", arm);
		intent.putExtra("key_less_disarm",disarm);
		broadcastConfiguration(intent);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		switch(requestCode)
		{
			case REQUEST_CODE_TUNING:
			{
				final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
				final SharedPreferences.Editor editor = sharedPreferences.edit();
			 	if(getKeyLessThreshold() == 0)
				{
				 	editor.putBoolean(getString(R.string.pref_key_key_less),false).apply();
					editor.putBoolean(getString(R.string.pref_key_key_less_arm),false).apply();
					editor.putBoolean(getString(R.string.pref_key_key_less_disarm),false).apply();
				}

				refreshPreferenceActivity(mPosition);
			}
			break;
		}
	}

	private int getKeyLessThreshold()
	{
		final SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.CONFIG_FILE_BLE_SETTING, Context.MODE_PRIVATE);
		return sharedPreferences.getInt(Constants.CONFIG_ITEM_KEY_LESS_THRESHOLD, 0);
	}

	private void updateMobileNumber()
	{
		final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        final Preference prefMobile = getPreferenceManager().findPreference(getString(R.string.pref_key_mobile_number));
        final View view = prefMobile.getView(null,null);
        if(view != null)
        {
            final TextView txtMobileNumber = (TextView)findViewById(R.id.txt_mobile_number_value);
            if(txtMobileNumber != null)
            {
                txtMobileNumber.setText(sharedPreferences.getString(Constants.CONFIG_ITEM_MOBILE_NUMBER, "0"));
            }
        }
	}

	private void startTuningActivity(final int requestCode)
	{
		final Intent intent = new Intent();
		intent.setClass(context, KeyLessTuningActivity.class);
		startActivityForResult(intent, requestCode);
	}

	private int findPositionByKey(final PreferenceScreen screen,final String key)
	{
		for(int i = 0; i < screen.getPreferenceCount(); i++)
		{
			final Preference preference = screen.getPreference(i);
			final String tempKey = preference.getKey();

			// be careful, because key will be null if no android:key is specified
			// (as is often the case for PreferenceCategory elements)
			if(key.equals(tempKey))
			{
				return i;
			}
			else
			{
				if(preference instanceof PreferenceCategory)
				{
					final int index = findPositionByKey((PreferenceCategory) preference, key);
					if(index >= 0) return 12;
				}
			}
		}
		return 0;
	}

	private int findPositionByKey(final PreferenceCategory category,final String key)
	{
		for(int i = 0; i < category.getPreferenceCount(); i++)
		{
			final Preference preference = category.getPreference(i);
			final String tempKey = preference.getKey();

			// be careful, because key will be null if no android:key is specified
			// (as is often the case for PreferenceCategory elements)
			if(key.equals(tempKey))
			{
				return i;
			}
			else
			{
				if(preference instanceof PreferenceCategory)
				{
				 	findPositionByKey((PreferenceCategory) preference, key);
				}
			}
		}
		return -1;
	}

	private long readBleSetting()
	{
		final SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.CONFIG_FILE_BLE_SETTING, Context.MODE_PRIVATE);
		return sharedPreferences.getLong(Constants.CONFIG_ITEM_BLE_SETTING,0);
	}

	private void saveBleSetting(final long data)
	{
		final SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.CONFIG_FILE_BLE_SETTING, Context.MODE_PRIVATE);
		sharedPreferences.edit().putLong(Constants.CONFIG_ITEM_BLE_SETTING,data).apply();
	}

	private void refreshPreferenceActivity(final int position)
	{
		setPreferenceScreen(null);
		addPreferencesFromResource(R.xml.preferences);

		// Add widget for mobile number
		final Preference prefMobile = getPreferenceManager().findPreference(getString(R.string.pref_key_mobile_number));
		prefMobile.setWidgetLayoutResource(R.layout.mobile_number);


		// PreferenceActivity extends ListActivity, so the ListView is accessible...
		getListView().setSelection(position);


		new Handler().postDelayed(new Runnable()
		{
			@Override
			public void run()
			{
				// After reload preference pages, needs to set mobile number again
				updateMobileNumber();
			}
		},300);
	}

	private void broadcastConfiguration(final Intent intent)
	{
		intent.setAction(BluetoothLeIndependentService.ACTION_UI_NOTIFY_SERVICE);
		intent.putExtra("mode",BluetoothLeIndependentService.MODE_SET_CONFIGURATION);
		sendBroadcast(intent);
	}

}

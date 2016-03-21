package com.startline.slble.Activity;

import android.app.Activity;
import android.content.*;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.startline.slble.PureClass.Constants;
import com.startline.slble.R;
import com.startline.slble.Service.BluetoothLeIndependentService;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static com.startline.slble.Service.BluetoothLeIndependentService.*;

/**
 * Created by terry on 2015/4/21.
 */
public class KeyLessTuningActivity extends Activity
{
    //*****************************************************************//
    //  Constant Variables                                             //
    //*****************************************************************//
	private final static String TAG = "com.startline.slble";

    //*****************************************************************//
    //  Global Variables                                               //
    //*****************************************************************//
	private int mKeyLessRssi = 0;
	private boolean mKeyLessArmEnable = false;
	private boolean mKeyLessDisarmEnable = false;
    //*****************************************************************//
    //  Object                                                         //
    //*****************************************************************//
	private Context context;
	private Handler mHandler = null;
    //*****************************************************************//
    //  View                                                           //
    //*****************************************************************//
	private LinearLayout layoutTuning = null;
	private TextView txtKeyLessRssi = null;
	private TextView txtRssiSampleArrayValue = null;
	private TextView txtAvgRssiArrayValue = null;
	private TextView txtFinalAvgRssiValue = null;
	private TextView txtStableRssiValue = null;
	private TextView txtAvgStableRssiValue = null;

	private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver()
	{
        @Override
        public void onReceive(Context context, Intent intent)
		{
			try
			{
				final int param = intent.getIntExtra("param",-1);
				switch (param)
				{
					case PARAM_TUNING_RESULT:
					{
						final int tuningResult = intent.getIntExtra("tuningResult", 0);
						final int[] rssiRecord = intent.getIntArrayExtra("rssiRecord");
						final int[] avgRssiRecord = intent.getIntArrayExtra("avgRssiRecord");

						final int avgRssi = intent.getIntExtra("avgRssi", 0);
						txtFinalAvgRssiValue.setText(avgRssi + "");

						final Button btnTuning = (Button)findViewById(R.id.btn_tuning);
						if(tuningResult == 1)
						{
							txtKeyLessRssi.setText(avgRssi + "");

							btnTuning.setEnabled(true);
							Toast.makeText(context,"Tuning Result : " + avgRssi,Toast.LENGTH_LONG).show();
						}
						else if(tuningResult == -1)
						{
							btnTuning.setEnabled(true);
							Toast.makeText(context,"Tuning failed",Toast.LENGTH_LONG).show();
						}

						String s="";
						for(int i=0;i<rssiRecord.length;i++)
						{
							if(i > 0 && i%10 == 0)
							{
								s = s+"\n";
							}
							s = s + rssiRecord[i] + "  ";
						}
						txtRssiSampleArrayValue.setText(s);
						s = "";
						for(int i=0;i<avgRssiRecord.length;i++)
						{
							if(i > 0 && i%10 == 0)
							{
								s = s+"\n";
							}
							s = s + avgRssiRecord[i] + "  ";
						}
						txtAvgRssiArrayValue.setText(s);
					}
					break;

					case PARAM_STABLE_RSSI_RECORD:
					{
						final int[] rssiRecord = intent.getIntArrayExtra("rssiRecord");
						String s="";
						int total = 0;
						for(int i=0;i<rssiRecord.length;i++)
						{
							total += rssiRecord[i];
							s = s + rssiRecord[i] + " ";
						}
						txtStableRssiValue.setText(s);
						txtAvgStableRssiValue.setText((total/rssiRecord.length)+"");
					}
					break;
				}
			}
			catch (Exception e)
			{

			}
        }
    };


	@Override
	protected void onResume()
	{
		super.onResume();
		registerReceiver(mBroadcastReceiver, makeIntentFilter());
		getAppConfiguration();
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		unregisterReceiver(mBroadcastReceiver);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.key_less_tuning);

		context  = this;


		layoutTuning = (LinearLayout)findViewById(R.id.layout_tuning);
		txtKeyLessRssi = (TextView)findViewById(R.id.txt_rssi_value);
		txtRssiSampleArrayValue = (TextView)findViewById(R.id.txt_rssi_sample_value1);
		txtAvgRssiArrayValue = (TextView)findViewById(R.id.txt_avg_rssi_value);
		txtFinalAvgRssiValue = (TextView)findViewById(R.id.txt_final_average_value);
		txtStableRssiValue = (TextView)findViewById(R.id.txt_stable_rssi_value);
		txtAvgStableRssiValue = (TextView)findViewById(R.id.txt_stable_average_value);
		final Button btnTuning = (Button)findViewById(R.id.btn_tuning);
		btnTuning.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				btnTuning.setEnabled(false);
				notifyKeyLessTuning();
				layoutTuning.setVisibility(View.VISIBLE);
			}
		});

		getKeyLessThreshold();

		updateKeyLessUI();
	}

	private void getAppConfiguration()
	{
		final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		final boolean keyLessEnable = sharedPreferences.getBoolean(getString(R.string.pref_key_key_less), false);
		final boolean armEnable = sharedPreferences.getBoolean(getString(R.string.pref_key_key_less_arm),false);
		final boolean disarmEnable = sharedPreferences.getBoolean(getString(R.string.pref_key_key_less_disarm),false);

		if(keyLessEnable && (armEnable || disarmEnable))
		{
			layoutTuning.setVisibility(View.VISIBLE);
		}
		else
		{
			layoutTuning.setVisibility(View.GONE);
		}
	}


	private void getKeyLessThreshold()
	{
		final SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.CONFIG_FILE_BLE_SETTING, Context.MODE_PRIVATE);
		mKeyLessRssi = sharedPreferences.getInt(Constants.CONFIG_ITEM_KEY_LESS_THRESHOLD, 0);
	}

	private void updateKeyLessUI()
	{
		if(mKeyLessRssi < 0)
		{
			txtKeyLessRssi.setText(mKeyLessRssi+"");
		}
	}

	private void notifyKeyLessTuning()
	{
		Log.i(TAG, "notifyKeyLessTuning");
        final Intent intent = new Intent(BluetoothLeIndependentService.ACTION_UI_NOTIFY_SERVICE);
		intent.putExtra("mode",BluetoothLeIndependentService.MODE_KEY_LESS_TUNING);
        sendBroadcast(intent);
	}

    private static IntentFilter makeIntentFilter()
	{
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeIndependentService.ACTION_SERVICE_NOTIFY_UI);
        return intentFilter;
    }

	private long byteArrayToLong(final byte[] array)
	{
		ByteBuffer buffer = ByteBuffer.wrap(array);
//		buffer.order(ByteOrder.BIG_ENDIAN);
//		System.out.println(buffer.getLong());
//		buffer = ByteBuffer.wrap(array);
		buffer.order(ByteOrder.LITTLE_ENDIAN);

		final long value = buffer.getLong();
		System.out.println(value);
		return value;
	}
}
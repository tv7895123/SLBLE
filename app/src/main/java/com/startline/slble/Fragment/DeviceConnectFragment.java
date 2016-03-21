package com.startline.slble.Fragment;

import android.app.Activity;
import android.content.*;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.startline.slble.Activity.TabActivity;
import com.startline.slble.R;
import com.startline.slble.Service.BluetoothLeIndependentService;
import com.startline.slble.Util.TimeUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by terry on 2015/8/4.
 */
public class DeviceConnectFragment extends Fragment
{
    //  Common State
	public static final int OFF = 0;
	public static final int ON = 1;
    //*****************************************************************//
    //  Constant Variables                                             //
    //*****************************************************************//
	private final static String TAG = DeviceConnectFragment.class.getSimpleName();
	private final String[] mTxPowerArray = new String[]{"-20","-16","-12","-8","-4","0","2","4","5","7"};

    //*****************************************************************//
    //  Global Variables                                               //
    //*****************************************************************//
    private String mDeviceName;
    private String mDeviceAddress;
	private boolean mConnected;


    //*****************************************************************//
    //  Object                                                         //
    //*****************************************************************//
    private Context context;
    private BluetoothLeIndependentService mBluetoothLeService = null;
	private File mLogFile = null;
    //*****************************************************************//
    //  View                                                           //
    //*****************************************************************//
	private View rootView = null;
	private LinearLayout mLayoutTxPower = null;
	private RelativeLayout mLayoutKeyInfo = null;
	private RelativeLayout mLayoutPwdInfo = null;
	private RelativeLayout mLayoutConnectionInfo = null;
    private TextView mTxtConnectionState;
	private TextView mTxtTxPowerValue;
	private TextView mTxtRssi;
	private TextView mTxtAverageRssi;
	private TextView mTxtLog = null;
	private ScrollView mScrollViewLog = null;

	private Button mBtnConnect;
	private Spinner spinnerTxPower = null;

	private TextView txtFirstConnect = null;
	private TextView txtLastConnect = null;

	private TextView txtFirstDisconnect = null;
	private TextView txtLastDisconnect = null;

	private TextView txtThermalCommandCount = null;
	private TextView txtThermalCstaCount = null;

	private TextView txtDisconnectCount = null;
	private Button btnThermalReset = null;


	@Override
	public void onAttach(Activity activity)
	{
		super.onAttach(activity);


		final String deviceInfo = ((TabActivity)activity).getDeviceInfo();
		final String[] array = deviceInfo.split(",");
		if(array.length == 2)
		{
			mDeviceAddress = array[0];
			mDeviceName = array[1];
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		if(rootView == null)
		{
			rootView = inflater.inflate(R.layout.device_connect_fragment, container, false);
			setupViews();
		}

		return rootView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		context = getActivity();
	}

	private void setupViews()
	{
		mLayoutTxPower = (LinearLayout)rootView.findViewById(R.id.layout_tx_power);
		mLayoutKeyInfo = (RelativeLayout)rootView.findViewById(R.id.layout_key_info);
		mLayoutPwdInfo = (RelativeLayout)rootView.findViewById(R.id.layout_pwd_info);
		mLayoutConnectionInfo = (RelativeLayout)rootView.findViewById(R.id.layout_connection_info);

		mTxtConnectionState = (TextView) rootView.findViewById(R.id.connection_state);
		mTxtRssi = (TextView)rootView.findViewById(R.id.rssi_value);
		mTxtAverageRssi = (TextView)rootView.findViewById(R.id.rssi_average_value);
		mTxtTxPowerValue = (TextView)rootView.findViewById(R.id.txt_tx_power_value);
		spinnerTxPower = (Spinner)rootView.findViewById(R.id.spinner_tx_power);
		mTxtLog = (TextView)rootView.findViewById(R.id.txt_log);
		//mTxtLog.setMaxLines(300);
		mScrollViewLog = (ScrollView)rootView.findViewById(R.id.scroll_view);

		mBtnConnect = (Button)rootView.findViewById(R.id.btn_connect);

		((TextView) rootView.findViewById(R.id.device_address)).setText(mDeviceAddress);
		// set view event
		mBtnConnect.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				if (mConnected)
				{
//					final ImageView imgArmLock = (ImageView) findViewById(R.id.img_arm_lock);
//					imgArmLock.setImageResource(R.drawable.gray_lock);

					sendAction(getUiActionIntent(BluetoothLeIndependentService.MODE_DISCONNECT));
				}
				else
				{
					sendAction(getUiActionIntent(BluetoothLeIndependentService.MODE_CONNECT));
				}
			}
		});

		txtFirstConnect = (TextView)rootView.findViewById(R.id.txt_first_connect_time_value);
		txtLastConnect = (TextView)rootView.findViewById(R.id.txt_last_connect_time_value);

		txtFirstDisconnect = (TextView)rootView.findViewById(R.id.txt_first_disconnect_time_value);
		txtLastDisconnect = (TextView)rootView.findViewById(R.id.txt_last_disconnect_time_value);

		txtThermalCommandCount = (TextView)rootView.findViewById(R.id.txt_thermal_command_count);
		txtThermalCstaCount = (TextView)rootView.findViewById(R.id.txt_thermal_csta_count);

		txtDisconnectCount = (TextView)rootView.findViewById(R.id.txt_disconnect_count_value);
		btnThermalReset = (Button)rootView.findViewById(R.id.btn_thermal_reset);
		btnThermalReset.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				resetThermal();
			}
		});

		btnThermalReset.performClick();


		final Button btnClear = (Button)rootView.findViewById(R.id.btn_clear);
		btnClear.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				mTxtLog.setText("");
			}
		});

		String[] txPowerArray = new String[mTxPowerArray.length];
		for (int i = 0; i< mTxPowerArray.length; i++)
		{
			txPowerArray[i] = mTxPowerArray[i] + " dBm";
		}
		spinnerTxPower.setAdapter( new ArrayAdapter<String>(getActivity(),android.R.layout.simple_spinner_item, txPowerArray));
		spinnerTxPower.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
		{
			@Override
			public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l)
			{
                sendAction(getTxPowerIntent(i));
			}

			@Override
			public void onNothingSelected(AdapterView<?> adapterView)
			{
			}
		});
	}


	private Intent getUiActionIntent(final int mode)
	{
		final Intent intent = new Intent();
		intent.putExtra("mode",mode);
		return intent;
	}

	private Intent getTxPowerIntent(final int value)
	{
        final Intent intent = getUiActionIntent(BluetoothLeIndependentService.MODE_TX_POWER);
		intent.putExtra("tx_power",value);
		return intent;
	}


	//==============================================================================
	//  Broadcast function
	//==============================================================================
	private void sendAction(final Intent intent)
	{
		Log.i(TAG, "sendUiActionBroadcast : " + intent.getIntExtra("mode",-1));
		intent.setAction(BluetoothLeIndependentService.ACTION_UI_NOTIFY_SERVICE);
		getActivity().sendBroadcast(intent);
	}


	//==============================================================================
	//  Call TabActivity function
	//==============================================================================
	private void resetThermal()
	{
		txtFirstConnect.setText("--:--");
		txtLastConnect.setText("--:--");
		txtFirstDisconnect.setText("--:--");
		txtLastDisconnect.setText("--:--");
		txtThermalCommandCount.setText("0");
		txtThermalCstaCount.setText("0");
		txtDisconnectCount.setText("0");
		((TabActivity) getActivity()).resetThermalTest(true);
	}

	//==============================================================================
	//  For TabActivity function
	//==============================================================================
	public void setConnected(final boolean connected)
	{
		mConnected = connected;
		btnThermalReset.setEnabled(!mConnected);
	}

	public void showTxPower(final boolean visible)
	{
		if(visible)
			mLayoutTxPower.setVisibility(View.VISIBLE);
		else
			mLayoutTxPower.setVisibility(View.GONE);
	}

	public void showKeyInfo(final boolean visible)
	{
		if(visible)
		{
			mLayoutKeyInfo.setVisibility(View.VISIBLE);
			mLayoutPwdInfo.setVisibility(View.VISIBLE);
		}
		else
		{
			mLayoutKeyInfo.setVisibility(View.GONE);
			mLayoutPwdInfo.setVisibility(View.GONE);
		}
	}

	public void showThermalInfo(final boolean visible)
	{
		if(visible)
			mLayoutConnectionInfo.setVisibility(View.VISIBLE);
		else
			mLayoutConnectionInfo.setVisibility(View.GONE);
	}

	public void updateTxPowerValue(final int value)
	{
		if(value != 100)
		{
			mTxtTxPowerValue.setText(value + " dBm");
			for(int i=0; i<mTxPowerArray.length;i++)
			{
				if(String.valueOf(value).equals(mTxPowerArray[i]))
				{
					spinnerTxPower.setSelection(i);
					break;
				}
			}
		}
	}

	public void updateConnectionState(final int resourceId)
	{
		mTxtConnectionState.setText(resourceId);
		if (resourceId == R.string.connected)
		{
			mTxtConnectionState.setTextColor(Color.GREEN);
		}
		else if (resourceId == R.string.disconnected)
		{
			mTxtConnectionState.setTextColor(Color.RED);
		}
		else
		{
			mTxtConnectionState.setTextColor(Color.WHITE);
		}
    }

	public void updateActionBarMenu()
	{
		if(mConnected)
		{
			mBtnConnect.setText("disconnect");
		}
		else
		{
			mBtnConnect.setText("connect");
		}
	}

	public void displayBleRssi(final int rssi, final int avgRssi)
	{
		mTxtRssi.setText(String.format(" %d dBm", rssi));
		mTxtAverageRssi.setText(String.format(" %d dBm", avgRssi));
	}

	public void updateThermalInfo(final int mode,final String... param)
	{
		int paramIndex = 0;
		if((mode & 0x1) > 0)
		{
			txtFirstConnect.setText(param[paramIndex]);
			paramIndex++;
		}

		if((mode & 0x2) > 0)
		{
			txtLastConnect.setText(param[paramIndex]);
			paramIndex++;
		}

		if((mode & 0x4) > 0)
		{
			txtFirstDisconnect.setText(param[paramIndex]);
			paramIndex++;
		}

		if((mode & 0x8) > 0)
		{
			txtLastDisconnect.setText(param[paramIndex]);
			paramIndex++;
		}

		if((mode & 0x10) > 0)
		{
			txtThermalCommandCount.setText(param[paramIndex]);
			paramIndex++;
		}

		if((mode & 0x20) > 0)
		{
			txtThermalCstaCount.setText(param[paramIndex]);
			paramIndex++;
		}

		if((mode & 0x40) > 0)
		{
			txtDisconnectCount.setText(param[paramIndex]);
			paramIndex++;
		}
	}

	public void appendLog(final boolean autoScrollDown, final String log)
	{
/*
String text = "This is <font color='red'>red</font>. This is <font color='blue'>blue</font>.";
textView.setText(Html.fromHtml(text), TextView.BufferType.SPANNABLE);
*/
		//mTxtLog.append(log+"\n");

		if(mTxtLog.getLineCount() >=280)
		{
			//mTxtLog.setText("");
		}


		final Date date = new Date();
		final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM-dd HH:mm:ss.sss", Locale.ENGLISH);
		final String time = String.format("[ %s ]",simpleDateFormat.format(date));
		final Spanned text = Html.fromHtml(time + "<BR/>" + log + "<BR/>");
		mTxtLog.append(text);
		appendLog(time + "\n" + log + "\n");

		if(autoScrollDown)
		{
			((TabActivity)getActivity()).getHandler().postDelayed(new Runnable() {
				@Override
				public void run() {
					mScrollViewLog.fullScroll(View.FOCUS_DOWN);
				}
			}, 600);
		}
	}

	private File getLogFile()
	{
		final String FORMAT_FULL_TIME = "yyyy-MM-dd HH mm ss";
		if(mLogFile == null)
		{
			final String dirName = "SLBLE";
			//final String dirName = "Download";
			final File logPath = new File(combinePath(Environment.getExternalStorageDirectory().getPath(),dirName));
			mLogFile = new File(logPath.getAbsolutePath(),String.format("%s.log", TimeUtil.getTimeNow(FORMAT_FULL_TIME)));

			if(logPath.exists() == false)
				logPath.mkdirs();

			if (!mLogFile.exists())
			{
				try
				{
					mLogFile.createNewFile();
				}
				catch (IOException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			MediaScannerConnection.scanFile(context, new String[]{logPath.getAbsolutePath()}, null, null);
		}

		return mLogFile;
	}

	public void appendLog(final String text)
	{
		try
		{
			//BufferedWriter for performance, true to set append to file flag
			BufferedWriter buf = new BufferedWriter(new FileWriter(getLogFile(), true));
			buf.append(text);
			buf.newLine();
			buf.close();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public String combinePath(String... paths)
    {
        File file = new File(paths[0]);

        for (int i = 1; i < paths.length ; i++)
		{
            file = new File(file, paths[i]);
        }

        return file.getPath();
    }
}

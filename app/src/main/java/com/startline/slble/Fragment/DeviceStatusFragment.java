package com.startline.slble.Fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.startline.slble.R;
import com.startline.slble.Service.BluetoothLeIndependentService;
import com.startline.slble.Util.LogUtil;
import com.startline.slble.module.StaCstaDecode;

import static com.startline.slble.PureClass.SlbleProtocol.*;

/**
 * Created by terry on 2015/8/4.
 */
public class DeviceStatusFragment extends Fragment
{
    //*****************************************************************//
    //  Constant Variables                                             //
    //*****************************************************************//
	private final static String TAG = DeviceStatusFragment.class.getSimpleName();

    //*****************************************************************//
    //  Global Variables                                               //
    //*****************************************************************//


    //*****************************************************************//
    //  Object                                                         //
    //*****************************************************************//
    private Context context;
    //*****************************************************************//
    //  View                                                           //
    //*****************************************************************//
	private View rootView = null;
	private LinearLayout layoutControlSection = null;

	private ImageView imgDoor = null;
	private ImageView imgHood = null;
	private ImageView imgTrunk = null;
	private ImageView imgIgn = null;
	private ImageView imgRunning = null;
	private ImageView imgArm = null;
	private ImageView imgRemoteStart = null;
	private ImageView imgValet = null;
	private ImageView imgAlarm = null;
	private ImageView imgPBrake = null;
	private ImageView imgHBrake = null;
	private ImageView imgHijack = null;
	private EditText editCommand = null;
	private ImageView imgConnectStatus = null;
	private TextView txtConnectStatus = null;
	private TextView txtProcess = null;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		if(rootView == null)
		{
			rootView = inflater.inflate(R.layout.device_status_fragment, container, false);
			setupViews();
		}

		return rootView;
	}

	private void setupViews()
	{
   		imgDoor = (ImageView)rootView.findViewById(R.id.img_door);
		imgHood = (ImageView)rootView.findViewById(R.id.img_hood);
		imgTrunk = (ImageView)rootView.findViewById(R.id.img_trunk);
		imgIgn = (ImageView)rootView.findViewById(R.id.img_ign);
		imgRunning = (ImageView)rootView.findViewById(R.id.img_running);
		imgArm = (ImageView)rootView.findViewById(R.id.img_arm);

		imgRemoteStart = (ImageView)rootView.findViewById(R.id.img_remote_start);
		imgValet = (ImageView)rootView.findViewById(R.id.img_valet);
		imgAlarm = (ImageView)rootView.findViewById(R.id.img_alarm);
		imgPBrake = (ImageView)rootView.findViewById(R.id.img_p_brake);
		imgHBrake = (ImageView)rootView.findViewById(R.id.img_h_brake);
		imgHijack = (ImageView)rootView.findViewById(R.id.img_hijack);

		layoutControlSection = (LinearLayout)rootView.findViewById(R.id.layout_control_section);
		imgConnectStatus = (ImageView)rootView.findViewById(R.id.img_connection_status);
		txtConnectStatus = (TextView)rootView.findViewById(R.id.txt_connection_status);
		txtProcess = (TextView)rootView.findViewById(R.id.txt_process);

		editCommand = (EditText)rootView.findViewById(R.id.edit_command);

		final Button btnArm = (Button)rootView.findViewById(R.id.btn_arm);
		final Button btnDisarm = (Button)rootView.findViewById(R.id.btn_disarm);
		final Button btnPanic = (Button)rootView.findViewById(R.id.btn_panic);
		final Button btnRemoteStart = (Button)rootView.findViewById(R.id.btn_remote_start);
		final Button btnRemoteStop = (Button)rootView.findViewById(R.id.btn_remote_stop);
		final Button btnCheck = (Button)rootView.findViewById(R.id.btn_check);
		final Button btnCustomCommand = (Button)rootView.findViewById(R.id.btn_command);
		final View.OnClickListener btnOnCLick = new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
			 	handleCommandButtonCLick(v);
			}
		};
		btnArm.setOnClickListener(btnOnCLick);
		btnDisarm.setOnClickListener(btnOnCLick);
		btnPanic.setOnClickListener(btnOnCLick);
		btnRemoteStart.setOnClickListener(btnOnCLick);
		btnRemoteStop.setOnClickListener(btnOnCLick);
		btnCheck.setOnClickListener(btnOnCLick);
		btnCustomCommand.setOnClickListener(btnOnCLick);
	}

	private void handleCommandButtonCLick(final View view)
	{
		int command = -1;
		switch (view.getId())
		{
			case R.id.btn_arm:
			{
				command = CONTROL_ALARM_ARM_LOCK_WITH_SIREN_CHIRP;
			}
			break;
			case R.id.btn_disarm:
			{
				command = CONTROL_ALARM_DISARM_UNLOCK_WITH_SIREN_CHIRP;
			}
			break;
			case R.id.btn_panic:
			{
				command = CONTROL_ALARM_PANIC_HIJACK;
			}
			break;
			case R.id.btn_remote_start:
			{
				//command = CONTROL_START_REMOTE_ENGINE_START;

				final BluetoothLeIndependentService service = BluetoothLeIndependentService.getInstance();
				service.writeProgramTable(0,0,null);
			}
			break;
			case R.id.btn_remote_stop:
			{
				//command = CONTROL_START_REMOTE_ENGINE_STOP;

				final BluetoothLeIndependentService service = BluetoothLeIndependentService.getInstance();
				service.sendProgramTableData();
			}
			break;
			case R.id.btn_check:
			{
				//command = CONTROL_ALARM_CHECK_CAR_STATUS;
				final BluetoothLeIndependentService service = BluetoothLeIndependentService.getInstance();
				service.readProgramTable(0,0,32);
			}
			break;
			case R.id.btn_command:
			{
				final String str = editCommand.getText().toString();
				if(str.length() > 0)
				{
					try
					{
						final int intCmd = Integer.parseInt(str);
						sendCommand(intCmd);
					}
					catch (Exception e)
					{
						LogUtil.e(TAG,e.toString(),Thread.currentThread().getStackTrace());
					}
				}
			}

			default:
				break;
		}

		if(command >=0 )
		{
			sendCommand(command);
		}
	}


	private void sendCommand(final int command)
	{
        final Intent intent = new Intent(BluetoothLeIndependentService.ACTION_UI_NOTIFY_SERVICE);
		intent.putExtra("mode",BluetoothLeIndependentService.MODE_CONTROL_COMMAND);
		intent.putExtra("param", command);
        sendCommand(intent);
	}

	public void disableEnableControls(final ViewGroup viewGroup,final boolean enable)
	{
		for (int i = 0; i < viewGroup.getChildCount(); i++)
		{
		   View child = viewGroup.getChildAt(i);
		   child.setEnabled(enable);
		   if (child instanceof ViewGroup)
		   {
			  disableEnableControls((ViewGroup)child,enable);
		   }
		}
	}


	//==============================================================================
	//  Call TabActivity function
	//==============================================================================
	private void sendCommand(final Intent intent)
	{
		getActivity().sendBroadcast(intent);
	}

	//==============================================================================
	//  For TabActivity function
	//==============================================================================
	public void updateProcess(final String message)
	{
		txtProcess.setText(message);
		txtConnectStatus.setText("");
    }

	public void updateConnectionStatus(final int stringId)
	{
		txtProcess.setText("");
		txtConnectStatus.setText(getString(stringId));
    }

	public void updateConnectionStatusIcon(final boolean activated,final boolean selected)
	{
		imgConnectStatus.setActivated(activated);
		imgConnectStatus.setSelected(selected);

		if(activated && selected)
		{
			disableEnableControls(layoutControlSection,true);
		}
		else
		{
			disableEnableControls(layoutControlSection,false);
		}
	}

	public void displayDeviceStatus(final int mode,final long csta)
	{
		// Error
	 	if(csta < 0)
		{

			return;
		}

		switch (mode)
		{
			case BluetoothLeIndependentService.CSTA_STA:
			{
				final StaCstaDecode cstaDecode = new StaCstaDecode(csta);
				imgDoor.setSelected(cstaDecode.getValue(StaCstaDecode.CSTA_DOOR)==1?true:false);
				imgHood.setSelected(cstaDecode.getValue(StaCstaDecode.CSTA_HOOD)==1?true:false);
				imgTrunk.setSelected(cstaDecode.getValue(StaCstaDecode.CSTA_TRUNK)==1?true:false);
				imgIgn.setSelected(cstaDecode.getValue(StaCstaDecode.CSTA_IGN)==1?true:false);
				imgRunning.setSelected(cstaDecode.getValue(StaCstaDecode.CSTA_RUNNING)==1?true:false);
				imgArm.setSelected(cstaDecode.getValue(StaCstaDecode.CSTA_ARM)==1?true:false);
				imgRemoteStart.setSelected(cstaDecode.getValue(StaCstaDecode.CSTA_REMOTE_START)==1?true:false);
				imgValet.setSelected(cstaDecode.getValue(StaCstaDecode.CSTA_VALET)==1?true:false);
				imgAlarm.setSelected(cstaDecode.getValue(StaCstaDecode.CSTA_ALARM)==1?true:false);
				imgPBrake.setSelected(cstaDecode.getValue(StaCstaDecode.CSTA_P_BRAKE)==1?true:false);
				imgHBrake.setSelected(cstaDecode.getValue(StaCstaDecode.CSTA_H_BRAKE)==1?true:false);
				imgHijack.setSelected(cstaDecode.getValue(StaCstaDecode.CSTA_HIJACK)==1?true:false);
			}
			break;
		}
	}
}

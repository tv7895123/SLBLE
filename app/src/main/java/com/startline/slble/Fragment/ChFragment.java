package com.startline.slble.Fragment;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.startline.slble.Activity.ProgramToolActivity;
import com.startline.slble.Interface.OnProgramDataChangedListener;
import com.startline.slble.Interface.ProgramTool;
import com.startline.slble.R;
import com.startline.slble.Util.DialogUtil;
import com.startline.slble.Util.LogUtil;

import static com.startline.slble.Interface.ProgramTool.*;

public class ChFragment extends BaseFragment
{
    final String COND_0_NONE ="[ 0 ] None";
    final String COND_1_ARMED = "[ 1 ] ARMED status";
    final String COND_2_DISARMED = "[ 2 ] DISARMED status";
    final String COND_3_IGN_ON = "[ 3 ] IGN ON status";
    final String COND_4_IGN_OFF = "[ 4 ] IGN OFF status";
    final String COND_5_ENGINE_START_BY_FACTORY = "[ 5 ] Engine was started by factory KEY";
    final String COND_6_ENGINE_START_IN_TT_MODE = "[ 6 ] Engine was started end in TT mode";
    final String COND_7_ENGINE_REMOTE_START = "[ 7 ] Engine was remote started by car alarm";
    final String COND_8_ENGINE_REMOTE_START_SUCCESS = "[ 8 ] Engine was started end in  'Arm with engine running' ";
    final String[] FULL_CONDITION = new String[]{COND_0_NONE,COND_1_ARMED,COND_2_DISARMED,COND_3_IGN_ON,COND_4_IGN_OFF,COND_5_ENGINE_START_BY_FACTORY,COND_6_ENGINE_START_IN_TT_MODE,COND_7_ENGINE_REMOTE_START};

    private EditText editT1,editT2,editT3,editT4;
    private CheckBox checkPBrake,checkTrunk,checkDoor,checkSensor;
    private Spinner spinnerChannel,spinnerFunction,spinnerEventOn,spinnerCondOn1,spinnerCondOn2,spinnerCondOn3;
    private Spinner spinnerEventOff,spinnerCondOff1,spinnerCondOff2,spinnerCondOff3;
    private int mCurrentChannel = 0;

    private class MyArrayAdapter<T> extends ArrayAdapter<T>
    {
        public MyArrayAdapter(Context context, int resource, T[] objects) {
            super(context, resource, objects);
        }

        @Override
        public View getDropDownView(final int position, View convertView, ViewGroup parent)
        {
            if(convertView == null)
            {
                convertView = View.inflate(getActivity(),R.layout.spinner_drop_down_item,null);
            }

            final CheckedTextView item = (CheckedTextView) convertView;
            if(item != null)
            {
                item.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,(int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 45, getResources().getDisplayMetrics())));
                item.setText((String)getItem(position));
                item.post(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        item.setSingleLine(false);
                    }
                });
            }

            return item;
        }
    }

    private class GenericTextWatcher implements TextWatcher
    {
        private View view;
        private GenericTextWatcher(View view)
        {
            this.view = view;
        }

        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2)
        {

        }
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2)
        {

        }

        public void afterTextChanged(Editable editable)
        {
            int oldTime = 0,time = 0;
            final String text = editable.toString();
            if(text.length() > 0)
            {
                try
                {
                    time = Integer.parseInt(text);
                }
                catch (Exception e)
                {
                    LogUtil.e(TAG,e.toString(),Thread.currentThread().getStackTrace());
                }
            }
            switch(view.getId())
            {
                case R.id.edit_output_t1:
                {
                    oldTime = mModifiedData[CH_ITEM_T1+1]*256 + mModifiedData[CH_ITEM_T1];
                    if(oldTime != time)
                    {
                        mModifiedData[CH_ITEM_T1] = (byte)(time % 256);
                        mModifiedData[CH_ITEM_T1+1] = (byte)(time / 256);
                        notifyDataChanged();
                    }
                }
                break;
                case R.id.edit_output_t2:
                {
                    oldTime = mModifiedData[CH_ITEM_T2+1]*256 + mModifiedData[CH_ITEM_T2];
                    if(oldTime != time)
                    {
                        mModifiedData[CH_ITEM_T2] = (byte)(time % 256);
                        mModifiedData[CH_ITEM_T2+1] = (byte)(time / 256);
                        notifyDataChanged();
                    }
                }
                break;
                case R.id.edit_output_t3:
                {
                    oldTime = mModifiedData[CH_ITEM_T3+1]*256 + mModifiedData[CH_ITEM_T3];
                    if(oldTime != time)
                    {
                        mModifiedData[CH_ITEM_T3] = (byte)(time % 256);
                        mModifiedData[CH_ITEM_T3+1] = (byte)(time / 256);
                        notifyDataChanged();
                    }
                }
                break;
                case R.id.edit_output_t4:
                {
                    oldTime = mModifiedData[CH_ITEM_T4+1]*256 + mModifiedData[CH_ITEM_T4];
                    if(oldTime != time)
                    {
                        mModifiedData[CH_ITEM_T4] = (byte)(time % 256);
                        mModifiedData[CH_ITEM_T4+1] = (byte)(time / 256);
                        notifyDataChanged();
                    }
                }
                break;
            }
        }
    }

    private AdapterView.OnItemSelectedListener onSpinnerItemClick = new AdapterView.OnItemSelectedListener()
    {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, final int position, long id)
        {
            if(view == null)return;

            boolean dataChanged = false;

            byte value = (byte)position;
            switch (parent.getId())
            {
                case R.id.spinner_channel:
                {
                    // If data changed, need to show a prompt dialog
                    if(ProgramToolActivity.isDataChanged(mInitData,mModifiedData))
                    {
                        // Only show dialog when switch to another channel
                        // No need show dialog when switch back
                        if(mCurrentChannel != position+1)
                        {
                            final DialogInterface.OnClickListener onSaveClick =  new DialogInterface.OnClickListener()
                            {
                                @Override
                                public void onClick(DialogInterface dialog, int which)
                                {
                                    spinnerChannel.setSelection(mCurrentChannel-1);
                                    ((ProgramToolActivity)getActivity()).saveSetting();
                                }
                            };
                            final DialogInterface.OnClickListener onDiscardClick =  new DialogInterface.OnClickListener()
                            {
                                @Override
                                public void onClick(DialogInterface dialog, int which)
                                {
                                    mCurrentChannel = position + 1;
                                    ((ProgramToolActivity)getActivity()).syncSetting();
                                }
                            };

                            DialogUtil.messageDialog(context,getString(R.string.setting_changed_prompt_title),getString(R.string.setting_changed_prompt),onSaveClick,onDiscardClick,R.string.save,R.string.discard);
                        }
                    }
                    // Data not changed
                    else
                    {
                        // If channel index changed, notify parent
                        if(mCurrentChannel != position + 1)
                        {
                            mCurrentChannel = position + 1;
                            ((ProgramToolActivity)getActivity()).syncSetting();
                        }
                    }
                }
                break;
                case R.id.spinner_function:
                {
                    final LinearLayout v = (LinearLayout)mRootView.findViewById(R.id.layout_settings);
                    if(position == 0)
                    {
                        disableEnableControls(v,true);
                    }
                    else
                    {
                        disableEnableControls(v,false);
                    }

                    if(mModifiedData[CH_ITEM_FUNCTION] != value)
                    {
                        mModifiedData[CH_ITEM_FUNCTION] = value;
                        dataChanged = true;
                    }
                }
                break;
                case R.id.spinner_event_on:
                {
                    if(mModifiedData[CH_ITEM_EVENT_ON] != value)
                    {
                        mModifiedData[CH_ITEM_EVENT_ON] = value;
                        dataChanged = true;
                    }

                    updateSpinnerConditionOn(position);
                }
                break;
                case R.id.spinner_on_condition_1:
                {
                    final int conditionIndex = getConditionIndex((String)spinnerCondOn1.getAdapter().getItem(position));
                    value = (byte)((mModifiedData[CH_ITEM_CONDITION_1] & 0xF0 ) | conditionIndex);

                    if(mModifiedData[CH_ITEM_CONDITION_1] != value)
                    {
                        mModifiedData[CH_ITEM_CONDITION_1] = value;
                        dataChanged = true;
                    }
                }
                break;
                case R.id.spinner_on_condition_2:
                {
                    final int conditionIndex = getConditionIndex((String)spinnerCondOn2.getAdapter().getItem(position));
                    value = (byte)((mModifiedData[CH_ITEM_CONDITION_2] & 0xF0 ) | conditionIndex);

                    if(mModifiedData[CH_ITEM_CONDITION_2] != value)
                    {
                        mModifiedData[CH_ITEM_CONDITION_2] = value;
                        dataChanged = true;
                    }
                }
                break;
                case R.id.spinner_on_condition_3:
                {
                    final int conditionIndex = getConditionIndex((String)spinnerCondOn3.getAdapter().getItem(position));
                    value = (byte)((mModifiedData[CH_ITEM_CONDITION_3] & 0xF0 ) | conditionIndex);

                    if(mModifiedData[CH_ITEM_CONDITION_3] != value)
                    {
                        mModifiedData[CH_ITEM_CONDITION_3] = value;
                        dataChanged = true;
                    }
                }
                break;
                case R.id.spinner_event_off:
                {
                    if(mModifiedData[CH_ITEM_EVENT_OFF] != value)
                    {
                        mModifiedData[CH_ITEM_EVENT_OFF] = value;
                        dataChanged = true;
                    }

                    updateSpinnerConditionOff(position);
                }
                break;
                case R.id.spinner_off_condition_1:
                {
                    final int conditionIndex = getConditionIndex((String)spinnerCondOff1.getAdapter().getItem(position));
                    value = (byte)((mModifiedData[CH_ITEM_CONDITION_1] & 0x0F ) | (conditionIndex<<4));

                    if(mModifiedData[CH_ITEM_CONDITION_1] != value)
                    {
                        mModifiedData[CH_ITEM_CONDITION_1] = value;
                        dataChanged = true;
                    }
                }
                break;
                case R.id.spinner_off_condition_2:
                {
                    final int conditionIndex = getConditionIndex((String)spinnerCondOff2.getAdapter().getItem(position));
                    value = (byte)((mModifiedData[CH_ITEM_CONDITION_2] & 0x0F ) | (conditionIndex<<4));

                    if(mModifiedData[CH_ITEM_CONDITION_2] != value)
                    {
                        mModifiedData[CH_ITEM_CONDITION_2] = value;
                        dataChanged = true;
                    }
                }
                break;
                case R.id.spinner_off_condition_3:
                {
                    final int conditionIndex = getConditionIndex((String)spinnerCondOff3.getAdapter().getItem(position));
                    value = (byte)((mModifiedData[CH_ITEM_CONDITION_3] & 0x0F ) | (conditionIndex<<4));

                    if(mModifiedData[CH_ITEM_CONDITION_3] != value)
                    {
                        mModifiedData[CH_ITEM_CONDITION_3] = value;
                        dataChanged = true;
                    }
                }
                break;
            }

            if(dataChanged)
            {
                notifyDataChanged();
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent)
        {

        }
    };

    private CompoundButton.OnCheckedChangeListener onCheckedChangeListener = new CompoundButton.OnCheckedChangeListener()
    {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
        {
            final int value = isChecked?1:0;
            switch (buttonView.getId())
            {
                case R.id.ck_pbrake:
                {
                    mModifiedData[CH_ITEM_BYPASS] = setBit(mModifiedData[CH_ITEM_BYPASS],3,value);
                }
                break;
                case R.id.ck_trunk:
                {
                    mModifiedData[CH_ITEM_BYPASS] = setBit(mModifiedData[CH_ITEM_BYPASS],2,value);
                }
                break;
                case R.id.ck_door:
                {
                    mModifiedData[CH_ITEM_BYPASS] = setBit(mModifiedData[CH_ITEM_BYPASS],1,value);
                }
                break;
                case R.id.ck_sensor:
                {
                    mModifiedData[CH_ITEM_BYPASS] = setBit(mModifiedData[CH_ITEM_BYPASS],0,value);
                }
                break;
            }

            notifyDataChanged();
        }
    };

    public static ChFragment newInstance(final int index,String title, int indicatorColor, int dividerColor)
    {
        return newInstance(index,title,indicatorColor,dividerColor,0,null);
    }

    public static ChFragment newInstance(final int index,String title, int indicatorColor, int dividerColor, final OnProgramDataChangedListener onProgramDataChangedListener)
    {
        return newInstance(index,title,indicatorColor,dividerColor,0,onProgramDataChangedListener);
    }

    public static ChFragment newInstance(final int index,String title, int indicatorColor, int dividerColor, int iconResId)
    {
        return newInstance(index,title,indicatorColor,dividerColor,iconResId,null);
    }

    public static ChFragment newInstance(final int index,String title, int indicatorColor, int dividerColor, int iconResId, final OnProgramDataChangedListener onProgramDataChangedListener)
    {
        ChFragment f = new ChFragment();
        f.setTitle(title);
        f.setIndicatorColor(indicatorColor);
        f.setDividerColor(dividerColor);
        f.setIconResId(iconResId);
        f.setOnProgramDataChangedListener(onProgramDataChangedListener);
        f.setPageIndex(index);

        //pass data
        Bundle args = new Bundle();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        if(mRootView == null)
        {
            mRootView = inflater.inflate(R.layout.fragment_ch, container, false);
        }

        return mRootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        context = getActivity();
        editT1 = (EditText)mRootView.findViewById(R.id.edit_output_t1);
        editT2 = (EditText)mRootView.findViewById(R.id.edit_output_t2);
        editT3 = (EditText)mRootView.findViewById(R.id.edit_output_t3);
        editT4 = (EditText)mRootView.findViewById(R.id.edit_output_t4);

        checkPBrake = (CheckBox)mRootView.findViewById(R.id.ck_pbrake);
        checkTrunk = (CheckBox)mRootView.findViewById(R.id.ck_trunk);
        checkDoor = (CheckBox)mRootView.findViewById(R.id.ck_door);
        checkSensor = (CheckBox)mRootView.findViewById(R.id.ck_sensor);

        spinnerChannel = (Spinner)mRootView.findViewById(R.id.spinner_channel);
        spinnerFunction = (Spinner)mRootView.findViewById(R.id.spinner_function);

        spinnerEventOn = (Spinner)mRootView.findViewById(R.id.spinner_event_on);
        spinnerCondOn1 = (Spinner)mRootView.findViewById(R.id.spinner_on_condition_1);
        spinnerCondOn2 = (Spinner)mRootView.findViewById(R.id.spinner_on_condition_2);
        spinnerCondOn3 = (Spinner)mRootView.findViewById(R.id.spinner_on_condition_3);

        spinnerEventOff = (Spinner)mRootView.findViewById(R.id.spinner_event_off);
        spinnerCondOff1 = (Spinner)mRootView.findViewById(R.id.spinner_off_condition_1);
        spinnerCondOff2 = (Spinner)mRootView.findViewById(R.id.spinner_off_condition_2);
        spinnerCondOff3 = (Spinner)mRootView.findViewById(R.id.spinner_off_condition_3);

        editT1.addTextChangedListener(new GenericTextWatcher(editT1));
        editT2.addTextChangedListener(new GenericTextWatcher(editT2));
        editT3.addTextChangedListener(new GenericTextWatcher(editT3));
        editT4.addTextChangedListener(new GenericTextWatcher(editT4));

        checkPBrake.setOnCheckedChangeListener(onCheckedChangeListener);
        checkTrunk.setOnCheckedChangeListener(onCheckedChangeListener);
        checkDoor.setOnCheckedChangeListener(onCheckedChangeListener);
        checkSensor.setOnCheckedChangeListener(onCheckedChangeListener);

        spinnerChannel.setAdapter(new MyArrayAdapter<String>(getActivity(), R.layout.spinner_item,
        new String[]
        {
                "[ 1 ] Channel 1", "[ 2 ] Channel 2", "[ 3 ] Channel 3", "[ 4 ] Channel 4"
                , "[ 5 ] Channel 5", "[ 6 ] Channel 6", "[ 7 ] Channel 7", "[ 8 ] Channel 8"
                , "[ 9 ] Channel 9", "[ 10 ] Channel 10", "[ 11 ] Channel 11", "[ 12 ] Channel 12"
                //"[ 1 ] Trunk release", "[ 2 ] 2-step unlock", "[ 3 ] Engine stop delay 2'S then output 3'S", "[ 4 ] After Arm (20'S)", "[ 5 ] Start killer", "[ 6 ] Remote Control (4L+2)", "[ 7 ] Lock Output", "[ 8 ] UnLock Output", "[ 9 ] Parking Light ", "[ 10 ] IGN", "[ 11 ] ACC", "[ 12 ] IGN2"
        }));

        spinnerFunction.setAdapter(new MyArrayAdapter<String>(getActivity(), R.layout.spinner_item,
        new String[]
        {
                  "[ 0 ] None", "[ 1 ] DOOR LOCK impulse 1 sec" ,"[ 2 ] DOOR UNLOCK impulse 1 sec" ,"[ 3 ] DOOR LOCK impulse 1 sec (once when arming)" ,"[ 4 ] DOOR UNLOCK impulse 1 sec (once when disarming)"
                , "[ 5 ] DOOR LOCK impulse 1 sec twice" , "[ 6 ] DOOR UNLOCK impulse 1 sec twice" , "[ 7 ] DOOR LOCK impulse 4 sec" , "[ 8 ] DOOR UNLOCK impulse 4 sec"
                , "[ 9 ] DOOR LOCK impulse 30 sec (comfort)" , "[ 10 ] 2-step unlock impulse 1 sec" , "[ 11 ] 2-step unlock impulse 1 sec twice" , "[ 12 ] 2-step unlock impulse 4 sec"
                , "[ 13 ] Trunk release - impulse 1 sec" , "[ 14 ] ----" , "[ 15 ] ----" , "[ 16 ] ----"
                , "[ 17 ] Parking light button" , "[ 18 ] Parking lightflash" , "[ 19 ] Impulse control of light signals", "[ 20 ] Engine stop delay 2 sec then output 3 sec (bypass door)"
                , "[ 21 ] Engine stop delay 1 sec then output 1 sec (bypass door)" , "[ 22 ] Hood Lock (R3)" , "[ 23 ] Engine Killer mode" , "[ 24 ] IGN"
                , "[ 25 ] IGN*(Output is OFF when cranking)" , "[ 26 ] ACC" , "[ 27 ] ACC*(2sec before IGN Output and keep when cranking)" , "[ 28 ] START"
                , "[ 29 ] Pedal brake pressing during remote engine starting" , "[ 30 ] Analog Bypass output" , "[ 31 ] Start Killer mode" , "[ 32 ] Digital Control Webasto"
                , "[ 33 ] Analog Control Webasto" , "[ 34 ] After Arm 20 sec" , "[ 35 ] After Disrm 20 sec" , "[ 36 ] DVR Control function", "[ 37 ] Digital Control Eberspacher"
        }));

        final String[] event = new String[]
        {
            "[ 0 ] No Event" ,"[ 1 ] ---" ,"[ 2 ] Armed" ,"[ 3 ] Disarmed" ,"[ 4 ] Armed or Disarmed" ,"[ 5 ] Disarmed or IGN OFF"
            ,"[ 6 ] IGN ON" ,"[ 7 ] IGN OFF" ,"[ 8 ] Door LOCK" ,"[ 9 ] Door UNLOCK" ,"[ 10 ] Alarm"
            ,"[ 11 ] Hand Brake UP(ON)" ,"[ 12 ] Hand Break Down(OFF)" ,"[ 13 ] Engine Is Starts To Run" ,"[ 14 ] Successful Start" ,"[ 15 ] Not Successful Start (4T)"
            ,"[ 16 ] Any Start by Alarm" ,"[ 17 ] Any shut down by Alarm" ,"[ 18 ] Any Engine Shut Down" ,"[ 19 ] Pulse to Push Start (START)" ,"[ 20 ] Pulse to Push Start (STOP)"
            ,"[ 21 ] Transfer From Remote Engine Start to KEY" ,"[ 22 ] Event 1 input ON" ,"[ 23 ] Event 1 input OFF" ,"[ 24 ] Slave TAG Search successful" ,"[ 25 ] Trunk release"
            ,"[ 26 ] 2-Step Unlock" ,"[ 27 ] Start Killer ON" ,"[ 28 ] Start Killer OFF" ,"[ 29 ] Engine Killer ON" ,"[ 30 ] Engine Killer OFF"
            ,"[ 31 ] Trunk Open" ,"[ 32 ] Trunk Close" ,"[ 33 ] Remote Control (2L+1)" ,"[ 34 ] Remote Control (3L+1)" ,"[ 35 ] Remote Control (4L+1)"
            ,"[ 36 ] Remote Control (2L+3)" ,"[ 37 ] Remote Control (3L+2)" ,"[ 38 ] Remote Control (4L+2)" ,"[ 39 ] Event 2 input ON" ,"[ 40 ] Event 2 input OFF"
            ,"[ 41 ] BT Connected","[ 42 ] BT Disconnected"
        };
        spinnerEventOn.setAdapter(new MyArrayAdapter<String>(getActivity(), R.layout.spinner_item,event));
        spinnerEventOff.setAdapter(new MyArrayAdapter<String>(getActivity(), R.layout.spinner_item,event));

        spinnerChannel.setSelection(0);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                spinnerChannel.setOnItemSelectedListener(onSpinnerItemClick);
                spinnerFunction.setOnItemSelectedListener(onSpinnerItemClick);
                spinnerEventOn.setOnItemSelectedListener(onSpinnerItemClick);
                spinnerCondOn1.setOnItemSelectedListener(onSpinnerItemClick);
                spinnerCondOn2.setOnItemSelectedListener(onSpinnerItemClick);
                spinnerCondOn3.setOnItemSelectedListener(onSpinnerItemClick);
                spinnerEventOff.setOnItemSelectedListener(onSpinnerItemClick);
                spinnerCondOff1.setOnItemSelectedListener(onSpinnerItemClick);
                spinnerCondOff2.setOnItemSelectedListener(onSpinnerItemClick);
                spinnerCondOff3.setOnItemSelectedListener(onSpinnerItemClick);
            }
        },1000);
    }

    @Override
    public void onResume()
    {
        super.onResume();

        mRootView.findViewById(R.id.layout_channel).requestFocus();

        refresh();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
    }

    @Override
    public void onDestroyView()
    {
        super.onDestroyView();
    }


    @Override
    public void refresh()
    {
        if(mModifiedData == null)
            return;

        if(context == null)
            return;

        spinnerFunction.setSelection(mModifiedData[CH_ITEM_FUNCTION]);

        editT1.setText(String.valueOf((mModifiedData[CH_ITEM_T1+1] & 0xFF)*256 + (mModifiedData[CH_ITEM_T1] & 0xFF)));
        editT2.setText(String.valueOf((mModifiedData[CH_ITEM_T2+1] & 0xFF)*256 + (mModifiedData[CH_ITEM_T2] & 0xFF)));
        editT3.setText(String.valueOf((mModifiedData[CH_ITEM_T3+1] & 0xFF)*256 + (mModifiedData[CH_ITEM_T3] & 0xFF)));
        editT4.setText(String.valueOf((mModifiedData[CH_ITEM_T4+1] & 0xFF)*256 + (mModifiedData[CH_ITEM_T4] & 0xFF)));

        spinnerEventOn.setSelection(mModifiedData[CH_ITEM_EVENT_ON] < 0 ? 0 : mModifiedData[CH_ITEM_EVENT_ON] > spinnerEventOn.getAdapter().getCount()? spinnerEventOn.getAdapter().getCount()-1: mModifiedData[CH_ITEM_EVENT_ON]);
        spinnerEventOff.setSelection(mModifiedData[CH_ITEM_EVENT_OFF] < 0 ? 0 : mModifiedData[CH_ITEM_EVENT_OFF] > spinnerEventOff.getAdapter().getCount()? spinnerEventOff.getAdapter().getCount()-1: mModifiedData[CH_ITEM_EVENT_OFF]);

        updateSpinnerConditionOn(spinnerEventOn.getSelectedItemPosition());
        updateSpinnerConditionOff(spinnerEventOff.getSelectedItemPosition());

        checkPBrake.setChecked((mModifiedData[CH_ITEM_BYPASS] & 0x08) > 0);
        checkTrunk.setChecked((mModifiedData[CH_ITEM_BYPASS] & 0x04) > 0);
        checkDoor.setChecked((mModifiedData[CH_ITEM_BYPASS] & 0x02) > 0);
        checkSensor.setChecked((mModifiedData[CH_ITEM_BYPASS] & 0x01) > 0);
    }

    public int getCurrentChannel()
    {
        return mCurrentChannel;
    }

    public String[] getConditionByEvent(final int event)
    {
        switch (event)
        {
            case 0:
                return new String[]{};
            case 2:
            case 3:
            case 4:
            case 5:
            case 10:
            case 21:
            {
                //0,6,7
                return new String[]
                {
                    COND_0_NONE
                    ,COND_6_ENGINE_START_IN_TT_MODE
                    ,COND_7_ENGINE_REMOTE_START
                };
            }
            case 6:
            case 7:
            case 9:
            case 14:
            case 15:
            case 16:
            case 17:
            case 19:
            //case 20:
            {
                //0,1,2
                return new String[]
                {
                    COND_0_NONE
                    ,COND_1_ARMED
                    ,COND_2_DISARMED
                };
            }

            case 11:
            {
                //0,2,3,4,5
                return new String[]
                {
                    COND_0_NONE
                    ,COND_2_DISARMED
                    ,COND_3_IGN_ON
                    ,COND_4_IGN_OFF
                    ,COND_5_ENGINE_START_BY_FACTORY
                };
            }
            case 12:
            {
                // 0,2,3,4,5,6,7
                return new String[]
                {
                    COND_0_NONE
                    ,COND_2_DISARMED
                    ,COND_3_IGN_ON
                    ,COND_4_IGN_OFF
                    ,COND_5_ENGINE_START_BY_FACTORY
                    ,COND_6_ENGINE_START_IN_TT_MODE
                    ,COND_7_ENGINE_REMOTE_START};
            }
            case 18:
            {
                // 0,1,2,5,6,7
                return new String[]
                {
                    COND_0_NONE
                    ,COND_1_ARMED
                    ,COND_2_DISARMED
                    ,COND_5_ENGINE_START_BY_FACTORY
                    ,COND_6_ENGINE_START_IN_TT_MODE
                    ,COND_7_ENGINE_REMOTE_START
                };
            }
            case 20:
            {
                return new String[]{COND_0_NONE,COND_1_ARMED,COND_2_DISARMED,COND_6_ENGINE_START_IN_TT_MODE,COND_7_ENGINE_REMOTE_START, COND_8_ENGINE_REMOTE_START_SUCCESS};
            }

            case 22:
            case 23:
            case 39:
            case 40:
            {
                // 0,1,2,3,4,5,6,7
                return new String[]
                {
                    COND_0_NONE
                    ,COND_1_ARMED
                    ,COND_2_DISARMED
                    ,COND_3_IGN_ON
                    ,COND_4_IGN_OFF,COND_5_ENGINE_START_BY_FACTORY
                    ,COND_6_ENGINE_START_IN_TT_MODE
                    ,COND_7_ENGINE_REMOTE_START
                };
            }
        }

        return new String[]{};
        //return new String[]{COND_0_NONE};
    }

    private int getConditionIndex(final String condition)
    {
        if(condition.equals(COND_1_ARMED))
        {
            return 1;
        }
        else if(condition.equals(COND_2_DISARMED))
        {
            return 2;
        }
        else if(condition.equals(COND_3_IGN_ON))
        {
            return 3;
        }
        else if(condition.equals(COND_4_IGN_OFF))
        {
            return 4;
        }
        else if(condition.equals(COND_5_ENGINE_START_BY_FACTORY))
        {
            return 5;
        }
        else if(condition.equals(COND_6_ENGINE_START_IN_TT_MODE))
        {
            return 6;
        }
        else if(condition.equals(COND_7_ENGINE_REMOTE_START))
        {
            return 7;
        }
        else if(condition.equals(COND_8_ENGINE_REMOTE_START_SUCCESS))
        {
            return 8;
        }

        return 0;
    }

    private int getConditionIndexByString(final String[] conditions,final String condition)
    {
        if(conditions == null || conditions.length == 0)
             return 0;

        if(condition == null || condition.length() == 0)
            return 0;

        for(int i=0;i<conditions.length;i++)
        {
            if(condition.equals(conditions[i]))
                return i;
        }

        return 0;
    }

    private void updateSpinnerConditionOn(final int position)
    {
        final String[] conditions = getConditionByEvent(position);
        spinnerCondOn1.setAdapter(new MyArrayAdapter<String>(getActivity(), R.layout.spinner_item,conditions));
        spinnerCondOn2.setAdapter(new MyArrayAdapter<String>(getActivity(), R.layout.spinner_item,conditions));
        spinnerCondOn3.setAdapter(new MyArrayAdapter<String>(getActivity(), R.layout.spinner_item,conditions));



        if(position == 0)
        {

        }
        else
        {
            final String[] conditions_on = getConditionByEvent(mModifiedData[CH_ITEM_EVENT_ON]);
            spinnerCondOn1.setSelection(getConditionIndexByString(conditions_on,FULL_CONDITION[mModifiedData[CH_ITEM_CONDITION_1] & 0x0F]));
            spinnerCondOn2.setSelection(getConditionIndexByString(conditions_on,FULL_CONDITION[mModifiedData[CH_ITEM_CONDITION_2] & 0x0F]));
            spinnerCondOn3.setSelection(getConditionIndexByString(conditions_on,FULL_CONDITION[mModifiedData[CH_ITEM_CONDITION_3] & 0x0F]));
        }
    }

    private void updateSpinnerConditionOff(final int position)
    {
        final String[] conditions = getConditionByEvent(position);
        spinnerCondOff1.setAdapter(new MyArrayAdapter<String>(getActivity(), R.layout.spinner_item,conditions));
        spinnerCondOff2.setAdapter(new MyArrayAdapter<String>(getActivity(), R.layout.spinner_item,conditions));
        spinnerCondOff3.setAdapter(new MyArrayAdapter<String>(getActivity(), R.layout.spinner_item,conditions));

        if(position == 0)
        {

        }
        else
        {
            final String[] conditions_off = getConditionByEvent(mModifiedData[CH_ITEM_EVENT_OFF]);
            spinnerCondOff1.setSelection(getConditionIndexByString(conditions_off,FULL_CONDITION[(mModifiedData[CH_ITEM_CONDITION_1] & 0xF0) >> 4]));
            spinnerCondOff2.setSelection(getConditionIndexByString(conditions_off,FULL_CONDITION[(mModifiedData[CH_ITEM_CONDITION_2] & 0xF0) >> 4]));
            spinnerCondOff3.setSelection(getConditionIndexByString(conditions_off,FULL_CONDITION[(mModifiedData[CH_ITEM_CONDITION_3] & 0xF0) >> 4]));
        }
    }

    private byte setBit(final byte src,final int bit,final int value)
    {
        return (byte)((src & ~(0x1 << bit)) | (value << bit));
    }
}

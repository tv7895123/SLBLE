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

import static com.startline.slble.Interface.ProgramTool.*;

public class ChFragment extends BaseFragment
{
    final String COND_NO_CONDITION ="[ 0 ] No conditions";
    final String COND_ARMED = "[ 1 ] ARMED status";
    final String COND_DISARMED = "[ 2 ] DISARMED status";
    final String COND_IGN_ON = "[ 3 ] IGN ON status";
    final String COND_IGN_OFF = "[ 4 ] IGN OFF status";
    final String COND_ENGINE_START_BY_FACTORY = "[ 5 ] Engine was started by factory KEY";
    final String COND_ENGINE_START_IN_TT_MODE = "[ 6 ] Engine was started end in TT mode";
    final String COND_ENGINE_REMOTE_START = "[ 7 ] Engine was remote started by car alarm";
    final String[] FULL_CONDITION = new String[]{COND_NO_CONDITION,COND_ARMED,COND_DISARMED,COND_IGN_ON,COND_IGN_OFF,COND_ENGINE_START_BY_FACTORY,COND_ENGINE_START_IN_TT_MODE,COND_ENGINE_REMOTE_START};

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
                time = Integer.parseInt(text);
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
                    final String[] conditions = getConditionByEvent(position);
                    spinnerCondOn1.setAdapter(new MyArrayAdapter<String>(getActivity(), R.layout.spinner_item,conditions));
                    spinnerCondOn2.setAdapter(new MyArrayAdapter<String>(getActivity(), R.layout.spinner_item,conditions));
                    spinnerCondOn3.setAdapter(new MyArrayAdapter<String>(getActivity(), R.layout.spinner_item,conditions));

                    if(mModifiedData[CH_ITEM_EVENT_ON] != value)
                    {
                        mModifiedData[CH_ITEM_EVENT_ON] = value;
                        dataChanged = true;
                    }

                    if(position == 0)
                    {

                    }
                    else
                    {
                        final String[] conditions_on = getConditionByEvent(mModifiedData[CH_ITEM_EVENT_ON]);
                        spinnerCondOn1.setSelection(getConditionIndexByString(conditions_on,FULL_CONDITION[mModifiedData[CH_ITEM_CONDITION_1 & 0x0F]]));
                        spinnerCondOn2.setSelection(getConditionIndexByString(conditions_on,FULL_CONDITION[mModifiedData[CH_ITEM_CONDITION_2 & 0x0F]]));
                        spinnerCondOn3.setSelection(getConditionIndexByString(conditions_on,FULL_CONDITION[mModifiedData[CH_ITEM_CONDITION_3 & 0x0F]]));
                    }
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
                    final String[] conditions = getConditionByEvent(position);
                    spinnerCondOff1.setAdapter(new MyArrayAdapter<String>(getActivity(), R.layout.spinner_item,conditions));
                    spinnerCondOff2.setAdapter(new MyArrayAdapter<String>(getActivity(), R.layout.spinner_item,conditions));
                    spinnerCondOff3.setAdapter(new MyArrayAdapter<String>(getActivity(), R.layout.spinner_item,conditions));

                    if(mModifiedData[CH_ITEM_EVENT_OFF] != value)
                    {
                        mModifiedData[CH_ITEM_EVENT_OFF] = value;
                        dataChanged = true;
                    }

                    if(position == 0)
                    {

                    }
                    else
                    {
                        final String[] conditions_off = getConditionByEvent(mModifiedData[CH_ITEM_EVENT_OFF]);
                        spinnerCondOn1.setSelection(getConditionIndexByString(conditions_off,FULL_CONDITION[(mModifiedData[CH_ITEM_CONDITION_1 & 0xF0]) >> 4]));
                        spinnerCondOn2.setSelection(getConditionIndexByString(conditions_off,FULL_CONDITION[(mModifiedData[CH_ITEM_CONDITION_2 & 0xF0]) >> 4]));
                        spinnerCondOn3.setSelection(getConditionIndexByString(conditions_off,FULL_CONDITION[(mModifiedData[CH_ITEM_CONDITION_3 & 0xF0]) >> 4]));
                    }
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
                "[ 1 ] Trunk release", "[ 2 ] 2-step unlock", "[ 3 ] Engine stop delay 2'S then output 3'S", "[ 4 ] After Arm (20'S)", "[ 5 ] Start killer", "[ 6 ] Remote Control (4L+2)", "[ 7 ] Lock Output", "[ 8 ] UnLock Output", "[ 9 ] Parking Light ", "[ 10 ] IGN", "[ 11 ] ACC", "[ 12 ] IGN2"
        }));

        spinnerFunction.setAdapter(new MyArrayAdapter<String>(getActivity(), R.layout.spinner_item,
        new String[]
        {
                "[ 0 ] None","[ 1 ] DOOR LOCK impulse 1 sec" ,"[ 2 ] DOOR UNLOCK impulse 1 sec" ,"[ 3 ] DOOR LOCK impulse 1 sec (once when arming)" ,"[ 4 ] DOOR UNLOCK impulse 1 sec (once when disarming)" ,"[ 5 ] DOOR LOCK impulse 1 sec twice" ,"[ 6 ] DOOR UNLOCK impulse 1 sec twice" ,"[ 7 ] DOOR LOCK impulse 4 sec" ,"[ 8 ] DOOR UNLOCK impulse 4 sec" ,"[ 9 ] DOOR LOCK impulse 30 sec (comfort)" ,"[ 10 ] 2-step unlock impulse 1 sec" ,"[ 11 ] 2-step unlock impulse 1 sec twice" ,"[ 12 ] 2-step unlock impulse 4 sec" ,"[ 13 ] Trunk release - impulse 1 sec" ,"[ 14 ] ----" ,"[ 15 ] ----" ,"[ 16 ] ----" ,"[ 17 ] Parking light button" ,"[ 18 ] Parking lightflash" ,"[ 19 ] Impulse control of light signals" ,"[ 20 ] Engine stop delay 2 sec then output 3 sec (bypass door)" ,"[ 21 ] Engine stop delay 1 sec then output 1 sec (bypass door)" ,"[ 22 ] Hood Lock (R3)" ,"[ 23 ] Engine Killer mode" ,"[ 24 ] IGN" ,"[ 25 ] IGN*(Output is OFF when cranking)" ,"[ 26 ] ACC" ,"[ 27 ] ACC*(2sec before IGN Output and keep when cranking)" ,"[ 28 ] START" ,"[ 29 ] Pedal brake pressing during remote engine starting" ,"[ 30 ] ----" ,"[ 31 ] Start Killer mode" ,"[ 32 ] ----" ,"[ 33 ] ----" ,"[ 34 ] After Arm 20 sec" ,"[ 35 ] After Disrm 20 sec"
        }));

        final String[] event = new String[]{"[ 0 ] No Event" ,"[ 1 ] ---" ,"[ 2 ] Armed" ,"[ 3 ] Disarmed" ,"[ 4 ] Armed or Disarmed" ,"[ 5 ] Disarmed or IGN OFF" ,"[ 6 ] IGN ON" ,"[ 7 ] IGN OFF" ,"[ 8 ] Door LOCK" ,"[ 9 ] Door UNLOCK" ,"[ 10 ] Alarm" ,"[ 11 ] Hand Brake UP(ON)" ,"[ 12 ] Hand Break Down(OFF)" ,"[ 13 ] Engine Is Starts To Run" ,"[ 14 ] Successful Start" ,"[ 15 ] Not Successful Start (4T)" ,"[ 16 ] Any Start by Alarm" ,"[ 17 ] Any shut down by Alarm" ,"[ 18 ] Any Engine Shut Down" ,"[ 19 ] Pulse to Push Start (START)" ,"[ 20 ] Pulse to Push Start (STOP)" ,"[ 21 ] Transfer From Remote Engine Start to KEY" ,"[ 22 ] Event 1 input ON" ,"[ 23 ] Event 1 input OFF" ,"[ 24 ] Slave_TAG_Search" ,"[ 25 ] Trunk release" ,"[ 26 ] 2-Step Unlock" ,"[ 27 ] Start Killer ON" ,"[ 28 ] Start Killer OFF" ,"[ 29 ] Engine Killer ON" ,"[ 30 ] Engine Killer OFF" ,"[ 31 ] Trunk Open" ,"[ 32 ] Trunk Close" ,"[ 33 ] Remote Control (2L+1)" ,"[ 34 ] Remote Control (3L+1)" ,"[ 35 ] Remote Control (4L+1)" ,"[ 36 ] Remote Control (2L+3)" ,"[ 37 ] Remote Control (3L+2)" ,"[ 38 ] Remote Control (4L+2)" ,"[ 39 ] Event 2 input ON" ,"[ 40 ] Event 2 nput OFF"};
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

        editT1.setText(String.valueOf(mModifiedData[CH_ITEM_T1+1]*256 + mModifiedData[CH_ITEM_T1]));
        editT2.setText(String.valueOf(mModifiedData[CH_ITEM_T2+1]*256 + mModifiedData[CH_ITEM_T2]));
        editT3.setText(String.valueOf(mModifiedData[CH_ITEM_T3+1]*256 + mModifiedData[CH_ITEM_T3]));
        editT4.setText(String.valueOf(mModifiedData[CH_ITEM_T4+1]*256 + mModifiedData[CH_ITEM_T4]));

        spinnerEventOn.setSelection(mModifiedData[CH_ITEM_EVENT_ON]);

        spinnerEventOff.setSelection(mModifiedData[CH_ITEM_EVENT_OFF]);

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
                return new String[]{COND_NO_CONDITION,COND_ENGINE_START_IN_TT_MODE,COND_ENGINE_REMOTE_START};
            }
            case 6:
            case 7:
            case 8:
            case 9:
            case 14:
            case 15:
            case 16:
            case 17:
            case 19:
            case 20:
            {
                return new String[]{COND_NO_CONDITION,COND_ARMED,COND_DISARMED};
            }

            case 11:
            {
                return new String[]{COND_NO_CONDITION,COND_DISARMED,COND_IGN_ON,COND_IGN_OFF,COND_ENGINE_START_BY_FACTORY};
            }
            case 12:
            {
                return new String[]{COND_NO_CONDITION,COND_DISARMED,COND_IGN_ON,COND_IGN_OFF,COND_ENGINE_START_BY_FACTORY,COND_ENGINE_START_IN_TT_MODE,COND_ENGINE_REMOTE_START};// 0,2,3,4,5,6,7
            }
            case 18:
            {
                return new String[]{COND_NO_CONDITION,COND_ARMED,COND_DISARMED,COND_ENGINE_START_BY_FACTORY,COND_ENGINE_START_IN_TT_MODE,COND_ENGINE_REMOTE_START};       // 0,1,2,5,6,7
            }
            case 22:
            case 23:
            case 39:
            case 40:
            {
                return new String[]{COND_NO_CONDITION,COND_ARMED,COND_DISARMED,COND_IGN_ON,COND_IGN_OFF,COND_ENGINE_START_BY_FACTORY,COND_ENGINE_START_IN_TT_MODE,COND_ENGINE_REMOTE_START};// 0,1,2,3,4,5,6,7
            }
        }

        return new String[]{COND_NO_CONDITION};
    }

    private int getConditionIndex(final String condition)
    {
        if(condition.equals(COND_ARMED))
        {
            return 1;
        }
        else if(condition.equals(COND_DISARMED))
        {
            return 2;
        }
        else if(condition.equals(COND_IGN_ON))
        {
            return 3;
        }
        else if(condition.equals(COND_IGN_OFF))
        {
            return 4;
        }
        else if(condition.equals(COND_ENGINE_START_BY_FACTORY))
        {
            return 5;
        }
        else if(condition.equals(COND_ENGINE_START_IN_TT_MODE))
        {
            return 6;
        }
        else if(condition.equals(COND_ENGINE_REMOTE_START))
        {
            return 7;
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

    private byte setBit(final byte src,final int bit,final int value)
    {
        return (byte)((src & ~(0x1 << bit)) | (value << bit));
    }
}

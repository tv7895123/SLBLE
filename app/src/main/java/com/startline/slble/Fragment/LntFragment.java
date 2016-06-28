package com.startline.slble.Fragment;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.startline.slble.Interface.OnProgramDataChangedListener;
import com.startline.slble.R;

import static com.startline.slble.Interface.ProgramTool.*;


public class LntFragment extends BaseFragment
{
    private Context context = null;
    private EditText editPinCode;
    private Spinner spinnerTilt,spinnerShockHeavy,spinnerShockLight,spinnerSiren;
    private AdapterView.OnItemSelectedListener onSpinnerItemClick = new AdapterView.OnItemSelectedListener()
    {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
        {
            if(view == null)return;
            switch (parent.getId())
            {
                case R.id.spinner_tilt:
                {
                    mModifiedData[LNT_ITEM_TILT_SENSOR] = (byte)position;
                }
                break;
                case R.id.spinner_heavy_shock:
                {
                    mModifiedData[LNT_ITEM_SHOCK_SENSOR] = (byte)((mModifiedData[LNT_ITEM_SHOCK_SENSOR] & 0x0F) | (position<<4));
                }
                break;
                case R.id.spinner_light_shock:
                {
                    mModifiedData[LNT_ITEM_SHOCK_SENSOR] = (byte)((mModifiedData[LNT_ITEM_SHOCK_SENSOR] & 0xF0) | position);

                    int heavyPosition = (mModifiedData[LNT_ITEM_SHOCK_SENSOR] & 0xF0) >>4;
                    // Level OFF, no limit
                    if(position == 0)
                    {
                        spinnerShockHeavy.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, generateStringArray(0,14,1)));
                    }
                    else
                    {
                        // Limit heavy level
                        spinnerShockHeavy.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, generateStringArray(0,position,1)));

                        // If heavy level greater than light
                        if(((mModifiedData[LNT_ITEM_SHOCK_SENSOR] & 0xF0) >>4) > position)
                        {
                            mModifiedData[LNT_ITEM_SHOCK_SENSOR] = (byte)((mModifiedData[LNT_ITEM_SHOCK_SENSOR] & 0x0F) | (position<<4));
                            heavyPosition = position;
                        }
                        else
                        {
                            heavyPosition = (mModifiedData[LNT_ITEM_SHOCK_SENSOR] & 0xF0) >> 4;
                        }
                    }

                    spinnerShockHeavy.setSelection(heavyPosition);
                }
                break;
                case R.id.spinner_siren:
                {
                    mModifiedData[LNT_ITEM_SIREN] = (byte)position;
                }
                break;
                default:
                    return;
            }

            notifyDataChanged();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent)
        {

        }
    };

    public static LntFragment newInstance(final int index,String title, int indicatorColor, int dividerColor)
    {
        return newInstance(index,title,indicatorColor,dividerColor,0,null);
    }

    public static LntFragment newInstance(final int index,String title, int indicatorColor, int dividerColor, final OnProgramDataChangedListener onProgramDataChangedListener)
    {
        return newInstance(index,title,indicatorColor,dividerColor,0,onProgramDataChangedListener);
    }

    public static LntFragment newInstance(final int index,String title, int indicatorColor, int dividerColor, int iconResId)
    {
        return newInstance(index,title,indicatorColor,dividerColor,iconResId,null);
    }

    public static LntFragment newInstance(final int index,String title, int indicatorColor, int dividerColor, int iconResId, final OnProgramDataChangedListener onProgramDataChangedListener)
    {
        LntFragment f = new LntFragment();
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
            mRootView = inflater.inflate(R.layout.fragment_lnt, container, false);
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
        editPinCode = (EditText)mRootView.findViewById(R.id.edit_pin_code);
        editPinCode.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {

            }

            @Override
            public void afterTextChanged(Editable s)
            {
                if(s.length() == 4)
                {
                    final int digit1 = (int)editPinCode.getText().charAt(0)-(int)'0';
                    final int digit2 = (int)editPinCode.getText().charAt(1)-(int)'0';
                    final int digit3 = (int)editPinCode.getText().charAt(2)-(int)'0';
                    final int digit4 = (int)editPinCode.getText().charAt(3)-(int)'0';

                    mModifiedData[LNT_ITEM_PIN_CODE_1] = (byte)digit1;
                    mModifiedData[LNT_ITEM_PIN_CODE_2] = (byte)digit2;
                    mModifiedData[LNT_ITEM_PIN_CODE_3] = (byte)digit3;
                    mModifiedData[LNT_ITEM_PIN_CODE_4] = (byte)digit4;

                    notifyDataChanged();
                    //Toast.makeText(getActivity(),String.valueOf(digit1) + digit2 + digit3 + digit4,Toast.LENGTH_SHORT).show();
                }
            }
        });

        editPinCode.setOnFocusChangeListener(new View.OnFocusChangeListener()
        {
            @Override
            public void onFocusChange(View v, boolean hasFocus)
            {
                if(hasFocus)
                {
                    final String text = editPinCode.getText().toString();
                    if(text.length() < 4)
                    {
                        //Toast.makeText(getActivity(),"Length must be 4",Toast.LENGTH_SHORT).show();
                        //editPinCode.requestFocus();
                        for(int i=text.length();i < 5;i++)
                        {
                            editPinCode.append("0");
                        }
                    }
                }
            }
        });

        spinnerTilt = (Spinner)mRootView.findViewById(R.id.spinner_tilt);
        spinnerShockHeavy = (Spinner)mRootView.findViewById(R.id.spinner_heavy_shock);
        spinnerShockLight = (Spinner)mRootView.findViewById(R.id.spinner_light_shock);
        spinnerSiren = (Spinner)mRootView.findViewById(R.id.spinner_siren);

        spinnerTilt.setOnItemSelectedListener(onSpinnerItemClick);
        spinnerShockHeavy.setOnItemSelectedListener(onSpinnerItemClick);
        spinnerShockLight.setOnItemSelectedListener(onSpinnerItemClick);
        spinnerSiren.setOnItemSelectedListener(onSpinnerItemClick);

        spinnerTilt.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, generateStringArray(0,14,1)));
        spinnerShockHeavy.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, generateStringArray(0,14,1)));
        spinnerShockLight.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, generateStringArray(0,14,1)));
        spinnerSiren.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, generateStringArray(0,8,1)));
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

        if(mModifiedData[3] == -1 || mModifiedData[4] == -1 || mModifiedData[5] == -1 || mModifiedData[6] == -1)
            editPinCode.setText("");
        else
            editPinCode.setText(String.valueOf(mModifiedData[3]) + String.valueOf(mModifiedData[4]) + String.valueOf(mModifiedData[5]) + String.valueOf(mModifiedData[6]));

        spinnerTilt.setSelection(mModifiedData[11]);

        spinnerShockHeavy.setSelection((mModifiedData[12] >> 4) & 0x0F);
        spinnerShockLight.setSelection(mModifiedData[12] & 0x0F);

        spinnerSiren.setSelection(mModifiedData[13]);
    }
}

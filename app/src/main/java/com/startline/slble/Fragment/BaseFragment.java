package com.startline.slble.Fragment;


import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RadioGroup;
import com.startline.slble.Adapter.ProgramTableListAdapter;
import com.startline.slble.Interface.OnProgramDataChangedListener;
import com.startline.slble.PureClass.ProgramItem;
import com.startline.slble.R;
import com.startline.slble.Util.DialogUtil;
import com.startline.slble.Util.LogUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static com.startline.slble.Adapter.ProgramTableListAdapter.TYPE_RADIO;

/**
 * Created by terry on 2016/6/8.
 */
public class BaseFragment extends Fragment
{
    public static String TAG = "BaseFragment";

    private String title = "";
    private int indicatorColor = Color.BLUE;
    private int dividerColor = Color.GRAY;
    private int iconResId = -1;



    private OnProgramDataChangedListener onProgramDataChangedListener;

    public String getTitle()
    {
        return title;
    }
    public void setTitle(String title)
    {
        this.title = title;
    }
    public int getIndicatorColor()
    {
        return indicatorColor;
    }
    public void setIndicatorColor(int indicatorColor)
    {
        this.indicatorColor = indicatorColor;
    }
    public int getDividerColor()
    {
        return dividerColor;
    }
    public void setDividerColor(int dividerColor)
    {
        this.dividerColor = dividerColor;
    }
    public void setOnProgramDataChangedListener(OnProgramDataChangedListener onProgramDataChangedListener)
    {
        this.onProgramDataChangedListener = onProgramDataChangedListener;
    }

    public OnProgramDataChangedListener getOnProgramDataChangedListener()
    {
        return onProgramDataChangedListener;
    }

    //
    public int getIconResId()
    {
        return iconResId;
    }
    public void setIconResId(int iconResId)
    {
        this.iconResId = iconResId;
    }

    protected int[] mTitleArray = null;
    protected byte[] mInitData = null;
    protected byte[] mModifiedData = null;
    protected int selectPosition = -1;

    protected List<Boolean> mExpandList = null;
    protected List<Integer> mTypeList = null;
    protected List<Map<String, Object>> mDataList = null;
    protected ProgramTableListAdapter programTableListAdapter = null;
    protected RadioGroup.OnCheckedChangeListener onCheckedChangeListener = new RadioGroup.OnCheckedChangeListener()
    {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId)
        {
            handleCheckChange(group,checkedId);
        }
    };

    protected ListView listView;

    public void setInitData(final byte[] data)
    {
        mInitData = data;
        mModifiedData = copyByteArray(mInitData);
    }

    protected void customPickDialog(final Context context, final String title, final String[] items, final int defaultIndex)
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
    protected void handleAction(final String title,final int position)
    {
        try
        {
            int index;
            index = getIndexByTitle(mTitleArray,title);
            if(index != -1)
            {
                mModifiedData[index] = (byte)position;
            }
        }
        catch (Exception e)
        {
            LogUtil.e(TAG, e.toString(), Thread.currentThread().getStackTrace());
        }
    }

    protected void handleOnItemClick(AdapterView<?> parent, View view, int position, long id)
    {
//        String title;
//        String[] itemArray;
//
//        final int index = position;
//        title = getDisplayString(mTitleArray[index]);
//        itemArray = getItemArray(index);
//
//        customPickDialog(getActivity(),title,itemArray,mModifiedData[index]);
        final boolean expand = mExpandList.get(position);
        mExpandList.set(position,!expand);

        refresh();
    }

    protected void handleCheckChange(final RadioGroup radioGroup, final int checkedId)
    {
        final int item = checkedId/100 - 1;
        final int newValue = checkedId % 100;

        if(mModifiedData[item] != newValue)
        {
            mModifiedData[item] = (byte)newValue;
            updateListAdapter(true);

            if(onProgramDataChangedListener != null)
            {
                onProgramDataChangedListener.onProgramDataChanged(mModifiedData);
            }
        }


        LogUtil.d(TAG,"ID:"+checkedId,Thread.currentThread().getStackTrace());
        //Toast.makeText(getActivity(),"ID:"+checkedId,Toast.LENGTH_SHORT).show();
    }

    protected void updateListAdapter(final boolean dataEnabled)
    {
        if(programTableListAdapter == null)
        {
            programTableListAdapter = new ProgramTableListAdapter(getActivity(),getDataList(),true,onCheckedChangeListener,null);
            programTableListAdapter.setTypeList(mTypeList);
            listView.setAdapter(programTableListAdapter);
        }
        else
        {
            programTableListAdapter.setDataEnabled(dataEnabled);
            programTableListAdapter.setDataList(getDataList());
            programTableListAdapter.notifyDataSetChanged();
        }
    }

    protected void initTypeList()
    {
        mTypeList = new ArrayList<Integer>();

        // AF List data type
        for(int i=0;i<mTitleArray.length;i++)
        {
            mTypeList.add(TYPE_RADIO);
        }
    }


    public void refresh()
    {
        updateListAdapter(true);
    }

    protected String[] getItemArray(final int itemIndex)
    {
        return null;
    }

    protected void initExpandList()
    {
        mExpandList = new ArrayList<Boolean>();

        // AF List data type
        for(int i=0;i<mTitleArray.length;i++)
        {
            mExpandList.add(false);
        }
    }

    protected ProgramItem initProgramItem(int[] itemDefine)
    {
        final ProgramItem programItem = new ProgramItem();
        programItem.offset = itemDefine[0];
        programItem.length = itemDefine[1];

        return programItem;
    }

    protected List<Map<String, Object>> getDataList()
    {
        return null;
    }

    protected byte[] getProgramItemValue(final byte[] dataArray,final int startIndex,final ProgramItem programItem)
    {
        final byte[] data = subByteArray(dataArray,startIndex+programItem.offset,programItem.length);
        return data;
    }

    private int getIndexByTitle(final int[] titleArray,final String title)
    {
        int index=-1;
        for(int i=0;i<titleArray.length;i++)
        {
            if(getDisplayString(titleArray[i]).equals(title))
            {
                index = i;
                break;
            }
        }

        return index;
    }

    protected byte[] subByteArray(final byte[] byteArray,final int offset,final int length)
    {
        if(length <= 0)
            return null;

        if(length >= byteArray.length)
            return byteArray;

        final byte[] subArray = new byte[length];
        for(int i= 0;i<length;i++)
        {
            subArray[i] = byteArray[i+offset];
        }

        return subArray;
    }

    protected byte[] generateRandomArray(final int length,final int mode)
    {
        if(length <= 0 ) return null;

        final byte[] randomArray = new byte[length];
        for(int i=0;i<length;i++)
        {
            randomArray[i] = getRandom(mode);
        }

        return randomArray;
    }

    protected byte getRandom(final int modeNum)
    {
        final Random r = new Random();
        return (byte)r.nextInt(modeNum);
    }

    protected String getDisplayString(final int stringId)
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

    protected byte[] copyByteArray(final byte[] data)
    {
        if(data == null)
            return null;
        final byte[] copied = new byte[data.length];
        for(int i=0;i<data.length;i++)
        {
            copied[i] = data[i];
        }

        return copied;
    }
}

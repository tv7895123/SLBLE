package com.startline.slble.Fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import com.startline.slble.Interface.OnProgramDataChangedListener;
import com.startline.slble.PureClass.ProgramItem;
import com.startline.slble.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.startline.slble.Adapter.ProgramTableListAdapter.TYPE_RADIO;
import static com.startline.slble.Adapter.ProgramTableListAdapter.TYPE_TEXT;
import static com.startline.slble.Interface.ProgramTool.*;


public class SfFragment extends BaseFragment
{
    public static SfFragment newInstance(final int index,String title, int indicatorColor, int dividerColor)
    {
        return newInstance(index,title,indicatorColor,dividerColor,0,null);
    }

    public static SfFragment newInstance(final int index,String title, int indicatorColor, int dividerColor,final OnProgramDataChangedListener onProgramDataChangedListener)
    {
        return newInstance(index,title,indicatorColor,dividerColor,0,onProgramDataChangedListener);
    }

    public static SfFragment newInstance(final int index,String title, int indicatorColor, int dividerColor, int iconResId)
    {
        return newInstance(index,title,indicatorColor,dividerColor,iconResId,null);
    }

    public static SfFragment newInstance(final int index,String title, int indicatorColor, int dividerColor, int iconResId, final OnProgramDataChangedListener onProgramDataChangedListener)
    {
        SfFragment f = new SfFragment();
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

        //get data
        //title = getArguments().getString(DATA_NAME);

        mTitleArray = new int[]
        {
            R.string.title_item_engine_start
            ,R.string.title_item_running_time
            ,R.string.title_item_sensor_running
            ,R.string.title_item_auto_shut_engine
            ,R.string.title_item_start_with_lock
            ,R.string.title_item_start_with_parking_light
            ,R.string.title_item_locking_management
            ,R.string.title_item_auto_disarm
            ,R.string.title_item_cranking_time
            ,R.string.title_item_fuel_type
            ,R.string.title_item_engine_running_detect
            ,R.string.title_item_turbo_time_active
            ,R.string.title_item_ign3_bypass
            ,R.string.title_item_engine_start_pts
            ,R.string.title_item_auto_gear
            ,R.string.title_item_engine_start_webasto
            ,R.string.title_item_webasto_time
            ,R.string.title_item_engine_start_algroithm
        };

        // Generate simulated data
        setInitData(generateRandomArray(32,2));

        // Fill data for listView
        initTypeList();

        initExpandList();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        if(mRootView == null)
        {
            mRootView = inflater.inflate(R.layout.fragment_program_table_af_sf, container, false);
            listView = (ListView)mRootView.findViewById(R.id.list_view);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
            {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id)
                {
                    handleOnItemClick(parent,view,position,id);
                }
            });
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

        updateListAdapter(true);
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
    protected String[] getItemArray(final int itemIndex)
    {
        final String[] OFF_ON = new String[]{"Off","On"};

        String[] itemArray = OFF_ON;

        switch(itemIndex)
        {
            case SF_ITEM_ENGINE_START :
            {
                itemArray = new String[]{"Off","On Normal car with key", "On PTS mode"};
            }
            break;
            case SF_ITEM_RUNNING_TIME :
            {
                itemArray = new String[]{"10 min", "20 min", "30 min", "without  time limitation/30 min"};
            }
            break;
            case SF_ITEM_SENSOR_RUNNING :
            {
                itemArray = new String[]{"Off", "Smart bypass for Dome light", "Pin switch delay 30 sec", "Shock On, Tilt On/ Add on"};
            }
            break;
            case SF_ITEM_AUTO_SHUT_ENGINE :
            {
                itemArray = OFF_ON;
            }
            break;
            case SF_ITEM_START_WITH_LOCK :
            {
                itemArray = new String[]{"On", "Off"};
            }
            break;
            case SF_ITEM_START_WITH_PARKING_LIGHT :
            {
                itemArray = new String[]{"Off", "Flash"};
            }
            break;
            case SF_ITEM_LOCKING_MANAGEMENT :
            {
                itemArray = new String[]{"Off", "Start successful", "Locking impulse after attempt to start (2 sec later IGN OFF)", "Start successful & Locking impulse after attempt to start (2 sec later IGN OFF)"};
            }
            break;
            case SF_ITEM_AUTO_DISARM :
            {
                itemArray = new String[]{"Continue", "Shutdown engine"};
            }
            break;
            case SF_ITEM_CRANKING_TIME :
            {
                itemArray = new String[]{"0.8 sec", "1.2 sec", "2.0 sec", "6.0 sec"};
            }
            break;
            case SF_ITEM_FUEL_TYPE :
            {
                itemArray = new String[]{"Gasoline (2s)", "Diesel  5s", "Diesel  10s", "Diesel  20s / Event input(60S)"};
            }
            break;
            case SF_ITEM_ENGINE_RUNNING_DETECT :
            {
                itemArray = new String[]{"Generator(+)", "Voltage", "Generator(-)", "RPM"};
            }
            break;
            case SF_ITEM_TURBO_TIME_ACTIVE :
            {
                itemArray = new String[]{"Brake mode", "Auto mode", "Safe mode", "Off"};
            }
            break;
            case SF_ITEM_IGN3_BYPASS :
            {
                itemArray = new String[]{"Full time (Without turbo time)", "30'S (Without turbo time)", "Full time with turbo time", "30'S with turbo time"};
            }
            break;
            case SF_ITEM_ENGINE_START_PTS :
            {
                itemArray = new String[]{"PTS mode 1 (SF 1=3)", "PTS mode 3 (SF 1=3)", "PTS mode 4 (SF 1=3)", "Push start (SF 1=3)(pulse 6 sec until start)"};
            }
            break;
            case SF_ITEM_AUTO_GEAR :
            {
                itemArray = new String[]{"Manual/after Arm", "Manual/Door Close", "Manual/Door Colse 20s", "Auto"};
            }
            break;
            case SF_ITEM_ENGINE_START_WEBASTO :
            {
                itemArray = new String[]{"Off", "-15 ℃", "-30 ℃", "On"};
            }
            break;
            case SF_ITEM_WEBASTO_TIME :
            {
                itemArray = new String[]{"20 min", "30 min", "40 min", "50 min"};
            }
            break;
            case SF_ITEM_ENGINE_START_ALGROITHM :
            {
                itemArray = new String[]{"Only engine starting", "Only Webasto starting", "First - Webasto heating,then engine starting", "First - Webasto heating,then engine starting"};
            }
            break;
            default:
            {
                itemArray = null;
            }
            break;
        }
        return itemArray;
    }


    @Override
    protected List<Map<String, Object>> getDataList()
    {
        mDataList = new ArrayList<Map<String, Object>>();

        Map<String, Object> map;
        ProgramItem programItem;
        String[] itemArray = null;

        // Build AF data list
        for (int i = 0; i < mTypeList.size(); i++)
        {
            itemArray = getItemArray(i);
            map = new HashMap<String, Object>();
            map.put("index",i+1);
            map.put("title", getDisplayString(mTitleArray[i]));


            int value = (int)mModifiedData[i];
            if(value < 0)
            {
                value = 0;
            }
            else if(value >= itemArray.length)
            {
                value = itemArray.length-1;
            }
            map.put("modified_value",itemArray.length==0? 0:value);
            map.put("init_value",(int)mInitData[i]);
            map.put("items",itemArray);
            map.put("expand",mExpandList.get(i));
            mDataList.add(map);
        }
        return mDataList;
    }

    @Override
    public void refresh()
    {
        updateListAdapter(true);
    }

}

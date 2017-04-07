package com.startline.slble.Fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.startline.slble.Interface.OnProgramDataChangedListener;
import com.startline.slble.PureClass.ProgramItem;
import com.startline.slble.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.startline.slble.Interface.ProgramTool.*;


public class AfFragment extends BaseFragment
{
    public static AfFragment newInstance(final int index,String title, int indicatorColor, int dividerColor)
    {
        return newInstance(index,title,indicatorColor,dividerColor,0,null);
    }

    public static AfFragment newInstance(final int index,String title, int indicatorColor, int dividerColor,final OnProgramDataChangedListener onProgramDataChangedListener)
    {
        return newInstance(index,title,indicatorColor,dividerColor,0,onProgramDataChangedListener);
    }

    public static AfFragment newInstance(final int index,String title, int indicatorColor, int dividerColor, int iconResId)
    {
        return newInstance(index,title,indicatorColor,dividerColor,iconResId,null);
    }

    public static AfFragment newInstance(final int index,String title, int indicatorColor, int dividerColor, int iconResId, final OnProgramDataChangedListener onProgramDataChangedListener)
    {
        AfFragment f = new AfFragment();
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
                R.string.title_item_auto_lock_trunk
                ,R.string.title_item_door_follow_ign
                ,R.string.title_item_detect_dome_pin_switch
                ,R.string.title_item_auto_lock_last_door
                ,R.string.title_item_auto_rearm
                ,R.string.title_item_siren_volume
                ,R.string.title_item_slave_tag_condition
                ,R.string.title_item_anti_car_jack
                ,R.string.title_item_engine_kill_output
                ,R.string.title_item_2_step_disarm
                //,R.string.title_item_2_step_disarm_slave
                ,R.string.title_item_range_check_timer
                ,R.string.title_item_door_input
                ,R.string.title_item_gsm_protocol
                ,R.string.title_item_gsm_out_command
                ,R.string.title_item_event_1_input
                //,R.string.title_item_event_1_input_slave
                ,R.string.title_item_car_alarm_mode
                ,R.string.title_item_slave_mode_event
                ,R.string.title_item_slave_al_au_input
                ,R.string.title_item_event_2_input
                ,R.string.title_item_slave_search_time
                ,R.string.title_item_trunk_detection
                ,R.string.title_item_lock_door_in_slave_mode
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
            case AF_ITEM_AUTO_LOCK_TRUNK:
            {
                itemArray = OFF_ON;
            }
            break;
            case AF_ITEM_DOOR_FOLLOW_IGN:
            {
                itemArray = new String[]{"Off","Ignition ON (10'S)","Ignition ON (10'S)/ unlock OFF","Brake pedal"};
            }
            break;
            case AF_ITEM_DETECT_DOME_PIN_SWITCH:
            {
                itemArray = new String[]{"Pin Switch","Smart bypass for Dome light","Pin switch delay 30 sec","Pin switch delay 5 sec"};
            }
            break;
            case AF_ITEM_AUTO_ARM_LOCK:
            {
                itemArray = new String[]{"Off", "On with Lock", "On w/o lock"};
            }
            break;
            case AF_ITEM_AUTO_REARM:
            {
                itemArray = new String[]{"On with Lock", "On w/o lock", "Off"};
            }
            break;
            case AF_ITEM_SIREN_VOLUME:
            {
                itemArray = new String[]{"Siren", "Horn"};
            }
            break;
            case AF_ITEM_SLAVE_TAG_CONDITION:
            {
                itemArray = new String[]{"Detection after disarm", "Detection after disarm but without unlock", "Detection after running"};
            }
            break;
            case AF_ITEM_ANTI_CAR_JACK:
            {
                itemArray = new String[]{"Off", "Auto mode", "Off", "Safe mode"};
            }
            break;
            case AF_ITEM_ENGINE_KILL_OUTPUT:
            {
                itemArray = new String[]{"N/C", "N/O", "Wireless relay with N/C", "Wireless relay with N/O"};
            }
            break;
            case AF_ITEM_2_STEP_DISARM:
            {
                if(mModifiedData[AF_ITEM_CAR_ALARM_MODE] == 0)
                {
                    itemArray = new String[]{"Off", "On(Valet button)"};
                }
                else
                {
                    itemArray = new String[]{"Off", "On(Remote)", "On(SEvent)", "On(CAN PIN CODE)"};
                }
            }
            break;
            case AF_ITEM_RANGE_CHECK_TIMER:
            {
                itemArray = new String[]{"Off", "3 min", "5 min", "7 min"};
            }
            break;
            case AF_ITEM_DOOR_INPUT:
            {
                itemArray = new String[]{"Door(-)", "Door(+)"};
            }
            break;
            case AF_ITEM_GSM_PROTOCOL:
            {
                itemArray = new String[]{"V2.0", "V1.0"};
            }
            break;
            case AF_ITEM_GSM_OUT_COMMAND:
            {
                itemArray = new String[]{"Flex CH Event 34 (3L+1)", "Flex CH Event 33 (2L+1)", "Flex CH Event 36 (2L+3)", "Webasto ON/OFF via UART"};
            }
            break;
            case AF_ITEM_EVENT_1_INPUT:
            {
                if(mModifiedData[AF_ITEM_CAR_ALARM_MODE] == 0)
                {
                    itemArray = new String[]{"Diesel grow plug input", "Start/Stop engine(pulse)", "Flexy CH event1","Stop engine(pulse)"};
                }
                else
                {
                    itemArray = new String[]{"Diesel grow plug input", "Start/Stop engine(pulse)", "Door Key Detect (-)","Stop engine(pulse)"};
                }
            }
            break;
            case AF_ITEM_CAR_ALARM_MODE:
            {
                itemArray = new String[]{"Normal", "Slave(CAN)", "Slave(Analog T=1 sec)", "Slave(Analog T=3 sec"};
            }
            break;
            case AF_ITEM_SLAVE_MODE_EVENT:
            {
                itemArray = new String[]{"Lockout pin", "Door trigger", "IGN trigger OR transfer to KEY", "Event in"};
            }
            break;
            case AF_ITEM_SLAVE_AL_AU_INPUT:
            {
                itemArray = new String[]{"AL(-) AU(-)", "AL(+) AU(+)", "AL(-) AU(+)", "AL(+) AU(-)"};
            }
            break;
            case AF_ITEM_EVENT_2_INPUT:
            {
                itemArray = new String[]{"Flexy CH event2", "Flexy CH event2", "Light Flash Detect (-)(Slave Analog)", "Light Flash Detect (+)(Slave Analog)"};
            }
            break;
            case AF_ITEM_SLAVE_SEARCH_TIME:
            {
                itemArray = new String[]{"Slave(20's)", "Slave(10's)", "Slave(30's)", "Slave(60's)"};
            }
            break;
            case AF_ITEM_TRUNK_DETECTION:
            {
                itemArray = OFF_ON;
            }
            break;
            case AF_ITEM_LOCK_DOOR_IN_SLAVE_MODE:
            {
                itemArray = OFF_ON;
            }
            break;
            default:
            {
                //itemArray = null;
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

            if(i==9)
            {
                if(mModifiedData[AF_ITEM_CAR_ALARM_MODE] == 0)
                {
                    map.put("title", getDisplayString(mTitleArray[i]));
                }
                else
                {
                    map.put("title", getDisplayString(R.string.title_item_2_step_disarm_slave));
                }
            }
            else if(i == 14)
            {
                if(mModifiedData[AF_ITEM_CAR_ALARM_MODE] == 0)
                {
                    map.put("title", getDisplayString(mTitleArray[i]));
                }
                else
                {
                    map.put("title", getDisplayString(R.string.title_item_event_1_input_slave));
                }
            }
            else
            {
                map.put("title", getDisplayString(mTitleArray[i]));
            }



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

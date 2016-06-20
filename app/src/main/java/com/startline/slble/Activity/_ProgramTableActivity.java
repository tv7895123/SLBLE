package com.startline.slble.Activity;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import com.startline.slble.Adapter.ProgramTableListAdapter;
import com.startline.slble.R;
import com.startline.slble.Util.DialogUtil;
import com.startline.slble.Util.LogUtil;

import java.util.*;

import static com.startline.slble.Adapter.SettingListAdapter.TYPE_GROUP;
import static com.startline.slble.Adapter.SettingListAdapter.TYPE_TEXT;

/**
 * Created by terry on 2016/6/3.
 */
public class _ProgramTableActivity extends Activity
{
    public static String TAG = "ProgramTable";
    public static final int AF_START = 0;
    public static final int SF_START = 32;
    public static final int SENSOR_START = 64;

    public static final int AF_ITEM_AUTO_LOCK_TRUNK = 0;
    public static final int AF_ITEM_DOOR_FOLLOW_IGN = 1;
    public static final int AF_ITEM_DETECT_DOME_PIN_SWITCH = 2;
    public static final int AF_ITEM_AUTO_ARM_LOCK = 3;
    public static final int AF_ITEM_AUTO_REARM = 4;
    public static final int AF_ITEM_SIREN_VOLUME = 5;
    public static final int AF_ITEM_SLAVE_TAG_CONDITION = 6;
    public static final int AF_ITEM_ANTI_CAR_JACK = 7;
    public static final int AF_ITEM_ENGINE_KILL_OUTPUT = 8;
    public static final int AF_ITEM_2_STEP_DISARM = 9;
    public static final int AF_ITEM_RANGE_CHECK_TIMER = 10;
    public static final int AF_ITEM_DOOR_INPUT = 11;
    public static final int AF_ITEM_GSM_PROTOCOL = 12;
    public static final int AF_ITEM_GSM_OUT_COMMAND = 13;
    public static final int AF_ITEM_EVENT_1_INPUT = 14;
    public static final int AF_ITEM_CAR_ALARM_MODE = 15;
    public static final int AF_ITEM_SLAVE_MODE_EVENT = 16;
    public static final int AF_ITEM_SLAVE_AL_AU_INPUT = 17;
    public static final int AF_ITEM_EVENT_2_INPUT = 18;
    public static final int AF_ITEM_SLAVE_SEARCH_TIME = 19;
    public static final int AF_ITEM_TRUNK_DETECTION = 20;

    public static final int[][] AF_TABLE = new int[][]
    {
        // ITEM                                       ,Length,     MinValue,   MaxValue
        {AF_ITEM_AUTO_LOCK_TRUNK                   ,1,0,1}
        ,{AF_ITEM_DOOR_FOLLOW_IGN                  ,1,0,3}
        ,{AF_ITEM_DETECT_DOME_PIN_SWITCH         ,1,0,3}
        ,{AF_ITEM_AUTO_ARM_LOCK                     ,1,0,2}
        ,{AF_ITEM_AUTO_REARM                         ,1,0,2}
        ,{AF_ITEM_SIREN_VOLUME                       ,1,0,1}
        ,{AF_ITEM_SLAVE_TAG_CONDITION               ,1,0,1}
        ,{AF_ITEM_ANTI_CAR_JACK                       ,1,0,3}
        ,{AF_ITEM_ENGINE_KILL_OUTPUT                 ,1,0,3}
        ,{AF_ITEM_2_STEP_DISARM                        ,1,0,3}
        ,{AF_ITEM_RANGE_CHECK_TIMER                 ,1,0,3}
        ,{AF_ITEM_DOOR_INPUT                            ,1,0,3}
        ,{AF_ITEM_GSM_PROTOCOL                         ,1,0,1}
        ,{AF_ITEM_GSM_OUT_COMMAND                   ,1,0,3}
        ,{AF_ITEM_EVENT_1_INPUT                        ,1,0,3}
        ,{AF_ITEM_CAR_ALARM_MODE                    ,1,0,3}
        ,{AF_ITEM_SLAVE_MODE_EVENT                    ,1,0,3}
        ,{AF_ITEM_SLAVE_AL_AU_INPUT                     ,1,0,3}
        ,{AF_ITEM_EVENT_2_INPUT                         ,1,0,3}
        ,{AF_ITEM_SLAVE_SEARCH_TIME                    ,1,0,3}
        ,{AF_ITEM_TRUNK_DETECTION                       ,1,0,1}
    };

    public static final int SF_ITEM_ENGINE_START = 0;
    public static final int SF_ITEM_RUNNING_TIME = 1;
    public static final int SF_ITEM_SENSOR_RUNNING = 2;
    public static final int SF_ITEM_AUTO_SHUT_ENGINE = 3;
    public static final int SF_ITEM_START_WITH_LOCK = 4;
    public static final int SF_ITEM_START_WITH_PARKING_LIGHT = 5;
    public static final int SF_ITEM_LOCKING_MANAGEMENT = 6;
    public static final int SF_ITEM_AUTO_DISARM = 7;
    public static final int SF_ITEM_CRANKING_TIME = 8;
    public static final int SF_ITEM_FUEL_TYPE = 9;
    public static final int SF_ITEM_ENGINE_RUNNING_DETECT = 10;
    public static final int SF_ITEM_TURBO_TIME_ACTIVE = 11;
    public static final int SF_ITEM_IGN3_BYPASS = 12;
    public static final int SF_ITEM_ENGINE_START_PTS = 13;
    public static final int SF_ITEM_AUTO_GEAR = 14;
    public static final int SF_ITEM_ENGINE_START_WEBASTO = 15;
    public static final int SF_ITEM_WEBASTO_TIME = 16;
    public static final int SF_ITEM_ENGINE_START_ALGROITHM = 17;

    public static final int[][] SF_TABLE = new int[][]
    {
        // ITEM                                       ,Length,     MinValue,   MaxValue
        {SF_ITEM_ENGINE_START                   ,1,0,2}
        ,{SF_ITEM_RUNNING_TIME                  ,1,0,3}
        ,{SF_ITEM_SENSOR_RUNNING               ,1,0,3}
        ,{SF_ITEM_AUTO_SHUT_ENGINE             ,1,0,1}
        ,{SF_ITEM_START_WITH_LOCK              ,1,0,1}
        ,{SF_ITEM_START_WITH_PARKING_LIGHT   ,1,0,1}
        ,{SF_ITEM_LOCKING_MANAGEMENT         ,1,0,3}
        ,{SF_ITEM_AUTO_DISARM                    ,1,0,1}
        ,{SF_ITEM_CRANKING_TIME                  ,1,0,3}
        ,{SF_ITEM_FUEL_TYPE                        ,1,0,3}
        ,{SF_ITEM_ENGINE_RUNNING_DETECT       ,1,0,3}
        ,{SF_ITEM_TURBO_TIME_ACTIVE             ,1,0,3}
        ,{SF_ITEM_IGN3_BYPASS                      ,1,0,3}
        ,{SF_ITEM_ENGINE_START_PTS               ,1,0,3}
        ,{SF_ITEM_AUTO_GEAR                        ,1,0,3}
        ,{SF_ITEM_ENGINE_START_WEBASTO         ,1,0,3}
        ,{SF_ITEM_WEBASTO_TIME                    ,1,0,3}
        ,{SF_ITEM_ENGINE_START_ALGROITHM      ,1,0,3}
    };

    public class ProgramItem
    {
        public int offset;
        public int length;
        public int minValue;
        public int maxValue;
        public byte[] value;
    }

    private int selectPosition = -1;
    private byte[] RANDOM_TABLE = null;
    private ArrayList<ProgramItem> mAfProgramItemList = null;
    private ArrayList<ProgramItem> mSfProgramItemList = null;
    private int[] mAfTitleArray = null;
    private int[] mSfTitleArray = null;

    private List<Integer> mTypeList = null;
    private List<Integer> mAfTypeList = null;
    private List<Integer> mSfTypeList = null;
    private List<Map<String, Object>> mDataList = null;
    private List<Map<String, Object>> mAfDataList = null;
    private List<Map<String, Object>> mSfDataList = null;


    private ListView listView = null;
    private ProgramTableListAdapter programTableListAdapter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_program_table_af_sf);

        mAfTitleArray = new int[]
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
        };

        mSfTitleArray = new int[]
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

        listView = (ListView)findViewById(R.id.list_view);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                String title;
                String[] itemArray;
                ProgramItem programItem;
                if(position< mAfTypeList.size())
                {
                    final int index = position-1;
                    title = getDisplayString(mAfTitleArray[index]);
                    itemArray = getAfItemArray(index);
                    programItem = mAfProgramItemList.get(index);
                }
                else
                {
                    final int index = position-mAfTypeList.size()-1;
                    title = getDisplayString(mSfTitleArray[index]);
                    itemArray = getSfItemArray(index);
                    programItem = mSfProgramItemList.get(index);
                }

                customPickDialog(title,itemArray,programItem.value[0]);
            }
        });


        // Generate simulated data
        RANDOM_TABLE = generateRandomArray(64,2);
        mAfProgramItemList = new ArrayList();
        mSfProgramItemList = new ArrayList();

        initAfProgramList();
        initSfProgramList();

        // Fill data for listView
        initTypeList();

        updateListAdapter(true);


//
//        Log.d(TAG,String.format("%s",formatByteArrayToLog(RANDOM_TABLE)));
//
//        for(int i=0;i<mAfProgramItemList.size();i++)
//        {
//            final ProgramItem programItem = mAfProgramItemList.get(i);
//            Log.d(TAG,String.format("[%2d] Offset=%d, Length=%d, Value=%s",i,programItem.offset,programItem.length,formatByteArrayToHexString(programItem.value)));
//        }
//
//        for(int i=0;i<mSfProgramItemList.size();i++)
//        {
//            final ProgramItem programItem = mSfProgramItemList.get(i);
//            Log.d(TAG,String.format("[%2d] Offset=%d, Length=%d, Value=%s",i,programItem.offset,programItem.length,formatByteArrayToHexString(programItem.value)));
//        }
    }

    private void initTypeList()
    {
        mTypeList = new ArrayList<Integer>();
        mAfTypeList = new ArrayList<Integer>();
        mSfTypeList = new ArrayList<Integer>();

        // AF List data type
        for(int i=0;i<mAfTitleArray.length;i++)
        {
            mAfTypeList.add(TYPE_TEXT);
        }

        // SF List data type
        for(int i=0;i<mSfTitleArray.length;i++)
        {
            mSfTypeList.add(TYPE_TEXT);
        }

        mAfTypeList.add(0,TYPE_GROUP);
        mSfTypeList.add(0,TYPE_GROUP);
        mTypeList.addAll(mAfTypeList);
        mTypeList.addAll(mSfTypeList);
    }


    private List<Map<String, Object>> getDataList()
    {
        mDataList = new ArrayList<Map<String, Object>>();
        mAfDataList = new ArrayList<Map<String, Object>>();
        mSfDataList = new ArrayList<Map<String, Object>>();
        Map<String, Object> map;
        ProgramItem programItem;
        String[] itemArray = null;

        // Build AF data list
        for (int i = 0; i < mAfTypeList.size()-1; i++)
        {
            programItem = mAfProgramItemList.get(i);
            itemArray = getAfItemArray(i);
            map = new HashMap<String, Object>();
            map.put("title", getDisplayString(mAfTitleArray[i]));
            map.put("index",String.valueOf(i+1)+".");

            int index = (int)programItem.value[0];
            if(index < 0)
            {
                index = 0;
            }
            else if(index >= itemArray.length)
            {
                index = itemArray.length-1;
            }
            map.put("value",itemArray.length==0?"":itemArray[index]);
            mAfDataList.add(map);
        }

        // Build SF data list
        for (int i = 0; i < mSfTypeList.size()-1; i++)
        {
            programItem = mSfProgramItemList.get(i);
            itemArray = getSfItemArray(i);
            map = new HashMap<String, Object>();
            map.put("title", getDisplayString(mSfTitleArray[i]));
            map.put("index",String.valueOf(i+1)+".");

            int index = (int)programItem.value[0];
            if(index <0)
            {
                index = 0;
            }

            if(index>= itemArray.length)
            {
                index = itemArray.length-1;
            }
            map.put("value",itemArray.length==0?"":itemArray[index]);
            mSfDataList.add(map);
        }

        map = new HashMap<String, Object>();
        map.put("title",  getDisplayString(R.string.title_item_af_table));
        map.put("value","");
        mAfDataList.add(0,map);

        map = new HashMap<String, Object>();
        map.put("title", getDisplayString(R.string.title_item_sf_table));
        map.put("value","");
        mSfDataList.add(0,map);

        mDataList.addAll(mAfDataList);
        mDataList.addAll(mSfDataList);

        return mDataList;
    }


    private void updateListAdapter(final boolean dataEnabled)
    {
        if(programTableListAdapter == null)
        {
            programTableListAdapter = new ProgramTableListAdapter(this,getDataList(),true,null,null);
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


    private String[] getAfItemArray(final int itemIndex)
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
                itemArray = new String[]{"Detection after disarm", "Detection after running"};
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
                itemArray = new String[]{};
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
                itemArray = new String[]{"Flex CH Event 34 (3L+1)", "Flex CH Event 33 (LL+1)", "Flex CH Event 36 (2L+3)", "Webasto ON/OFF via UART"};
            }
            break;
            case AF_ITEM_EVENT_1_INPUT:
            {
                itemArray = new String[]{};
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
            default:
            {
                itemArray = null;
            }
            break;
        }


        return itemArray;
    }

    private String[] getSfItemArray(final int itemIndex)
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

    private String getDisplayString(final int stringId)
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

    private void customPickDialog(final String title, final String[] items, final int defaultIndex)
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
        DialogUtil.singleChoiceDialog(this, title, items, defaultIndex, onOkClickListener, onCancelClick, onItemClickListener, R.string.ok, R.string.cancel);
    }

    // Handle selection of pop dialog
    private void handleAction(final String title,int position)
    {
        try
        {
            int index;
            index = getIndexByTitle(mAfTitleArray,title);
            if(index != -1)
            {
                final ProgramItem programItem = mAfProgramItemList.get(index);
                programItem.value[0] = (byte)position;
            }
            else
            {
                index = getIndexByTitle(mSfTitleArray,title);
                if(index != -1)
                {
                    final ProgramItem programItem = mSfProgramItemList.get(index);
                    programItem.value[0] = (byte)position;
                }
            }
        }
        catch (Exception e)
        {
            LogUtil.e(getPackageName(), e.toString(), Thread.currentThread().getStackTrace());
        }
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


    private void initAfProgramList()
    {
        for(int i=0;i<AF_TABLE.length;i++)
        {
            final ProgramItem programItem = initProgramItem(AF_TABLE[i]);
            programItem.value = getProgramItemValue(RANDOM_TABLE,AF_START,programItem);
            mAfProgramItemList.add(programItem);
        }
    }

    private void initSfProgramList()
    {
        for(int i=0;i<SF_TABLE.length;i++)
        {
            final ProgramItem programItem = initProgramItem(SF_TABLE[i]);
            programItem.value = getProgramItemValue(RANDOM_TABLE,SF_START,programItem);
            mSfProgramItemList.add(programItem);
        }
    }

    private byte[] getProgramItemValue(final byte[] dataArray,final int startIndex,final ProgramItem programItem)
    {
        final byte[] data = subByteArray(dataArray,startIndex+programItem.offset,programItem.length);
        return data;
    }

    private ProgramItem initProgramItem(int[] itemDefine)
    {
        final ProgramItem programItem = new ProgramItem();
        programItem.offset = itemDefine[0];
        programItem.length = itemDefine[1];
        programItem.minValue = itemDefine[2];
        programItem.maxValue = itemDefine[3];

        return programItem;
    }


    private byte[] generateRandomArray(final int length,final int mode)
    {
        if(length <= 0 ) return null;

        final byte[] randomArray = new byte[length];
        for(int i=0;i<length;i++)
        {
            randomArray[i] = getRandom(mode);
        }

        return randomArray;
    }

    private byte getRandom(final int modeNum)
    {
        final Random r = new Random();
        return (byte)r.nextInt(modeNum);
    }

    private byte[] subByteArray(final byte[] byteArray,final int offset,final int length)
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

    private String formatByteArrayToLog(final byte[] byteArray)
    {
        String outputString = "";
        for(int i=0 ;i<byteArray.length;i++)
        {
            outputString = outputString + String.format(" %02X",byteArray[i]);
            if((i+1) % 8 == 0)
                outputString = outputString+ "\n";
        }

        return outputString;
    }

    private String formatByteArrayToHexString(final byte[] byteArray)
    {
        String outputString = "";
        for(int i=0 ;i<byteArray.length;i++)
        {
            outputString = outputString + String.format("0x%02X ",byteArray[i]);
        }

        return outputString;
    }
}

package com.startline.slble.Activity;

/**
 * Created by terry on 2016/6/8.
 */

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;
import com.startline.slble.Fragment.BaseFragment;
import com.startline.slble.Fragment.ChFragment;
import com.startline.slble.Fragment.TabFragment;
import com.startline.slble.Interface.OnProgramDataChangedListener;
import com.startline.slble.R;
import com.startline.slble.Service.BluetoothLeIndependentService;
import com.startline.slble.Util.LogUtil;

import static com.startline.slble.Service.BluetoothLeIndependentService.*;

import static com.startline.slble.PureClass.SlbleProtocol.*;
import static com.startline.slble.Interface.ProgramTool.*;

public class ProgramToolActivity extends AppCompatActivity
{
    private static final String TAG = "ProgramToolActivity";
    public static final int PROGRAM_DATA_TYPE_AF = 0;
    public static final int PROGRAM_DATA_TYPE_SF = 1;
    public static final int PROGRAM_DATA_TYPE_LNT = 2;
    public static final int PROGRAM_DATA_TYPE_CH = 3;

    private int mCurrentIndex = 0;

    private Context context = null;
    private Menu mMenu = null;
    private TabFragment mTabFragment = null;
    private FragmentManager mFragmentManager = null;
    private BluetoothLeIndependentService mService = null;
    private byte[] mInitAfData = null;
    private byte[] mInitSfData = null;
    private byte[] mInitLntData = null;
    private byte[] mInitChData = null;

    private byte[] mModifiedAfData = null;
    private byte[] mModifiedSfData = null;
    private byte[] mModifiedLntData = null;
    private byte[] mModifiedChData = null;
    private byte[] mWriteData = null;

    private ProgressDialog mProgressDialog = null;
    private OnProgramDataChangedListener onProgramDataChangedListener = new OnProgramDataChangedListener()
    {
        @Override
        public void onProgramDataChanged(final int index,final byte[] data)
        {
            handleProgramDataChanged(index,data);
        }
    };

    private ViewPager.OnPageChangeListener onPageChangeListener = new ViewPager.OnPageChangeListener()
    {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels)
        {
            //Toast.makeText(context,"onPageScrolled : " + position,Toast.LENGTH_SHORT).show();
            Log.d(TAG,"onPageScrolled : " + position);
        }

        @Override
        public void onPageSelected(int position)
        {
            //Toast.makeText(context,"onPageSelected : " + position,Toast.LENGTH_SHORT).show();
            Log.d(TAG,"onPageSelected : " + position);
        }

        @Override
        public void onPageScrollStateChanged(int state)
        {
            //Toast.makeText(context,"onPageScrollStateChanged : " + state,Toast.LENGTH_SHORT).show();
            Log.d(TAG,"onPageScrollStateChanged : " + state + ",Index = " + mTabFragment.getCurrentIndex());
            if(state == 0 )
            {
                handleProgramDataChanged(mTabFragment.getCurrentIndex(),getModifiedData(mTabFragment.getCurrentIndex()));
            }
            else if(state == 2)
            {
                mCurrentIndex = mTabFragment.getCurrentIndex();
                Log.d(TAG,"Set mCurrentIndex = " + mCurrentIndex);
            }
        }
    };

    private Handler mHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            if(msg.arg1 == IPC_COMMAND_PROGRAM_TASK_FINISH)
            {
                if(mProgressDialog.isShowing())
                {
                    mProgressDialog.dismiss();
                }
            }
            else if(msg.arg1 == IPC_COMMAND_PROGRAM_RESULT)
            {
                final BluetoothLeIndependentService.TaskObject taskObject = (BluetoothLeIndependentService.TaskObject)msg.obj;
                final BluetoothLeIndependentService.ProgramData programData = (BluetoothLeIndependentService.ProgramData)taskObject.taskData;

                if(taskObject.taskCommand == CMD_PROGRAM_INTERFACE)
                {
                    if(taskObject.taskParameter == PARAM_ASK_INTO_PROGRAM_INTERFACE)
                    {
                        if(programData.dataCount == programData.dataLength)
                        {
                            //syncAllData();
                            Toast.makeText(context,"Open program mode success",Toast.LENGTH_SHORT).show();
                        }
                        else
                        {
                            Toast.makeText(context,"Open program mode failed",Toast.LENGTH_SHORT).show();
                        }
                    }
                    // Exit Program mode
                    else
                    {

                    }
                }
                else if(taskObject.taskCommand == CMD_PROGRAM_DATA)
                {
                    // Read
                    if(taskObject.taskParameter == PARAM_READ_PROGRAM_DATA)
                    {
                        if(programData.dataCount == programData.dataLength)
                        {
                            // Verify data
                            if(mWriteData != null)
                            {
                                final byte[] readData = copyByteArray(programData.dataBuffer);
                                if(isDataChanged(mWriteData,readData))
                                {
                                    Toast.makeText(context,"Save fail.",Toast.LENGTH_SHORT).show();
                                }
                                else
                                {
                                    Toast.makeText(context,"Save success.",Toast.LENGTH_SHORT).show();
                                    saveAndUpdate(programData);
                                }
                                mWriteData = null;
                            }
                            // Read data
                            else
                            {
                                saveAndUpdate(programData);
                            }
                        }
                        else
                        {
                            Toast.makeText(context,"Sync failed",Toast.LENGTH_SHORT).show();
                        }
                    }
                    // Write
                    else
                    {
                        if(programData.dataCount == programData.dataLength)
                        {
                            // Verify
                            if(mWriteData != null)
                            {
                                mProgressDialog.setMessage("Verify...Please wait.");
                                mProgressDialog.show();
                                syncSetting();
                            }
                        }
                        else
                        {
                            Toast.makeText(context,"Save failed",Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.program_tool_activity_menu, menu);
        if(mMenu == null)
        {
            mMenu = menu;
            showSaveButton(false);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle presses on the action bar items
        switch (item.getItemId())
        {
            case R.id.action_sync:
            {
                syncSetting();
            }
            return true;
            case R.id.action_save:
            {
                saveSetting();
            }
            return true;
            case R.id.action_into:
            {
                intoProgramMode();
            }
            return true;
            case R.id.action_exit:
            {
                exitProgramMode();
            }
            return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if(getService() != null)
        {
            getService().setIpcCallbackhandler(mHandler);
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        if(getService() != null)
        {
            getService().setIpcCallbackhandler(null);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        context = this;

        setContentView(R.layout.program_table_container);
//
//        ActionBar actionBar = getSupportActionBar();
//        actionBar.hide();

        //initToolbar();
        initTabFragment(savedInstanceState);

        mProgressDialog = new ProgressDialog(context);

        mHandler.postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                saveInitData(0,new byte[32]);
                saveModifiedData(0,new byte[32]);
                updateFragmentProgramData(0,new byte[32]);

                saveInitData(1,new byte[32]);
                saveModifiedData(1,new byte[32]);
                updateFragmentProgramData(1,new byte[32]);

                saveInitData(2,new byte[16]);
                saveModifiedData(2,new byte[16]);
                updateFragmentProgramData(2,new byte[16]);

                saveInitData(3,new byte[16]);
                saveModifiedData(3,new byte[16]);
                updateFragmentProgramData(3,new byte[16]);

                //intoProgramMode();
            }
        },500);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState)
    {
        super.onPostCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        //getService().askExitProgramMode();
    }

    //    private void initToolbar()
//    {
//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);
//
//        ActionBar actionBar = getSupportActionBar();
//        actionBar.setTitle("SlidingTabs Demo");
//    }

    private void initTabFragment(Bundle savedInstanceState)
    {
        if (savedInstanceState == null)
        {
            mTabFragment = new TabFragment();
            mTabFragment.setOnProgramDataChangedListener(onProgramDataChangedListener);
            mTabFragment.setOnPageChangeListener(onPageChangeListener);
            mFragmentManager = getSupportFragmentManager();
            mFragmentManager
                .beginTransaction()
                .add(R.id.content_fragment, mTabFragment)
                .commit();
        }
    }

    private void saveAndUpdate(final BluetoothLeIndependentService.ProgramData programData)
    {
        saveInitData(programData.dataId,copyByteArray(programData.dataBuffer));
        saveModifiedData(programData.dataId,copyByteArray(programData.dataBuffer));

        // Update fragment data
        updateFragmentProgramData(programData.dataId,copyByteArray(programData.dataBuffer));

        // Refresh action button
        handleProgramDataChanged(programData.dataId,programData.dataBuffer);
    }

    private void intoProgramMode()
    {
        mProgressDialog.setMessage("Enter Program...Please wait.");
        mProgressDialog.setMax(0);
        mProgressDialog.show();

        getService().askIntoProgramMode();
    }

    private void exitProgramMode()
    {
//        mProgressDialog.setMessage("Exit Program...Please wait.");
//        mProgressDialog.setMax(0);
//        mProgressDialog.show();

        getService().askExitProgramMode();
    }

    private void syncAllData()
    {
        mProgressDialog.setMessage("Sync...Please wait.");
        mProgressDialog.setMax(1);
        mProgressDialog.show();

        getService().readProgramTable(PROGRAM_DATA_TYPE_AF,AF_ADDRESS_HIGH,AF_ADDRESS_LOW,AF_DATA_LENGTH);
        getService().readProgramTable(PROGRAM_DATA_TYPE_SF,SF_ADDRESS_HIGH,SF_ADDRESS_LOW,SF_DATA_LENGTH);
        getService().readProgramTable(PROGRAM_DATA_TYPE_LNT,LNT_ADDRESS_HIGH,LNT_ADDRESS_LOW,LNT_DATA_LENGTH);
        getService().readProgramTable(PROGRAM_DATA_TYPE_CH,FLEX_CH1_ADDRESS_HIGH,FLEX_CH1_ADDRESS_LOW,CH_DATA_LENGTH);
    }

    public void syncSetting()
    {
        final int currentIndex = mTabFragment.getCurrentIndex();
        if(currentIndex < 0)
        {
            return;
        }

        if(getService() == null)
        {
            Toast.makeText(context, "Can not get bluetooth service", Toast.LENGTH_SHORT).show();
            return;
        }

        if(!mProgressDialog.isShowing())
        {
            mProgressDialog.setMessage("Sync...Please wait.");
            mProgressDialog.show();
            mProgressDialog.setMax(0);
        }

        switch (currentIndex)
        {
            case PROGRAM_DATA_TYPE_AF:
            {
                getService().readProgramTable(currentIndex,AF_ADDRESS_HIGH,AF_ADDRESS_LOW,AF_DATA_LENGTH);
            }
            break;
            case PROGRAM_DATA_TYPE_SF:
            {
                getService().readProgramTable(currentIndex,SF_ADDRESS_HIGH,SF_ADDRESS_LOW,SF_DATA_LENGTH);
            }
            break;
            case PROGRAM_DATA_TYPE_LNT:
            {
                getService().readProgramTable(currentIndex,LNT_ADDRESS_HIGH,LNT_ADDRESS_LOW,LNT_DATA_LENGTH);
            }
            break;
            case PROGRAM_DATA_TYPE_CH:
            {
                final ChFragment chFragment = (ChFragment)mTabFragment.getTabFragmentAdapter().getItem(PROGRAM_DATA_TYPE_CH);
                final int channel = chFragment.getCurrentChannel();
                final int[] address = getChannelAddress(channel);
                LogUtil.d(TAG,"Sync channel " + channel,Thread.currentThread().getStackTrace());
                getService().readProgramTable(currentIndex,address[1],address[0],CH_DATA_LENGTH);
            }
            break;
            default:
            {
                mProgressDialog.dismiss();
            }
            break;
        }
    }

    private void showSaveButton(final boolean visible)
    {
        mMenu.findItem(R.id.action_save).setEnabled(visible);
        //invalidateOptionsMenu();
    }

    private void saveInitData(final int index,final byte[] data)
    {
        switch (index)
        {
            case 0:
            {
                mInitAfData = data;
            }
            break;
            case 1:
            {
                mInitSfData = data;
            }
            break;
            case 2:
            {
                mInitLntData = data;
            }
            break;
            case 3:
            {
                mInitChData = data;
            }
            break;
        }
    }

    private byte[] getInitData(final int index)
    {
        switch (index)
        {
            case PROGRAM_DATA_TYPE_AF:
            {
                return mInitAfData;
            }

            case PROGRAM_DATA_TYPE_SF:
            {
                return mInitSfData;
            }

            case PROGRAM_DATA_TYPE_LNT:
            {
                return mInitLntData;
            }

            case PROGRAM_DATA_TYPE_CH:
            {
                return mInitChData;
            }
        }

        return null;
    }

    private void saveModifiedData(final int index,final byte[] data)
    {
        switch (index)
        {
            case PROGRAM_DATA_TYPE_AF:
            {
                mModifiedAfData = data;
            }
            break;
            case PROGRAM_DATA_TYPE_SF:
            {
                mModifiedSfData = data;
            }
            break;
            case PROGRAM_DATA_TYPE_LNT:
            {
                mModifiedLntData = data;
            }
            break;
            case PROGRAM_DATA_TYPE_CH:
            {
                mModifiedChData = data;
            }
            break;
        }
    }

    private byte[] getModifiedData(final int index)
    {
        switch (index)
        {
            case PROGRAM_DATA_TYPE_AF:
            {
                return mModifiedAfData;
            }

            case PROGRAM_DATA_TYPE_SF:
            {
                return mModifiedSfData;
            }

            case PROGRAM_DATA_TYPE_LNT:
            {
                return mModifiedLntData;
            }

            case PROGRAM_DATA_TYPE_CH:
            {
                return mModifiedChData;
            }
        }

        return null;
    }

    private void handleProgramDataChanged(final int index,final byte[] data)
    {
        LogUtil.d(TAG,"Index="+index,Thread.currentThread().getStackTrace());

        if(index != mCurrentIndex)
        {
            LogUtil.d(TAG,"Skip",Thread.currentThread().getStackTrace());
            return;
        }

        if(isDataChanged(getInitData(index),data))
        {
            saveModifiedData(index,data);
            showSaveButton(true);
        }
        else
        {
            showSaveButton(false);
        }
    }

    private void updateFragmentProgramData(final int index,final byte[] data)
    {
        if(mTabFragment == null)
        {
            return;
        }

        final BaseFragment baseFragment = (BaseFragment) mTabFragment.getTabFragmentAdapter().getItem(index);
        baseFragment.setInitData(data);
        baseFragment.refresh();
    }

    public void saveSetting()
    {
        final int currentIndex = mTabFragment.getCurrentIndex();
        if(currentIndex < 0)
        {
            return;
        }

        if(getService() == null)
        {
            Toast.makeText(context, "Can not get bluetooth service", Toast.LENGTH_SHORT).show();
            return;
        }

        mProgressDialog.setMessage("Save...Please wait.");
        mProgressDialog.show();
        mProgressDialog.setMax(0);
        final byte[] temp = new byte[]{0x00,0x02,0x04,0x06,0x08,0x0A,0x0C,0x0E,0x00,0x02,0x04,0x06,0x08,0x0A,0x0C,0x0E,0x00,0x02,0x04,0x06,0x08,0x0A,0x0C,0x0E,0x00,0x02,0x04,0x06,0x08,0x0A,0x0C,0x0E};
        switch (currentIndex)
        {
            case PROGRAM_DATA_TYPE_AF:
            {
                mWriteData = copyByteArray(mModifiedAfData);
                getService().writeProgramTable(currentIndex,AF_ADDRESS_HIGH,AF_ADDRESS_LOW,mWriteData);
            }
            break;
            case PROGRAM_DATA_TYPE_SF:
            {
                mWriteData = copyByteArray(mModifiedSfData);
                getService().writeProgramTable(currentIndex,SF_ADDRESS_HIGH,SF_ADDRESS_LOW,mWriteData);
            }
            break;
            case PROGRAM_DATA_TYPE_LNT:
            {
                mWriteData = copyByteArray(mModifiedLntData);
                getService().writeProgramTable(currentIndex,LNT_ADDRESS_HIGH,LNT_ADDRESS_LOW,mWriteData);
            }
            break;
            case PROGRAM_DATA_TYPE_CH:
            {
                mWriteData = copyByteArray(mModifiedChData);
                final ChFragment chFragment = (ChFragment)mTabFragment.getTabFragmentAdapter().getItem(PROGRAM_DATA_TYPE_CH);
                final int channel = chFragment.getCurrentChannel();
                final int[] address = getChannelAddress(channel);
                LogUtil.d(TAG,"Save channel " + channel,Thread.currentThread().getStackTrace());
                getService().writeProgramTable(currentIndex,address[1],address[0],mWriteData);
            }
            break;
            default:
            {
                mProgressDialog.dismiss();
            }
            break;
        }
    }

    private BluetoothLeIndependentService getService()
    {
        if(mService == null)
        {
            mService = BluetoothLeIndependentService.getInstance();
        }

        return mService;
    }

    private int[] getChannelAddress(final int channel)
    {
        final int[] address = new int[2];
        switch (channel)
        {
            default:
            case 1:
            {
                address[1] = FLEX_CH1_ADDRESS_HIGH;
                address[0] = FLEX_CH1_ADDRESS_LOW;
            }
            break;
            case 2:
            {
                address[1] = FLEX_CH2_ADDRESS_HIGH;
                address[0] = FLEX_CH2_ADDRESS_LOW;
            }
            break;
            case 3:
            {
                address[1] = FLEX_CH3_ADDRESS_HIGH;
                address[0] = FLEX_CH3_ADDRESS_LOW;
            }
            break;
            case 4:
            {
                address[1] = FLEX_CH4_ADDRESS_HIGH;
                address[0] = FLEX_CH4_ADDRESS_LOW;
            }
            break;
            case 5:
            {
                address[1] = FLEX_CH5_ADDRESS_HIGH;
                address[0] = FLEX_CH5_ADDRESS_LOW;
            }
            break;
            case 6:
            {
                address[1] = FLEX_CH6_ADDRESS_HIGH;
                address[0] = FLEX_CH6_ADDRESS_LOW;
            }
            break;
            case 7:
            {
                address[1] = FLEX_CH7_ADDRESS_HIGH;
                address[0] = FLEX_CH7_ADDRESS_LOW;
            }
            break;
            case 8:
            {
                address[1] = FLEX_CH8_ADDRESS_HIGH;
                address[0] = FLEX_CH8_ADDRESS_LOW;
            }
            break;
            case 9:
            {
                address[1] = FLEX_CH9_ADDRESS_HIGH;
                address[0] = FLEX_CH9_ADDRESS_LOW;
            }
            break;
            case 10:
            {
                address[1] = FLEX_CH10_ADDRESS_HIGH;
                address[0] = FLEX_CH10_ADDRESS_LOW;
            }
            break;
            case 11:
            {
                address[1] = FLEX_CH11_ADDRESS_HIGH;
                address[0] = FLEX_CH11_ADDRESS_LOW;
            }
            break;
            case 12:
            {
                address[1] = FLEX_CH12_ADDRESS_HIGH;
                address[0] = FLEX_CH12_ADDRESS_LOW;
            }
            break;
        }

        return address;
    }

    public static boolean isDataChanged(final byte[] data1,final byte[] data2)
    {
        if(data1 == null || data2 == null)
        {
            return true;
        }

        if(data1.length != data2.length)
        {
            return true;
        }

        for(int i=0;i<data1.length; i++)
        {
            if(data1[i]!=data2[i])
            {
                return true;
            }
        }

        return false;
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

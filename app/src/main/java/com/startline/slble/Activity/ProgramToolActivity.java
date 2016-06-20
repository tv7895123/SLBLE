package com.startline.slble.Activity;

/**
 * Created by terry on 2016/6/8.
 */

import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;
import com.startline.slble.Fragment.BaseFragment;
import com.startline.slble.Fragment.TabFragment;
import com.startline.slble.Interface.OnProgramDataChangedListener;
import com.startline.slble.R;
import com.startline.slble.Service.BluetoothLeIndependentService;

import static com.startline.slble.Service.BluetoothLeIndependentService.*;

import static com.startline.slble.Interface.ProgramTool.*;

public class ProgramToolActivity extends AppCompatActivity
{
    public static final int PROGRAM_DATA_TYPE_AF = 0;
    public static final int PROGRAM_DATA_TYPE_SF = 1;
    public static final int PROGRAM_DATA_TYPE_LNT = 2;
    public static final int PROGRAM_DATA_TYPE_CH = 3;


    private Menu mMenu = null;
    private TabFragment mTabFragment = null;
    private FragmentManager mFragmentManager = null;
    private BluetoothLeIndependentService mService = null;
    private byte[] mInitData = null;
    private byte[] mModifiedData = null;

    private ProgressDialog mProgressDialog = null;
    private OnProgramDataChangedListener onProgramDataChangedListener = new OnProgramDataChangedListener()
    {
        @Override
        public void onProgramDataChanged(final byte[] data)
        {
            if(isDataChanged(mInitData,data))
            {
                mModifiedData = copyByteArray(data);
                showSaveButton(true);
            }
            else
            {
                showSaveButton(false);
            }
        }
    };

    private Handler mHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.arg1)
            {
                case PROGRAM_TABLE_READ:
                {
                    mProgressDialog.dismiss();
                    if(msg.arg2 == PROGRAM_TABLE_FAIL)
                    {
                        Toast.makeText(ProgramToolActivity.this,"Read failed.",Toast.LENGTH_SHORT).show();
                    }
                    else
                    {
                        updateProgramData();
                    }
                }
                break;
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

        setContentView(R.layout.program_table_container);
//
//        ActionBar actionBar = getSupportActionBar();
//        actionBar.hide();

        //initToolbar();
        initTabFragment(savedInstanceState);

        mProgressDialog = new ProgressDialog(this);

        syncSetting();
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
            mFragmentManager = getSupportFragmentManager();
            mFragmentManager
                .beginTransaction()
                .add(R.id.content_fragment, mTabFragment)
                .commit();
        }
    }

    private void syncSetting()
    {
        final int currentIndex = mTabFragment.getCurrentIndex();
        if(currentIndex < 0)
        {
            return;
        }

        if(getService() == null)
        {
            Toast.makeText(ProgramToolActivity.this, "Can not get bluetooth service", Toast.LENGTH_SHORT).show();
            return;
        }

        mProgressDialog.setMessage("Sync...Please wait.");
        mProgressDialog.show();
        switch (currentIndex)
        {
            case PROGRAM_DATA_TYPE_AF:
            {
                getService().readProgramTable(AF_ADDRESS_HIGH,AF_ADDRESS_LOW,AF_DATA_LENGTH);
            }
            break;
            case PROGRAM_DATA_TYPE_SF:
            {
                getService().readProgramTable(SF_ADDRESS_HIGH,SF_ADDRESS_LOW,SF_DATA_LENGTH);
            }
            break;
            case PROGRAM_DATA_TYPE_LNT:
            {
                getService().readProgramTable(LNT_ADDRESS_HIGH,LNT_ADDRESS_LOW,LNT_DATA_LENGTH);
            }
            break;
            case PROGRAM_DATA_TYPE_CH:
            {
                //getService().readProgramTable(CH_ADDRESS_HIGH,LNT_ADDRESS_LOW,CH_DATA_LENGTH);
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
        invalidateOptionsMenu();
    }

    private void updateProgramData()
    {
        if(mTabFragment == null)
        {
            return;
        }

        final BaseFragment baseFragment = (BaseFragment) mTabFragment.getTabFragmentAdapter().getItem(mTabFragment.getCurrentIndex());
        baseFragment.setInitData(mModifiedData);
        baseFragment.refresh();
    }

    private void saveSetting()
    {
        if(mTabFragment == null)
        {
            return;
        }

        if(getService() != null)
        {
            getService().writeProgramTable(0,0,null);
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

    private boolean isDataChanged(final byte[] data1,final byte[] data2)
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

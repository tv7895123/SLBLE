package com.startline.slble.Util;

import android.content.Context;
import android.content.SharedPreferences;
import com.startline.slble.PureClass.Constants;

import java.util.HashMap;

import static com.startline.slble.PureClass.Constants.CONFIG_ITEM_DEVICE_ADDRESS;
import static com.startline.slble.PureClass.Constants.CONFIG_ITEM_DEVICE_NAME;
import static com.startline.slble.PureClass.Constants.CONFIG_ITEM_WRITE_LOG;

/**
 * Created by terry on 2017/6/14.
 */
public class StorageUtil
{
    //================================================================================================================//
    //
    //  SharedPreferences
    //
    //================================================================================================================//
    public static void saveAutoConnectDevice(final Context context, final String name, final String address)
    {
        final SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.CONFIG_FILE_ACT_SETTING, Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(CONFIG_ITEM_DEVICE_NAME,name);
        editor.putString(CONFIG_ITEM_DEVICE_ADDRESS,address);
        editor.commit();
    }

    public static HashMap getAutoConnectDevice(final Context context)
    {
        final SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.CONFIG_FILE_ACT_SETTING, Context.MODE_PRIVATE);
        final HashMap<String,String> hashMap = new HashMap<>();
        hashMap.put(CONFIG_ITEM_DEVICE_NAME, sharedPreferences.getString(CONFIG_ITEM_DEVICE_NAME,""));
        hashMap.put(CONFIG_ITEM_DEVICE_ADDRESS, sharedPreferences.getString(CONFIG_ITEM_DEVICE_ADDRESS,""));
        return hashMap;
    }

    public static void saveWriteLogConfig(final Context context, final boolean writeLog)
    {
        final SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.CONFIG_FILE_ACT_SETTING, Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(CONFIG_ITEM_WRITE_LOG,writeLog);
        editor.commit();
    }

    public static boolean getWriteLogConfig(final Context context)
    {
        final SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.CONFIG_FILE_ACT_SETTING, Context.MODE_PRIVATE);
        final boolean writeLog = sharedPreferences.getBoolean(CONFIG_ITEM_WRITE_LOG,false);
        return writeLog;
    }


}

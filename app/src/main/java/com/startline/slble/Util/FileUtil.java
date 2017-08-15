package com.startline.slble.Util;

import android.content.Context;
import android.util.Log;

import java.io.*;

/**
 * Created by terry on 2016/1/20.
 */
public class FileUtil
{
    private static final String TAG = FileUtil.class.getSimpleName();

    public static void writeToFile(final Context context, final String strFileName, final Object srcObject)
    {
        if(null == strFileName)
            throw new RuntimeException("FileName is null!");

        final File file = context.getFileStreamPath(strFileName);
        final String strPath = file.getParent();

        writeToFile(context,strPath,strFileName,srcObject);
    }

    public static void writeToFile(final Context context, final String strPath, final String strFileName, final Object srcObject)
    {
        if(null == strFileName)
            throw new RuntimeException("FileName is null!");

        final File filePath = new File(strPath);
        try
        {
            if(!filePath.exists())
            {
                filePath.mkdirs();
            }

            final File file = new File(strPath,strFileName);

            if(file.exists() || file.createNewFile())
            {
                final FileOutputStream fos = context.openFileOutput(strFileName, Context.MODE_PRIVATE);
                final ObjectOutputStream oos = new ObjectOutputStream(fos);
                oos.writeObject(srcObject);
                fos.close();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }



    public static Object loadFromFile(final Context context, final String strFileName)
    {
        if(strFileName == null)
            return null;

        final File file = context.getFileStreamPath(strFileName);
        final String strPath = file.getParent();

        return loadFromFile(context,strPath,strFileName);
    }

    public static Object loadFromFile(final Context context, final String strPath, final String strFileName)
    {
        if(strFileName == null)
            return null;

        final File filePath = new File(strPath);
        Object object = null;
        try
        {
            if(!filePath.exists())
            {
                filePath.mkdirs();
            }

            final File file = new File(strPath,strFileName);

            if(file.exists())
            {
                final FileInputStream fis = context.openFileInput(strFileName);
                final ObjectInputStream ois = new ObjectInputStream(fis);
                object = ois.readObject();
                fis.close();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return object;
    }

    public static String readFromFile(final Context context, final String strPath, final String strFileName)
    {
        String ret = "";

        try
        {
            final File filePath = new File(strPath);
            if(!filePath.exists())
            {
                filePath.mkdirs();
            }

            final File file = new File(strPath,strFileName);
            if(!file.exists())
            {
                return null;
            }


            final FileInputStream fileInputStream = new FileInputStream(file);

            if ( fileInputStream != null )
            {
                final InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
                final int size = fileInputStream.available();
                final char[] buffer = new char[size];

                inputStreamReader.read(buffer);
                fileInputStream.close();
                ret = new String(buffer);
            }
        }
        catch (FileNotFoundException e)
        {
            Log.e(TAG, "File not found: " + e.toString());
        }
        catch (IOException e)
        {
            Log.e(TAG, "Can not read file: " + e.toString());
        }

        return ret;
    }

    public static void appendToFile(final Context context, final String strPath, final String strFileName, final String message)
    {
        if(null == strFileName)
            throw new RuntimeException("FileName is null!");

        final File filePath = new File(strPath);
        try
        {
            if(!filePath.exists())
            {
                filePath.mkdirs();
            }
            final File file = new File(strPath,strFileName);

            if(file.exists() || file.createNewFile())
            {
                //BufferedWriter for performance, true to set append to file flag
                BufferedWriter buf = new BufferedWriter(new FileWriter(file, true));
                buf.append(message);
                buf.newLine();
                buf.close();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static void deleteFile(final File fileOrDirectory)
    {
        if (fileOrDirectory.isDirectory())
        {
            for (File child : fileOrDirectory.listFiles())
            {
                deleteFile(child);
            }
        }

        fileOrDirectory.delete();
    }

}

package com.startline.slble.Util;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created with IntelliJ IDEA.
 * User: terry
 * Date: 2014/3/26
 * Time: 下午 2:29
 * To change this template use File | Settings | File Templates.
 */
public class LogUtil
{
    private final static String TAG = LogUtil.class.getSimpleName();
    public static final int LOG_ERROR = 0x06;
    public static final int LOG_WARNING = 0x05;
    public static final int LOG_INFO = 0x04;
    public static final int LOG_DEBUG = 0x03;
    public static final int LOG_VERBOSE = 0x02;

	public static String mPathLog = "";

	public static final String LOG_PATTERN_APP = "App-%s.log";
	public static final String LOG_PATTERN_SYS = "Sys-%s.log";


    private static int debugLevel = LOG_VERBOSE;

    public int getDebugLevel()
    {
        return debugLevel;
    }

    public void setDebugLevel(int level)
    {
        debugLevel = level;
    }

    private static String getPathLog(final Context context)
    {
        if(mPathLog.isEmpty())
            mPathLog = context.getFilesDir().getAbsolutePath();

        return mPathLog;
    }

	public static void saveLogToFile(final String message)
	{
		try
		{
//			final Date date = new Date();
//        	final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.sss", Locale.ENGLISH);
//			final String YMD = simpleDateFormat.format(date);
//			final String YM = YMD.substring(0,7);
//			final String MD = YMD.substring(5,YMD.length());
//        	final String fullName = String.format(LOG_PATTERN_APP,YM);
//			final File logDir = new File(getPathLog());
//			final File file = new File (getPathLog(), fullName);
//
//			if(logDir.exists() == false)
//				logDir.mkdirs();
//
//			if(file.exists() == false)
//				file.createNewFile();
//
//			final FileWriter fileWriter = new FileWriter(file.getAbsolutePath(),true);
//			final BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
//			bufferedWriter.write(String.format("\r\n[%s]:  ",MD) +message);
//			bufferedWriter.close();
		}
		catch (Exception e)
		{
			Log.e(TAG, e.toString());
		}
	}


    public static void v(String tagName,String message,StackTraceElement[] stackTraceElement)
    {
        if (debugLevel <= LOG_VERBOSE)
        {
            String fullClassName = stackTraceElement[2].getClassName();
            String className = fullClassName.substring(fullClassName.lastIndexOf(".") + 1);
            String methodName = stackTraceElement[2].getMethodName();
            int lineNumber = stackTraceElement[2].getLineNumber();

			final String fullMessage = String.format("%s.java:%d - %s() : %s", className, lineNumber, methodName, message);
            Log.v(tagName, fullMessage);
			saveLogToFile(fullMessage);
        }
    }


    public static void d(String tagName,String message,StackTraceElement[] stackTraceElement)
    {
        if (debugLevel <= LOG_DEBUG)
        {
            String fullClassName = stackTraceElement[2].getClassName();
            String className = fullClassName.substring(fullClassName.lastIndexOf(".") + 1);
            String methodName = stackTraceElement[2].getMethodName();
            int lineNumber = stackTraceElement[2].getLineNumber();

            final String fullMessage = String.format("%s.java:%d - %s() : %s",  className, lineNumber , methodName , message);
            Log.d(tagName, fullMessage);
			saveLogToFile(fullMessage);
        }
    }


    public static void i(String tagName,String message,StackTraceElement[] stackTraceElement)
    {
        if (debugLevel <= LOG_INFO)
        {
            String fullClassName = stackTraceElement[2].getClassName();
            String className = fullClassName.substring(fullClassName.lastIndexOf(".") + 1);
            String methodName = stackTraceElement[2].getMethodName();
            int lineNumber = stackTraceElement[2].getLineNumber();

            final String fullMessage = String.format("%s.java:%d - %s() : %s",  className, lineNumber , methodName , message);
            Log.i(tagName, fullMessage);
			saveLogToFile(fullMessage);
        }
    }


    public static void w(String tagName,String message,StackTraceElement[] stackTraceElement)
    {
        if (debugLevel <= LOG_WARNING)
        {
            String fullClassName = stackTraceElement[2].getClassName();
            String className = fullClassName.substring(fullClassName.lastIndexOf(".") + 1);
            String methodName = stackTraceElement[2].getMethodName();
            int lineNumber = stackTraceElement[2].getLineNumber();

            final String fullMessage = String.format("%s.java:%d - %s() : %s",  className, lineNumber , methodName , message);
            Log.w(tagName, fullMessage);
			saveLogToFile(fullMessage);
        }
    }


    public static void e(String tagName,String message,StackTraceElement[] stackTraceElement)
    {
        if (debugLevel <= LOG_ERROR)
        {
            String fullClassName = stackTraceElement[2].getClassName();
            String className = fullClassName.substring(fullClassName.lastIndexOf(".") + 1);
            String methodName = stackTraceElement[2].getMethodName();
            int lineNumber = stackTraceElement[2].getLineNumber();

            final String fullMessage = String.format("%s.java:%d - %s() : %s",  className, lineNumber , methodName , message);
            Log.e(tagName, fullMessage);
			saveLogToFile(fullMessage);
        }
    }


	public static File extractLogToFileAndWeb(final Context context)
	{
        //set a file
        final Date date = new Date();
        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        final String fullName = String.format(LOG_PATTERN_SYS,simpleDateFormat.format(date));
		final File logDir = new File(getPathLog(context));
        final File file = new File (getPathLog(context), fullName);

		if(logDir.exists() == false)
			logDir.mkdirs();

        //clears a file
        if(file.exists())
		{
            file.delete();
        }


        //write log to file
        int pid = android.os.Process.myPid();
        try
		{
            String command = String.format("logcat -d -v threadtime *:*");
            Process process = Runtime.getRuntime().exec(command);

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder result = new StringBuilder();
            String currentLine = null;

            while ((currentLine = reader.readLine()) != null)
			{
                   if (currentLine != null)// && currentLine.contains(String.valueOf(pid)))
				   {
                       result.append(currentLine);
                       result.append("\n");
                    }
            }

            FileWriter out = new FileWriter(file);
            out.write(result.toString());
            out.close();

            //Runtime.getRuntime().exec("logcat -d -v time -f "+file.getAbsolutePath());
        }
		catch (IOException e)
		{
            Toast.makeText(context.getApplicationContext(), e.toString(), Toast.LENGTH_SHORT).show();
        }


        //clear the log
        try
		{
            Runtime.getRuntime().exec("logcat -c");
        }
		catch (IOException e)
		{
            Toast.makeText(context.getApplicationContext(), e.toString(), Toast.LENGTH_SHORT).show();
        }

        return file;
    }



}

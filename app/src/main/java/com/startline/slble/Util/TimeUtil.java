package com.startline.slble.Util;

import android.content.Context;
import com.startline.slble.BuildConfig;
import com.startline.slble.R;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created with IntelliJ IDEA.
 * User: roy
 * Date: 13-4-13
 * Time: 下午7:11
 * To change this template use File | Settings | File Templates.
 */
public class TimeUtil
{

	public final static String TAG = BuildConfig.APPLICATION_ID;
	public final static String FORMAT_FULL_TIME = "yyyy-MM-dd HH:mm:ss";
    public final static String FORMAT_YYYY_MM_dd_HH_mm = "yyyy-MM-dd HH:mm";
	public final static String FORMAT_YYYY_MM_dd = "yyyy-MM-dd";
    public final static String FORMAT_HH_mm = "HH:mm";
    public final static String FORMAT_BUILD_TIME = "yyyyMMddHHmm";




	public static String getTimeNow(final String format)
	{
		final SimpleDateFormat sdf = new SimpleDateFormat(format);
		final String now = sdf.format(new Date());

		return now;
	}
    /**
	* 獲取GMT標準時間
	*
	* @return
	*/
    public static Timestamp getGMTTimestamp()
	{
        return new Timestamp(getGMTTimeMillis());
    }

    /**
	 * 獲取GMT標準時間毫秒數
	 *
	 * @return
	 */
    public static long getGMTTimeMillis()
	{
        return System.currentTimeMillis() - TimeZone.getDefault().getRawOffset();
    }

    /**
	 * 由用户时间计算GMT标准时间
	 *
	 * @param date
	 * 格式：yyyy-MM-dd HH:mm:ss
	 **/
    public static Timestamp getGMTTimestamp(String date, String dateFmt)
	{
        SimpleDateFormat sdf = new SimpleDateFormat(dateFmt);
        try
		{
            sdf.parse(date);
            return new Timestamp(sdf.parse(date).getTime() - TimeZone.getDefault().getRawOffset());
        }
		catch (ParseException e)
		{
            return new Timestamp(getGMTTimeMillis());
        }
    }

    /**
	* 獲取傳入時間當天最大時間或最小時間0：min 1：max
	*
	* @paam date
	*        格式：yyyy-MM-dd HH:mm:ss
	**/
    public static Timestamp getTimestampMaxOrMin(Timestamp timestamp, int type)
	{
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String returnDate = "";
        if (type == 0)
		{
            returnDate = sdf.format(new Date(timestamp.getTime())) + " 00:00:00";
        }
		else
		{
            returnDate = sdf.format(new Date(timestamp.getTime())) + " 23:59:59";
        }
        return Timestamp.valueOf(returnDate);
    }

    /**
	 * 獲取兩個時間之間的差值
	 *
	 * @param startDate
	 * @param endDate
	 * @return
     */
    public static String getDifferenceOfDate(String startDate, String endDate)
	{
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        try {
            java.util.Date begin = df.parse(startDate);
            java.util.Date end = df.parse(endDate);
            long between = (end.getTime() - begin.getTime()) / 1000;//除以1000是为了转换成秒
            long hour = between / 3600;
            long minute = between % 3600 / 60;
            String returnVal = "";

            if (hour == 0)
			{
                returnVal = "00:";
            }
			else if (hour < 10)
			{
                returnVal = "0" + hour + ":";
            }
			else
				returnVal = String.valueOf(hour) + ":";

            if (minute == 0)
			{
                returnVal += "00";
            }
			else if (minute < 10)
			{
                returnVal += "0" + minute;
            }
			else
				returnVal += String.valueOf(minute);

            return returnVal;
        }
		catch (Exception e)
		{
            return "00:00";
        }
    }

    public static String formatDate(final Date date,final String format)
    {
        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
        final String newDate = simpleDateFormat.format(date);
        return newDate;
    }

	// Convert from UTC time to local time
    public static String convertToLocalTime(final String utcTime)
	{
		return convertToLocalTime(utcTime,FORMAT_FULL_TIME);
	}

    public static String convertToLocalTime(final String utcTime,final String format)
    {
        String localTime = "";

		// Setup local time zone and format
        final SimpleDateFormat localFormat = new SimpleDateFormat(format);
        final Calendar calendar = Calendar.getInstance();
        final TimeZone localZone = calendar.getTimeZone();
        localFormat.setTimeZone(localZone);

		// Setup UTC time zone and format
        final SimpleDateFormat utcFormat = new SimpleDateFormat(format);
        utcFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

        java.util.Date utcDate = new java.util.Date();
        try
		{

            utcDate = utcFormat.parse(utcTime);

            localTime = localFormat.format(utcDate);
        }
		catch (ParseException e1)
		{
            e1.printStackTrace();
        }
        return localTime;
    }

	public static String convertToUtcTime(final String localTime)
	{
		return convertToUtcTime(localTime,FORMAT_FULL_TIME);
	}

	public static String convertToUtcTime(final String localTime,final String format)
	{
        String utcTime = "";

		// Setup local time zone and format
        final SimpleDateFormat localFormat = new SimpleDateFormat(format);
        final Calendar calendar = Calendar.getInstance();
        final TimeZone localZone = calendar.getTimeZone();
        localFormat.setTimeZone(localZone);

		// Setup UTC time zone and format
        final SimpleDateFormat utcFormat = new SimpleDateFormat(format);
        utcFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

        java.util.Date localDate = new java.util.Date();
        try
		{

            localDate = localFormat.parse(localTime);

            utcTime = utcFormat.format(localDate);
        }
		catch (ParseException e1)
		{
            e1.printStackTrace();
        }
        return utcTime;
	}


    public static String getTimeZoneOffset()
    {
        //    format is "+0500"
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"), Locale.getDefault());
        java.util.Date currentLocalTime = calendar.getTime();
        DateFormat date = new SimpleDateFormat("Z");
        String localTime = date.format(currentLocalTime);
        //Log.d("Time",localTime);

        //  Transform to "+8.5" format
        char sign = localTime.charAt(0);
        int timeOffset = Integer.parseInt(localTime.substring(1, 5));
        int hour = timeOffset/100;
        int minuteOfHour = timeOffset%100*100/60;
        localTime = String.format("%s%d.%d", sign, hour, minuteOfHour);
        return localTime;
        // */

        /*     format is +4
        Calendar mCalendar = new GregorianCalendar();
        TimeZone mTimeZone = mCalendar.getTimeZone();
        int mGMTOffset = mTimeZone.getRawOffset();
        String offsetTime = String.format("GMT offset is %s hours", TimeUnit.HOURS.convert(mGMTOffset, TimeUnit.MILLISECONDS));
        Log.d("Time",offsetTime);
        return offsetTime;
        */
    }



	public static String GetUtcDatetimeAsString(final String dateFormat)
	{
		final SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		final String utcTime = sdf.format(new java.util.Date());

		return utcTime;
	}

	public static String timeOffset(final String format,final String fullDate,final long offset_ms)
	{
		final SimpleDateFormat dateFormat = new SimpleDateFormat(format);
		java.util.Date date = null;
		try
		{
			date = dateFormat.parse(fullDate);
			long time = date.getTime();

			time = time + offset_ms;

			date = new Date(time);
		}
		catch (ParseException e)
		{
			LogUtil.e(TAG, e.toString(), Thread.currentThread().getStackTrace());
		}

		return dateFormat.format(date);
	}

	public static long getTimeDifference(final String strDate1,final String strDate2,final String format)
	{
		final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);

		try
		{
			final Date date1 = simpleDateFormat.parse(strDate1);

			final Date date2 =simpleDateFormat.parse(strDate2);

			return date1.getTime()-date2.getTime();
		}
		catch (Exception e)
		{

		}

		return 0;
	}

    public static String formatDifference(final Context context,final long diff_s)
    {
        String result = "";
        //final long diff_s = diff_ms/1000;
        final int day = (int)diff_s / 86400;
        final int hour = (int)(diff_s%86400)/3600;
        final int minute = (int)(diff_s%3600)/60;


        if(day > 0)
        {
            result = day + context.getString(R.string.days);
        }

        if(hour > 0 || (day>0 && hour == 0))
        {
            result = result + hour + context.getString(R.string.hour);
        }

        if(minute > 0 || day > 0 || minute > 0)
        {
            result = result+minute + context.getString(R.string.minute);
        }

        return result;
    }

	public static String getDateShift(final String format,final int shift)
	{
        final DateFormat dateFormat = new SimpleDateFormat(format);
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, shift);
        return dateFormat.format(cal.getTime());
	}

	public static String getTimeDiff(String start,String end)
    {
        final long MILLISECOND_PER_MINUTE = 1000*60;
        final long MILLISECOND_PER_HOUR = MILLISECOND_PER_MINUTE*60;
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        String timeDiff="";
        try
        {
            Date Date1 = format.parse(start);
            Date Date2 = format.parse(end);
            long mills = Date2.getTime() - Date1.getTime();
            long mm = (mills/MILLISECOND_PER_MINUTE)%60;
            long hh = mills/MILLISECOND_PER_HOUR;
            //timeDiff = hh+" H "+mm+" m";
            if(hh <10)
                timeDiff = timeDiff+"0";
            timeDiff = timeDiff+hh+":";
            if(mm <10)
                timeDiff = timeDiff+"0";
            timeDiff = timeDiff+mm;
        }
        catch (ParseException e)
        {
			LogUtil.e(TAG, e.toString(), Thread.currentThread().getStackTrace());
        }

        return timeDiff;
    }

}
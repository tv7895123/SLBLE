package com.startline.slble.Util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by terry on 2015/7/6.
 */
public class FileUtil
{
	public static File createFile(final String path,final String fileName)
	{
		try
		{
			final File logDir = new File(path);
			final File file = new File (path, fileName);

			if(logDir.exists() == false)
				logDir.mkdirs();

			if(file.exists() == false)
				file.createNewFile();

			return file;
		}
		catch (Exception e)
		{

		}
		return null;
	}

	public static void appendFile(final File file,final String message)
	{
		try
		{
			final FileWriter fileWriter = new FileWriter(file.getAbsolutePath(),true);
			final BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

			final Date date = new Date();
			final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh:mm:ss.sss", Locale.ENGLISH);
			final String time = simpleDateFormat.format(date);

			bufferedWriter.write(String.format("[%s]: %s",time,message));
			bufferedWriter.close();
		}
		catch (Exception e)
		{

		}
	}
}

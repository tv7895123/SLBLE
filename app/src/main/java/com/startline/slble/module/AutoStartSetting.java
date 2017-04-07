package com.startline.slble.module;

/**
 * Created by terry on 2015/8/7.
 */
public class AutoStartSetting
{
//***************************			Info		***************************
//	Name	Bit 7	Bit 6	Bit 5	Bit 4	Bit 3	Bit 2	Bit 1	Bit 0
//	BUF[0]	Daily Time              		Turbo Time
//	BUF[1]	Low Voltage Range               Temperature Range
//	BUF[2]	Clock Time ( Minute )
//	BUF[3]	Clock Time ( Hour )
//	BUF[4]	---		---		---		---		---		---		---		---

    public static final int MASK_HIGH_BYTE = 0x0F;
    public static final int MASK_LOW_BYTE = 0xF0;
    public static final int MASK_BYTE = 0xFF;

	private byte[] setting;

	public AutoStartSetting(final byte[] setting)
	{
		this.setting = setting;
	}

	public byte[] getSetting()
    {
        return setting;
    }

	public void setSetting(final byte[] value)
	{
		setting = value;
	}

	private int getHighByte(final byte src)
    {
        return (src>>4) & MASK_HIGH_BYTE;
    }

    private int getLowByte(final byte src)
    {
        return src & MASK_HIGH_BYTE;
    }

    private byte setHighByte(final byte src, final int value)
    {
        return (byte)((src & MASK_HIGH_BYTE) | ((value << 4) & MASK_LOW_BYTE));
    }

    private byte setLowByte(final byte src, final int value)
    {
        return (byte)((src & MASK_LOW_BYTE) | (value & MASK_HIGH_BYTE));
    }




    public int getTurboTime()
    {
        return getLowByte(setting[0]);
    }

    public int getDailyTime()
    {
        return getHighByte(setting[0]);
    }

    public int getTemperature()
    {
        return getLowByte(setting[1]);
    }

    public int getLowVoltage()
    {
        return getHighByte(setting[1]);
    }

    public int getClockTimeMinute()
    {
        return setting[2] & MASK_BYTE;
    }

    public int getClockTimeHour()
    {
        return setting[3] & MASK_BYTE;
    }




    public void setTurboTime(final int value)
    {
        setting[0] = setLowByte(setting[0], value);
    }

    public void setDailyTime(final int value)
    {
        setting[0] = setHighByte(setting[0], value);
    }

    public void setTemperature(final int value)
    {
        setting[1] = setLowByte(setting[1], value);
    }

    public void setLowVoltage(final int value)
    {
        setting[1] = setHighByte(setting[1], value);
    }

    public void setClockTimeMinute(final int value)
    {
        setting[2] = (byte)value;
    }

    public void setClockTimeHour(final int value)
    {
        setting[3] = (byte)value;
    }

}

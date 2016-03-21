package com.startline.slble.module;

/**
 * Created by terry on 2015/8/7.
 */
public class BleConfiguration
{
//***************************			Info		***************************
//	Name	Bit 7	Bit 6	Bit 5	Bit 4	Bit 3	Bit 2	Bit 1		Bit 0
//	BUF[0]	---		---		---		---		---		---		---			Mobile Type
//	BUF[1]	---		---		---		---		---		---		---			---
//	BUF[2]	---		---		---		---		---		---		---			---
//	BUF[3]	---		---		---		---		---		---		---			---
//	BUF[4]	---		---		---		---		---		---		---			---
	public static final int BLE_INFO_MOBILE_TYPE = 0;


//***************************			Setting		***************************
//	BUF[5]	---		---		---		---		---		---		Keyless Arm	Keyless Disarm
//	BUF[6]	---		---		---		---		---		---		---			---
//	BUF[7]	---		---		---		---		---		---		---			---
//	BUF[8]	---		---		---		---		---		---		---			---
//	BUF[9]	---		---		---		---		---		---		---			---
	public static final int BLE_SETTING_KEY_LES_DISARM = 0;
	public static final int BLE_SETTING_KEY_LES_ARM = 1;


    public static final int MASK_BIT = 0x01;
    public static final int MASK_4BIT = 0x0F;

	private long setting = 0;
	private long info = 0;

	public BleConfiguration(final long info, final long setting)
	{
	 	this.info = info;
		this.setting = setting;
	}

    public void setInfo(final long value)
    {
        info = value;
    }

	public void setSetting(final long value)
	{
		setting = value;
	}

	public static long setValue(final long param,final int value,final int shift,final int mask)
	{
		return	(param & (~(mask<<shift))) | (value<<shift);
	}

    public int getSettingValue(final int bit)
    {
        return getSettingValue(bit,0x1);
    }

    public int getSettingValue(final int bit,final int mask)
    {
		final long value = setting>>bit;
        return (int)(value & mask);
    }

    public int getInfoValue(final int bit)
    {
        return getInfoValue(bit, 0x1);
    }

    public int getInfoValue(final int bit,final int mask)
    {
		final long value = info>>bit;
        return (int)(value & mask);
    }

	public static int getValue(final long param,final int bit,final int mask)
    {
		final long value = param>>bit;
        return (int)(value & mask);
    }
}

package com.startline.slble.module;

/**
 * Created by terry on 2015/8/7.
 */
public class BleConfiguration
{
//***************************			Info		***************************
//	Name	Bit 7	Bit 6	Bit 5	Bit 4	Bit 3	Bit 2	Bit 1	Bit 0
//	BUF[0]	Info Version            		Mobile number
//	BUF[1]	Keyless Unlock                  Keyless Lock
//	BUF[2]	BTR Mode                		Slave Tag Mode
//	BUF[3]	---		---		---		---		---		---		---		---
//	BUF[4]	---		---		---		---		---		---		---		---

    public static final int MASK_HIGH_BYTE = 0x0F;
    public static final int MASK_LOW_BYTE = 0xF0;

	private byte[] setting;

	public BleConfiguration(final byte[] setting)
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

    public int getMobileNumber()
    {
        return getLowByte(setting[0]);
    }

    public int getInfoVersion()
    {
        return getHighByte(setting[0]);
    }

    public int getKeylessLock()
    {
        return getLowByte(setting[1]);
    }

    public int getKeylessUnlock()
    {
        return getHighByte(setting[1]);
    }

    public int getSlaveTagMode()
    {
        return getLowByte(setting[2]);
    }

    public int getBtrMode()
    {
        return getHighByte(setting[2]);
    }




    private byte setHighByte(final byte src, final int value)
    {
        return (byte)((src & MASK_HIGH_BYTE) | ((value << 4) & MASK_LOW_BYTE));
    }

    private byte setLowByte(final byte src, final int value)
    {
        return (byte)((src & MASK_LOW_BYTE) | (value & MASK_HIGH_BYTE));
    }

    public void setKeylessLock(final int value)
    {
        setting[1] = setLowByte(setting[1], value);
    }

    public void setKeylessUnlock(final int value)
    {
        setting[1] = setHighByte(setting[1], value);
    }

    public void setSlaveTagMode(final int value)
    {
        setting[2] = setLowByte(setting[2], value);
    }

}

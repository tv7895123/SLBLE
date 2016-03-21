package com.startline.slble.module;

/**
 * Created by terry on 2015/8/4.
 */
public class StaCstaDecode
{
	private long csta = 0;

    //*****************************************************************//
    //  CSTA Flag (STA format)                                         //
    //*****************************************************************//
	//	Bit		Bit 7		Bit 6		Bit 5		Bit 4		Bit 3		Bit 2		Bit 1		Bit 0
	//	BUF[0]	ch4_main	ch3_main	ch2_main	ch1_main	ch4_m31		ch3_m31		ch2_m31		ch1_m31
	//	BUF[1]				shk_bpss	add_bps		tilt_bpass	door		hood		trunk		ign
	//	BUF[2]	run			arm			r_start		valet		alarm		pbrake		hbrake		hijack
	//	BUF[3]	<-----------GPS Strength-----------------------><-----------GSM Strength----------------->
	//	BUF[4]

	public static final int CSTA_GPS_STRENGTH = 28;
	public static final int CSTA_GSM_STRENGTH = 24;
	public static final int CSTA_RUNNING = 23;
	public static final int CSTA_ARM = 22;
	public static final int CSTA_REMOTE_START = 21;
	public static final int CSTA_VALET = 20;
	public static final int CSTA_ALARM = 19;
	public static final int CSTA_P_BRAKE = 18;
	public static final int CSTA_H_BRAKE = 17;
	public static final int CSTA_HIJACK = 16;
	public static final int CSTA_15 = 15;
	public static final int CSTA_SHK_BYPASS = 14;
	public static final int CSTA_ADD_BYPASS = 13;
	public static final int CSTA_TILT_BYPASS = 12;
	public static final int CSTA_DOOR = 11;
	public static final int CSTA_HOOD = 10;
	public static final int CSTA_TRUNK = 9;
	public static final int CSTA_IGN = 8;
	public static final int CSTA_CH4_MAIN = 7;
	public static final int CSTA_CH3_MAIN = 6;
	public static final int CSTA_CH2_MAIN = 5;
	public static final int CSTA_CH1_MAIN = 4;
	public static final int CSTA_CH4_M31 = 3;
	public static final int CSTA_CH3_M31 = 2;
	public static final int CSTA_CH2_M31 = 1;
	public static final int CSTA_CH1_M31 = 0;

    public static final int MASK_BIT = 0x01;
    public static final int MASK_4BIT = 0x0F;


	public StaCstaDecode(final long value)
	{
	 	csta = value;
	}

    public void setCsta(final long value)
    {
        csta = value;
    }

	public static long setValue(final long csta,final int value,final int shift,final int mask)
	{
		return	(csta & (~(mask<<shift))) | (value<<shift);
	}

    public int getValue(final int bit)
    {
        return getValue(bit,0x1);
    }

    public int getValue(final int bit,final int mask)
    {
		final long value = csta>>bit;
        return (int)(value & mask);
    }

	public static int getValue(final long csta,final int bit,final int mask)
    {
		final long value = csta>>bit;
        return (int)(value & mask);
    }

	public static int getArmSTA(final byte[] dataArray)
	{
		return  (dataArray[2] >> 6) & 0x1;
	}
}

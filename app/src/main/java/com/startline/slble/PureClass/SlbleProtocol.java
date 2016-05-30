package com.startline.slble.PureClass;

/**
 * Created by terry on 2015/3/31.
 */
public class SlbleProtocol
{
	//*****************************************************************//
    //  Packet field		                                           //
    //*****************************************************************//
	public static final int PACKET_ID = 0;
	public static final int PACKET_COMMAND = 1;
	public static final int PACKET_PARAMETER = 2;
	public static final int PACKET_DATA = 3;
	public static final int PACKET_CHECK_SUM = 19;
	public static final int PACKET_LENGTH = 20;

	public static final int PACKET_DATA_LENGTH = 16;
	//*****************************************************************//
    //  Packet Command												   //
    //*****************************************************************//
	public static final int CMD_BLE_DEBUG = 0x01;
	public static final int CMD_ACK = 0x02;
	public static final int CMD_ERROR_MESSAGE = 0x03;
	public static final int CMD_CAR_STATUS = 0x20;
	public static final int CMD_PHONE_CONTROL_COMMAND = 0x30;
	public static final int CMD_TX_POWER = 0x40;
	public static final int CMD_CHECK_CONNECTION = 0x50;
	public static final int CMD_SETTING_INFORMATION = 0x60;


    //*****************************************************************//
    //  Packet Parameter											   //
    //*****************************************************************//
	public static final int PARAM_NONE = 0xFF;


	//-------------------------------------------------------------------
	// CMD_BLE_CONNECT_TEST   0x01
	public static final int PARAM_BLE_CONNECTION_TEST = 0xFF;


	//-------------------------------------------------------------------
	// CMD_ACK   0x02
	public static final int PARAM_ACK = 0xFF;


	//-------------------------------------------------------------------
	// CMD_ERROR_MESSAGE   0x03
	public static final int PARAM_MESSAGE_RESERVE = 0x00;
	public static final int PARAM_MESSAGE_CHECKSUM_ERROR = 0x01;
	public static final int PARAM_MESSAGE_FORMAT_ERROR = 0x02;
	public static final int PARAM_MESSAGE_LOGIN_PWD_ERROR = 0x03;
	public static final int PARAM_MESSAGE_RANDOM_NUMBER = 0x04;
	public static final int PARAM_MESSAGE_TX_POWER_ERROR = 0x05;


	//-------------------------------------------------------------------
	// CMD_CAR_STATUS   0x20
	public static final int PARAM_INFO_BLE_SEND_CAR_STATUS = 0xFF;


	//-------------------------------------------------------------------
	// CMD_PHONE_CONTROL_COMMAND   0x30
	public static final int PARAM_CONTROL_PHONE_SEND_COMMAND = 0xFF;


	//-------------------------------------------------------------------
	// CMD_TX_POWER   0x40
	public static final int PARAM_SET_NORMAL_LEVEL = 0x00;
	public static final int PARAM_SET_KEYLESS_LEVEL = 0x01;


	//-------------------------------------------------------------------
	// CMD_SETTING_CAR_ALARM_FUNCTION   0x60
	public static final int PARAM_SETTING_REQUEST = 0x00;
	public static final int PARAM_SETTING_RESPONSE = 0x01;


    //*****************************************************************//
    //  Protocol functions                                             //
    //*****************************************************************//
	public static byte[] getDataField(final byte[] receivedData)
	{
		if(receivedData == null || receivedData.length != PACKET_LENGTH) return null;

		byte[] data = new byte[PACKET_DATA_LENGTH];
		for(int i=0;i<data.length;i++)
		{
			data[i] = receivedData[PACKET_DATA+i];
		}

		return data;
	}

	public static byte[] setDataField(final byte[] outputPacket,final byte[] data)
	{
		if(outputPacket == null || outputPacket.length != PACKET_LENGTH) return null;
		//if(data == null || data.length != PACKET_DATA_LENGTH) return null;


		for(int i=0;i<data.length;i++)
		{
			outputPacket[PACKET_DATA+i] = data[i];
		}

		return outputPacket;
	}



    //*****************************************************************//
    //  Phone control CMD                                	           //
    //*****************************************************************//
	public static final int CONTROL_ALARM_ARM_LOCK_WITH_SIREN_CHIRP = 0x01;
	public static final int CONTROL_ALARM_DISARM_UNLOCK_WITH_SIREN_CHIRP = 0x09;
	public static final int CONTROL_ALARM_CHECK_CAR_STATUS = 0x0C;
	public static final int CONTROL_ALARM_PANIC_HIJACK = 0x0E;
	public static final int CONTROL_START_REMOTE_ENGINE_START = 0x41; //(RUN_TIME_ADD-UP)
	public static final int CONTROL_START_REMOTE_ENGINE_STOP = 0x42;
}

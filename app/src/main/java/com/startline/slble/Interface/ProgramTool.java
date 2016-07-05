package com.startline.slble.Interface;

/**
 * Created by terry on 2016/6/8.
 */
public interface  ProgramTool
{
    public static final int AF_DATA_LENGTH = 32;
    public static final int SF_DATA_LENGTH = 32;
    public static final int LNT_DATA_LENGTH = 16;
    public static final int CH_DATA_LENGTH = 16;

    public static final int PROGRAM_ADDRESS_START =0x0;
    public static final int AF_ADDRESS_HIGH =0x00;
    public static final int AF_ADDRESS_LOW =0x40;
    public static final int SF_ADDRESS_HIGH =0x00;
    public static final int SF_ADDRESS_LOW =0x60;
    public static final int LNT_ADDRESS_HIGH =0x00;
    public static final int LNT_ADDRESS_LOW =0x80;

    public static final byte FLEX_CH1_ADDRESS_HIGH =0x01;
    public static final byte FLEX_CH1_ADDRESS_LOW = (byte)0x80;
    public static final byte FLEX_CH2_ADDRESS_HIGH =0x01;
    public static final byte FLEX_CH2_ADDRESS_LOW =(byte)0x90;
    public static final byte FLEX_CH3_ADDRESS_HIGH =0x01;
    public static final byte FLEX_CH3_ADDRESS_LOW =(byte)0xA0;
    public static final byte FLEX_CH4_ADDRESS_HIGH =0x01;
    public static final byte FLEX_CH4_ADDRESS_LOW =(byte)0xB0;
    public static final byte FLEX_CH5_ADDRESS_HIGH =0x01;
    public static final byte FLEX_CH5_ADDRESS_LOW =(byte)0xC0;
    public static final byte FLEX_CH6_ADDRESS_HIGH =0x01;
    public static final byte FLEX_CH6_ADDRESS_LOW =(byte)0xD0;
    public static final byte FLEX_CH7_ADDRESS_HIGH =0x01;
    public static final byte FLEX_CH7_ADDRESS_LOW =(byte)0xE0;
    public static final byte FLEX_CH8_ADDRESS_HIGH =0x01;
    public static final byte FLEX_CH8_ADDRESS_LOW =(byte)0xF0;
    public static final byte FLEX_CH9_ADDRESS_HIGH =0x01;
    public static final byte FLEX_CH9_ADDRESS_LOW =0x40;
    public static final byte FLEX_CH10_ADDRESS_HIGH =0x01;
    public static final byte FLEX_CH10_ADDRESS_LOW =0x50;
    public static final byte FLEX_CH11_ADDRESS_HIGH =0x01;
    public static final byte FLEX_CH11_ADDRESS_LOW =0x60;
    public static final byte FLEX_CH12_ADDRESS_HIGH =0x01;
    public static final byte FLEX_CH12_ADDRESS_LOW =0x70;


    public static final int AF_START = 0;
    public static final int SF_START = 32;
    public static final int LNT_START = 64;


    public static final int AF_ITEM_AUTO_LOCK_TRUNK = 0;
    public static final int AF_ITEM_DOOR_FOLLOW_IGN = 1;
    public static final int AF_ITEM_DETECT_DOME_PIN_SWITCH = 2;
    public static final int AF_ITEM_AUTO_ARM_LOCK = 3;
    public static final int AF_ITEM_AUTO_REARM = 4;
    public static final int AF_ITEM_SIREN_VOLUME = 5;
    public static final int AF_ITEM_SLAVE_TAG_CONDITION = 6;
    public static final int AF_ITEM_ANTI_CAR_JACK = 7;
    public static final int AF_ITEM_ENGINE_KILL_OUTPUT = 8;
    public static final int AF_ITEM_2_STEP_DISARM = 9;
    public static final int AF_ITEM_RANGE_CHECK_TIMER = 10;
    public static final int AF_ITEM_DOOR_INPUT = 11;
    public static final int AF_ITEM_GSM_PROTOCOL = 12;
    public static final int AF_ITEM_GSM_OUT_COMMAND = 13;
    public static final int AF_ITEM_EVENT_1_INPUT = 14;
    public static final int AF_ITEM_CAR_ALARM_MODE = 15;
    public static final int AF_ITEM_SLAVE_MODE_EVENT = 16;
    public static final int AF_ITEM_SLAVE_AL_AU_INPUT = 17;
    public static final int AF_ITEM_EVENT_2_INPUT = 18;
    public static final int AF_ITEM_SLAVE_SEARCH_TIME = 19;
    public static final int AF_ITEM_TRUNK_DETECTION = 20;

    public static final int[][] AF_TABLE = new int[][]
    {
            // ITEM                                       ,Length,     MinValue,   MaxValue
            {AF_ITEM_AUTO_LOCK_TRUNK                   ,1,0,1}
            ,{AF_ITEM_DOOR_FOLLOW_IGN                  ,1,0,3}
            ,{AF_ITEM_DETECT_DOME_PIN_SWITCH         ,1,0,3}
            ,{AF_ITEM_AUTO_ARM_LOCK                     ,1,0,2}
            ,{AF_ITEM_AUTO_REARM                         ,1,0,2}
            ,{AF_ITEM_SIREN_VOLUME                       ,1,0,1}
            ,{AF_ITEM_SLAVE_TAG_CONDITION               ,1,0,1}
            ,{AF_ITEM_ANTI_CAR_JACK                       ,1,0,3}
            ,{AF_ITEM_ENGINE_KILL_OUTPUT                 ,1,0,3}
            ,{AF_ITEM_2_STEP_DISARM                        ,1,0,3}
            ,{AF_ITEM_RANGE_CHECK_TIMER                 ,1,0,3}
            ,{AF_ITEM_DOOR_INPUT                            ,1,0,3}
            ,{AF_ITEM_GSM_PROTOCOL                         ,1,0,1}
            ,{AF_ITEM_GSM_OUT_COMMAND                   ,1,0,3}
            ,{AF_ITEM_EVENT_1_INPUT                        ,1,0,3}
            ,{AF_ITEM_CAR_ALARM_MODE                    ,1,0,3}
            ,{AF_ITEM_SLAVE_MODE_EVENT                    ,1,0,3}
            ,{AF_ITEM_SLAVE_AL_AU_INPUT                     ,1,0,3}
            ,{AF_ITEM_EVENT_2_INPUT                         ,1,0,3}
            ,{AF_ITEM_SLAVE_SEARCH_TIME                    ,1,0,3}
            ,{AF_ITEM_TRUNK_DETECTION                       ,1,0,1}
    };

    public static final int SF_ITEM_ENGINE_START = 0;
    public static final int SF_ITEM_RUNNING_TIME = 1;
    public static final int SF_ITEM_SENSOR_RUNNING = 2;
    public static final int SF_ITEM_AUTO_SHUT_ENGINE = 3;
    public static final int SF_ITEM_START_WITH_LOCK = 4;
    public static final int SF_ITEM_START_WITH_PARKING_LIGHT = 5;
    public static final int SF_ITEM_LOCKING_MANAGEMENT = 6;
    public static final int SF_ITEM_AUTO_DISARM = 7;
    public static final int SF_ITEM_CRANKING_TIME = 8;
    public static final int SF_ITEM_FUEL_TYPE = 9;
    public static final int SF_ITEM_ENGINE_RUNNING_DETECT = 10;
    public static final int SF_ITEM_TURBO_TIME_ACTIVE = 11;
    public static final int SF_ITEM_IGN3_BYPASS = 12;
    public static final int SF_ITEM_ENGINE_START_PTS = 13;
    public static final int SF_ITEM_AUTO_GEAR = 14;
    public static final int SF_ITEM_ENGINE_START_WEBASTO = 15;
    public static final int SF_ITEM_WEBASTO_TIME = 16;
    public static final int SF_ITEM_ENGINE_START_ALGROITHM = 17;

    public static final int[][] SF_TABLE = new int[][]
    {
            // ITEM                                       ,Length,     MinValue,   MaxValue
            {SF_ITEM_ENGINE_START                   ,1,0,2}
            ,{SF_ITEM_RUNNING_TIME                  ,1,0,3}
            ,{SF_ITEM_SENSOR_RUNNING               ,1,0,3}
            ,{SF_ITEM_AUTO_SHUT_ENGINE             ,1,0,1}
            ,{SF_ITEM_START_WITH_LOCK              ,1,0,1}
            ,{SF_ITEM_START_WITH_PARKING_LIGHT   ,1,0,1}
            ,{SF_ITEM_LOCKING_MANAGEMENT         ,1,0,3}
            ,{SF_ITEM_AUTO_DISARM                    ,1,0,1}
            ,{SF_ITEM_CRANKING_TIME                  ,1,0,3}
            ,{SF_ITEM_FUEL_TYPE                        ,1,0,3}
            ,{SF_ITEM_ENGINE_RUNNING_DETECT       ,1,0,3}
            ,{SF_ITEM_TURBO_TIME_ACTIVE             ,1,0,3}
            ,{SF_ITEM_IGN3_BYPASS                      ,1,0,3}
            ,{SF_ITEM_ENGINE_START_PTS               ,1,0,3}
            ,{SF_ITEM_AUTO_GEAR                        ,1,0,3}
            ,{SF_ITEM_ENGINE_START_WEBASTO         ,1,0,3}
            ,{SF_ITEM_WEBASTO_TIME                    ,1,0,3}
            ,{SF_ITEM_ENGINE_START_ALGROITHM      ,1,0,3}
    };


    // LNT Fragment
    public static final int LNT_ITEM_PIN_CODE_1 = 3;
    public static final int LNT_ITEM_PIN_CODE_2 = 4;
    public static final int LNT_ITEM_PIN_CODE_3 = 5;
    public static final int LNT_ITEM_PIN_CODE_4 = 6;
    public static final int LNT_ITEM_TILT_SENSOR = 11;
    public static final int LNT_ITEM_SHOCK_SENSOR = 12;
    public static final int LNT_ITEM_SIREN = 13;

    // CH Fragment
    public static final int CH_ITEM_EVENT_ON = 0;
    public static final int CH_ITEM_EVENT_OFF = 1;
    public static final int CH_ITEM_T1 = 2;
    public static final int CH_ITEM_T2 = 4;
    public static final int CH_ITEM_T3 = 6;
    public static final int CH_ITEM_T4 = 8;
    public static final int CH_ITEM_CONDITION_1 = 10;
    public static final int CH_ITEM_CONDITION_2 = 11;
    public static final int CH_ITEM_CONDITION_3 = 12;
    public static final int CH_ITEM_BYPASS = 13;
    public static final int CH_ITEM_FUNCTION = 14;
}

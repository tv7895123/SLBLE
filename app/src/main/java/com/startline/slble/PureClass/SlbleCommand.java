package com.startline.slble.PureClass;

/**
 * Created by terry on 2016/8/4.
 */
public class SlbleCommand implements Cloneable
{
    public int command;
    public int packetId;
    public int state;

    public int maxTimes;
    public int currentTimes;
    public int resend_interval;

    public Object data;

    public long updateTime;

    public SlbleCommand()
    {
        command = 0;
        packetId = 0;
        state = -1;
        maxTimes = 0;
        currentTimes = 0;
        resend_interval = 0;

        data = null;
        updateTime = 0;
    }

    public Object clone() throws CloneNotSupportedException
    {
        return super.clone();
    }
}

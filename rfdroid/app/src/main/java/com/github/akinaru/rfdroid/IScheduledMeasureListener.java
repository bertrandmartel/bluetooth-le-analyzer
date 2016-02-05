package com.github.akinaru.rfdroid;

import java.util.List;

/**
 * Created by akinaru on 05/02/16.
 */
public interface IScheduledMeasureListener {

    public void onNewMeasure(long samplingTime,
                             int finalPacketReceptionRate,
                             List<Integer> globalSumPerSecond,
                             List<Integer> globalPacketReceivedPerSecond,
                             float averagePacket);

    public void onMeasureClear();
}

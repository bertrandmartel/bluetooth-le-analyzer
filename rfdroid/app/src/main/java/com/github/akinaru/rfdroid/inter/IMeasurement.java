package com.github.akinaru.rfdroid.inter;

import com.github.akinaru.rfdroid.bluetooth.events.BluetoothObject;

import java.util.List;

/**
 * Created by akinaru on 05/02/16.
 */
public interface IMeasurement {

    public List<Long> getHistoryList();

    public void setBtDevice(BluetoothObject btDevice);

    public BluetoothObject getBtDevice();

    public boolean isSelectionningDevice();
}

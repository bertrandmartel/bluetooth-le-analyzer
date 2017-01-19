/****************************************************************************
 * This file is part of Bluetooth LE Analyzer.                              *
 * <p/>                                                                     *
 * Copyright (C) 2017  Bertrand Martel                                      *
 * <p/>                                                                     *
 * Foobar is free software: you can redistribute it and/or modify           *
 * it under the terms of the GNU General Public License as published by     *
 * the Free Software Foundation, either version 3 of the License, or        *
 * (at your option) any later version.                                      *
 * <p/>                                                                     *
 * Foobar is distributed in the hope that it will be useful,                *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of           *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the            *
 * GNU General Public License for more details.                             *
 * <p/>                                                                     *
 * You should have received a copy of the GNU General Public License        *
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.          *
 */
package com.github.akinaru.bleanalyzer.service;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.github.akinaru.bleanalyzer.bluetooth.BluetoothCustomManager;
import com.github.akinaru.bleanalyzer.bluetooth.connection.IBluetoothDeviceConn;
import com.github.akinaru.bleanalyzer.bluetooth.events.BluetoothObject;
import com.github.akinaru.bleanalyzer.inter.IADListener;
import com.github.akinaru.bleanalyzer.inter.IMeasurement;
import com.github.akinaru.bleanalyzer.inter.IScheduledMeasureListener;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Service persisting bluetooth connection
 *
 * @author Bertrand Martel
 */
public class BtAnalyzerService extends Service implements IMeasurement {

    private String TAG = BtAnalyzerService.class.getSimpleName();

    /**
     * Service binder
     */
    private final IBinder mBinder = new LocalBinder();

    public void setADListener(IADListener listener) {
        btManager.setADListener(listener);
    }

    private IScheduledMeasureListener scheduledMeasureListener = null;

    private BluetoothObject btDevice = null;

    private boolean selectionning = false;

    @Override
    public List<Long> getHistoryList() {
        return history;
    }

    @Override
    public void setBtDevice(BluetoothObject btDevice) {
        this.btDevice = btDevice;
    }

    @Override
    public BluetoothObject getBtDevice() {
        return btDevice;
    }

    @Override
    public boolean isSelectionningDevice() {
        return selectionning;
    }

    public void setSelectionningDevice(boolean state) {
        this.selectionning = state;
    }

    public List<Integer> getGlobalSumPerSecond() {
        return globalSumPerSecond;
    }

    public List<Integer> getGlobalPacketReceivedPerSecond() {
        return globalPacketReceivedPerSecond;
    }

    /*
     * LocalBInder that render public getService() for public access
     */
    public class LocalBinder extends Binder {
        public BtAnalyzerService getService() {
            return BtAnalyzerService.this;
        }
    }

    private BluetoothCustomManager btManager = null;

    private ScheduledExecutorService executor;

    private ScheduledFuture<?> measurementTask;

    private List<Long> history = new ArrayList<>();
    private List<Integer> globalSumPerSecond = new ArrayList<>();
    private List<Integer> globalPacketReceivedPerSecond = new ArrayList<>();

    @Override
    public void onCreate() {

        //initiate bluetooth manager object used to manage all Android Bluetooth API
        btManager = new BluetoothCustomManager(this, this);

        //initialize bluetooth adapter
        btManager.init(this);
        executor = Executors.newScheduledThreadPool(1);

        setMeasurementTask();
    }

    /**
     * start measurement task
     */
    public void setMeasurementTask() {

        stopMeasurement();
        history.clear();
        globalSumPerSecond.clear();
        globalPacketReceivedPerSecond.clear();

        if (scheduledMeasureListener != null)
            scheduledMeasureListener.onMeasureClear();

        measurementTask = executor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {

                if (btDevice != null && btDevice.getAdvertizingInterval() > 0 && history.size() > 0) {

                    final long ts = new Date().getTime();
                    final List<Long> historyCopy = new ArrayList<Long>(history);

                    int packetReceptionRate = calculateReceptionRate(historyCopy, ts);

                    int rateCurrentSecond = calculateRateLastMillis(1000, historyCopy, ts);

                    globalSumPerSecond.add(rateCurrentSecond);

                    int totalPacketReceived = calculateTotalPacketReceivedMillis(1000, historyCopy, ts);

                    globalPacketReceivedPerSecond.add(totalPacketReceived);

                    if (packetReceptionRate > 100)
                        packetReceptionRate = 100;

                    final int finalPacketReceptionRate = packetReceptionRate;

                    if (scheduledMeasureListener != null) {
                        scheduledMeasureListener.onNewMeasure(getSamplingTime(ts),
                                finalPacketReceptionRate,
                                globalSumPerSecond,
                                globalPacketReceivedPerSecond,
                                getAveragePacket(ts, historyCopy));
                    }

                } else if (btDevice != null && history.size() > 0) {

                    final long ts = new Date().getTime();
                    final List<Long> historyCopy = new ArrayList<Long>(history);

                    int totalPacketReceived = calculateTotalPacketReceivedMillis(1000, historyCopy, ts);

                    globalPacketReceivedPerSecond.add(totalPacketReceived);

                    if (scheduledMeasureListener != null) {
                        scheduledMeasureListener.onNewMeasure(getSamplingTime(ts),
                                -1,
                                null,
                                globalPacketReceivedPerSecond,
                                getAveragePacket(ts, historyCopy));
                    }

                }

            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    private float getAveragePacket(long currentTS, List<Long> historyList) {

        if (history.size() > 0) {
            if ((currentTS - history.get(0)) != 0) {
                return ((historyList.size() * 1000) / (float) ((currentTS - historyList.get(0))));
            }
            return 1;
        }
        return 0;
    }

    private long getSamplingTime(long ts) {
        return (ts - history.get(0)) / 1000;
    }

    private int calculateReceptionRate(List<Long> historyList, long ts) {

        if (btDevice.getAdvertizingInterval() >= (ts - historyList.get(0)))
            return 100;
        return (int) ((historyList.size() * 100) / ((ts - historyList.get(0)) / btDevice.getAdvertizingInterval()));
    }

    private int calculateTotalPacketReceivedMillis(int millis, List<Long> historyList, long ts) {

        long currentTS = ts - millis;

        int count = 0;
        boolean control = false;

        for (int i = historyList.size() - 1; i >= 0 && !control; i--) {

            if (historyList.get(i) <= currentTS)
                control = true;
            else {
                count++;
            }
        }
        return count;
    }

    private int calculateRateLastMillis(int millis, List<Long> historyList, long ts) {

        long currentTS = ts - millis;

        int count = 0;
        boolean control = false;

        for (int i = historyList.size() - 1; i >= 0 && !control; i--) {

            if (historyList.get(i) <= currentTS)
                control = true;
            else {
                count++;
            }
        }
        return (int) ((count * 100) / (millis / btDevice.getAdvertizingInterval()));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopMeasurement();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * retrieve scanning list
     *
     * @return
     */
    public Map<String, BluetoothDevice> getScanningList() {
        return btManager.getScanningList();
    }

    /**
     * define a listener for measurement events
     *
     * @param listener
     */
    public void setScheduledMeasureListener(IScheduledMeasureListener listener) {
        scheduledMeasureListener = listener;
    }

    /**
     * get scanning state
     *
     * @return
     */
    public boolean isScanning() {
        return btManager.isScanning();
    }

    /**
     * stop bluetooth scan
     */
    public void stopScan() {
        stopMeasurement();
        btManager.stopScan();
    }

    /**
     * connect to a bluetooth device
     *
     * @param deviceAddress
     */
    public void connect(String deviceAddress) {
        btManager.connect(deviceAddress);
    }

    /**
     * stop measurement task
     */
    public void stopMeasurement() {
        if (measurementTask != null) {
            measurementTask.cancel(true);
            measurementTask = null;
        }
    }

    /**
     * start Bluetooth scan
     *
     * @return
     */
    public boolean startScan() {
        setMeasurementTask();
        return btManager.scanLeDevice();
    }

    /**
     * clear bluetooth scanning list
     */
    public void clearScanningList() {
        btManager.clearScanningList();
    }

    /**
     * disconnect a Bluetooth device by address
     *
     * @param deviceAddress bluetooth device address
     * @return true if disconection is successfull
     */
    public boolean disconnect(String deviceAddress) {
        return btManager.disconnect(deviceAddress);
    }

    /**
     * disconnect all bluetooth devices
     */
    public void disconnectall() {
        btManager.disconnectAll();
    }

    /**
     * Retrieve all devices associated
     *
     * @return
     */
    public HashMap<String, IBluetoothDeviceConn> getConnectionList() {
        return btManager.getConnectionList();
    }

}
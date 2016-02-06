/**
 * The MIT License (MIT)
 * <p/>
 * Copyright (c) 2016 Bertrand Martel
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.github.akinaru.rfdroid.service;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.github.akinaru.rfdroid.inter.IADListener;
import com.github.akinaru.rfdroid.inter.IMeasurement;
import com.github.akinaru.rfdroid.inter.IScheduledMeasureListener;
import com.github.akinaru.rfdroid.bluetooth.BluetoothCustomManager;
import com.github.akinaru.rfdroid.bluetooth.connection.IBluetoothDeviceConn;
import com.github.akinaru.rfdroid.bluetooth.events.BluetoothObject;

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
public class RFdroidService extends Service implements IMeasurement {

    private String TAG = RFdroidService.class.getSimpleName();

    /**
     * Service binder
     */
    private final IBinder mBinder = new LocalBinder();

    public void setADListener(IADListener listener) {
        btManager.setADListener(listener);
    }

    private IScheduledMeasureListener scheduledMeasureListener = null;

    private BluetoothObject btDevice = null;

    @Override
    public List<Long> getHistoryList() {
        return history;
    }

    @Override
    public void setBtDevice(BluetoothObject btDevice) {
        this.btDevice = btDevice;
    }

    public BluetoothObject getBtDevice() {
        return btDevice;
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
        public RFdroidService getService() {
            return RFdroidService.this;
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

    private void setMeasurementTask() {

        if (measurementTask != null) {
            measurementTask.cancel(true);
            measurementTask = null;
        }
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
        if (measurementTask != null) {
            measurementTask.cancel(true);
            measurementTask = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public Map<String, BluetoothDevice> getScanningList() {
        return btManager.getScanningList();
    }

    public void setScheduledMeasureListener(IScheduledMeasureListener listener) {
        scheduledMeasureListener = listener;
    }

    public boolean isScanning() {
        return btManager.isScanning();
    }

    public void stopScan() {
        if (measurementTask != null) {
            measurementTask.cancel(true);
            measurementTask = null;
        }
        btManager.stopScan();
    }

    public void connect(String deviceAddress) {
        btManager.connect(deviceAddress);
    }

    public boolean startScan() {
        setMeasurementTask();
        return btManager.scanLeDevice();
    }

    public void clearScanningList() {
        btManager.clearScanningList();
    }

    public boolean disconnect(String deviceAddress) {
        return btManager.disconnect(deviceAddress);
    }

    public void disconnectall() {
        btManager.disconnectAll();
    }

    public HashMap<String, IBluetoothDeviceConn> getConnectionList() {
        return btManager.getConnectionList();
    }

}
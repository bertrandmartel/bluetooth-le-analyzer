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
package com.github.akinaru.rfdroid.bluetooth.rfduino;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import com.github.akinaru.rfdroid.bluetooth.connection.BluetoothDeviceAbstr;
import com.github.akinaru.rfdroid.bluetooth.connection.IBluetoothDeviceConn;
import com.github.akinaru.rfdroid.bluetooth.listener.ICharacteristicListener;
import com.github.akinaru.rfdroid.bluetooth.listener.IDeviceInitListener;
import com.github.akinaru.rfdroid.bluetooth.listener.IPushListener;

import java.util.ArrayList;
import java.util.UUID;

/**
 * RFduino Bluetooth device management
 *
 * @author Bertrand Martel
 */
public class RfduinoDevice extends BluetoothDeviceAbstr implements IRfduinoDevice {

    private String TAG = RfduinoDevice.this.getClass().getName();

    public final static String RFDUINO_SERVICE = "00002220-0000-1000-8000-00805f9b34fb";
    public final static String RFDUINO_RECEIVE_CHARAC = "00002221-0000-1000-8000-00805f9b34fb";
    public final static String RFDUINO_SEND_CHARAC = "00002222-0000-1000-8000-00805f9b34fb";

    private ArrayList<IDeviceInitListener> initListenerList = new ArrayList<>();

    private boolean init = false;

    /**
     * @param conn
     */
    @SuppressLint("NewApi")
    public RfduinoDevice(IBluetoothDeviceConn conn) {
        super(conn);
        setCharacteristicListener(new ICharacteristicListener() {

            @Override
            public void onCharacteristicReadReceived(BluetoothGattCharacteristic charac) {

            }

            @Override
            public void onCharacteristicChangeReceived(BluetoothGattCharacteristic charac) {

            }

            @Override
            public void onCharacteristicWriteReceived(BluetoothGattCharacteristic charac) {

            }
        });
    }

    @Override
    public void init() {

        Log.i(TAG, "initializing RFduino");

        conn.enableDisableNotification(UUID.fromString(RFDUINO_SERVICE), UUID.fromString(RFDUINO_RECEIVE_CHARAC), true);
        conn.enableGattNotifications(RFDUINO_SERVICE, RFDUINO_RECEIVE_CHARAC);

        for (int i = 0; i < initListenerList.size(); i++) {
            initListenerList.get(i).onInit();
        }
    }

    @Override
    public boolean isInit() {
        return init;
    }

    @Override
    public void addInitListener(IDeviceInitListener listener) {
        initListenerList.add(listener);
    }

    @Override
    public void setAdvertisingInterval(int intervalMillis, IPushListener listener) {

        Log.i(TAG, "setAdvertisingInterval " + intervalMillis + "ms");

        byte[] data = new byte[]{(byte) (intervalMillis >> 8), (byte) intervalMillis};
        getConn().writeCharacteristic(RFDUINO_SERVICE, RFDUINO_SEND_CHARAC, data, listener);

    }
}
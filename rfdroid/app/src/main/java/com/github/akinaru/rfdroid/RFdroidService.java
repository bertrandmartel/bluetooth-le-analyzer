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
package com.github.akinaru.rfdroid;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.github.akinaru.rfdroid.bluetooth.BluetoothCustomManager;
import com.github.akinaru.rfdroid.bluetooth.connection.IBluetoothDeviceConn;

import java.util.HashMap;
import java.util.Map;

/**
 * Service persisting bluetooth connection
 *
 * @author Bertrand Martel
 */
public class RFdroidService extends Service {

    /**
     * Service binder
     */
    private final IBinder mBinder = new LocalBinder();

    /*
     * LocalBInder that render public getService() for public access
     */
    public class LocalBinder extends Binder {
        public RFdroidService getService() {
            return RFdroidService.this;
        }
    }

    private BluetoothCustomManager btManager = null;

    @Override
    public void onCreate() {

        //initiate bluetooth manager object used to manage all Android Bluetooth API
        btManager = new BluetoothCustomManager(this);

        //initialize bluetooth adapter
        btManager.init(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public Map<String, BluetoothDevice> getScanningList() {
        return btManager.getScanningList();
    }

    public boolean isScanning() {
        return btManager.isScanning();
    }

    public void stopScan() {
        btManager.stopScan();
    }

    public void connect(String deviceAddress) {
        btManager.connect(deviceAddress);
    }

    public boolean startScan() {
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
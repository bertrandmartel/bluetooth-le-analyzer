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

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.akinaru.rfdroid.bluetooth.events.BluetoothEvents;
import com.github.akinaru.rfdroid.bluetooth.events.BluetoothObject;
import com.github.akinaru.rfdroid.bluetooth.listener.IPushListener;
import com.github.akinaru.rfdroid.bluetooth.rfduino.IRfduinoDevice;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

/**
 * Dotti device management main activity
 *
 * @author Bertrand Martel
 */
public class RFdroidActivity extends Activity {

    /**
     * debug tag
     */
    private String TAG = this.getClass().getName();

    private String deviceAddress = "";

    private ProgressDialog dialog = null;

    private boolean toSecondLevel = false;

    private boolean bound = false;

    /**
     * define if bluetooth is enabled on device
     */
    private final static int REQUEST_ENABLE_BT = 1;

    /**
     * Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;

    /**
     * list of device to display
     */
    private ListView scanningListView = null;

    private ScanItemArrayAdapter scanningAdapter = null;

    /**
     * current index of connecting device item in device list
     */
    private int list_item_position = 0;

    private RFdroidService currentService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rfdroid);

        // Use this check to determine whether BLE is supported on the device. Then
        // you can selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Bluetooth Smart is not supported on your device", Toast.LENGTH_SHORT).show();
            finish();
        }

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        final ProgressBar progress_bar = (ProgressBar) findViewById(R.id.scanningProgress);

        if (progress_bar != null)
            progress_bar.setEnabled(false);

        final Button button_stop_scanning = (Button) findViewById(R.id.stop_scanning_button);

        if (button_stop_scanning != null)
            button_stop_scanning.setEnabled(false);

        final TextView scanText = (TextView) findViewById(R.id.scanText);

        if (scanText != null)
            scanText.setText("");

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

        button_stop_scanning.setEnabled(false);

        final Button button_find_accessory = (Button) findViewById(R.id.scanning_button);

        button_stop_scanning.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (currentService != null && currentService.isScanning()) {

                    currentService.stopScan();

                    if (progress_bar != null) {
                        progress_bar.setEnabled(false);
                        progress_bar.setVisibility(View.GONE);
                    }

                    if (scanText != null)
                        scanText.setText("");

                    if (button_stop_scanning != null)
                        button_stop_scanning.setEnabled(false);
                }
            }
        });

        button_find_accessory.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                triggerNewScan();
            }
        });

        Button sendBtn = (Button) findViewById(R.id.send);
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (deviceAddress != null && !deviceAddress.equals("")) {

                    if (currentService.getConnectionList().get(deviceAddress).getDevice() instanceof IRfduinoDevice) {
                        IRfduinoDevice device = (IRfduinoDevice) currentService.getConnectionList().get(deviceAddress).getDevice();

                        device.setAdvertisingInterval(100, new IPushListener() {
                            @Override
                            public void onPushFailure() {
                                Log.i(TAG, "onPushFailure");
                            }

                            @Override
                            public void onPushSuccess() {
                                Log.i(TAG, "onPushSuccess");
                            }
                        });
                    }
                }

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQUEST_ENABLE_BT) {

            if (mBluetoothAdapter.isEnabled()) {


                Intent intent = new Intent(this, RFdroidService.class);

                // bind the service to current activity and create it if it didnt exist before
                startService(intent);
                bound = bindService(intent, mServiceConnection, BIND_AUTO_CREATE);

            } else {

                Toast.makeText(this, "Bluetooth disabled", Toast.LENGTH_SHORT).show();
            }
        }
    }


    /**
     * trigger a BLE scan
     */
    public void triggerNewScan() {

        Button button_stop_scanning = (Button) findViewById(R.id.stop_scanning_button);
        ProgressBar progress_bar = (ProgressBar) findViewById(R.id.scanningProgress);
        TextView scanText = (TextView) findViewById(R.id.scanText);

        if (button_stop_scanning != null && progress_bar != null && scanText != null) {
            if (currentService != null && !currentService.isScanning()) {

                Toast.makeText(RFdroidActivity.this, "Looking for new accessories", Toast.LENGTH_SHORT).show();

                if (button_stop_scanning != null)
                    button_stop_scanning.setEnabled(true);

                if (progress_bar != null) {
                    progress_bar.setEnabled(true);
                    progress_bar.setVisibility(View.VISIBLE);
                }

                if (scanText != null)
                    scanText.setText("Scanning ...");

                //start scan so clear list view

                if (scanningAdapter != null) {
                    scanningAdapter.clear();
                    scanningAdapter.notifyDataSetChanged();
                }

                currentService.clearScanningList();

                Log.i(TAG, "START SCAN");

                currentService.disconnectall();

                currentService.startScan();

            } else {
                Toast.makeText(RFdroidActivity.this, "Scanning already engaged...", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "RFdroidActivity onDestroy");
        //currentService.disconnect(deviceAddress);
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
        toSecondLevel = false;

        if (mBluetoothAdapter.isEnabled()) {

            Intent intent = new Intent(this, RFdroidService.class);

            // bind the service to current activity and create it if it didnt exist before
            startService(intent);
            bound = bindService(intent, mServiceConnection, BIND_AUTO_CREATE);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (!toSecondLevel) {

            if (scanningListView != null) {
                scanningListView.setAdapter(null);
            }

            if (currentService != null) {

                currentService.disconnectall();

                /*
                if (scanningAdapter != null) {
                    scanningAdapter.clear();
                    scanningAdapter.notifyDataSetChanged();
                }
                */
            }
        }

        if (dialog != null) {
            dialog.cancel();
            dialog = null;
        }

        if (currentService != null) {
            if (currentService.isScanning())
                currentService.stopScan();
        }

        try {
            if (bound) {
                // unregister receiver or you will have strong exception
                unbindService(mServiceConnection);
                bound = false;
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            final String action = intent.getAction();

            if (BluetoothEvents.BT_EVENT_SCAN_START.equals(action)) {

                Log.i(TAG, "Scan has started");

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        final Button button_start_pairing = (Button) findViewById(R.id.scanning_button);
                        button_start_pairing.setEnabled(false);
                    }
                });
            } else if (BluetoothEvents.BT_EVENT_SCAN_END.equals(action)) {

                Log.i(TAG, "Scan has ended");

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        final Button button_stop_scanning = (Button) findViewById(R.id.stop_scanning_button);
                        final ProgressBar progress_bar = (ProgressBar) findViewById(R.id.scanningProgress);
                        final TextView scanText = (TextView) findViewById(R.id.scanText);

                        Toast.makeText(RFdroidActivity.this, "End of scanning...", Toast.LENGTH_SHORT).show();

                        if (button_stop_scanning != null)
                            button_stop_scanning.setEnabled(false);
                        if (progress_bar != null)
                            progress_bar.setEnabled(false);
                        if (scanText != null)
                            scanText.setText("");

                        final Button button_start_pairing = (Button) findViewById(R.id.scanning_button);
                        button_start_pairing.setEnabled(true);
                    }
                });

            } else if (BluetoothEvents.BT_EVENT_DEVICE_DISCOVERED.equals(action)) {

                Log.i(TAG, "New device has been discovered");

                final BluetoothObject btDevice = BluetoothObject.parseArrayList(intent);

                if (btDevice != null) {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            if (scanningAdapter != null) {
                                scanningAdapter.add(btDevice);
                                scanningAdapter.notifyDataSetChanged();
                            }
                        }
                    });
                }

            } else if (BluetoothEvents.BT_EVENT_DEVICE_DISCONNECTED.equals(action)) {

                Log.i(TAG, "Device disconnected");

                BluetoothObject btDevice = BluetoothObject.parseArrayList(intent);

                if (btDevice != null) {

                    if (scanningListView != null && scanningListView.getChildAt(list_item_position) != null) {
                        scanningListView.getChildAt(list_item_position).setBackgroundColor(Color.TRANSPARENT);
                    }

                    invalidateOptionsMenu();

                    if (dialog != null) {

                        dialog.cancel();
                        dialog = null;
                    }
                }

            } else if (BluetoothEvents.BT_EVENT_DEVICE_CONNECTED.equals(action)) {

                Log.i(TAG, "Device connected");

                BluetoothObject btDevice = BluetoothObject.parseArrayList(intent);

                if (btDevice != null) {

                    scanningListView.getChildAt(list_item_position).setBackgroundColor(Color.BLUE);
                    invalidateOptionsMenu();

                    Log.i(TAG, "Setting for device = > " + btDevice.getDeviceAddress() + " - " + btDevice.getDeviceName());

                    if (dialog != null) {
                        dialog.cancel();
                        dialog = null;
                    }

                    /*
                    Intent intentDevice = new Intent(RFdroidActivity.this, NottiDeviceActivity.class);
                    intentDevice.putExtra("deviceAddr", btDevice.getDeviceAddress());
                    intentDevice.putExtra("deviceName", btDevice.getDeviceName());
                    toSecondLevel = true;
                    startActivity(intentDevice);
                    */
                }
            }
        }
    };

    /**
     * Manage Bluetooth Service lifecycle
     */
    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            Log.i(TAG, "Connected to service");

            currentService = ((RFdroidService.LocalBinder) service).getService();

            scanningListView = (ListView) findViewById(R.id.listView);

            final ArrayList<BluetoothObject> list = new ArrayList<>();

            Iterator it = currentService.getScanningList().entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, BluetoothDevice> entry = (Map.Entry) it.next();
                list.add(new BluetoothObject(entry.getValue().getAddress(), entry.getValue().getName(), -1));
            }

            scanningAdapter = new ScanItemArrayAdapter(RFdroidActivity.this,
                    android.R.layout.simple_list_item_1, list);

            scanningListView.setAdapter(scanningAdapter);

            scanningListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, final View view,
                                        int position, long id) {

                    final ProgressBar progress_bar = (ProgressBar) findViewById(R.id.scanningProgress);
                    final TextView scanText = (TextView) findViewById(R.id.scanText);

                    if (progress_bar != null) {
                        progress_bar.setEnabled(false);
                        progress_bar.setVisibility(View.GONE);
                    }

                    if (scanText != null)
                        scanText.setText("");

                    /*stop scanning*/
                    if (currentService.isScanning()) {

                        currentService.stopScan();
                    }

                    /*connect to bluetooth gatt server on the device*/
                    deviceAddress = scanningAdapter.getItem(position).getDeviceAddress();

                    list_item_position = position;

                    if (!currentService.getConnectionList().containsKey(deviceAddress) ||
                            !currentService.getConnectionList().get(deviceAddress).isConnected()) {

                        dialog = ProgressDialog.show(RFdroidActivity.this, "", "Connecting ...", true);

                        currentService.connect(deviceAddress);
                    } else {

                        if (dialog != null) {
                            dialog.cancel();
                            dialog = null;
                        }
                        /*
                        Intent intentDevice = new Intent(RFdroidActivity.this, NottiDeviceActivity.class);
                        intentDevice.putExtra("deviceAddr", deviceAddress);
                        intentDevice.putExtra("deviceName", currentService.getConnectionList().get(deviceAddress).getDeviceName());
                        toSecondLevel = true;
                        startActivity(intentDevice);
                        */

                    }
                }
            });

            triggerNewScan();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }

    };

    /**
     * add filter to intent to receive notification from bluetooth service
     *
     * @return intent filter
     */
    private static IntentFilter makeGattUpdateIntentFilter() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothEvents.BT_EVENT_SCAN_START);
        intentFilter.addAction(BluetoothEvents.BT_EVENT_SCAN_END);
        intentFilter.addAction(BluetoothEvents.BT_EVENT_DEVICE_DISCOVERED);
        intentFilter.addAction(BluetoothEvents.BT_EVENT_DEVICE_CONNECTED);
        intentFilter.addAction(BluetoothEvents.BT_EVENT_DEVICE_DISCONNECTED);
        return intentFilter;
    }
}